package com.pizzatown.customer.domain.model

data class Broadcast(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val targetUserId: String? = null,
    val createdAt: Long = 0L
)
