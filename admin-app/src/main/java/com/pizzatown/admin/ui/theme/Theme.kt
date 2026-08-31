package com.pizzatown.admin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = LightAccent,
    onPrimary = Color.White,

    primaryContainer = Color(0xFFE8ECFF),
    onPrimaryContainer = Color(0xFF17245C),

    secondary = LightInfo,
    onSecondary = Color.White,

    secondaryContainer = Color(0xFFDDF6FA),
    onSecondaryContainer = Color(0xFF063B46),

    tertiary = LightSuccess,
    onTertiary = Color.White,

    tertiaryContainer = Color(0xFFDDF7E5),
    onTertiaryContainer = Color(0xFF063D18),

    background = LightBgBase,
    onBackground = LightTextPrimary,

    surface = LightBgSurface,
    onSurface = LightTextPrimary,

    surfaceVariant = LightBgSurface2,
    onSurfaceVariant = LightTextSecondary,

    outline = LightBorder,
    outlineVariant = LightBorder,

    error = LightDanger,
    onError = Color.White,

    errorContainer = Color(0xFFFFE5E5),
    onErrorContainer = Color(0xFF5C1111)
)

private val DarkColors = darkColorScheme(
    primary = DarkAccent,
    onPrimary = Color.White,

    primaryContainer = Color(0xFF202A55),
    onPrimaryContainer = Color(0xFFDCE3FF),

    secondary = DarkInfo,
    onSecondary = Color(0xFF00343E),

    secondaryContainer = Color(0xFF12343B),
    onSecondaryContainer = Color(0xFFB8F0F7),

    tertiary = DarkSuccess,
    onTertiary = Color(0xFF003912),

    tertiaryContainer = Color(0xFF124A24),
    onTertiaryContainer = Color(0xFFB8F5C7),

    background = DarkBgBase,
    onBackground = DarkTextPrimary,

    surface = DarkBgSurface,
    onSurface = DarkTextPrimary,

    surfaceVariant = DarkBgSurface2,
    onSurfaceVariant = DarkTextSecondary,

    outline = DarkBorder,
    outlineVariant = DarkBorder,

    error = DarkDanger,
    onError = Color(0xFF5C0000),

    errorContainer = Color(0xFF5A2020),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun PizzaTownAdminTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()

    val useDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (useDarkTheme) {
        DarkColors
    } else {
        LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AdminTypography,
        shapes = AdminShapes,
        content = content
    )
}
