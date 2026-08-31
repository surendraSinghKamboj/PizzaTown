package com.pizzatown.customer.domain.model

/**
 * A saved delivery address. Customers can save multiple (Home, Work,
 * Other...) and pick one at checkout, instead of a single address
 * field on their profile.
 */
data class Address(
    val id: String = "",
    val label: String = "",       // e.g. "Home", "Work", "Other"
    val fullAddress: String = "",
    val isDefault: Boolean = false
)

data class UserProfile(
    val userId: String = "",
    val fullName: String = "",
    val mobile: String = "",
    val email: String = "",
    val addresses: List<Address> = emptyList(),
    val profileImageUrl: String = "",
    /** Epoch millis at UTC midnight of the birth date, or 0L if not set. */
    val dateOfBirth: Long = 0L,
    /** Epoch millis at UTC midnight of the anniversary date, or 0L if not set. */
    val anniversaryDate: Long = 0L
)
