package com.pizzatown.admin.presentation.profile

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.pizzatown.admin.domain.repository.AdminAuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class AdminProfile(
    val email: String,
    val uid: String,
    val displayName: String
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val authRepository: AdminAuthRepository
) : ViewModel() {

    fun profile(): AdminProfile {
        val user = firebaseAuth.currentUser

        return AdminProfile(
            email = user?.email.orEmpty(),
            uid = user?.uid.orEmpty(),
            displayName = user?.displayName
                ?.takeIf { it.isNotBlank() }
                ?: "Pizza Town Admin"
        )
    }

    fun logout() {
        authRepository.logout()
    }
}
