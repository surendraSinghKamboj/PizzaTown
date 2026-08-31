package com.pizzatown.admin.presentation.dashboard

import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pizzatown.admin.ui.theme.PizzaSuccess
import com.pizzatown.admin.ui.theme.StatusPreparing
import com.pizzatown.admin.core.common.UiState
import com.pizzatown.admin.domain.model.Order
import com.pizzatown.admin.domain.model.OrderStatus
import com.pizzatown.admin.presentation.components.ErrorView
import com.pizzatown.admin.presentation.components.LoadingView
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max
import com.pizzatown.admin.ui.theme.StatusPending
import com.pizzatown.admin.ui.theme.StatusConfirmed
import com.pizzatown.admin.ui.theme.StatusReady
import com.pizzatown.admin.ui.theme.StatusCancelled

private val currencyFormat =
    NumberFormat.getCurrencyInstance(Locale("en", "IN"))

private fun money(value: Double): String =
    currencyFormat.format(value)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenCategories: () -> Unit,
    onOpenMenu: () -> Unit,
    onOpenOrders: () -> Unit,
    onOpenOffers: () -> Unit,
    onOpenUpcomingEvents: () -> Unit,
    onOpenBroadcast: () -> Unit,
    onOpenCoupons: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenShopSettings: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Pizza Town",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Admin Dashboard",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.logout()
                            onLoggedOut()
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout"
                        )
                    }
                }
            )
        }
    ) { padding ->
        when (val current = stats) {
            is UiState.Loading ->
                LoadingView(Modifier.padding(padding))

            is UiState.Error ->
                ErrorView(
                    current.message,
                    modifier = Modifier.padding(padding)
                )

            is UiState.Empty -> {
                DashboardEmptyState(
                    modifier = Modifier.padding(padding)
                )
            }

            is UiState.Success -> {
                DashboardContent(
                    data = current.data,
                    modifier = Modifier.padding(padding),
                    onOpenOrders = onOpenOrders,
                    onOpenAnalytics = onOpenAnalytics,
                    onOpenCategories = onOpenCategories,
                    onOpenMenu = onOpenMenu,
                    onOpenOffers = onOpenOffers,
                    onOpenUpcomingEvents = onOpenUpcomingEvents,
                    onOpenBroadcast = onOpenBroadcast,
                    onOpenCoupons = onOpenCoupons,
                    onOpenShopSettings = onOpenShopSettings
                )
            }
        }
    }
}

@Composable
private fun DashboardContent(
    data: DashboardStats,
    modifier: Modifier,
    onOpenOrders: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenMenu: () -> Unit,
    onOpenOffers: () -> Unit,
    onOpenUpcomingEvents: () -> Unit,
    onOpenBroadcast: () -> Unit,
    onOpenCoupons: () -> Unit,
    onOpenShopSettings: () -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 12.dp,
            end = 16.dp,
            bottom = 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            WelcomeCard(data)
        }

        item {
            SectionTitle(
                title = "Today",
                subtitle = "Your business at a glance"
            )
        }

        item {
            KpiGrid(data)
        }

        item {
            SectionTitle(
                title = "Sales overview",
                subtitle = "Last 7 days"
            )
        }

        item {
            RevenueChartCard(data.dailyPoints)
        }

        item {
            OrdersChartCard(data.dailyPoints)
        }

        item {
            SectionTitle(
                title = "Order pipeline",
                subtitle = "Live operational status"
            )
        }

        item {
            OrderPipelineCard(data)
        }

        item {
            SectionTitle(
                title = "Recent orders",
                subtitle = "Latest activity"
            )
        }

        item {
            RecentOrdersCard(
                orders = data.recentOrders,
                onOpenOrders = onOpenOrders
            )
        }

        item {
            SectionTitle(
                title = "Menu health",
                subtitle = "Availability snapshot"
            )
        }

        item {
            MenuHealthCard(data)
        }

        item {
            SectionTitle(
                title = "Quick actions",
                subtitle = "Manage your restaurant"
            )
        }

        item {
            QuickActionsGrid(
                onOpenOrders = onOpenOrders,
                onOpenAnalytics = onOpenAnalytics,
                onOpenCategories = onOpenCategories,
                onOpenMenu = onOpenMenu,
                onOpenOffers = onOpenOffers,
                onOpenUpcomingEvents = onOpenUpcomingEvents,
                onOpenBroadcast = onOpenBroadcast,
                onOpenCoupons = onOpenCoupons,
                onOpenShopSettings = onOpenShopSettings
            )
        }
    }
}

