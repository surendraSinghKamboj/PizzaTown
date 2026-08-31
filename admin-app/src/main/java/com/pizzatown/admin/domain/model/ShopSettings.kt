package com.pizzatown.admin.domain.model

/** Whether the shop is currently accepting orders. Read by the customer
 *  app to block checkout while closed, and enforced again server-side
 *  (see firestore.rules' restaurantIsOpen()). */
data class RestaurantStatus(
    val isOpen: Boolean = false,
    val updatedAt: Long = 0L
)

/** The circular area the shop delivers to: a center point plus a radius
 *  in kilometers. The customer app measures the customer's device
 *  location against this before allowing checkout. */
data class DeliveryArea(
    val centerLat: Double = 0.0,
    val centerLng: Double = 0.0,
    val radiusKm: Double = 5.0,
    val updatedAt: Long = 0L
) {
    /** True once the admin has actually set a center point (not the zero default). */
    fun isConfigured(): Boolean = centerLat != 0.0 || centerLng != 0.0
}


/** Customer-facing order pricing rules managed by the admin. */
data class DeliveryPricing(
    val minimumOrderValue: Double = 0.0,
    val deliveryCharge: Double = 0.0,
    val freeDeliveryAbove: Double = 0.0,
    val updatedAt: Long = 0L
) {
    fun expectedDeliveryFee(subtotal: Double): Double =
        if (freeDeliveryAbove > 0.0 && subtotal >= freeDeliveryAbove) {
            0.0
        } else {
            deliveryCharge
        }

    fun minimumOrderSatisfied(subtotal: Double): Boolean =
        subtotal >= minimumOrderValue
}
