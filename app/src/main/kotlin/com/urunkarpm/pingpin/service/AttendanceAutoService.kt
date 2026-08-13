package com.urunkarpm.pingpin.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.urunkarpm.pingpin.R
import com.urunkarpm.pingpin.data.local.AppDatabase
import com.urunkarpm.pingpin.data.repository.AttendanceRepository
import com.urunkarpm.pingpin.data.repository.OfficeConfigRepository
import com.urunkarpm.pingpin.receiver.WifiConnectionReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Short-lived foreground service that checks WiFi SSID against the configured office network
 * and marks attendance automatically when a match is found.
 *
 * Lifecycle:
 *  1. Started by [WifiConnectionReceiver] when a WiFi connection event fires.
 *  2. Posts a low-key "Checking attendance…" foreground notification immediately.
 *  3. Runs [AttendanceService.checkAndMarkAttendance] on IO dispatcher.
 *  4. On SUCCESS  → updates notification to "✅ Attendance marked", sets daily guard.
 *  5. On ALREADY_MARKED → sets daily guard (no duplicate notification).
 *  6. On any other result → stops silently.
 *  7. Always calls [stopSelf] when done.
 */
class AttendanceAutoService : Service() {

    companion object {
        private const val TAG = "AttendanceAutoService"
        private const val FOREGROUND_NOTIF_ID = 2001
        private const val CHANNEL_ID = "auto_attendance_service_channel"
        private const val SUCCESS_NOTIF_ID = 2002
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onCreate() {
        super.onCreate()
        createServiceChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Post foreground notification immediately to satisfy Android's 5-second rule
        startForeground(FOREGROUND_NOTIF_ID, buildCheckingNotification())

        scope.launch {
            try {
                val db = AppDatabase.getInstance(applicationContext)
                val officeConfigRepo = OfficeConfigRepository(db.officeConfigDao())
                val attendanceRepo = AttendanceRepository(db.attendanceRecordDao())

                val config = officeConfigRepo.getConfig()
                if (config == null || config.ssid.isEmpty()) {
                    Log.d(TAG, "No office config/SSID set — stopping.")
                    stopSelf()
                    return@launch
                }

                val wifiService = WifiService(applicationContext)
                val attendanceService = AttendanceService(applicationContext, wifiService)

                val result = attendanceService.checkAndMarkAttendance(
                    officeConfig = config,
                    attendanceRepo = attendanceRepo,
                    onAttendanceMarked = {
                        // Show a success notification (separate from the foreground one)
                        showSuccessNotification()
                    }
                )

                Log.d(TAG, "Auto-attendance result: $result")

                when (result) {
                    AttendanceCheckResult.SUCCESS -> {
                        // Set the daily guard so the receiver won't fire again today
                        WifiConnectionReceiver.markTodayAsHandled(applicationContext)
                    }
                    AttendanceCheckResult.ALREADY_MARKED -> {
                        // Attendance was already recorded; still set the guard to prevent
                        // further unnecessary service launches for the rest of the day
                        WifiConnectionReceiver.markTodayAsHandled(applicationContext)
                        Log.d(TAG, "Attendance already marked for today — daily guard set.")
                    }
                    AttendanceCheckResult.WIFI_MISMATCH -> {
                        Log.d(TAG, "Connected WiFi does not match office SSID — not marking.")
                    }
                    AttendanceCheckResult.NON_WORKING_DAY -> {
                        // Set guard on non-working days too so we don't keep checking all day
                        WifiConnectionReceiver.markTodayAsHandled(applicationContext)
                        Log.d(TAG, "Non-working day — daily guard set, no attendance marked.")
                    }
                    AttendanceCheckResult.NO_OFFICE_CONFIG,
                    AttendanceCheckResult.ERROR -> {
                        Log.w(TAG, "Attendance check failed with result: $result")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error in AttendanceAutoService", e)
            } finally {
                stopSelf()
            }
        }

        // If the system kills the service before it finishes, don't restart automatically
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        // Remove the "Checking attendance…" foreground notification
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(FOREGROUND_NOTIF_ID)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Notification helpers ───────────────────────────────────────────────

    private fun buildCheckingNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("PingPin")
            .setContentText("Checking office WiFi attendance…")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSilent(true)
            .setOngoing(true)
            .build()
    }

    private fun showSuccessNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, NotificationService.ATTENDANCE_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Attendance Marked Automatically ✅")
            .setContentText("You're connected to the office WiFi — attendance recorded for today.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(SUCCESS_NOTIF_ID, notification)
    }

    private fun createServiceChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Auto Attendance Service",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Background attendance check when connected to office WiFi"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}
