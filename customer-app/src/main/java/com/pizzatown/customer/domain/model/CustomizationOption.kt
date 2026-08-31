package com.pizzatown.customer.domain.model

data class CustomizationOption(
    val id: String = "",
    val name: String = "",
    val priceAdjustment: Double = 0.0,
    val available: Boolean = true
)
