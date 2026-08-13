package com.urunkarpm.pingpin.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.urunkarpm.pingpin.AlarmActivity
import com.urunkarpm.pingpin.MainActivity
import com.urunkarpm.pingpin.R
import com.urunkarpm.pingpin.data.local.entity.OfficeConfigEntity
import com.urunkarpm.pingpin.receiver.AlarmReceiver
import java.util.Calendar

class NotificationService(private val context: Context) {

    companion object {
        private const val TAG = "NotificationService"

        const val ATTENDANCE_CHANNEL_ID = "attendance_channel"
        const val ALARM_CHANNEL_ID = "alarm_channel_v4"

        const val CHECK_IN_ALARM_ID = 101
        const val CHECK_OUT_ALARM_ID = 102
        const val CHECK_IN_SNOOZE_ID = 103
        const val CHECK_OUT_SNOOZE_ID = 104

        const val PREFS_NAME = "pingpin_native_alarm_prefs"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    init {
        createNotificationChannels()
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
        } else {
            true
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attendanceChannel = NotificationChannel(
                ATTENDANCE_CHANNEL_ID,
                "Attendance Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for attendance marking and reminders"
                enableVibration(true)
            }

            val beepSoundUri = android.net.Uri.parse("android.resource://" + context.packageName + "/" + R.raw.beep)
            val alarmChannel = NotificationChannel(
                ALARM_CHANNEL_ID,
                "Check-In & Check-Out Clock Alarms",
                NotificationManager.IMPORTANCE_MAX
            ).apply {
                description = "Daily check-in and check-out alarms"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 500, 500)
                setBypassDnd(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setSound(
                    beepSoundUri,
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }

            notificationManager.createNotificationChannel(attendanceChannel)
            notificationManager.createNotificationChannel(alarmChannel)
        }
    }

    fun saveNativeAlarmConfig(
        checkInTime: String,
        checkOutTime: String,
        workingDaysMask: Int,
        portalUrl: String,
        enabled: Boolean = true
    ) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("checkInTime", checkInTime)
            .putString("checkOutTime", checkOutTime)
            .putInt("workingDaysMask", workingDaysMask)
            .putString("portalUrl", portalUrl)
            .putBoolean("enabled", enabled)
            .apply()
    }

    fun scheduleAlarmsFromConfig(config: OfficeConfigEntity) {
        saveNativeAlarmConfig(
            checkInTime = config.checkInTime,
            checkOutTime = config.checkOutTime,
            workingDaysMask = config.workingDaysMask,
            portalUrl = config.portalUrl,
            enabled = true
        )
        scheduleCheckInAlarm(config.checkInTime, config.workingDaysMask, config.portalUrl)
        scheduleCheckOutAlarm(config.checkOutTime, config.workingDaysMask, config.portalUrl)
    }

    fun scheduleCheckInAlarm(checkInTimeStr: String, workingDaysMask: Int, portalUrl: String) {
        val (hour, minute) = parseTime(checkInTimeStr) ?: return
        val targetTime = getNextOccurrence(hour, minute, workingDaysMask)

        setExactAlarm(
            alarmId = CHECK_IN_ALARM_ID,
            timeInMillis = targetTime.timeInMillis,
            title = "CHECK-IN ALARM",
            portalUrl = portalUrl
        )
        Log.d(TAG, "Check-in alarm scheduled for $targetTime")
    }

    fun scheduleCheckOutAlarm(checkOutTimeStr: String, workingDaysMask: Int, portalUrl: String) {
        val (hour, minute) = parseTime(checkOutTimeStr) ?: return
        val targetTime = getNextOccurrence(hour, minute, workingDaysMask)

        setExactAlarm(
            alarmId = CHECK_OUT_ALARM_ID,
            timeInMillis = targetTime.timeInMillis,
            title = "CHECK-OUT ALARM",
            portalUrl = portalUrl
        )
        Log.d(TAG, "Check-out alarm scheduled for $targetTime")
    }

