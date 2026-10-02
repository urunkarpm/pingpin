package com.urunkarpm.pingpin

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.Density
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.urunkarpm.pingpin.service.AlarmSoundService
import com.urunkarpm.pingpin.service.NotificationService
import com.urunkarpm.pingpin.ui.components.GlassCard
import com.urunkarpm.pingpin.ui.theme.PingPinTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

import com.urunkarpm.pingpin.ui.theme.optimizeDisplayRefreshRate

class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        optimizeDisplayRefreshRate()
        turnScreenOnAndKeyguard()

        val alarmId = intent.getIntExtra("alarmId", 101)
        val intentActionType = intent.getStringExtra("actionType")
        val intentTitle = intent.getStringExtra("title") ?: "ATTENDANCE ALARM"
        val intentPortalUrl = intent.getStringExtra("portalUrl") ?: ""

        val initialIsCheckIn = when {
            !intentActionType.isNullOrBlank() -> intentActionType == com.urunkarpm.pingpin.ui.portal.PortalActivity.ACTION_CHECK_IN
            alarmId == NotificationService.CHECK_OUT_ALARM_ID || alarmId == NotificationService.CHECK_OUT_SNOOZE_ID || intentTitle.contains("CHECK-OUT", ignoreCase = true) -> false
            else -> true
        }

        val resolvedActionType = if (initialIsCheckIn) com.urunkarpm.pingpin.ui.portal.PortalActivity.ACTION_CHECK_IN else com.urunkarpm.pingpin.ui.portal.PortalActivity.ACTION_CHECK_OUT

        // Ensure alarm sound service is actively playing
        AlarmSoundService.startAlarmSound(this, alarmId, intentTitle, intentPortalUrl, resolvedActionType)

        // Handle back button press safely
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        val prefs = NotificationService.getAlarmPreferences(this)
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

            val isDark = com.urunkarpm.pingpin.ui.theme.ThemePreference.isDarkMode(this)
            PingPinTheme(darkTheme = isDark) {
                AlarmScreenContent(
                    isCheckInMode = initialIsCheckIn,
                    onCheckIn = {
                        stopAlarmSound()
                        val notifService = NotificationService(this)
                        notifService.dismissNotification(alarmId)
                        if (alarmId == NotificationService.CHECK_IN_SNOOZE_ID || alarmId == NotificationService.CHECK_OUT_SNOOZE_ID) {
                            notifService.cancelAlarm(alarmId)
                        }
                        dismissKeyguardAndExecute(shouldFinish = false) { openPortalAction(com.urunkarpm.pingpin.ui.portal.PortalActivity.ACTION_CHECK_IN, alarmId, portalUrlState) }
                    },
                    onSnooze = { durationMins ->
                        stopAlarmSound()
                        val notifService = NotificationService(this)
                        notifService.snoozeAlarm(alarmId, durationMins, portalUrlState)
                        finish()
                    },
                    onLeave = {
                        stopAlarmSound()
                        val notifService = NotificationService(this)
                        notifService.dismissNotification(alarmId)
                        if (alarmId == NotificationService.CHECK_IN_SNOOZE_ID || alarmId == NotificationService.CHECK_OUT_SNOOZE_ID) {
                            notifService.cancelAlarm(alarmId)
                        }
                        if (initialIsCheckIn) {
                            notifService.skipTodayCheckOutAlarm()
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
                    },
                    onCancel = {
                        stopAlarmSound()
                        val notifService = NotificationService(this)
                        notifService.dismissNotification(alarmId)
                        if (alarmId == NotificationService.CHECK_IN_SNOOZE_ID || alarmId == NotificationService.CHECK_OUT_SNOOZE_ID) {
                            notifService.cancelAlarm(alarmId)
                        }
                        finish()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        turnScreenOnAndKeyguard()
        val alarmId = intent.getIntExtra("alarmId", 101)
        val intentActionType = intent.getStringExtra("actionType")
        val intentTitle = intent.getStringExtra("title") ?: "ATTENDANCE ALARM"
        val intentPortalUrl = intent.getStringExtra("portalUrl") ?: ""
        val resolvedActionType = if (intentActionType.isNullOrBlank()) {
            if (alarmId == NotificationService.CHECK_OUT_ALARM_ID || alarmId == NotificationService.CHECK_OUT_SNOOZE_ID) "CHECK_OUT" else "CHECK_IN"
        } else intentActionType
        AlarmSoundService.startAlarmSound(this, alarmId, intentTitle, intentPortalUrl, resolvedActionType)
    }

    private fun turnScreenOnAndKeyguard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )
    }

    private fun stopAlarmSound() {
        AlarmSoundService.stopAlarmSound(this)
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
            // ponytail: IN_APP_AUTO always routes to full PortalActivity without overlay window overhead
            withContext(Dispatchers.Main) {
                if (portalMode == "IN_APP_AUTO") {
                    val intent = com.urunkarpm.pingpin.ui.portal.PortalActivity.createIntent(
                        context = this@AlarmActivity,
                        actionType = actionType,
                        portalUrl = url,
                        alarmId = currentAlarmId
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    startActivity(intent)
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
    onCheckIn: () -> Unit,
    onSnooze: (durationMins: Int) -> Unit,
    onLeave: () -> Unit,
    onCheckOut: () -> Unit,
    onCancel: () -> Unit = {}
) {
    val isCheckIn = isCheckInMode

    var selectedSnoozeMins by remember { mutableIntStateOf(10) }
    var activeRingingSeconds by remember { mutableIntStateOf(0) }
    var currentTime by remember { mutableStateOf(Calendar.getInstance()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Calendar.getInstance()
            activeRingingSeconds++
            delay(1000L)
        }
    }

    val timeFormat = remember { SimpleDateFormat("hh:mm", Locale.US) }
    val secFormat = remember { SimpleDateFormat(":ss", Locale.US) }
    val amPmFormat = remember { SimpleDateFormat("a", Locale.US) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.US) }

    val formattedTime = timeFormat.format(currentTime.time)
    val formattedSeconds = secFormat.format(currentTime.time)
    val formattedAmPm = amPmFormat.format(currentTime.time)
    val formattedDate = dateFormat.format(currentTime.time)

    val durationText = remember(activeRingingSeconds) {
        val mins = activeRingingSeconds / 60
        val secs = activeRingingSeconds % 60
        String.format(Locale.US, "RINGING • %02d:%02d", mins, secs)
    }

    // Weekend Countdown & Humorous Messages Logic
    val (weekendHeadline, weekendSubtext, weekendIcon) = remember(currentTime) {
        val dayOfWeek = currentTime.get(Calendar.DAY_OF_WEEK)
        when (dayOfWeek) {
            Calendar.MONDAY -> Triple(
                "4 Days to Friday! ☕",
                "Monday initialized. May your coffee be extra strong and your code bugs be non-existent!",
                Icons.Default.Schedule
            )
            Calendar.TUESDAY -> Triple(
                "3 Days to Friday! ⏳",
                "Tuesday: You survived Monday! Only 72 hours until sweet weekend freedom.",
                Icons.Default.Schedule
            )
            Calendar.WEDNESDAY -> Triple(
                "Hump Day! 🐫 2 Days to Friday!",
                "Halfway to the weekend! Hold on tight and don't push straight to prod today!",
                Icons.Default.Face
            )
            Calendar.THURSDAY -> Triple(
                "1 Day to Friday! 🚀",
                "Tomorrow is the promised land. Stay calm, keep calm and pretend to be busy!",
                Icons.Default.Lightbulb
            )
            Calendar.FRIDAY -> Triple(
                "IT'S FRIDAY BABY! 🎉",
                "Holiday tomorrow! Put on your party shoes and fake your enthusiasm for just 8 more hours! 💃🕺",
                Icons.Default.Star
            )
            Calendar.SATURDAY -> Triple(
                "IT'S SATURDAY! 🥳",
                "Why on earth is this alarm ringing?! Go back to sleep, you absolute legend! 🛌✨",
                Icons.Default.Face
            )
            Calendar.SUNDAY -> Triple(
                "Sunday Chill Mode 🧘‍♂️",
                "Weekend is wrapping up... 5 days until Friday. Don't panic, enjoy your Sunday!",
                Icons.Default.Schedule
            )
            else -> Triple("Weekend Loading...", "Hang in there, champion!", Icons.Default.Timer)
        }
    }

    val currentDensity = LocalDensity.current
    val clampedDensity = Density(
        density = currentDensity.density,
        fontScale = currentDensity.fontScale.coerceAtMost(1.15f)
    )
    val haptic = LocalHapticFeedback.current

    val primaryGradient = if (isCheckIn) {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF00C853),
                Color(0xFF00E676),
                Color(0xFF0288D1)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFFF3D00),
                Color(0xFFFF6E40),
                Color(0xFFFF9100)
            )
        )
    }

    CompositionLocalProvider(LocalDensity provides clampedDensity) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top section: Status Pill (URL removed completely per request)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (isCheckIn) Color(0xFF00E676) else Color(0xFFFF5252), CircleShape)
                        )
                        Text(
                            text = if (isCheckIn) "CHECK-IN ALARM" else "CHECK-OUT ALARM",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(12.dp)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                        )
                        Text(
                            text = durationText,
                            color = if (isCheckIn) Color(0xFF00E676) else Color(0xFFFF5252),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Center section: Time Display with Ticking Red Seconds
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Main Time (hh:mm)
                            Text(
                                text = formattedTime,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 56.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-2).sp
                            )
                            // Ticking Seconds in Vibrant Bright Red!
                            Text(
                                text = formattedSeconds,
                                color = Color(0xFFFF2D55),
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // AM / PM
                            Text(
                                text = formattedAmPm,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formattedDate,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Bottom section: Actions Container
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // HIGHLY EMPHASIZED HERO BUTTON (Check-in / Check-out)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(68.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(primaryGradient)
                                .border(
                                    2.dp,
                                    Brush.linearGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.6f),
                                            Color.White.copy(alpha = 0.1f)
                                        )
                                    ),
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (isCheckIn) onCheckIn() else onCheckOut()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.25f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isCheckIn) Icons.Default.OpenInBrowser else Icons.AutoMirrored.Filled.ExitToApp,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = if (isCheckIn) "CHECK-IN NOW" else "CHECK-OUT NOW",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 17.sp,
                                        color = Color.White,
                                        letterSpacing = 1.2.sp
                                    )
                                    Text(
                                        text = if (isCheckIn) "Tap to confirm office entry" else "Tap to record departure",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 1.dp
                        )

                        // Snooze Duration Options Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val options = listOf(5, 10, 15, 30)
                            options.forEach { mins ->
                                val isSelected = selectedSnoozeMins == mins
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 3.dp)
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedSnoozeMins = mins
                                        }
                                ) {
                                    Text(
                                        text = "${mins}m",
                                        color = if (isSelected) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 10.dp)
                                    )
                                }
                            }
                        }

                        // Snooze Action Button
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSnooze(selectedSnoozeMins)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Snooze,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SNOOZE FOR ${selectedSnoozeMins} MINS",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                letterSpacing = 0.8.sp
                            )
                        }

                        // Leave Application Option for Check-In Mode
                        if (isCheckIn) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                thickness = 1.dp
                            )

                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onLeave()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MailOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "APPLY FOR LEAVE TODAY",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                            }
                        }

                        // Cancel Alarm Option for both Check-In and Check-Out Modes
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 1.dp
                        )

                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onCancel()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "CANCEL ALARM",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }
                    }
                }

                // Humorous Weekend Countdown Card at Bottom
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = weekendIcon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = weekendHeadline,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = weekendSubtext,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
