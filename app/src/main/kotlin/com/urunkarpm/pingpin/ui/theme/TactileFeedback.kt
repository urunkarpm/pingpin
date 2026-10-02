package com.urunkarpm.pingpin.ui.theme

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * High-definition tactile haptic feedback provider for PingPin.
 * Utilizes modern Android VibrationEffect primitives (API 29+) for crisp,
 * mechanical-feeling ticks and clicks, avoiding sluggish LongPress buzzes.
 */
class TactileFeedback(
    private val context: Context,
    private val composeHaptic: HapticFeedback
) {
    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Crisp, satisfying mechanical tap for buttons, switches, tabs, and toggles.
     */
    fun click() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && vibrator?.hasVibrator() == true) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                composeHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        } catch (_: Exception) {
            composeHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    /**
     * Delicate micro-tick for subtle movements (e.g. day toggling, scrolling tabs).
     */
    fun tick() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && vibrator?.hasVibrator() == true) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                composeHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        } catch (_: Exception) {
            composeHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    /**
     * Confirmatory double-pulse for successful action saves or milestone activations.
     */
    fun success() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator?.hasVibrator() == true) {
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 20, 60, 30),
                        intArrayOf(0, 180, 0, 240),
                        -1
                    )
                )
            } else {
                composeHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        } catch (_: Exception) {
            composeHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
}

@Composable
fun rememberTactileFeedback(): TactileFeedback {
    val context = LocalContext.current
    val composeHaptic = LocalHapticFeedback.current
    return remember(context, composeHaptic) {
        TactileFeedback(context, composeHaptic)
    }
}
