package com.pizzatown.admin.data.model

import com.google.firebase.firestore.DocumentId
import com.pizzatown.admin.domain.model.Broadcast

data class BroadcastDto(
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String = "",
    val targetUserId: String? = null,
    val targetCustomerName: String = "",
    val createdAt: Long = 0L
)

fun BroadcastDto.toDomain() = Broadcast(id, title, message, targetUserId, targetCustomerName, createdAt)
fun Broadcast.toDto() = BroadcastDto(id, title, message, targetUserId, targetCustomerName, createdAt)
