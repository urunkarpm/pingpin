package com.urunkarpm.pingpin.ui.settings

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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import com.urunkarpm.pingpin.ui.components.TimeFormatUtils
import com.urunkarpm.pingpin.ui.components.TimePickerField
import com.urunkarpm.pingpin.ui.components.WfoDaysSelector
import com.urunkarpm.pingpin.ui.components.WifiSsidPickerField
import com.urunkarpm.pingpin.ui.components.WorkingDaysSelector
import com.urunkarpm.pingpin.ui.theme.AmberOrange
import com.urunkarpm.pingpin.ui.theme.ElectricBlue
import com.urunkarpm.pingpin.ui.theme.EmeraldGreen
import com.urunkarpm.pingpin.ui.theme.WfoDayPurple
import kotlinx.coroutines.delay

private enum class SettingsSection {
    PROFILE,
    WORKSPACE,
    PORTAL_AUTOMATION,
    ALARM,
    OEM
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

    // Accordion expansion state: null means all collapsed by default
    var expandedSection by rememberSaveable { mutableStateOf<SettingsSection?>(null) }
    val profileExpanded = expandedSection == SettingsSection.PROFILE
    val workspaceExpanded = expandedSection == SettingsSection.WORKSPACE
    val portalAutomationExpanded = expandedSection == SettingsSection.PORTAL_AUTOMATION
    val alarmExpanded = expandedSection == SettingsSection.ALARM
    val oemExpanded = expandedSection == SettingsSection.OEM

    val oemGuidance = remember { OemBatteryHelper.getGuidance() }
    val fieldShape = remember { RoundedCornerShape(16.dp) }

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
            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero User Profile Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar Box with Gradient Rim & Specular Glow
                    Box(
                        modifier = Modifier
                            .size(60.dp)
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
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fullName.ifBlank { "Setup Profile" },
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = if (fullName.isBlank()) "Tap profile card to configure" else "PingPin Attendance Profile",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Status pill with pulse dot
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isProfileComplete) EmeraldGreen.copy(alpha = 0.15f) else AmberOrange.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
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

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

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

