package com.pizzatown.customer.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/** Slides the new quantity in and the old one out, instead of a jarring instant swap. */
@Composable
fun AnimatedQuantityText(quantity: Int) {
    AnimatedContent(
        targetState = quantity,
        transitionSpec = {
            if (targetState > initialState) {
                (slideInVertically(tween(200)) { it } togetherWith slideOutVertically(tween(200)) { -it })
            } else {
                (slideInVertically(tween(200)) { -it } togetherWith slideOutVertically(tween(200)) { it })
            }
        },
        label = "quantity"
    ) { value ->
        Text(value.toString(), style = MaterialTheme.typography.titleMedium)
    }
}
