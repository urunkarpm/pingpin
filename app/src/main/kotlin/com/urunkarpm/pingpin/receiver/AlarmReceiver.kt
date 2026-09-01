package com.urunkarpm.pingpin.receiver

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import com.urunkarpm.pingpin.AlarmActivity
import com.urunkarpm.pingpin.service.AlarmSoundService
import com.urunkarpm.pingpin.service.NotificationService

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("alarmId", 101)
        val title = intent.getStringExtra("title") ?: "ATTENDANCE ALARM"
        val portalUrl = intent.getStringExtra("portalUrl") ?: ""

        Log.d(TAG, "Alarm triggered: $alarmId ($title)")

        // Acquire WakeLock to turn screen on immediately if device is asleep
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        @Suppress("DEPRECATION")
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "PingPin:AlarmWakeLock"
        )
        try {
            wakeLock?.acquire(5000L)
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring wake lock: ${e.message}", e)
        }

        val intentActionType = intent.getStringExtra("actionType")
        val isCheckOutType = alarmId == NotificationService.CHECK_OUT_ALARM_ID || alarmId == NotificationService.CHECK_OUT_SNOOZE_ID || title.contains("CHECK-OUT", ignoreCase = true)
        val actionType = if (!intentActionType.isNullOrBlank()) intentActionType else {
            if (isCheckOutType) com.urunkarpm.pingpin.ui.portal.PortalActivity.ACTION_CHECK_OUT else com.urunkarpm.pingpin.ui.portal.PortalActivity.ACTION_CHECK_IN
        }

        val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra("alarmId", alarmId)
            putExtra("actionType", actionType)
            putExtra("title", title)
            putExtra("portalUrl", portalUrl)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_NO_USER_ACTION
            )
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            alarmId,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )



        // Start continuous foreground alarm playback, vibration & ongoing full-screen notification
        AlarmSoundService.startAlarmSound(
            context = context,
            alarmId = alarmId,
            title = title,
            portalUrl = portalUrl,
            actionType = actionType
        )

        // Directly launch AlarmActivity into foreground
        val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ActivityOptions.makeBasic().apply {
                setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
            }.toBundle()
        } else null

        try {
            if (options != null) {
                context.startActivity(alarmIntent, options)
            } else {
                context.startActivity(alarmIntent)
            }
            Log.d(TAG, "Directly started AlarmActivity in foreground")
        } catch (e: Exception) {
            Log.w(TAG, "startActivity failed (${e.message}), trying pendingIntent send fallback")
            try {
                fullScreenPendingIntent.send(context, 0, null, null, null, null, options)
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to launch AlarmActivity: ${ex.message}", ex)
            }
        }

        // Reschedule next occurrence for recurring daily alarms (handles regular & snoozed triggers)
        val isCheckInType = alarmId == NotificationService.CHECK_IN_ALARM_ID || alarmId == NotificationService.CHECK_IN_SNOOZE_ID

        if (isCheckInType || isCheckOutType) {
            try {
                val prefs = NotificationService.getAlarmPreferences(context)
                val checkInTime = prefs.getString("checkInTime", null)
                val checkOutTime = prefs.getString("checkOutTime", null)
                val workingDaysMask = prefs.getInt("workingDaysMask", 0x1F)
                val prefPortalUrl = prefs.getString("portalUrl", portalUrl) ?: portalUrl
                val enabled = prefs.getBoolean("enabled", true)

                if (enabled) {
                    val notifService = NotificationService(context)
                    if (isCheckInType && !checkInTime.isNullOrEmpty()) {
                        notifService.scheduleCheckInAlarm(checkInTime, workingDaysMask, prefPortalUrl)
                        Log.d(TAG, "Successfully auto-rescheduled next check-in alarm (triggered by $alarmId)")
                    }
                    if (isCheckOutType && !checkOutTime.isNullOrEmpty()) {
                        notifService.scheduleCheckOutAlarm(checkOutTime, workingDaysMask, prefPortalUrl)
                        Log.d(TAG, "Successfully auto-rescheduled next check-out alarm (triggered by $alarmId)")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error auto-rescheduling next alarm in receiver: ${e.message}", e)
            }
        }
    }
}
