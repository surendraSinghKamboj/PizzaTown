package com.pizzatown.customer.domain.model

data class Category(
    val id: String = "",
    val name: String = "",
    val enabled: Boolean = true,
    val sortOrder: Int = 0
)
