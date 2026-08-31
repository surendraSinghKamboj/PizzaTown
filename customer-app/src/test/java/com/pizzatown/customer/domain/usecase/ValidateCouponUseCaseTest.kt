package com.pizzatown.customer.domain.usecase

import com.pizzatown.customer.domain.model.Coupon
import com.pizzatown.customer.domain.model.DiscountType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateCouponUseCaseTest {

    private val useCase = ValidateCouponUseCase()

    @Test
    fun `null coupon is invalid`() {
        val result = useCase(null, subtotal = 500.0, currentUserId = "user1")
        assertTrue(result is CouponValidationResult.Invalid)
    }

    @Test
    fun `inactive coupon is invalid`() {
        val coupon = Coupon(code = "OFF10", active = false, discountValue = 10.0)
        val result = useCase(coupon, subtotal = 500.0, currentUserId = "user1")
        assertTrue(result is CouponValidationResult.Invalid)
    }

    @Test
    fun `expired coupon is invalid`() {
        val coupon = Coupon(code = "OFF10", discountValue = 10.0, expiresAt = 1000L)
        val result = useCase(coupon, subtotal = 500.0, currentUserId = "user1", now = 2000L)
        assertTrue(result is CouponValidationResult.Invalid)
    }

    @Test
    fun `coupon with future expiry is valid`() {
        val coupon = Coupon(code = "OFF10", discountType = DiscountType.PERCENTAGE, discountValue = 10.0, expiresAt = 5000L)
        val result = useCase(coupon, subtotal = 500.0, currentUserId = "user1", now = 2000L)
        assertTrue(result is CouponValidationResult.Valid)
    }

    @Test
    fun `coupon targeted at another customer is invalid for this user`() {
        val coupon = Coupon(code = "BDAY20", discountValue = 20.0, targetUserId = "user-42")
        val result = useCase(coupon, subtotal = 500.0, currentUserId = "user1")
        assertTrue(result is CouponValidationResult.Invalid)
    }

    @Test
    fun `coupon targeted at this customer is valid`() {
        val coupon = Coupon(code = "BDAY20", discountType = DiscountType.PERCENTAGE, discountValue = 20.0, targetUserId = "user1")
        val result = useCase(coupon, subtotal = 500.0, currentUserId = "user1")
        assertTrue(result is CouponValidationResult.Valid)
    }

    @Test
    fun `fully redeemed coupon is invalid`() {
        val coupon = Coupon(code = "LIMITED", discountValue = 10.0, usageLimit = 5, usageCount = 5)
        val result = useCase(coupon, subtotal = 500.0, currentUserId = "user1")
        assertTrue(result is CouponValidationResult.Invalid)
    }

    @Test
    fun `coupon under usage limit is valid`() {
        val coupon = Coupon(code = "LIMITED", discountType = DiscountType.PERCENTAGE, discountValue = 10.0, usageLimit = 5, usageCount = 4)
        val result = useCase(coupon, subtotal = 500.0, currentUserId = "user1")
        assertTrue(result is CouponValidationResult.Valid)
    }

    @Test
    fun `subtotal below minimum order value is invalid`() {
        val coupon = Coupon(code = "BIG500", discountValue = 50.0, minOrderValue = 500.0)
        val result = useCase(coupon, subtotal = 300.0, currentUserId = "user1")
        assertTrue(result is CouponValidationResult.Invalid)
    }

    @Test
    fun `percentage discount calculates correctly`() {
        val coupon = Coupon(code = "OFF20", discountType = DiscountType.PERCENTAGE, discountValue = 20.0)
        val result = useCase(coupon, subtotal = 1000.0, currentUserId = "user1") as CouponValidationResult.Valid
        assertEquals(200.0, result.discountAmount, 0.001)
    }

    @Test
    fun `percentage discount respects max cap`() {
        val coupon = Coupon(code = "OFF50", discountType = DiscountType.PERCENTAGE, discountValue = 50.0, maxDiscountAmount = 100.0)
        val result = useCase(coupon, subtotal = 1000.0, currentUserId = "user1") as CouponValidationResult.Valid
        // 50% of 1000 = 500, but capped at 100
        assertEquals(100.0, result.discountAmount, 0.001)
    }

    @Test
    fun `fixed amount discount calculates correctly`() {
        val coupon = Coupon(code = "FLAT50", discountType = DiscountType.FIXED_AMOUNT, discountValue = 50.0)
        val result = useCase(coupon, subtotal = 200.0, currentUserId = "user1") as CouponValidationResult.Valid
        assertEquals(50.0, result.discountAmount, 0.001)
    }

    @Test
    fun `fixed amount discount never exceeds subtotal`() {
        val coupon = Coupon(code = "FLAT500", discountType = DiscountType.FIXED_AMOUNT, discountValue = 500.0)
        val result = useCase(coupon, subtotal = 100.0, currentUserId = "user1") as CouponValidationResult.Valid
        assertEquals(100.0, result.discountAmount, 0.001)
    }

    @Test
    fun `unlimited usage coupon (limit zero) is always valid regardless of usage count`() {
        val coupon = Coupon(code = "UNLIMITED", discountType = DiscountType.PERCENTAGE, discountValue = 5.0, usageLimit = 0, usageCount = 9999)
        val result = useCase(coupon, subtotal = 500.0, currentUserId = "user1")
        assertTrue(result is CouponValidationResult.Valid)
    }

    @Test
    fun `no minimum order value means any subtotal qualifies`() {
        val coupon = Coupon(code = "ANY", discountType = DiscountType.PERCENTAGE, discountValue = 5.0, minOrderValue = 0.0)
        val result = useCase(coupon, subtotal = 1.0, currentUserId = "user1")
        assertTrue(result is CouponValidationResult.Valid)
    }
}
