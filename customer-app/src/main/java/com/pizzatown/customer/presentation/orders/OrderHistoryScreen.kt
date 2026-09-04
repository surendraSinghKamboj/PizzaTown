package com.pizzatown.customer.presentation.orders

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pizzatown.customer.core.common.UiState
import com.pizzatown.customer.domain.model.Order
import com.pizzatown.customer.domain.model.OrderStatus
import com.pizzatown.customer.domain.model.PaymentMethod
import com.pizzatown.customer.domain.model.PaymentStatus
import com.pizzatown.customer.presentation.components.EmptyView
import com.pizzatown.customer.presentation.components.ErrorView
import com.pizzatown.customer.presentation.components.LoadingView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    onBack: () -> Unit,
    onOpenOrder: (String) -> Unit,
    viewModel: OrderHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.ordersState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "My Orders",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Your delicious order history",
                            style = MaterialTheme.typography.labelMedium,
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

            is UiState.Loading -> {
                LoadingView(
                    Modifier.padding(padding)
                )
            }

            is UiState.Error -> {
                ErrorView(
                    current.message,
                    modifier = Modifier.padding(padding)
                )
            }

            is UiState.Empty -> {
                EmptyView(
                    "You haven't placed any orders yet.",
                    Modifier.padding(padding)
                )
            }

            is UiState.Success -> {

                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),

                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    ),

                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    item {
                        Text(
                            "${current.data.size} order" +
                                if (current.data.size == 1) "" else "s",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                horizontal = 4.dp,
                                vertical = 2.dp
                            )
                        )
                    }

                    items(
                        current.data,
                        key = { it.orderId }
                    ) { order ->

                        OrderCard(
                            order = order,
                            onOpen = { onOpenOrder(order.orderId) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderCard(
    order: Order,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = orderStatusColor(order.status)

    Card(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(
            Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ) {
                    Icon(
                        Icons.Filled.Storefront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(46.dp)
                            .padding(11.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    Modifier.weight(1f)
                ) {
                    Text(
                        "Order #${order.orderId.takeLast(6).uppercase()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(3.dp))

                    Text(
                        formatDate(order.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(5.dp))

                    Text(
                        "${order.totalItems} item" +
                            if (order.totalItems == 1) "" else "s" +
                            "  •  ₹${order.grandTotal.toInt()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.width(8.dp))

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    OrderStatusBadge(
                        status = order.status,
                        color = statusColor
                    )

                    Spacer(Modifier.height(7.dp))

                    Text(
                        "View details",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
            )

            Spacer(Modifier.height(11.dp))

            PaymentLabel(
                order.paymentMethod,
                order.paymentStatus
            )
        }
    }
}

@Composable
private fun OrderStatusBadge(
    status: OrderStatus,
    color: androidx.compose.ui.graphics.Color
) {
    val label = when (status) {
        OrderStatus.BEING_PAYMENT -> "Payment Processing"
        OrderStatus.PENDING -> "Pending"
        OrderStatus.CONFIRMED -> "Confirmed"
        OrderStatus.PREPARING -> "Preparing"
        OrderStatus.READY -> "Ready"
        OrderStatus.ON_THE_WAY -> "Out for delivery"
        OrderStatus.DELIVERED -> "Delivered"
        OrderStatus.COMPLETED -> "Completed"
        OrderStatus.CANCELLED -> "Cancelled"
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            color.copy(alpha = 0.28f)
        )
    ) {
        Text(
            label,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 7.dp
            )
        )
    }
}

@Composable
private fun BillRow(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (highlight)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface
        )

        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (highlight)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface,
            fontWeight = if (highlight)
                FontWeight.SemiBold
            else
                FontWeight.Normal
        )
    }
}

@Composable
private fun PaymentLabel(
    method: PaymentMethod,
    status: PaymentStatus
) {
    val text = when (method) {
        PaymentMethod.COD ->
            "Cash on Delivery"

        PaymentMethod.ONLINE -> when (status) {
            PaymentStatus.PAID ->
                "Paid online"

            PaymentStatus.PENDING ->
                "Online payment pending"

            PaymentStatus.FAILED ->
                "Online payment failed"

            PaymentStatus.CANCELLED ->
                "Online payment cancelled"

            PaymentStatus.NOT_REQUIRED ->
                "Online"
        }
    }

    val color = when {
        method == PaymentMethod.ONLINE &&
            status == PaymentStatus.PAID ->
            androidx.compose.ui.graphics.Color(0xFF4CAF50)

        method == PaymentMethod.ONLINE &&
            status == PaymentStatus.FAILED ->
            MaterialTheme.colorScheme.error

        method == PaymentMethod.ONLINE &&
            status == PaymentStatus.CANCELLED ->
            MaterialTheme.colorScheme.error

        method == PaymentMethod.ONLINE &&
            status == PaymentStatus.PENDING ->
            androidx.compose.ui.graphics.Color(0xFFFFB300)

        else ->
            MaterialTheme.colorScheme.onSurfaceVariant
    }

    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun orderStatusColor(
    status: OrderStatus
): Color {
    return when (status) {
        OrderStatus.BEING_PAYMENT ->
            androidx.compose.ui.graphics.Color(0xFFFFB300)

        OrderStatus.PENDING ->
            androidx.compose.ui.graphics.Color(0xFFFFA726)

        OrderStatus.CONFIRMED ->
            androidx.compose.ui.graphics.Color(0xFF42A5F5)

        OrderStatus.PREPARING ->
            androidx.compose.ui.graphics.Color(0xFFAB69C6)

        OrderStatus.READY ->
            androidx.compose.ui.graphics.Color(0xFF26A69A)

        OrderStatus.ON_THE_WAY ->
            androidx.compose.ui.graphics.Color(0xFF42A5F5)

        OrderStatus.DELIVERED ->
            androidx.compose.ui.graphics.Color(0xFF4CAF50)

        OrderStatus.COMPLETED ->
            androidx.compose.ui.graphics.Color(0xFF4CAF50)

        OrderStatus.CANCELLED ->
            androidx.compose.ui.graphics.Color(0xFFEF5350)
    }
}

private fun formatDate(epochMillis: Long): String {
    if (epochMillis <= 0L) return ""

    return SimpleDateFormat(
        "d MMM yyyy, h:mm a",
        Locale.getDefault()
    ).format(Date(epochMillis))
}

