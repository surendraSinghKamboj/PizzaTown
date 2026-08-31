package com.pizzatown.admin.domain.model

/**
 * Whether a menu item is sold at one fixed price, or via priced variants.
 * e.g. French Fries -> FIXED (₹120). Pizza -> VARIANTS (Regular/Medium/Large).
 */
enum class PricingMode {
    FIXED,
    VARIANTS
}

/**
 * A completely generic restaurant menu item.
 *
 * IMPORTANT: this class must never be replaced with food-specific
 * classes like PizzaItem/BurgerItem. Every product type (pizza, burger,
 * cold drink, mocktail, combo, dessert, etc.) is represented by this
 * one model, driven entirely by admin-entered data.
 */
data class MenuItem(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val categoryId: String = "",
    val imageUrl: String = "",
    val pricingMode: PricingMode = PricingMode.FIXED,
    val basePrice: Double = 0.0,
    val available: Boolean = true,
    val variants: List<MenuVariant> = emptyList(),
    val customizationGroups: List<CustomizationGroup> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    /**
     * The price shown as the "starting" price on menu cards.
     * FIXED -> basePrice. VARIANTS -> cheapest available variant price.
     */
    fun displayFromPrice(): Double = when (pricingMode) {
        PricingMode.FIXED -> basePrice
        PricingMode.VARIANTS -> variants.filter { it.available }.minOfOrNull { it.price } ?: 0.0
    }

    fun isValid(): Boolean {
        if (name.isBlank()) return false
        if (categoryId.isBlank()) return false
        if (pricingMode == PricingMode.FIXED && basePrice <= 0.0) return false
        if (pricingMode == PricingMode.VARIANTS && variants.isEmpty()) return false
        if (pricingMode == PricingMode.VARIANTS && variants.any { it.price <= 0.0 || it.name.isBlank() }) return false
        if (customizationGroups.any { !it.isValid() }) return false
        return true
    }
}
