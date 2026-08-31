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

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loginSuccess: Boolean = false,
    val resetEmailSent: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val analyticsLogger: AnalyticsLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(v: String) { _uiState.value = _uiState.value.copy(email = v, errorMessage = null) }
    fun onPasswordChange(v: String) { _uiState.value = _uiState.value.copy(password = v, errorMessage = null) }

    fun login() {
        val state = _uiState.value
        if (state.email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _uiState.value = state.copy(errorMessage = "Enter a valid email.")
            return
        }
        if (state.password.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Password is required.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.login(state.email.trim(), state.password)) {
                is AuthResult.Success -> {
                    analyticsLogger.setUserId(authRepository.currentUserId)
                    analyticsLogger.logLogin()
                    _uiState.value = _uiState.value.copy(isLoading = false, loginSuccess = true)
                }
                is AuthResult.Failure -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }

    fun forgotPassword() {
        val email = _uiState.value.email
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Enter your email above first, then tap 'Forgot password'.")
            return
        }
        viewModelScope.launch {
            when (authRepository.sendPasswordReset(email.trim())) {
                is AuthResult.Success -> _uiState.value = _uiState.value.copy(resetEmailSent = true, errorMessage = null)
                is AuthResult.Failure -> _uiState.value = _uiState.value.copy(errorMessage = "Couldn't send reset email. Try again.")
            }
        }
    }
}
