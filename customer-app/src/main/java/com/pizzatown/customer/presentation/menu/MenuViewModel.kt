package com.pizzatown.customer.presentation.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pizzatown.customer.core.common.UiState
import com.pizzatown.customer.domain.model.Address
import com.pizzatown.customer.domain.model.CartItem
import com.pizzatown.customer.domain.model.Category
import com.pizzatown.customer.domain.model.MenuItem
import com.pizzatown.customer.domain.model.RestaurantStatus
import com.pizzatown.customer.domain.repository.AuthRepository
import com.pizzatown.customer.domain.repository.CartRepository
import com.pizzatown.customer.domain.repository.CategoryRepository
import com.pizzatown.customer.domain.repository.FavoriteRepository
import com.pizzatown.customer.domain.repository.MenuRepository
import com.pizzatown.customer.domain.repository.ProfileRepository
import com.pizzatown.customer.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MenuScreenData(
    val categories: List<Category>,
    val items: List<MenuItem>,
    val bestsellers: List<MenuItem>
)

enum class SortOption(val label: String) {
    RELEVANCE("Relevance"),
    PRICE_LOW_HIGH("Price: Low to High"),
    PRICE_HIGH_LOW("Price: High to Low"),
    RATING("Rating")
}

@HiltViewModel
class MenuViewModel @Inject constructor(
    menuRepository: MenuRepository,
    categoryRepository: CategoryRepository,
    private val cartRepository: CartRepository,
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    private val favoriteRepository: FavoriteRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<String?>(null) // null = All
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.RELEVANCE)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    /** First name only, for the "Good evening, X" greeting — blank until loaded. */
    private val _customerFirstName = MutableStateFlow("")
    val customerFirstName: StateFlow<String> = _customerFirstName.asStateFlow()

    private val _defaultAddress = MutableStateFlow<Address?>(null)
    val defaultAddress: StateFlow<Address?> = _defaultAddress.asStateFlow()

    private val _profileLoaded = MutableStateFlow(false)
    val profileLoaded: StateFlow<Boolean> = _profileLoaded.asStateFlow()

    val restaurantStatus: StateFlow<RestaurantStatus> = settingsRepository.observeRestaurantStatus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RestaurantStatus(isOpen = true))

    /** cartItemId -> quantity, for menu items with no variants/customizations
     *  (their cartItemId equals their menuItemId — see CartItem.configKey),
     *  which is what lets the Home grid drive a live +/- stepper safely. */
    val simpleCartQuantities: StateFlow<Map<String, Int>> = cartRepository.observeCart()
        .map { items -> items.associate { it.menuItemId to it.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** Device-local favorites (see FavoriteRepository) — real toggle, not decorative. */
    val favoriteIds: StateFlow<Set<String>> = favoriteRepository.observeFavoriteIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    init {
        refreshProfile()
    }

    fun refreshProfile() {
        val uid = authRepository.currentUserId ?: return

        viewModelScope.launch {
            profileRepository.getProfile(uid)
                .onSuccess { profile ->
                    _customerFirstName.value =
                        profile.fullName.trim().substringBefore(" ")

                    _defaultAddress.value =
                        profile.addresses.find { it.isDefault }
                            ?: profile.addresses.firstOrNull()

                    _profileLoaded.value = true
                }
                .onFailure {
                    _profileLoaded.value = true
                }
        }
    }

    private val rawState: StateFlow<UiState<MenuScreenData>> = combine(
        menuRepository.observeMenuItems(),
        categoryRepository.observeCategories()
    ) { items, categories ->
        if (items.isEmpty() && categories.isEmpty()) UiState.Empty
        else UiState.Success(
            MenuScreenData(
                categories = categories,
                items = items,
                bestsellers = items.filter { it.isBestseller && it.available }
            )
        ) as UiState<MenuScreenData>
    }.catch { emit(UiState.Error(it.message ?: "Unable to load the menu right now.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val screenState: StateFlow<UiState<MenuScreenData>> = combine(
        rawState, _searchQuery, _selectedCategoryId, _sortOption
    ) { state, query, categoryId, sort ->
        if (state !is UiState.Success) return@combine state
        var filtered = state.data.items.filter { item ->
            val matchesCategory = categoryId == null || item.categoryId == categoryId
            val matchesQuery = query.isBlank() || item.name.contains(query, ignoreCase = true) ||
                item.description.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
        filtered = when (sort) {
            SortOption.RELEVANCE -> filtered
            SortOption.PRICE_LOW_HIGH -> filtered.sortedBy { it.displayFromPrice() }
            SortOption.PRICE_HIGH_LOW -> filtered.sortedByDescending { it.displayFromPrice() }
            SortOption.RATING -> filtered.sortedByDescending { it.rating ?: -1.0 }
        }
        UiState.Success(state.data.copy(items = filtered)) as UiState<MenuScreenData>
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }
    fun onCategorySelected(categoryId: String?) { _selectedCategoryId.value = categoryId }
    fun onSortSelected(option: SortOption) { _sortOption.value = option }

    fun toggleFavorite(menuItemId: String) {
        viewModelScope.launch {
            val currentlyFavorite = favoriteIds.value.contains(menuItemId)
            favoriteRepository.toggleFavorite(menuItemId, currentlyFavorite)
        }
    }

    /**
     * One-tap add for items with a single fixed price and no customization
     * choices to make — safe to add directly from the grid. Items priced
     * by variant, or with customizations, return false so the caller can
     * navigate to MenuItemDetailsScreen instead, where a real choice can
     * actually be made (a variant/customization can't be guessed here).
     */
    fun quickAdd(item: MenuItem): Boolean {
        if (item.pricingMode != com.pizzatown.customer.domain.model.PricingMode.FIXED || item.customizationGroups.isNotEmpty()) return false
        viewModelScope.launch {
            cartRepository.addOrIncrement(
                CartItem(
                    menuItemId = item.id,
                    menuItemName = item.name,
                    imageUrl = item.imageUrl,
                    basePrice = item.displayFromPrice(),
                    quantity = 1
                )
            )
        }
        return true
    }

    fun increment(menuItemId: String) {
        viewModelScope.launch {
            val current = simpleCartQuantities.value[menuItemId] ?: 0
            val cartItemId = CartItem.configKey(
                menuItemId = menuItemId,
                variantId = null,
                selectedOptionIds = emptyList()
            )
            cartRepository.updateQuantity(cartItemId, current + 1)
        }
    }

    fun decrement(menuItemId: String) {
        viewModelScope.launch {
            val current = simpleCartQuantities.value[menuItemId] ?: 0
            val cartItemId = CartItem.configKey(
                menuItemId = menuItemId,
                variantId = null,
                selectedOptionIds = emptyList()
            )
            cartRepository.updateQuantity(cartItemId, current - 1)
        }
    }

    fun logout() = authRepository.logout()
}
