package com.pizzatown.admin.domain.model

/** A registered customer, as seen from the admin side (read-only). */
data class Customer(
    val userId: String = "",
    val fullName: String = "",
    val mobile: String = "",
    val email: String = "",
    val dateOfBirth: Long = 0L,
    val anniversaryDate: Long = 0L
)
