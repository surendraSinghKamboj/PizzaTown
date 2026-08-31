package com.pizzatown.admin.presentation.analytics
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AssistChip
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf







import androidx.compose.material.icons.filled.DateRange

import androidx.compose.material.icons.filled.CalendarMonth




import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pizzatown.admin.ui.theme.PizzaSuccess
import com.pizzatown.admin.ui.theme.StatusPreparing
import com.pizzatown.admin.core.common.UiState
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
import java.text.SimpleDateFormat

private val currencyFormat: NumberFormat =
    NumberFormat.getCurrencyInstance(Locale("en", "IN"))

private fun money(value: Double): String = currencyFormat.format(value)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val state by viewModel.analyticsState.collectAsStateWithLifecycle()
    val selectedRange by viewModel.selectedRange.collectAsStateWithLifecycle()
    val customDateRange by viewModel.customDateRange.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Analytics",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Business performance",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        when (val current = state) {
            is UiState.Loading ->
                LoadingView(Modifier.padding(padding))

            is UiState.Error ->
                ErrorView(
                    current.message,
                    modifier = Modifier.padding(padding)
                )

            is UiState.Empty ->
                EmptyAnalyticsMessage(Modifier.padding(padding))

            is UiState.Success ->
                AnalyticsContent(
                    data = current.data,
                    modifier = Modifier.padding(padding),
                    selectedRange = selectedRange,
                    customDateRange = customDateRange,
                    onRangeSelected = viewModel::selectRange,
                    onCustomRangeSelected = viewModel::setCustomDateRange
                )
        }
    }
}

