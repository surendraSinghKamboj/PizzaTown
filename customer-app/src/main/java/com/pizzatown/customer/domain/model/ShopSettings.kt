package com.pizzatown.customer.domain.model

/** Whether the shop is currently accepting orders — read-only here; only
 *  the admin app can change it. */
data class RestaurantStatus(
    val isOpen: Boolean = false,
    val updatedAt: Long = 0L
)

/** The circular area the shop delivers to. The customer app measures the
 *  device's current location against this before allowing checkout. */
data class DeliveryArea(
    val centerLat: Double = 0.0,
    val centerLng: Double = 0.0,
    val radiusKm: Double = 5.0,
    val updatedAt: Long = 0L
) {
    fun isConfigured(): Boolean = centerLat != 0.0 || centerLng != 0.0
}


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
