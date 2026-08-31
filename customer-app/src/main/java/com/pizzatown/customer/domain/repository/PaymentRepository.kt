package com.pizzatown.customer.domain.repository

import com.pizzatown.customer.domain.model.CashfreePaymentSession
import com.pizzatown.customer.domain.model.PaymentVerificationResult

interface PaymentRepository {
    /** Asks the backend to create a Cashfree order for an already-saved
     *  Firestore order (paymentMethod = ONLINE, paymentStatus = PENDING)
     *  and returns the payment session needed to open Cashfree checkout.
     *  Safe to call again for the same order (e.g. retry after a network
     *  error) — the backend reuses/re-activates the existing session
     *  rather than double-charging. */
    suspend fun createCashfreeOrder(orderId: String): Result<CashfreePaymentSession>

    /** Asks the backend to fetch the *real* payment status for this order
     *  directly from Cashfree and to update Firestore accordingly. Never
     *  trust the Android SDK's local callback as the final word — always
     *  go through this after the checkout screen closes. */
    suspend fun abandonCashfreeOrder(orderId: String): Result<Unit>

    suspend fun verifyCashfreePayment(orderId: String): Result<PaymentVerificationResult>
}
