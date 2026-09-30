package com.urunkarpm.pingpin.ui.onboarding

import android.content.Intent
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.urunkarpm.pingpin.R
import com.urunkarpm.pingpin.data.local.AppDatabase
import com.urunkarpm.pingpin.data.local.OnboardingPreferences
import com.urunkarpm.pingpin.data.local.entity.OfficeConfigEntity
import com.urunkarpm.pingpin.data.local.entity.UserProfileEntity
import com.urunkarpm.pingpin.data.repository.OfficeConfigRepository
import com.urunkarpm.pingpin.data.repository.UserProfileRepository
import com.urunkarpm.pingpin.service.NotificationService
import com.urunkarpm.pingpin.service.WifiService
import com.urunkarpm.pingpin.service.WorkingDays
import com.urunkarpm.pingpin.ui.components.GlassCard
import com.urunkarpm.pingpin.ui.components.PingPinSwitch
import com.urunkarpm.pingpin.ui.components.TimePickerField
import com.urunkarpm.pingpin.ui.components.WfoDaysSelector
import com.urunkarpm.pingpin.ui.components.WifiSsidPickerField
import com.urunkarpm.pingpin.ui.components.WorkingDaysSelector
import com.urunkarpm.pingpin.ui.portal.PortalActivity
import com.urunkarpm.pingpin.ui.theme.ElectricBlue
import com.urunkarpm.pingpin.ui.theme.EmeraldGreen
import com.urunkarpm.pingpin.ui.theme.WfoDayPurple
import com.urunkarpm.pingpin.ui.theme.rememberIsReduceMotionEnabled
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class StepMetadata(
    val number: Int,
    val title: String,
    val subtitle: String,
    val category: String
)

