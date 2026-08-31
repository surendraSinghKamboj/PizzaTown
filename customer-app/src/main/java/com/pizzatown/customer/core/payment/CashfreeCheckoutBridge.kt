package com.pizzatown.customer.core.payment

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * The Cashfree Android SDK requires an Activity to host its checkout
 * callback ([com.cashfree.pg.core.api.callback.CFCheckoutResponseCallback])
 * and to call `doPayment()` from — but the app's checkout flow/state lives
 * in [com.pizzatown.customer.presentation.checkout.CheckoutViewModel], not
 * in an Activity. This singleton bridges the two without coupling the
 * ViewModel to Android's Activity APIs:
 *
 *   CheckoutViewModel --launchRequests--> MainActivity --doPayment()--> Cashfree SDK
 *   Cashfree SDK --callback--> MainActivity --outcomes--> CheckoutViewModel
 */
object CashfreeCheckoutBridge {

    data class LaunchRequest(val orderId: String, val paymentSessionId: String)

    sealed class Outcome(open val orderId: String) {
        /** Cashfree SDK finished the checkout journey — the *actual* result
         *  (paid/failed/pending) must still be confirmed server-side. */
        data class Verify(override val orderId: String) : Outcome(orderId)

        /** SDK reported an error or the user cancelled/backed out. Even so,
         *  the app still verifies server-side rather than trusting this. */
        data class Failure(override val orderId: String, val message: String) : Outcome(orderId)
    }

    private val _launchRequests = MutableSharedFlow<LaunchRequest>(extraBufferCapacity = 1)
    val launchRequests: SharedFlow<LaunchRequest> = _launchRequests

    private val _outcomes = MutableSharedFlow<Outcome>(extraBufferCapacity = 1)
    val outcomes: SharedFlow<Outcome> = _outcomes

    suspend fun requestCheckout(orderId: String, paymentSessionId: String) {
        _launchRequests.emit(LaunchRequest(orderId, paymentSessionId))
    }

    suspend fun reportVerify(orderId: String) {
        _outcomes.emit(Outcome.Verify(orderId))
    }

    suspend fun reportFailure(orderId: String, message: String) {
        _outcomes.emit(Outcome.Failure(orderId, message))
    }
}
