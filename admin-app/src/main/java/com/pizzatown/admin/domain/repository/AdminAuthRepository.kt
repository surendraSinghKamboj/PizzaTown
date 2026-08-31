package com.pizzatown.admin.domain.repository

import kotlinx.coroutines.flow.Flow

sealed interface AdminAuthResult {
    data object Success : AdminAuthResult
    /** Signed in with Firebase Auth, but the account has no admin custom claim. */
    data object NotAuthorized : AdminAuthResult
    data class Failure(val message: String) : AdminAuthResult
}

interface AdminAuthRepository {
    /** Emits true once we've confirmed the current session belongs to an authorized admin. */
    val isAdminSignedIn: Flow<Boolean>

    suspend fun login(email: String, password: String): AdminAuthResult

    fun logout()
}
