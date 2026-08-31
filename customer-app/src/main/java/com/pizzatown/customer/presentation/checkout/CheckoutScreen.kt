package com.pizzatown.customer.presentation.checkout

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pizzatown.customer.domain.model.Address
import com.pizzatown.customer.domain.model.PaymentMethod

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    onBack: () -> Unit,
    onOrderPlaced: (com.pizzatown.customer.domain.model.Order) -> Unit,
    viewModel: CheckoutViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onLocationPermissionResult(granted)
    }

    LaunchedEffect(Unit) {
        if (!state.locationPermissionGranted) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(state.orderPlaced) {
        state.orderPlaced?.let { order ->
            viewModel.consumeOrderPlaced()
            onOrderPlaced(order)
        }
    }

    val subtotal = state.cartItems.sumOf { it.lineTotal }
    val deliveryFee =
        if (state.freeDeliveryAbove > 0.0 && subtotal >= state.freeDeliveryAbove) {
            0.0
        } else {
            state.deliveryCharge
        }

    val total =
        subtotal - state.couponDiscount + deliveryFee

    val minimumOrderMet =
        subtotal >= state.minimumOrderValue

    val canOrder =
        !state.isBusy &&
        state.restaurantOpen == true &&
        state.locationPermissionGranted &&
        state.deliveryAreaConfigured &&
        state.locationInsideDeliveryArea == true &&
        !state.isCheckingDeliveryArea &&
        minimumOrderMet

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Column {
                        Text(
                            "Checkout",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Complete your order",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
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
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 10.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (state.priceChangedNotice != null) {
                        Text(
                            state.priceChangedNotice ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(6.dp))
                    }

                    if (state.paymentFailedMessage != null) {
                        Text(
                            state.paymentFailedMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(6.dp))
                    }

                    if (
                        state.errorMessage != null &&
                        state.locationInsideDeliveryArea == true &&
                        state.restaurantOpen == true
                    ) {
                        Text(
                            state.errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(6.dp))
                    }

                    if (
                        state.paymentFailedMessage != null &&
                        state.paymentMethod == PaymentMethod.ONLINE
                    ) {
                        Button(
                            onClick = viewModel::retryOnlinePayment,
                            enabled = canOrder,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                "Retry Payment",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = viewModel::placeOrder,
                            enabled = canOrder,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (state.isBusy) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        paymentStageLabel(
                                            state.paymentStage,
                                            state.isPlacingOrder
                                        )
                                    )
                                }
                            } else if (state.paymentMethod == PaymentMethod.ONLINE) {
                                Text(
                                    "Pay ₹${total.toInt()} Online",
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    "Place COD Order",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {

            Spacer(Modifier.height(8.dp))

            CheckoutSectionTitle(
                icon = Icons.Filled.Person,
                title = "Customer Details"
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Full name") },
                leadingIcon = {
                    Icon(Icons.Filled.Person, null)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = state.phone,
                onValueChange = viewModel::onPhoneChange,
                label = { Text("Mobile number") },
                leadingIcon = {
                    Icon(Icons.Filled.Phone, null)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(Modifier.height(22.dp))

            CheckoutSectionTitle(
                icon = Icons.Filled.LocationOn,
                title = "Delivery Address"
            )

            Spacer(Modifier.height(10.dp))

            val showClosed = state.restaurantOpen == false
            val showOutsideArea =
                state.restaurantOpen == true &&
                state.locationPermissionGranted &&
                state.deliveryAreaConfigured &&
                !state.isCheckingDeliveryArea &&
                state.locationInsideDeliveryArea == false

            val showDeliveryAreaUnavailable =
                state.restaurantOpen == true &&
                (
                    !state.locationPermissionGranted ||
                    !state.deliveryAreaConfigured ||
                    state.isCheckingDeliveryArea
                )

            if (showClosed || showOutsideArea || showDeliveryAreaUnavailable) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Storefront,
                            contentDescription = null
                        )

                        Spacer(Modifier.width(10.dp))

                        Column(Modifier.weight(1f)) {
                            Text(
                                when {
                                    showClosed ->
                                        "Pizza Town is currently closed."

                                    showOutsideArea ->
                                        "You're outside our delivery area (${state.deliveryRadiusKm.toInt()} km)."

                                    !state.locationPermissionGranted ->
                                        "Location permission is required to place your order."

                                    state.isCheckingDeliveryArea ->
                                        "Checking your delivery location…"

                                    else ->
                                        "Delivery area is not configured yet."
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )

                            if (!state.locationPermissionGranted && !showClosed) {
                                TextButton(
                                    onClick = {
                                        locationPermissionLauncher.launch(
                                            Manifest.permission.ACCESS_FINE_LOCATION
                                        )
                                    }
                                ) {
                                    Text("Grant location permission")
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
            }

            if (state.addresses.isNotEmpty() && !state.showAddAddressForm) {
                state.addresses.forEach { address ->
                    AddressOption(
                        address = address,
                        selected = state.selectedAddressId == address.id,
                        onSelect = { viewModel.selectAddress(address.id) }
                    )
                    Spacer(Modifier.height(8.dp))
                }

                OutlinedButton(
                    onClick = viewModel::startAddingNewAddress,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("+ Add a new address")
                }
            } else {
                Text(
                    "Where should we deliver your order?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(10.dp))

                Row {
                    listOf("Home", "Work", "Other").forEach { option ->
                        FilterChip(
                            selected = state.newAddressLabel == option,
                            onClick = {
                                viewModel.onNewAddressLabelChange(option)
                            },
                            label = { Text(option) },
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = state.newAddressText,
                    onValueChange = viewModel::onNewAddressTextChange,
                    label = { Text("Full address") },
                    leadingIcon = {
                        Icon(Icons.Filled.LocationOn, null)
                    },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(Modifier.height(10.dp))

                Row {
                    Button(
                        onClick = viewModel::saveNewAddress,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Address")
                    }

                    if (state.addresses.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        TextButton(
                            onClick = viewModel::cancelAddingNewAddress
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            CheckoutSectionTitle(
                icon = Icons.Filled.Notes,
                title = "Special Instructions"
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = state.specialInstructions,
                onValueChange = viewModel::onInstructionsChange,
                label = { Text("Cooking instructions") },
                placeholder = {
                    Text("e.g. No onion, extra crispy")
                },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(Modifier.height(22.dp))

            CheckoutSectionTitle(
                icon = Icons.Filled.Tag,
                title = "Coupon"
            )

            Spacer(Modifier.height(10.dp))

            if (state.appliedCoupon != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.CheckCircle, null)

                        Spacer(Modifier.width(10.dp))

                        Column(Modifier.weight(1f)) {
                            Text(
                                state.appliedCoupon?.code ?: "",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "You saved ₹${state.couponDiscount.toInt()}!",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        TextButton(onClick = viewModel::removeCoupon) {
                            Text("Remove")
                        }
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.couponCodeInput,
                        onValueChange = viewModel::onCouponCodeChange,
                        label = { Text("Coupon code") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(Modifier.width(8.dp))

                    Button(
                        onClick = viewModel::applyCoupon,
                        enabled = !state.isApplyingCoupon,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (state.isApplyingCoupon) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Apply")
                        }
                    }
                }

                if (state.couponError != null) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        state.couponError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            CheckoutSectionTitle(
                icon = Icons.Filled.Storefront,
                title = "Price Summary"
            )

            Spacer(Modifier.height(10.dp))

            SummaryRow(
                label = "Subtotal",
                amount = subtotal
            )

            SummaryRow(
                label = "Discount",
                amount = -state.couponDiscount
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Delivery",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = if (deliveryFee <= 0.0) {
                        "FREE"
                    } else {
                        "₹${deliveryFee.toInt()}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (deliveryFee <= 0.0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            if (state.freeDeliveryAbove > 0.0 && deliveryFee > 0.0) {
                val remaining = (state.freeDeliveryAbove - subtotal).coerceAtLeast(0.0)

                if (remaining > 0.0) {
                    Text(
                        "Add ₹${remaining.toInt()} more for FREE delivery",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }

            if (!minimumOrderMet) {
                Text(
                    "Minimum order ₹${state.minimumOrderValue.toInt()} • Add ₹${(state.minimumOrderValue - subtotal).coerceAtLeast(0.0).toInt()} more",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "₹${total.toInt()}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(22.dp))

            CheckoutSectionTitle(
                icon = Icons.Filled.Payments,
                title = "Payment Method"
            )

            Spacer(Modifier.height(10.dp))

            PaymentMethodOption(
                label = "Online Payment",
                subtitle = "UPI, cards, netbanking & wallets",
                icon = Icons.Filled.Payments,
                selected = state.paymentMethod == PaymentMethod.ONLINE,
                enabled = !state.isBusy,
                onSelect = {
                    viewModel.onPaymentMethodChange(PaymentMethod.ONLINE)
                }
            )

            Spacer(Modifier.height(8.dp))

            PaymentMethodOption(
                label = "Cash on Delivery",
                subtitle = "Pay when your order arrives",
                icon = Icons.Filled.Payments,
                selected = state.paymentMethod == PaymentMethod.COD,
                enabled = !state.isBusy,
                onSelect = {
                    viewModel.onPaymentMethodChange(PaymentMethod.COD)
                }
            )

            Spacer(Modifier.height(22.dp))

            CheckoutSectionTitle(
                icon = Icons.Filled.Storefront,
                title = "Order Summary"
            )

            Spacer(Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {

                    state.cartItems.forEach { item ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${item.menuItemName} × ${item.quantity}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                "₹${item.lineTotal.toInt()}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                    }

                    HorizontalDivider()

                    Spacer(Modifier.height(10.dp))

                    SummaryRow("Subtotal", subtotal)

                    if (state.couponDiscount > 0) {
                        Spacer(Modifier.height(5.dp))
                        SummaryRow(
                            "Coupon discount",
                            -state.couponDiscount
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Total",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "₹${total.toInt()}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(Modifier.height(110.dp))
        }
    }
}

@Composable
private fun CheckoutSectionTitle(
    icon: ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier
                    .size(38.dp)
                    .padding(9.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.width(10.dp))

        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SummaryRow(
    label: String,
    amount: Double
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            "₹${amount.toInt()}",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun PaymentMethodOption(
    label: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
            }
        ),
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                enabled = enabled,
                onClick = onSelect
            )
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(44.dp)
                        .padding(10.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            RadioButton(
                selected = selected,
                onClick = onSelect,
                enabled = enabled,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
private fun AddressOption(
    address: Address,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
            }
        ),
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onSelect
            )
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ) {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(44.dp)
                        .padding(10.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    address.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    address.fullAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            RadioButton(
                selected = selected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

/** Short label shown next to the spinner on the CTA while an order/payment is in flight. */
private fun paymentStageLabel(
    stage: PaymentStage,
    isPlacingOrder: Boolean
): String = when {
    isPlacingOrder -> "Placing order…"
    stage == PaymentStage.CREATING_ORDER -> "Creating payment…"
    stage == PaymentStage.OPENING_GATEWAY -> "Opening payment gateway…"
    stage == PaymentStage.AWAITING_GATEWAY -> "Waiting for payment…"
    stage == PaymentStage.VERIFYING -> "Verifying payment…"
    else -> "Please wait…"
}
