package com.urunkarpm.pingpin

import android.app.ActivityManager
import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ProcessLifecycleOwner
import com.gdelataillade.alarm.services.AlarmRingingLiveData
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val ALARM_CHANNEL = "com.urunkarpm.pingpin/alarm"
    private val OEM_CHANNEL = "com.urunkarpm.pingpin/oem_battery"

    // Cached MethodChannel so the AlarmRingingLiveData observer can invoke
    // a Dart callback when the alarm fires while the app is in background
    // (the Dart ringStream does not fire in that state).
    private var alarmMethodChannel: MethodChannel? = null

    // Set when the observer fires before the Flutter engine is ready, then
    // flushed in configureFlutterEngine.
    private var pendingAlarmRinging = false

    // Alarm id captured during cold-start by full-screen intent. Flutter reads
    // this during initialize() and pushes the alarm screen over whatever route
    // is current — including the freshly-built widget tree on cold start.
    private var coldStartAlarmId: Int = 0

    companion object {
        /**
         * Launches MainActivity from any context (e.g. from our alarm observer).
         * Works even when the app process is dead or the screen is off.
         */
        @JvmStatic
        fun launchAlarmActivity(context: Context) {
            try {
                val intent = Intent(context, MainActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                    )
                    putExtra("alarm_ringing", true)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        // ── Alarm channel ──────────────────────────────────────────────────
        val alarmChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, ALARM_CHANNEL)
        alarmMethodChannel = alarmChannel
        alarmChannel
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "bringToForeground" -> {
                        bringToForeground()
                        result.success(true)
                    }
                    "dismissNotifications" -> {
                        val id = call.argument<Int>("id")
                        dismissNotifications(id)
                        result.success(true)
                    }
                    "getColdStartAlarmId" -> {
                        result.success(coldStartAlarmId)
                    }
                    "consumeColdStartAlarmId" -> {
                        val id = coldStartAlarmId
                        coldStartAlarmId = 0
                        result.success(id)
                    }
                    "checkOverlayPermission" -> {
                        val canDraw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            Settings.canDrawOverlays(this)
                        } else {
                            true
                        }
                        result.success(canDraw)
                    }
                    "requestOverlayPermission" -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")
                            ).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(intent)
                        }
                        result.success(true)
                    }
                    "checkFullScreenIntentPermission" -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            result.success(notificationManager.canUseFullScreenIntent())
                        } else {
                            result.success(true)
                        }
                    }
                    "requestFullScreenIntentPermission" -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                Uri.parse("package:$packageName")
                            ).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(intent)
                        }
                        result.success(true)
                    }
                    else -> result.notImplemented()
                }
            }

        // Flush any alarm-ringing event that arrived before the engine was ready.
        if (pendingAlarmRinging) {
            pendingAlarmRinging = false
            try {
                alarmChannel.invokeMethod("onAlarmRinging", null)
            } catch (e: Exception) { e.printStackTrace() }
        }

        // ── OEM Battery channel ────────────────────────────────────────────
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, OEM_CHANNEL)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "getDeviceInfo" -> {
                        result.success(
                            mapOf(
                                "manufacturer" to Build.MANUFACTURER,
                                "brand" to Build.BRAND,
                                "model" to Build.MODEL,
                            )
                        )
                    }
                    "launchOemSettings" -> {
                        val action = call.argument<String>("action")
                        val pkg = call.argument<String>("package")
                        val cls = call.argument<String>("class")
                        launchOemSettings(action, pkg, cls)
                        result.success(true)
                    }
                    else -> result.notImplemented()
                }
            }
    }

    private fun bringToForeground() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
                val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                keyguardManager?.requestDismissKeyguard(this, null)
            }
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )

            // Move task to front immediately ahead of any active application
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            activityManager?.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_WITH_HOME)

            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun dismissNotifications(id: Int?) {
        try {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (id != null) notificationManager.cancel(id)
            else notificationManager.cancelAll()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Attempts to open the OEM-specific battery/autostart settings screen.
     * Falls back to the standard battery optimization settings if the
     * deep-link fails (e.g. on stock Android where the OEM intent doesn't exist).
     */
    private fun launchOemSettings(action: String?, pkg: String?, cls: String?) {
        var launched = false

        // 1. Try OEM-specific deep-link
        if (action != null && pkg != null) {
            try {
                val intent = Intent(action).apply {
                    if (cls != null) {
                        component = ComponentName(pkg, cls)
                    } else {
                        `package` = pkg
                    }
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                launched = true
            } catch (e: Exception) { e.printStackTrace() }
        }

        // 2. Try action alone (e.g. android.intent.action.MAIN with package)
        if (!launched && action != null) {
            try {
                val intent = Intent(action).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                launched = true
            } catch (e: Exception) { e.printStackTrace() }
        }

        // 3. Fallback: standard Android battery optimization settings for this app
        if (!launched) {
            try {
                val intent = Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")
                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                startActivity(intent)
            } catch (e: Exception) {
                // Last resort: open general battery settings
                try {
                    startActivity(
                        Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                } catch (e2: Exception) { e2.printStackTrace() }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Capture the alarm id (101/102) BEFORE super.onCreate so the Flutter
        // engine can read it as soon as it boots. This is the critical path
        // for cold-start-via-fullScreenIntent: the system launches MainActivity,
        // the activity must already know which alarm is ringing so Dart can
        // push the alarm screen on first frame.
        captureColdStartAlarmId()

        super.onCreate(savedInstanceState)
        applyWindowFlags()
        observeAlarmRinging()

        // If the LiveData already says an alarm is ringing (because AlarmService
        // postValue(true)'d before MainActivity launched), bring the window to
        // front immediately so the user sees a full-screen alarm — not the
        // home screen under the notification.
        if (AlarmRingingLiveData.instance.value == true) {
            bringToForeground()
        }
    }

    /**
     * Snapshot the alarm id that fired this cold-start. The alarm package
     * posts the ringing id to AlarmRingingLiveData AFTER the notification
     * fires; we read the currently-ringing alarm from the package's storage
     * shim indirectly via the intent extras the AlarmService already wrote.
     *
     * Fallback: if no extras are present we leave coldStartAlarmId = 0 and let
     * Dart's ringStream listener (which fires post-Alarm.init()) handle it.
     */
    private fun captureColdStartAlarmId() {
        try {
            // The AlarmService passes id via AlarmReceiver, but by the time
            // MainActivity is created via full-screen intent the broadcast has
            // already been processed. Use AlarmRingingLiveData value as the
            // canonical "is ringing right now" signal.
            // The id itself is recoverable from the foreground notification.
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val active = nm.activeNotifications
            for (s in active) {
                val id = s.id
                if (id == 101 || id == 102) {
                    coldStartAlarmId = id
                    return
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        bringToForeground()
    }

    override fun onResume() {
        super.onResume()
        applyWindowFlags()
    }

    private fun applyWindowFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
    }

    /**
     * Observe AlarmRingingLiveData from the alarm package. When the alarm
     * starts ringing, this observer fires and brings the activity to the
     * foreground — even if the app was in background or the screen was off.
     *
     * This is a NATIVE-side fallback that works independently of the Dart
     * ringStream listener, covering the case where the Flutter engine hasn't
     * fully initialized yet when the alarm fires.
     */
    private fun observeAlarmRinging() {
        AlarmRingingLiveData.instance.observe(this, Observer { isRinging ->
            if (isRinging) {
                bringToForeground()
                // Notify Dart. The Dart ringStream does not fire reliably when
                // the Flutter engine is in the background (e.g. app swiped
                // away, screen off, phone locked), so the native observer
                // pushes an explicit event that the Flutter side listens for.
                val ch = alarmMethodChannel
                if (ch != null) {
                    try {
                        ch.invokeMethod("onAlarmRinging", null)
                    } catch (e: Exception) { e.printStackTrace() }
                } else {
                    // Flutter engine not ready yet — flag so configureFlutterEngine
                    // can flush this event as soon as the channel is wired up.
                    pendingAlarmRinging = true
                }
            }
        })
    }
}





