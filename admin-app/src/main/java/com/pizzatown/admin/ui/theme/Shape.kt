package com.pizzatown.admin.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

val AdminShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp()),
    small = RoundedCornerShape(8.dp()),
    medium = RoundedCornerShape(12.dp()),
    large = RoundedCornerShape(16.dp()),
    extraLarge = RoundedCornerShape(28.dp())
)

// small helper to keep this file self-contained without extra imports noise
private fun Int.dp() = androidx.compose.ui.unit.Dp(this.toFloat())
