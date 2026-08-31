package com.pizzatown.customer.core.notifications

/**
 * Single source of truth for the FCM data-payload keys. The Cloud
 * Functions in /functions/index.js send messages using these exact
 * keys — if you rename one here, update it there too.
 */
object NotificationConstants {
    const val CHANNEL_ID = "pizzatown_updates"
    const val CHANNEL_NAME = "Order & offer updates"
    const val CHANNEL_DESCRIPTION =
        "Order status changes, coupons made just for you, and announcements from Pizza Town."

    /** Every customer app instance subscribes to this so admin broadcasts reach everyone. */
    const val BROADCAST_TOPIC = "customers_all"

    // Data payload keys (all messages from the backend are data-only, see functions/index.js)
    const val KEY_TYPE = "type"
    const val KEY_TITLE = "title"
    const val KEY_BODY = "body"
    const val KEY_ORDER_ID = "orderId"
    const val KEY_COUPON_CODE = "couponCode"

    const val TYPE_ORDER_STATUS = "ORDER_STATUS"
    const val TYPE_COUPON = "COUPON"
    const val TYPE_BROADCAST = "BROADCAST"
}