private val STEP_LIST = listOf(
    StepMetadata(1, "Employee Profile", "Personalize your attendance dashboard & monthly PDF reports", "Identity"),
    StepMetadata(2, "Office Wi-Fi Network", "Automatic local attendance detection when connected", "Detection"),
    StepMetadata(3, "Shift Timings & Alarms", "Schedule check-in reminders & calculate working shift hours", "Timings"),
    StepMetadata(4, "Work & WFO Days Schedule", "Configure weekly working days & mandatory office targets", "Schedule"),
    StepMetadata(5, "Check-In Path & HR Portal", "Set up web portal URL and execution mode", "Portal"),
    StepMetadata(6, "Review & Launch", "Confirm configuration and launch PingPin 100% locally", "Completion")
)

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val db = remember { AppDatabase.getInstance(context) }
    val officeConfigRepo = remember { OfficeConfigRepository(db.officeConfigDao()) }
    val profileRepo = remember { UserProfileRepository(db.userProfileDao()) }
    val wifiService = remember { WifiService(context) }

    // Streamlined 6-step wizard state
    var currentStep by remember { mutableIntStateOf(1) }
    val totalSteps = STEP_LIST.size

    // Custom Glass Banner Alert State (replaces Toast)
    var bannerMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(bannerMessage) {
        if (bannerMessage != null) {
            delay(3000L)
            bannerMessage = null
        }
    }

    BackHandler(enabled = currentStep > 1) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        currentStep--
    }

    // Form state
    var fullName by remember { mutableStateOf("") }
    var ssid by remember { mutableStateOf("") }
    var checkInTime by remember { mutableStateOf("09:30") }
    var checkOutTime by remember { mutableStateOf("18:10") }
    val lateCutoffTime = remember(checkInTime) {
        val parts = checkInTime.split(":")
        if (parts.size == 2) {
            val hour = (parts[0].toIntOrNull() ?: 9) + 1
            val min = parts[1]
            String.format("%02d:%s", hour % 24, min)
        } else "10:30"
    }

    var workingDaysMask by remember { mutableIntStateOf(WorkingDays.DEFAULT_WEEKDAYS) }
    var wfoDaysMask by remember { mutableIntStateOf(WorkingDays.DEFAULT_WEEKDAYS) }

    var portalMode by remember { mutableStateOf("IN_APP_AUTO") } // "IN_APP_AUTO" vs "EXTERNAL_BROWSER"
    var portalUrl by remember { mutableStateOf("") }
    var autoCheckInEnabled by remember { mutableStateOf(true) }
    var customCheckInKeywords by remember { mutableStateOf("") }
    var customCheckOutKeywords by remember { mutableStateOf("") }

    var liveConnectedSsid by remember { mutableStateOf<String?>(null) }

    val shiftDurationText = remember(checkInTime, checkOutTime) {
        com.urunkarpm.pingpin.ui.components.TimeFormatUtils.calculateShiftDuration(checkInTime, checkOutTime)
    }

    val fieldShape = RoundedCornerShape(16.dp)

    LaunchedEffect(Unit) {
        val currentSsid = wifiService.getWifiSSID()
        if (!currentSsid.isNullOrEmpty()) {
            liveConnectedSsid = currentSsid
            if (ssid.isEmpty()) {
                ssid = currentSsid
            }
        }
    }

    val isReduceMotion = rememberIsReduceMotionEnabled()
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    // Progress percentage animation
    val progressAnimated by animateFloatAsState(
        targetValue = (currentStep - 1).toFloat() / (totalSteps - 1).coerceAtLeast(1).toFloat(),
        animationSpec = if (isReduceMotion) tween(0) else spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow),
        label = "setup_progress"
    )

    val stepMeta = STEP_LIST[currentStep - 1]

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(Color(0xFF070A11), Color(0xFF0E1726), Color(0xFF0B132B))
                    } else {
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f)
                        )
                    }
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Floating In-App Glass Banner Alert
        AnimatedVisibility(
            visible = bannerMessage != null,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp)
                .padding(bottom = 80.dp)
                .zIndex(10f)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) Color(0xFF1E293B) else MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f)),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = ElectricBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = bannerMessage ?: "",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Top ambient glowing aura
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-50).dp)
                .size(340.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ElectricBlue.copy(alpha = if (isDark) 0.25f else 0.12f),
                            WfoDayPurple.copy(alpha = if (isDark) 0.12f else 0.05f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ==================== TOP FLOATING GLASS STEP HEADER ====================
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.65f else 0.88f),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Header Row: Step Title & Percentage Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = ElectricBlue.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = stepMeta.category.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ElectricBlue,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    letterSpacing = 0.6.sp
                                )
                            }
                            Text(
                                text = "Step ${stepMeta.number} of $totalSteps",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EmeraldGreen.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "${(progressAnimated * 100).toInt()}% COMPLETE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldGreen,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Animated Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressAnimated)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(ElectricBlue, WfoDayPurple, EmeraldGreen)
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tappable Step Indicator Dots Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        (1..totalSteps).forEach { stepNumber ->
                            val isCompleted = stepNumber < currentStep
                            val isCurrent = stepNumber == currentStep

                            Box(
                                modifier = Modifier
                                    .size(if (isCurrent) 28.dp else 22.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isCurrent -> ElectricBlue
                                            isCompleted -> EmeraldGreen
                                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        when {
                                            isCurrent -> ElectricBlue.copy(alpha = 0.6f)
                                            isCompleted -> EmeraldGreen.copy(alpha = 0.6f)
                                            else -> Color.Transparent
                                        },
                                        CircleShape
                                    )
                                    .clickable(enabled = stepNumber <= currentStep) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        currentStep = stepNumber
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCompleted) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                } else {
                                    Text(
                                        text = "$stepNumber",
                                        fontSize = if (isCurrent) 12.sp else 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==================== INTERACTIVE STEP WIZARD CONTENT ====================
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (isReduceMotion) {
                            fadeIn(animationSpec = tween(0)) togetherWith fadeOut(animationSpec = tween(0))
                        } else if (targetState > initialState) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut()
                            )
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut()
                            )
                        }
                    },
                    label = "step_transition"
                ) { step ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(4.dp))

                        when (step) {
                            1 -> Step1ProfileSection(
                                fullName = fullName,
                                onFullNameChange = { fullName = it },
                                isDark = isDark,
                                fieldShape = fieldShape
                            )

                            2 -> Step2WifiSection(
                                ssid = ssid,
                                onSsidChange = { ssid = it },
                                liveConnectedSsid = liveConnectedSsid,
                                onUseConnectedSsid = {
                                    liveConnectedSsid?.let {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        ssid = it
                                    }
                                }
                            )

                            3 -> Step3ShiftTimingsSection(
                                checkInTime = checkInTime,
                                onCheckInTimeChange = { checkInTime = it },
                                checkOutTime = checkOutTime,
                                onCheckOutTimeChange = { checkOutTime = it },
                                shiftDurationText = shiftDurationText
                            )

                            4 -> Step4ScheduleSection(
                                workingDaysMask = workingDaysMask,
                                onWorkingDaysMaskChange = { workingDaysMask = it },
                                wfoDaysMask = wfoDaysMask,
                                onWfoDaysMaskChange = { wfoDaysMask = it }
                            )

                            5 -> Step5PortalSection(
                                portalMode = portalMode,
                                onPortalModeChange = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    portalMode = it
                                },
                                portalUrl = portalUrl,
                                onPortalUrlChange = { portalUrl = it },
                                autoCheckInEnabled = autoCheckInEnabled,
                                onAutoCheckInEnabledChange = { autoCheckInEnabled = it },
                                customCheckInKeywords = customCheckInKeywords,
                                onCustomCheckInKeywordsChange = { customCheckInKeywords = it },
                                customCheckOutKeywords = customCheckOutKeywords,
                                onCustomCheckOutKeywordsChange = { customCheckOutKeywords = it },
                                fieldShape = fieldShape
                            )

                            6 -> Step6ReviewAndLaunchSection(
                                fullName = fullName,
                                ssid = ssid,
                                checkInTime = checkInTime,
                                checkOutTime = checkOutTime,
                                shiftDurationText = shiftDurationText,
                                workingDaysMask = workingDaysMask,
                                wfoDaysMask = wfoDaysMask,
                                portalMode = portalMode,
                                portalUrl = portalUrl,
                                autoCheckInEnabled = autoCheckInEnabled
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            // ==================== BOTTOM STEP NAVIGATION BAR ====================
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.75f else 0.92f),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back Button
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                currentStep--
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Back", fontSize = 13.sp, color = ElectricBlue, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    // Next / Complete Launch Button
                    if (currentStep < totalSteps) {
                        val nextStepMeta = STEP_LIST[currentStep]
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                when (currentStep) {
                                    1 -> {
                                        if (fullName.trim().isEmpty()) {
                                            bannerMessage = "Please enter your Full Name to continue"
                                            return@Button
                                        }
                                    }
                                    2 -> {
                                        if (ssid.trim().isEmpty()) {
                                            bannerMessage = "Please select or enter your Office Wi-Fi SSID"
                                            return@Button
                                        }
                                    }
                                    5 -> {
                                        // Launch HR portal in PingPin's embedded PortalActivity web viewer
                                        if (portalUrl.trim().isNotEmpty()) {
                                            val formattedUrl = if (portalUrl.trim().startsWith("http://") || portalUrl.trim().startsWith("https://")) {
                                                portalUrl.trim()
                                            } else {
                                                "https://${portalUrl.trim()}"
                                            }
                                            try {
                                                val intent = PortalActivity.createIntent(
                                                    context = context,
                                                    actionType = PortalActivity.ACTION_CHECK_IN,
                                                    portalUrl = formattedUrl
                                                )
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                bannerMessage = "Could not open HR portal viewer"
                                            }
                                        }
                                    }
                                }
                                currentStep++
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Next: ${nextStepMeta.category}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else {
                        // Complete Setup & Launch CTA
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (ssid.trim().isEmpty()) {
                                    bannerMessage = "Please select or enter your Office Wi-Fi SSID"
                                    currentStep = 2
                                    return@Button
                                }

                                scope.launch {
                                    val config = OfficeConfigEntity(
                                        ssid = ssid.trim(),
                                        checkInTime = checkInTime.trim(),
                                        checkOutTime = checkOutTime.trim(),
                                        lateCutoffTime = lateCutoffTime.trim(),
                                        portalUrl = portalUrl.trim(),
                                        workingDaysMask = workingDaysMask,
                                        wfoDaysMask = wfoDaysMask,
                                        portalMode = portalMode,
                                        autoLoginEnabled = false,
                                        autoCheckInEnabled = autoCheckInEnabled,
                                        portalPreset = "GENERIC",
                                        customCheckInKeywords = customCheckInKeywords.trim(),
                                        customCheckOutKeywords = customCheckOutKeywords.trim()
                                    )
                                    officeConfigRepo.saveConfig(config)

                                    val profile = UserProfileEntity(
                                        fullName = fullName.trim()
                                    )
                                    profileRepo.saveProfile(profile)

                                    // Schedule Alarms
                                    NotificationService(context).scheduleAlarmsFromConfig(config)

                                    OnboardingPreferences.setOnboardingComplete(context, true)

                                    onOnboardingComplete()
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("COMPLETE & LAUNCH", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== PIXEL ART GIF VIEW COMPOSABLE ====================
@Composable
private fun PixelArtGifView(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    AndroidView(
        factory = { ctx ->
            ImageView(ctx).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    try {
                        val source = ImageDecoder.createSource(ctx.resources, R.drawable.welcome_pixel_art)
                        val drawable = ImageDecoder.decodeDrawable(source)
                        setImageDrawable(drawable)
                        if (drawable is AnimatedImageDrawable) {
                            drawable.start()
                        }
                    } catch (e: Exception) {
                        setImageResource(R.drawable.welcome_pixel_art)
                    }
                } else {
                    setImageResource(R.drawable.welcome_pixel_art)
                }
            }
        },
        modifier = modifier
    )
}

// ==================== REUSABLE STEP EXPLANATION BANNER ====================
@Composable
private fun StepExplanationBanner(explanationText: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = ElectricBlue.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = ElectricBlue,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = explanationText,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ==================== STEP 1: EMPLOYEE PROFILE ====================
@Composable
private fun Step1ProfileSection(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    isDark: Boolean,
    fieldShape: RoundedCornerShape
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Animated Pixel Art GIF Icon directly placed at top without box or border
        PixelArtGifView(
            modifier = Modifier.size(110.dp)
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Welcome to PingPin",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.5).sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Privacy-First Hybrid Work & Attendance Assistant",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        StepExplanationBanner(
            explanationText = "Enter your full name to personalize your daily attendance dashboard, greetings, and exported monthly PDF reports."
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = ElectricBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "EMPLOYEE PROFILE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ElectricBlue,
                        letterSpacing = 0.8.sp
                    )
                }

                OutlinedTextField(
                    value = fullName,
                    onValueChange = onFullNameChange,
                    label = { Text("Full Name *") },
                    placeholder = { Text("e.g. Alex Morgan") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = ElectricBlue) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = fieldShape,
                    singleLine = true
                )
            }
        }

        // Privacy Guarantee Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("100% Local Privacy", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.BatterySaver, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Zero Battery Drain", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==================== STEP 2: OFFICE WI-FI ====================
@Composable
private fun Step2WifiSection(
    ssid: String,
    onSsidChange: (String) -> Unit,
    liveConnectedSsid: String?,
    onUseConnectedSsid: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Office Workspace Wi-Fi",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        StepExplanationBanner(
            explanationText = "PingPin detects when you arrive at work by matching your connected Wi-Fi network SSID. No background GPS tracking is ever used."
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = EmeraldGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "OFFICE WI-FI NETWORK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldGreen,
                        letterSpacing = 0.8.sp
                    )
                }

                if (!liveConnectedSsid.isNullOrBlank() && liveConnectedSsid != ssid) {
                    Surface(
                        onClick = onUseConnectedSsid,
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldGreen.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CellWifi, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Connected: $liveConnectedSsid",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "USE THIS WI-FI",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldGreen
                            )
                        }
                    }
                }

                WifiSsidPickerField(
                    value = ssid,
                    onValueChange = onSsidChange,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ==================== STEP 3: SHIFT TIMINGS & ALARMS ====================
@Composable
private fun Step3ShiftTimingsSection(
    checkInTime: String,
    onCheckInTimeChange: (String) -> Unit,
    checkOutTime: String,
    onCheckOutTimeChange: (String) -> Unit,
    shiftDurationText: String
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Shift Timings & Alarms",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        StepExplanationBanner(
            explanationText = "Set your usual shift start and end times to schedule automated check-in reminders and calculate daily working hours."
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "QUICK SHIFT PRESETS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ElectricBlue,
                    letterSpacing = 0.8.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf("09:00 - 17:40", "09:30 - 18:10", "10:00 - 18:40")
                    presets.forEach { preset ->
                        val parts = preset.split(" - ")
                        val isSelected = checkInTime == parts[0] && checkOutTime == parts[1]
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onCheckInTimeChange(parts[0])
                                onCheckOutTimeChange(parts[1])
                            },
                            label = { Text(preset, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TimePickerField(
                        label = "Check-In Alarm",
                        time24 = checkInTime,
                        onTimeSelected = onCheckInTimeChange,
                        modifier = Modifier.weight(1f)
                    )
                    TimePickerField(
                        label = "Check-Out Alarm",
                        time24 = checkOutTime,
                        onTimeSelected = onCheckOutTimeChange,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Shift Duration Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Calculated Shift Duration",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = shiftDurationText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen
                        )
                    }
                }
            }
        }
    }
}

