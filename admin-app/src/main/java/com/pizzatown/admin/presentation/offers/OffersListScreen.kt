package com.pizzatown.admin.presentation.offers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pizzatown.admin.core.common.UiState
import com.pizzatown.admin.domain.model.Offer
import com.pizzatown.admin.presentation.components.EmptyView
import com.pizzatown.admin.presentation.components.ErrorView
import com.pizzatown.admin.presentation.components.LoadingView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OffersListScreen(
    onBack: () -> Unit,
    onAddOffer: () -> Unit,
    onEditOffer: (String) -> Unit,
    viewModel: OffersViewModel = hiltViewModel()
) {
    val state by viewModel.offersState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offers & Banners") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAddOffer, text = { Text("Add Offer") }, icon = {})
        }
    ) { padding ->
        when (val current = state) {
            is UiState.Loading -> LoadingView(Modifier.padding(padding))
            is UiState.Error -> ErrorView(current.message, modifier = Modifier.padding(padding))
            is UiState.Empty -> EmptyView(
                "No offers yet. Tap \"Add Offer\" to create a promotional banner for the customer app.",
                Modifier.padding(padding)
            )
            is UiState.Success -> {
                LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(current.data, key = { it.id }) { offer ->
                        OfferRow(
                            offer = offer,
                            onToggleActive = { active -> viewModel.setActive(offer, active) },
                            onEdit = { onEditOffer(offer.id) },
                            onDelete = { viewModel.deleteOffer(offer) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OfferRow(
    offer: Offer,
    onToggleActive: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(onClick = onEdit, modifier = modifier) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = offer.imageUrl.ifBlank { null },
                contentDescription = offer.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(offer.title, style = MaterialTheme.typography.titleMedium)
                if (offer.description.isNotBlank()) {
                    Text(offer.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Switch(checked = offer.active, onCheckedChange = onToggleActive)
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete ${offer.title}")
            }
        }
    }
}
