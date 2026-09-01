package com.urunkarpm.pingpin.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.urunkarpm.pingpin.AlarmActivity
import com.urunkarpm.pingpin.R
import com.urunkarpm.pingpin.receiver.NotificationActionReceiver
import kotlinx.coroutines.*

class AlarmSoundService : Service() {

    companion object {
        private const val TAG = "AlarmSoundService"

        const val ACTION_START_ALARM = "com.urunkarpm.pingpin.ACTION_START_ALARM"
        const val ACTION_STOP_ALARM = "com.urunkarpm.pingpin.ACTION_STOP_ALARM"

        const val EXTRA_ALARM_ID = "alarmId"
        const val EXTRA_TITLE = "title"
        const val EXTRA_PORTAL_URL = "portalUrl"
        const val EXTRA_ACTION_TYPE = "actionType"

        private const val AUTO_STOP_DELAY_MS = 10 * 60 * 1000L // 10 minutes safety timeout

        fun startAlarmSound(
            context: Context,
            alarmId: Int = NotificationService.CHECK_IN_ALARM_ID,
            title: String = "ATTENDANCE ALARM",
            portalUrl: String = "",
            actionType: String = ""
        ) {
            val resolvedActionType = if (actionType.isNotBlank()) actionType else {
                val isCheckOut = alarmId == NotificationService.CHECK_OUT_ALARM_ID || alarmId == NotificationService.CHECK_OUT_SNOOZE_ID || title.contains("CHECK-OUT", ignoreCase = true)
                if (isCheckOut) "CHECK_OUT" else "CHECK_IN"
            }
            val intent = Intent(context, AlarmSoundService::class.java).apply {
                action = ACTION_START_ALARM
                putExtra(EXTRA_ALARM_ID, alarmId)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_PORTAL_URL, portalUrl)
                putExtra(EXTRA_ACTION_TYPE, resolvedActionType)
            }
            try {
                ContextCompat.startForegroundService(context, intent)
                Log.d(TAG, "Started AlarmSoundService for alarmId=$alarmId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start AlarmSoundService: ${e.message}", e)
            }
        }

        fun stopAlarmSound(context: Context) {
            val intent = Intent(context, AlarmSoundService::class.java).apply {
                action = ACTION_STOP_ALARM
            }
            try {
                context.startService(intent)
                Log.d(TAG, "Sent ACTION_STOP_ALARM to AlarmSoundService")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop AlarmSoundService: ${e.message}", e)
            }
        }
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timeoutJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_ALARM
        if (action == ACTION_STOP_ALARM) {
            stopAlarm()
            return START_NOT_STICKY
        }

        // Keep CPU awake while alarm sound is ringing
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (wakeLock == null) {
                wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PingPin:AlarmSoundWakeLock")
            }
            if (wakeLock?.isHeld != true) {
                wakeLock?.acquire(AUTO_STOP_DELAY_MS)
                Log.d(TAG, "Acquired WakeLock for AlarmSoundService")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring WakeLock in AlarmSoundService: ${e.message}", e)
        }

        val alarmId = intent?.getIntExtra(EXTRA_ALARM_ID, NotificationService.CHECK_IN_ALARM_ID)
            ?: NotificationService.CHECK_IN_ALARM_ID
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "ATTENDANCE ALARM"
        val portalUrl = intent?.getStringExtra(EXTRA_PORTAL_URL) ?: ""
        val isCheckOut = alarmId == NotificationService.CHECK_OUT_ALARM_ID || alarmId == NotificationService.CHECK_OUT_SNOOZE_ID || title.contains("CHECK-OUT", ignoreCase = true)
        val actionType = intent?.getStringExtra(EXTRA_ACTION_TYPE) ?: (if (isCheckOut) "CHECK_OUT" else "CHECK_IN")

