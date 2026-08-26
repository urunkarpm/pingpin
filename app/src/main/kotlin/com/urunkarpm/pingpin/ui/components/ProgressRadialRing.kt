package com.urunkarpm.pingpin.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics

@Composable
fun ProgressRadialRing(
    percentage: Float, // 0.0 to 100.0
    size: Dp = 90.dp,
    strokeWidth: Dp = 9.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val animatedPercentage by animateFloatAsState(
        targetValue = percentage.coerceIn(0f, 100f),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "ProgressSweep"
    )

    val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

    val gradientBrush = remember(color) {
        Brush.sweepGradient(
            colors = listOf(
                color,
                color.copy(alpha = 0.85f),
                color
            )
        )
    }

    Box(
        modifier = Modifier
            .size(size)
            .semantics(mergeDescendants = true) {
                contentDescription = "WFO target compliance gauge: ${animatedPercentage.toInt()} percent"
                progressBarRangeInfo = ProgressBarRangeInfo(animatedPercentage, 0f..100f)
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val currentSweepAngle = (animatedPercentage / 100f) * 360f
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f
            val arcSize = androidx.compose.ui.geometry.Size(
                width = size.toPx() - strokePx,
                height = size.toPx() - strokePx
            )

            // Background Track Ring
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx)
            )

            // Animated Active Arc
            if (currentSweepAngle > 0f) {
                drawArc(
                    brush = gradientBrush,
                    startAngle = -90f,
                    sweepAngle = currentSweepAngle,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        Text(
            text = "${animatedPercentage.toInt()}%",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = (-0.5).sp
        )
    }
}
