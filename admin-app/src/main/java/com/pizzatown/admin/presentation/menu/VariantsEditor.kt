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

/**
 * Dynamic list editor for [VariantDraft]s — e.g. Regular/Medium/Large for
 * a pizza, Single/Double for a burger, or 250ml/500ml/750ml for a drink.
 * Names and prices are entirely admin-defined; nothing is hardcoded.
 */
@Composable
fun VariantsEditor(
    variants: List<VariantDraft>,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
    onUpdate: (uiKey: String, name: String?, price: String?, available: Boolean?) -> Unit
) {
    Column {
        variants.forEach { variant ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = variant.name,
                    onValueChange = { onUpdate(variant.uiKey, it, null, null) },
                    label = { Text("Variant name") },
                    singleLine = true,
                    modifier = Modifier.weight(1.2f)
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = variant.price,
                    onValueChange = { onUpdate(variant.uiKey, null, it, null) },
                    label = { Text("Price") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(0.8f)
                )
                Switch(
                    checked = variant.available,
                    onCheckedChange = { onUpdate(variant.uiKey, null, null, it) }
                )
                IconButton(onClick = { onRemove(variant.uiKey) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove variant ${variant.name}")
                }
            }
        }
        OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
            Text("+ Add Variant")
        }
    }
}
