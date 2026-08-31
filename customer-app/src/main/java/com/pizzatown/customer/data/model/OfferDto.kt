package com.pizzatown.customer.data.model

import com.google.firebase.firestore.DocumentId
import com.pizzatown.customer.domain.model.Offer

data class OfferDto(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val active: Boolean = true,
    val sortOrder: Int = 0
)

fun OfferDto.toDomain() = Offer(id, title, description, imageUrl, sortOrder)