// ==================== STEP 4: WORK & WFO SCHEDULE ====================
@Composable
private fun Step4ScheduleSection(
    workingDaysMask: Int,
    onWorkingDaysMaskChange: (Int) -> Unit,
    wfoDaysMask: Int,
    onWfoDaysMaskChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Work & WFO Days Schedule",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        StepExplanationBanner(
            explanationText = "Define your weekly working days and mandatory office days to calculate hybrid attendance compliance and makeup dates."
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                WorkingDaysSelector(
                    workingDaysMask = workingDaysMask,
                    onMaskChanged = onWorkingDaysMaskChange
                )

                WfoDaysSelector(
                    wfoDaysMask = wfoDaysMask,
                    onMaskChanged = onWfoDaysMaskChange
                )

                val wfoCount = Integer.bitCount(wfoDaysMask)
                val totalCount = Integer.bitCount(workingDaysMask)
                val wfhCount = (totalCount - wfoCount).coerceAtLeast(0)

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Weekly Ratio Target", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "$wfoCount Office Days | $wfhCount WFH Days",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricBlue
                        )
                    }
                }
            }
        }
    }
}

// ==================== STEP 5: CHECK-IN PATH & HR PORTAL ====================
@Composable
private fun Step5PortalSection(
    portalMode: String,
    onPortalModeChange: (String) -> Unit,
    portalUrl: String,
    onPortalUrlChange: (String) -> Unit,
    autoCheckInEnabled: Boolean,
    onAutoCheckInEnabledChange: (Boolean) -> Unit,
    customCheckInKeywords: String,
    onCustomCheckInKeywordsChange: (String) -> Unit,
    customCheckOutKeywords: String,
    onCustomCheckOutKeywordsChange: (String) -> Unit,
    fieldShape: RoundedCornerShape
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Check-In Path & HR Portal",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        StepExplanationBanner(
            explanationText = "Enter your company HR portal URL. Tapping Next will open your portal in the app's HR portal viewer so your login is saved."
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "CHECK-IN EXECUTION PATH",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ElectricBlue,
                    letterSpacing = 0.8.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = { onPortalModeChange("IN_APP_AUTO") },
                        shape = RoundedCornerShape(14.dp),
                        color = if (portalMode == "IN_APP_AUTO") ElectricBlue.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(
                            1.dp,
                            if (portalMode == "IN_APP_AUTO") ElectricBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoMode,
                                contentDescription = null,
                                tint = if (portalMode == "IN_APP_AUTO") ElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Automated Path",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (portalMode == "IN_APP_AUTO") ElectricBlue else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "In-App Auto Web Portal",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Surface(
                        onClick = { onPortalModeChange("EXTERNAL_BROWSER") },
                        shape = RoundedCornerShape(14.dp),
                        color = if (portalMode == "EXTERNAL_BROWSER") EmeraldGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(
                            1.dp,
                            if (portalMode == "EXTERNAL_BROWSER") EmeraldGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                contentDescription = null,
                                tint = if (portalMode == "EXTERNAL_BROWSER") EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Manual Path",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (portalMode == "EXTERNAL_BROWSER") EmeraldGreen else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "External Chrome Browser",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                OutlinedTextField(
                    value = portalUrl,
                    onValueChange = onPortalUrlChange,
                    label = { Text("Company HR Portal URL") },
                    placeholder = { Text("e.g. hr.mycompany.com") },
                    leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, tint = EmeraldGreen) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = fieldShape,
                    singleLine = true
                )

                if (portalMode == "IN_APP_AUTO") {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Auto Check-In Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto Check-In / Punch Click",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Auto-clicks punch button when portal loads",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        PingPinSwitch(
                            checked = autoCheckInEnabled,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onAutoCheckInEnabledChange(it)
                            },
                            checkedTrackColor = EmeraldGreen
                        )
                    }

                    if (autoCheckInEnabled) {
                        OutlinedTextField(
                            value = customCheckInKeywords,
                            onValueChange = onCustomCheckInKeywordsChange,
                            label = { Text("Custom Check-In Keywords") },
                            placeholder = { Text("Check In, Clock In, Web Punch") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = WfoDayPurple) },
                            supportingText = { Text("Leave blank to use default keywords", fontSize = 10.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = fieldShape,
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = customCheckOutKeywords,
                            onValueChange = onCustomCheckOutKeywordsChange,
                            label = { Text("Custom Check-Out Keywords") },
                            placeholder = { Text("Check Out, Clock Out, Punch Out") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = WfoDayPurple) },
                            supportingText = { Text("Leave blank to use default keywords", fontSize = 10.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = fieldShape,
                            singleLine = true
                        )
                    }
                }
            }
        }
    }
}

