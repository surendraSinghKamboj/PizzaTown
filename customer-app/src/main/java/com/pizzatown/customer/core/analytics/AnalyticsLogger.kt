package com.pizzatown.customer.core.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.pizzatown.customer.domain.model.CartItem
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Firebase Analytics with PizzaTown's e-commerce events, using
 * Google's standard GA4 e-commerce event/param names wherever one exists
 * (view_item, add_to_cart, begin_checkout, purchase, ...). Sticking to
 * the standard names means these show up pre-built in the Firebase
 * console's "Monetization"/"E-commerce purchases" reports and export
 * cleanly to BigQuery — no custom dashboard needed to see revenue,
 * funnels, or drop-off by step.
 *
 * Uses the plain FirebaseAnalytics.logEvent(String, Bundle) API rather
 * than the KTX DSL, since that surface has been stable across every
 * Firebase BOM version — no risk of a KTX artifact/version mismatch
 * breaking the build.
 *
 * Every method is a thin, best-effort call — analytics must never be
 * able to crash or slow down a real user action like adding to cart.
 */
@Singleton
class AnalyticsLogger @Inject constructor(
    @ApplicationContext context: Context
) {
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)

    private fun CartItem.toItemBundle(): Bundle = Bundle().apply {
        putString(FirebaseAnalytics.Param.ITEM_ID, menuItemId)
        putString(FirebaseAnalytics.Param.ITEM_NAME, menuItemName)
        selectedVariantName?.let { putString(FirebaseAnalytics.Param.ITEM_VARIANT, it) }
        putDouble(FirebaseAnalytics.Param.PRICE, finalUnitPrice)
        putLong(FirebaseAnalytics.Param.QUANTITY, quantity.toLong())
    }

    fun logViewItem(itemId: String, itemName: String, categoryId: String?, price: Double) {
        val itemBundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, itemId)
            putString(FirebaseAnalytics.Param.ITEM_NAME, itemName)
            categoryId?.let { putString(FirebaseAnalytics.Param.ITEM_CATEGORY, it) }
            putDouble(FirebaseAnalytics.Param.PRICE, price)
        }
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.CURRENCY, "INR")
            putDouble(FirebaseAnalytics.Param.VALUE, price)
            putParcelableArray(FirebaseAnalytics.Param.ITEMS, arrayOf(itemBundle))
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM, bundle)
    }

    fun logAddToCart(item: CartItem) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.CURRENCY, "INR")
            putDouble(FirebaseAnalytics.Param.VALUE, item.lineTotal)
            putParcelableArray(FirebaseAnalytics.Param.ITEMS, arrayOf(item.toItemBundle()))
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.ADD_TO_CART, bundle)
    }

    fun logRemoveFromCart(item: CartItem) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.CURRENCY, "INR")
            putDouble(FirebaseAnalytics.Param.VALUE, item.lineTotal)
            putParcelableArray(FirebaseAnalytics.Param.ITEMS, arrayOf(item.toItemBundle()))
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.REMOVE_FROM_CART, bundle)
    }

    fun logBeginCheckout(items: List<CartItem>, subtotal: Double) {
        if (items.isEmpty()) return
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.CURRENCY, "INR")
            putDouble(FirebaseAnalytics.Param.VALUE, subtotal)
            putParcelableArray(FirebaseAnalytics.Param.ITEMS, items.map { it.toItemBundle() }.toTypedArray())
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.BEGIN_CHECKOUT, bundle)
    }

    /** GA4 has no dedicated "coupon applied" event; select_promotion is the closest standard fit. */
    fun logApplyCoupon(code: String, discountAmount: Double) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.PROMOTION_ID, code)
            putString(FirebaseAnalytics.Param.PROMOTION_NAME, code)
            putDouble("discount_amount", discountAmount)
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_PROMOTION, bundle)
    }

    fun logPurchase(orderId: String, items: List<CartItem>, subtotal: Double, discount: Double, grandTotal: Double, couponCode: String?) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.TRANSACTION_ID, orderId)
            putString(FirebaseAnalytics.Param.CURRENCY, "INR")
            putDouble(FirebaseAnalytics.Param.VALUE, grandTotal)
            putDouble(FirebaseAnalytics.Param.SHIPPING, 0.0)
            putDouble(FirebaseAnalytics.Param.TAX, 0.0)
            if (!couponCode.isNullOrBlank()) putString(FirebaseAnalytics.Param.COUPON, couponCode)
            putParcelableArray(FirebaseAnalytics.Param.ITEMS, items.map { it.toItemBundle() }.toTypedArray())
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.PURCHASE, bundle)
    }

    fun logLogin(method: String = "email") {
        val bundle = Bundle().apply { putString(FirebaseAnalytics.Param.METHOD, method) }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, bundle)
    }

    fun logSignUp(method: String = "email") {
        val bundle = Bundle().apply { putString(FirebaseAnalytics.Param.METHOD, method) }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, bundle)
    }

    /** Ties every subsequent event on this device to the signed-in user for cross-session analysis. */
    fun setUserId(userId: String?) {
        firebaseAnalytics.setUserId(userId)
    }
}
