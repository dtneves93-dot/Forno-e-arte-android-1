package com.fornoearte.app.util

import android.content.*
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.fornoearte.app.data.OrderEntity
import com.fornoearte.app.data.OrderStatus
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private val money = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

fun whatsappMessage(order: OrderEntity, status: OrderStatus) = when (status) {
    OrderStatus.RECEIVED -> "Olá, ${order.customerName}! Recebemos seu pedido #${order.id} na Forno e Arte. Em breve iniciaremos o preparo."
    OrderStatus.PREPARING -> "Olá, ${order.customerName}! Seu pedido #${order.id} já está sendo preparado com todo carinho. 🍕"
    OrderStatus.OUT_FOR_DELIVERY -> "Olá, ${order.customerName}! Seu pedido #${order.id} saiu para entrega. Prepare-se para saborear! 🛵"
    OrderStatus.DELIVERED -> "Olá, ${order.customerName}! O pedido #${order.id} foi entregue. Obrigado por escolher a Forno e Arte! ❤️"
}

fun openWhatsApp(context: Context, phone: String, message: String) {
    val digits = phone.filter(Char::isDigit).let { if (it.startsWith("55")) it else "55$it" }
    val uri = Uri.parse("https://wa.me/$digits?text=${Uri.encode(message)}")
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        .onFailure { context.startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage(null)) }
}

fun shareReceiptPdf(context: Context, order: OrderEntity) {
    val document = PdfDocument(); val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
    val canvas = page.canvas; val paint = Paint(Paint.ANTI_ALIAS_FLAG); var y = 70f
    fun line(text: String, bold: Boolean = false, size: Float = 15f) { paint.textSize = size; paint.isFakeBoldText = bold; canvas.drawText(text.take(75), 48f, y, paint); y += size + 14 }
    line("FORNO E ARTE", true, 28f); line("Resumo do pedido #${order.id}", true, 20f)
    line(SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR")).format(Date(order.createdAt)))
    y += 12; line("Cliente: ${order.customerName}", true); line("Telefone: ${order.phone}"); line("Endereço: ${order.address}")
    y += 12; line("Itens: ${order.items}", true); line("Sabores: ${order.flavors}"); line("Tamanho: ${order.size}  |  Quantidade: ${order.quantity}")
    if (order.extras.isNotBlank()) line("Adicionais: ${order.extras}"); if (order.notes.isNotBlank()) line("Observações: ${order.notes}")
    y += 12; line("Subtotal: ${money.format(order.unitPrice * order.quantity)}"); line("Entrega: ${money.format(order.deliveryFee)}"); line("Desconto: - ${money.format(order.discount)}")
    line("TOTAL: ${money.format(order.total)}", true, 22f); line("Pagamento: ${order.paymentMethod.label}"); line("Status: ${order.status.label}")
    document.finishPage(page)
    val dir = File(context.cacheDir, "receipts").apply { mkdirs() }; val file = File(dir, "pedido-${order.id}.pdf")
    file.outputStream().use(document::writeTo); document.close()
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Compartilhar comprovante"))
}
