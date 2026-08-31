package com.pizzatown.admin.domain.repository

import com.pizzatown.admin.domain.model.Coupon
import kotlinx.coroutines.flow.Flow

interface CouponRepository {
    fun observeCoupons(): Flow<List<Coupon>>
    suspend fun getCoupon(id: String): Result<Coupon>
    suspend fun addCoupon(coupon: Coupon): Result<String>
    suspend fun updateCoupon(coupon: Coupon): Result<Unit>
    suspend fun deleteCoupon(id: String): Result<Unit>
    suspend fun setActive(id: String, active: Boolean): Result<Unit>
}
