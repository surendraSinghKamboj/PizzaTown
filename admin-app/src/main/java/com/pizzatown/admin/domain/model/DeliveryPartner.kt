package com.pizzatown.admin.domain.model

data class DeliveryPartner(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val active: Boolean = true,
    val createdAt: Long = 0L
)
