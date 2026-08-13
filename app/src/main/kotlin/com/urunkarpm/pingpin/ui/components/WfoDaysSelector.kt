package com.urunkarpm.pingpin.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urunkarpm.pingpin.ui.theme.EmeraldGreen

private val DAY_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@Composable
fun WfoDaysSelector(
    wfoDaysMask: Int,
    onMaskChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val wfoCount = (0 until 7).count { (wfoDaysMask and (1 shl it)) != 0 }

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
                text = "WFO DAYS (WORK FROM OFFICE)",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = EmeraldGreen,
                letterSpacing = 0.8.sp
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = EmeraldGreen.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "$wfoCount WFO days / week",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreen,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        // Days Selection Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DAY_LABELS.forEachIndexed { index, label ->
                val isSelected = (wfoDaysMask and (1 shl index)) != 0

                val bgAnim by animateColorAsState(
                    targetValue = if (isSelected) {
                        EmeraldGreen
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "wfo_day_bg"
                )

                val textAnim by animateColorAsState(
                    targetValue = if (isSelected) {
                        Color.White
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "wfo_day_text"
                )

                val borderAnim by animateColorAsState(
                    targetValue = if (isSelected) {
                        EmeraldGreen
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    },
                    label = "wfo_day_border"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgAnim)
                        .border(1.dp, borderAnim, RoundedCornerShape(12.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val newMask = wfoDaysMask xor (1 shl index)
                            onMaskChanged(newMask)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = textAnim,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                    )
                }
            }
        }

        // Quick WFO Presets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            WfoPresetChip(
                label = "Mon - Fri",
                isSelected = wfoDaysMask == 31,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onMaskChanged(31) // 0b0011111 (Mon-Fri)
                },
                modifier = Modifier.weight(1f)
            )
            WfoPresetChip(
                label = "3 Days (M/W/F)",
                isSelected = wfoDaysMask == 21,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onMaskChanged(21) // 1|4|16 = Mon(1) + Wed(4) + Fri(16) = 21
                },
                modifier = Modifier.weight(1f)
            )
            WfoPresetChip(
                label = "2 Days (T/Th)",
                isSelected = wfoDaysMask == 10,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onMaskChanged(10) // 2|8 = Tue(2) + Thu(8) = 10
                },
                modifier = Modifier.weight(1f)
            )
            WfoPresetChip(
                label = "All 7 Days",
                isSelected = wfoDaysMask == 127,
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
private fun WfoPresetChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) EmeraldGreen.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isSelected) EmeraldGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        ),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
