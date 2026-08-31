package com.pizzatown.customer.presentation.cart

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun CartMiniBar(
    itemCount: Int,
    total: Double,
    onViewCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dismissed by remember { mutableStateOf(false) }
    var previousItemCount by remember { mutableStateOf(itemCount) }

    /*
     * A genuinely NEW item should bring the bar back.
     *
     * Quantity changes to an already-existing item don't resurrect it.
     */
    LaunchedEffect(itemCount) {
        if (itemCount > previousItemCount) {
            dismissed = false
        }

        if (itemCount <= 0) {
            dismissed = false
        }

        previousItemCount = itemCount
    }

    val visible = itemCount > 0 && !dismissed

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .pointerInput(itemCount) {
                    var totalDrag = 0f

                    detectVerticalDragGestures(
                        onDragStart = {
                            totalDrag = 0f
                        },
                        onVerticalDrag = { _, dragAmount ->
                            totalDrag += dragAmount
                        },
                        onDragEnd = {
                            when {
                                // Swipe UP -> open Cart
                                totalDrag < -80f -> {
                                    onViewCart()
                                }

                                // Swipe DOWN -> temporarily hide bar
                                totalDrag > 80f -> {
                                    dismissed = true
                                }
                            }
                        }
                    )
                },
            shape = RoundedCornerShape(28.dp),
            shadowElevation = 10.dp,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                /*
                 * Small drag handle gives the user an obvious affordance
                 * that this surface can be swiped.
                 */
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(width = 34.dp, height = 4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = 0.35f
                            )
                        )
                        .align(Alignment.CenterHorizontally)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 10.dp,
                            end = 10.dp,
                            top = 7.dp,
                            bottom = 10.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ShoppingCart,
                            contentDescription = "Cart",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "$itemCount ${if (itemCount == 1) "item" else "items"}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(2.dp))

                        Text(
                            text = "₹${"%.0f".format(total)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onViewCart,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.height(46.dp)
                    ) {
                        Text(
                            text = "View Cart",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
