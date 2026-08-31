package com.pizzatown.admin.presentation.menu

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pizzatown.admin.domain.model.PricingMode
import com.pizzatown.admin.domain.model.SelectionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuEditorScreen(
    onBack: () -> Unit,
    viewModel: MenuEditorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var previewImage by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            previewImage = uri
            context.contentResolver.openInputStream(uri)?.use { stream ->
                viewModel.onPendingImage(stream.readBytes())
            }
        }
    }

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "Add Menu Item" else "Edit Menu Item") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(Modifier.padding(16.dp)) {
                    if (state.errorMessage != null) {
                        Text(
                            state.errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Button(
                        onClick = viewModel::save,
                        enabled = !state.isSaving,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(if (state.isNew) "Add Item" else "Save Changes")
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Image picker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { imagePicker.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                val imageModel = previewImage ?: state.imageUrl.ifBlank { null }
                if (imageModel != null) {
                    AsyncImage(
                        model = imageModel,
                        contentDescription = "Item image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.AddAPhoto, contentDescription = "Upload image")
                        Text("Tap to upload image")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Item name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Description") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            CategoryDropdown(
                categories = state.categories,
                selectedId = state.categoryId,
                onSelect = viewModel::onCategoryChange
            )

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Available to customers", modifier = Modifier.weight(1f))
                Switch(checked = state.available, onCheckedChange = viewModel::onAvailableChange)
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Text("Pricing", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            Row {
                FilterChip(
                    selected = state.pricingMode == PricingMode.FIXED,
                    onClick = { viewModel.onPricingModeChange(PricingMode.FIXED) },
                    label = { Text("Fixed Price") }
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = state.pricingMode == PricingMode.VARIANTS,
                    onClick = { viewModel.onPricingModeChange(PricingMode.VARIANTS) },
                    label = { Text("Variants") }
                )
            }

            Spacer(Modifier.height(12.dp))

            if (state.pricingMode == PricingMode.FIXED) {
                OutlinedTextField(
                    value = state.basePrice,
                    onValueChange = viewModel::onBasePriceChange,
                    label = { Text("Base Price (\u20B9)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                VariantsEditor(
                    variants = state.variants,
                    onAdd = viewModel::addVariant,
                    onRemove = viewModel::removeVariant,
                    onUpdate = viewModel::updateVariant
                )
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Text("Customization Groups", style = MaterialTheme.typography.titleMedium)
            Text(
                "e.g. \"Add-ons\", \"Extra Toppings\", \"Size\" — fully optional and applies to any product.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            state.customizationGroups.forEach { group ->
                CustomizationGroupEditor(
                    group = group,
                    onUpdate = { transform -> viewModel.updateGroup(group.uiKey, transform) },
                    onRemove = { viewModel.removeGroup(group.uiKey) },
                    onAddOption = { viewModel.addOption(group.uiKey) },
                    onUpdateOption = { optKey, name, price, avail -> viewModel.updateOption(group.uiKey, optKey, name, price, avail) },
                    onRemoveOption = { optKey -> viewModel.removeOption(group.uiKey, optKey) }
                )
                Spacer(Modifier.height(12.dp))
            }

            OutlinedButton(onClick = viewModel::addGroup, modifier = Modifier.fillMaxWidth()) {
                Text("+ Add Customization Group")
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<com.pizzatown.admin.domain.model.Category>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = categories.find { it.id == selectedId }?.name ?: "Select category"

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (categories.isEmpty()) {
                DropdownMenuItem(text = { Text("No categories yet — add one first") }, onClick = { expanded = false })
            }
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = { onSelect(category.id); expanded = false }
                )
            }
        }
    }
}
