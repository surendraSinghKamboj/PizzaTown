package com.pizzatown.customer.core.common

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
    data object Empty : UiState<Nothing>
}

sealed interface OpState {
    data object Idle : OpState
    data object InProgress : OpState
    data object Success : OpState
    data class Error(val message: String) : OpState
}
