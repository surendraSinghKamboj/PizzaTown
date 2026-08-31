package com.pizzatown.admin.presentation.broadcast

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pizzatown.admin.core.common.OpState
import com.pizzatown.admin.core.common.UiState
import com.pizzatown.admin.domain.model.Broadcast
import com.pizzatown.admin.domain.model.Customer
import com.pizzatown.admin.presentation.components.EmptyView
import com.pizzatown.admin.presentation.components.ErrorView
import com.pizzatown.admin.presentation.components.LoadingView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcastScreen(
    onBack: () -> Unit,
    viewModel: BroadcastViewModel = hiltViewModel()
) {
    val composeState by viewModel.composeState.collectAsStateWithLifecycle()
    val historyState by viewModel.historyState.collectAsStateWithLifecycle()
    val opState by viewModel.opState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCustomerPicker by remember { mutableStateOf(false) }

    LaunchedEffect(opState) {
        when (val current = opState) {
            is OpState.Success -> { snackbarHostState.showSnackbar("Sent!"); viewModel.consumeOpState() }
            is OpState.Error -> { snackbarHostState.showSnackbar(current.message); viewModel.consumeOpState() }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Broadcast & Offers") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                Text("New Message", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                Row {
                    FilterChip(
                        selected = composeState.targetCustomer == null,
                        onClick = { viewModel.onTargetCustomerChange(null) },
                        label = { Text("All Customers") }
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = composeState.targetCustomer != null,
                        onClick = { showCustomerPicker = true },
                        label = { Text(composeState.targetCustomer?.fullName ?: "Specific Customer") }
                    )
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = composeState.title, onValueChange = viewModel::onTitleChange,
                    label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = composeState.message, onValueChange = viewModel::onMessageChange,
                    label = { Text("Message") }, minLines = 3, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = viewModel::sendBroadcast, modifier = Modifier.fillMaxWidth()) {
                    Text(if (composeState.targetCustomer == null) "Send to All Customers" else "Send to ${composeState.targetCustomer?.fullName}")
                }
            }

            HorizontalDivider()
            Text("History", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))

            when (val current = historyState) {
                is UiState.Loading -> LoadingView()
                is UiState.Error -> ErrorView(current.message)
                is UiState.Empty -> EmptyView("No messages sent yet.")
                is UiState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(current.data, key = { it.id }) { broadcast ->
                            BroadcastHistoryRow(
                                broadcast = broadcast,
                                onDelete = { viewModel.deleteBroadcast(broadcast) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCustomerPicker) {
        CustomerPickerDialog(
            customers = composeState.customers,
            onDismiss = { showCustomerPicker = false },
            onSelect = { customer ->
                viewModel.onTargetCustomerChange(customer)
                showCustomerPicker = false
            }
        )
    }
}

@Composable
private fun BroadcastHistoryRow(broadcast: Broadcast, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(broadcast.title, style = MaterialTheme.typography.titleMedium)
                Text(broadcast.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                Spacer(Modifier.height(4.dp))
                val targetLabel = if (broadcast.targetUserId == null) "All customers" else "To: ${broadcast.targetCustomerName}"
                Text("$targetLabel \u00B7 ${formatDate(broadcast.createdAt)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
        }
    }
}

@Composable
private fun CustomerPickerDialog(
    customers: List<Customer>,
    onDismiss: () -> Unit,
    onSelect: (Customer) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Customer") },
        text = {
            if (customers.isEmpty()) {
                Text("No customers registered yet.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(customers, key = { it.userId }) { customer ->
                        Text(
                            customer.fullName.ifBlank { customer.email },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .clickable { onSelect(customer) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun formatDate(epochMillis: Long): String {
    if (epochMillis <= 0L) return ""
    return SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date(epochMillis))
}