@Composable
private fun EmptyAnalyticsMessage(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.BarChart,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "No analytics yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Analytics will appear once orders start coming in.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AnalyticsContent(
    data: SalesAnalytics,
    modifier: Modifier = Modifier,
    selectedRange: AnalyticsRange,
    customDateRange: AnalyticsDateRange,
    onRangeSelected: (AnalyticsRange) -> Unit,
    onCustomRangeSelected: (Long, Long) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            AnalyticsRangeSelector(
                selectedRange = selectedRange,
                customDateRange = customDateRange,
                onRangeSelected = onRangeSelected,
                onCustomRangeSelected = onCustomRangeSelected
            )
        }

        item {
            HeroRevenueCard(data)
        }

        item {
            SectionTitle(
                title = "Revenue trend",
                subtitle = "Completed orders • last 7 days"
            )
        }

        item {
            RevenueTrendCard(data.dailySales)
        }

        item {
            SectionTitle(
                title = "Order activity",
                subtitle = "All visible orders • last 7 days"
            )
        }

        item {
            OrdersTrendCard(data.dailySales)
        }

        item {
            SectionTitle(
                title = "Order health",
                subtitle = "Current order pipeline"
            )
        }

        item {
            OrderHealthCard(data.orderStatusCounts)
        }

        item {
            SectionTitle(
                title = "Payments",
                subtitle = "Order payment methods"
            )
        }

        item {
            PaymentBreakdownCard(data.paymentMethodCounts)
        }

        item {
            SectionTitle(
                title = "Performance",
                subtitle = "Key business indicators"
            )
        }

        item {
            PerformanceGrid(data)
        }

        item {
            SectionTitle(
                title = "Best sellers",
                subtitle = "Top items by units sold"
            )
        }

        if (data.topSellingItems.isEmpty()) {
            item {
                SimpleInfoCard("No completed orders yet.")
            }
        } else {
            items(
                data.topSellingItems,
                key = { it.name }
            ) { item ->
                TopSellerCard(item)
            }
        }

        item {
            SectionTitle(
                title = "Promotions",
                subtitle = "Coupon performance"
            )
        }

        item {
            PromotionCard(data)
        }

        item {
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnalyticsRangeSelector(
    selectedRange: AnalyticsRange,
    customDateRange: AnalyticsDateRange,
    onRangeSelected: (AnalyticsRange) -> Unit,
    onCustomRangeSelected: (Long, Long) -> Unit
) {
    val showPicker = mutableStateOf(false)

    val pickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = customDateRange.start,
        initialSelectedEndDateMillis = customDateRange.end
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnalyticsRange.entries.forEach { range ->
                FilterChip(
                    selected = selectedRange == range,
                    onClick = {
                        if (range == AnalyticsRange.CUSTOM) {
                            showPicker.value = true
                        } else {
                            onRangeSelected(range)
                        }
                    },
                    label = {
                        Text(
                            when (range) {
                                AnalyticsRange.TODAY -> "Today"
                                AnalyticsRange.THIS_WEEK -> "This Week"
                                AnalyticsRange.LAST_7_DAYS -> "Last 7 Days"
                                AnalyticsRange.THIS_MONTH -> "This Month"
                                AnalyticsRange.LAST_30_DAYS -> "Last 30 Days"
                                AnalyticsRange.ALL_TIME -> "All Time"
                                AnalyticsRange.CUSTOM -> "Custom"
                            }
                        )
                    }
                )
            }
        }

        Text(
            when (selectedRange) {
                AnalyticsRange.TODAY -> "Showing today"
                AnalyticsRange.THIS_WEEK -> "Showing current week"
                AnalyticsRange.LAST_7_DAYS -> "Showing the last 7 days"
                AnalyticsRange.THIS_MONTH -> "Showing current month"
                AnalyticsRange.LAST_30_DAYS -> "Showing the last 30 days"
                AnalyticsRange.ALL_TIME -> "Showing all available data"
                AnalyticsRange.CUSTOM -> "Showing selected date range"
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (
            selectedRange == AnalyticsRange.CUSTOM &&
            customDateRange.start != null &&
            customDateRange.end != null
        ) {
            val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

            val startText = formatter.format(
                java.util.Date(customDateRange.start!!)
            )

            val endText = formatter.format(
                java.util.Date(customDateRange.end!!)
            )

            AssistChip(
                onClick = {
                    showPicker.value = true
                },
                label = {
                    Text("$startText  →  $endText")
                },
                leadingIcon = {
                    Icon(
                        Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }

        if (showPicker.value) {
            DatePickerDialog(
                onDismissRequest = {
                    showPicker.value = false
                },
                confirmButton = {
                    TextButton(
                        enabled =
                            pickerState.selectedStartDateMillis != null &&
                            pickerState.selectedEndDateMillis != null,
                        onClick = {
                            val start =
                                pickerState.selectedStartDateMillis
                            val end =
                                pickerState.selectedEndDateMillis

                            if (start != null && end != null) {
                                onCustomRangeSelected(start, end)
                                showPicker.value = false
                            }
                        }
                    ) {
                        Text("Apply")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showPicker.value = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            ) {
                DateRangePicker(
                    state = pickerState,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun HeroRevenueCard(data: SalesAnalytics) {
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
                "Revenue today",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(Modifier.height(4.dp))

            Text(
                money(data.revenueToday),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MiniMetric(
                    modifier = Modifier.weight(1f),
                    label = "Last 7 days",
                    value = money(data.revenueThisWeek)
                )
                MiniMetric(
                    modifier = Modifier.weight(1f),
                    label = "Last 30 days",
                    value = money(data.revenueThisMonth)
                )
                MiniMetric(
                    modifier = Modifier.weight(1f),
                    label = "All time",
                    value = money(data.revenueAllTime)
                )
            }
        }
    }
}

@Composable
private fun MiniMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(3.dp))
            Text(
                value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RevenueTrendCard(
    points: List<DailySalesPoint>
) {
    ChartCard {
        SimpleLineChart(
            values = points.map { it.revenue },
            labels = points.map { it.label },
            lineColor = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun OrdersTrendCard(
    points: List<DailySalesPoint>
) {
    ChartCard {
        SimpleBarChart(
            values = points.map { it.orders.toFloat() },
            labels = points.map { it.label },
            barColor = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ChartCard(
    content: @Composable ColumnScope.() -> Unit
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
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
private fun SimpleLineChart(
    values: List<Double>,
    labels: List<String>,
    lineColor: Color
) {
    val safeValues = if (values.isEmpty()) listOf(0.0) else values
    val maxValue = max(safeValues.maxOrNull() ?: 0.0, 1.0)

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        ) {
            val left = 10f
            val right = size.width - 10f
            val top = 12f
            val bottom = size.height - 20f
            val width = right - left
            val height = bottom - top

            repeat(4) { index ->
                val y = top + (height * index / 3f)
                drawLine(
                    color = lineColor.copy(alpha = 0.10f),
                    start = Offset(left, y),
                    end = Offset(right, y),
                    strokeWidth = 1f
                )
            }

            if (safeValues.size == 1) return@Canvas

            val path = Path()

            safeValues.forEachIndexed { index, value ->
                val x = left + width * index / (safeValues.lastIndex.coerceAtLeast(1))
                val y = bottom - (value / maxValue).toFloat() * height

                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }

                drawCircle(
                    color = lineColor,
                    radius = 4.5f,
                    center = Offset(x, y)
                )
            }

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(
                    width = 5f,
                    cap = StrokeCap.Round
                )
            )
        }

        if (labels.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                labels.forEach {
                    Text(
                        text = it,
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
    barColor: Color
) {
    val maxValue = max(values.maxOrNull() ?: 0f, 1f)

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        ) {
            val chartBottom = size.height - 10f
            val chartTop = 12f
            val chartHeight = chartBottom - chartTop
            val count = values.size.coerceAtLeast(1)
            val slotWidth = size.width / count
            val barWidth = slotWidth * 0.52f

            values.forEachIndexed { index, value ->
                val barHeight = (value / maxValue) * chartHeight
                val left = index * slotWidth + (slotWidth - barWidth) / 2f

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(
                        left,
                        chartBottom - barHeight
                    ),
                    size = Size(
                        width = barWidth,
                        height = barHeight
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                        x = 14f,
                        y = 14f
                    )
                )
            }
        }

        if (labels.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                labels.forEach {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderHealthCard(
    counts: List<OrderStatusCount>
) {
    val total = counts.sumOf { it.count }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        counts.forEach { entry ->
            StatusProgressRow(
                label = statusLabel(entry.status),
                count = entry.count,
                total = total,
                color = statusColor(entry.status)
            )
        }
    }
}

@Composable
private fun StatusProgressRow(
    label: String,
    count: Int,
    total: Int,
    color: Color
) {
    val fraction = if (total > 0) count.toFloat() / total else 0f

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(color, CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                count.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(5.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(50)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(8.dp)
                    .background(color, RoundedCornerShape(50))
            )
        }
    }
}

@Composable
private fun PaymentBreakdownCard(
    counts: List<PaymentMethodCount>
) {
    val total = counts.sumOf { it.count }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        counts.forEachIndexed { index, entry ->
            PaymentMethodCard(
                modifier = Modifier.weight(1f),
                label = entry.label,
                count = entry.count,
                total = total,
                color = if (index == 0) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondary
                }
            )
        }
    }
}

@Composable
private fun PaymentMethodCard(
    label: String,
    count: Int,
    total: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val fraction = if (total > 0) count.toFloat() / total else 0f

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                Icons.Filled.Payments,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )

            Spacer(Modifier.height(9.dp))

            Text(
                count.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(50)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .height(6.dp)
                        .background(color, RoundedCornerShape(50))
                )
            }
        }
    }
}

@Composable
private fun PerformanceGrid(
    data: SalesAnalytics
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PerformanceCard(
                modifier = Modifier.weight(1f),
                label = "Completed orders",
                value = data.completedOrderCount.toString(),
                icon = Icons.Filled.CheckCircle,
                color = PizzaSuccess
            )

            PerformanceCard(
                modifier = Modifier.weight(1f),
                label = "Average order",
                value = money(data.averageOrderValue),
                icon = Icons.Filled.Payments,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PerformanceCard(
                modifier = Modifier.weight(1f),
                label = "Cancelled",
                value = data.cancelledOrderCount.toString(),
                icon = Icons.Filled.TrendingDown,
                color = MaterialTheme.colorScheme.error
            )

            PerformanceCard(
                modifier = Modifier.weight(1f),
                label = "Revenue lost",
                value = money(data.cancelledRevenueLost),
                icon = Icons.Filled.TrendingDown,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun PerformanceCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(21.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TopSellerCard(
    item: TopSellingItem
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    item.quantitySold.toString(),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${item.quantitySold} sold",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                money(item.revenue),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PromotionCard(
    data: SalesAnalytics
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.LocalOffer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "${data.couponsRedeemed} coupons redeemed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Discount given: ${money(data.couponDiscountGiven)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
private fun SimpleInfoCard(
    text: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun statusLabel(status: OrderStatus): String =
    when (status) {
        OrderStatus.BEING_PAYMENT -> "Payment processing"
        OrderStatus.PENDING -> "Pending"
        OrderStatus.CONFIRMED -> "Confirmed"
        OrderStatus.PREPARING -> "Preparing"
        OrderStatus.READY -> "Ready"
        OrderStatus.COMPLETED -> "Completed"
        OrderStatus.CANCELLED -> "Cancelled"
    }

private fun statusColor(status: OrderStatus): Color =
    when (status) {
        OrderStatus.BEING_PAYMENT -> StatusPending
        OrderStatus.PENDING -> StatusPending
        OrderStatus.CONFIRMED -> StatusConfirmed
        OrderStatus.PREPARING -> StatusPreparing
        OrderStatus.READY -> StatusReady
        OrderStatus.COMPLETED -> PizzaSuccess
        OrderStatus.CANCELLED -> StatusCancelled
    }