    fun snoozeAlarm(alarmId: Int, durationMins: Int = 10, portalUrl: String = "") {
        cancelAlarm(alarmId)
        val isCheckIn = alarmId == CHECK_IN_ALARM_ID || alarmId == CHECK_IN_SNOOZE_ID
        val snoozeId = if (isCheckIn) CHECK_IN_SNOOZE_ID else CHECK_OUT_SNOOZE_ID
        val snoozeTime = System.currentTimeMillis() + (durationMins * 60 * 1000L)

        setExactAlarm(
            alarmId = snoozeId,
            timeInMillis = snoozeTime,
            title = if (isCheckIn) "SNOOZED CHECK-IN ALARM" else "SNOOZED CHECK-OUT ALARM",
            portalUrl = portalUrl
        )
    }

    fun cancelAlarm(alarmId: Int) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        notificationManager.cancel(alarmId)
    }

    fun cancelCheckOutAlarm() {
        cancelAlarm(CHECK_OUT_ALARM_ID)
        cancelAlarm(CHECK_OUT_SNOOZE_ID)
    }

    private fun setExactAlarm(
        alarmId: Int,
        timeInMillis: Long,
        title: String,
        portalUrl: String
    ) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarmId", alarmId)
            putExtra("title", title)
            putExtra("portalUrl", portalUrl)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            alarmId,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExactAlarms()) {
                Log.w(TAG, "Exact alarm permission missing. Falling back to allowWhileIdle.")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
                }
                return
            }

            val clockInfo = AlarmManager.AlarmClockInfo(timeInMillis, showPendingIntent)
            alarmManager.setAlarmClock(clockInfo, pendingIntent)
            Log.d(TAG, "Exact clock alarm set successfully for ID $alarmId at $timeInMillis")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException setting alarm clock: ${e.message}", e)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            }
        }
    }

    fun showAttendanceSuccessNotification() {
        val builder = NotificationCompat.Builder(context, ATTENDANCE_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Attendance Marked Successfully")
            .setContentText("Your attendance has been recorded for today.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(1, builder.build())
    }

    private fun parseTime(timeStr: String): Pair<Int, Int>? {
        val parts = timeStr.split(":")
        if (parts.size < 2) return null
        return try {
            Pair(parts[0].toInt(), parts[1].toInt())
        } catch (_: Exception) {
            null
        }
    }

    private fun getNextOccurrence(hour: Int, minute: Int, workingDaysMask: Int): Calendar {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        // If target time is in the past or within 60 seconds of current time (e.g., when alarm just fired), move to tomorrow
        if (cal.timeInMillis <= System.currentTimeMillis() + 60_000L) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Guard: if no working days are configured, skip the loop to avoid scheduling on random days
        if (workingDaysMask == 0) {
            Log.w(TAG, "workingDaysMask is 0 — no working days configured. Alarm will not be scheduled.")
            return cal
        }

        var safetyLimit = 0
        while (!WorkingDays.isWorkingDay(cal, workingDaysMask) && safetyLimit < 14) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            safetyLimit++
        }

        if (safetyLimit >= 14) {
            Log.e(TAG, "getNextOccurrence: no valid working day found in 14 days for mask=$workingDaysMask")
        }

        return cal
    }

    /**
     * Fires a test alarm exactly [delaySeconds] seconds from now.
     * Bypasses working-day filtering — fires unconditionally regardless of day or schedule.
     * Used from Settings to verify the full alarm → AlarmReceiver → AlarmActivity pipeline.
     */
    fun fireTestAlarm(delaySeconds: Int = 5) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedPortalUrl = prefs.getString("portalUrl", "") ?: ""
        val testAlarmId = 999
        val triggerAt = System.currentTimeMillis() + (delaySeconds * 1_000L)
        setExactAlarm(
            alarmId = testAlarmId,
            timeInMillis = triggerAt,
            title = "🔔 TEST ALARM",
            portalUrl = savedPortalUrl
        )
        Log.d(TAG, "Test alarm scheduled to fire in ${delaySeconds}s (at $triggerAt) with portalUrl='$savedPortalUrl'")
    }
}
