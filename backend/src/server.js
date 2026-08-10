import "dotenv/config";
import crypto from "node:crypto";
import express from "express";
import { getAssistantReply, clearSession } from "./chatbot.js";
import { listOrders, getOrder } from "./orderStore.js";

const app = express();
const port = Number(process.env.PORT || 3000);
const processedMessageIds = new Set();

function graphApiVersion() {
  return process.env.GRAPH_API_VERSION || "v23.0";
}

function verifyMetaSignature(rawBody, signatureHeader) {
  const secret = process.env.META_APP_SECRET;
  if (!secret) return process.env.NODE_ENV !== "production";
  if (!signatureHeader?.startsWith("sha256=")) return false;

  const received = signatureHeader.slice("sha256=".length);
  const expected = crypto.createHmac("sha256", secret).update(rawBody).digest("hex");
  const a = Buffer.from(received, "hex");
  const b = Buffer.from(expected, "hex");
  return a.length === b.length && crypto.timingSafeEqual(a, b);
}

async function sendWhatsAppText(to, body) {
  const token = process.env.WHATSAPP_ACCESS_TOKEN;
  const phoneNumberId = process.env.WHATSAPP_PHONE_NUMBER_ID;
  if (!token || !phoneNumberId) throw new Error("Credenciais do WhatsApp não configuradas");

  const response = await fetch(
    `https://graph.facebook.com/${graphApiVersion()}/${phoneNumberId}/messages`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        messaging_product: "whatsapp",
        recipient_type: "individual",
        to,
        type: "text",
        text: { preview_url: false, body }
      })
    }
  );

  if (!response.ok) {
    const detail = await response.text();
    throw new Error(`WhatsApp API ${response.status}: ${detail}`);
  }

  return response.json();
}

function formatOwnerAlert(event) {
  if (event.handoff) {
    return `🚨 Atendimento humano solicitado\nCliente: ${event.customerPhone}\nMotivo: ${event.reason}`;
  }

  const items = event.items
    .map((item) => `${item.quantity}x ${item.flavor}${item.drink_1_5l ? " + refri 1,5 L" : ""}`)
    .join("\n");
  const total = event.total == null ? "a confirmar" : `R$ ${event.total.toFixed(2).replace(".", ",")}`;

  return `🍕 NOVO PEDIDO #${event.number}\n${event.customerName} - ${event.customerPhone}\n${items}\nPagamento: ${event.paymentMethod}\nEndereço: ${event.address}${event.reference ? ` (${event.reference})` : ""}\nTotal: ${total}\nTaxa de entrega: a confirmar`;
}

async function notifyOwner(event) {
  const ownerNumber = process.env.OWNER_ALERT_NUMBER;
  if (!ownerNumber || ownerNumber === event.customerPhone) return;
  await sendWhatsAppText(ownerNumber, formatOwnerAlert(event));
}

function extractText(message) {
  if (message.type === "text") return message.text?.body || "";
  if (message.type === "button") return message.button?.text || message.button?.payload || "";
  if (message.type === "interactive") {
    return (
      message.interactive?.button_reply?.title ||
      message.interactive?.button_reply?.id ||
      message.interactive?.list_reply?.title ||
      message.interactive?.list_reply?.id ||
      ""
    );
  }
  return "";
}

function inboundMessages(payload) {
  const result = [];
  for (const entry of payload.entry || []) {
    for (const change of entry.changes || []) {
      for (const message of change.value?.messages || []) {
        result.push(message);
      }
    }
  }
  return result;
}

async function handleIncomingMessage(message) {
  if (!message.id || processedMessageIds.has(message.id)) return;
  processedMessageIds.add(message.id);
  if (processedMessageIds.size > 2000) processedMessageIds.clear();

  const phone = message.from;
  const text = extractText(message).trim();

  if (!text) {
    await sendWhatsAppText(
      phone,
      "Por enquanto consigo atender pedidos por texto. Me diga o que você gostaria de pedir 🍕"
    );
    return;
  }

  const reply = await getAssistantReply({ phone, text, notifyOwner });
  await sendWhatsAppText(phone, reply);
}

async function processWebhook(payload) {
  for (const message of inboundMessages(payload)) {
    try {
      await handleIncomingMessage(message);
    } catch (error) {
      console.error("Erro processando mensagem:", error);
      if (message.from) {
        try {
          await sendWhatsAppText(
            message.from,
            "Tive um probleminha aqui no atendimento automático. Vou deixar sinalizado para um atendente continuar com você. 🍕"
          );
          await notifyOwner({ handoff: true, customerPhone: message.from, reason: String(error.message || error) });
        } catch (secondaryError) {
          console.error("Erro no fallback:", secondaryError);
        }
      }
    }
  }
}

app.get("/health", (_req, res) => {
  res.json({ ok: true, service: "forno-e-arte-whatsapp-bot" });
});

app.get("/webhook", (req, res) => {
  const mode = req.query["hub.mode"];
  const token = req.query["hub.verify_token"];
  const challenge = req.query["hub.challenge"];

  if (mode === "subscribe" && token && token === process.env.WHATSAPP_VERIFY_TOKEN) {
    return res.status(200).send(challenge);
  }
  return res.sendStatus(403);
});

app.post("/webhook", express.raw({ type: "application/json", limit: "1mb" }), (req, res) => {
  if (!verifyMetaSignature(req.body, req.get("x-hub-signature-256"))) {
    return res.sendStatus(401);
  }

  let payload;
  try {
    payload = JSON.parse(req.body.toString("utf8"));
  } catch {
    return res.sendStatus(400);
  }

  res.sendStatus(200);
  setImmediate(() => processWebhook(payload).catch((error) => console.error("Webhook assíncrono:", error)));
});

app.use(express.json());

function requireAdmin(req, res, next) {
  const configured = process.env.ADMIN_API_TOKEN;
  if (!configured) return res.status(503).json({ error: "ADMIN_API_TOKEN não configurado" });
  const token = req.get("authorization")?.replace(/^Bearer\s+/i, "");
  if (token !== configured) return res.sendStatus(401);
  next();
}

app.get("/api/orders", requireAdmin, (_req, res) => {
  res.json({ orders: listOrders() });
});

app.get("/api/orders/:id", requireAdmin, (req, res) => {
  const order = getOrder(req.params.id);
  if (!order) return res.sendStatus(404);
  res.json({ order });
});

app.delete("/api/sessions/:phone", requireAdmin, (req, res) => {
  clearSession(req.params.phone);
  res.sendStatus(204);
});

app.listen(port, () => {
  console.log(`Forno e Arte WhatsApp bot ouvindo na porta ${port}`);
});
