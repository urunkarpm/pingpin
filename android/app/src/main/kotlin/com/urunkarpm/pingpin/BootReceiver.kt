package com.urunkarpm.pingpin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receives BOOT_COMPLETED (and related) broadcasts so that alarms can be
 * rescheduled after a device reboot or app update.
 *
 * AlarmManager alarms do NOT survive reboots. This receiver launches
 * MainActivity so that Flutter's _rescheduleAlarmsFromSavedConfig() runs
 * and re-registers the daily check-in / check-out alarms.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        val isBootOrUpdate = action == Intent.ACTION_BOOT_COMPLETED ||
                action == "android.intent.action.LOCKED_BOOT_COMPLETED" ||
                action == "com.htc.intent.action.QUICKBOOT_POWERON" ||
                action == Intent.ACTION_MY_PACKAGE_REPLACED

        if (!isBootOrUpdate) return

        try {
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
                putExtra("reschedule_alarms", true)
            }
            context.startActivity(launchIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
