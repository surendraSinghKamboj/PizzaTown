package com.pizzatown.customer.presentation.orders

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pizzatown.customer.domain.model.Order
import com.pizzatown.customer.domain.model.OrderStatus

@Composable
fun CurrentOrderMiniBar(
    orders: List<Order>,
    onViewOrders: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeOrders = orders.filter {
        it.status != OrderStatus.COMPLETED &&
        it.status != OrderStatus.CANCELLED
    }

    var dismissed by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf(0) }

    LaunchedEffect(activeOrders.map { it.orderId }) {
        if (activeOrders.isEmpty()) {
            dismissed = false
            selectedIndex = 0
        } else {
            selectedIndex = selectedIndex.coerceIn(0, activeOrders.lastIndex)
        }
    }

    if (activeOrders.isEmpty()) return

    val safeIndex = selectedIndex.coerceIn(0, activeOrders.lastIndex)
    val order = activeOrders[safeIndex]

    AnimatedVisibility(
        visible = !dismissed,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .pointerInput(activeOrders.map { it.orderId }, safeIndex) {
                    var dragX = 0f
                    var dragY = 0f

                    detectDragGestures(
                        onDragStart = {
                            dragX = 0f
                            dragY = 0f
                        },
                        onDragEnd = {
                            val x = dragX
                            val y = dragY

                            if (kotlin.math.abs(y) > kotlin.math.abs(x)) {
                                when {
                                    y < -80f -> onViewOrders()
                                    y > 80f -> dismissed = true
                                }
                            } else if (activeOrders.size > 1) {
                                when {
                                    x < -80f -> {
                                        selectedIndex =
                                            (safeIndex + 1) % activeOrders.size
                                    }

                                    x > 80f -> {
                                        selectedIndex =
                                            if (safeIndex == 0) {
                                                activeOrders.lastIndex
                                            } else {
                                                safeIndex - 1
                                            }
                                    }
                                }
                            }
                        },
                        onDragCancel = {},
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragX += dragAmount.x
                            dragY += dragAmount.y
                        }
                    )
                },
            shape = RoundedCornerShape(26.dp),
            shadowElevation = 10.dp,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ShoppingBag,
                            contentDescription = "Current order",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Order #${order.orderId.takeLast(6).uppercase()}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(2.dp))

                        Text(
                            text = "${order.totalItems} ${
                                if (order.totalItems == 1) "item" else "items"
                            } • ₹${order.grandTotal.toInt()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(2.dp))

                        Text(
                            text = order.status.displayName(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = onViewOrders,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.height(42.dp)
                    ) {
                        Text(
                            text = "My Orders",
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.width(4.dp))

                        Icon(
                            imageVector = Icons.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                if (activeOrders.size > 1) {
                    Spacer(Modifier.height(5.dp))

                    Text(
                        text = "${safeIndex + 1} / ${activeOrders.size} • Swipe sideways for other orders",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

private fun OrderStatus.displayName(): String =
    when (this) {
        OrderStatus.BEING_PAYMENT -> "Payment Processing"
        OrderStatus.PENDING -> "Order Pending"
        OrderStatus.CONFIRMED -> "Order Confirmed"
        OrderStatus.PREPARING -> "Preparing your order"
        OrderStatus.READY -> "Ready for delivery"
        OrderStatus.COMPLETED -> "Completed"
        OrderStatus.CANCELLED -> "Cancelled"
    }

