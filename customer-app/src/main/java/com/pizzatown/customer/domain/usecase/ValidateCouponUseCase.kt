package com.pizzatown.customer.domain.usecase

import com.pizzatown.customer.domain.model.Coupon
import com.pizzatown.customer.domain.model.DiscountType
import javax.inject.Inject

sealed interface CouponValidationResult {
    data class Valid(val discountAmount: Double) : CouponValidationResult
    data class Invalid(val reason: String) : CouponValidationResult
}

/**
 * Pure validation + discount calculation for a coupon at checkout. Kept
 * completely independent of Firestore/network so it's cheap to unit
 * test every edge case (expiry, per-customer targeting, usage limits,
 * minimum order value, percentage caps).
 */
class ValidateCouponUseCase @Inject constructor() {

    operator fun invoke(
        coupon: Coupon?,
        subtotal: Double,
        currentUserId: String?,
        now: Long = System.currentTimeMillis()
    ): CouponValidationResult {
        if (coupon == null) return CouponValidationResult.Invalid("Invalid coupon code.")
        if (!coupon.active) return CouponValidationResult.Invalid("This coupon is no longer active.")
        if (coupon.expiresAt > 0 && now > coupon.expiresAt) {
            return CouponValidationResult.Invalid("This coupon has expired.")
        }
        if (coupon.targetUserId != null && coupon.targetUserId != currentUserId) {
            return CouponValidationResult.Invalid("This coupon isn't valid for your account.")
        }
        if (coupon.usageLimit > 0 && coupon.usageCount >= coupon.usageLimit) {
            return CouponValidationResult.Invalid("This coupon has already been fully redeemed.")
        }
        if (subtotal < coupon.minOrderValue) {
            return CouponValidationResult.Invalid("Minimum order of \u20B9${coupon.minOrderValue.toInt()} required for this coupon.")
        }

        val rawDiscount = when (coupon.discountType) {
            DiscountType.PERCENTAGE -> subtotal * coupon.discountValue / 100.0
            DiscountType.FIXED_AMOUNT -> coupon.discountValue
        }
        val cappedDiscount = if (coupon.discountType == DiscountType.PERCENTAGE && coupon.maxDiscountAmount > 0) {
            minOf(rawDiscount, coupon.maxDiscountAmount)
        } else rawDiscount

        // Never let a coupon make the order total negative.
        val finalDiscount = minOf(cappedDiscount, subtotal)

        return CouponValidationResult.Valid(finalDiscount)
    }
}