// ==================== STEP 6: REVIEW & LAUNCH ====================
@Composable
private fun Step6ReviewAndLaunchSection(
    fullName: String,
    ssid: String,
    checkInTime: String,
    checkOutTime: String,
    shiftDurationText: String,
    workingDaysMask: Int,
    wfoDaysMask: Int,
    portalMode: String,
    portalUrl: String,
    autoCheckInEnabled: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Review Configuration & Launch",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        StepExplanationBanner(
            explanationText = "Double-check your setup summary below. Tapping Launch saves all settings 100% locally on your device."
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Profile Summary
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PROFILE SUMMARY", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = ElectricBlue)
                }
                ReviewItem("Full Name", fullName.ifBlank { "Not specified" })

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Workspace Summary
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Business, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("WORKSPACE & SHIFT", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = EmeraldGreen)
                }
                ReviewItem("Office Wi-Fi SSID", ssid.ifBlank { "Not configured" })
                ReviewItem("Check-In Alarm", checkInTime)
                ReviewItem("Check-Out Alarm", checkOutTime)
                ReviewItem("Shift Duration", shiftDurationText)
                ReviewDaysItem("Working Days", workingDaysMask, "${Integer.bitCount(workingDaysMask)} Days/Wk", ElectricBlue)
                ReviewDaysItem("WFO Days", wfoDaysMask, "${Integer.bitCount(wfoDaysMask)} Days/Wk", EmeraldGreen)

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Portal Summary
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VpnKey, contentDescription = null, tint = WfoDayPurple, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("HR PORTAL SETUP", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = WfoDayPurple)
                }
                ReviewItem("Execution Path", if (portalMode == "IN_APP_AUTO") "Automated Path (In-App Auto Portal)" else "Manual Path (External Browser)")
                if (portalUrl.isNotBlank()) ReviewItem("Portal URL", portalUrl)
                ReviewItem("Auto-Punch", if (autoCheckInEnabled) "Enabled" else "Disabled")
            }
        }
    }
}

@Composable
private fun DaysCirclePreview(
    mask: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        dayLabels.forEachIndexed { index, label ->
            val isSelected = (mask and (1 shl index)) != 0
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) accentColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(
                        1.dp,
                        if (isSelected) accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun ReviewDaysItem(label: String, mask: Int, countText: String, accentColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DaysCirclePreview(mask = mask, accentColor = accentColor)
            Text(text = countText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun ReviewItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}
