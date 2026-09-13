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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urunkarpm.pingpin.ui.theme.ElectricBlue

/**
 * State representation for items inside navigation bar.
 */
data class LiquidNavItem(
    val icon: ImageVector,
    val activeIcon: ImageVector = icon,
    val label: String,
    val badgeCount: Int = 0
)

/**
 * Backward compatibility type alias for existing codebase callers.
 */
typealias NavItemData = LiquidNavItem

/**
 * Default sample navigation items for PingPin.
 */
val DefaultPingPinNavItems = listOf(
    LiquidNavItem(Icons.Outlined.Home, Icons.Filled.Home, "Home"),
    LiquidNavItem(Icons.Outlined.Insights, Icons.Filled.Insights, "Insights"),
    LiquidNavItem(Icons.Outlined.Settings, Icons.Filled.Settings, "Settings")
)

/**
 * High-Definition Apple-Style Liquid Glass Floating Navigation Bar.
 * Features subtle, refined spring bounce animations for tab tiles and selection puck.
 */
@Composable
fun LiquidGlassBottomBar(
    items: List<LiquidNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    barHeight: Dp = 64.dp,
    containerCornerRadius: Dp = 32.dp,
    indicatorCornerRadius: Dp = 24.dp
) {
    val haptic = LocalHapticFeedback.current
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    val validIndex = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))

    val capsuleShape = RoundedCornerShape(containerCornerRadius)
    val indicatorShape = RoundedCornerShape(indicatorCornerRadius)

    // High-Clarity Glass Container Fill
    val containerGlassBrush = remember(isDark) {
        if (isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xF0141923),
                    Color(0xE60F131C)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFF1F5F9)
                )
            )
        }
    }

    // Solid Non-Fading Border Colors (Uniform 360-degree hairline contrast)
    val solidBorderColor = remember(isDark) {
        if (isDark) {
            Color.White.copy(alpha = 0.25f)
        } else {
            Color(0xFF475569) // Crisp Slate Dark Border in Light Mode
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        var containerWidthPx by remember { mutableIntStateOf(0) }
        val itemCount = items.size.coerceAtLeast(1)

        val targetX = if (containerWidthPx > 0) {
            (containerWidthPx.toFloat() / itemCount) * validIndex
        } else 0f

        // Hardware-accelerated puck animation with subtle spring bounce
        val indicatorOffsetPxState = animateFloatAsState(
            targetValue = targetX,
            animationSpec = spring(
                dampingRatio = 0.72f,
                stiffness = 1200f
            ),
            label = "subtle_puck_bounce"
        )

        // Outer Floating Liquid Glass Panel Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .shadow(
                    elevation = if (isDark) 20.dp else 12.dp,
                    shape = capsuleShape,
                    ambientColor = if (isDark) Color.Black.copy(alpha = 0.60f) else Color(0xFF0F172A).copy(alpha = 0.18f),
                    spotColor = if (isDark) Color.Black.copy(alpha = 0.70f) else Color(0xFF1E293B).copy(alpha = 0.22f)
                )
                .clip(capsuleShape)
                .background(containerGlassBrush)
                .border(width = 1.5.dp, color = solidBorderColor, shape = capsuleShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 6.dp)
                    .onSizeChanged { containerWidthPx = it.width }
            ) {
                // High-contrast Liquid Glass selection puck
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(1f / itemCount)
                        .graphicsLayer { translationX = indicatorOffsetPxState.value }
                        .padding(horizontal = 4.dp)
                        .clip(indicatorShape)
                        .background(
                            if (isDark) {
                                ElectricBlue.copy(alpha = 0.30f)
                            } else {
                                ElectricBlue.copy(alpha = 0.18f)
                            }
                        )
                        .border(
                            width = 1.2.dp,
                            color = if (isDark) Color(0xFF60A5FA).copy(alpha = 0.80f) else ElectricBlue,
                            shape = indicatorShape
                        )
                )

                // Tab Buttons Row
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEachIndexed { index, item ->
                        val isSelected = index == validIndex
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()

                        val activeColor = if (isDark) Color.White else Color(0xFF1D4ED8)
                        val inactiveColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF334155)

                        val animatedColorState = animateColorAsState(
                            targetValue = if (isSelected) activeColor else inactiveColor,
                            animationSpec = tween(durationMillis = 150),
                            label = "TabColor_$index"
                        )

                        // Subtle, refined spring bounce scale animation for tile buttons
                        val tileScaleState = animateFloatAsState(
                            targetValue = if (isPressed) 0.92f else if (isSelected) 1.04f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = 0.70f,
                                stiffness = 1100f
                            ),
                            label = "subtle_tile_bounce_$index"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .semantics {
                                    this.role = Role.Tab
                                    this.selected = isSelected
                                    this.contentDescription = if (item.badgeCount > 0) {
                                        "${item.label}, ${item.badgeCount} unread notifications"
                                    } else {
                                        item.label
                                    }
                                }
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null
                                ) {
                                    if (!isSelected) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onItemSelected(index)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = tileScaleState.value
                                        scaleY = tileScaleState.value
                                    }
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (isSelected) item.activeIcon else item.icon,
                                    contentDescription = null,
                                    tint = animatedColorState.value,
                                    modifier = Modifier.size(22.dp)
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                Text(
                                    text = item.label,
                                    color = animatedColorState.value,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    letterSpacing = 0.2.sp,
                                    maxLines = 1
                                )
                            }

                            if (item.badgeCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 8.dp, end = 12.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.error)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Backward-compatible wrapper for PingPin's navbar callers.
 */
@Composable
fun LiquidGlassNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassBottomBar(
        items = DefaultPingPinNavItems,
        selectedIndex = selectedTab,
        onItemSelected = onTabSelected,
        modifier = modifier
    )
}
