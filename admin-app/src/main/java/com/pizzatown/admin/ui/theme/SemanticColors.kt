package com.pizzatown.admin.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * PizzaTown Admin semantic colors.
 *
 * These colors are intentionally neutral and consistent with the
 * Admin design system. Branding/action color comes from
 * MaterialTheme.colorScheme.primary.
 */

// Order lifecycle
val StatusPending = LightWarning
val StatusConfirmed = LightInfo
val StatusPreparing = LightWarning
val StatusReady = LightInfo
val StatusCompleted = LightSuccess
val StatusCancelled = LightDanger

// Payment
val PaymentPaid = LightSuccess
val PaymentPending = LightWarning
val PaymentFailed = LightDanger
