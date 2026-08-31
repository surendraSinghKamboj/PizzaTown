package com.pizzatown.admin.presentation.menu

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pizzatown.admin.domain.model.SelectionType

/**
 * Editor for one [GroupDraft] — e.g. "Extra Toppings" (multi-select,
 * optional, max 5) or "Choose Size" (single-select, required). Works
 * identically for any product type since nothing here is food-specific.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizationGroupEditor(
    group: GroupDraft,
    onUpdate: ((GroupDraft) -> GroupDraft) -> Unit,
    onRemove: () -> Unit,
    onAddOption: () -> Unit,
    onUpdateOption: (uiKey: String, name: String?, price: String?, available: Boolean?) -> Unit,
    onRemoveOption: (uiKey: String) -> Unit
) {
    ElevatedCard {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = group.name,
                    onValueChange = { name -> onUpdate { it.copy(name = name) } },
                    label = { Text("Group name (e.g. Add-ons)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove group ${group.name}")
                }
            }

            Spacer(Modifier.height(8.dp))

            Row {
                FilterChip(
                    selected = group.selectionType == SelectionType.SINGLE,
                    onClick = { onUpdate { it.copy(selectionType = SelectionType.SINGLE, maxSelections = "1") } },
                    label = { Text("Single choice") }
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = group.selectionType == SelectionType.MULTIPLE,
                    onClick = { onUpdate { it.copy(selectionType = SelectionType.MULTIPLE) } },
                    label = { Text("Multiple choice") }
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Required", modifier = Modifier.weight(1f))
                Switch(
                    checked = group.required,
                    onCheckedChange = { checked -> onUpdate { it.copy(required = checked) } }
                )
            }

            if (group.selectionType == SelectionType.MULTIPLE) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = group.minSelections,
                        onValueChange = { v -> onUpdate { it.copy(minSelections = v) } },
                        label = { Text("Min") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = group.maxSelections,
                        onValueChange = { v -> onUpdate { it.copy(maxSelections = v) } },
                        label = { Text("Max") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("Options", style = MaterialTheme.typography.labelLarge)

            group.options.forEach { option ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = option.name,
                        onValueChange = { onUpdateOption(option.uiKey, it, null, null) },
                        label = { Text("Option") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = option.priceAdjustment,
                        onValueChange = { onUpdateOption(option.uiKey, null, it, null) },
                        label = { Text("+\u20B9") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(0.8f)
                    )
                    Switch(
                        checked = option.available,
                        onCheckedChange = { onUpdateOption(option.uiKey, null, null, it) }
                    )
                    IconButton(onClick = { onRemoveOption(option.uiKey) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove option ${option.name}")
                    }
                }
            }

            TextButton(onClick = onAddOption) {
                Text("+ Add Option")
            }
        }
    }
}
