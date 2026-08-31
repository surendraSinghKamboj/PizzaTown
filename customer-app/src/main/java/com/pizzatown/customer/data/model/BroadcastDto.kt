package com.pizzatown.customer.data.model

import com.google.firebase.firestore.DocumentId
import com.pizzatown.customer.domain.model.Broadcast

data class BroadcastDto(
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String = "",
    val targetUserId: String? = null,
    val createdAt: Long = 0L
)

fun BroadcastDto.toDomain() = Broadcast(id, title, message, targetUserId, createdAt)
