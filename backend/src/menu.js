const comboPrice = Number(process.env.PIZZA_DRINK_COMBO_PRICE || 0);

export const MENU = {
  business: {
    name: "Forno e Arte Pizzaria",
    phone: "(21) 97688-7803",
    address: "Rua Urucuia, 449"
  },
  pizzas: {
    sizeCm: 35,
    unitPrice: 36,
    flavors: [
      "Mussarela",
      "Calabresa",
      "Queijo com presunto",
      "Portuguesa"
    ]
  },
  softDrinks: {
    sizeLiters: 1.5,
    note: "Refrigerantes de 1,5 L disponíveis, incluindo Pepsi, sujeitos à disponibilidade.",
    pizzaDrinkComboPrice: comboPrice > 0 ? comboPrice : null
  },
  payments: ["Pix", "Cartão", "Dinheiro", "Link de pagamento"],
  delivery: {
    fee: null,
    area: null,
    note: "Taxa e área de entrega precisam ser confirmadas antes de prometer valor final ao cliente."
  }
};

export function normalizeFlavor(value = "") {
  const normalized = value.trim().toLocaleLowerCase("pt-BR");
  return MENU.pizzas.flavors.find(
    (flavor) => flavor.toLocaleLowerCase("pt-BR") === normalized
  ) || null;
}

export function calculateOrder(items = []) {
  let pizzaCount = 0;
  let drinkComboCount = 0;
  const normalizedItems = [];

  for (const item of items) {
    const flavor = normalizeFlavor(item.flavor);
    const quantity = Math.max(1, Number(item.quantity || 1));
    if (!flavor || !Number.isFinite(quantity)) {
      return { ok: false, error: `Item inválido: ${item.flavor || "sem sabor"}` };
    }

    pizzaCount += quantity;
    if (item.drink_1_5l) drinkComboCount += quantity;
    normalizedItems.push({ flavor, quantity, drink_1_5l: Boolean(item.drink_1_5l) });
  }

  const baseSubtotal = pizzaCount * MENU.pizzas.unitPrice;
  const comboPrice = MENU.softDrinks.pizzaDrinkComboPrice;

  if (drinkComboCount > 0 && !comboPrice) {
    return {
      ok: true,
      items: normalizedItems,
      pizzaCount,
      subtotal: baseSubtotal,
      total: null,
      pricePending: true,
      note: "O preço atualizado do combo com refrigerante 1,5 L ainda não foi configurado. Não invente o valor; confirme com um atendente."
    };
  }

  const withoutDrinkCount = pizzaCount - drinkComboCount;
  const total = withoutDrinkCount * MENU.pizzas.unitPrice + drinkComboCount * comboPrice;

  return {
    ok: true,
    items: normalizedItems,
    pizzaCount,
    subtotal: total,
    total,
    pricePending: false
  };
}
