package com.pizzatown.customer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.pizzatown.customer.core.preferences.ThemeMode

private val LightColors = lightColorScheme(
    primary = PizzaGold,
    onPrimary = PizzaCrustBrown,
    secondary = PizzaCrustBrown,
    onSecondary = PizzaWhite,
    tertiary = PizzaBadgeRed,
    onTertiary = PizzaWhite,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    error = PizzaError,
    onError = PizzaWhite
)

private val DarkColors = darkColorScheme(
    primary = PizzaGold,
    onPrimary = PizzaCrustBrown,
    secondary = PizzaGold,
    onSecondary = PizzaCrustBrown,
    tertiary = PizzaBadgeRed,
    onTertiary = PizzaWhite,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    error = PizzaError,
    onError = PizzaWhite
)

@Composable
fun PizzaTownCustomerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = CustomerTypography,
        shapes = CustomerShapes,
        content = content
    )
}
