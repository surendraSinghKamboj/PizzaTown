package com.pizzatown.customer.domain.usecase

import com.pizzatown.customer.domain.model.CartItem
import com.pizzatown.customer.domain.model.SelectedOption
import org.junit.Assert.assertEquals
import org.junit.Test

class CartPricingUseCasesTest {

    private val calculateCartTotal = CalculateCartTotalUseCase()

    // Matches the spec example exactly:
    // Burger Double = ₹219, Extra Cheese = ₹20, Quantity = 2
    // Unit price = ₹239, Line total = ₹478
    @Test
    fun `burger double with extra cheese calculates correctly`() {
        val item = CartItem(
            cartItemId = "1",
            menuItemId = "burger",
            menuItemName = "Classic Cheese Burger",
            imageUrl = "",
            selectedVariantId = "double",
            selectedVariantName = "Double",
            basePrice = 219.0,
            selectedOptions = listOf(
                SelectedOption("addons", "Add-ons", "cheese", "Extra Cheese", 20.0)
            ),
            quantity = 2
        )

        assertEquals(239.0, item.finalUnitPrice, 0.001)
        assertEquals(478.0, item.lineTotal, 0.001)
    }

    @Test
    fun `fixed price item with no customization`() {
        val fries = CartItem(
            cartItemId = "2", menuItemId = "fries", menuItemName = "French Fries",
            imageUrl = "", basePrice = 120.0, quantity = 3
        )
        assertEquals(120.0, fries.finalUnitPrice, 0.001)
        assertEquals(360.0, fries.lineTotal, 0.001)
    }

    @Test
    fun `cart total reconciles subtotal and grand total with no discount`() {
        val items = listOf(
            CartItem("1", "burger", "Burger", "", "double", "Double", 219.0,
                listOf(SelectedOption("addons", "Add-ons", "cheese", "Extra Cheese", 20.0)), 2),
            CartItem("2", "fries", "Fries", "", basePrice = 120.0, quantity = 1)
        )
        val totals = calculateCartTotal(items)

        // 478 (burger line) + 120 (fries line) = 598
        assertEquals(598.0, totals.subtotal, 0.001)
        assertEquals(598.0, totals.grandTotal, 0.001)
        assertEquals(3, totals.totalItemCount) // 2 burgers + 1 fries
    }

    @Test
    fun `discount and delivery fee and tax reconcile into grand total`() {
        val items = listOf(
            CartItem("1", "pizza", "Margherita", "", basePrice = 330.0, quantity = 1)
        )
        val totals = calculateCartTotal(items, discount = 30.0, deliveryFee = 40.0, taxRate = 0.05)

        // subtotal 330, tax = 330*0.05 = 16.5, grand = 330 - 30 + 40 + 16.5 = 356.5
        assertEquals(330.0, totals.subtotal, 0.001)
        assertEquals(16.5, totals.tax, 0.001)
        assertEquals(356.5, totals.grandTotal, 0.001)
    }

    @Test
    fun `different customizations produce different cart line identities`() {
        val keyDouble = CartItem.configKey("burger", "double", listOf("cheese"))
        val keySingle = CartItem.configKey("burger", "single", listOf("cheese"))
        val keyNoCheese = CartItem.configKey("burger", "double", emptyList())

        // Different variant/customization selections must never collapse to the same key.
        assert(keyDouble != keySingle)
        assert(keyDouble != keyNoCheese)

        // Same configuration, regardless of option order, must produce the same key
        // (so re-adding the identical combo increments quantity instead of duplicating).
        val keyA = CartItem.configKey("burger", "double", listOf("cheese", "jalapeno"))
        val keyB = CartItem.configKey("burger", "double", listOf("jalapeno", "cheese"))
        assertEquals(keyA, keyB)
    }
}
