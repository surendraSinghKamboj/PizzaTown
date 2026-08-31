package com.pizzatown.customer.presentation.components

import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ScheduleSend
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pizzatown.customer.ui.theme.Dimens
import com.pizzatown.customer.ui.theme.PizzaErrorSurfaceDark
import com.pizzatown.customer.ui.theme.PizzaErrorSurfaceLight
import com.pizzatown.customer.ui.theme.PizzaWarning
import com.pizzatown.customer.ui.theme.PizzaWarningSurfaceDark
import com.pizzatown.customer.ui.theme.PizzaWarningSurfaceLight

/** Section title + optional "View all" action, used above every home-screen list. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/**
 * Non-blocking status strip shown at the top of Home when the restaurant
 * is closed. Purely informational here — the real gate that prevents
 * ordering lives in CheckoutViewModel/Cloud Functions, this just avoids
 * surprising the customer late in the journey.
 */
@Composable
fun RestaurantClosedBanner(modifier: Modifier = Modifier) {
    StatusBanner(
        icon = Icons.Filled.ScheduleSend,
        text = "PizzaTown is currently closed. You can browse the menu, but ordering will open once we're back.",
        tone = StatusTone.WARNING,
        modifier = modifier
    )
}

@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    StatusBanner(
        icon = Icons.Filled.WifiOff,
        text = "You're offline. Showing the last loaded menu.",
        tone = StatusTone.ERROR,
        modifier = modifier
    )
}

enum class StatusTone { WARNING, ERROR }

@Composable
fun StatusBanner(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    tone: StatusTone,
    modifier: Modifier = Modifier
) {
    val dark = isSystemDark()
    val (bg, fg) = when (tone) {
        StatusTone.WARNING -> (if (dark) PizzaWarningSurfaceDark else PizzaWarningSurfaceLight) to PizzaWarning
        StatusTone.ERROR -> (if (dark) PizzaErrorSurfaceDark else PizzaErrorSurfaceLight) to MaterialTheme.colorScheme.error
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.cardRadiusSmall))
            .background(bg)
            .padding(horizontal = Dimens.spaceL, vertical = Dimens.spaceM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(Dimens.spaceS))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = fg)
    }
}

@Composable
private fun isSystemDark(): Boolean = androidx.compose.foundation.isSystemInDarkTheme()

/** "Delivering to <label> — <address>" bar, tap to change/manage addresses in Profile. */
@Composable
fun DeliveryAddressBar(
    label: String,
    addressLine: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.spaceM, vertical = Dimens.spaceM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(Dimens.spaceM))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                "Delivering to",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(2.dp))

            Text(
                if (label.isNotBlank()) "$label — $addressLine" else addressLine,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(Dimens.spaceS))

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Change address",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Small pill category filter with an icon, used on Home's category rail. */
@Composable
fun CategoryIconChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor =
        if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        }

    val contentColor =
        if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    Column(
        modifier = modifier
            .width(72.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(containerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(Modifier.height(Dimens.spaceS))

        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

/** Star + numeric rating + review count, e.g. "4.6 (230)". Caller must only show this when rating is non-null. */
@Composable
fun RatingBadge(rating: Double, reviewCount: Int?, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Star, contentDescription = null, tint = PizzaWarning, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(2.dp))
        Text(
            if (reviewCount != null) "$rating ($reviewCount)" else rating.toString(),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

/** "Bestseller" tag shown next to rating on curated cards — only rendered for items an admin has flagged. */
@Composable
fun BestsellerTag(modifier: Modifier = Modifier) {
    Text(
        "Bestseller",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.tertiary,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
    )
}

/** Heart toggle for a real, device-local favorites list (see FavoriteRepository) — not decorative. */
@Composable
fun FavoriteHeartButton(isFavorite: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
            tint = if (isFavorite) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** +/- stepper for an item already in the cart; shows a plain add button at quantity 0. */
@Composable
fun QuantityStepper(
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    /*
     * IMPORTANT:
     * This component has NO local quantity state.
     *
     * The UI is always derived directly from the cart quantity
     * supplied by the parent.
     *
     * 0 -> [+]
     * 1 -> [- 1 +]
     * 2 -> [- 2 +]
     * etc.
     */
    if (quantity <= 0) {
        FilledIconButton(
            onClick = onIncrement,
            enabled = enabled,
            modifier = modifier.size(Dimens.quantityStepperHeight)
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "Add to cart",
                modifier = Modifier.size(20.dp)
            )
        }

        return
    }

    Row(
        modifier = modifier
            .height(Dimens.quantityStepperHeight)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onDecrement,
            enabled = enabled,
            modifier = Modifier.size(Dimens.quantityStepperHeight)
        ) {
            Icon(
                Icons.Filled.Remove,
                contentDescription = "Decrease quantity",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(17.dp)
            )
        }

        Text(
            text = quantity.toString(),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp)
        )

        IconButton(
            onClick = onIncrement,
            enabled = enabled,
            modifier = Modifier.size(Dimens.quantityStepperHeight)
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "Increase quantity",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@Composable
fun SoldOutChip(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = Dimens.spaceM, vertical = Dimens.spaceXS)
    ) {
        Text("Sold out", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Branded header used above Login/Register forms: logo on a soft brand-
 * tinted circular backdrop, title + subtitle. Kept as a single reusable
 * piece so both auth screens (and any future one) stay visually
 * identical instead of drifting apart.
 */
@Composable
fun AuthHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    logoContent: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                logoContent()
            }
        }
        Spacer(Modifier.height(Dimens.spaceL))
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(Dimens.spaceXXS))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Inline error strip for form screens (login/register/checkout) — consistent look for validation/server errors. */
@Composable
fun FormErrorBanner(message: String, modifier: Modifier = Modifier) {
    StatusBanner(icon = Icons.Filled.ErrorOutline, text = message, tone = StatusTone.ERROR, modifier = modifier)
}

/** Inline success strip, e.g. "Password reset email sent." */
@Composable
fun FormInfoBanner(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.cardRadiusSmall))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
            .padding(horizontal = Dimens.spaceL, vertical = Dimens.spaceM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(Dimens.spaceS))
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}
