package com.pizzatown.customer.domain.model

data class Offer(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val sortOrder: Int = 0
)
