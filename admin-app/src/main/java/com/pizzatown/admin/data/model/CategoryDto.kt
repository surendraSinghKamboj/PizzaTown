package com.pizzatown.admin.data.model

import com.google.firebase.firestore.DocumentId
import com.pizzatown.admin.domain.model.Category

data class CategoryDto(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val enabled: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

fun CategoryDto.toDomain() = Category(id, name, enabled, sortOrder, createdAt, updatedAt)
fun Category.toDto() = CategoryDto(id, name, enabled, sortOrder, createdAt, updatedAt)
