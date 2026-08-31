package com.pizzatown.customer.presentation.orders

import androidx.compose.foundation.layout.Box
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pizzatown.customer.domain.model.Order
import com.pizzatown.customer.domain.model.PaymentMethod
import com.pizzatown.customer.domain.model.PaymentStatus

@Composable
fun OrderPlacedScreen(
    order: Order,
    onViewOrders: () -> Unit,
    onBackToHome: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        // =====================================================
        // NORMAL UI LAYER
        // =====================================================

        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackToHome
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Home"
                    )
                }

                Text(
                    "Order Confirmation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(Modifier.height(42.dp))

                // =================================================
                // SUCCESS ICON
                // =================================================

                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.primary.copy(
                        alpha = 0.10f
                    )
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(82.dp)
                            .padding(15.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    "Order Placed!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(7.dp))

                Text(
                    when (order.paymentMethod) {
                        PaymentMethod.ONLINE ->
                            if (order.paymentStatus == PaymentStatus.PAID) {
                                "Payment successful. Your order is confirmed."
                            } else {
                                "Your order has been received successfully."
                            }

                        PaymentMethod.COD ->
                            "Your order has been received successfully."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(28.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(
                            alpha = 0.55f
                        )
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(18.dp)
                    ) {
                        Text(
                            "Order summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(14.dp))

                        SuccessInfoRow(
                            label = "Order ID",
                            value = order.orderId
                        )

                        Spacer(Modifier.height(9.dp))

                        SuccessInfoRow(
                            label = "Items",
                            value = order.totalItems.toString()
                        )

                        Spacer(Modifier.height(9.dp))

                        SuccessInfoRow(
                            label = "Payment",
                            value = when (order.paymentMethod) {
                                PaymentMethod.COD ->
                                    "Cash on Delivery"

                                PaymentMethod.ONLINE ->
                                    if (order.paymentStatus == PaymentStatus.PAID) {
                                        "Paid Online"
                                    } else {
                                        "Online Payment"
                                    }
                            }
                        )

                        Spacer(Modifier.height(12.dp))

                        HorizontalDivider()

                        Spacer(Modifier.height(12.dp))

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Total",
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
                    }
                }

                Spacer(Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(
                        alpha = 0.45f
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.ShoppingBag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(Modifier.width(10.dp))

                        Column(
                            Modifier.weight(1f)
                        ) {
                            Text(
                                "We're getting your food ready.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(Modifier.height(2.dp))

                            Text(
                                "You can check your order anytime from My Orders.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = onViewOrders,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = null
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        "View My Orders",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }

        // =====================================================
        // CONFETTI OVERLAY LAYER
        //
        // Box child order means this is drawn ABOVE the content.
        // It does NOT take part in the Column's layout sizing.
        // =====================================================

        val compositionResult = rememberLottieComposition(
            LottieCompositionSpec.Asset("order_confetti.json")
        )

        val composition = compositionResult.value

        val animationState = animateLottieCompositionAsState(
            composition = composition,
            iterations = 1
        )

        if (composition != null) {
            LottieAnimation(
                composition = composition,
                progress = { animationState.progress },
                modifier = Modifier.matchParentSize()
            )
        }
    }
}


@Composable
private fun SuccessInfoRow(
    label: String,
    value: String
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.width(12.dp))

        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}
