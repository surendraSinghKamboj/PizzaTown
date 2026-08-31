package com.pizzatown.customer.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pizzatown.customer.core.analytics.AnalyticsLogger
import com.pizzatown.customer.domain.repository.AuthRepository
import com.pizzatown.customer.domain.repository.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val fullName: String = "",
    val mobile: String = "",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val registerSuccess: Boolean = false
)

/**
 * Registration intentionally does NOT collect a delivery address —
 * that's asked on the customer's first checkout instead (see
 * CheckoutViewModel), so sign-up stays fast and address entry happens
 * when it's actually needed.
 */
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val analyticsLogger: AnalyticsLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onFullNameChange(v: String) { _uiState.value = _uiState.value.copy(fullName = v, errorMessage = null) }
    fun onMobileChange(v: String) { _uiState.value = _uiState.value.copy(mobile = v, errorMessage = null) }
    fun onEmailChange(v: String) { _uiState.value = _uiState.value.copy(email = v, errorMessage = null) }
    fun onPasswordChange(v: String) { _uiState.value = _uiState.value.copy(password = v, errorMessage = null) }

    fun register() {
        val state = _uiState.value
        val error = validate(state)
        if (error != null) {
            _uiState.value = state.copy(errorMessage = error)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = authRepository.register(
                fullName = state.fullName.trim(),
                mobile = state.mobile.trim(),
                email = state.email.trim(),
                password = state.password
            )
            _uiState.value = when (result) {
                is AuthResult.Success -> {
                    analyticsLogger.setUserId(authRepository.currentUserId)
                    analyticsLogger.logSignUp()
                    _uiState.value.copy(isLoading = false, registerSuccess = true)
                }
                is AuthResult.Failure -> _uiState.value.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }

    private fun validate(state: RegisterUiState): String? {
        if (state.fullName.isBlank()) return "Full name is required."
        if (state.mobile.isBlank() || state.mobile.length < 10) return "Enter a valid mobile number."
        if (state.email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) return "Enter a valid email."
        if (state.password.length < 6) return "Password must be at least 6 characters."
        return null
    }
}
