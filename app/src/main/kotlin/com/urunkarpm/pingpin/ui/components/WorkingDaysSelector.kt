package com.urunkarpm.pingpin.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urunkarpm.pingpin.ui.theme.ElectricBlue

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

private val DAY_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val FULL_DAY_NAMES = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

@Composable
fun WorkingDaysSelector(
    workingDaysMask: Int,
    onMaskChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val activeCount = (0 until 7).count { (workingDaysMask and (1 shl it)) != 0 }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Working Days",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ElectricBlue.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "$activeCount days / week",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricBlue,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        // Days Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DAY_LABELS.forEachIndexed { index, label ->
                val isSelected = (workingDaysMask and (1 shl index)) != 0

                val bgAnim by animateColorAsState(
                    targetValue = if (isSelected) {
                        ElectricBlue
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "day_bg"
                )

                val textAnim by animateColorAsState(
                    targetValue = if (isSelected) {
                        Color.White
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "day_text"
                )

                val borderAnim by animateColorAsState(
                    targetValue = if (isSelected) {
                        ElectricBlue
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    },
                    label = "day_border"
                )

                val scaleAnim by animateFloatAsState(
                    targetValue = if (isSelected) 1.06f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "day_scale"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .scale(scaleAnim)
                        .clip(CircleShape)
                        .background(bgAnim)
                        .border(1.dp, borderAnim, CircleShape)
                        .semantics {
                            this.role = Role.Checkbox
                            this.selected = isSelected
                            this.stateDescription = if (isSelected) "Selected" else "Not selected"
                            this.contentDescription = "${FULL_DAY_NAMES[index]} working day"
                        }
                        .clickable(
                            onClickLabel = "Toggle ${FULL_DAY_NAMES[index]} working day"
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val newMask = workingDaysMask xor (1 shl index)
                            onMaskChanged(newMask)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = textAnim,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }

        // Quick Preset Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PresetChip(
                label = "Mon - Fri",
                isSelected = workingDaysMask == 31,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onMaskChanged(31) // 0b0011111
                },
                modifier = Modifier.weight(1f)
            )
            PresetChip(
                label = "Mon - Sat",
                isSelected = workingDaysMask == 63,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onMaskChanged(63) // 0b0111111
                },
                modifier = Modifier.weight(1f)
            )
            PresetChip(
                label = "All 7 Days",
                isSelected = workingDaysMask == 127,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onMaskChanged(127) // 0b1111111
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) ElectricBlue.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isSelected) ElectricBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        ),
        modifier = modifier.defaultMinSize(minHeight = 48.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) ElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