        val notification = buildForegroundNotification(alarmId, title, portalUrl, actionType)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    alarmId,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(alarmId, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed: ${e.message}", e)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                @Suppress("DEPRECATION")
                startForeground(alarmId, notification)
            }
        }

        startAudioAndVibration()
        startSafetyTimeout()

        return START_STICKY
    }

    private fun startAudioAndVibration() {
        requestAudioFocus()

        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            mediaPlayer?.release()
            mediaPlayer = null

            try {
                val player = MediaPlayer.create(applicationContext, R.raw.beep, audioAttributes, 0)
                if (player != null) {
                    player.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                    player.isLooping = true
                    player.setOnErrorListener { _, what, extra ->
                        Log.e(TAG, "MediaPlayer error in beep loop (what=$what, extra=$extra). Falling back to system ringtone.")
                        fallbackToSystemAlarmRingtone(audioAttributes)
                        true
                    }
                    player.start()
                    mediaPlayer = player
                    Log.d(TAG, "Alarm sound started via MediaPlayer beep loop.")
                } else {
                    fallbackToSystemAlarmRingtone(audioAttributes)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed initializing R.raw.beep: ${e.message}, attempting fallback.")
                fallbackToSystemAlarmRingtone(audioAttributes)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting alarm audio: ${e.message}", e)
        }

        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            val pattern = longArrayOf(0, 500, 500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting vibration: ${e.message}", e)
        }
    }

    private fun fallbackToSystemAlarmRingtone(audioAttributes: AudioAttributes) {
        try {
            mediaPlayer?.release()
            val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val player = MediaPlayer().apply {
                setDataSource(applicationContext, alarmUri)
                setAudioAttributes(audioAttributes)
                setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                isLooping = true
                prepare()
                start()
            }
            mediaPlayer = player
            Log.d(TAG, "Alarm sound running via system ringtone fallback.")
        } catch (ex: Exception) {
            Log.e(TAG, "Failed starting ringtone fallback: ${ex.message}", ex)
        }
    }

    private fun requestAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(attributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener { focusChange ->
                        Log.d(TAG, "Audio focus changed: $focusChange")
                    }
                    .build()

                audioFocusRequest = focusRequest
                audioManager?.requestAudioFocus(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager?.requestAudioFocus(
                    null,
                    AudioManager.STREAM_ALARM,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting audio focus: ${e.message}", e)
        }
    }

    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error abandoning audio focus: ${e.message}", e)
        }
    }

    private fun buildForegroundNotification(alarmId: Int, title: String, portalUrl: String, actionType: String): Notification {
        val alarmIntent = Intent(this, AlarmActivity::class.java).apply {
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
            this,
            alarmId,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SNOOZE
            putExtra(NotificationActionReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(NotificationActionReceiver.EXTRA_PORTAL_URL, portalUrl)
            putExtra("actionType", actionType)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            this,
            alarmId * 10 + 1,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openPortalIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_OPEN_PORTAL
            putExtra(NotificationActionReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(NotificationActionReceiver.EXTRA_PORTAL_URL, portalUrl)
            putExtra("actionType", actionType)
        }
        val openPortalPendingIntent = PendingIntent.getBroadcast(
            this,
            alarmId * 10 + 2,
            openPortalIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_DISMISS
            putExtra(NotificationActionReceiver.EXTRA_ALARM_ID, alarmId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this,
            alarmId * 10 + 3,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeFormatter = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        val formattedTime = timeFormatter.format(java.util.Date())

        val smallLayout = android.widget.RemoteViews(packageName, R.layout.notification_small).apply {
            setTextViewText(R.id.notif_title, title)
            setTextViewText(R.id.notif_text, "Tap to open HR portal or swipe to manage alarm")
            setTextViewText(R.id.notif_time, formattedTime)
            setOnClickPendingIntent(R.id.btn_notif_snooze, snoozePendingIntent)
            setOnClickPendingIntent(R.id.btn_notif_open, openPortalPendingIntent)
            setOnClickPendingIntent(R.id.btn_notif_dismiss, dismissPendingIntent)
        }

        val expandedLayout = android.widget.RemoteViews(packageName, R.layout.notification_expanded).apply {
            setTextViewText(R.id.notif_expanded_title, title)
            setTextViewText(R.id.notif_expanded_text, "Don't forget to mark your daily attendance on the office HR portal.")
            setTextViewText(R.id.notif_expanded_time, formattedTime)
            setOnClickPendingIntent(R.id.btn_notif_expanded_snooze, snoozePendingIntent)
            setOnClickPendingIntent(R.id.btn_notif_expanded_open, openPortalPendingIntent)
            setOnClickPendingIntent(R.id.btn_notif_expanded_dismiss, dismissPendingIntent)
        }

        return NotificationCompat.Builder(this, NotificationService.ALARM_CHANNEL_ID)
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
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }

    private fun startSafetyTimeout() {
        timeoutJob?.cancel()
        timeoutJob = serviceScope.launch {
            delay(AUTO_STOP_DELAY_MS)
            Log.w(TAG, "Safety timeout reached (10 mins). Stopping alarm sound.")
            stopAlarm()
        }
    }

    private fun stopAlarm() {
        timeoutJob?.cancel()
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping mediaPlayer: ${e.message}")
        }

        try {
            vibrator?.cancel()
            vibrator = null
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling vibrator: ${e.message}")
        }

        abandonAudioFocus()

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing wakeLock: ${e.message}", e)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
        Log.d(TAG, "AlarmSoundService stopped cleanly.")
    }

    override fun onDestroy() {
        stopAlarm()
        serviceScope.cancel()
        super.onDestroy()
    }
}
