package com.urunkarpm.pingpin.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
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
 * Default sample navigation items for PingPin.
 */
val DefaultPingPinNavItems = listOf(
    LiquidNavItem(Icons.Outlined.Home, Icons.Filled.Home, "Home"),
    LiquidNavItem(Icons.Outlined.Insights, Icons.Filled.Insights, "Insights"),
    LiquidNavItem(Icons.Outlined.Settings, Icons.Filled.Settings, "Settings")
)

/**
 * Bottom Navigation Bar matching reference design.
 * Adapts dynamically to Light and Dark app themes.
 */
@Composable
fun LiquidGlassBottomBar(
    items: List<LiquidNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    containerCornerRadius: Dp = 32.dp
) {
    val haptic = LocalHapticFeedback.current
    val validIndex = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    val containerShape = RoundedCornerShape(
        topStart = containerCornerRadius,
        topEnd = containerCornerRadius,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )

    // Strictly check active Compose theme background color (without querying system OS override)
    val isDarkTheme = MaterialTheme.colorScheme.background.red < 0.5f

    // Theme adaptive colors
    val targetContainerBg = if (isDarkTheme) Color(0xFF141618) else Color(0xFFFFFFFF)
    val targetBorderColor = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)

    val containerBg by animateColorAsState(
        targetValue = targetContainerBg,
        animationSpec = tween(durationMillis = 250),
        label = "container_bg"
    )

    val borderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = tween(durationMillis = 250),
        label = "border_color"
    )

    val activePillBg = if (isDarkTheme) Color.White else Color(0xFF0F172A)
    val activeContentColor = if (isDarkTheme) Color(0xFF141618) else Color.White
    val inactiveContentColor = if (isDarkTheme) Color(0xFF9CA3AF) else Color(0xFF64748B)

    val pillShape = CircleShape

    // ponytail: using fixed horizontal arrangement for tabs; ceiling at 6 tabs. Upgrade path: scrollable LazyRow if tab count > 6.
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = borderColor, shape = containerShape),
        shape = containerShape,
        color = containerBg,
        shadowElevation = if (isDarkTheme) 8.dp else 12.dp
    ) {
        // ponytail: Smooth bottom nav animation by removing redundant animateContentSize layout listener and unifying transition specs to 220ms FastOutSlowInEasing.
        // Ceiling: Standard pill width & color transition. Upgrade path: Canvas-rendered fluid liquid indicator if custom shader morphing is required.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = index == validIndex
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                val pillBgColor by animateColorAsState(
                    targetValue = if (isSelected) activePillBg else Color.Transparent,
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                    label = "pill_bg_$index"
                )

                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) activeContentColor else inactiveContentColor,
                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                    label = "content_color_$index"
                )

                val pressScale by animateFloatAsState(
                    targetValue = if (isPressed) 0.94f else 1.0f,
                    animationSpec = tween(durationMillis = 100),
                    label = "press_scale_$index"
                )

                val horizontalPadding by animateDpAsState(
                    targetValue = if (isSelected) 20.dp else 14.dp,
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                    label = "horizontal_padding_$index"
                )

                Box(
                    modifier = Modifier
                        .clip(pillShape)
                        .background(pillBgColor)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            if (!isSelected) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onItemSelected(index)
                            }
                        }
                        .graphicsLayer {
                            scaleX = pressScale
                            scaleY = pressScale
                        }
                        .padding(horizontal = horizontalPadding, vertical = 10.dp)
                        .semantics {
                            this.role = Role.Tab
                            this.selected = isSelected
                            this.contentDescription = if (item.badgeCount > 0) {
                                "${item.label}, ${item.badgeCount} unread"
                            } else {
                                item.label
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) item.activeIcon else item.icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(22.dp)
                        )

                        AnimatedVisibility(
                            visible = isSelected,
                            enter = fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing)) + expandHorizontally(
                                animationSpec = tween(220, easing = FastOutSlowInEasing),
                                expandFrom = Alignment.Start
                            ),
                            exit = fadeOut(animationSpec = tween(120, easing = FastOutSlowInEasing)) + shrinkHorizontally(
                                animationSpec = tween(180, easing = FastOutSlowInEasing),
                                shrinkTowards = Alignment.Start
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.label,
                                    color = contentColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    if (item.badgeCount > 0 && !isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
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
