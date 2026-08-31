package com.pizzatown.admin.presentation.menu

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pizzatown.admin.core.common.UiState
import com.pizzatown.admin.domain.model.MenuItem
import com.pizzatown.admin.domain.model.Category
import com.pizzatown.admin.domain.model.PricingMode
import com.pizzatown.admin.presentation.components.EmptyView
import com.pizzatown.admin.presentation.components.ErrorView
import com.pizzatown.admin.presentation.components.LoadingView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuListScreen(
    onBack: () -> Unit,
    onAddItem: () -> Unit,
    onEditItem: (String) -> Unit,
    viewModel: MenuListViewModel = hiltViewModel()
) {
    val state by viewModel.menuState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val categoryFilter by viewModel.categoryFilter.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Menu Items") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAddItem, text = { Text("Add Item") }, icon = {})
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::setSearchQuery,
                placeholder = { Text("Search menu items") },
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

            if (allCategories.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(selected = categoryFilter == null, onClick = { viewModel.setCategoryFilter(null) }, label = { Text("All") })
                    }
                    items(allCategories, key = { it.id }) { category ->
                        FilterChip(
                            selected = categoryFilter == category.id,
                            onClick = { viewModel.setCategoryFilter(category.id) },
                            label = { Text(category.name) }
                        )
                    }
                }
            }

            when (val current = state) {
                is UiState.Loading -> LoadingView()
                is UiState.Error -> ErrorView(current.message)
                is UiState.Empty -> EmptyView(
                    if (searchQuery.isNotBlank() || categoryFilter != null)
                        "No menu items match these filters."
                    else "No menu items yet. Tap \"Add Item\" to create your first pizza, burger, drink, or any product."
                )
                is UiState.Success -> {
                    val data = current.data
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(data.items, key = { it.id }) { item ->
                            MenuItemRow(
                                item = item,
                                categoryName = data.categoriesById[item.categoryId]?.name ?: "Uncategorized",
                                onToggleAvailable = { available -> viewModel.setAvailable(item, available) },
                                onEdit = { onEditItem(item.id) },
                                onDelete = { viewModel.deleteItem(item) },
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
private fun MenuItemRow(
    item: MenuItem,
    categoryName: String,
    onToggleAvailable: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(onClick = onEdit, modifier = modifier) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = item.imageUrl.ifBlank { null },
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                Text(categoryName, style = MaterialTheme.typography.bodyMedium)
                val priceLabel = when (item.pricingMode) {
                    PricingMode.FIXED -> "\u20B9${item.basePrice.toInt()}"
                    PricingMode.VARIANTS -> "From \u20B9${item.displayFromPrice().toInt()} \u00B7 ${item.variants.size} variants"
                }
                Text(priceLabel, style = MaterialTheme.typography.bodyMedium)
            }
            Switch(checked = item.available, onCheckedChange = onToggleAvailable)
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete ${item.name}")
            }
        }
    }
}
