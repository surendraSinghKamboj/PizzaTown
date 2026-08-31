package com.pizzatown.admin.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pizzatown.admin.domain.repository.AdminAuthRepository
import com.pizzatown.admin.domain.repository.AdminAuthResult
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
    val loginSuccess: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AdminAuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(email = value, errorMessage = null)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, errorMessage = null)
    }

    fun login() {
        val state = _uiState.value
        val validationError = validate(state.email, state.password)
        if (validationError != null) {
            _uiState.value = state.copy(errorMessage = validationError)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.login(state.email.trim(), state.password)) {
                is AdminAuthResult.Success ->
                    _uiState.value = _uiState.value.copy(isLoading = false, loginSuccess = true)

                is AdminAuthResult.NotAuthorized ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "This account is not authorized as an admin."
                    )

                is AdminAuthResult.Failure ->
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }

    private fun validate(email: String, password: String): String? {
        if (email.isBlank()) return "Email is required."
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) return "Enter a valid email."
        if (password.isBlank()) return "Password is required."
        return null
    }
}
