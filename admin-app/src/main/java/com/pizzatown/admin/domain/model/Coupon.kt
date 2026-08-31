package com.pizzatown.admin.domain.model

enum class DiscountType { PERCENTAGE, FIXED_AMOUNT }

/**
 * A discount coupon. [targetUserId] null means any customer can use it;
 * set to one customer's id, only they can (e.g. a birthday coupon
 * created from the Upcoming Events screen).
 */
data class Coupon(
    val id: String = "",
    val code: String = "",
    val discountType: DiscountType = DiscountType.PERCENTAGE,
    val discountValue: Double = 0.0,
    val minOrderValue: Double = 0.0,
    val maxDiscountAmount: Double = 0.0, // 0 = no cap (only meaningful for PERCENTAGE)
    val targetUserId: String? = null,
    val targetCustomerName: String = "",
    val usageLimit: Int = 0, // 0 = unlimited
    val usageCount: Int = 0,
    val active: Boolean = true,
    val expiresAt: Long = 0L, // 0 = never expires
    val createdAt: Long = 0L
) {
    fun isValid(): Boolean = code.isNotBlank() && discountValue > 0.0
}
