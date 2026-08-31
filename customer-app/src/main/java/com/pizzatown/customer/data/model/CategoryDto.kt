package com.pizzatown.customer.data.model

import com.google.firebase.firestore.DocumentId
import com.pizzatown.customer.domain.model.Category

data class CategoryDto(
    @DocumentId val id: String = "",
    val name: String = "", val enabled: Boolean = true, val sortOrder: Int = 0
)

fun CategoryDto.toDomain() = Category(id, name, enabled, sortOrder)
