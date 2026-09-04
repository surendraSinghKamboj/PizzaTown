package com.pizzatown.customer.domain.repository

import com.pizzatown.customer.domain.model.UserProfile

interface ProfileRepository {
    suspend fun getProfile(userId: String): Result<UserProfile>
    suspend fun updateProfile(profile: UserProfile): Result<Unit>
    suspend fun setDefaultAddress(
        userId: String,
        addressId: String
    ): Result<Unit>
    suspend fun uploadProfileImage(userId: String, imageBytes: ByteArray): Result<String>
}
