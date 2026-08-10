import { randomUUID } from "node:crypto";

const orders = [];

export function createOrder(data) {
  const order = {
    id: randomUUID(),
    number: orders.length + 1,
    status: "RECEIVED",
    createdAt: new Date().toISOString(),
    ...data
  };
  orders.unshift(order);
  return order;
}

export function listOrders() {
  return orders.map((order) => ({ ...order }));
}

export function getOrder(id) {
  const order = orders.find((item) => item.id === id || String(item.number) === String(id));
  return order ? { ...order } : null;
}
