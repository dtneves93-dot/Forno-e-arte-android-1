import OpenAI from "openai";
import { MENU, calculateOrder } from "./menu.js";
import { createOrder } from "./orderStore.js";

const openai = new OpenAI({ apiKey: process.env.OPENAI_API_KEY });
const sessions = new Map();

const tools = [
  {
    type: "function",
    name: "get_menu",
    description: "Consulta o cardápio e as regras comerciais oficiais da Forno e Arte. Use antes de responder sobre sabores, preços, tamanho, refrigerantes, pagamento ou entrega.",
    strict: true,
    parameters: {
      type: "object",
      properties: {},
      required: [],
      additionalProperties: false
    }
  },
  {
    type: "function",
    name: "calculate_order",
    description: "Calcula o pedido usando somente preços cadastrados no sistema. Nunca faça conta de preço por conta própria quando esta ferramenta puder ser usada.",
    strict: true,
    parameters: {
      type: "object",
      properties: {
        items: {
          type: "array",
          minItems: 1,
          items: {
            type: "object",
            properties: {
              flavor: {
                type: "string",
                enum: ["Mussarela", "Calabresa", "Queijo com presunto", "Portuguesa"]
              },
              quantity: { type: "integer", minimum: 1, maximum: 20 },
              drink_1_5l: { type: "boolean" }
            },
            required: ["flavor", "quantity", "drink_1_5l"],
            additionalProperties: false
          }
        }
      },
      required: ["items"],
      additionalProperties: false
    }
  },
  {
    type: "function",
    name: "create_order",
    description: "Registra um pedido somente depois que o cliente confirmou o que quer, informou nome, endereço e forma de pagamento. O preço é recalculado pelo servidor.",
    strict: true,
    parameters: {
      type: "object",
      properties: {
        customerName: { type: "string", minLength: 2 },
        address: { type: "string", minLength: 5 },
        reference: { type: ["string", "null"] },
        paymentMethod: {
          type: "string",
          enum: ["Pix", "Cartão", "Dinheiro", "Link de pagamento"]
        },
        items: {
          type: "array",
          minItems: 1,
          items: {
            type: "object",
            properties: {
              flavor: {
                type: "string",
                enum: ["Mussarela", "Calabresa", "Queijo com presunto", "Portuguesa"]
              },
              quantity: { type: "integer", minimum: 1, maximum: 20 },
              drink_1_5l: { type: "boolean" }
            },
            required: ["flavor", "quantity", "drink_1_5l"],
            additionalProperties: false
          }
        },
        notes: { type: ["string", "null"] }
      },
      required: ["customerName", "address", "reference", "paymentMethod", "items", "notes"],
      additionalProperties: false
    }
  },
  {
    type: "function",
    name: "handoff_to_human",
    description: "Solicita atendimento humano quando o cliente pedir uma pessoa, houver reclamação, dúvida de entrega/taxa que o sistema não saiba responder, alteração fora do cardápio ou qualquer situação que exija decisão humana.",
    strict: true,
    parameters: {
      type: "object",
      properties: {
        reason: { type: "string", minLength: 3 }
      },
      required: ["reason"],
      additionalProperties: false
    }
  }
];

function buildInstructions(firstTurn) {
  return `Você é o atendente virtual de vendas da Forno e Arte Pizzaria no WhatsApp.

Objetivo: conversar em português do Brasil de forma natural, simpática e curta, entender o pedido e levar o cliente até a confirmação. Você pode usar humor leve, mas não seja insistente.

${firstTurn ? "Esta é a primeira resposta desta conversa. Diga de forma breve que você é o assistente virtual da Forno e Arte e já comece a ajudar." : "A conversa já começou; não repita sua apresentação como assistente virtual."}

Regras obrigatórias:
- Nunca invente sabor, preço, promoção, taxa de entrega, prazo, disponibilidade ou área atendida.
- Para cardápio e preço, use as ferramentas.
- Pizza atual: 35 cm. Sabores cadastrados: mussarela, calabresa, queijo com presunto e portuguesa.
- O preço da pizza avulsa é R$ 36,00.
- Os refrigerantes atuais são de 1,5 L, incluindo Pepsi, conforme disponibilidade.
- O preço atualizado do combo com refrigerante pode ainda não estar configurado; se a ferramenta indicar preço pendente, não invente valor e peça confirmação humana.
- Endereço da pizzaria: Rua Urucuia, 449. Telefone: (21) 97688-7803.
- Formas cadastradas no app: Pix, cartão, dinheiro e link de pagamento.
- Antes de criar pedido, tenha nome, endereço, itens e forma de pagamento, recapitule e obtenha uma confirmação clara do cliente.
- Depois de criar o pedido, informe o número do pedido e o resumo. Se preço/taxa estiver pendente, deixe isso explícito.
- Se o cliente pedir uma pessoa ou houver situação fora das regras, use handoff_to_human.
- Tente oferecer refrigerante de 1,5 L uma vez quando fizer sentido, sem pressionar.
- Não diga que é humano e não invente uma identidade pessoal.
- Prefira mensagens de 1 a 4 frases, como um bom atendente de WhatsApp.`;
}

