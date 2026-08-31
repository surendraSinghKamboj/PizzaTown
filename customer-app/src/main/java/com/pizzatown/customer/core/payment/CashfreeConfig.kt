package com.pizzatown.customer.core.payment

import com.cashfree.pg.core.api.CFSession

/**
 * Only thing the Android app needs to know about Cashfree: which
 * environment to point the SDK at. The App ID and Secret Key are never
 * present here or anywhere else in the APK — order creation and payment
 * verification happen entirely in Cloud Functions using the secret key.
 *
 * IMPORTANT: flip this to [CFSession.Environment.PRODUCTION] only when
 * you also switch the Cloud Functions secrets (CASHFREE_APP_ID /
 * CASHFREE_SECRET_KEY) to your live/production credentials — see
 * functions/index.js and the README "Switching to production" section.
 * Keeping this and the backend environment in sync is the only thing
 * that matters; there is no code path here that needs to change besides
 * this one constant.
 */
object CashfreeConfig {
    val environment: CFSession.Environment = CFSession.Environment.SANDBOX
}
