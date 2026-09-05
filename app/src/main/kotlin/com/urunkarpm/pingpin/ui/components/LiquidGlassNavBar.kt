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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urunkarpm.pingpin.ui.theme.ElectricBlue
import kotlin.math.roundToInt

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
 * Modern High-Contrast Floating Navigation Bar with zero-recomposition hardware accelerated indicator.
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

    // Solid/High-opacity container background for zero blur distortion
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

        // Hardware-accelerated puck animation (State read deferred to IntOffset lambda)
        val indicatorOffsetPxState = animateFloatAsState(
            targetValue = targetX,
            animationSpec = spring(
                dampingRatio = 0.82f,
                stiffness = 1400f
            ),
            label = "iOSPuckOffset"
        )

        // Outer Floating Panel Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 6.dp)
                    .onSizeChanged { containerWidthPx = it.width }
            ) {
                // High-contrast selection pill (Zero recompositions & layout passes during movement)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(1f / itemCount)
                        .graphicsLayer { translationX = indicatorOffsetPxState.value }
                        .padding(horizontal = 4.dp)
                        .clip(indicatorShape)
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

                        val activeColor = if (isDark) Color.White else ElectricBlue
                        val inactiveColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

                        val animatedColorState = animateColorAsState(
                            targetValue = if (isSelected) activeColor else inactiveColor,
                            animationSpec = tween(durationMillis = 150),
                            label = "TabColor_$index"
                        )

                        val scaleState = animateFloatAsState(
                            targetValue = if (isSelected) 1.05f else 1.0f,
                            animationSpec = spring(stiffness = Spring.StiffnessHigh),
                            label = "TabScale_$index"
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
                                        scaleX = scaleState.value
                                        scaleY = scaleState.value
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
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
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
