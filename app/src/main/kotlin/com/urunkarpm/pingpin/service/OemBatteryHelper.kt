package com.urunkarpm.pingpin.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log

data class OemBatteryGuidance(
    val oemName: String,
    val steps: List<String>,
    val intentAction: String,
    val intentPackage: String? = null,
    val intentClass: String? = null
)

object OemBatteryHelper {

    private const val TAG = "OemBatteryHelper"

    fun detectOem(): String? {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()

        return when {
            manufacturer.contains("xiaomi") || brand.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco") -> "Xiaomi"
            manufacturer.contains("samsung") || brand.contains("samsung") -> "Samsung"
            manufacturer.contains("oneplus") || brand.contains("oneplus") -> "OnePlus"
            manufacturer.contains("vivo") || brand.contains("vivo") -> "Vivo"
            manufacturer.contains("oppo") || brand.contains("oppo") || brand.contains("realme") -> "Oppo/Realme"
            manufacturer.contains("huawei") || manufacturer.contains("honor") || brand.contains("huawei") || brand.contains("honor") -> "Huawei/Honor"
            else -> null
        }
    }

    fun getGuidance(): OemBatteryGuidance? {
        return when (detectOem()) {
            "Xiaomi" -> OemBatteryGuidance(
                oemName = "Xiaomi / MIUI",
                steps = listOf(
                    "Open Settings → Apps → Manage apps",
                    "Search for \"PingPin\" and tap it",
                    "Tap \"Battery saver\" → select \"No restrictions\"",
                    "Go back → tap \"Autostart\" → toggle ON"
                ),
                intentAction = "miui.intent.action.APP_PERM_EDITOR",
                intentPackage = "com.miui.securitycenter"
            )
            "Samsung" -> OemBatteryGuidance(
                oemName = "Samsung OneUI",
                steps = listOf(
                    "Open Settings → Battery",
                    "Tap \"Background usage limits\"",
                    "Tap \"Never sleeping apps\"",
                    "Tap + and add \"PingPin\""
                ),
                intentAction = Intent.ACTION_MAIN,
                intentPackage = "com.samsung.android.lool",
                intentClass = "com.samsung.android.sm.battery.ui.BatteryActivity"
            )
            "OnePlus" -> OemBatteryGuidance(
                oemName = "OnePlus / OxygenOS",
                steps = listOf(
                    "Open Settings → Battery → Battery Optimization",
                    "Tap the dropdown → select \"All apps\"",
                    "Find \"PingPin\" → tap → select \"Don't optimize\""
                ),
                intentAction = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
            )
            "Vivo" -> OemBatteryGuidance(
                oemName = "Vivo / FuntouchOS",
                steps = listOf(
                    "Open Settings → Battery → High background power consumption",
                    "Enable \"PingPin\"",
                    "Also: Settings → Apps → PingPin → Battery → \"No restrictions\""
                ),
                intentAction = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            )
            "Oppo/Realme" -> OemBatteryGuidance(
                oemName = "Oppo / Realme / ColorOS",
                steps = listOf(
                    "Open Settings → Battery → \"App quick freeze\"",
                    "Disable \"PingPin\" from the frozen list",
                    "Settings → Apps → PingPin → Battery → \"Allow background activity\""
                ),
                intentAction = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            )
            "Huawei/Honor" -> OemBatteryGuidance(
                oemName = "Huawei / Honor / EMUI",
                steps = listOf(
                    "Open Settings → Apps → Apps → PingPin",
                    "Tap \"Battery\" → select \"Run in background\"",
                    "Phone Manager → Protected apps → enable \"PingPin\""
                ),
                intentAction = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            )
            else -> null
        }
    }

    fun launchOemSettings(context: Context, guidance: OemBatteryGuidance) {
        try {
            val intent = Intent(guidance.intentAction)
            if (!guidance.intentPackage.isNullOrEmpty()) {
                if (!guidance.intentClass.isNullOrEmpty()) {
                    intent.component = ComponentName(guidance.intentPackage, guidance.intentClass)
                } else {
                    intent.setPackage(guidance.intentPackage)
                }
            } else if (guidance.intentAction == Settings.ACTION_APPLICATION_DETAILS_SETTINGS) {
                intent.data = Uri.parse("package:${context.packageName}")
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch OEM settings intent, falling back to standard app info", e)
            try {
                val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {}
        }
    }
}
