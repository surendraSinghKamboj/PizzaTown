package com.pizzatown.admin.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pizzatown.admin.core.common.UiState
import com.pizzatown.admin.domain.model.Order
import com.pizzatown.admin.domain.model.OrderStatus
import com.pizzatown.admin.domain.repository.AdminAuthRepository
import com.pizzatown.admin.domain.repository.CategoryRepository
import com.pizzatown.admin.domain.repository.MenuRepository
import com.pizzatown.admin.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

data class DashboardDailyPoint(
    val label: String,
    val revenue: Double,
    val orders: Int
)

data class DashboardStats(
    val totalItems: Int,
    val availableItems: Int,
    val unavailableItems: Int,
    val totalCategories: Int,
    val todayRevenue: Double,
    val todayOrders: Int,
    val activeOrders: Int,
    val completedOrders: Int,
    val averageOrderValue: Double,
    val pendingOrders: Int,
    val confirmedOrders: Int,
    val preparingOrders: Int,
    val readyOrders: Int,
    val cancelledOrders: Int,
    val dailyPoints: List<DashboardDailyPoint>,
    val recentOrders: List<Order>
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val menuRepository: MenuRepository,
    private val categoryRepository: CategoryRepository,
    private val orderRepository: OrderRepository,
    private val authRepository: AdminAuthRepository
) : ViewModel() {

    val stats: StateFlow<UiState<DashboardStats>> = combine(
        menuRepository.observeMenuItems(),
        categoryRepository.observeCategories(),
        orderRepository.observeOrders()
    ) { items, categories, orders ->
        UiState.Success(computeStats(items.size, items.count { it.available }, items.count { !it.available }, categories.size, orders))
            as UiState<DashboardStats>
    }.catch {
        emit(UiState.Error(it.message ?: "Failed to load dashboard"))
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        UiState.Loading
    )

    private fun computeStats(
        totalItems: Int,
        availableItems: Int,
        unavailableItems: Int,
        totalCategories: Int,
        orders: List<Order>
    ): DashboardStats {
        val todayStart = startOfToday()
        val dayMs = 24L * 60L * 60L * 1000L

        val todayOrders = orders.filter { it.createdAt >= todayStart }

        val completed = orders.filter { it.status == OrderStatus.COMPLETED }
        val todayCompleted = todayOrders.filter { it.status == OrderStatus.COMPLETED }

        val todayRevenue = todayCompleted.sumOf { it.grandTotal }

        val active = orders.count {
            it.status == OrderStatus.PENDING ||
            it.status == OrderStatus.CONFIRMED ||
            it.status == OrderStatus.PREPARING ||
            it.status == OrderStatus.READY
        }

        val averageOrderValue =
            if (completed.isNotEmpty()) {
                completed.sumOf { it.grandTotal } / completed.size
            } else {
                0.0
            }

        val dailyPoints = (6 downTo 0).map { daysAgo ->
            val start = todayStart - daysAgo * dayMs
            val end = start + dayMs

            val dayOrders = orders.filter {
                it.createdAt >= start && it.createdAt < end
            }

            val dayCompleted = dayOrders.filter {
                it.status == OrderStatus.COMPLETED
            }

            DashboardDailyPoint(
                label = dayLabel(start),
                revenue = dayCompleted.sumOf { it.grandTotal },
                orders = dayOrders.size
            )
        }

        return DashboardStats(
            totalItems = totalItems,
            availableItems = availableItems,
            unavailableItems = unavailableItems,
            totalCategories = totalCategories,
            todayRevenue = todayRevenue,
            todayOrders = todayOrders.size,
            activeOrders = active,
            completedOrders = completed.size,
            averageOrderValue = averageOrderValue,
            pendingOrders = orders.count { it.status == OrderStatus.PENDING },
            confirmedOrders = orders.count { it.status == OrderStatus.CONFIRMED },
            preparingOrders = orders.count { it.status == OrderStatus.PREPARING },
            readyOrders = orders.count { it.status == OrderStatus.READY },
            cancelledOrders = orders.count { it.status == OrderStatus.CANCELLED },
            dailyPoints = dailyPoints,
            recentOrders = orders.take(5)
        )
    }

    private fun startOfToday(): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun dayLabel(timestamp: Long): String =
        Calendar.getInstance().apply {
            timeInMillis = timestamp
        }.let {
            when (it.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> "Sun"
                Calendar.MONDAY -> "Mon"
                Calendar.TUESDAY -> "Tue"
                Calendar.WEDNESDAY -> "Wed"
                Calendar.THURSDAY -> "Thu"
                Calendar.FRIDAY -> "Fri"
                else -> "Sat"
            }
        }

    fun logout() = authRepository.logout()
}
