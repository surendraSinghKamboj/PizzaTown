package com.pizzatown.admin.presentation.orders
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background

import androidx.compose.foundation.clickable

import androidx.compose.ui.platform.LocalContext

import android.net.Uri

import android.content.Intent

import androidx.compose.material.icons.filled.RestaurantMenu

import androidx.compose.ui.graphics.Color

import androidx.compose.material.icons.filled.ExpandLess

import androidx.compose.material.icons.filled.ExpandMore

import androidx.compose.material.icons.filled.Payments

import androidx.compose.material.icons.filled.Person

import androidx.compose.material.icons.filled.Phone

import androidx.compose.material.icons.filled.LocationOn

import com.pizzatown.admin.ui.theme.LightWarning

import androidx.compose.foundation.BorderStroke

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pizzatown.admin.ui.theme.PizzaSuccess
import com.pizzatown.admin.ui.theme.StatusPreparing
import com.pizzatown.admin.core.common.UiState
import com.pizzatown.admin.domain.model.Order
import com.pizzatown.admin.domain.model.OrderStatus
import com.pizzatown.admin.domain.model.PaymentMethod
import com.pizzatown.admin.domain.model.PaymentStatus
import com.pizzatown.admin.presentation.components.EmptyView
import com.pizzatown.admin.presentation.components.ErrorView
import com.pizzatown.admin.presentation.components.LoadingView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.pizzatown.admin.ui.theme.StatusPending
import com.pizzatown.admin.ui.theme.StatusConfirmed
import com.pizzatown.admin.ui.theme.StatusReady
import com.pizzatown.admin.ui.theme.StatusCancelled

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    onBack: () -> Unit,
    onOpenOrder: (String) -> Unit,
    viewModel: OrdersViewModel = hiltViewModel()
) {
    val state by viewModel.ordersState.collectAsStateWithLifecycle()
    val filter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val dateFilter by viewModel.dateFilter.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Orders") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::setSearchQuery,
                placeholder = {
                    Text("Search customer, phone or order ID")
                },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                viewModel.setSearchQuery("")
                            }
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 10.dp
                    )
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(OrderDateFilter.entries.toList()) { option ->
                    FilterChip(
                        selected = dateFilter == option,
                        onClick = { viewModel.setDateFilter(option) },
                        label = { Text(option.label) }
                    )
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(selected = filter == null, onClick = { viewModel.setStatusFilter(null) }, label = { Text("All") })
                }
                items(OrderStatus.entries.filter { it != OrderStatus.BEING_PAYMENT }) { status ->
                    FilterChip(
                        selected = filter == status,
                        onClick = { viewModel.setStatusFilter(status) },
                        label = { Text(status.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            when (val current = state) {
                is UiState.Loading -> LoadingView()
                is UiState.Error -> ErrorView(current.message)
                is UiState.Empty -> EmptyView(
                    if (searchQuery.isNotBlank() || filter != null || dateFilter != OrderDateFilter.ALL)
                        "No orders match these filters."
                    else "No orders here yet."
                )
                is UiState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(current.data, key = { it.orderId }) { order ->
                            AdminOrderCard(
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
}

@Composable
private fun AdminOrderCard(
    order: Order,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onOpen,
        shape = RoundedCornerShape(20.dp),
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "#${shortOrderId(order.orderId)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(3.dp))

                    Text(
                        text = order.customer.name.ifBlank { "Customer" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = formatDate(order.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusChip(order.status)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${order.totalItems} ${if (order.totalItems == 1) "item" else "items"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.width(8.dp))

                Text("•", color = MaterialTheme.colorScheme.outline)

                Spacer(Modifier.width(8.dp))

                Text(
                    text = "₹${order.grandTotal.toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.weight(1f))

                PaymentChip(order.paymentMethod, order.paymentStatus)

                Spacer(Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = "Open order details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val phone = order.customer.phone.trim()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(Modifier.width(8.dp))

                        Text(
                            text = order.customer.name.ifBlank { "Customer" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(Modifier.weight(1f))

                        Text(
                            text = if (phone.isNotBlank()) phone else "Phone unavailable",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(Modifier.width(8.dp))

                        Text(
                            text = order.customer.address.ifBlank { "Address not available" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderDetailSectionTitle(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
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
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            modifier = Modifier.width(90.dp),
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
        OrderStatus.BEING_PAYMENT ->
            "Payment Processing" to LightWarning

        OrderStatus.PENDING ->
            "Pending" to StatusPending

        OrderStatus.CONFIRMED ->
            "Confirmed" to StatusConfirmed

        OrderStatus.PREPARING ->
            "Preparing" to StatusPreparing

        OrderStatus.READY ->
            "Ready" to StatusReady

        OrderStatus.ON_THE_WAY ->
            "Out for delivery" to StatusConfirmed

        OrderStatus.DELIVERED ->
            "Delivered" to PizzaSuccess

        OrderStatus.COMPLETED ->
            "Completed" to PizzaSuccess

        OrderStatus.CANCELLED ->
            "Cancelled" to StatusCancelled
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.10f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            color.copy(alpha = 0.28f)
        )
    ) {
        Text(
            text = label,
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

/** Shows how the order was/will be paid — e.g. "COD" or "Online \u00b7 Paid" / "Online \u00b7 Pending". */
@Composable
private fun PaymentChip(method: PaymentMethod, status: PaymentStatus) {
    val label = when (method) {
        PaymentMethod.COD -> "COD"
        PaymentMethod.ONLINE -> when (status) {
            PaymentStatus.PAID -> "Online \u00B7 Paid"
            PaymentStatus.PENDING -> "Online \u00B7 Pending"
            PaymentStatus.FAILED -> "Online \u00B7 Failed"
            PaymentStatus.CANCELLED -> "Online \u00B7 Cancelled"
            PaymentStatus.NOT_REQUIRED -> "Online"
        }
    }
    val color = when {
        status == PaymentStatus.PAID ->
            MaterialTheme.colorScheme.tertiary

        status == PaymentStatus.FAILED ||
            status == PaymentStatus.CANCELLED ->
            MaterialTheme.colorScheme.error

        status == PaymentStatus.PENDING ->
            LightWarning

        method == PaymentMethod.COD ->
            MaterialTheme.colorScheme.primary

        else ->
            MaterialTheme.colorScheme.onSurfaceVariant
    }
    AssistChip(onClick = {}, enabled = false, label = { Text(label) }, colors = AssistChipDefaults.assistChipColors(disabledLabelColor = color))
}

private fun shortOrderId(id: String): String {
    val clean = id.trim()

    if (clean.length <= 8) {
        return clean
    }

    return clean.takeLast(8)
}

private fun formatDate(epochMillis: Long): String {
    if (epochMillis <= 0L) return ""
    return SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date(epochMillis))
}
