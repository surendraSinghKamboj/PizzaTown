package com.pizzatown.customer.domain.model

enum class PricingMode { FIXED, VARIANTS }

/**
 * Fully generic menu item — identical shape to the admin app's model.
 * A pizza, a burger, a mocktail, or a combo are all just a MenuItem
 * with different data; there is no per-food-type class.
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
    // Optional merchandising fields — null/false when the admin hasn't set
    // them yet. Screens must only show rating/bestseller UI when present.
    val rating: Double? = null,
    val reviewCount: Int? = null,
    val isBestseller: Boolean = false,
    val variants: List<MenuVariant> = emptyList(),
    val customizationGroups: List<CustomizationGroup> = emptyList()
) {
    fun displayFromPrice(): Double = when (pricingMode) {
        PricingMode.FIXED -> basePrice
        PricingMode.VARIANTS -> variants.filter { it.available }.minOfOrNull { it.price } ?: 0.0
    }
}
