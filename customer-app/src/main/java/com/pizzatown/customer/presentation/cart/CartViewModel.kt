package com.pizzatown.customer.presentation.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pizzatown.customer.core.analytics.AnalyticsLogger
import com.pizzatown.customer.domain.model.CartItem
import com.pizzatown.customer.domain.repository.CartRepository
import com.pizzatown.customer.domain.usecase.CalculateCartTotalUseCase
import com.pizzatown.customer.domain.usecase.CartTotals
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CartUiData(
    val items: List<CartItem>,
    val totals: CartTotals
)

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val calculateCartTotal: CalculateCartTotalUseCase,
    private val analyticsLogger: AnalyticsLogger
) : ViewModel() {

    val cartData: StateFlow<CartUiData> = cartRepository.observeCart()
        .map { items -> CartUiData(items, calculateCartTotal(items)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CartUiData(emptyList(), calculateCartTotal(emptyList())))

    fun increment(item: CartItem) {
        viewModelScope.launch { cartRepository.updateQuantity(item.cartItemId, item.quantity + 1) }
    }

    fun decrement(item: CartItem) {
        viewModelScope.launch { cartRepository.updateQuantity(item.cartItemId, item.quantity - 1) }
    }

    fun remove(item: CartItem) {
        analyticsLogger.logRemoveFromCart(item)
        viewModelScope.launch { cartRepository.removeItem(item.cartItemId) }
    }

    fun clearCart() {
        viewModelScope.launch { cartRepository.clearCart() }
    }
}
