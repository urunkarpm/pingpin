package com.urunkarpm.pingpin

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.Observer
import com.gdelataillade.alarm.services.AlarmRingingLiveData
import com.gdelataillade.alarm.alarm.AlarmReceiver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Full-screen alarm UI. The system launches this Activity via the alarm
 * package's fullScreenIntent — bypassing the Flutter engine entirely for the
 * critical "show on lock screen / over any app" path.
 *
 * Why native instead of Flutter:
 *   - Renders even when Flutter engine is dead/cold-starting.
 *   - Renders over the lock screen with showWhenLocked + dismissKeyguard.
 *   - No race with ringStream / go_router / coldStartAlarmId plumbing.
 *   - The alarm package's PendingIntent points at the package's MAIN/LAUNCHER
 *     activity (the one in the manifest), so we register THIS activity as the
 *     launcher entry-point — getLaunchIntentForPackage() returns us.
 */
class AlarmActivity : Activity() {

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_PORTAL_URL = "portal_url"

        // SharedPreferences keys mirrored from Dart. Native reads them on
        // activity start so we know which portal/leave mail URL to open.
        private const val PREFS_NAME = "FlutterSharedPreferences"
    }

    private var alarmId: Int = 101
    private var portalUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        alarmId = intent?.getIntExtra(EXTRA_ALARM_ID, 101) ?: 101
        portalUrl = intent?.getStringExtra(EXTRA_PORTAL_URL)
            ?: readPortalUrlFromPrefs()
            ?: ""

        // Show over lock screen + dismiss keyguard BEFORE setContentView so
        // the layout appears immediately when the system unlocks.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            km?.requestDismissKeyguard(this, null)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        setContentView(R.layout.activity_alarm)

        val isCheckIn = alarmId == 101
        val badge = findViewById<TextView>(R.id.badge)
        val clock = findViewById<TextView>(R.id.clock)
        val message = findViewById<TextView>(R.id.message)
        val primary = findViewById<Button>(R.id.primary_btn)
        val secondary = findViewById<Button>(R.id.secondary_btn)

        badge.text = if (isCheckIn) "CHECK-IN ALARM" else "CHECK-OUT ALARM"
        clock.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        message.text = if (isCheckIn) {
            "It is time to check in! Choose Check-in or Leave below."
        } else {
            "It is time to check out! Tap Check-out to open portal."
        }

        primary.text = if (isCheckIn) "CHECK-IN (OPEN PORTAL)" else "CHECK-OUT (OPEN PORTAL)"
        secondary.text = "APPLY FOR LEAVE"
        secondary.visibility = if (isCheckIn) View.VISIBLE else View.GONE

        primary.setOnClickListener {
            stopAlarmAndFinish()
            openPortal()
        }

        secondary.setOnClickListener {
            stopAlarmAndFinish()
            // Also cancel the check-out alarm when user takes leave at check-in.
            if (isCheckIn) {
                sendStopBroadcast(102)
            }
            openLeaveMail()
        }

        // Update the clock every second.
        val clockTicker = object : Thread() {
            override fun run() {
                try {
                    while (!isInterrupted) {
                        sleep(1000)
                        runOnUiThread {
                            clock.text =
                                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                        }
                    }
                } catch (_: InterruptedException) { }
            }
        }
        clockTicker.isDaemon = true
        clockTicker.start()
    }

    override fun onBackPressed() {
        // Don't allow back-press to silently dismiss. User must tap a button.
        // Otherwise the alarm keeps ringing in the background service.
    }

    private fun stopAlarmAndFinish() {
        sendStopBroadcast(alarmId)
        finish()
    }

    private fun sendStopBroadcast(id: Int) {
        try {
            val stopIntent = Intent(this, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_ALARM_STOP
                putExtra("id", id)
            }
            sendBroadcast(stopIntent)
        } catch (_: Exception) { }
    }

    private fun openPortal() {
        if (portalUrl.isBlank()) return
        var raw = portalUrl.trim()
        if (!raw.startsWith("http://") && !raw.startsWith("https://")) {
            raw = "https://$raw"
        }
        try {
            val uri = Uri.parse(raw)
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (_: Exception) { }
    }

    private fun openLeaveMail() {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_SUBJECT, "Leave Application")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Dear Team,\n\nI will be taking leave today.\n\nThank you,"
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (_: Exception) { }
    }

    private fun readPortalUrlFromPrefs(): String? {
        return try {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getString("flutter.portal_url", null)
        } catch (_: Exception) { null }
    }
}
