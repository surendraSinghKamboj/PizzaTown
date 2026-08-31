package com.pizzatown.customer.domain.repository

import kotlinx.coroutines.flow.Flow

sealed interface AuthResult {
    data object Success : AuthResult
    data class Failure(val message: String) : AuthResult
}

interface AuthRepository {
    val isSignedIn: Flow<Boolean>
    val currentUserId: String?

    suspend fun login(email: String, password: String): AuthResult

    /** Delivery address is intentionally NOT collected here — it's asked
     *  on the customer's first checkout instead (see CheckoutViewModel). */
    suspend fun register(
        fullName: String,
        mobile: String,
        email: String,
        password: String
    ): AuthResult

    suspend fun sendPasswordReset(email: String): AuthResult
    fun logout()
}
