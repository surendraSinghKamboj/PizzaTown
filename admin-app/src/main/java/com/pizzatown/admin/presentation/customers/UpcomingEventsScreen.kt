package com.pizzatown.admin.presentation.customers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pizzatown.admin.core.common.UiState
import com.pizzatown.admin.domain.usecase.CustomerEventType
import com.pizzatown.admin.domain.usecase.UpcomingCustomerEvent
import com.pizzatown.admin.presentation.components.EmptyView
import com.pizzatown.admin.presentation.components.ErrorView
import com.pizzatown.admin.presentation.components.LoadingView
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpcomingEventsScreen(
    onBack: () -> Unit,
    viewModel: UpcomingEventsViewModel = hiltViewModel()
) {
    val state by viewModel.eventsState.collectAsStateWithLifecycle()
    val windowDays by viewModel.windowDays.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Birthdays & Anniversaries") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(7, 30, 90).forEach { days ->
                    FilterChip(
                        selected = windowDays == days,
                        onClick = { viewModel.setWindowDays(days) },
                        label = { Text("Next $days days") }
                    )
                }
            }

            when (val current = state) {
                is UiState.Loading -> LoadingView()
                is UiState.Error -> ErrorView(current.message)
                is UiState.Empty -> EmptyView("No birthdays or anniversaries coming up in this window.")
                is UiState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(current.data, key = { "${it.type}-${it.customer.userId}" }) { event ->
                            EventRow(event, modifier = Modifier.animateItem())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventRow(event: UpcomingCustomerEvent, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (event.type == CustomerEventType.BIRTHDAY) Icons.Filled.Cake else Icons.Filled.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(event.customer.fullName.ifBlank { "Unnamed customer" }, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (event.type == CustomerEventType.BIRTHDAY) "Birthday" else "Anniversary",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (event.customer.mobile.isNotBlank()) {
                    Text(event.customer.mobile, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    event.occurrenceDate.format(DateTimeFormatter.ofPattern("d MMM")),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    when (event.daysUntil) {
                        0 -> "Today!"
                        1 -> "Tomorrow"
                        else -> "in ${event.daysUntil} days"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
