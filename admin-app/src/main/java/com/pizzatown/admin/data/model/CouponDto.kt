package com.pizzatown.admin.data.model

import com.google.firebase.firestore.DocumentId
import com.pizzatown.admin.domain.model.Coupon
import com.pizzatown.admin.domain.model.DiscountType

data class CouponDto(
    @DocumentId val id: String = "",
    val code: String = "",
    val discountType: String = "PERCENTAGE",
    val discountValue: Double = 0.0,
    val minOrderValue: Double = 0.0,
    val maxDiscountAmount: Double = 0.0,
    val targetUserId: String? = null,
    val targetCustomerName: String = "",
    val usageLimit: Int = 0,
    val usageCount: Int = 0,
    val active: Boolean = true,
    val expiresAt: Long = 0L,
    val createdAt: Long = 0L
)

fun CouponDto.toDomain() = Coupon(
    id, code,
    runCatching { DiscountType.valueOf(discountType) }.getOrDefault(DiscountType.PERCENTAGE),
    discountValue, minOrderValue, maxDiscountAmount, targetUserId, targetCustomerName,
    usageLimit, usageCount, active, expiresAt, createdAt
)

fun Coupon.toDto() = CouponDto(
    id, code, discountType.name, discountValue, minOrderValue, maxDiscountAmount,
    targetUserId, targetCustomerName, usageLimit, usageCount, active, expiresAt, createdAt
)
