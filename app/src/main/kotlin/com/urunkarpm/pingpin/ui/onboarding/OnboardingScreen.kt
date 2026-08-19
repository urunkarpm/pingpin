package com.urunkarpm.pingpin.ui.onboarding

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urunkarpm.pingpin.data.local.AppDatabase
import com.urunkarpm.pingpin.data.local.entity.OfficeConfigEntity
import com.urunkarpm.pingpin.data.local.entity.UserProfileEntity
import com.urunkarpm.pingpin.data.repository.OfficeConfigRepository
import com.urunkarpm.pingpin.data.repository.UserProfileRepository
import com.urunkarpm.pingpin.service.NotificationService
import com.urunkarpm.pingpin.service.WifiService
import com.urunkarpm.pingpin.service.WorkingDays
import com.urunkarpm.pingpin.service.portal.PortalCredentialManager
import com.urunkarpm.pingpin.ui.components.GlassCard
import com.urunkarpm.pingpin.ui.components.TimePickerField
import com.urunkarpm.pingpin.ui.components.WifiSsidPickerField
import com.urunkarpm.pingpin.ui.components.WorkingDaysSelector
import com.urunkarpm.pingpin.ui.components.WfoDaysSelector
import com.urunkarpm.pingpin.ui.portal.PortalActivity
import com.urunkarpm.pingpin.ui.theme.ElectricBlue
import com.urunkarpm.pingpin.ui.theme.EmeraldGreen
import kotlinx.coroutines.launch

