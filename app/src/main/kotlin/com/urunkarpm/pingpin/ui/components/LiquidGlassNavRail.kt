package com.urunkarpm.pingpin.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urunkarpm.pingpin.ui.theme.ElectricBlue

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

    val containerBg = if (isDark) {
        Color(0xFF10141D)
    } else {
        Color(0xFFFFFFFF)
    }

    val hairlineBorder = if (isDark) {
        Color.White.copy(alpha = 0.15f)
    } else {
        Color.Black.copy(alpha = 0.10f)
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 8.dp, top = 16.dp, bottom = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer Rail Container
        Box(
            modifier = Modifier
                .width(76.dp)
                .fillMaxHeight()
                .shadow(
                    elevation = if (isDark) 16.dp else 8.dp,
                    shape = capsuleShape,
                    ambientColor = Color.Black.copy(alpha = if (isDark) 0.50f else 0.12f),
                    spotColor = Color.Black.copy(alpha = if (isDark) 0.60f else 0.18f)
                )
                .clip(capsuleShape)
                .background(containerBg)
                .border(width = 1.dp, color = hairlineBorder, shape = capsuleShape)
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
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
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
                        .background(
                            if (isDark) {
                                ElectricBlue.copy(alpha = 0.25f)
                            } else {
                                ElectricBlue.copy(alpha = 0.12f)
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = if (isDark) Color(0xFF60A5FA).copy(alpha = 0.50f) else ElectricBlue.copy(alpha = 0.35f),
                            shape = puckShape
                        )
                )

                // Nav Buttons Column
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NavItems.forEachIndexed { index, item ->
                        val isSelected = index == validIndex
                        val interactionSource = remember { MutableInteractionSource() }

                        val activeColor = if (isDark) Color.White else ElectricBlue
                        val inactiveColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

                        val animatedColor by animateColorAsState(
                            targetValue = if (isSelected) activeColor else inactiveColor,
                            animationSpec = spring(stiffness = Spring.StiffnessMedium),
                            label = "RailTabColor_$index"
                        )

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
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (isSelected) item.activeIcon else item.icon,
                                    contentDescription = item.label,
                                    tint = animatedColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.label,
                                    color = animatedColor,
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
