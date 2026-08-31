package com.pizzatown.customer.presentation.menu

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pizzatown.customer.core.analytics.AnalyticsLogger
import com.pizzatown.customer.core.common.UiState
import com.pizzatown.customer.domain.model.*
import com.pizzatown.customer.domain.repository.CartRepository
import com.pizzatown.customer.domain.repository.MenuRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MenuItemDetailsUiState(
    val menuItem: MenuItem? = null,
    val selectedVariantId: String? = null,
    // groupId -> set of selected optionIds
    val selectedOptions: Map<String, Set<String>> = emptyMap(),
    val quantity: Int = 1,
    val addedToCart: Boolean = false,
    val validationError: String? = null
) {
    val unitPrice: Double
        get() {
            val item = menuItem ?: return 0.0
            val basePrice = when (item.pricingMode) {
                PricingMode.FIXED -> item.basePrice
                PricingMode.VARIANTS -> item.variants.find { it.id == selectedVariantId }?.price ?: 0.0
            }
            val customizationTotal = item.customizationGroups.sumOf { group ->
                val selectedIds = selectedOptions[group.id].orEmpty()
                group.options.filter { it.id in selectedIds }.sumOf { it.priceAdjustment }
            }
            return basePrice + customizationTotal
        }

    val totalPrice: Double get() = unitPrice * quantity
}

@HiltViewModel
class MenuItemDetailsViewModel @Inject constructor(
    private val menuRepository: MenuRepository,
    private val cartRepository: CartRepository,
    private val analyticsLogger: AnalyticsLogger,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val itemId: String = checkNotNull(savedStateHandle["itemId"])

    private val _loadState = MutableStateFlow<UiState<MenuItem>>(UiState.Loading)
    val loadState: StateFlow<UiState<MenuItem>> = _loadState.asStateFlow()

    private val _uiState = MutableStateFlow(MenuItemDetailsUiState())
    val uiState: StateFlow<MenuItemDetailsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            menuRepository.getMenuItem(itemId)
                .onSuccess { item ->
                    _loadState.value = UiState.Success(item)
                    _uiState.value = MenuItemDetailsUiState(
                        menuItem = item,
                        selectedVariantId = item.variants.firstOrNull { it.available }?.id
                    )
                    val displayPrice = when (item.pricingMode) {
                        PricingMode.FIXED -> item.basePrice
                        PricingMode.VARIANTS -> item.variants.firstOrNull { it.available }?.price ?: item.basePrice
                    }
                    analyticsLogger.logViewItem(item.id, item.name, item.categoryId, displayPrice)
                }
                .onFailure { _loadState.value = UiState.Error(it.message ?: "Unable to load this item.") }
        }
    }

    fun selectVariant(variantId: String) {
        _uiState.value = _uiState.value.copy(selectedVariantId = variantId, validationError = null)
    }

    fun toggleOption(group: CustomizationGroup, optionId: String) {
        val current = _uiState.value.selectedOptions[group.id].orEmpty()
        val updated = if (group.selectionType == SelectionType.SINGLE) {
            setOf(optionId)
        } else {
            if (optionId in current) current - optionId else current + optionId
        }
        val newMap = _uiState.value.selectedOptions.toMutableMap().apply { put(group.id, updated) }
        _uiState.value = _uiState.value.copy(selectedOptions = newMap, validationError = null)
    }

    fun incrementQuantity() {
        _uiState.value = _uiState.value.copy(quantity = _uiState.value.quantity + 1)
    }

    fun decrementQuantity() {
        val newQty = (_uiState.value.quantity - 1).coerceAtLeast(1)
        _uiState.value = _uiState.value.copy(quantity = newQty)
    }

    fun addToCart() {
        val state = _uiState.value
        val item = state.menuItem ?: return

        if (item.pricingMode == PricingMode.VARIANTS && state.selectedVariantId == null) {
            _uiState.value = state.copy(validationError = "Please select an option before adding to cart.")
            return
        }
        for (group in item.customizationGroups) {
            val selected = state.selectedOptions[group.id].orEmpty()
            if (group.required && selected.size < group.minSelections.coerceAtLeast(1)) {
                _uiState.value = state.copy(validationError = "Please complete \"${group.name}\" before adding to cart.")
                return
            }
            if (selected.size > group.maxSelections) {
                _uiState.value = state.copy(validationError = "You can select up to ${group.maxSelections} options in \"${group.name}\".")
                return
            }
        }

        val selectedVariant = item.variants.find { it.id == state.selectedVariantId }
        val selectedOptionsList = item.customizationGroups.flatMap { group ->
            val selectedIds = state.selectedOptions[group.id].orEmpty()
            group.options.filter { it.id in selectedIds }.map { option ->
                SelectedOption(group.id, group.name, option.id, option.name, option.priceAdjustment)
            }
        }

        val cartItem = CartItem(
            menuItemId = item.id,
            menuItemName = item.name,
            imageUrl = item.imageUrl,
            selectedVariantId = selectedVariant?.id,
            selectedVariantName = selectedVariant?.name,
            basePrice = if (item.pricingMode == PricingMode.FIXED) item.basePrice else (selectedVariant?.price ?: 0.0),
            selectedOptions = selectedOptionsList,
            quantity = state.quantity
        )

        viewModelScope.launch {
            cartRepository.addOrIncrement(cartItem)
            analyticsLogger.logAddToCart(cartItem)
            _uiState.value = _uiState.value.copy(addedToCart = true)
        }
    }

    fun consumeAddedToCart() {
        _uiState.value = _uiState.value.copy(addedToCart = false)
    }
}
