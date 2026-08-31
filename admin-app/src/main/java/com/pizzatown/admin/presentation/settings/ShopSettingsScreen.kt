package com.pizzatown.admin.presentation.settings

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.PaddingValues

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.items

import androidx.compose.material.icons.filled.LocationOn

import androidx.compose.material3.Card

import androidx.compose.material3.CardDefaults

import androidx.compose.material3.Button

import androidx.compose.material3.ButtonDefaults

import com.pizzatown.admin.domain.model.DeliveryPricing

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsSuggest
import com.pizzatown.admin.ui.theme.AppearancePreferences
import com.pizzatown.admin.ui.theme.ThemeMode
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopSettingsScreen(
    onBack: () -> Unit,
    appearancePreferences: AppearancePreferences,
    viewModel: ShopSettingsViewModel = hiltViewModel()
) {
    val pricingState by viewModel.pricingState.collectAsStateWithLifecycle()
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val saveMessage by viewModel.saveMessage.collectAsStateWithLifecycle()
    val themeMode by appearancePreferences.themeMode.collectAsState(
        initial = ThemeMode.SYSTEM
    )

    val themeScope = rememberCoroutineScope()

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted =
                permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (granted) {
                viewModel.fetchCurrentLocation()
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Shop Settings",
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Restaurant configuration",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                horizontal = 16.dp,
                vertical = 18.dp
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            // ==================================================
            // HEADER
            // ==================================================

            item {
                Column {
                    Text(
                        text = "Configure your shop",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Manage appearance, order pricing and delivery coverage.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ==================================================
            // APPEARANCE
            // ==================================================

            item {
                SettingsSectionHeader(
                    title = "Appearance",
                    subtitle = "Choose how the admin app should look."
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SettingsSuggest,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Spacer(Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "Theme",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = when (themeMode) {
                                        ThemeMode.SYSTEM -> "Follow device theme"
                                        ThemeMode.LIGHT -> "Always use light mode"
                                        ThemeMode.DARK -> "Always use dark mode"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeChoice(
                                text = "System",
                                icon = Icons.Filled.SettingsSuggest,
                                selected = themeMode == ThemeMode.SYSTEM,
                                onClick = {
                                    themeScope.launch {
                                        appearancePreferences.setThemeMode(
                                            ThemeMode.SYSTEM
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )

                            ThemeChoice(
                                text = "Light",
                                icon = Icons.Filled.LightMode,
                                selected = themeMode == ThemeMode.LIGHT,
                                onClick = {
                                    themeScope.launch {
                                        appearancePreferences.setThemeMode(
                                            ThemeMode.LIGHT
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )

                            ThemeChoice(
                                text = "Dark",
                                icon = Icons.Filled.DarkMode,
                                selected = themeMode == ThemeMode.DARK,
                                onClick = {
                                    themeScope.launch {
                                        appearancePreferences.setThemeMode(
                                            ThemeMode.DARK
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // ==================================================
            // ORDER PRICING
            // ==================================================

            item {
                SettingsSectionHeader(
                    title = "Order pricing",
                    subtitle = "Set the minimum order and delivery rules customers see at checkout."
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {

                        PricingField(
                            value = pricingState.minimumOrderValue,
                            onValueChange = viewModel::onMinimumOrderValueChange,
                            label = "Minimum order value",
                            supporting = "Customers cannot place an order below this amount."
                        )

                        PricingField(
                            value = pricingState.deliveryCharge,
                            onValueChange = viewModel::onDeliveryChargeChange,
                            label = "Delivery charge",
                            supporting = "Applied when the free-delivery threshold is not reached."
                        )

                        PricingField(
                            value = pricingState.freeDeliveryAbove,
                            onValueChange = viewModel::onFreeDeliveryAboveChange,
                            label = "Free delivery above",
                            supporting = "Set 0 to disable free delivery."
                        )

                        Button(
                            onClick = viewModel::saveDeliveryPricing,
                            enabled = pricingState.isDirty,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(15.dp)
                        ) {
                            Text(
                                text = "Save order pricing",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // ==================================================
            // DELIVERY AREA
            // ==================================================

            item {
                SettingsSectionHeader(
                    title = "Delivery area",
                    subtitle = "Choose your shop location and define how far you deliver."
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Spacer(Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = "Shop location",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "Orders are accepted only inside this delivery radius.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null
                            )

                            Spacer(Modifier.width(8.dp))

                            Text("Use current location")
                        }

                        OutlinedTextField(
                            value = formState.centerLat,
                            onValueChange = viewModel::onLatitudeChange,
                            label = { Text("Latitude") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = formState.centerLng,
                            onValueChange = viewModel::onLongitudeChange,
                            label = { Text("Longitude") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = formState.radiusKm,
                            onValueChange = viewModel::onRadiusChange,
                            label = { Text("Delivery radius (km)") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = viewModel::saveDeliveryArea,
                            enabled = formState.isDirty,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(15.dp)
                        ) {
                            Text(
                                text = "Save delivery area",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // ==================================================
            // SAVE FEEDBACK
            // ==================================================

            item {
                if (!saveMessage.isNullOrBlank()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = saveMessage ?: "",
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    subtitle: String
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(3.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PricingField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    supporting: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("$label (₹)") },
        supportingText = {
            Text(supporting)
        },
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = KeyboardType.Decimal
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ThemeChoice(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(13.dp),
        colors = if (selected) {
            ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary
            )
        } else {
            ButtonDefaults.outlinedButtonColors()
        }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )

        Spacer(Modifier.width(6.dp))

        Text(
            text = text,
            maxLines = 1
        )
    }
}
