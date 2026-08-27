package com.urunkarpm.pingpin

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urunkarpm.pingpin.service.NotificationService
import com.urunkarpm.pingpin.ui.theme.PingPinTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.media.MediaPlayer

class AlarmActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        turnScreenOnAndKeyguard()
        startAlarmSoundAndVibration()

        val alarmId = intent.getIntExtra("alarmId", 101)
        val intentActionType = intent.getStringExtra("actionType")
        val intentTitle = intent.getStringExtra("title") ?: ""
        val initialIsCheckIn = when {
            !intentActionType.isNullOrBlank() -> intentActionType == com.urunkarpm.pingpin.ui.portal.PortalActivity.ACTION_CHECK_IN
            alarmId == NotificationService.CHECK_OUT_ALARM_ID || alarmId == NotificationService.CHECK_OUT_SNOOZE_ID || intentTitle.contains("CHECK-OUT", ignoreCase = true) -> false
            else -> true
        }

        val intentPortalUrl = intent.getStringExtra("portalUrl") ?: ""
        val prefs = getSharedPreferences(NotificationService.PREFS_NAME, Context.MODE_PRIVATE)
        val prefPortalUrl = prefs.getString("portalUrl", "") ?: ""
        val initialPortalUrl = if (intentPortalUrl.isNotBlank()) intentPortalUrl else prefPortalUrl

        setContent {
            var portalUrlState by remember { mutableStateOf(initialPortalUrl) }

            LaunchedEffect(Unit) {
                if (portalUrlState.isBlank()) {
                    try {
                        val db = com.urunkarpm.pingpin.data.local.AppDatabase.getInstance(applicationContext)
                        val config = db.officeConfigDao().getConfig()
                        if (config != null && config.portalUrl.isNotBlank()) {
                            portalUrlState = config.portalUrl
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            PingPinTheme(darkTheme = true) {
                AlarmScreenContent(
                    isCheckInMode = initialIsCheckIn,
                    portalUrl = portalUrlState,
                    onCheckIn = {
                        stopAlarmSound()
                        val notifService = NotificationService(this)
                        notifService.dismissNotification(alarmId)
                        if (alarmId == NotificationService.CHECK_IN_SNOOZE_ID || alarmId == NotificationService.CHECK_OUT_SNOOZE_ID) {
                            notifService.cancelAlarm(alarmId)
                        }
                        dismissKeyguardAndExecute(shouldFinish = false) { openPortalAction(com.urunkarpm.pingpin.ui.portal.PortalActivity.ACTION_CHECK_IN, alarmId, portalUrlState) }
                    },
                    onSnooze = {
                        stopAlarmSound()
                        val notifService = NotificationService(this)
                        notifService.snoozeAlarm(alarmId, 10, portalUrlState)
                        finish()
                    },
                    onLeave = {
                        stopAlarmSound()
                        val notifService = NotificationService(this)
                        notifService.dismissNotification(alarmId)
                        if (alarmId == NotificationService.CHECK_IN_SNOOZE_ID || alarmId == NotificationService.CHECK_OUT_SNOOZE_ID) {
                            notifService.cancelAlarm(alarmId)
                        }
                        dismissKeyguardAndExecute { openLeaveMail() }
                    },
                    onCheckOut = {
                        stopAlarmSound()
                        val notifService = NotificationService(this)
                        notifService.dismissNotification(alarmId)
                        if (alarmId == NotificationService.CHECK_IN_SNOOZE_ID || alarmId == NotificationService.CHECK_OUT_SNOOZE_ID) {
                            notifService.cancelAlarm(alarmId)
                        }
                        dismissKeyguardAndExecute(shouldFinish = false) { openPortalAction(com.urunkarpm.pingpin.ui.portal.PortalActivity.ACTION_CHECK_OUT, alarmId, portalUrlState) }
                    }
                )
            }
        }
    }

    private fun turnScreenOnAndKeyguard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )
    }

    private fun startAlarmSoundAndVibration() {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            // Pass audioAttributes to MediaPlayer.create so attributes are applied BEFORE prepare()
            val player = MediaPlayer.create(applicationContext, R.raw.beep, audioAttributes, 0)
            if (player != null) {
                mediaPlayer = player.apply {
                    isLooping = true
                    setLooping(true)
                    setOnCompletionListener { mp ->
                        // Safety net: restart if looping flag didn't take effect
                        try {
                            mp.seekTo(0)
                            mp.start()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    start()
                }
            } else {
                // Fallback to system default alarm uri if MediaPlayer fails
                val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ringtone = RingtoneManager.getRingtone(applicationContext, alarmUri)?.apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        this.audioAttributes = audioAttributes
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        isLooping = true
                    }
                    play()
                }
            }

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
            e.printStackTrace()
        }
    }

    private fun stopAlarmSound() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null

            ringtone?.stop()
            ringtone = null

            vibrator?.cancel()
            vibrator = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        stopAlarmSound()
        super.onDestroy()
    }

    private fun dismissKeyguardAndExecute(shouldFinish: Boolean = true, action: () -> Unit) {
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val safeFinish = {
            window?.decorView?.post {
                finish()
            } ?: finish()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && keyguardManager != null && keyguardManager.isKeyguardLocked) {
            keyguardManager.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    action()
                    if (shouldFinish) safeFinish()
                }
                override fun onDismissError() {
                    action()
                    if (shouldFinish) safeFinish()
                }
                override fun onDismissCancelled() {
                    action()
                    if (shouldFinish) safeFinish()
                }
            })
        } else {
            action()
            if (shouldFinish) safeFinish()
        }
    }

    private fun openPortalAction(actionType: String, currentAlarmId: Int, currentPortalUrl: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = com.urunkarpm.pingpin.data.local.AppDatabase.getInstance(applicationContext)
            val config = db.officeConfigDao().getConfig()
            val portalMode = config?.portalMode ?: "EXTERNAL_BROWSER"
            val url = if (currentPortalUrl.isNotBlank()) currentPortalUrl else (config?.portalUrl ?: "")

            val useFloating = config?.useFloatingPortal ?: true

            withContext(Dispatchers.Main) {
                if (portalMode == "IN_APP_AUTO") {
                    if (useFloating && android.provider.Settings.canDrawOverlays(this@AlarmActivity)) {
                        com.urunkarpm.pingpin.service.portal.FloatingPortalService.startService(
                            context = this@AlarmActivity,
                            actionType = actionType,
                            portalUrl = url,
                            alarmId = currentAlarmId
                        )
                    } else {
                        val intent = com.urunkarpm.pingpin.ui.portal.PortalActivity.createIntent(
                            context = this@AlarmActivity,
                            actionType = actionType,
                            portalUrl = url,
                            alarmId = currentAlarmId
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                        startActivity(intent)
                    }
                } else {
                    openBrowser(url)
                }
                window?.decorView?.post {
                    finish()
                } ?: finish()
            }
        }
    }

    private fun openBrowser(url: String) {
        var rawUrl = url.trim()
        if (rawUrl.isBlank()) {
            val prefs = getSharedPreferences(NotificationService.PREFS_NAME, Context.MODE_PRIVATE)
            rawUrl = prefs.getString("portalUrl", "")?.trim() ?: ""
        }
        if (rawUrl.isBlank()) {
            rawUrl = "https://google.com"
        }
        if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
            rawUrl = "https://$rawUrl"
        }

        val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            android.app.ActivityOptions.makeBasic().apply {
                setPendingIntentBackgroundActivityStartMode(android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
            }.toBundle()
        } else null

        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(rawUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            if (options != null) {
                startActivity(browserIntent, options)
            } else {
                startActivity(browserIntent)
            }
        } catch (e: Exception) {
            android.util.Log.e("AlarmActivity", "Error opening browser: ${e.message}", e)
            try {
                val chooserIntent = Intent.createChooser(
                    Intent(Intent.ACTION_VIEW, Uri.parse(rawUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                    "Open HR Portal"
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (options != null) {
                    startActivity(chooserIntent, options)
                } else {
                    startActivity(chooserIntent)
                }
            } catch (ex: Exception) {
                android.util.Log.e("AlarmActivity", "Error opening chooser: ${ex.message}", ex)
                try {
                    val fallbackIntent = Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    startActivity(fallbackIntent)
                } catch (_: Exception) {}
            }
        }
    }

    private fun openLeaveMail() {
        try {
            val mailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_SUBJECT, "Leave Application")
                putExtra(Intent.EXTRA_TEXT, "Dear Team,\n\nI will be taking leave today.\n\nThank you,")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(mailIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
fun AlarmScreenContent(
    isCheckInMode: Boolean = true,
    portalUrl: String,
    onCheckIn: () -> Unit,
    onSnooze: () -> Unit,
    onLeave: () -> Unit,
    onCheckOut: () -> Unit
) {
    val isCheckIn = isCheckInMode

    // Pitch Black AMOLED color palette
    val bgGradientStart = Color(0xFF000000)
    val bgGradientEnd = Color(0xFF06080F)
    val inkWhite = Color(0xFFF8FAFC)
    val inkMuted = Color(0xFF94A3B8)

    // Dynamic accent theme based on Check-in (Electric Blue/Cyan) vs Check-out (Emerald/Teal)
    val accentPrimary = if (isCheckIn) Color(0xFF3B82F6) else Color(0xFF10B981)
    val accentSecondary = if (isCheckIn) Color(0xFF06B6D4) else Color(0xFF34D399)
    val accentGlow = if (isCheckIn) Color(0x403B82F6) else Color(0x4010B981)

    // Live clock ticker
    var currentTime by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Calendar.getInstance()
            delay(1000L)
        }
    }

    val timeFormat = remember { SimpleDateFormat("hh:mm", Locale.US) }
    val amPmFormat = remember { SimpleDateFormat("a", Locale.US) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.US) }

    val formattedTime = timeFormat.format(currentTime.time)
    val formattedAmPm = amPmFormat.format(currentTime.time)
    val formattedDate = dateFormat.format(currentTime.time)

    // Infinite animations for pulse and expanding sonar ripple rings
    val infiniteTransition = rememberInfiniteTransition(label = "alarm_effects")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val ring1Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1"
    )

    val ring2Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 660, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2"
    )

    val ring3Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 1330, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring3"
    )

    val dotBlinkAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_blink"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(bgGradientStart, bgGradientEnd)
                )
            )
    ) {
        // Ambient background glowing radial aura
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-40).dp)
                .size(360.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(accentGlow, Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Top Section: Status pill & Portal domain badge
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Live Alarm Status Chip
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = Color.White.copy(alpha = 0.07f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        accentPrimary.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .alpha(dotBlinkAlpha)
                                .background(accentPrimary, CircleShape)
                        )
                        Text(
                            text = if (isCheckIn) "CHECK-IN ALARM" else "CHECK-OUT ALARM",
                            color = inkWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }
                }

                // Portal Link pill if available
                if (portalUrl.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.3f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color.White.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = accentSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = portalUrl,
                                color = inkMuted,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Center Hero Display: Ripple Sonar Rings & Giant Clock
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Animated Sonar Ripple Icon Orb
                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Expanding sonar ripple ring 1
                    Box(
                        modifier = Modifier
                            .scale(1.0f + ring1Progress * 0.8f)
                            .alpha((1.0f - ring1Progress).coerceIn(0f, 1f))
                            .fillMaxSize()
                            .border(1.5.dp, accentPrimary, CircleShape)
                    )
                    // Expanding sonar ripple ring 2
                    Box(
                        modifier = Modifier
                            .scale(1.0f + ring2Progress * 0.8f)
                            .alpha((1.0f - ring2Progress).coerceIn(0f, 1f))
                            .fillMaxSize()
                            .border(1.5.dp, accentPrimary, CircleShape)
                    )
                    // Expanding sonar ripple ring 3
                    Box(
                        modifier = Modifier
                            .scale(1.0f + ring3Progress * 0.8f)
                            .alpha((1.0f - ring3Progress).coerceIn(0f, 1f))
                            .fillMaxSize()
                            .border(1.5.dp, accentPrimary, CircleShape)
                    )

                    // Center Orb with Glowing Gradient & Icon
                    Box(
                        modifier = Modifier
                            .scale(pulseScale)
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        accentPrimary.copy(alpha = 0.35f),
                                        Color.Black.copy(alpha = 0.5f)
                                    )
                                )
                            )
                            .border(2.dp, accentPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AlarmOn,
                            contentDescription = "Alarm Alerting",
                            tint = Color.White,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Time Display with elevated AM/PM badge
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = formattedTime,
                        color = inkWhite,
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-2).sp,
                        lineHeight = 72.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentPrimary.copy(alpha = 0.2f),
                        modifier = Modifier.padding(bottom = 14.dp)
                    ) {
                        Text(
                            text = formattedAmPm,
                            color = accentPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Date Label
                Text(
                    text = formattedDate,
                    color = inkMuted,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Subtitle Instruction Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color.White.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = accentSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isCheckIn)
                                "Time to check in! Open portal or snooze below."
                            else
                                "Time to check out! Open portal or snooze below.",
                            color = inkWhite.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Bottom Actions Column
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isCheckIn) {
                    // Primary Check-In Hero Button (Gradient Fill)
                    Button(
                        onClick = onCheckIn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(accentPrimary, accentSecondary)
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInBrowser,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "CHECK-IN (OPEN PORTAL)",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Secondary Action: Snooze
                    FilledTonalButton(
                        onClick = onSnooze,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White.copy(alpha = 0.1f),
                            contentColor = inkWhite
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color.White.copy(alpha = 0.15f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Snooze,
                                contentDescription = null,
                                tint = inkWhite
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "SNOOZE FOR 10 MINS",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Tertiary Action: Apply for Leave
                    OutlinedButton(
                        onClick = onLeave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFF87171)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color(0xFFF87171).copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MailOutline,
                                contentDescription = null,
                                tint = Color(0xFFF87171)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "APPLY FOR LEAVE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    // Check-Out Primary Hero Button
                    Button(
                        onClick = onCheckOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(accentPrimary, accentSecondary)
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "CHECK-OUT (OPEN PORTAL)",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Secondary Action: Snooze
                    FilledTonalButton(
                        onClick = onSnooze,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White.copy(alpha = 0.1f),
                            contentColor = inkWhite
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color.White.copy(alpha = 0.15f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Snooze,
                                contentDescription = null,
                                tint = inkWhite
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "SNOOZE FOR 10 MINS",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

