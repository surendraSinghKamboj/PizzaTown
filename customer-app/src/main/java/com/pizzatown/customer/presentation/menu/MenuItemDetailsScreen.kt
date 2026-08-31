package com.pizzatown.customer.presentation.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pizzatown.customer.core.common.UiState
import com.pizzatown.customer.domain.model.PricingMode
import com.pizzatown.customer.domain.model.SelectionType
import com.pizzatown.customer.presentation.components.BouncyButton
import com.pizzatown.customer.presentation.components.ErrorView
import com.pizzatown.customer.presentation.components.LoadingView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuItemDetailsScreen(
    onBack: () -> Unit,
    onAddedToCart: () -> Unit,
    viewModel: MenuItemDetailsViewModel = hiltViewModel()
) {
    val loadState by viewModel.loadState.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.validationError) {
        state.validationError?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    LaunchedEffect(state.addedToCart) {
        if (state.addedToCart) {
            snackbarHostState.showSnackbar("Added to cart")
            viewModel.consumeAddedToCart()
            onAddedToCart()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        bottomBar = {
            val item = state.menuItem

            if (item != null && item.available) {
                Surface(
                    tonalElevation = 4.dp,
                    shadowElevation = 10.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 16.dp,
                                vertical = 12.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        QuantityStepper(
                            quantity = state.quantity,
                            onDecrement = viewModel::decrementQuantity,
                            onIncrement = viewModel::incrementQuantity
                        )

                        Spacer(Modifier.width(12.dp))

                        BouncyButton(
                            text = "ADD TO CART  •  ₹${state.totalPrice.toInt()}",
                            onClick = viewModel::addToCart,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        )
                    }
                }
            }
        }
    ) { padding ->

        when (val current = loadState) {

            is UiState.Loading -> {
                LoadingView(Modifier.padding(padding))
            }

            is UiState.Error -> {
                ErrorView(
                    current.message,
                    modifier = Modifier.padding(padding)
                )
            }

            is UiState.Empty -> Unit

            is UiState.Success -> {
                val item = current.data

                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {

                    // HERO IMAGE
                    AsyncImage(
                        model = item.imageUrl.ifBlank { null },
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .padding(horizontal = 12.dp)
                            .clip(RoundedCornerShape(24.dp))
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 20.dp,
                                vertical = 18.dp
                            )
                    ) {

                        // PRODUCT TITLE + PRICE
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                item.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(Modifier.width(12.dp))

                            Text(
                                "₹${state.totalPrice.toInt()}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (item.description.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))

                            Text(
                                item.description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!item.available) {
                            Spacer(Modifier.height(16.dp))

                            AssistChip(
                                onClick = {},
                                enabled = false,
                                label = {
                                    Text("Currently unavailable")
                                }
                            )
                        }

                        // VARIANTS
                        if (
                            item.pricingMode == PricingMode.VARIANTS &&
                            item.variants.isNotEmpty()
                        ) {
                            Spacer(Modifier.height(28.dp))

                            Text(
                                "Choose an option",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )

                            Spacer(Modifier.height(4.dp))

                            Text(
                                "Select one",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(12.dp))

                            item.variants
                                .filter { it.available }
                                .forEach { variant ->

                                    val selected =
                                        state.selectedVariantId == variant.id

                                    Surface(
                                        onClick = {
                                            viewModel.selectVariant(variant.id)
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (selected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outlineVariant
                                            }
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(
                                                horizontal = 14.dp,
                                                vertical = 12.dp
                                            ),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = selected,
                                                onClick = {
                                                    viewModel.selectVariant(variant.id)
                                                }
                                            )

                                            Spacer(Modifier.width(8.dp))

                                            Text(
                                                variant.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight =
                                                    if (selected)
                                                        androidx.compose.ui.text.font.FontWeight.SemiBold
                                                    else
                                                        androidx.compose.ui.text.font.FontWeight.Normal,
                                                modifier = Modifier.weight(1f)
                                            )

                                            Text(
                                                "₹${variant.price.toInt()}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                        }

                        // CUSTOMIZATION GROUPS
                        item.customizationGroups.forEach { group ->

                            Spacer(Modifier.height(26.dp))

                            Text(
                                group.name +
                                    if (group.required) "  • Required" else "",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )

                            Spacer(Modifier.height(3.dp))

                            Text(
                                if (group.selectionType == SelectionType.SINGLE)
                                    "Choose 1"
                                else
                                    "Choose up to ${group.maxSelections}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(12.dp))

                            group.options
                                .filter { it.available }
                                .forEach { option ->

                                    val selectedIds =
                                        state.selectedOptions[group.id].orEmpty()

                                    val checked =
                                        option.id in selectedIds

                                    Surface(
                                        onClick = {
                                            viewModel.toggleOption(
                                                group,
                                                option.id
                                            )
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (checked) {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (checked) {
                                                MaterialTheme.colorScheme.secondary
                                            } else {
                                                MaterialTheme.colorScheme.outlineVariant
                                            }
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(
                                                horizontal = 12.dp,
                                                vertical = 9.dp
                                            ),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {

                                            if (
                                                group.selectionType ==
                                                    SelectionType.SINGLE
                                            ) {
                                                RadioButton(
                                                    selected = checked,
                                                    onClick = {
                                                        viewModel.toggleOption(
                                                            group,
                                                            option.id
                                                        )
                                                    }
                                                )
                                            } else {
                                                Checkbox(
                                                    checked = checked,
                                                    onCheckedChange = {
                                                        viewModel.toggleOption(
                                                            group,
                                                            option.id
                                                        )
                                                    }
                                                )
                                            }

                                            Spacer(Modifier.width(6.dp))

                                            Text(
                                                option.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                modifier = Modifier.weight(1f)
                                            )

                                            if (option.priceAdjustment > 0) {
                                                Text(
                                                    "+₹${option.priceAdjustment.toInt()}",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                        }

                        // Bottom breathing room for sticky CTA
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}


@Composable
private fun QuantityStepper(quantity: Int, onDecrement: () -> Unit, onIncrement: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onDecrement, modifier = Modifier.clip(CircleShape)) { Text("\u2212") }
        Box(modifier = Modifier.padding(horizontal = 8.dp)) {
            com.pizzatown.customer.presentation.components.AnimatedQuantityText(quantity)
        }
        IconButton(onClick = onIncrement, modifier = Modifier.clip(CircleShape)) { Text("+") }
    }
}
