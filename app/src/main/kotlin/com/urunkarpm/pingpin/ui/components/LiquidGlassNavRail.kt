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

private val NavItems = listOf(
    NavItemData(Icons.Outlined.Home, Icons.Filled.Home, "Home"),
    NavItemData(Icons.Outlined.Insights, Icons.Filled.Insights, "Insights"),
    NavItemData(Icons.Outlined.Settings, Icons.Filled.Settings, "Settings")
)

@Composable
fun LiquidGlassNavRail(
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
            .fillMaxHeight()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 8.dp, top = 16.dp, bottom = 16.dp),
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

        // Outer Rail Pill Container
        Box(
            modifier = Modifier
                .width(76.dp)
                .fillMaxHeight()
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
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp)
            ) {
                val totalHeight = maxHeight
                val itemHeight = totalHeight / NavItems.size

                val animatedTopOffset by animateDpAsState(
                    targetValue = itemHeight * validIndex,
                    animationSpec = tween(durationMillis = 300, easing = LinearEasing),
                    label = "SelectionPuckOffsetRail"
                )

                // Selection Pill Background
                Box(
                    modifier = Modifier
                        .offset(y = animatedTopOffset)
                        .fillMaxWidth()
                        .height(itemHeight)
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                        .clip(puckShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )

                // Nav Buttons Column
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NavItems.forEachIndexed { index, item ->
                        val isSelected = index == validIndex
                        val interactionSource = remember { MutableInteractionSource() }

                        val flipProgress by animateFloatAsState(
                            targetValue = if (isSelected) 1f else 0f,
                            animationSpec = tween(durationMillis = 300, easing = LinearEasing),
                            label = "IconTextFlipRail_$index"
                        )

                        val rotationY = if (flipProgress <= 0.5f) {
                            flipProgress * 180f
                        } else {
                            (flipProgress - 1f) * 180f
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
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
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.graphicsLayer {
                                    this.rotationY = rotationY
                                    cameraDistance = 12f * density
                                }
                            ) {
                                Icon(
                                    imageVector = if (isSelected) item.activeIcon else item.icon,
                                    contentDescription = item.label,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.label,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
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
