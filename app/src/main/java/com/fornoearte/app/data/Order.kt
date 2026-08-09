package com.fornoearte.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class OrderStatus(val label: String) { RECEIVED("Pedido recebido"), PREPARING("Em preparo"), OUT_FOR_DELIVERY("Saiu para entrega"), DELIVERED("Entregue") }
enum class PaymentMethod(val label: String) { PIX("Pix"), CARD("Cartão"), CASH("Dinheiro"), PAYMENT_LINK("Link de pagamento") }

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerName: String,
    val phone: String,
    val address: String,
    val items: String,
    val flavors: String,
    val size: String,
    val quantity: Int,
    val extras: String,
    val notes: String,
    val unitPrice: Double,
    val deliveryFee: Double,
    val discount: Double,
    val paymentMethod: PaymentMethod,
    val status: OrderStatus = OrderStatus.RECEIVED,
    val createdAt: Long = System.currentTimeMillis()
) { val total: Double get() = (unitPrice * quantity + deliveryFee - discount).coerceAtLeast(0.0) }
