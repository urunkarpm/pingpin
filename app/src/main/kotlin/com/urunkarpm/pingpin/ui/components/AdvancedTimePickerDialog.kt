package com.urunkarpm.pingpin.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Locale

object TimeFormatUtils {
    fun parse24HourTime(time24: String): Pair<Int, Int> {
        val parts = time24.split(":")
        if (parts.size >= 2) {
            val hour = parts[0].trim().toIntOrNull()
            val minute = parts[1].trim().toIntOrNull()
            if (hour != null && minute != null) {
                return Pair(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
            }
        }
        return Pair(9, 30)
    }

    fun format12To24Hour(hour: Int, minute: Int): String {
        return String.format(Locale.US, "%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    }

    fun format24To12Hour(time24: String): String {
        val (hour, minute) = parse24HourTime(time24)
        return formatHourMinuteTo12(hour, minute)
    }

    fun formatHourMinuteTo12(hour: Int, minute: Int): String {
        val period = if (hour >= 12) "PM" else "AM"
        val hour12 = when (val h = hour % 12) {
            0 -> 12
            else -> h
        }
        return String.format(Locale.US, "%02d:%02d %s", hour12, minute, period)
    }

    fun calculateShiftDuration(checkIn: String, checkOut: String): String {
        return try {
            val inParts = checkIn.split(":").map { it.toInt() }
            val outParts = checkOut.split(":").map { it.toInt() }
            val inMins = inParts[0] * 60 + inParts[1]
            var outMins = outParts[0] * 60 + outParts[1]
            if (outMins < inMins) outMins += 24 * 60
            val diff = outMins - inMins
            val hours = diff / 60
            val mins = diff % 60
            if (mins == 0) "$hours hrs 00 mins" else "$hours hrs ${mins} mins"
        } catch (e: Exception) {
            "8 hrs 00 mins"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedTimePickerDialog(
    title: String,
    initialTime24: String,
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val (initialHour, initialMinute) = remember(initialTime24) {
        TimeFormatUtils.parse24HourTime(initialTime24)
    }

    var selectedHour by remember(initialTime24) { mutableIntStateOf(initialHour) }
    var selectedMinute by remember(initialTime24) { mutableIntStateOf(initialMinute) }

    val timePickerState = key(selectedHour, selectedMinute) {
        rememberTimePickerState(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            is24Hour = false
        )
    }

    var isTextInputMode by remember { mutableStateOf(false) }

    val isCheckIn = title.lowercase().contains("check-in") || title.lowercase().contains("in")
    val presets = remember(isCheckIn) {
        if (isCheckIn) {
            listOf("08:30", "09:00", "09:30", "10:00")
        } else {
            listOf("17:00", "17:30", "18:00", "18:30")
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(28.dp))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    RoundedCornerShape(28.dp)
                ),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "12-Hour Alarm Clock",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = TimeFormatUtils.formatHourMinuteTo12(
                                timePickerState.hour,
                                timePickerState.minute
                            ),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Time Picker Body (Dial or Input Mode)
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isTextInputMode) {
                        TimeInput(state = timePickerState)
                    } else {
                        TimePicker(state = timePickerState)
                    }
                }

                // Presets row & Keyboard Mode Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quick Presets:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    IconButton(
                        onClick = { isTextInputMode = !isTextInputMode },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isTextInputMode) Icons.Default.Schedule else Icons.Default.Keyboard,
                            contentDescription = "Toggle Input Mode",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(presets) { preset24 ->
                        val formatted12 = TimeFormatUtils.format24To12Hour(preset24)
                        val isSelected = TimeFormatUtils.format12To24Hour(
                            timePickerState.hour,
                            timePickerState.minute
                        ) == preset24

                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val (h, m) = TimeFormatUtils.parse24HourTime(preset24)
                                selectedHour = h
                                selectedMinute = m
                            },
                            label = { Text(formatted12, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            val selected24 = TimeFormatUtils.format12To24Hour(
                                timePickerState.hour,
                                timePickerState.minute
                            )
                            onTimeSelected(selected24)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Set Time", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
