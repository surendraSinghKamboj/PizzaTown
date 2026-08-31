package com.pizzatown.admin.domain.model

data class Category(
    val id: String = "",
    val name: String = "",
    val enabled: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
