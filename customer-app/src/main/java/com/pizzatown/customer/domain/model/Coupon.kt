package com.pizzatown.customer.domain.model

enum class DiscountType { PERCENTAGE, FIXED_AMOUNT }

data class Coupon(
    val id: String = "",
    val code: String = "",
    val discountType: DiscountType = DiscountType.PERCENTAGE,
    val discountValue: Double = 0.0,
    val minOrderValue: Double = 0.0,
    val maxDiscountAmount: Double = 0.0,
    val targetUserId: String? = null,
    val usageLimit: Int = 0,
    val usageCount: Int = 0,
    val active: Boolean = true,
    val expiresAt: Long = 0L
)
