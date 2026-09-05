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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.urunkarpm.pingpin.service.AlarmSoundService
import com.urunkarpm.pingpin.service.NotificationService
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

/**
 * Flagship Sophisticated Luxury Canvas
 * Features Iridescent Liquid Silk Aurora Mesh, Faceted Obsidian Glass Crystals with Rim Lights,
 * Ambient Refractive Light Shimmer, and Floating Quantum Bokeh Stardust.
 */
@Composable
fun CinematicEldritchMirrorCanvas(
    accentColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "luxury_cinematic")

    val lightShimmerPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "light_shimmer"
    )

    val meshPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mesh_phase"
    )

    val stardustPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "stardust_phase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height * 0.38f)
        val maxRadius = max(size.width, size.height) * 0.85f

        // 1. Pitch Black OLED Base
        drawRect(color = Color(0xFF020408))

        // 2. Liquid Aurora Silk Mesh (Morphing iridescent background orbs)
        val orb1X = center.x + sin(meshPhase) * (size.width * 0.28f)
        val orb1Y = center.y + cos(meshPhase * 0.7f) * (size.height * 0.18f)

        val orb2X = center.x + cos(meshPhase * 1.1f) * (size.width * 0.32f)
        val orb2Y = center.y + sin(meshPhase * 0.85f) * (size.height * 0.22f)

        // Morphing Sapphire/Cyan Aurora Mesh Blob 1
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.42f),
                    secondaryColor.copy(alpha = 0.18f),
                    Color.Transparent
                ),
                center = Offset(orb1X, orb1Y),
                radius = maxRadius * 0.72f
            ),
            center = Offset(orb1X, orb1Y),
            radius = maxRadius * 0.72f
        )

        // Morphing Deep Indigo/Teal Aurora Mesh Blob 2
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    secondaryColor.copy(alpha = 0.32f),
                    Color(0xFF6366F1).copy(alpha = 0.14f),
                    Color.Transparent
                ),
                center = Offset(orb2X, orb2Y),
                radius = maxRadius * 0.68f
            ),
            center = Offset(orb2X, orb2Y),
            radius = maxRadius * 0.68f
        )

        // 3. Ambient Fluid Concentric Wavefront Aura (Soft expanding soundwave ripples)
        val numAuraRings = 4
        for (r in 0 until numAuraRings) {
            val ringProgress = ((stardustPhase + r.toFloat() / numAuraRings) % 1.0f)
            val ringR = maxRadius * (0.18f + ringProgress * 0.48f)
            val ringAlpha = (1.0f - ringProgress).coerceIn(0f, 1f) * 0.22f

            drawCircle(
                color = accentColor.copy(alpha = ringAlpha),
                radius = ringR,
                center = center,
                style = Stroke(width = (1.5f + (1f - ringProgress) * 2f).dp.toPx())
            )
        }

        // 4. Faceted 3D Glass Crystals (Obsidian Shards with Refractive Rim Lights)
        val numShards = 12
        val shardRadius = maxRadius * 0.55f

        for (i in 0 until numShards) {
            val baseAngle = (i * 360f / numShards) * (PI.toFloat() / 180f)
            val nextAngle = ((i + 1) * 360f / numShards) * (PI.toFloat() / 180f)

            // Dynamic soft shimmer light modulation
            val shimmerFactor = (sin(lightShimmerPhase + i * 0.6f) + 1f) / 2f

            val innerR = shardRadius * (0.32f + sin(meshPhase + i) * 0.04f)
            val outerR = shardRadius * (if (i % 2 == 0) 1.05f else 0.85f)

            val p1 = Offset(center.x + cos(baseAngle) * innerR, center.y + sin(baseAngle) * innerR)
            val p2 = Offset(center.x + cos(baseAngle) * outerR, center.y + sin(baseAngle) * outerR)
            val p3 = Offset(center.x + cos(nextAngle) * (outerR * 0.88f), center.y + sin(nextAngle) * (outerR * 0.88f))

            val shardPath = Path().apply {
                moveTo(p1.x, p1.y)
                lineTo(p2.x, p2.y)
                lineTo(p3.x, p3.y)
                close()
            }

            // Glass Refraction Fill
            val fillAlpha = 0.05f + shimmerFactor * 0.12f
            drawPath(
                path = shardPath,
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = fillAlpha + 0.06f),
                        accentColor.copy(alpha = fillAlpha),
                        Color.Transparent
                    ),
                    center = Offset((p1.x + p2.x + p3.x) / 3f, (p1.y + p2.y + p3.y) / 3f)
                )
            )

            // Razor Specular Rim Light Edge
            val rimAlpha = 0.20f + shimmerFactor * 0.40f
            drawPath(
                path = shardPath,
                color = Color.White.copy(alpha = rimAlpha),
                style = Stroke(width = (1.0f + shimmerFactor * 1.2f).dp.toPx())
            )
        }

        // 5. Quantum Bokeh & Floating Stardust Field
        val numParticles = 40
        for (p in 0 until numParticles) {
            val seed = p * 137.5f
            val pAngle = (seed + stardustPhase * 360f * (if (p % 2 == 0) 1f else -1f)) * (PI.toFloat() / 180f)
            val distProgress = ((stardustPhase + p.toFloat() / numParticles) % 1.0f)
            val particleRadius = distProgress * maxRadius * 0.8f

            val px = center.x + cos(pAngle) * particleRadius
            val py = center.y + sin(pAngle) * particleRadius
            val pAlpha = sin(distProgress * PI.toFloat()).coerceIn(0f, 1f) * 0.8f

            val isBokeh = p % 5 == 0
            val pSize = if (isBokeh) (8f + distProgress * 12f).dp.toPx() else (1.5f + distProgress * 3.5f).dp.toPx()

            val sparkColor = when (p % 4) {
                0 -> Color.White
                1 -> accentColor
                2 -> Color(0xFFFFD700)
                else -> secondaryColor
            }

            drawCircle(
                color = sparkColor.copy(alpha = if (isBokeh) pAlpha * 0.20f else pAlpha),
                radius = pSize,
                center = Offset(px, py)
            )
        }

        // 6. Foreground Cinematic Radial Vignette
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.4f),
                    Color.Black.copy(alpha = 0.94f)
                ),
                center = center,
                radius = maxRadius * 0.9f
            )
        )
    }
}

