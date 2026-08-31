package com.pizzatown.admin.core.firebase

/** Single source of truth for Firestore collection names, shared in spirit with the customer app. */
object FirestoreCollections {
    const val USERS = "users"
    const val CATEGORIES = "categories"
    const val MENU_ITEMS = "menuItems"
    const val ORDERS = "orders"
    const val OFFERS = "offers"
    const val BROADCASTS = "broadcasts"
    const val COUPONS = "coupons"
    const val SETTINGS = "settings"
    const val RESTAURANT_STATUS_DOC = "restaurantStatus"
    const val DELIVERY_AREA_DOC = "deliveryArea"
    const val DELIVERY_PRICING_DOC = "deliveryPricing"
}
