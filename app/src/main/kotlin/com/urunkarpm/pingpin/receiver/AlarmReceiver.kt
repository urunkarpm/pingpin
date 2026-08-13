package com.urunkarpm.pingpin.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.urunkarpm.pingpin.AlarmActivity
import com.urunkarpm.pingpin.R
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

        val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra("alarmId", alarmId)
            putExtra("title", title)
            putExtra("portalUrl", portalUrl)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            alarmId,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Post High-Priority Full Screen Intent Notification for system-level pop-up UI
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        val notification = NotificationCompat.Builder(context, NotificationService.ALARM_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText("Tap or swipe to manage alarm")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .build()

        notificationManager?.notify(alarmId, notification)

        // Direct launch to bring AlarmActivity into foreground instantly even when phone is actively in use
        try {
            context.startActivity(alarmIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AlarmActivity directly: ${e.message}", e)
        }

        // Reschedule next occurrence for recurring daily alarms
        if (alarmId == NotificationService.CHECK_IN_ALARM_ID || alarmId == NotificationService.CHECK_OUT_ALARM_ID) {
            try {
                val prefs = context.getSharedPreferences(NotificationService.PREFS_NAME, Context.MODE_PRIVATE)
                val checkInTime = prefs.getString("checkInTime", null)
                val checkOutTime = prefs.getString("checkOutTime", null)
                val workingDaysMask = prefs.getInt("workingDaysMask", 0x1F)
                val prefPortalUrl = prefs.getString("portalUrl", portalUrl) ?: portalUrl
                val enabled = prefs.getBoolean("enabled", true)

                if (enabled) {
                    val notifService = NotificationService(context)
                    if (alarmId == NotificationService.CHECK_IN_ALARM_ID && !checkInTime.isNullOrEmpty()) {
                        notifService.scheduleCheckInAlarm(checkInTime, workingDaysMask, prefPortalUrl)
                        Log.d(TAG, "Successfully auto-rescheduled next check-in alarm")
                    } else if (alarmId == NotificationService.CHECK_OUT_ALARM_ID && !checkOutTime.isNullOrEmpty()) {
                        notifService.scheduleCheckOutAlarm(checkOutTime, workingDaysMask, prefPortalUrl)
                        Log.d(TAG, "Successfully auto-rescheduled next check-out alarm")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error auto-rescheduling next alarm in receiver: ${e.message}", e)
            }
        }
    }
}
