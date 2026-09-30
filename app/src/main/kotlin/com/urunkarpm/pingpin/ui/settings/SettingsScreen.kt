package com.urunkarpm.pingpin.ui.settings

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.urunkarpm.pingpin.R
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urunkarpm.pingpin.data.local.AppDatabase
import com.urunkarpm.pingpin.data.local.entity.OfficeConfigEntity
import com.urunkarpm.pingpin.data.local.entity.UserProfileEntity
import com.urunkarpm.pingpin.service.NotificationService
import com.urunkarpm.pingpin.service.OemBatteryHelper
import com.urunkarpm.pingpin.service.WorkingDays
import com.urunkarpm.pingpin.ui.components.AppChangelogDialog
import com.urunkarpm.pingpin.ui.components.GlassCard
import com.urunkarpm.pingpin.ui.components.PingPinSwitch
import com.urunkarpm.pingpin.ui.components.TimeFormatUtils
import com.urunkarpm.pingpin.ui.components.TimePickerField
import com.urunkarpm.pingpin.ui.components.WfoDaysSelector
import com.urunkarpm.pingpin.ui.components.WifiSsidPickerField
import com.urunkarpm.pingpin.ui.components.WorkingDaysSelector

private enum class SettingsCategory(
    val title: String,
    val tabLabel: String,
    val icon: ImageVector
) {
    PROFILE_SHIFT("Profile & Shift", "Profile", Icons.Outlined.Badge),
    AUTOMATION("Automation", "Automation", Icons.Outlined.AutoAwesome),
    RELIABILITY("System Health", "Health", Icons.Outlined.Shield),
    UPDATES("Updates", "Updates", Icons.Outlined.RocketLaunch)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    onToggleTheme: (Boolean) -> Unit = {},
    viewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    appUpdateViewModel: com.urunkarpm.pingpin.ui.update.AppUpdateViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val haptic = LocalHapticFeedback.current

    val currentAppVersion = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "2.2.0"
        } catch (e: Exception) {
            "2.2.0"
        }
    }

    val configState by viewModel.configState.collectAsState()
    val profileState by viewModel.profileState.collectAsState()

    var fullName by remember(profileState) { mutableStateOf(profileState?.fullName ?: "") }
    var ssid by remember(configState) { mutableStateOf(configState?.ssid ?: "") }
    var checkInTime by remember(configState) { mutableStateOf(configState?.checkInTime ?: "09:30") }
    var checkOutTime by remember(configState) { mutableStateOf(configState?.checkOutTime ?: "17:30") }
    var portalUrl by remember(configState) { mutableStateOf(configState?.portalUrl ?: "") }
    var workingDaysMask by remember(configState) { mutableStateOf(configState?.workingDaysMask ?: WorkingDays.DEFAULT_WEEKDAYS) }
    var wfoDaysMask by remember(configState) { mutableStateOf(configState?.wfoDaysMask ?: WorkingDays.DEFAULT_WEEKDAYS) }

    val credManager = remember { com.urunkarpm.pingpin.service.portal.PortalCredentialManager(context) }
    var portalMode by remember { mutableStateOf("EXTERNAL_BROWSER") }
    var autoLoginEnabled by remember { mutableStateOf(false) }
    var autoCheckInEnabled by remember { mutableStateOf(false) }
    var customCheckInKeywords by remember { mutableStateOf("") }
    var customCheckOutKeywords by remember { mutableStateOf("") }


    var showAppChangelogDialog by remember { mutableStateOf(false) }

    var selectedCategory by rememberSaveable { mutableStateOf(SettingsCategory.PROFILE_SHIFT) }

    val oemGuidance = remember { OemBatteryHelper.getGuidance() }
    val fieldShape = remember { RoundedCornerShape(14.dp) }

    // Cinematic Emergence Animation State (Originating from bottom nav Settings icon)
    var isDockEmerged by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isDockEmerged = true
    }

    val dockScale by animateFloatAsState(
        targetValue = if (isDockEmerged) 1.0f else 0.45f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow),
        label = "dock_scale"
    )

    val dockAlpha by animateFloatAsState(
        targetValue = if (isDockEmerged) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        label = "dock_alpha"
    )

    val dockTranslationY by animateDpAsState(
        targetValue = if (isDockEmerged) 0.dp else 52.dp,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow),
        label = "dock_translation_y"
    )

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
    }

    val shiftDurationText = remember(checkInTime, checkOutTime) {
        TimeFormatUtils.calculateShiftDuration(checkInTime, checkOutTime)
    }

    // System Permissions lifecycle polling
    val notifService = remember { NotificationService(context) }
    var hasExactAlarmPerm by remember { mutableStateOf(notifService.canScheduleExactAlarms()) }
    var isBatteryIgnored by remember { mutableStateOf(notifService.isIgnoringBatteryOptimizations()) }
    var hasFullScreenIntentPerm by remember { mutableStateOf(notifService.canUseFullScreenIntent()) }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasExactAlarmPerm = notifService.canScheduleExactAlarms()
                isBatteryIgnored = notifService.isIgnoringBatteryOptimizations()
                hasFullScreenIntentPerm = notifService.canUseFullScreenIntent()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // 1. Scrollable Settings Form Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 150.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Sleek Header Bar with Theme Switch Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settings",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.5).sp,
                    modifier = Modifier.semantics { heading() }
                )

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = if (isDarkTheme) Color(0xFF141923) else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = {
                                if (isDarkTheme) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onToggleTheme(false)
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = if (!isDarkTheme) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shadowElevation = if (!isDarkTheme) 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.LightMode,
                                    contentDescription = "Light Theme",
                                    tint = if (!isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Light",
                                    fontSize = 11.5.sp,
                                    fontWeight = if (!isDarkTheme) FontWeight.Bold else FontWeight.Medium,
                                    color = if (!isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                if (!isDarkTheme) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onToggleTheme(true)
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isDarkTheme) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shadowElevation = if (isDarkTheme) 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DarkMode,
                                    contentDescription = "Dark Theme",
                                    tint = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Dark",
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isDarkTheme) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Category Content Sections
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (selectedCategory) {
                    SettingsCategory.PROFILE_SHIFT -> {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "PERSONAL PROFILE & SHIFT",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.8.sp
                                )

                                OutlinedTextField(
                                    value = fullName,
                                    onValueChange = { fullName = it },
                                    label = { Text("Full Name") },
                                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = fieldShape,
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                    )
                                )

                                WifiSsidPickerField(
                                    value = ssid,
                                    onValueChange = { ssid = it },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    TimePickerField(
                                        label = "Check-In",
                                        time24 = checkInTime,
                                        onTimeSelected = { checkInTime = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                    TimePickerField(
                                        label = "Check-Out",
                                        time24 = checkOutTime,
                                        onTimeSelected = { checkOutTime = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Outlined.Bolt,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Calculated Shift Duration",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            text = shiftDurationText,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = portalUrl,
                                    onValueChange = { portalUrl = it },
                                    label = { Text("Company HR Portal URL") },
                                    placeholder = { Text("e.g. hr.mycompany.com") },
                                    leadingIcon = { Icon(Icons.Outlined.Language, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
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
                                                Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = "Open Portal URL", tint = MaterialTheme.colorScheme.secondary)
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = fieldShape,
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                    )
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                                WorkingDaysSelector(
                                    workingDaysMask = workingDaysMask,
                                    onMaskChanged = { workingDaysMask = it }
                                )

                                WfoDaysSelector(
                                    wfoDaysMask = wfoDaysMask,
                                    onMaskChanged = { wfoDaysMask = it }
                                )
                            }
                        }
                    }

                    SettingsCategory.AUTOMATION -> {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "PORTAL AUTOMATION",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    letterSpacing = 0.8.sp
                                )

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDarkTheme) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(3.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val modes = listOf(
                                            Triple("IN_APP_AUTO", "In-App Auto Portal", Icons.Outlined.AutoAwesome),
                                            Triple("EXTERNAL_BROWSER", "Chrome Browser", Icons.Outlined.OpenInBrowser)
                                        )

                                        modes.forEach { (mode, label, icon) ->
                                            val isSelected = portalMode == mode
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(
                                                        if (isSelected) MaterialTheme.colorScheme.tertiary else Color.Transparent
                                                    )
                                                    .clickable { portalMode = mode }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = icon,
                                                        contentDescription = null,
                                                        tint = if (isSelected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = label,
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (portalMode == "IN_APP_AUTO") {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

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
                                                text = "Automatically clicks Punch button on portal load",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        PingPinSwitch(
                                            checked = autoCheckInEnabled,
                                            onCheckedChange = { autoCheckInEnabled = it },
                                            checkedTrackColor = com.urunkarpm.pingpin.ui.theme.EmeraldGreen
                                        )
                                    }

                                    if (autoCheckInEnabled) {
                                        KeywordChipsGroup(
                                            label = "Check-In Trigger Keywords:",
                                            keywordsString = customCheckInKeywords,
                                            onKeywordsChanged = { customCheckInKeywords = it }
                                        )

                                        KeywordChipsGroup(
                                            label = "Check-Out Trigger Keywords:",
                                            keywordsString = customCheckOutKeywords,
                                            onKeywordsChanged = { customCheckOutKeywords = it }
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
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        shape = fieldShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.tertiary,
                                            contentColor = MaterialTheme.colorScheme.onTertiary
                                        )
                                    ) {
                                        Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Test In-App Auto Portal Now", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    SettingsCategory.RELIABILITY -> {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "SYSTEM HEALTH & PERMISSIONS",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.8.sp
                                )

                                StatusPermissionRow(
                                    icon = if (hasExactAlarmPerm) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                                    title = "Exact Alarm Execution",
                                    subtitle = if (hasExactAlarmPerm) "Granted • Guaranteed precise timing" else "Restricted • Tap to enable exact alarm permission",
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

                                StatusPermissionRow(
                                    icon = if (isBatteryIgnored) Icons.Outlined.BatteryFull else Icons.Outlined.BatterySaver,
                                    title = "Unrestricted Battery Mode",
                                    subtitle = if (isBatteryIgnored) "Unrestricted • Immune to OS killer" else "Optimized • Tap to allow unrestricted background execution",
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

                                StatusPermissionRow(
                                    icon = if (hasFullScreenIntentPerm) Icons.Outlined.Fullscreen else Icons.Outlined.Layers,
                                    title = "Full-Screen Alert Display",
                                    subtitle = if (hasFullScreenIntentPerm) "Granted • Alarm pops up full-screen" else "Restricted • Tap to allow full-screen alerts",
                                    isGranted = hasFullScreenIntentPerm,
                                    actionText = "GRANT",
                                    onActionClick = {
                                        try {
                                            val intent = if (!hasFullScreenIntentPerm && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                                android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, android.net.Uri.parse("package:${context.packageName}"))
                                            } else {
                                                android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}"))
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Open Settings -> Permissions to grant full screen alerts", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )

                                if (oemGuidance != null) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                                    OutlinedButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            OemBatteryHelper.launchOemSettings(context, oemGuidance)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = fieldShape,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Configure ${oemGuidance.oemName} Battery Settings", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    SettingsCategory.UPDATES -> {
                        // ANIMATED LOGO BANNER CARD
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                val imageLoader = remember(context) {
                                    ImageLoader.Builder(context)
                                        .components {
                                            if (android.os.Build.VERSION.SDK_INT >= 28) {
                                                add(ImageDecoderDecoder.Factory())
                                            } else {
                                                add(GifDecoder.Factory())
                                            }
                                        }
                                        .build()
                                }

                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                                        .border(
                                            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                            RoundedCornerShape(16.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = R.drawable.welcome_pixel_art,
                                        imageLoader = imageLoader,
                                        contentDescription = "PingPin Animated Logo",
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "OFFICIAL BRANDING",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "PingPin Assistant",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Work-life balance, simplified. Zero-touch office attendance tracking.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }

                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "APP UPDATES & RELEASES",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.8.sp
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "PingPin v$currentAppVersion",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Build: Production (Android)",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Surface(
                                        onClick = { showAppChangelogDialog = true },
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "CHANGELOG",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Icon(
                                                imageVector = Icons.Outlined.ChevronRight,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        appUpdateViewModel.checkForUpdates(isAutoCheck = false)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = fieldShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Check for Updates Now", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // ABOUT CREATOR & ATTRIBUTIONS CARD
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "CREATOR & ATTRIBUTIONS",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.8.sp
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.PersonPin,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = "Prasenjeet Urunkar",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "PingPin Architect & Chief Coffee Ingestor ☕",
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            try { uriHandler.openUri("https://uprasenjeet.vercel.app") } catch (_: Exception) {}
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Outlined.Language, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                                            Text("Website", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            try { uriHandler.openUri("https://github.com/urunkarpm") } catch (_: Exception) {}
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Outlined.Code, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                                            Text("GitHub", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                                // Colorful Logos for Antigravity & GitHub
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                                ) {
                                    // Antigravity (AGY) Colorful Brand Badge
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                        border = BorderStroke(
                                            width = 1.2.dp,
                                            brush = Brush.horizontalGradient(
                                                listOf(Color(0xFF4285F4), Color(0xFFEA4335), Color(0xFFFBBC05), Color(0xFF34A853))
                                            )
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_antigravity),
                                                contentDescription = "Antigravity Logo",
                                                tint = Color(0xFF00E5FF),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Antigravity (AGY)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF0284C7)
                                            )
                                        }
                                    }

                                    // GitHub Colorful Brand Badge
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isDarkTheme) Color(0xFF181825) else Color(0xFFF5F3FF),
                                        border = BorderStroke(1.2.dp, Color(0xFF8957E5))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_github),
                                                contentDescription = "GitHub Logo",
                                                tint = if (isDarkTheme) Color.White else Color(0xFF181717),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "GitHub",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isDarkTheme) Color(0xFFC084FC) else Color(0xFF7E22CE)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Cinematic Emerged Category Navigation Dock (Anchored at Bottom above Bottom Nav)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 14.dp, end = 14.dp, bottom = 68.dp)
                .graphicsLayer {
                    scaleX = dockScale
                    scaleY = dockScale
                    alpha = dockAlpha
                    translationY = dockTranslationY.toPx()
                    transformOrigin = TransformOrigin(0.85f, 1.0f) // Originating from Settings button on bottom nav!
                },
            shape = RoundedCornerShape(26.dp),
            color = if (isDarkTheme) Color(0xD9141923) else Color(0xF5FFFFFF),
            border = BorderStroke(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                    )
                )
            ),
            shadowElevation = if (isDarkTheme) 14.dp else 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val categories = remember { SettingsCategory.values() }

                categories.forEach { category ->
                    val isSelected = selectedCategory == category
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                            .clickable {
                                if (!isSelected) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedCategory = category
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = category.icon,
                                contentDescription = category.tabLabel,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = category.tabLabel,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        if (showAppChangelogDialog) {
            AppChangelogDialog(
                currentAppVersion = currentAppVersion,
                onDismiss = { showAppChangelogDialog = false }
            )
        }

        // Auto-save effect: debounced 600ms after any field change
        LaunchedEffect(
            fullName, ssid, checkInTime, checkOutTime, portalUrl,
            workingDaysMask, wfoDaysMask, portalMode, autoLoginEnabled,
            autoCheckInEnabled, customCheckInKeywords, customCheckOutKeywords
        ) {
            val cfg = configState
            val prof = profileState
            if (cfg != null && prof != null) {
                val isChanged = fullName.trim() != prof.fullName ||
                        ssid.trim() != cfg.ssid ||
                        checkInTime.trim() != cfg.checkInTime ||
                        checkOutTime.trim() != cfg.checkOutTime ||
                        portalUrl.trim() != cfg.portalUrl ||
                        workingDaysMask != cfg.workingDaysMask ||
                        wfoDaysMask != cfg.wfoDaysMask ||
                        portalMode != cfg.portalMode ||
                        autoLoginEnabled != cfg.autoLoginEnabled ||
                        autoCheckInEnabled != cfg.autoCheckInEnabled ||
                        customCheckInKeywords.trim() != cfg.customCheckInKeywords ||
                        customCheckOutKeywords.trim() != cfg.customCheckOutKeywords
                if (!isChanged) return@LaunchedEffect
            }
            kotlinx.coroutines.delay(600L)
            viewModel.saveConfigAndProfile(
                fullName = fullName,
                ssid = ssid,
                checkInTime = checkInTime,
                checkOutTime = checkOutTime,
                portalUrl = portalUrl,
                workingDaysMask = workingDaysMask,
                wfoDaysMask = wfoDaysMask,
                portalMode = portalMode,
                autoLoginEnabled = autoLoginEnabled,
                autoCheckInEnabled = autoCheckInEnabled,
                customCheckInKeywords = customCheckInKeywords,
                customCheckOutKeywords = customCheckOutKeywords
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
                        val newList = keywordsList.filter { it != kw }
                        onKeywordsChanged(newList.joinToString(", "))
                    },
                    label = { Text(kw, fontSize = 11.sp) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Remove",
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = InputChipDefaults.inputChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                )
            }

            SuggestionChip(
                onClick = { showAddDialog = true },
                label = { Text("+ Add Tag", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(14.dp)) }
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
    alwaysShowAction: Boolean = false,
    onActionClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isGranted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
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
                    tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
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
            if (actionText.isNotBlank() && (!isGranted || alwaysShowAction)) {
                TextButton(
                    onClick = onActionClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = actionText,
                        fontWeight = FontWeight.Bold,
                        color = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
