package com.pizzatown.delivery.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DeliveryLightColors = lightColorScheme(
    primary = Color(0xFFE85D3F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD1),
    onPrimaryContainer = Color(0xFF3A0B04),

    secondary = Color(0xFF76566C),
    secondaryContainer = Color(0xFFF1D9E7),
    onSecondaryContainer = Color(0xFF2D1827),

    tertiary = Color(0xFF765B2E),
    tertiaryContainer = Color(0xFFFFDEAB),
    onTertiaryContainer = Color(0xFF291800),

    background = Color(0xFFFFFBF9),
    surface = Color(0xFFFFFBF9),
    surfaceVariant = Color(0xFFF5DEDA),
    onSurface = Color(0xFF201A18),
    onSurfaceVariant = Color(0xFF53433F)
)

private val DeliveryDarkColors = darkColorScheme(
    primary = Color(0xFFFFB5A1),
    onPrimary = Color(0xFF5A170B),
    primaryContainer = Color(0xFF7D2D1D),
    onPrimaryContainer = Color(0xFFFFDAD1),

    secondary = Color(0xFFE5BFD7),
    secondaryContainer = Color(0xFF5A3F52),
    onSecondaryContainer = Color(0xFFFFD9EB),

    tertiary = Color(0xFFE8C48F),
    tertiaryContainer = Color(0xFF59431F),
    onTertiaryContainer = Color(0xFFFFDEAB),

    background = Color(0xFF171312),
    surface = Color(0xFF171312),
    surfaceVariant = Color(0xFF514441),
    onSurface = Color(0xFFEDE0DC),
    onSurfaceVariant = Color(0xFFD8C2BC)
)

@Composable
fun DeliveryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) {
            DeliveryDarkColors
        } else {
            DeliveryLightColors
        },
        content = content
    )
}
