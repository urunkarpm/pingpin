package com.urunkarpm.pingpin.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Authentic High-Definition iOS-Style Liquid Glassmorphism Surface Container.
 * Features 100% solid non-fading perimeter borders for sharp, high-contrast edge definition.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    borderColor: Color? = null,
    backgroundColor: Color? = null,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    role: Role? = if (onClick != null) Role.Button else null,
    contentDescription: String? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val bgColor = MaterialTheme.colorScheme.background
    val isDark = remember(bgColor) { bgColor.red < 0.5f }

    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }

    // ponytail: Single-pass background container fill for high-performance card rendering. Ceiling: M3 surface elevation. Upgrade: Multi-pass liquid glass shader.
    val containerBgColor = remember(backgroundColor, isDark) {
        backgroundColor ?: if (isDark) Color(0xD9141923) else Color(0xFFFFFFFF)
    }

    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val solidBorderColor = remember(borderColor, isDark, outlineVariant) {
        borderColor ?: if (isDark) {
            Color.White.copy(alpha = 0.22f)
        } else {
            outlineVariant.copy(alpha = 0.30f)
        }
    }

    val cardModifier = if (onClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.97f else 1.0f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "GlassCardScale"
        )
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (isDark) 8.dp else 4.dp,
                shape = shape
            )
            .clip(shape)
            .background(containerBgColor)
            .border(width = 1.2.dp, color = solidBorderColor, shape = shape)
            .semantics {
                role?.let { this.role = it }
                contentDescription?.let { this.contentDescription = it }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClickLabel = onClickLabel
            ) {
                // ponytail: Native HapticFeedbackType.LongPress for immediate tactile feedback. Upgrade: Custom Vibrator waveform API.
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
    } else {
        modifier
            .shadow(
                elevation = if (isDark) 6.dp else 3.dp,
                shape = shape
            )
            .clip(shape)
            .background(containerBgColor)
            .border(width = 1.2.dp, color = solidBorderColor, shape = shape)
            .semantics {
                contentDescription?.let { this.contentDescription = it }
            }
    }

    Box(
        modifier = cardModifier.padding(20.dp),
        content = content
    )
}
