package com.pizzatown.admin.data.model

import com.google.firebase.firestore.DocumentId
import com.pizzatown.admin.domain.model.Offer

data class OfferDto(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val active: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

fun OfferDto.toDomain() = Offer(id, title, description, imageUrl, active, sortOrder, createdAt, updatedAt)
fun Offer.toDto() = OfferDto(id, title, description, imageUrl, active, sortOrder, createdAt, updatedAt)
