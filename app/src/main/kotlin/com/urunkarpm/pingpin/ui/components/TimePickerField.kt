package com.urunkarpm.pingpin.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun TimePickerField(
    label: String,
    time24: String,
    onTimeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val formatted12Hour = remember(time24) { TimeFormatUtils.format24To12Hour(time24) }

    Box(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                this.role = Role.Button
                this.contentDescription = "$label: $formatted12Hour. Tap to change time."
            }
            .clickable {
                showDialog = true
            }
    ) {
        OutlinedTextField(
            value = formatted12Hour,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLeadingIconColor = MaterialTheme.colorScheme.primary
            )
        )
    }

    if (showDialog) {
        AdvancedTimePickerDialog(
            title = "Select $label Time",
            initialTime24 = time24,
            onTimeSelected = { selectedTime24 ->
                onTimeSelected(selectedTime24)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}
