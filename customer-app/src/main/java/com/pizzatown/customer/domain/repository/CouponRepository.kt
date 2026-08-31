package com.pizzatown.customer.domain.repository

import com.pizzatown.customer.domain.model.Coupon

interface CouponRepository {
    /** Looks up a coupon by its code (case-insensitive). Null if no coupon has that code. */
    suspend fun getCouponByCode(code: String): Result<Coupon?>

    /** Atomically increments usageCount by 1 — call only after the order is successfully placed. */
    suspend fun incrementUsage(couponId: String): Result<Unit>
}
