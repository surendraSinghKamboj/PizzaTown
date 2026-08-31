package com.pizzatown.customer.domain.model

/** Returned by the createCashfreeOrder Cloud Function — everything the
 *  Cashfree Android SDK needs to open the checkout screen. */
data class CashfreePaymentSession(
    val orderId: String,
    val paymentSessionId: String,
    val cashfreeOrderId: String
)

/** Returned by the verifyCashfreePayment Cloud Function — the *server's*
 *  confirmed view of the payment, fetched directly from Cashfree. Never
 *  derived from the Android SDK's callback alone. */
data class PaymentVerificationResult(
    val paymentStatus: PaymentStatus,
    val cashfreePaymentId: String?
)
