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

    val hourOfDay = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val ambientTint = remember(hourOfDay, isDark) {
        when (hourOfDay) {
            in 6..8, in 17..19 -> if (isDark) Color(0x1AFFF3E0) else Color(0x0FFFF8E1) // Warm golden sunrise/sunset ambient glow
            in 20..23, in 0..5 -> if (isDark) Color(0x1E1A237E) else Color(0x10E8EAF6) // Deep night ambient glass tint
            else -> Color.Transparent
        }
    }

    // High-Clarity Translucent Fill
    val glassFillBrush = remember(backgroundColor, isDark, ambientTint) {
        if (backgroundColor != null) {
            Brush.verticalGradient(listOf(backgroundColor, backgroundColor))
        } else if (isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xD9141923),
                    Color(0xC80F131C)
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
                elevation = if (isDark) 14.dp else 8.dp,
                shape = shape,
                clip = false,
                ambientColor = if (isDark) Color.Black.copy(alpha = 0.50f) else Color(0xFF0F172A).copy(alpha = 0.12f),
                spotColor = if (isDark) Color.Black.copy(alpha = 0.60f) else Color(0xFF3B82F6).copy(alpha = 0.18f)
            )
            .clip(shape)
            .background(glassFillBrush)
            .background(ambientTint)
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
                // ponytail: Native HapticFeedbackType.LongPress for immediate tactile feedback (Laws of UX: Doherty Threshold). Upgrade: Custom Vibrator waveform API.
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
    } else {
        modifier
            .shadow(
                elevation = if (isDark) 12.dp else 6.dp,
                shape = shape,
                clip = false,
                ambientColor = if (isDark) Color.Black.copy(alpha = 0.45f) else Color(0xFF0F172A).copy(alpha = 0.10f),
                spotColor = if (isDark) Color.Black.copy(alpha = 0.55f) else Color(0xFF3B82F6).copy(alpha = 0.15f)
            )
            .clip(shape)
            .background(glassFillBrush)
            .background(ambientTint)
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
