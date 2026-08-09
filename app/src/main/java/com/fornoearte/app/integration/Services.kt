package com.fornoearte.app.integration

import com.fornoearte.app.data.OrderEntity
import com.fornoearte.app.data.PaymentMethod
import kotlinx.coroutines.delay

data class PaymentRequest(val orderId: Long, val amount: Double, val method: PaymentMethod)
sealed interface PaymentResult { data class Success(val reference: String, val paymentUrl: String? = null) : PaymentResult; data class Error(val message: String) : PaymentResult }

/** Contrato desacoplado para uma futura implementação oficial da InfinityPay. Nunca armazene chaves no app. */
interface PaymentGateway { suspend fun createPayment(request: PaymentRequest): PaymentResult }

class SimulatedInfinityPayGateway : PaymentGateway {
    override suspend fun createPayment(request: PaymentRequest): PaymentResult {
        delay(700)
        return PaymentResult.Success("SIM-${request.orderId}-${System.currentTimeMillis()}", if (request.method == PaymentMethod.PAYMENT_LINK) "https://exemplo.invalid/pagar/${request.orderId}" else null)
    }
}

data class InvoiceResult(val externalId: String)
/** Ponto de extensão para emissão fiscal no servidor, onde credenciais devem permanecer protegidas. */
interface FiscalInvoiceService { suspend fun issue(order: OrderEntity): Result<InvoiceResult> }

class UnconfiguredFiscalInvoiceService : FiscalInvoiceService {
    override suspend fun issue(order: OrderEntity) = Result.failure<InvoiceResult>(IllegalStateException("Configure uma API fiscal segura no servidor"))
}
