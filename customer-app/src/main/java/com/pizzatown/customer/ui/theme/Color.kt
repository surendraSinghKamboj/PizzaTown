package com.pizzatown.customer.ui.theme

import androidx.compose.ui.graphics.Color

// Pizza Town brand palette — sourced from the official logo (golden
// pizza/cheese yellow, dark-brown crust, red "Best in Town" badge).
// Change these values to re-theme the entire customer app.
val PizzaGold = Color(0xFFF5B301)
val PizzaGoldDark = Color(0xFFD99700)
val PizzaCrustBrown = Color(0xFF3D2314)
val PizzaBadgeRed = Color(0xFFC1272D)
val PizzaSuccess = Color(0xFF2E9B4F)
val PizzaError = PizzaBadgeRed
val PizzaGrey = Color(0xFF6F6862)
val PizzaWhite = Color(0xFFFFFFFF)

// Clean, neutral surfaces — plain white for light mode, near-black for
// dark mode (not a tinted/cream color), so content and photos read
// clearly against the background.
val LightBackground = Color(0xFFFFFFFF)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF2F2F2)

val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkSurfaceVariant = Color(0xFF2A2A2A)

// Status/banner tones used by the design system's status surfaces
// (restaurant closed, outside delivery area, sold out, etc.). Kept
// distinct from onSurface text colors so both light and dark mode
// stay readable — see AppComponents.kt / StateViews.kt.
val PizzaWarning = Color(0xFFB25E00)
val PizzaWarningSurfaceLight = Color(0xFFFFF1DE)
val PizzaWarningSurfaceDark = Color(0xFF3A2A12)
val PizzaErrorSurfaceLight = Color(0xFFFCE9E9)
val PizzaErrorSurfaceDark = Color(0xFF3A1414)
val PizzaSuccessSurfaceLight = Color(0xFFE6F5EA)
val PizzaSuccessSurfaceDark = Color(0xFF14331E)
