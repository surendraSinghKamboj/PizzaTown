package com.pizzatown.customer.presentation.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pizzatown.customer.core.common.UiState
import com.pizzatown.customer.domain.model.Order
import com.pizzatown.customer.domain.model.OrderStatus
import com.pizzatown.customer.domain.model.PaymentMethod
import com.pizzatown.customer.domain.model.PaymentStatus
import com.pizzatown.customer.presentation.components.ErrorView
import com.pizzatown.customer.presentation.components.LoadingView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsScreen(
    orderId: String,
    onBack: () -> Unit,
    viewModel: OrderHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.ordersState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order Details") },
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
            is UiState.Loading -> LoadingView(modifier = Modifier.padding(padding))
            is UiState.Error -> ErrorView(
                current.message,
                modifier = Modifier.padding(padding)
            )
            is UiState.Empty -> ErrorView(
                "Order not found.",
                modifier = Modifier.padding(padding)
            )
            is UiState.Success -> {
                val order = current.data.firstOrNull { it.orderId == orderId }
                if (order == null) {
                    ErrorView(
                        "Order not found.",
                        modifier = Modifier.padding(padding)
                    )
                } else {
                    OrderDetailsContent(
                        modifier = Modifier.padding(padding),
                        order = order
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderDetailsContent(
    modifier: Modifier,
    order: Order
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 12.dp,
            end = 16.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ) {
                        Icon(
                            Icons.Filled.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(52.dp)
                                .padding(13.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Order #${order.orderId.takeLast(6).uppercase()}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            formatDate(order.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                        OrderStatusBadge(
                            status = order.status,
                            color = orderStatusColor(order.status)
                        )
                    }
                }
            }
        }

        item {
            SectionTitle("Order details")

            order.items.forEachIndexed { index, item ->
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(9.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                "${item.quantity}×",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(
                                    horizontal = 9.dp,
                                    vertical = 7.dp
                                )
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        Column(Modifier.weight(1f)) {
                            Text(
                                item.name +
                                    (item.variantName?.let { "  •  $it" } ?: ""),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            if (item.customizationNames.isNotEmpty()) {
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    item.customizationNames.joinToString(", "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        Text(
                            "₹${item.lineTotal.toInt()}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (index != order.items.lastIndex) {
                    Spacer(Modifier.height(7.dp))
                }
            }
        }

        if (order.specialInstructions.isNotBlank()) {
            item {
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "Special instructions",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            order.specialInstructions,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(Modifier.width(8.dp))

                    Column {
                        Text(
                            "Delivered to",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(2.dp))

                        Text(
                            order.customer.address.ifBlank { "Address not available" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (
                            order.status == OrderStatus.DELIVERED ||
                            order.status == OrderStatus.COMPLETED
                        ) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Delivered by",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(2.dp))

                            Text(
                                order.deliveredByName.ifBlank { "Delivery Partner" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionTitle("Bill summary")
            BillRow("Subtotal", "₹${order.subtotal.toInt()}")

            if (order.discount > 0) {
                Spacer(Modifier.height(5.dp))
                BillRow(
                    if (order.couponCode.isNotBlank())
                        "Discount (${order.couponCode})"
                    else
                        "Discount",
                    "-₹${order.discount.toInt()}",
                    highlight = true
                )
            }

            if (order.deliveryFee > 0) {
                Spacer(Modifier.height(5.dp))
                BillRow("Delivery Fee", "₹${order.deliveryFee.toInt()}")
            }

            if (order.tax > 0) {
                Spacer(Modifier.height(5.dp))
                BillRow("Tax", "₹${order.tax.toInt()}")
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Grand Total",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "₹${order.grandTotal.toInt()}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(8.dp))
            PaymentLabel(order.paymentMethod, order.paymentStatus)
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
    )
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
        PaymentMethod.COD -> "Cash on Delivery"
        PaymentMethod.ONLINE -> when (status) {
            PaymentStatus.PAID -> "Paid online"
            PaymentStatus.PENDING -> "Online payment pending"
            PaymentStatus.FAILED -> "Online payment failed"
            PaymentStatus.CANCELLED -> "Online payment cancelled"
            PaymentStatus.NOT_REQUIRED -> "Online"
        }
    }

    val color = when {
        method == PaymentMethod.ONLINE && status == PaymentStatus.PAID ->
            MaterialTheme.colorScheme.primary
        method == PaymentMethod.ONLINE &&
            (status == PaymentStatus.FAILED || status == PaymentStatus.CANCELLED) ->
            MaterialTheme.colorScheme.error
        method == PaymentMethod.ONLINE && status == PaymentStatus.PENDING ->
            MaterialTheme.colorScheme.tertiary
        else ->
            MaterialTheme.colorScheme.onSurfaceVariant
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun OrderStatusBadge(
    status: OrderStatus,
    color: Color
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
        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.28f))
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

private fun orderStatusColor(status: OrderStatus): Color =
    when (status) {
        OrderStatus.BEING_PAYMENT -> Color(0xFFFFB300)
        OrderStatus.PENDING -> Color(0xFFFFA726)
        OrderStatus.CONFIRMED -> Color(0xFF42A5F5)
        OrderStatus.PREPARING -> Color(0xFFAB69C6)
        OrderStatus.READY -> Color(0xFF26A69A)
        OrderStatus.ON_THE_WAY -> Color(0xFF42A5F5)
        OrderStatus.DELIVERED -> Color(0xFF4CAF50)
        OrderStatus.COMPLETED -> Color(0xFF4CAF50)
        OrderStatus.CANCELLED -> Color(0xFFEF5350)
    }

private fun formatDate(epochMillis: Long): String {
    if (epochMillis <= 0L) return ""
    return SimpleDateFormat(
        "d MMM yyyy, h:mm a",
        Locale.getDefault()
    ).format(Date(epochMillis))
}
