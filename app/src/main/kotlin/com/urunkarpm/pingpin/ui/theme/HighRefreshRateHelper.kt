package com.urunkarpm.pingpin.ui.theme

import android.app.Activity
import android.os.Build
import android.util.Log
import android.view.WindowManager

object HighRefreshRateHelper {
    private const val TAG = "HighRefreshRateHelper"

    /**
     * Enforces peak native display refresh rate (120Hz / 90Hz / 144Hz) for the Activity Window.
     */
    fun optimizeActivityRefreshRate(activity: Activity) {
        try {
            val window = activity.window ?: return
            enableHighRefreshRate(window.attributes)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.display?.let { display ->
                    val maxMode = display.supportedModes.maxByOrNull { it.refreshRate }
                    if (maxMode != null && maxMode.modeId != window.attributes.preferredDisplayModeId) {
                        val lp = window.attributes
                        lp.preferredDisplayModeId = maxMode.modeId
                        window.attributes = lp
                    }
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                val display = window.windowManager.defaultDisplay
                val maxMode = display?.supportedModes?.maxByOrNull { it.refreshRate }
                if (maxMode != null && maxMode.modeId != window.attributes.preferredDisplayModeId) {
                    val lp = window.attributes
                    lp.preferredDisplayModeId = maxMode.modeId
                    window.attributes = lp
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to set maximum display refresh rate: ${e.message}")
        }
    }

    /**
     * Enables hardware acceleration flag on WindowManager parameters.
     */
    fun enableHighRefreshRate(params: WindowManager.LayoutParams) {
        params.flags = params.flags or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
    }
}

/**
     * Activity extension function to optimize display refresh rate to max available Hz.
 */
fun Activity.optimizeDisplayRefreshRate() {
    HighRefreshRateHelper.optimizeActivityRefreshRate(this)
}
