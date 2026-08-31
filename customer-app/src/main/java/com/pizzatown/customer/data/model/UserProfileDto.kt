package com.pizzatown.customer.data.model

import com.pizzatown.customer.domain.model.Address
import com.pizzatown.customer.domain.model.UserProfile

data class AddressDto(
    val id: String = "",
    val label: String = "",
    val fullAddress: String = "",
    val isDefault: Boolean = false
)

data class UserProfileDto(
    val fullName: String = "",
    val mobile: String = "",
    val email: String = "",
    val addresses: List<AddressDto> = emptyList(),
    val profileImageUrl: String = "",
    val dateOfBirth: Long = 0L,
    val anniversaryDate: Long = 0L
)

fun AddressDto.toDomain() = Address(id, label, fullAddress, isDefault)
fun Address.toDto() = AddressDto(id, label, fullAddress, isDefault)

fun UserProfileDto.toDomain(userId: String) = UserProfile(
    userId = userId,
    fullName = fullName,
    mobile = mobile,
    email = email,
    addresses = addresses.map { it.toDomain() },
    profileImageUrl = profileImageUrl,
    dateOfBirth = dateOfBirth,
    anniversaryDate = anniversaryDate
)

fun UserProfile.toDto() = UserProfileDto(
    fullName = fullName,
    mobile = mobile,
    email = email,
    addresses = addresses.map { it.toDto() },
    profileImageUrl = profileImageUrl,
    dateOfBirth = dateOfBirth,
    anniversaryDate = anniversaryDate
)