function getHistory(phone) {
  return sessions.get(phone) || [];
}

function saveTurn(phone, userText, assistantText) {
  const history = getHistory(phone);
  history.push({ role: "user", content: userText });
  history.push({ role: "assistant", content: assistantText });
  sessions.set(phone, history.slice(-12));
}

function safeArgs(raw) {
  try {
    return JSON.parse(raw || "{}");
  } catch {
    return {};
  }
}

async function executeTool(call, phone, notifyOwner) {
  const args = safeArgs(call.arguments);

  if (call.name === "get_menu") {
    return { ok: true, menu: MENU };
  }

  if (call.name === "calculate_order") {
    return calculateOrder(args.items || []);
  }

  if (call.name === "create_order") {
    const calculation = calculateOrder(args.items || []);
    if (!calculation.ok) return calculation;

    const order = createOrder({
      customerPhone: phone,
      customerName: args.customerName.trim(),
      address: args.address.trim(),
      reference: args.reference?.trim() || null,
      paymentMethod: args.paymentMethod,
      items: calculation.items,
      pizzaCount: calculation.pizzaCount,
      subtotal: calculation.subtotal,
      total: calculation.total,
      pricePending: calculation.pricePending,
      deliveryFee: null,
      deliveryFeePending: true,
      notes: args.notes?.trim() || null
    });

    if (notifyOwner) await notifyOwner(order);

    return {
      ok: true,
      order: {
        number: order.number,
        status: order.status,
        items: order.items,
        subtotal: order.subtotal,
        total: order.total,
        pricePending: order.pricePending,
        deliveryFeePending: order.deliveryFeePending,
        paymentMethod: order.paymentMethod
      }
    };
  }

  if (call.name === "handoff_to_human") {
    if (notifyOwner) {
      await notifyOwner({
        handoff: true,
        customerPhone: phone,
        reason: args.reason || "Atendimento humano solicitado"
      });
    }
    return {
      ok: true,
      handoffRequested: true,
      message: "Um atendente humano foi solicitado. Não prometa tempo exato de resposta."
    };
  }

  return { ok: false, error: "Ferramenta desconhecida" };
}

export async function getAssistantReply({ phone, text, notifyOwner }) {
  if (!process.env.OPENAI_API_KEY) {
    throw new Error("OPENAI_API_KEY não configurada");
  }

  const history = getHistory(phone);
  let input = [...history, { role: "user", content: text }];
  let response;

  for (let step = 0; step < 5; step += 1) {
    response = await openai.responses.create({
      model: process.env.OPENAI_MODEL || "gpt-5",
      instructions: buildInstructions(history.length === 0),
      input,
      tools,
      tool_choice: "auto",
      store: false
    });

    const calls = response.output.filter((item) => item.type === "function_call");
    if (calls.length === 0) {
      const reply = response.output_text?.trim() || "Posso te ajudar a montar seu pedido 🍕";
      saveTurn(phone, text, reply);
      return reply;
    }

    input = [...input, ...response.output];
    for (const call of calls) {
      const result = await executeTool(call, phone, notifyOwner);
      input.push({
        type: "function_call_output",
        call_id: call.call_id,
        output: JSON.stringify(result)
      });
    }
  }

  const fallback = "Consegui entender seu pedido, mas preciso passar esta parte para um atendente. Já vou sinalizar aqui. 🍕";
  saveTurn(phone, text, fallback);
  if (notifyOwner) await notifyOwner({ handoff: true, customerPhone: phone, reason: "Limite de etapas do chatbot" });
  return fallback;
}

export function clearSession(phone) {
  sessions.delete(phone);
}
