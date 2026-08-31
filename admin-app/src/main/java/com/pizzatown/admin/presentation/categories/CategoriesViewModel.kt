package com.pizzatown.admin.presentation.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pizzatown.admin.core.common.OpState
import com.pizzatown.admin.core.common.UiState
import com.pizzatown.admin.domain.model.Category
import com.pizzatown.admin.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val repository: CategoryRepository
) : ViewModel() {

    val categories: StateFlow<UiState<List<Category>>> = repository.observeCategories()
        .map { if (it.isEmpty()) UiState.Empty else UiState.Success(it) as UiState<List<Category>> }
        .catch { emit(UiState.Error(it.message ?: "Failed to load categories")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    private val _opState = MutableStateFlow<OpState>(OpState.Idle)
    val opState: StateFlow<OpState> = _opState.asStateFlow()

    fun addCategory(name: String) {
        if (name.isBlank()) {
            _opState.value = OpState.Error("Category name is required.")
            return
        }
        viewModelScope.launch {
            _opState.value = OpState.InProgress
            val result = repository.addCategory(Category(name = name.trim()))
            _opState.value = result.fold(
                onSuccess = { OpState.Success },
                onFailure = { OpState.Error(it.message ?: "Failed to add category") }
            )
        }
    }

    fun renameCategory(category: Category, newName: String) {
        if (newName.isBlank()) {
            _opState.value = OpState.Error("Category name is required.")
            return
        }
        viewModelScope.launch {
            _opState.value = OpState.InProgress
            val result = repository.updateCategory(category.copy(name = newName.trim()))
            _opState.value = result.fold(
                onSuccess = { OpState.Success },
                onFailure = { OpState.Error(it.message ?: "Failed to update category") }
            )
        }
    }

    fun setEnabled(category: Category, enabled: Boolean) {
        viewModelScope.launch {
            repository.setEnabled(category.id, enabled)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            _opState.value = OpState.InProgress
            val result = repository.deleteCategory(category.id)
            _opState.value = result.fold(
                onSuccess = { OpState.Success },
                onFailure = { OpState.Error(it.message ?: "Failed to delete category") }
            )
        }
    }

    fun consumeOpState() {
        _opState.value = OpState.Idle
    }
}
