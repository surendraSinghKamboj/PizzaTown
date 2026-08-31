package com.pizzatown.admin.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * PizzaTown Admin — Core Design System
 *
 * Neutral enterprise surfaces + blue primary accent.
 * These tokens are intentionally centralized so every Admin
 * screen shares the same visual language.
 */

// ------------------------------------------------------------
// LIGHT MODE
// ------------------------------------------------------------

val LightBgBase = Color(0xFFF7F8FA)
val LightBgSurface = Color(0xFFFFFFFF)
val LightBgSurface2 = Color(0xFFEEF1F5)
val LightBorder = Color(0xFFE2E5EA)

val LightTextPrimary = Color(0xFF111418)
val LightTextSecondary = Color(0xFF5A6270)

val LightAccent = Color(0xFF3D5AFE)
val LightAccentHover = Color(0xFF2E46D6)

val LightSuccess = Color(0xFF16A34A)
val LightWarning = Color(0xFFD97706)
val LightDanger = Color(0xFFDC2626)
val LightInfo = Color(0xFF0891B2)

// ------------------------------------------------------------
// DARK MODE
// ------------------------------------------------------------

val DarkBgBase = Color(0xFF0E1116)
val DarkBgSurface = Color(0xFF161A21)
val DarkBgSurface2 = Color(0xFF1E232B)
val DarkBorder = Color(0xFF2A303A)

val DarkTextPrimary = Color(0xFFF1F3F6)
val DarkTextSecondary = Color(0xFF9AA3B2)

val DarkAccent = Color(0xFF5B7CFF)
val DarkAccentHover = Color(0xFF7C97FF)

val DarkSuccess = Color(0xFF3ECF6E)
val DarkWarning = Color(0xFFF5A623)
val DarkDanger = Color(0xFFFF5C5C)
val DarkInfo = Color(0xFF38BDF8)

// ------------------------------------------------------------
// Shared semantic aliases
// ------------------------------------------------------------

val PizzaSuccess = LightSuccess
val PizzaError = LightDanger
val PizzaWhite = Color(0xFFFFFFFF)

// Legacy aliases kept temporarily so existing screens/code
// continue compiling while the UI migration is completed.
val PizzaGold = LightAccent
val PizzaGoldDark = LightAccentHover
val PizzaCrustBrown = LightTextPrimary
val PizzaBadgeRed = LightDanger
val PizzaGrey = LightTextSecondary

val LightBackground = LightBgBase
val LightSurface = LightBgSurface
val LightSurfaceVariant = LightBgSurface2

val DarkBackground = DarkBgBase
val DarkSurface = DarkBgSurface
val DarkSurfaceVariant = DarkBgSurface2
