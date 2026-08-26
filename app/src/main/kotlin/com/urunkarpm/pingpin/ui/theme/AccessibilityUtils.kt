package com.urunkarpm.pingpin.ui.theme

import android.provider.Settings
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Helper to check if system "Reduce Motion" or zero animator scale is enabled.
 */
@Composable
fun rememberIsReduceMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            val durationScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            val transitionScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE,
                1.0f
            )
            durationScale == 0f || transitionScale == 0f
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Modifier enforcing Android standard 48dp x 48dp minimum touch target size
 * for interactive elements without altering baseline layout alignment.
 */
fun Modifier.minimumTouchTargetSize(
    minWidth: Dp = 48.dp,
    minHeight: Dp = 48.dp
): Modifier = this.defaultMinSize(minWidth = minWidth, minHeight = minHeight)
