package com.pizzatown.customer.presentation.components

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Wraps a single grid/list item so it fades and slides up into place the
 * first time it's composed — gives the menu grid a subtle, lively
 * "appearing" feel instead of just popping in.
 */
@Composable
fun FadeSlideInItem(content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = EaseOutCubic),
        label = "item-alpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 24f,
        animationSpec = tween(durationMillis = 320, easing = EaseOutCubic),
        label = "item-offset"
    )

    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .alpha(alpha)
            .graphicsLayer { translationY = offsetY }
    ) {
        content()
    }
}
