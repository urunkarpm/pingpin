package com.urunkarpm.pingpin.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
 * Default navigation items for PingPin.
 */
val DefaultPingPinNavItems = listOf(
    LiquidNavItem(Icons.Outlined.Home, Icons.Filled.Home, "Home"),
    LiquidNavItem(Icons.Outlined.Insights, Icons.Filled.Insights, "Insights"),
    LiquidNavItem(Icons.Outlined.Settings, Icons.Filled.Settings, "Settings")
)

/**
 * Floating Liquid Glass Bottom Navigation Bar.
 * Perfectly matching the floating glass aesthetics of the settings navigation dock.
 */
@Composable
fun LiquidGlassBottomBar(
    items: List<LiquidNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    containerCornerRadius: Dp = 26.dp
) {
    val haptic = LocalHapticFeedback.current
    val validIndex = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    val containerShape = RoundedCornerShape(containerCornerRadius)

    val isDarkTheme = MaterialTheme.colorScheme.background.red < 0.5f

    val targetContainerBg = if (isDarkTheme) Color(0xD9141923) else Color(0xF5FFFFFF)
    val containerBg by animateColorAsState(
        targetValue = targetContainerBg,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "container_bg"
    )

    val borderGradient = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
        )
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
        shape = containerShape,
        color = containerBg,
        border = BorderStroke(width = 1.5.dp, brush = borderGradient),
        shadowElevation = if (isDarkTheme) 14.dp else 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = index == validIndex
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                val pressScale by animateFloatAsState(
                    targetValue = if (isPressed) 0.94f else 1.0f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
                    label = "press_scale_$index"
                )

                Surface(
                    onClick = {
                        if (!isSelected) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onItemSelected(index)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer {
                            scaleX = pressScale
                            scaleY = pressScale
                        }
                        .semantics {
                            this.role = Role.Tab
                            this.selected = isSelected
                            this.contentDescription = if (item.badgeCount > 0) {
                                "${item.label}, ${item.badgeCount} unread"
                            } else {
                                item.label
                            }
                        },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shadowElevation = if (isSelected) 4.dp else 0.dp
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = isSelected,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                                        scaleIn(initialScale = 0.85f, animationSpec = tween(220, easing = FastOutSlowInEasing)))
                                    .togetherWith(
                                        fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                                                scaleOut(targetScale = 0.85f, animationSpec = tween(180, easing = FastOutSlowInEasing))
                                    )
                            },
                            label = "NavTabAnim_$index",
                            contentAlignment = Alignment.Center
                        ) { active ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (active) item.activeIcon else item.icon,
                                    contentDescription = null,
                                    tint = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = item.label,
                                    fontSize = 12.5.sp,
                                    fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (item.badgeCount > 0 && !isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(end = 6.dp)
                                    .size(6.dp)
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
