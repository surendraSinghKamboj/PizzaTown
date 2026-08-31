package com.pizzatown.admin.presentation.coupons

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pizzatown.admin.core.common.UiState
import com.pizzatown.admin.domain.model.Coupon
import com.pizzatown.admin.domain.model.DiscountType
import com.pizzatown.admin.presentation.components.EmptyView
import com.pizzatown.admin.presentation.components.ErrorView
import com.pizzatown.admin.presentation.components.LoadingView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouponsListScreen(
    onBack: () -> Unit,
    onAddCoupon: () -> Unit,
    viewModel: CouponsViewModel = hiltViewModel()
) {
    val state by viewModel.couponsState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeFilter by viewModel.activeFilter.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Coupons") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAddCoupon, text = { Text("Add Coupon") }, icon = {})
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::setSearchQuery,
                placeholder = { Text("Search by code or customer") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { FilterChip(selected = activeFilter == null, onClick = { viewModel.setActiveFilter(null) }, label = { Text("All") }) }
                item { FilterChip(selected = activeFilter == true, onClick = { viewModel.setActiveFilter(true) }, label = { Text("Active") }) }
                item { FilterChip(selected = activeFilter == false, onClick = { viewModel.setActiveFilter(false) }, label = { Text("Inactive") }) }
            }

            when (val current = state) {
                is UiState.Loading -> LoadingView()
                is UiState.Error -> ErrorView(current.message)
                is UiState.Empty -> EmptyView(
                    if (searchQuery.isNotBlank() || activeFilter != null)
                        "No coupons match these filters."
                    else "No coupons yet. Tap \"Add Coupon\" to create a discount code for your customers."
                )
                is UiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(current.data, key = { it.id }) { coupon ->
                            CouponRow(
                                coupon = coupon,
                                onToggleActive = { active -> viewModel.setActive(coupon, active) },
                                onDelete = { viewModel.deleteCoupon(coupon) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CouponRow(
    coupon: Coupon,
    onToggleActive: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(coupon.code, style = MaterialTheme.typography.titleMedium)
                val discountLabel = when (coupon.discountType) {
                    DiscountType.PERCENTAGE -> "${coupon.discountValue.toInt()}% off"
                    DiscountType.FIXED_AMOUNT -> "\u20B9${coupon.discountValue.toInt()} off"
                }
                Text(discountLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (coupon.targetUserId != null) {
                    Text("Only for: ${coupon.targetCustomerName}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                val usageLabel = if (coupon.usageLimit > 0) "Used ${coupon.usageCount}/${coupon.usageLimit}" else "Used ${coupon.usageCount} times"
                Text(usageLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = coupon.active, onCheckedChange = onToggleActive)
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete ${coupon.code}")
            }
        }
    }
}
