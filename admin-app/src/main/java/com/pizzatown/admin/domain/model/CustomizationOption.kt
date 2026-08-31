package com.pizzatown.admin.domain.model

/**
 * A single selectable option inside a [CustomizationGroup].
 * e.g. "Extra Cheese" +20, "Jalapeno" +20
 */
data class CustomizationOption(
    val id: String = "",
    val name: String = "",
    val priceAdjustment: Double = 0.0,
    val available: Boolean = true
)
