package com.urunkarpm.pingpin.ui.settings

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urunkarpm.pingpin.data.local.AppDatabase
import com.urunkarpm.pingpin.data.local.entity.OfficeConfigEntity
import com.urunkarpm.pingpin.data.local.entity.UserProfileEntity
import com.urunkarpm.pingpin.data.repository.OfficeConfigRepository
import com.urunkarpm.pingpin.data.repository.UserProfileRepository
import com.urunkarpm.pingpin.service.NotificationService
import com.urunkarpm.pingpin.service.OemBatteryHelper
import com.urunkarpm.pingpin.service.WorkingDays
import com.urunkarpm.pingpin.ui.components.GlassCard
import com.urunkarpm.pingpin.ui.components.TimePickerField
import com.urunkarpm.pingpin.ui.components.TimeFormatUtils
import com.urunkarpm.pingpin.ui.components.WifiSsidPickerField
import com.urunkarpm.pingpin.ui.components.WorkingDaysSelector
import com.urunkarpm.pingpin.ui.components.WfoDaysSelector
import com.urunkarpm.pingpin.ui.theme.AmberOrange
import com.urunkarpm.pingpin.ui.theme.ElectricBlue
import com.urunkarpm.pingpin.ui.theme.EmeraldGreen
import kotlinx.coroutines.launch

private enum class SettingsSection {
    PROFILE,
    WORKSPACE,
    PORTAL_AUTOMATION,
    ALARM,
    OEM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    onToggleTheme: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val haptic = LocalHapticFeedback.current

    val db = remember { AppDatabase.getInstance(context) }
    val officeConfigRepo = remember { OfficeConfigRepository(db.officeConfigDao()) }
    val profileRepo = remember { UserProfileRepository(db.userProfileDao()) }

    val configState by officeConfigRepo.configFlow.collectAsState(initial = null)
    val profileState by profileRepo.profileFlow.collectAsState(initial = null)

    var fullName by remember { mutableStateOf("") }

    var ssid by remember { mutableStateOf("") }
    var checkInTime by remember { mutableStateOf("09:30") }
    var checkOutTime by remember { mutableStateOf("17:30") }
    var portalUrl by remember { mutableStateOf("") }
    var workingDaysMask by remember { mutableStateOf(WorkingDays.DEFAULT_WEEKDAYS) }
    var wfoDaysMask by remember { mutableStateOf(WorkingDays.DEFAULT_WEEKDAYS) }

    val credManager = remember { com.urunkarpm.pingpin.service.portal.PortalCredentialManager(context) }
    var portalMode by remember { mutableStateOf("EXTERNAL_BROWSER") }
    var autoLoginEnabled by remember { mutableStateOf(false) }
    var autoCheckInEnabled by remember { mutableStateOf(false) }
    var customCheckInKeywords by remember { mutableStateOf("") }
    var customCheckOutKeywords by remember { mutableStateOf("") }
    var portalUsername by remember { mutableStateOf("") }
    var portalPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var testAlarmFired by remember { mutableStateOf(false) }

    // Accordion expansion state: only one section expanded at a time
    var expandedSection by remember { mutableStateOf<SettingsSection?>(null) }
    val profileExpanded = expandedSection == SettingsSection.PROFILE
    val workspaceExpanded = expandedSection == SettingsSection.WORKSPACE
    val portalAutomationExpanded = expandedSection == SettingsSection.PORTAL_AUTOMATION
    val alarmExpanded = expandedSection == SettingsSection.ALARM
    val oemExpanded = expandedSection == SettingsSection.OEM

    val oemGuidance = remember { OemBatteryHelper.getGuidance() }

    val fieldShape = RoundedCornerShape(16.dp)

    LaunchedEffect(configState, profileState) {
        configState?.let { cfg ->
            ssid = cfg.ssid
            checkInTime = cfg.checkInTime
            checkOutTime = cfg.checkOutTime
            portalUrl = cfg.portalUrl
            workingDaysMask = cfg.workingDaysMask
            wfoDaysMask = cfg.wfoDaysMask
            portalMode = cfg.portalMode
            autoLoginEnabled = cfg.autoLoginEnabled
            autoCheckInEnabled = cfg.autoCheckInEnabled
            customCheckInKeywords = cfg.customCheckInKeywords
            customCheckOutKeywords = cfg.customCheckOutKeywords
        }
        profileState?.let { prof ->
            fullName = prof.fullName
        }
        portalUsername = credManager.getUsername()
        portalPassword = credManager.getPassword()
    }

