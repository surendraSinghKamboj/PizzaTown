package com.pizzatown.admin.presentation.coupons

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pizzatown.admin.domain.model.Customer
import com.pizzatown.admin.domain.model.DiscountType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouponEditorScreen(
    onBack: () -> Unit,
    viewModel: CouponEditorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCustomerPicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.saveSuccess) { if (state.saveSuccess) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Coupon") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(Modifier.padding(16.dp)) {
                    if (state.errorMessage != null) {
                        Text(state.errorMessage ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    Button(onClick = viewModel::save, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        if (state.isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Text("Create Coupon")
                    }
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            OutlinedTextField(
                value = state.code, onValueChange = viewModel::onCodeChange,
                label = { Text("Coupon Code (e.g. PIZZA20)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))
            Text("Discount Type", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.padding(top = 4.dp)) {
                FilterChip(
                    selected = state.discountType == DiscountType.PERCENTAGE,
                    onClick = { viewModel.onDiscountTypeChange(DiscountType.PERCENTAGE) },
                    label = { Text("Percentage %") }
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = state.discountType == DiscountType.FIXED_AMOUNT,
                    onClick = { viewModel.onDiscountTypeChange(DiscountType.FIXED_AMOUNT) },
                    label = { Text("Fixed \u20B9 amount") }
                )
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.discountValue, onValueChange = viewModel::onDiscountValueChange,
                label = { Text(if (state.discountType == DiscountType.PERCENTAGE) "Discount %" else "Discount amount (\u20B9)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            if (state.discountType == DiscountType.PERCENTAGE) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.maxDiscountAmount, onValueChange = viewModel::onMaxDiscountAmountChange,
                    label = { Text("Max discount cap (\u20B9, 0 = no cap)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.minOrderValue, onValueChange = viewModel::onMinOrderValueChange,
                label = { Text("Minimum order value (\u20B9, 0 = none)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.usageLimit, onValueChange = viewModel::onUsageLimitChange,
                label = { Text("Usage limit (0 = unlimited)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Text("Who can use this coupon?", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.padding(top = 4.dp)) {
                FilterChip(
                    selected = state.targetCustomer == null,
                    onClick = { viewModel.onTargetCustomerChange(null) },
                    label = { Text("Any Customer") }
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = state.targetCustomer != null,
                    onClick = { showCustomerPicker = true },
                    label = { Text(state.targetCustomer?.fullName ?: "Specific Customer") }
                )
            }

            Spacer(Modifier.height(80.dp))
        }
    }

    if (showCustomerPicker) {
        AlertDialog(
            onDismissRequest = { showCustomerPicker = false },
            title = { Text("Select Customer") },
            text = {
                if (state.customers.isEmpty()) {
                    Text("No customers registered yet.")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(state.customers, key = { it.userId }) { customer: Customer ->
                            Text(
                                customer.fullName.ifBlank { customer.email },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                                    .clickable {
                                        viewModel.onTargetCustomerChange(customer)
                                        showCustomerPicker = false
                                    }
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCustomerPicker = false }) { Text("Cancel") } }
        )
    }
}
