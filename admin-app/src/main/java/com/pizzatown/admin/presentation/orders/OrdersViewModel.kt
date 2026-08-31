package com.pizzatown.admin.presentation.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pizzatown.admin.core.common.UiState
import com.pizzatown.admin.domain.model.Order
import com.pizzatown.admin.domain.model.OrderStatus
import com.pizzatown.admin.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/** null = show all statuses */
@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _statusFilter = MutableStateFlow<OrderStatus?>(null)
    val statusFilter: StateFlow<OrderStatus?> = _statusFilter.asStateFlow()

    /** Free-text search over customer name, phone, and order id. */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** null = no date restriction */
    private val _dateFilter = MutableStateFlow<OrderDateFilter>(OrderDateFilter.ALL)
    val dateFilter: StateFlow<OrderDateFilter> = _dateFilter.asStateFlow()

    private val allOrders: StateFlow<UiState<List<Order>>> = orderRepository.observeOrders()
        .map { orders -> if (orders.isEmpty()) UiState.Empty else UiState.Success(orders) as UiState<List<Order>> }
        .catch { emit(UiState.Error(it.message ?: "Failed to load orders")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val ordersState: StateFlow<UiState<List<Order>>> =
        combine(allOrders, _statusFilter, _searchQuery, _dateFilter) { state, status, query, dateFilter ->
            if (state !is UiState.Success) return@combine state
            var filtered = state.data
            if (status != null) filtered = filtered.filter { it.status == status }
            if (query.isNotBlank()) {
                val q = query.trim().lowercase()
                filtered = filtered.filter {
                    it.customer.name.lowercase().contains(q) ||
                        it.customer.phone.lowercase().contains(q) ||
                        it.orderId.lowercase().contains(q)
                }
            }
            filtered = filtered.filter { dateFilter.matches(it.createdAt) }
            if (filtered.isEmpty()) UiState.Empty else UiState.Success(filtered)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    fun setStatusFilter(status: OrderStatus?) { _statusFilter.value = status }
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setDateFilter(filter: OrderDateFilter) { _dateFilter.value = filter }

    fun advanceStatus(order: Order) {
        val next = order.nextStatus() ?: return
        viewModelScope.launch { orderRepository.updateOrderStatus(order.orderId, next) }
    }

    fun cancelOrder(order: Order) {
        viewModelScope.launch { orderRepository.updateOrderStatus(order.orderId, OrderStatus.CANCELLED) }
    }
}

/** Coarse date-range filter for the admin orders list, so a busy shop can
 *  jump straight to "what needs attention today" instead of scrolling
 *  through the full history. */
enum class OrderDateFilter(val label: String) {
    ALL("All time"),
    TODAY("Today"),
    LAST_7_DAYS("Last 7 days"),
    LAST_30_DAYS("Last 30 days");

    fun matches(createdAt: Long): Boolean {
        if (this == ALL) return true
        val now = System.currentTimeMillis()
        val cutoff = when (this) {
            TODAY -> {
                val cal = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                cal.timeInMillis
            }
            LAST_7_DAYS -> now - 7L * 24 * 60 * 60 * 1000
            LAST_30_DAYS -> now - 30L * 24 * 60 * 60 * 1000
            ALL -> 0L
        }
        return createdAt >= cutoff
    }
}
