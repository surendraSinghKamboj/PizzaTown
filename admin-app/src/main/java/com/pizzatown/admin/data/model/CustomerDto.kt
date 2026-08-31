package com.pizzatown.admin.data.model

import com.google.firebase.firestore.DocumentId
import com.pizzatown.admin.domain.model.Customer

data class CustomerDto(
    @DocumentId val userId: String = "",
    val fullName: String = "",
    val mobile: String = "",
    val email: String = "",
    val dateOfBirth: Long = 0L,
    val anniversaryDate: Long = 0L
)

fun CustomerDto.toDomain() = Customer(userId, fullName, mobile, email, dateOfBirth, anniversaryDate)
