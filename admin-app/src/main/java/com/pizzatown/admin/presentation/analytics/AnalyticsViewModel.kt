package com.pizzatown.admin.presentation.analytics

import java.util.Date
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pizzatown.admin.core.common.UiState
import com.pizzatown.admin.domain.model.Order
import com.pizzatown.admin.domain.model.OrderStatus
import com.pizzatown.admin.domain.repository.CouponRepository
import com.pizzatown.admin.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

data class TopSellingItem(
    val name: String,
    val quantitySold: Int,
    val revenue: Double
)

data class DailySalesPoint(
    val label: String,
    val revenue: Double,
    val orders: Int
)

data class OrderStatusCount(
    val status: OrderStatus,
    val count: Int
)

data class PaymentMethodCount(
    val label: String,
    val count: Int
)

enum class AnalyticsRange {
    TODAY,
    THIS_WEEK,
    LAST_7_DAYS,
    THIS_MONTH,
    LAST_30_DAYS,
    ALL_TIME,
    CUSTOM
}

data class AnalyticsDateRange(
    val start: Long? = null,
    val end: Long? = null
)

data class SalesAnalytics(
    val revenueToday: Double,
    val revenueThisWeek: Double,
    val revenueThisMonth: Double,
    val revenueAllTime: Double,
    val completedOrderCount: Int,
    val averageOrderValue: Double,
    val cancelledOrderCount: Int,
    val cancelledRevenueLost: Double,
    val topSellingItems: List<TopSellingItem>,
    val couponsRedeemed: Int,
    val couponDiscountGiven: Double,
    val dailySales: List<DailySalesPoint>,
    val orderStatusCounts: List<OrderStatusCount>,
    val paymentMethodCounts: List<PaymentMethodCount>
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val couponRepository: CouponRepository
) : ViewModel() {

    private val _selectedRange =
        MutableStateFlow(AnalyticsRange.LAST_7_DAYS)

    val selectedRange = _selectedRange.asStateFlow()

    private val _customDateRange =
        MutableStateFlow(AnalyticsDateRange())

    val customDateRange = _customDateRange.asStateFlow()

    fun selectRange(range: AnalyticsRange) {
        _selectedRange.value = range
    }

    fun setCustomDateRange(start: Long, end: Long) {
        _customDateRange.value = AnalyticsDateRange(
            start = start,
            end = end
        )
        _selectedRange.value = AnalyticsRange.CUSTOM
    }

    val analyticsState: StateFlow<UiState<SalesAnalytics>> = combine(
        orderRepository.observeOrders(),
        couponRepository.observeCoupons()
    ) { orders, _ ->
        UiState.Success(computeAnalytics(orders)) as UiState<SalesAnalytics>
    }.catch {
        emit(UiState.Error(it.message ?: "Failed to load analytics"))
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        UiState.Loading
    )

    private fun computeAnalytics(
        orders: List<Order>
    ): SalesAnalytics {

        val completed = orders.filter {
            it.status == OrderStatus.COMPLETED
        }

        val cancelled = orders.filter {
            it.status == OrderStatus.CANCELLED
        }

        val todayStart = startOfToday()

        val range = when (_selectedRange.value) {
            AnalyticsRange.TODAY ->
                todayStart to null

            AnalyticsRange.THIS_WEEK -> {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = todayStart
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val daysFromMonday =
                    if (dayOfWeek == Calendar.SUNDAY) 6
                    else dayOfWeek - Calendar.MONDAY

                (todayStart - daysFromMonday * DAY_MS) to null
            }

            AnalyticsRange.LAST_7_DAYS ->
                (todayStart - 6L * DAY_MS) to null

            AnalyticsRange.THIS_MONTH -> {
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis to null
            }

            AnalyticsRange.LAST_30_DAYS ->
                (todayStart - 29L * DAY_MS) to null

            AnalyticsRange.ALL_TIME ->
                null to null

            AnalyticsRange.CUSTOM ->
                _customDateRange.value.start to _customDateRange.value.end
        }

        val rangeStart = range.first
        val rangeEnd = range.second

        fun inRange(timestamp: Long): Boolean {
            if (rangeStart == null) return true
            if (timestamp < rangeStart) return false
            if (rangeEnd != null && timestamp >= rangeEnd) return false
            return true
        }

        val rangedOrders = orders.filter {
            inRange(it.createdAt)
        }

        val rangedCompleted = completed.filter {
            inRange(it.createdAt)
        }

        val rangedCancelled = cancelled.filter {
            inRange(it.createdAt)
        }

        val revenue = rangedCompleted.sumOf {
            it.grandTotal
        }

        val avgOrderValue =
            if (rangedCompleted.isNotEmpty()) {
                revenue / rangedCompleted.size
            } else {
                0.0
            }

        val topItems = rangedCompleted
            .flatMap { it.items }
            .groupBy { it.name }
            .map { (name, lines) ->
                TopSellingItem(
                    name = name,
                    quantitySold = lines.sumOf { it.quantity },
                    revenue = lines.sumOf { it.lineTotal }
                )
            }
            .sortedByDescending { it.quantitySold }
            .take(5)

        val couponOrders =
            rangedCompleted.filter {
                it.couponCode.isNotBlank()
            }

        val dailySales = buildDailySales(
            rangedOrders,
            rangedCompleted,
            rangeStart
        )

        val orderStatusCounts = OrderStatus.entries
            .filter { it != OrderStatus.BEING_PAYMENT }
            .map { status ->
                OrderStatusCount(
                    status = status,
                    count = rangedOrders.count {
                        it.status == status
                    }
                )
            }

        val paymentMethodCounts = listOf(
            PaymentMethodCount(
                label = "Online",
                count = rangedOrders.count {
                    it.paymentMethod.name == "ONLINE"
                }
            ),
            PaymentMethodCount(
                label = "COD",
                count = rangedOrders.count {
                    it.paymentMethod.name == "COD"
                }
            )
        )

        return SalesAnalytics(
            revenueToday = rangedCompleted
                .filter { it.createdAt >= todayStart }
                .sumOf { it.grandTotal },

            revenueThisWeek = rangedCompleted
                .filter { it.createdAt >= todayStart - 6L * DAY_MS }
                .sumOf { it.grandTotal },

            revenueThisMonth = rangedCompleted
                .filter { it.createdAt >= todayStart - 29L * DAY_MS }
                .sumOf { it.grandTotal },

            revenueAllTime = revenue,

            completedOrderCount = rangedCompleted.size,

            averageOrderValue = avgOrderValue,

            cancelledOrderCount = rangedCancelled.size,

            cancelledRevenueLost =
                rangedCancelled.sumOf { it.grandTotal },

            topSellingItems = topItems,

            couponsRedeemed = couponOrders.size,

            couponDiscountGiven =
                couponOrders.sumOf { it.discount },

            dailySales = dailySales,

            orderStatusCounts = orderStatusCounts,

            paymentMethodCounts = paymentMethodCounts
        )
    }

    private fun buildDailySales(
        rangedOrders: List<Order>,
        rangedCompleted: List<Order>,
        rangeStart: Long?
    ): List<DailySalesPoint> {

        val start =
            rangeStart ?: startOfToday() - 6L * DAY_MS

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = start

        val days =
            when (_selectedRange.value) {
                AnalyticsRange.TODAY -> 1
                AnalyticsRange.THIS_WEEK -> 7
                AnalyticsRange.LAST_7_DAYS -> 7
                AnalyticsRange.THIS_MONTH -> {
                    calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                }
                AnalyticsRange.LAST_30_DAYS -> 30
                AnalyticsRange.ALL_TIME -> 30
                AnalyticsRange.CUSTOM -> {
                    val customEnd =
                        _customDateRange.value.end ?: start + 6L * DAY_MS
                    (((customEnd - start) / DAY_MS).toInt() + 1)
                        .coerceIn(1, 30)
                }
            }

        return (days - 1 downTo 0).map { daysAgo ->
            val dayStart = start + (days - 1 - daysAgo) * DAY_MS
            val dayEnd = dayStart + DAY_MS

            val dayOrders = rangedOrders.filter {
                it.createdAt >= dayStart &&
                    it.createdAt < dayEnd
            }

            val dayCompleted = rangedCompleted.filter {
                it.createdAt >= dayStart &&
                    it.createdAt < dayEnd
            }

            DailySalesPoint(
                label = dayLabel(dayStart),
                revenue = dayCompleted.sumOf {
                    it.grandTotal
                },
                orders = dayOrders.size
            )
        }
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

    private companion object {
        const val DAY_MS = 24L * 60L * 60L * 1000L
    }
}
