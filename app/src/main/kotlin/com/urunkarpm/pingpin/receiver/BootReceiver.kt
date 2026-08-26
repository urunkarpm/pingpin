package com.urunkarpm.pingpin.receiver

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.urunkarpm.pingpin.data.local.AppDatabase
import com.urunkarpm.pingpin.service.NotificationService
import com.urunkarpm.pingpin.service.WifiCheckWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "System event received in BootReceiver: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_DATE_CHANGED ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
        ) {
            WifiCheckWorker.schedulePeriodicCheck(context)
            WifiConnectionReceiver.registerWifiNetworkCallback(context)
            rescheduleAlarms(context)
        }
    }

    private fun rescheduleAlarms(context: Context) {
        // Fast DirectBoot-compatible reschedule using SharedPreferences
        try {
            val prefs = context.getSharedPreferences(NotificationService.PREFS_NAME, Context.MODE_PRIVATE)
            val checkInTime = prefs.getString("checkInTime", null)
            val checkOutTime = prefs.getString("checkOutTime", null)
            val workingDaysMask = prefs.getInt("workingDaysMask", 0x1F)
            val portalUrl = prefs.getString("portalUrl", "") ?: ""
            val enabled = prefs.getBoolean("enabled", true)

            if (enabled && !checkInTime.isNullOrEmpty() && !checkOutTime.isNullOrEmpty()) {
                val notificationService = NotificationService(context)
                notificationService.scheduleCheckInAlarm(checkInTime, workingDaysMask, portalUrl)
                notificationService.scheduleCheckOutAlarm(checkOutTime, workingDaysMask, portalUrl)
                Log.d(TAG, "Rescheduled alarms via SharedPreferences successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error rescheduling alarms via prefs: ${e.message}", e)
        }

        // Room DB reschedule as secondary confirmation
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val config = db.officeConfigDao().getConfig()
                if (config != null) {
                    val notificationService = NotificationService(context)
                    notificationService.scheduleAlarmsFromConfig(config)
                    Log.d(TAG, "Rescheduled alarms via Room DB successfully")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling alarms via DB: ${e.message}", e)
            }
        }
    }
}
