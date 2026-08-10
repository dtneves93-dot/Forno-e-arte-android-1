# Forno e Arte — Chatbot do WhatsApp

Backend do atendente virtual da Forno e Arte. Ele recebe mensagens pela WhatsApp Business Cloud API, usa a OpenAI Responses API para conversar e chama funções do servidor para consultar cardápio, calcular valores, registrar pedidos e pedir atendimento humano.

## O que já funciona

- Verificação do webhook da Meta (`GET /webhook`).
- Validação de assinatura `x-hub-signature-256` nas mensagens recebidas.
- Recebimento de texto, botões e respostas de listas do WhatsApp.
- Respostas naturais em português usando OpenAI.
- Cardápio controlado pelo servidor; a IA é instruída a não inventar sabores ou preços.
- Pedido de 35 cm por R$ 36,00 com sabores: mussarela, calabresa, queijo com presunto e portuguesa.
- Refrigerantes de 1,5 L, incluindo Pepsi, sujeitos à disponibilidade.
- Coleta de nome, endereço, forma de pagamento e itens antes do fechamento.
- Criação de pedido e alerta opcional para um número do responsável.
- Transferência lógica para atendimento humano quando necessário.
- Endpoint protegido `GET /api/orders` para o aplicativo consumir na próxima etapa.

## Limitações desta primeira versão

1. Os pedidos e sessões ficam em memória. Reiniciar o servidor apaga esses dados. A próxima etapa é persistir em banco e sincronizar com o app Android.
2. O valor atualizado do combo pizza + refrigerante 1,5 L ainda não foi confirmado. Enquanto `PIZZA_DRINK_COMBO_PRICE=0`, o bot nunca inventa esse preço e marca a confirmação como pendente.
3. Taxa e área de entrega ainda não foram cadastradas, então o bot não promete valores ou bairros de atendimento.
4. Áudio, imagem e localização ainda recebem uma resposta pedindo texto. Podem ser adicionados depois.

## Variáveis de ambiente

Copie `.env.example` para `.env` apenas no ambiente local. Nunca envie `.env` ao GitHub.

Obrigatórias para produção:

- `WHATSAPP_VERIFY_TOKEN`: token escolhido por você para verificar o webhook.
- `WHATSAPP_ACCESS_TOKEN`: token da WhatsApp Business Platform.
- `WHATSAPP_PHONE_NUMBER_ID`: ID do número registrado na Cloud API.
- `META_APP_SECRET`: segredo do aplicativo Meta, usado para validar assinatura do webhook.
- `OPENAI_API_KEY`: chave da OpenAI, somente no servidor.
- `ADMIN_API_TOKEN`: token criado por você para proteger `/api/orders`.

Opcionais:

- `OPENAI_MODEL`: padrão `gpt-5`.
- `GRAPH_API_VERSION`: padrão `v23.0`; pode ser trocado sem alterar código.
- `OWNER_ALERT_NUMBER`: número pessoal que receberá alerta de novo pedido ou solicitação de atendimento humano.
- `PIZZA_DRINK_COMBO_PRICE`: preço do combo atualizado; deixe `0` até o valor ser confirmado.

## Executar localmente

```bash
cd backend
npm install
cp .env.example .env
npm run check
npm start
```

A aplicação sobe em `http://localhost:3000` e possui `GET /health`.

## Conectar à Meta

No painel do aplicativo Meta/WhatsApp Business Platform:

1. Configure a URL pública do callback como `https://SEU-DOMINIO/webhook`.
2. Use exatamente o mesmo valor de `WHATSAPP_VERIFY_TOKEN` no painel e no servidor.
3. Assine o campo de mensagens da conta do WhatsApp Business.
4. Configure `WHATSAPP_ACCESS_TOKEN`, `WHATSAPP_PHONE_NUMBER_ID` e `META_APP_SECRET` no servidor.
5. Faça um teste enviando uma mensagem para o número conectado.

O envio de respostas usa `/{PHONE_NUMBER_ID}/messages` da Graph API.

## Fluxo de venda

Exemplo esperado:

- Cliente: `Boa noite, quero uma pizza.`
- Bot: apresenta-se brevemente como assistente virtual e pergunta o sabor.
- Cliente: `Calabresa.`
- Bot: informa o valor cadastrado, oferece refrigerante uma vez e continua o pedido.
- Bot coleta nome, endereço e pagamento.
- Bot recapitula e pergunta se pode confirmar.
- Cliente confirma.
- O servidor executa `create_order`, devolve o número do pedido e opcionalmente alerta o responsável.

## Próximas etapas

- Persistir pedidos em banco de dados.
- Fazer o app Android ler e atualizar os pedidos do chatbot.
- Enviar automaticamente os status `recebido`, `em preparo`, `saiu para entrega` e `entregue`.
- Conectar InfinityPay pelo backend.
- Integrar emissão fiscal pelo backend.
- Criar painel para assumir uma conversa manualmente.
