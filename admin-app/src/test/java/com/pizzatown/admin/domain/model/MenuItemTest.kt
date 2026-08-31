package com.pizzatown.admin.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuItemTest {

    @Test
    fun `fixed price item with valid price is valid`() {
        val item = MenuItem(name = "French Fries", categoryId = "sides", pricingMode = PricingMode.FIXED, basePrice = 120.0)
        assertTrue(item.isValid())
    }

    @Test
    fun `fixed price item with zero price is invalid`() {
        val item = MenuItem(name = "French Fries", categoryId = "sides", pricingMode = PricingMode.FIXED, basePrice = 0.0)
        assertFalse(item.isValid())
    }

    @Test
    fun `variant item requires at least one variant`() {
        val item = MenuItem(name = "Pizza", categoryId = "pizza", pricingMode = PricingMode.VARIANTS, variants = emptyList())
        assertFalse(item.isValid())
    }

    @Test
    fun `variant item with valid variants is valid`() {
        val item = MenuItem(
            name = "Margherita", categoryId = "pizza", pricingMode = PricingMode.VARIANTS,
            variants = listOf(
                MenuVariant(id = "reg", name = "Regular", price = 120.0),
                MenuVariant(id = "med", name = "Medium", price = 220.0)
            )
        )
        assertTrue(item.isValid())
    }

    @Test
    fun `displayFromPrice returns cheapest available variant`() {
        val item = MenuItem(
            name = "Margherita", categoryId = "pizza", pricingMode = PricingMode.VARIANTS,
            variants = listOf(
                MenuVariant(id = "reg", name = "Regular", price = 120.0, available = true),
                MenuVariant(id = "med", name = "Medium", price = 220.0, available = true),
                MenuVariant(id = "lg", name = "Large", price = 90.0, available = false) // cheaper but unavailable
            )
        )
        assertTrue(item.displayFromPrice() == 120.0)
    }

    @Test
    fun `customization group requiring selection but min zero is invalid`() {
        val group = CustomizationGroup(
            name = "Add-ons", selectionType = SelectionType.MULTIPLE, required = true, minSelections = 0, maxSelections = 3,
            options = listOf(CustomizationOption(name = "Extra Cheese", priceAdjustment = 20.0))
        )
        assertFalse(group.isValid())
    }

    @Test
    fun `single select group with max greater than one is invalid`() {
        val group = CustomizationGroup(
            name = "Size", selectionType = SelectionType.SINGLE, maxSelections = 2,
            options = listOf(CustomizationOption(name = "Small"), CustomizationOption(name = "Large"))
        )
        assertFalse(group.isValid())
    }
}
