package com.urunkarpm.pingpin.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urunkarpm.pingpin.ui.theme.EmeraldGreen
import com.urunkarpm.pingpin.ui.theme.rememberTactileFeedback

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

private val DAY_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val FULL_DAY_NAMES = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

/**
 * Clean WFO Days Selector (Day pills only, without cluttering preset buttons).
 */
@Composable
fun WfoDaysSelector(
    wfoDaysMask: Int,
    onMaskChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
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
                text = "WFO Days (Work from Office)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
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

        val tactile = com.urunkarpm.pingpin.ui.theme.rememberTactileFeedback()

        // Days Selection Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DAY_LABELS.forEachIndexed { index, label ->
                val isSelected = (wfoDaysMask and (1 shl index)) != 0
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                val scale by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (isPressed) 0.90f else if (isSelected) 1.05f else 1.0f,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                    ),
                    label = "WfoDayPillScale"
                )

                val bgColor by androidx.compose.animation.animateColorAsState(
                    targetValue = if (isSelected) EmeraldGreen else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 200),
                    label = "WfoDayPillBg"
                )

                val textColor by androidx.compose.animation.animateColorAsState(
                    targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 200),
                    label = "WfoDayPillText"
                )

                val borderColor by androidx.compose.animation.animateColorAsState(
                    targetValue = if (isSelected) EmeraldGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 200),
                    label = "WfoDayPillBorder"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(CircleShape)
                        .background(bgColor)
                        .border(1.2.dp, borderColor, CircleShape)
                        .semantics {
                            this.role = Role.Checkbox
                            this.selected = isSelected
                            this.stateDescription = if (isSelected) "Selected" else "Not selected"
                            this.contentDescription = "${FULL_DAY_NAMES[index]} WFO day"
                        }
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClickLabel = "Toggle ${FULL_DAY_NAMES[index]} WFO day"
                        ) {
                            tactile.tick()
                            val newMask = wfoDaysMask xor (1 shl index)
                            onMaskChanged(newMask)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = textColor,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
