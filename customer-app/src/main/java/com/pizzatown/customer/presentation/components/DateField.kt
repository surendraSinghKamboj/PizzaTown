package com.pizzatown.customer.presentation.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A text field that opens a Material3 date picker on tap. Stores/emits
 * epoch millis (UTC midnight of the selected day); 0L means "not set".
 * Used for date of birth and anniversary, which only need day/month/year
 * — no time component, and no need to ever be in the future for a
 * birthday (though anniversaries technically could be recent).
 *
 * [locked] disables further edits once a date has already been saved
 * (date of birth / anniversary are one-time-set fields).
 *
 * NOTE: tapping is detected via [interactionSource] presses, not a plain
 * `Modifier.clickable`. A read-only OutlinedTextField still consumes the
 * initial pointer-down internally (for cursor/selection handling), so an
 * outer `.clickable` modifier never receives the tap and the dialog never
 * opens. Passing our own interactionSource into the field and observing
 * its press interactions is the reliable way to catch the tap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    epochMillis: Long,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    locked: Boolean = false
) {
    var showPicker by remember { mutableStateOf(false) }
    val displayText = if (epochMillis > 0L) formatDate(epochMillis) else ""
    val interactionSource = remember { MutableInteractionSource() }

    if (!locked) {
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { interaction ->
                if (interaction is PressInteraction.Release) {
                    showPicker = true
                }
            }
        }
    }

    OutlinedTextField(
        value = displayText,
        onValueChange = {},
        label = { Text(label) },
        placeholder = { Text(if (locked) "" else "Tap to select") },
        readOnly = true,
        enabled = !locked,
        interactionSource = interactionSource,
        trailingIcon = {
            if (locked) {
                Icon(Icons.Filled.Lock, contentDescription = "Locked — already set")
            } else {
                Icon(Icons.Filled.CalendarToday, contentDescription = null)
            }
        },
        modifier = modifier,
        visualTransformation = VisualTransformation.None
    )

    if (showPicker && !locked) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = epochMillis.takeIf { it > 0L }
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let(onDateSelected)
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private fun formatDate(epochMillis: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(epochMillis))

