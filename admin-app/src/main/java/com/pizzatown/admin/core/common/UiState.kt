package com.pizzatown.admin.core.common

/**
 * Generic, sealed result state used by ViewModels so screens can render
 * Loading / Error / Empty / Success consistently instead of ad-hoc booleans.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
    data object Empty : UiState<Nothing>
}

/** Result of a single fire-and-forget operation (save, delete, upload...). */
sealed interface OpState {
    data object Idle : OpState
    data object InProgress : OpState
    data object Success : OpState
    data class Error(val message: String) : OpState
}
