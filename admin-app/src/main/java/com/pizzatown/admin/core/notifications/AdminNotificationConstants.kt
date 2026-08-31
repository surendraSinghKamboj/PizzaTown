package com.pizzatown.admin.core.notifications

/**
 * Single source of truth for the admin-side FCM data-payload keys and
 * the topic every admin device subscribes to. The Cloud Function
 * onOrderCreated in /functions/index.js sends to this exact topic and
 * these exact keys — keep both sides in sync if you change either.
 */
object AdminNotificationConstants {
    const val CHANNEL_ID = "pizzatown_admin_orders"
    const val CHANNEL_NAME = "New orders"
    const val CHANNEL_DESCRIPTION = "Alerts you the moment a customer places a new order."

    /** Every signed-in admin device subscribes to this — supports more than one admin phone/tablet. */
    const val ADMIN_TOPIC = "admin_all"

    const val KEY_TYPE = "type"
    const val KEY_TITLE = "title"
    const val KEY_BODY = "body"
    const val KEY_ORDER_ID = "orderId"

    const val TYPE_NEW_ORDER = "NEW_ORDER"
}
