package com.pizzatown.delivery.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DeliveryLightColors = lightColorScheme(
    primary = Color(0xFFE94B23),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBD0),
    onPrimaryContainer = Color(0xFF3A0B03),

    secondary = Color(0xFF303030),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE7E2DF),
    onSecondaryContainer = Color(0xFF1C1B1A),

    tertiary = Color(0xFFFF8A3D),
    onTertiary = Color(0xFF351000),
    tertiaryContainer = Color(0xFFFFDCC7),
    onTertiaryContainer = Color(0xFF351000),

    background = Color(0xFFFFFAF7),
    surface = Color(0xFFFFFAF7),
    surfaceVariant = Color(0xFFF1E9E5),
    onSurface = Color(0xFF1D1B1A),
    onSurfaceVariant = Color(0xFF514541)
)

private val DeliveryDarkColors = darkColorScheme(
    primary = Color(0xFFFF8B6F),
    onPrimary = Color(0xFF521406),
    primaryContainer = Color(0xFF7A2815),
    onPrimaryContainer = Color(0xFFFFDBD0),

    secondary = Color(0xFFE6E1DE),
    onSecondary = Color(0xFF252321),
    secondaryContainer = Color(0xFF3A3735),
    onSecondaryContainer = Color(0xFFECE7E4),

    tertiary = Color(0xFFFFB078),
    onTertiary = Color(0xFF3A1805),
    tertiaryContainer = Color(0xFF6B3518),
    onTertiaryContainer = Color(0xFFFFDCC7),

    background = Color(0xFF151313),
    surface = Color(0xFF191716),
    surfaceVariant = Color(0xFF302B29),
    onSurface = Color(0xFFF2EAE7),
    onSurfaceVariant = Color(0xFFD8C7C1)
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
