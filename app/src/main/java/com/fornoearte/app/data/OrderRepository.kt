package com.fornoearte.app.data

class OrderRepository(private val dao: OrderDao) {
    fun orders(query: String = "") = if (query.isBlank()) dao.observeAll() else dao.search(query.trim())
    suspend fun save(order: OrderEntity) = if (order.id == 0L) dao.insert(order) else { dao.update(order); order.id }
    suspend fun delete(order: OrderEntity) = dao.delete(order)
}