enum class TestRunStatus {
    NOT_TESTED,
    VERIFIED,
    FAILED
}

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val db = remember { AppDatabase.getInstance(context) }
    val officeConfigRepo = remember { OfficeConfigRepository(db.officeConfigDao()) }
    val profileRepo = remember { UserProfileRepository(db.userProfileDao()) }
    val wifiService = remember { WifiService(context) }
    val credManager = remember { PortalCredentialManager(context) }

    // Progressive 7-step wizard state
    var currentStep by remember { mutableIntStateOf(1) }
    val totalSteps = 7

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

    var portalMode by remember { mutableStateOf("IN_APP_AUTO") } // "IN_APP_AUTO" (Automated Path) vs "EXTERNAL_BROWSER" (Manual Path)
    var portalPreset by remember { mutableStateOf("GENERIC") }
    var portalUrl by remember { mutableStateOf("") }
    var autoCheckInEnabled by remember { mutableStateOf(true) }
    var customCheckInKeywords by remember { mutableStateOf("") }
    var customCheckOutKeywords by remember { mutableStateOf("") }
    var autoLoginEnabled by remember { mutableStateOf(false) }
    var portalUsername by remember { mutableStateOf("") }
    var portalPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var testRunStatus by remember { mutableStateOf(TestRunStatus.NOT_TESTED) }

    val shiftDurationText = remember(checkInTime, checkOutTime) {
        com.urunkarpm.pingpin.ui.components.TimeFormatUtils.calculateShiftDuration(checkInTime, checkOutTime)
    }

    val fieldShape = RoundedCornerShape(16.dp)

    LaunchedEffect(Unit) {
        val currentSsid = wifiService.getWifiSSID()
        if (!currentSsid.isNullOrEmpty()) {
            ssid = currentSsid
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "hero_glow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    // Progress animation
    val progressAnimated by animateFloatAsState(
        targetValue = (currentStep - 1).toFloat() / totalSteps.toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "setup_progress"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(Color(0xFF070A11), Color(0xFF0F172A))
                    } else {
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f)
                        )
                    }
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Top ambient glowing aura
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-40).dp)
                .size(320.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ElectricBlue.copy(alpha = if (isDark) 0.22f else 0.10f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ==================== TOP COMPLETION STATUS BAR ====================
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.6f else 0.85f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    // Header Row: Step Title & Percentage
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (currentStep) {
                                    1 -> "Step 1 of 7: Employee Profile"
                                    2 -> "Step 2 of 7: Office Wi-Fi Network"
                                    3 -> "Step 3 of 7: Shift Timings & Alarms"
                                    4 -> "Step 4 of 7: Work & WFO Days Schedule"
                                    5 -> "Step 5 of 7: Check-In Path & HR Portal"
                                    6 -> "Step 6 of 7: Test Run Setup"
                                    else -> "Step 7 of 7: Review & Complete"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ElectricBlue.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${(progressAnimated * 100).toInt()}% COMPLETE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ElectricBlue,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Animated Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressAnimated)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(ElectricBlue, EmeraldGreen)
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tappable Step Indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val stepTitles = listOf("Profile", "Wi-Fi", "Shift", "Schedule", "Portal", "Test", "Launch")
                        stepTitles.forEachIndexed { index, title ->
                            val stepNumber = index + 1
                            val isCompleted = stepNumber < currentStep
                            val isCurrent = stepNumber == currentStep

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(enabled = stepNumber <= currentStep) {
                                        currentStep = stepNumber
                                    }
                                    .padding(horizontal = 3.dp, vertical = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isCurrent -> ElectricBlue
                                                isCompleted -> EmeraldGreen
                                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCompleted) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "$stepNumber",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = title,
                                    fontSize = 10.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrent) ElectricBlue else if (isCompleted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // ==================== INTERACTIVE STEP CONTENT WIZARD ====================
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
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
                                pulseScale = pulseScale,
                                isDark = isDark,
                                fieldShape = fieldShape
                            )

                            2 -> Step2WifiSection(
                                ssid = ssid,
                                onSsidChange = { ssid = it }
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
                                onPortalModeChange = { portalMode = it },
                                portalPreset = portalPreset,
                                onPortalPresetChange = { preset ->
                                    portalPreset = preset
                                    when (preset) {
                                        "DARWINBOX" -> {
                                            if (portalUrl.isBlank()) portalUrl = "https://mycompany.darwinbox.in"
                                            if (customCheckInKeywords.isBlank()) customCheckInKeywords = "Clock In, Check In"
                                            if (customCheckOutKeywords.isBlank()) customCheckOutKeywords = "Clock Out, Check Out"
                                        }
                                        "KEKA" -> {
                                            if (portalUrl.isBlank()) portalUrl = "https://mycompany.keka.com"
                                            if (customCheckInKeywords.isBlank()) customCheckInKeywords = "Web Punch, Clock In"
                                            if (customCheckOutKeywords.isBlank()) customCheckOutKeywords = "Web Punch Out, Clock Out"
                                        }
                                        "GREYTHR" -> {
                                            if (portalUrl.isBlank()) portalUrl = "https://mycompany.greythr.com"
                                            if (customCheckInKeywords.isBlank()) customCheckInKeywords = "Sign In, Check In"
                                            if (customCheckOutKeywords.isBlank()) customCheckOutKeywords = "Sign Out, Check Out"
                                        }
                                        else -> {}
                                    }
                                },
                                portalUrl = portalUrl,
                                onPortalUrlChange = { portalUrl = it },
                                autoCheckInEnabled = autoCheckInEnabled,
                                onAutoCheckInEnabledChange = { autoCheckInEnabled = it },
                                customCheckInKeywords = customCheckInKeywords,
                                onCustomCheckInKeywordsChange = { customCheckInKeywords = it },
                                customCheckOutKeywords = customCheckOutKeywords,
                                onCustomCheckOutKeywordsChange = { customCheckOutKeywords = it },
                                autoLoginEnabled = autoLoginEnabled,
                                onAutoLoginEnabledChange = { autoLoginEnabled = it },
                                portalUsername = portalUsername,
                                onPortalUsernameChange = { portalUsername = it },
                                portalPassword = portalPassword,
                                onPortalPasswordChange = { portalPassword = it },
                                passwordVisible = passwordVisible,
                                onPasswordVisibleToggle = { passwordVisible = !passwordVisible },
                                fieldShape = fieldShape
                            )

                            6 -> Step6TestRunSection(
                                portalMode = portalMode,
                                portalUrl = portalUrl,
                                autoLoginEnabled = autoLoginEnabled,
                                portalUsername = portalUsername,
                                testRunStatus = testRunStatus,
                                onRunTestClick = {
                                    scope.launch {
                                        // Temporarily save config and credentials for testing
                                        val tempConfig = OfficeConfigEntity(
                                            ssid = ssid.trim(),
                                            checkInTime = checkInTime.trim(),
                                            checkOutTime = checkOutTime.trim(),
                                            lateCutoffTime = lateCutoffTime.trim(),
                                            portalUrl = portalUrl.trim(),
                                            workingDaysMask = workingDaysMask,
                                            wfoDaysMask = wfoDaysMask,
                                            portalMode = portalMode,
                                            autoLoginEnabled = autoLoginEnabled,
                                            autoCheckInEnabled = autoCheckInEnabled,
                                            portalPreset = portalPreset,
                                            customCheckInKeywords = customCheckInKeywords.trim(),
                                            customCheckOutKeywords = customCheckOutKeywords.trim()
                                        )
                                        officeConfigRepo.saveConfig(tempConfig)

                                        if (portalUsername.isNotBlank() || portalPassword.isNotBlank()) {
                                            credManager.saveCredentials(portalUsername.trim(), portalPassword)
                                        }

                                        // Launch test in PortalActivity
                                        val intent = PortalActivity.createIntent(
                                            context = context,
                                            actionType = PortalActivity.ACTION_CHECK_IN,
                                            portalUrl = portalUrl.trim(),
                                            isTestMode = true
                                        )
                                        context.startActivity(intent)
                                        testRunStatus = TestRunStatus.VERIFIED
                                    }
                                }
                            )

                            7 -> Step7ReviewAndLaunchSection(
                                fullName = fullName,
                                ssid = ssid,
                                checkInTime = checkInTime,
                                checkOutTime = checkOutTime,
                                shiftDurationText = shiftDurationText,
                                workingDaysMask = workingDaysMask,
                                wfoDaysMask = wfoDaysMask,
                                portalMode = portalMode,
                                portalPreset = portalPreset,
                                portalUrl = portalUrl,
                                autoLoginEnabled = autoLoginEnabled,
                                autoCheckInEnabled = autoCheckInEnabled,
                                testRunStatus = testRunStatus
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
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.7f else 0.9f),
                border = androidx.compose.foundation.BorderStroke(
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
                            onClick = { currentStep-- },
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f))
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

                    // Next / Complete Button
                    if (currentStep < totalSteps) {
                        Button(
                            onClick = {
                                when (currentStep) {
                                    1 -> {
                                        if (fullName.trim().isEmpty()) {
                                            Toast.makeText(context, "Please enter your Full Name to continue", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                    }
                                    2 -> {
                                        if (ssid.trim().isEmpty()) {
                                            Toast.makeText(context, "Please select or enter your Office Wi-Fi SSID", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                    }
                                }
                                currentStep++
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Next Step", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                        // Complete Setup CTA
                        Button(
                            onClick = {
                                if (ssid.trim().isEmpty()) {
                                    Toast.makeText(context, "Please select or enter your Office Wi-Fi SSID", Toast.LENGTH_SHORT).show()
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
                                        autoLoginEnabled = autoLoginEnabled,
                                        autoCheckInEnabled = autoCheckInEnabled,
                                        portalPreset = portalPreset,
                                        customCheckInKeywords = customCheckInKeywords.trim(),
                                        customCheckOutKeywords = customCheckOutKeywords.trim()
                                    )
                                    officeConfigRepo.saveConfig(config)

                                    val profile = UserProfileEntity(
                                        fullName = fullName.trim()
                                    )
                                    profileRepo.saveProfile(profile)

                                    if (portalUsername.isNotBlank() || portalPassword.isNotBlank()) {
                                        credManager.saveCredentials(portalUsername.trim(), portalPassword)
                                    }

                                    // Schedule Alarms
                                    NotificationService(context).scheduleAlarmsFromConfig(config)

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

// ==================== REUSABLE 1-2 LINE STEP EXPLANATION BANNER ====================
@Composable
private fun StepExplanationBanner(explanationText: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = ElectricBlue.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.25f))
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
    pulseScale: Float,
    isDark: Boolean,
    fieldShape: RoundedCornerShape
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {


        Text(
            text = "Welcome to PingPin",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = (-0.5).sp,
            textAlign = TextAlign.Center
        )

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
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = ElectricBlue) },
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
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("100% Local", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
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
    onSsidChange: (String) -> Unit
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
            explanationText = "PingPin detects when you arrive at work by matching this Wi-Fi network SSID. No background GPS tracking is ever used."
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
            explanationText = "Set your usual shift start and end times to schedule automated check-in reminders and calculate working hours."
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
                        val isSelected = checkInTime == preset.split(" - ")[0] && checkOutTime == preset.split(" - ")[1]
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val parts = preset.split(" - ")
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
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
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
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Weekly Ratio", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    portalPreset: String,
    onPortalPresetChange: (String) -> Unit,
    portalUrl: String,
    onPortalUrlChange: (String) -> Unit,
    autoCheckInEnabled: Boolean,
    onAutoCheckInEnabledChange: (Boolean) -> Unit,
    customCheckInKeywords: String,
    onCustomCheckInKeywordsChange: (String) -> Unit,
    customCheckOutKeywords: String,
    onCustomCheckOutKeywordsChange: (String) -> Unit,
    autoLoginEnabled: Boolean,
    onAutoLoginEnabledChange: (Boolean) -> Unit,
    portalUsername: String,
    onPortalUsernameChange: (String) -> Unit,
    portalPassword: String,
    onPortalPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibleToggle: () -> Unit,
    fieldShape: RoundedCornerShape
) {
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
            explanationText = "Choose Automated Path (in-app auto-fill & punch) or Manual Path (external Chrome browser), then enter your portal details."
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
                        border = androidx.compose.foundation.BorderStroke(
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
                        border = androidx.compose.foundation.BorderStroke(
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
                                imageVector = Icons.Default.OpenInNew,
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
                        Switch(
                            checked = autoCheckInEnabled,
                            onCheckedChange = onAutoCheckInEnabledChange
                        )
                    }

                    if (autoCheckInEnabled) {
                        OutlinedTextField(
                            value = customCheckInKeywords,
                            onValueChange = onCustomCheckInKeywordsChange,
                            label = { Text("Custom Check-In Keywords (Comma separated)") },
                            placeholder = { Text("Check In, Clock In, Web Punch") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF8B5CF6)) },
                            supportingText = { Text("Leave blank to use default keywords", fontSize = 10.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = fieldShape,
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = customCheckOutKeywords,
                            onValueChange = onCustomCheckOutKeywordsChange,
                            label = { Text("Custom Check-Out Keywords (Comma separated)") },
                            placeholder = { Text("Check Out, Clock Out, Punch Out") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF8B5CF6)) },
                            supportingText = { Text("Leave blank to use default keywords", fontSize = 10.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = fieldShape,
                            singleLine = true
                        )
                    }

                    // Auto Login Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Fill Credentials",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Auto-fills login credentials securely on device",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoLoginEnabled,
                            onCheckedChange = onAutoLoginEnabledChange
                        )
                    }

                    if (autoLoginEnabled) {
                        OutlinedTextField(
                            value = portalUsername,
                            onValueChange = onPortalUsernameChange,
                            label = { Text("Portal Username / Email / Emp ID") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF8B5CF6)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = fieldShape,
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = portalPassword,
                            onValueChange = onPortalPasswordChange,
                            label = { Text("Portal Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF8B5CF6)) },
                            trailingIcon = {
                                IconButton(onClick = onPasswordVisibleToggle) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
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

// ==================== STEP 6: TEST RUN SETUP ====================
@Composable
private fun Step6TestRunSection(
    portalMode: String,
    portalUrl: String,
    autoLoginEnabled: Boolean,
    portalUsername: String,
    testRunStatus: TestRunStatus,
    onRunTestClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Test Run Check-In Setup",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        StepExplanationBanner(
            explanationText = "Run a live dry-run test of your selected check-in path right now to verify login credentials and punch button detection."
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DRY-RUN VERIFICATION ENGINE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ElectricBlue,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when (testRunStatus) {
                            TestRunStatus.VERIFIED -> EmeraldGreen.copy(alpha = 0.15f)
                            TestRunStatus.FAILED -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = when (testRunStatus) {
                                TestRunStatus.VERIFIED -> "VERIFIED"
                                TestRunStatus.FAILED -> "FAILED"
                                else -> "NOT TESTED"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = when (testRunStatus) {
                                TestRunStatus.VERIFIED -> EmeraldGreen
                                TestRunStatus.FAILED -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                ReviewItem("Selected Path", if (portalMode == "IN_APP_AUTO") "Automated Path (In-App Auto Portal)" else "Manual Path (External Browser)")
                ReviewItem("Portal Target URL", portalUrl.ifBlank { "Not configured" })
                if (portalMode == "IN_APP_AUTO") {
                    ReviewItem("Auto-Fill Credentials", if (autoLoginEnabled && portalUsername.isNotBlank()) "Configured ($portalUsername)" else "Disabled / None")
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onRunTestClick,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TEST RUN HR PORTAL LOGIN & PUNCH",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ==================== STEP 7: REVIEW & LAUNCH ====================
@Composable
private fun Step7ReviewAndLaunchSection(
    fullName: String,
    ssid: String,
    checkInTime: String,
    checkOutTime: String,
    shiftDurationText: String,
    workingDaysMask: Int,
    wfoDaysMask: Int,
    portalMode: String,
    portalPreset: String,
    portalUrl: String,
    autoLoginEnabled: Boolean,
    autoCheckInEnabled: Boolean,
    testRunStatus: TestRunStatus
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
                ReviewItem("Office Wi-Fi SSID", ssid)
                ReviewItem("Check-In Alarm", checkInTime)
                ReviewItem("Check-Out Alarm", checkOutTime)
                ReviewItem("Shift Duration", shiftDurationText)
                ReviewDaysItem("Working Days", workingDaysMask, "${java.lang.Integer.bitCount(workingDaysMask)} Days/Wk", ElectricBlue)
                ReviewDaysItem("WFO Days", wfoDaysMask, "${java.lang.Integer.bitCount(wfoDaysMask)} Days/Wk", EmeraldGreen)

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Portal Summary
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VpnKey, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("HR PORTAL SETUP", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF8B5CF6))
                }
                ReviewItem("Execution Path", if (portalMode == "IN_APP_AUTO") "Automated Path (In-App Auto Portal)" else "Manual Path (External Browser)")
                if (portalUrl.isNotBlank()) ReviewItem("Portal URL", portalUrl)
                ReviewItem("Auto-Punch", if (autoCheckInEnabled) "Enabled" else "Disabled")
                ReviewItem("Auto-Login", if (autoLoginEnabled) "Enabled" else "Disabled")
                ReviewItem("Test Verification", if (testRunStatus == TestRunStatus.VERIFIED) "Verified Successfully" else "Not Tested")
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
