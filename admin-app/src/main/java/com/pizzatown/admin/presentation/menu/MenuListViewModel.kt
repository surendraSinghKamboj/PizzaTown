package com.pizzatown.admin.presentation.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pizzatown.admin.core.common.UiState
import com.pizzatown.admin.domain.model.Category
import com.pizzatown.admin.domain.model.MenuItem
import com.pizzatown.admin.domain.repository.CategoryRepository
import com.pizzatown.admin.domain.repository.MenuRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MenuListData(
    val items: List<MenuItem>,
    val categoriesById: Map<String, Category>
)

@HiltViewModel
class MenuListViewModel @Inject constructor(
    private val menuRepository: MenuRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** null = all categories */
    private val _categoryFilter = MutableStateFlow<String?>(null)
    val categoryFilter: StateFlow<String?> = _categoryFilter.asStateFlow()

    private val rawMenu: StateFlow<UiState<MenuListData>> = combine(
        menuRepository.observeMenuItems(),
        categoryRepository.observeCategories()
    ) { items, categories ->
        if (items.isEmpty()) {
            UiState.Empty
        } else {
            UiState.Success(MenuListData(items, categories.associateBy { it.id }))
        } as UiState<MenuListData>
    }.catch { emit(UiState.Error(it.message ?: "Failed to load menu")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val menuState: StateFlow<UiState<MenuListData>> =
        combine(rawMenu, _searchQuery, _categoryFilter) { state, query, categoryId ->
            if (state !is UiState.Success) return@combine state
            var items = state.data.items
            if (categoryId != null) items = items.filter { it.categoryId == categoryId }
            if (query.isNotBlank()) {
                val q = query.trim().lowercase()
                items = items.filter { it.name.lowercase().contains(q) }
            }
            if (items.isEmpty()) UiState.Empty else UiState.Success(state.data.copy(items = items))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    /** Full, unfiltered category list — always shown in the filter row even
     *  when the current search/category filter has narrowed [menuState] down. */
    val allCategories: StateFlow<List<Category>> = categoryRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setCategoryFilter(categoryId: String?) { _categoryFilter.value = categoryId }

    fun setAvailable(item: MenuItem, available: Boolean) {
        viewModelScope.launch {
            menuRepository.setAvailable(item.id, available)
        }
    }

    fun deleteItem(item: MenuItem) {
        viewModelScope.launch {
            menuRepository.deleteMenuItem(item.id)
        }
    }
}
