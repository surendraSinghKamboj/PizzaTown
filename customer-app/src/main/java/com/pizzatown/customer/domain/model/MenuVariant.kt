package com.pizzatown.customer.domain.model

data class MenuVariant(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val available: Boolean = true
)
