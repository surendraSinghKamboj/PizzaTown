package com.pizzatown.admin.presentation.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

/** Animates a stat number counting up to its target whenever it changes, instead of snapping. */
@Composable
fun AnimatedCountUp(targetValue: Int, style: TextStyle) {
    val animated by animateIntAsState(
        targetValue = targetValue,
        animationSpec = tween(durationMillis = 600),
        label = "count-up"
    )
    Text(text = animated.toString(), style = style)
}
