package com.pizzatown.admin.presentation.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pizzatown.admin.core.common.UiState
import com.pizzatown.admin.domain.model.Order
import com.pizzatown.admin.domain.model.OrderStatus
import com.pizzatown.admin.domain.model.PaymentMethod
import com.pizzatown.admin.domain.model.PaymentStatus
import com.pizzatown.admin.presentation.components.ErrorView
import com.pizzatown.admin.presentation.components.LoadingView
import com.pizzatown.admin.ui.theme.LightWarning
import com.pizzatown.admin.ui.theme.PizzaSuccess
import com.pizzatown.admin.ui.theme.StatusCancelled
import com.pizzatown.admin.ui.theme.StatusConfirmed
import com.pizzatown.admin.ui.theme.StatusPending
import com.pizzatown.admin.ui.theme.StatusPreparing
import com.pizzatown.admin.ui.theme.StatusReady
import android.content.Intent
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsScreen(
    orderId: String,
    onBack: () -> Unit,
    viewModel: OrdersViewModel = hiltViewModel()
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
            is UiState.Loading -> LoadingView()
            is UiState.Error -> ErrorView(current.message)
            is UiState.Empty -> ErrorView("Order not found.")
            is UiState.Success -> {
                val order = current.data.firstOrNull { it.orderId == orderId }
                if (order == null) {
                    ErrorView("Order not found.")
                } else {
                    OrderDetailsContent(
                        modifier = Modifier.padding(padding),
                        order = order,
                        onAdvance = { viewModel.advanceStatus(order) },
                        onCancel = { viewModel.cancelOrder(order) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderDetailsContent(
    modifier: Modifier,
    order: Order,
    onAdvance: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val phone = order.customer.phone.trim()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
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
                    MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "#${shortOrderId(order.orderId)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = order.customer.name.ifBlank { "Customer" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = formatDate(order.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    StatusChip(order.status)
                }
            }
        }

        item {
            SectionTitle("Customer", Icons.Filled.Person)

            DetailRow("Name", order.customer.name.ifBlank { "—" })

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DetailRow(
                    label = "Phone",
                    value = phone.ifBlank { "—" },
                    modifier = Modifier.weight(1f)
                )

                if (phone.isNotBlank()) {
                    IconButton(
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_DIAL,
                                    Uri.parse("tel:$phone")
                                )
                            )
                        }
                    ) {
                        Icon(
                            Icons.Filled.Phone,
                            contentDescription = "Call customer"
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            DetailRow("Address", order.customer.address.ifBlank { "—" })

            if (
                order.status == OrderStatus.DELIVERED ||
                order.status == OrderStatus.COMPLETED
            ) {
                Spacer(Modifier.height(10.dp))
                DetailRow(
                    label = "Delivered by",
                    value = order.deliveredByName.ifBlank { "Delivery Partner" }
                )
            }
        }

        item {
            SectionTitle("Items", Icons.Filled.RestaurantMenu)

            if (order.items.isEmpty()) {
                Text(
                    text = "No item details available.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    order.items.forEachIndexed { index, item ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                            color = if (index % 2 == 0) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    item.variantName
                                        ?.takeIf { it.isNotBlank() }
                                        ?.let {
                                            Text(
                                                text = it,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                    if (item.customizationNames.isNotEmpty()) {
                                        Spacer(Modifier.height(3.dp))
                                        Text(
                                            text = item.customizationNames.joinToString(" • "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(Modifier.height(5.dp))

                                    Text(
                                        text = "₹${item.unitPrice.toInt()} × ${item.quantity}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(Modifier.width(12.dp))

                                Text(
                                    text = "₹${item.lineTotal.toInt()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionTitle("Bill summary", Icons.Filled.Payments)
            BillRow("Subtotal", order.subtotal)

            if (order.discount > 0.0) {
                BillRow(
                    "Discount",
                    -order.discount,
                    MaterialTheme.colorScheme.tertiary
                )
            }

            if (order.couponCode.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Coupon: ${order.couponCode}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            BillRow("Delivery fee", order.deliveryFee)
            BillRow("Tax", order.tax)

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Grand total",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "₹${order.grandTotal.toInt()}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (order.specialInstructions.isNotBlank()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "Special instructions",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            order.specialInstructions,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        if (
            order.paymentMethod == PaymentMethod.ONLINE &&
            (
                order.cashfreeOrderId.isNotBlank() ||
                order.cashfreePaymentId.isNotBlank()
            )
        ) {
            item {
                SectionTitle("Payment details", Icons.Filled.Payments)

                if (order.cashfreeOrderId.isNotBlank()) {
                    DetailRow("Cashfree order", order.cashfreeOrderId)
                }

                if (order.cashfreePaymentId.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    DetailRow("Payment ID", order.cashfreePaymentId)
                }
            }
        }

        item {
            val next = order.nextStatus()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (next != null) {
                    Button(
                        onClick = onAdvance,
                        modifier = Modifier.weight(1f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Mark as ${
                                next.name.lowercase().replaceFirstChar { it.uppercase() }
                            }"
                        )
                    }
                }

                if (order.canCancel()) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel Order")
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(19.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            modifier = Modifier.width(110.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun BillRow(
    label: String,
    amount: Double,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (amount < 0) {
                "-₹${kotlin.math.abs(amount).toInt()}"
            } else {
                "₹${amount.toInt()}"
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
private fun StatusChip(status: OrderStatus) {
    val (label, color) = when (status) {
        OrderStatus.BEING_PAYMENT -> "Payment Processing" to LightWarning
        OrderStatus.PENDING -> "Pending" to StatusPending
        OrderStatus.CONFIRMED -> "Confirmed" to StatusConfirmed
        OrderStatus.PREPARING -> "Preparing" to StatusPreparing
        OrderStatus.READY -> "Ready" to StatusReady
        OrderStatus.ON_THE_WAY -> "Out for delivery" to StatusConfirmed
        OrderStatus.DELIVERED -> "Delivered" to PizzaSuccess
        OrderStatus.COMPLETED -> "Completed" to PizzaSuccess
        OrderStatus.CANCELLED -> "Cancelled" to StatusCancelled
    }

    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
        color = color.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.28f))
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

private fun shortOrderId(id: String): String =
    id.takeLast(8).uppercase()

private fun formatDate(timestamp: Long): String =
    if (timestamp <= 0L) {
        "Date unavailable"
    } else {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            .format(Date(timestamp))
    }