        // 2. Modern Theme Switcher Bar Card
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
                                        listOf(Color(0xFF6366F1), WfoDayPurple)
                                    else
                                        listOf(AmberOrange, Color(0xFFEF4444))
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
                            text = if (isDarkTheme) "Dark Theme Active" else "Light Theme Active",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isDarkTheme) "Switch to clean e-ink light mode" else "Switch to amoled dark mode",
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
                        uncheckedTrackColor = AmberOrange
                    )
                )
            }
        }

        // 3. Employee Profile Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    icon = Icons.Default.Badge,
                    title = "EMPLOYEE PROFILE",
                    subtitle = "Personal identity & organization profile",
                    gradientColors = listOf(ElectricBlue, Color(0xFF06B6D4)),
                    summaryBadge = if (!profileExpanded && fullName.isNotBlank()) fullName else null,
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

        // 4. Workspace & Shift Timings Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    icon = Icons.Default.Business,
                    title = "WORKSPACE & TIMINGS",
                    subtitle = "Office Wi-Fi network & shift schedule",
                    gradientColors = listOf(EmeraldGreen, Color(0xFF10B981)),
                    summaryBadge = if (!workspaceExpanded && ssid.isNotBlank()) "$ssid • $shiftDurationText" else null,
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
                        // Wi-Fi SSID Picker
                        WifiSsidPickerField(
                            value = ssid,
                            onValueChange = { ssid = it },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Check-In & Check-Out Time Pickers
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

                        // Shift Duration Tint Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EmeraldGreen.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.25f)),
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
                                        text = "Calculated Shift Horizon",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
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

                        // Company Portal URL
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
                                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open Portal URL", tint = EmeraldGreen)
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
                    }
                }
            }
        }

        // 5. HR Portal Automation Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    icon = Icons.Default.VpnKey,
                    title = "HR PORTAL AUTOMATION",
                    subtitle = "Auto-login & automated check-in execution",
                    gradientColors = listOf(WfoDayPurple, Color(0xFFEC4899)),
                    summaryBadge = if (!portalAutomationExpanded) if (autoLoginEnabled) "Auto-Login: ON" else "Auto-Login: OFF" else null,
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
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                            // Auto Check-In Switch Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Auto Check-In / Punch Action",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Auto-clicks Punch button on portal load",
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
                                Text(
                                    text = "Trigger Keywords (Removable Tags)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Interactive Keyword Chips Display
                                KeywordChipsGroup(
                                    label = "Check-In Keywords:",
                                    keywordsString = customCheckInKeywords,
                                    onKeywordsChanged = { customCheckInKeywords = it }
                                )

                                KeywordChipsGroup(
                                    label = "Check-Out Keywords:",
                                    keywordsString = customCheckOutKeywords,
                                    onKeywordsChanged = { customCheckOutKeywords = it }
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                            // Auto Login Switch Row
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
                                        text = "Fills username & password into portal form",
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
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = WfoDayPurple) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = fieldShape,
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = WfoDayPurple,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                    )
                                )

                                OutlinedTextField(
                                    value = portalPassword,
                                    onValueChange = { portalPassword = it },
                                    label = { Text("Portal Password") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = WfoDayPurple) },
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
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = WfoDayPurple,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                    )
                                )
                            }

                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val intent = com.urunkarpm.pingpin.ui.portal.PortalActivity.createIntent(
                                        context = context,
                                        actionType = com.urunkarpm.pingpin.ui.portal.PortalActivity.ACTION_CHECK_IN,
                                        portalUrl = portalUrl
                                    )
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = fieldShape,
                                colors = ButtonDefaults.buttonColors(containerColor = WfoDayPurple)
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

        // 6. Alarm Reliability & Precision Card
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
                    subtitle = "System permissions for punctual alert execution",
                    gradientColors = listOf(WfoDayPurple, Color(0xFF6366F1)),
                    summaryBadge = if (!alarmExpanded) if (hasExactAlarmPerm) "Exact Alarm: Ready" else "Exact Alarm: Action Req." else null,
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
                        // Exact Alarm Status Tile
                        StatusPermissionRow(
                            icon = if (hasExactAlarmPerm) Icons.Default.CheckCircle else Icons.Default.Warning,
                            title = "Exact Alarm Execution",
                            subtitle = if (hasExactAlarmPerm) "Granted • Guaranteed precise second timing" else "Restricted by OS • Tap to enable exact alarm permission",
                            isGranted = hasExactAlarmPerm,
                            actionText = "ENABLE",
                            onActionClick = {
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
                        )

                        // Battery Exemption Status Tile
                        StatusPermissionRow(
                            icon = if (isBatteryIgnored) Icons.Default.BatteryFull else Icons.Default.BatterySaver,
                            title = "Unrestricted Battery Mode",
                            subtitle = if (isBatteryIgnored) "Unrestricted • Immune to background app killer" else "Optimized • Tap to allow unrestricted background alarm execution",
                            isGranted = isBatteryIgnored,
                            actionText = "UNRESTRICT",
                            onActionClick = {
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
                        )

                        // Full Screen Intent Status Tile
                        val hasFullScreenAccess = hasOverlayPerm && hasFullScreenIntentPerm
                        StatusPermissionRow(
                            icon = if (hasFullScreenAccess) Icons.Default.Fullscreen else Icons.Default.Layers,
                            title = "Full-Screen Alert Display",
                            subtitle = if (hasFullScreenAccess) "Granted • Alarm pops up full-screen when screen is on" else "Restricted • Tap to allow full-screen alerts when screen is on",
                            isGranted = hasFullScreenAccess,
                            actionText = "GRANT",
                            onActionClick = {
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
                        )

                        // Test Alarm Button
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                notifService.fireTestAlarm(delaySeconds = 5)
                                testAlarmFired = true
                                Toast.makeText(
                                    context,
                                    "🔔 Test alarm will fire in 5 seconds!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = fieldShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WfoDayPurple,
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
                    }
                }
            }
        }

        // 7. OEM Battery Optimization Section Card
        if (oemGuidance != null) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(
                        icon = Icons.Default.BatteryAlert,
                        title = "BATTERY & SYSTEM HEALTH",
                        subtitle = "Device specific optimization settings for ${oemGuidance.oemName}",
                        gradientColors = listOf(AmberOrange, Color(0xFFF59E0B)),
                        summaryBadge = if (!oemExpanded) oemGuidance.oemName else null,
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
                                shape = fieldShape,
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
                        }
                    }
                }
            }
        }

        // 8. App Branding & Version Card
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
                                    colors = listOf(WfoDayPurple, Color(0xFF6366F1))
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
                            text = "Automated Wi-Fi & Schedule Engine",
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
            delay(600L)
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
    summaryBadge: String? = null,
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
        if (!expanded && summaryBadge != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(end = 6.dp)
            ) {
                Text(
                    text = summaryBadge,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeywordChipsGroup(
    label: String,
    keywordsString: String,
    onKeywordsChanged: (String) -> Unit
) {
    val keywordsList = remember(keywordsString) {
        keywordsString.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var newKeywordText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            keywordsList.forEach { kw ->
                InputChip(
                    selected = true,
                    onClick = {
                        // Remove chip on tap
                        val newList = keywordsList.filter { it != kw }
                        onKeywordsChanged(newList.joinToString(", "))
                    },
                    label = { Text(kw, fontSize = 11.sp) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove",
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = InputChipDefaults.inputChipColors(
                        selectedContainerColor = WfoDayPurple.copy(alpha = 0.18f),
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            // + Add Keyword Chip Button
            SuggestionChip(
                onClick = { showAddDialog = true },
                label = { Text("+ Add Tag", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp)) }
            )
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Trigger Keyword", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newKeywordText,
                    onValueChange = { newKeywordText = it },
                    label = { Text("Keyword (e.g. Punch In)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newKeywordText.isNotBlank()) {
                            val updated = (keywordsList + newKeywordText.trim()).joinToString(", ")
                            onKeywordsChanged(updated)
                            newKeywordText = ""
                        }
                        showAddDialog = false
                    }
                ) {
                    Text("ADD", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@Composable
private fun StatusPermissionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isGranted: Boolean,
    actionText: String,
    onActionClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isGranted) EmeraldGreen.copy(alpha = 0.12f) else ElectricBlue.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isGranted) EmeraldGreen.copy(alpha = 0.3f) else ElectricBlue.copy(alpha = 0.4f)),
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
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) EmeraldGreen else ElectricBlue,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!isGranted) {
                TextButton(onClick = onActionClick) {
                    Text(actionText, fontWeight = FontWeight.Bold, color = ElectricBlue, fontSize = 12.sp)
                }
            }
        }
    }
}

private fun calculateShiftDuration(checkIn: String, checkOut: String): String {
    return TimeFormatUtils.calculateShiftDuration(checkIn, checkOut)
}
