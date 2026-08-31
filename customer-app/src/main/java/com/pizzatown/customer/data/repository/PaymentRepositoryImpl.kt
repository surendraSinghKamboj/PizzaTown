package com.pizzatown.customer.data.repository

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.functions.FirebaseFunctions
import com.pizzatown.customer.domain.model.CashfreePaymentSession
import com.pizzatown.customer.domain.model.PaymentStatus
import com.pizzatown.customer.domain.model.PaymentVerificationResult
import com.pizzatown.customer.domain.repository.PaymentRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * All Cashfree order-creation and payment-verification logic lives in
 * Cloud Functions (functions/index.js: createCashfreeOrder,
 * verifyCashfreePayment) — this class only calls those two callables.
 * The Cashfree secret key never reaches this app.
 */
class PaymentRepositoryImpl @Inject constructor(
    private val functions: FirebaseFunctions
) : PaymentRepository {

    override suspend fun createCashfreeOrder(orderId: String): Result<CashfreePaymentSession> = runCatching {
        val result = functions.getHttpsCallable("createCashfreeOrder")
            .call(mapOf("orderId" to orderId))
            .await()

        @Suppress("UNCHECKED_CAST")
        val data = result.getData() as? Map<String, Any?> ?: error("Empty response from createCashfreeOrder")

        CashfreePaymentSession(
            orderId = data["orderId"] as? String ?: orderId,
            paymentSessionId = data["paymentSessionId"] as? String ?: error("Missing paymentSessionId"),
            cashfreeOrderId = data["cashfreeOrderId"] as? String ?: ""
        )
    }.onFailure {
        FirebaseCrashlytics.getInstance().recordException(it)
    }

    override suspend fun abandonCashfreeOrder(orderId: String): Result<Unit> =
        runCatching {
            functions.getHttpsCallable("abandonCashfreeOrder")
                .call(mapOf("orderId" to orderId))
                .await()
            Unit
        }.onFailure {
            FirebaseCrashlytics.getInstance().recordException(it)
        }

    override suspend fun verifyCashfreePayment(orderId: String): Result<PaymentVerificationResult> = runCatching {
        val result = functions.getHttpsCallable("verifyCashfreePayment")
            .call(mapOf("orderId" to orderId))
            .await()

        @Suppress("UNCHECKED_CAST")
        val data = result.getData() as? Map<String, Any?> ?: error("Empty response from verifyCashfreePayment")

        val statusString = data["paymentStatus"] as? String ?: "PENDING"
        PaymentVerificationResult(
            paymentStatus = runCatching { PaymentStatus.valueOf(statusString) }.getOrDefault(PaymentStatus.PENDING),
            cashfreePaymentId = data["cashfreePaymentId"] as? String
        )
    }.onFailure {
        FirebaseCrashlytics.getInstance().recordException(it)
    }
}