@Composable
private fun WelcomeCard(data: DashboardStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                "Welcome back",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(Modifier.height(4.dp))

            Text(
                "Keep the kitchen moving.",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (data.activeOrders > 0) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    ) {
                        Text(
                            "${data.activeOrders} active orders",
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 7.dp
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                ) {
                    Text(
                        "₹${data.todayRevenue.toInt()} today",
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 7.dp
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun KpiGrid(data: DashboardStats) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiCard(
                modifier = Modifier.weight(1f),
                title = "Revenue",
                value = money(data.todayRevenue),
                accent = PizzaSuccess
            )

            KpiCard(
                modifier = Modifier.weight(1f),
                title = "Orders",
                value = data.todayOrders.toString(),
                accent = MaterialTheme.colorScheme.primary
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiCard(
                modifier = Modifier.weight(1f),
                title = "Active",
                value = data.activeOrders.toString(),
                accent = StatusPending
            )

            KpiCard(
                modifier = Modifier.weight(1f),
                title = "Avg. order",
                value = money(data.averageOrderValue),
                accent = StatusPreparing
            )
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(accent, RoundedCornerShape(50))
            )

            Spacer(Modifier.height(12.dp))

            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(4.dp))

            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RevenueChartCard(
    points: List<DashboardDailyPoint>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Revenue",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(12.dp))

            SimpleLineChart(
                values = points.map { it.revenue },
                labels = points.map { it.label },
                color = PizzaSuccess
            )
        }
    }
}

@Composable
private fun OrdersChartCard(
    points: List<DashboardDailyPoint>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Orders",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(12.dp))

            SimpleBarChart(
                values = points.map { it.orders.toFloat() },
                labels = points.map { it.label },
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SimpleLineChart(
    values: List<Double>,
    labels: List<String>,
    color: Color
) {
    val safeValues = if (values.isEmpty()) listOf(0.0) else values
    val maximum = max(safeValues.maxOrNull() ?: 0.0, 1.0)

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(175.dp)
        ) {
            val left = 8f
            val right = size.width - 8f
            val top = 12f
            val bottom = size.height - 10f
            val width = right - left
            val height = bottom - top

            repeat(4) { index ->
                val y = top + height * index / 3f
                drawLine(
                    color = color.copy(alpha = 0.10f),
                    start = Offset(left, y),
                    end = Offset(right, y),
                    strokeWidth = 1f
                )
            }

            if (safeValues.size < 2) return@Canvas

            val path = Path()

            safeValues.forEachIndexed { index, value ->
                val x = left + width * index / safeValues.lastIndex
                val y = bottom - (value / maximum).toFloat() * height

                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = 5f,
                    cap = StrokeCap.Round
                )
            )

            safeValues.forEachIndexed { index, value ->
                val x = left + width * index / safeValues.lastIndex
                val y = bottom - (value / maximum).toFloat() * height

                drawCircle(
                    color = color,
                    radius = 4f,
                    center = Offset(x, y)
                )
            }
        }

        if (labels.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                labels.forEach { label ->
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SimpleBarChart(
    values: List<Float>,
    labels: List<String>,
    color: Color
) {
    val maximum = max(values.maxOrNull() ?: 0f, 1f)

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(175.dp)
        ) {
            val slotWidth = size.width / values.coerceAtLeastOne()
            val barWidth = slotWidth * 0.50f
            val bottom = size.height - 10f
            val chartHeight = size.height - 22f

            values.forEachIndexed { index, value ->
                val barHeight = value / maximum * chartHeight
                val left = index * slotWidth + (slotWidth - barWidth) / 2f

                drawRoundRect(
                    color = color,
                    topLeft = Offset(
                        left,
                        bottom - barHeight
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        barWidth,
                        barHeight
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                        14f,
                        14f
                    )
                )
            }
        }

        if (labels.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                labels.forEach { label ->
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun List<Float>.coerceAtLeastOne(): Int =
    size.coerceAtLeast(1)

@Composable
private fun OrderPipelineCard(
    data: DashboardStats
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PipelineRow("Pending", data.pendingOrders, StatusPending)
        PipelineRow("Confirmed", data.confirmedOrders, StatusConfirmed)
        PipelineRow("Preparing", data.preparingOrders, StatusPreparing)
        PipelineRow("Ready", data.readyOrders, StatusReady)
    }
}

@Composable
private fun PipelineRow(
    label: String,
    count: Int,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 15.dp,
                    vertical = 13.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, RoundedCornerShape(50))
            )

            Spacer(Modifier.width(10.dp))

            Text(
                label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                count.toString(),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RecentOrdersCard(
    orders: List<Order>,
    onOpenOrders: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (orders.isEmpty()) {
                Text(
                    "No orders yet.",
                    modifier = Modifier.padding(8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                orders.take(4).forEach { order ->
                    RecentOrderRow(order)
                }

                Spacer(Modifier.height(4.dp))

                FilterChip(
                    selected = false,
                    onClick = onOpenOrders,
                    label = {
                        Text("View all orders")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun RecentOrderRow(
    order: Order
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 8.dp,
                vertical = 10.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                order.customer.name.ifBlank { "Customer" },
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "#${shortOrderId(order.orderId)} • ${order.totalItems} items",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                money(order.grandTotal),
                fontWeight = FontWeight.Bold
            )
            Text(
                dashboardStatusLabel(order.status),
                style = MaterialTheme.typography.labelSmall,
                color = dashboardStatusColor(order.status)
            )
        }
    }
}

@Composable
private fun MenuHealthCard(
    data: DashboardStats
) {
    val total = data.totalItems.coerceAtLeast(1)
    val availableFraction =
        (data.availableItems.toFloat() / total).coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HealthValue(
                    label = "Available",
                    value = data.availableItems,
                    color = PizzaSuccess
                )
                HealthValue(
                    label = "Unavailable",
                    value = data.unavailableItems,
                    color = MaterialTheme.colorScheme.error
                )
                HealthValue(
                    label = "Categories",
                    value = data.totalCategories,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(50)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(availableFraction)
                        .height(9.dp)
                        .background(
                            PizzaSuccess,
                            RoundedCornerShape(50)
                        )
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "${data.availableItems} of ${data.totalItems} menu items are available",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HealthValue(
    label: String,
    value: Int,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuickActionsGrid(
    onOpenOrders: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenMenu: () -> Unit,
    onOpenOffers: () -> Unit,
    onOpenUpcomingEvents: () -> Unit,
    onOpenBroadcast: () -> Unit,
    onOpenCoupons: () -> Unit,
    onOpenShopSettings: () -> Unit
) {
    val actions = listOf(
        QuickAction("Orders", Icons.Filled.ReceiptLong, onOpenOrders),
        QuickAction("Analytics", Icons.Filled.BarChart, onOpenAnalytics),
        QuickAction("Menu", Icons.Filled.RestaurantMenu, onOpenMenu),
        QuickAction("Categories", Icons.Filled.Category, onOpenCategories),
        QuickAction("Offers", Icons.Filled.Campaign, onOpenOffers),
        QuickAction("Coupons", Icons.Filled.LocalOffer, onOpenCoupons),
        QuickAction("Events", Icons.Filled.Cake, onOpenUpcomingEvents),
        QuickAction("Broadcast", Icons.AutoMirrored.Filled.Send, onOpenBroadcast),
        QuickAction("Settings", Icons.Filled.Settings, onOpenShopSettings)
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        actions.chunked(3).forEach { rowActions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowActions.forEach { action ->
                    QuickActionCard(
                        action = action,
                        modifier = Modifier.weight(1f)
                    )
                }

                repeat(3 - rowActions.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

private data class QuickAction(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun QuickActionCard(
    action: QuickAction,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        ),
        onClick = action.onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 10.dp,
                    vertical = 14.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                action.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(23.dp)
            )

            Spacer(Modifier.height(7.dp))

            Text(
                action.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    subtitle: String
) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DashboardEmptyState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "No dashboard data available yet.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun shortOrderId(id: String): String {
    if (id.length <= 8) return id
    return id.takeLast(8)
}

private fun dashboardStatusLabel(status: OrderStatus): String =
    when (status) {
        OrderStatus.BEING_PAYMENT -> "Payment processing"
        OrderStatus.PENDING -> "Pending"
        OrderStatus.CONFIRMED -> "Confirmed"
        OrderStatus.PREPARING -> "Preparing"
        OrderStatus.READY -> "Ready"
        OrderStatus.COMPLETED -> "Completed"
        OrderStatus.CANCELLED -> "Cancelled"
    }

private fun dashboardStatusColor(status: OrderStatus): Color =
    when (status) {
        OrderStatus.BEING_PAYMENT -> StatusPending
        OrderStatus.PENDING -> StatusPending
        OrderStatus.CONFIRMED -> StatusConfirmed
        OrderStatus.PREPARING -> StatusPreparing
        OrderStatus.READY -> StatusReady
        OrderStatus.COMPLETED -> PizzaSuccess
        OrderStatus.CANCELLED -> StatusCancelled
    }
