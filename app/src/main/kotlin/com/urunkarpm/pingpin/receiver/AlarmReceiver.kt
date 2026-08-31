package com.urunkarpm.pingpin.receiver

import android.app.ActivityOptions
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.urunkarpm.pingpin.AlarmActivity
import com.urunkarpm.pingpin.R
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
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            alarmId,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build Action PendingIntents for Quick Action Buttons
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SNOOZE
            putExtra(NotificationActionReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(NotificationActionReceiver.EXTRA_PORTAL_URL, portalUrl)
            putExtra("actionType", actionType)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId * 10 + 1,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openPortalIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_OPEN_PORTAL
            putExtra(NotificationActionReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(NotificationActionReceiver.EXTRA_PORTAL_URL, portalUrl)
            putExtra("actionType", actionType)
        }

        val openPortalPendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId * 10 + 2,
            openPortalIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_DISMISS
            putExtra(NotificationActionReceiver.EXTRA_ALARM_ID, alarmId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId * 10 + 3,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeFormatter = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        val formattedTime = timeFormatter.format(java.util.Date())

        val smallLayout = android.widget.RemoteViews(context.packageName, R.layout.notification_small).apply {
            setTextViewText(R.id.notif_title, title)
            setTextViewText(R.id.notif_text, "Tap to open HR portal or swipe to manage alarm")
            setTextViewText(R.id.notif_time, formattedTime)
            setOnClickPendingIntent(R.id.btn_notif_snooze, snoozePendingIntent)
            setOnClickPendingIntent(R.id.btn_notif_open, openPortalPendingIntent)
            setOnClickPendingIntent(R.id.btn_notif_dismiss, dismissPendingIntent)
        }

        val expandedLayout = android.widget.RemoteViews(context.packageName, R.layout.notification_expanded).apply {
            setTextViewText(R.id.notif_expanded_title, title)
            setTextViewText(R.id.notif_expanded_text, "Don't forget to mark your daily attendance on the office HR portal.")
            setTextViewText(R.id.notif_expanded_time, formattedTime)
            setOnClickPendingIntent(R.id.btn_notif_expanded_snooze, snoozePendingIntent)
            setOnClickPendingIntent(R.id.btn_notif_expanded_open, openPortalPendingIntent)
            setOnClickPendingIntent(R.id.btn_notif_expanded_dismiss, dismissPendingIntent)
        }

        val notification = NotificationCompat.Builder(context, NotificationService.ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setColor(android.graphics.Color.parseColor("#6366F1"))
            .setCustomContentView(smallLayout)
            .setCustomBigContentView(expandedLayout)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setContentTitle(title)
            .setContentText("Tap or swipe to manage alarm")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(R.drawable.ic_stat_notification, "Open Portal", openPortalPendingIntent)
            .addAction(R.drawable.ic_stat_notification, "Snooze 10m", snoozePendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        notificationManager?.notify(alarmId, notification)

        // Start continuous foreground alarm playback & vibration
        AlarmSoundService.startAlarmSound(
            context = context,
            alarmId = alarmId,
            title = title,
            portalUrl = portalUrl,
            actionType = actionType
        )

        // Direct launch to bring AlarmActivity into foreground instantly even when phone is actively in use
        val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ActivityOptions.makeBasic().apply {
                setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
            }.toBundle()
        } else null

        try {
            fullScreenPendingIntent.send(context, 0, null, null, null, null, options)
            Log.d(TAG, "Triggered AlarmActivity full-screen launch via fullScreenPendingIntent.send()")
        } catch (e: Exception) {
            Log.w(TAG, "fullScreenPendingIntent.send() failed (${e.message}), falling back to startActivity")
            try {
                if (options != null) {
                    context.startActivity(alarmIntent, options)
                } else {
                    context.startActivity(alarmIntent)
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to start AlarmActivity directly: ${ex.message}", ex)
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