    val avatarInitials = remember(fullName) {
        if (fullName.isBlank()) "P"
        else fullName.trim().split("\\s+".toRegex()).mapNotNull { it.firstOrNull()?.uppercaseChar() }.take(2).joinToString("")
    }

    val shiftDurationText = remember(checkInTime, checkOutTime) {
        calculateShiftDuration(checkInTime, checkOutTime)
    }

    val activeDaysCount = remember(workingDaysMask) {
        (0 until 7).count { (workingDaysMask and (1 shl it)) != 0 }
    }

    val isProfileComplete = remember(fullName, ssid, checkInTime, checkOutTime) {
        fullName.isNotBlank() && ssid.isNotBlank()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Hero User Profile Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar Box with Gradient Rim & Pulse Glow
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(ElectricBlue, Color(0xFF06B6D4))
                                )
                            )
                            .border(2.5.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = avatarInitials,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = fullName.ifBlank { "Setup Profile" },
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = "Tap below to update your name",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Status pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isProfileComplete) EmeraldGreen.copy(alpha = 0.15f) else AmberOrange.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isProfileComplete) EmeraldGreen else AmberOrange)
                                )
                                Text(
                                    text = if (isProfileComplete) "AUTOMATION READY" else "CONFIGURATION PENDING",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isProfileComplete) EmeraldGreen else AmberOrange,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Stat Summary Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatSummaryChip(
                        icon = Icons.Default.Schedule,
                        label = "${TimeFormatUtils.format24To12Hour(checkInTime)} - ${TimeFormatUtils.format24To12Hour(checkOutTime)}",
                        modifier = Modifier.weight(1f)
                    )
                    StatSummaryChip(
                        icon = Icons.Default.DateRange,
                        label = "$activeDaysCount Days/Wk",
                        modifier = Modifier.weight(1f)
                    )
                    StatSummaryChip(
                        icon = Icons.Default.Wifi,
                        label = ssid.ifBlank { "No Wi-Fi" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Theme Toggle Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    if (isDarkTheme)
                                        listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                                    else
                                        listOf(Color(0xFFF59E0B), Color(0xFFEF4444))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (isDarkTheme) "Dark Mode" else "Light Mode",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isDarkTheme) "Switch to light theme" else "Switch to dark theme",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleTheme(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF6366F1),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFF59E0B)
                    )
                )
            }
        }

        // 1. Employee Profile Section Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    icon = Icons.Default.Badge,
                    title = "EMPLOYEE PROFILE",
                    subtitle = "Personal identity & organizational profile",
                    gradientColors = listOf(ElectricBlue, Color(0xFF06B6D4)),
                    expanded = profileExpanded,
                    onToggle = { expandedSection = if (profileExpanded) null else SettingsSection.PROFILE }
                )

                AnimatedVisibility(
                    visible = profileExpanded,
                    enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                    exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = ElectricBlue) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = fieldShape,
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            }
        }

        // 2. Office Workspace & Schedule Section Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    icon = Icons.Default.Business,
                    title = "WORKSPACE & TIMINGS",
                    subtitle = "Office Wi-Fi network & automated shift schedule",
                    gradientColors = listOf(EmeraldGreen, Color(0xFF10B981)),
                    expanded = workspaceExpanded,
                    onToggle = { expandedSection = if (workspaceExpanded) null else SettingsSection.WORKSPACE }
                )

                AnimatedVisibility(
                    visible = workspaceExpanded,
                    enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                    exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                // SSID Picker
                WifiSsidPickerField(
                    value = ssid,
                    onValueChange = { ssid = it },
                    modifier = Modifier.fillMaxWidth()
                )

                // Check-In & Check-Out Dual Pickers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TimePickerField(
                        label = "Check-In Time",
                        time24 = checkInTime,
                        onTimeSelected = { checkInTime = it },
                        modifier = Modifier.weight(1f)
                    )
                    TimePickerField(
                        label = "Check-Out Time",
                        time24 = checkOutTime,
                        onTimeSelected = { checkOutTime = it },
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

                // Portal URL Field
                OutlinedTextField(
                    value = portalUrl,
                    onValueChange = { portalUrl = it },
                    label = { Text("Company HR Portal URL (Optional)") },
                    placeholder = { Text("e.g. hr.mycompany.com") },
                    leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, tint = EmeraldGreen) },
                    trailingIcon = {
                        if (portalUrl.isNotBlank()) {
                            IconButton(onClick = {
                                val target = if (portalUrl.startsWith("http://") || portalUrl.startsWith("https://")) {
                                    portalUrl
                                } else {
                                    "https://$portalUrl"
                                }
                                try {
                                    uriHandler.openUri(target)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cannot open URL", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Default.OpenInNew, contentDescription = "Open Portal URL", tint = EmeraldGreen)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = fieldShape,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                )

                // Working Days Selector
                WorkingDaysSelector(
                    workingDaysMask = workingDaysMask,
                    onMaskChanged = { workingDaysMask = it }
                )

                // WFO Days Selector
                WfoDaysSelector(
                    wfoDaysMask = wfoDaysMask,
                    onMaskChanged = { wfoDaysMask = it }
                )
                } // end Column
                } // end AnimatedVisibility
            }
        }

        // HR Portal Automation Card (Separated Module)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    icon = Icons.Default.VpnKey,
                    title = "HR PORTAL AUTOMATION",
                    subtitle = "Automate login & check-in when alert triggers",
                    gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)),
                    expanded = portalAutomationExpanded,
                    onToggle = { expandedSection = if (portalAutomationExpanded) null else SettingsSection.PORTAL_AUTOMATION }
                )

                AnimatedVisibility(
                    visible = portalAutomationExpanded,
                    enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                    exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Execution Mode",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = portalMode == "IN_APP_AUTO",
                                onClick = { portalMode = "IN_APP_AUTO" },
                                label = { Text("In-App Auto Portal", fontSize = 12.sp) },
                                leadingIcon = {
                                    if (portalMode == "IN_APP_AUTO") {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = portalMode == "EXTERNAL_BROWSER",
                                onClick = { portalMode = "EXTERNAL_BROWSER" },
                                label = { Text("Chrome Browser", fontSize = 12.sp) },
                                leadingIcon = {
                                    if (portalMode == "EXTERNAL_BROWSER") {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

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
                                        text = "Auto Check-In / Check-Out Button Click",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Auto-clicks Punch button when portal loads",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = autoCheckInEnabled,
                                    onCheckedChange = { autoCheckInEnabled = it }
                                )
                            }

                            if (autoCheckInEnabled) {
                                OutlinedTextField(
                                    value = customCheckInKeywords,
                                    onValueChange = { customCheckInKeywords = it },
                                    label = { Text("Custom Check-In Keywords (Comma separated)") },
                                    placeholder = { Text("Check In, Clock In, Web Punch") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF8B5CF6)) },
                                    supportingText = { Text("Leave blank to use smart default keywords", fontSize = 10.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = fieldShape,
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = customCheckOutKeywords,
                                    onValueChange = { customCheckOutKeywords = it },
                                    label = { Text("Custom Check-Out Keywords (Comma separated)") },
                                    placeholder = { Text("Check Out, Clock Out, Punch Out") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF8B5CF6)) },
                                    supportingText = { Text("Leave blank to use smart default keywords", fontSize = 10.sp) },
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
                                        text = "Auto-Fill & Submit Credentials",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Fills username & password on login form",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = autoLoginEnabled,
                                    onCheckedChange = { autoLoginEnabled = it }
                                )
                            }

                            if (autoLoginEnabled) {
                                OutlinedTextField(
                                    value = portalUsername,
                                    onValueChange = { portalUsername = it },
                                    label = { Text("Portal Username / Email / Emp ID") },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF8B5CF6)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = fieldShape,
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = portalPassword,
                                    onValueChange = { portalPassword = it },
                                    label = { Text("Portal Password") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF8B5CF6)) },
                                    trailingIcon = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
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

                            Button(
                                onClick = {
                                    val intent = com.urunkarpm.pingpin.ui.portal.PortalActivity.createIntent(
                                        context = context,
                                        actionType = com.urunkarpm.pingpin.ui.portal.PortalActivity.ACTION_CHECK_IN,
                                        portalUrl = portalUrl
                                    )
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = fieldShape,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test In-App Auto Portal Now", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 3. Alarm Reliability & Precision Diagnostics Card
        val notifService = remember { NotificationService(context) }
        var hasExactAlarmPerm by remember { mutableStateOf(notifService.canScheduleExactAlarms()) }
        var isBatteryIgnored by remember { mutableStateOf(notifService.isIgnoringBatteryOptimizations()) }
        var hasOverlayPerm by remember { mutableStateOf(notifService.canDrawOverlays()) }
        var hasFullScreenIntentPerm by remember { mutableStateOf(notifService.canUseFullScreenIntent()) }

        val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                    hasExactAlarmPerm = notifService.canScheduleExactAlarms()
                    isBatteryIgnored = notifService.isIgnoringBatteryOptimizations()
                    hasOverlayPerm = notifService.canDrawOverlays()
                    hasFullScreenIntentPerm = notifService.canUseFullScreenIntent()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    icon = Icons.Default.AlarmOn,
                    title = "ALARM PRECISION & RELIABILITY",
                    subtitle = "System permissions for on-time clock alarms",
                    gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFF6366F1)),
                    expanded = alarmExpanded,
                    onToggle = { expandedSection = if (alarmExpanded) null else SettingsSection.ALARM }
                )

                AnimatedVisibility(
                    visible = alarmExpanded,
                    enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                    exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                // Exact Alarm Permission Status
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (hasExactAlarmPerm) EmeraldGreen.copy(alpha = 0.12f) else AmberOrange.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (hasExactAlarmPerm) EmeraldGreen.copy(alpha = 0.3f) else AmberOrange.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (hasExactAlarmPerm) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (hasExactAlarmPerm) EmeraldGreen else AmberOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Exact Alarm Execution",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (hasExactAlarmPerm) "Granted • Guaranteed precise second timing" else "Restricted by OS • Tap to enable exact alarm permission",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (!hasExactAlarmPerm) {
                            TextButton(
                                onClick = {
                                    try {
                                        val intent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                            android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, android.net.Uri.parse("package:${context.packageName}"))
                                        } else {
                                            android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}"))
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Open Settings -> Permissions to grant exact alarm permission", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Text("ENABLE", fontWeight = FontWeight.Bold, color = AmberOrange, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Battery Saver Exemption Status
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isBatteryIgnored) EmeraldGreen.copy(alpha = 0.12f) else ElectricBlue.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isBatteryIgnored) EmeraldGreen.copy(alpha = 0.3f) else ElectricBlue.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isBatteryIgnored) Icons.Default.BatteryFull else Icons.Default.BatterySaver,
                                contentDescription = null,
                                tint = if (isBatteryIgnored) EmeraldGreen else ElectricBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Unrestricted Battery Mode",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isBatteryIgnored) "Unrestricted • Immune to background app killer" else "Optimized • Tap to allow unrestricted background alarm execution",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (!isBatteryIgnored) {
                            TextButton(
                                onClick = {
                                    try {
                                        val intent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                            android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, android.net.Uri.parse("package:${context.packageName}"))
                                        } else {
                                            android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}"))
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Open Settings -> Battery to allow unrestricted execution", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Text("UNRESTRICT", fontWeight = FontWeight.Bold, color = ElectricBlue, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Display Over Other Apps / Full Screen Intent Status
                val hasFullScreenAccess = hasOverlayPerm && hasFullScreenIntentPerm
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (hasFullScreenAccess) EmeraldGreen.copy(alpha = 0.12f) else ElectricBlue.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (hasFullScreenAccess) EmeraldGreen.copy(alpha = 0.3f) else ElectricBlue.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (hasFullScreenAccess) Icons.Default.Fullscreen else Icons.Default.Layers,
                                contentDescription = null,
                                tint = if (hasFullScreenAccess) EmeraldGreen else ElectricBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Full-Screen Alert Display",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (hasFullScreenAccess) "Granted • Alarm pops up full-screen when screen is on" else "Restricted • Tap to allow full-screen alerts when screen is on",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (!hasFullScreenAccess) {
                            TextButton(
                                onClick = {
                                    try {
                                        val intent = if (!hasOverlayPerm && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                            android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:${context.packageName}"))
                                        } else if (!hasFullScreenIntentPerm && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                            android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, android.net.Uri.parse("package:${context.packageName}"))
                                        } else {
                                            android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}"))
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Open Settings -> Permissions to grant display over apps", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Text("GRANT", fontWeight = FontWeight.Bold, color = ElectricBlue, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Test Alarm Button
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        notifService.fireTestAlarm(delaySeconds = 5)
                        testAlarmFired = true
                        Toast.makeText(
                            context,
                            "\uD83D\uDD14 Test alarm will fire in 5 seconds!",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF8B5CF6),
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (testAlarmFired) "Test Alarm Scheduled (−5s)" else "Fire Test Alarm in 5 Seconds",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
                } // end Column
                } // end AnimatedVisibility
            }
        }

        // 4. OEM Battery Optimization Section Card
        if (oemGuidance != null) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(
                        icon = Icons.Default.BatteryAlert,
                        title = "BATTERY & SYSTEM HEALTH",
                        subtitle = "Device specific optimization settings for ${oemGuidance.oemName}",
                        gradientColors = listOf(AmberOrange, Color(0xFFF59E0B)),
                        expanded = oemExpanded,
                        onToggle = { expandedSection = if (oemExpanded) null else SettingsSection.OEM }
                    )

                    AnimatedVisibility(
                        visible = oemExpanded,
                        enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                        exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
                    ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = AmberOrange.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmberOrange.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            oemGuidance.steps.forEachIndexed { idx, step ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(AmberOrange),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${idx + 1}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Text(
                                        text = step,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            OemBatteryHelper.launchOemSettings(context, oemGuidance)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, AmberOrange),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberOrange)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open ${oemGuidance.oemName} Battery Settings", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    } // end Column
                    } // end AnimatedVisibility
                }
            }
        }

        // 4. App Branding & Version Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "PingPin Attendance v1.0",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Automated Wi-Fi & Schedule Scanner",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "ACTIVE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // Auto-save effect: debounced 600ms after any field change
        val isFirstLoad = remember { mutableStateOf(true) }
        LaunchedEffect(
            fullName, ssid, checkInTime, checkOutTime, portalUrl,
            workingDaysMask, wfoDaysMask, portalMode, autoLoginEnabled,
            autoCheckInEnabled, customCheckInKeywords, customCheckOutKeywords,
            portalUsername, portalPassword
        ) {
            if (isFirstLoad.value) {
                isFirstLoad.value = false
                return@LaunchedEffect
            }
            kotlinx.coroutines.delay(600L)
            val currentCfg = configState ?: OfficeConfigEntity()
            val newConfig = currentCfg.copy(
                id = configState?.id ?: 0,
                ssid = ssid.trim(),
                checkInTime = checkInTime.trim(),
                checkOutTime = checkOutTime.trim(),
                portalUrl = portalUrl.trim(),
                workingDaysMask = workingDaysMask,
                wfoDaysMask = wfoDaysMask,
                portalMode = portalMode,
                autoLoginEnabled = autoLoginEnabled,
                autoCheckInEnabled = autoCheckInEnabled,
                customCheckInKeywords = customCheckInKeywords.trim(),
                customCheckOutKeywords = customCheckOutKeywords.trim()
            )
            officeConfigRepo.saveConfig(newConfig)

            credManager.saveCredentials(portalUsername, portalPassword)

            val newProfile = UserProfileEntity(
                id = profileState?.id ?: 0,
                fullName = fullName.trim()
            )
            profileRepo.saveProfile(newProfile)

            val svc = NotificationService(context)
            svc.scheduleAlarmsFromConfig(newConfig)
        }
    }
}

@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String,
    subtitle: String,
    gradientColors: List<Color>,
    expanded: Boolean = true,
    onToggle: (() -> Unit)? = null
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "chevron_rotation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onToggle != null) Modifier.clickable(onClick = onToggle)
                else Modifier
            )
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(gradientColors)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = 0.6.sp
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (onToggle != null) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(22.dp)
                    .rotate(chevronRotation)
            )
        }
    }
}

@Composable
private fun StatSummaryChip(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun calculateShiftDuration(checkIn: String, checkOut: String): String {
    return TimeFormatUtils.calculateShiftDuration(checkIn, checkOut)
}

