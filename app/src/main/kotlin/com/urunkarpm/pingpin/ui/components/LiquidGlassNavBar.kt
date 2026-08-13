package com.urunkarpm.pingpin.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class NavItemData(
    val icon: ImageVector,
    val activeIcon: ImageVector,
    val label: String
)

private val NavItems = listOf(
    NavItemData(Icons.Outlined.Home, Icons.Filled.Home, "Home"),
    NavItemData(Icons.Outlined.Insights, Icons.Filled.Insights, "Insights"),
    NavItemData(Icons.Outlined.Settings, Icons.Filled.Settings, "Settings")
)

@Composable
fun LiquidGlassNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val capsuleShape = RoundedCornerShape(32.dp)
    val puckShape = RoundedCornerShape(24.dp)
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    val validIndex = selectedTab.coerceIn(0, NavItems.size - 1)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        val navBg = if (isDark) {
            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        }

        val navBorder = if (isDark) {
            Color.White.copy(alpha = 0.12f)
        } else {
            Color.Black.copy(alpha = 0.08f)
        }

        // Outer Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = if (isDark) 12.dp else 6.dp,
                    shape = capsuleShape,
                    ambientColor = Color.Black.copy(alpha = 0.15f),
                    spotColor = Color.Black.copy(alpha = 0.2f)
                )
                .clip(capsuleShape)
                .background(navBg)
                .border(width = 1.dp, color = navBorder, shape = capsuleShape)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val totalWidth = maxWidth
                val itemWidth = totalWidth / NavItems.size

                val animatedLeftOffset by animateDpAsState(
                    targetValue = itemWidth * validIndex,
                    animationSpec = tween(durationMillis = 300, easing = LinearEasing),
                    label = "SelectionPuckOffset"
                )

                // Selection Pill Background
                Box(
                    modifier = Modifier
                        .offset(x = animatedLeftOffset)
                        .width(itemWidth)
                        .fillMaxHeight()
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                        .clip(puckShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )

                // Nav Buttons Row
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavItems.forEachIndexed { index, item ->
                        val isSelected = index == validIndex
                        val interactionSource = remember { MutableInteractionSource() }

                        val flipProgress by animateFloatAsState(
                            targetValue = if (isSelected) 1f else 0f,
                            animationSpec = tween(durationMillis = 300, easing = LinearEasing),
                            label = "IconTextFlip_$index"
                        )

                        val rotationX = if (flipProgress <= 0.5f) {
                            flipProgress * 180f
                        } else {
                            (flipProgress - 1f) * 180f
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null
                                ) {
                                    if (!isSelected) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onTabSelected(index)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .graphicsLayer {
                                        this.rotationX = rotationX
                                        cameraDistance = 12f * density
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (flipProgress <= 0.5f) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Text(
                                        text = item.label,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.3.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