@Composable
fun AlarmScreenContent(
    isCheckInMode: Boolean = true,
    portalUrl: String,
    onCheckIn: () -> Unit,
    onSnooze: (durationMins: Int) -> Unit,
    onLeave: () -> Unit,
    onCheckOut: () -> Unit
) {
    val isCheckIn = isCheckInMode

    // Dynamic accent theme based on Check-in (Electric Sapphire/Cyan) vs Check-out (Emerald/Mint Jade)
    val accentPrimary by animateColorAsState(
        targetValue = if (isCheckIn) Color(0xFF3B82F6) else Color(0xFF10B981),
        label = "accent_primary"
    )
    val accentSecondary by animateColorAsState(
        targetValue = if (isCheckIn) Color(0xFF06B6D4) else Color(0xFF34D399),
        label = "accent_secondary"
    )

    val inkWhite = Color(0xFFF8FAFC)
    val inkMuted = Color(0xFF94A3B8)

    // Snooze Duration Selector State
    var selectedSnoozeMins by remember { mutableIntStateOf(10) }

    // Alarm active elapsed seconds counter
    var activeRingingSeconds by remember { mutableIntStateOf(0) }

    // Live clock ticker
    var currentTime by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Calendar.getInstance()
            activeRingingSeconds++
            delay(1000L)
        }
    }

    val timeFormat = remember { SimpleDateFormat("hh:mm", Locale.US) }
    val amPmFormat = remember { SimpleDateFormat("a", Locale.US) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.US) }

    val formattedTime = timeFormat.format(currentTime.time)
    val formattedAmPm = amPmFormat.format(currentTime.time)
    val formattedDate = dateFormat.format(currentTime.time)

    // Format active duration string e.g. "RINGING • 00:42"
    val durationText = remember(activeRingingSeconds) {
        val mins = activeRingingSeconds / 60
        val secs = activeRingingSeconds % 60
        String.format(Locale.US, "RINGING • %02d:%02d", mins, secs)
    }

    // Infinite animations for breathing pulse
    val infiniteTransition = rememberInfiniteTransition(label = "alarm_effects")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
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

    Box(modifier = Modifier.fillMaxSize()) {

        // --- BACKGROUND: Flagship Luxury Cinematic Canvas ---
        CinematicEldritchMirrorCanvas(
            accentColor = accentPrimary,
            secondaryColor = accentSecondary
        )

        // --- FOREGROUND: Interactive UI Layers ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Top Section: Status pill & Portal domain badge
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Live Alarm Status Chip with Blinking LED & Ringing Elapsed Counter
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        accentPrimary.copy(alpha = 0.7f)
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
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(12.dp)
                                .background(Color.White.copy(alpha = 0.2f))
                        )
                        Text(
                            text = durationText,
                            color = accentSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Portal Link pill if available
                if (portalUrl.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.45f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color.White.copy(alpha = 0.12f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = accentSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = portalUrl,
                                color = inkMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Center Hero Display: Hero Digital Time Display inside Kinetic Glass Ring
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Kinetic Fluid Glass Centerpiece containing the Hero Digital Time!
                Box(
                    modifier = Modifier.size(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer Fluid Halo Ring
                    Box(
                        modifier = Modifier
                            .scale(pulseScale * 1.12f)
                            .size(220.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        accentPrimary.copy(alpha = 0.28f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Kinetic Liquid Glass Orb enclosing the Digital Clock & AM/PM Badge
                    Box(
                        modifier = Modifier
                            .scale(pulseScale)
                            .size(190.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        accentPrimary.copy(alpha = 0.45f),
                                        accentSecondary.copy(alpha = 0.25f),
                                        Color.Black.copy(alpha = 0.88f)
                                    )
                                )
                            )
                            .border(
                                2.dp,
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.85f),
                                        accentSecondary,
                                        accentPrimary.copy(alpha = 0.4f)
                                    )
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Digital Time Display
                            Text(
                                text = formattedTime,
                                color = inkWhite,
                                fontSize = 54.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-2).sp,
                                lineHeight = 54.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // AM / PM Pill Badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = accentPrimary.copy(alpha = 0.35f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    Color.White.copy(alpha = 0.4f)
                                )
                            ) {
                                Text(
                                    text = formattedAmPm,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Formatted Date Label
                Text(
                    text = formattedDate,
                    color = inkMuted,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Bottom Section: Liquid Gesture Slide Controls & Integrated Snooze Hub
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. HERO LIQUID SLIDE TO CONFIRM (Primary Action: Check-In / Check-Out)
                LiquidSlideControl(
                    label = if (isCheckIn) "SLIDE TO CHECK-IN" else "SLIDE TO CHECK-OUT",
                    accentPrimary = accentPrimary,
                    accentSecondary = accentSecondary,
                    icon = if (isCheckIn) Icons.Default.OpenInBrowser else Icons.AutoMirrored.Filled.ExitToApp,
                    onConfirm = { if (isCheckIn) onCheckIn() else onCheckOut() }
                )

                // 2. UNIFIED SNOOZE ACTION HUB (Segmented Pills + Slide-to-Snooze)
                SnoozeHubControl(
                    selectedMins = selectedSnoozeMins,
                    onMinsSelected = { selectedSnoozeMins = it },
                    onSnoozeConfirmed = { onSnooze(selectedSnoozeMins) }
                )

                // 3. TERTIARY ACTION: APPLY FOR LEAVE (Check-In Mode Only)
                if (isCheckIn) {
                    OutlinedButton(
                        onClick = onLeave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF87171)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF87171).copy(alpha = 0.35f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MailOutline,
                                contentDescription = null,
                                tint = Color(0xFFF87171),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "APPLY FOR LEAVE TODAY",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Unified Glass Snooze Hub Control (Segmented Duration Pills + Slide to Snooze Bar)
 */
@Composable
fun SnoozeHubControl(
    selectedMins: Int,
    onMinsSelected: (Int) -> Unit,
    onSnoozeConfirmed: () -> Unit
) {
    val amberGold = Color(0xFFFBBF24)
    val amberDeep = Color(0xFFF59E0B)

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = Color.Black.copy(alpha = 0.55f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            amberGold.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Snooze Title + Segmented Glass Tab Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Snooze,
                        contentDescription = null,
                        tint = amberGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "SNOOZE DURATION",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }

                // Segmented Pill Tabs Bar
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color.White.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val options = listOf(5, 10, 15, 30)
                        options.forEach { mins ->
                            val isSelected = selectedMins == mins
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) amberGold.copy(alpha = 0.3f) else Color.Transparent
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) amberGold else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onMinsSelected(mins) }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${mins}m",
                                    color = if (isSelected) amberGold else Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Integrated Slide to Snooze Bar
            LiquidSlideControl(
                label = "SLIDE TO SNOOZE (${selectedMins}m)",
                accentPrimary = amberDeep,
                accentSecondary = amberGold,
                icon = Icons.Default.Snooze,
                onConfirm = onSnoozeConfirmed
            )
        }
    }
}

/**
 * Fluid Liquid Slide-to-Confirm Hero Gesture Control Bar
 */
@Composable
fun LiquidSlideControl(
    label: String,
    accentPrimary: Color,
    accentSecondary: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onConfirm: () -> Unit
) {
    val density = LocalDensity.current
    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    val handleSizeDp = 54.dp
    val handleSizePx = with(density) { handleSizeDp.toPx() }

    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val maxDragPx = (containerWidthPx - handleSizePx).coerceAtLeast(1f)
    val dragProgress = (dragOffsetPx / maxDragPx).coerceIn(0f, 1f)

    val animatedDragOffset by animateFloatAsState(
        targetValue = dragOffsetPx,
        animationSpec = if (isDragging) spring(stiffness = Spring.StiffnessHigh) else spring(stiffness = Spring.StiffnessMediumLow),
        label = "handle_offset"
    )

    // Animated shimmering arrow hint
    val infiniteTransition = rememberInfiniteTransition(label = "arrow_pulse")
    val arrowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrow_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(29.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .border(
                1.5.dp,
                Brush.horizontalGradient(
                    colors = listOf(
                        accentPrimary.copy(alpha = 0.7f),
                        accentSecondary.copy(alpha = 0.4f)
                    )
                ),
                RoundedCornerShape(29.dp)
            )
            .onGloballyPositioned { layoutCoordinates ->
                containerWidthPx = layoutCoordinates.size.width.toFloat()
            }
            .pointerInput(containerWidthPx) {
                if (containerWidthPx <= 0f) return@pointerInput
                val currentMaxDragPx = (containerWidthPx - handleSizePx).coerceAtLeast(1f)

                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                    },
                    onDragEnd = {
                        isDragging = false
                        if (dragOffsetPx >= currentMaxDragPx * 0.70f) {
                            onConfirm()
                        }
                        dragOffsetPx = 0f
                    },
                    onDragCancel = {
                        isDragging = false
                        dragOffsetPx = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffsetPx = (dragOffsetPx + dragAmount.x).coerceIn(0f, currentMaxDragPx)
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // 1. Dynamic Liquid Gradient Fill Track
        val fillWidthDp = with(density) { (animatedDragOffset + handleSizePx / 2f).toDp() }
        Box(
            modifier = Modifier
                .width(fillWidthDp)
                .fillMaxHeight()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(accentPrimary.copy(alpha = 0.85f), accentSecondary)
                    ),
                    shape = RoundedCornerShape(29.dp)
                )
        )

        // 2. Morphing Track Text
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            val textAlpha = (1.0f - dragProgress * 1.5f).coerceIn(0f, 1f)
            val isNearEnd = dragProgress >= 0.75f

            Text(
                text = if (isNearEnd) "RELEASE TO CONFIRM  ✓" else "$label  ►►",
                color = if (isNearEnd) Color.White else Color.White.copy(alpha = textAlpha * arrowAlpha),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )
        }

        // 3. Sliding Handle Orb
        Box(
            modifier = Modifier
                .offset { IntOffset(animatedDragOffset.roundToInt(), 0) }
                .padding(3.dp)
                .size(handleSizeDp - 6.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White,
                            accentPrimary
                        )
                    )
                )
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (dragProgress >= 0.75f) Icons.Default.Check else icon,
                contentDescription = "Slide Handle",
                tint = Color(0xFF0F172A),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
