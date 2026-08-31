package com.pizzatown.customer.presentation.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pizzatown.customer.core.common.UiState
import com.pizzatown.customer.domain.model.Order
import com.pizzatown.customer.domain.repository.AuthRepository
import com.pizzatown.customer.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class OrderHistoryViewModel @Inject constructor(
    orderRepository: OrderRepository,
    authRepository: AuthRepository
) : ViewModel() {

    private val userId = authRepository.currentUserId

    val ordersState: StateFlow<UiState<List<Order>>> =
        (userId?.let { uid ->
            orderRepository.observeOrdersForUser(uid)
                .map { orders -> if (orders.isEmpty()) UiState.Empty else UiState.Success(orders) }
        } ?: flowOf(UiState.Error("You must be signed in to view your orders.")))
            .catch { emit(UiState.Error(it.message ?: "Unable to load your orders right now.")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)
}
