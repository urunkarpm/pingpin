package com.urunkarpm.pingpin.ui.home

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urunkarpm.pingpin.data.local.entity.AttendanceRecordEntity
import com.urunkarpm.pingpin.data.model.WeatherState
import com.urunkarpm.pingpin.service.*
import com.urunkarpm.pingpin.ui.components.ExpandableWeeklyCalendarCard
import com.urunkarpm.pingpin.ui.components.GlassCard
import com.urunkarpm.pingpin.ui.components.MakeupWfoCard
import com.urunkarpm.pingpin.ui.components.UpcomingHolidaysCard
import com.urunkarpm.pingpin.ui.components.WeatherTravelCard
import com.urunkarpm.pingpin.ui.components.weather.WeatherDetailBottomSheet
import com.urunkarpm.pingpin.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current

    val configState by viewModel.configState.collectAsState()
    val recordsState by viewModel.recordsState.collectAsState()
    val activeSuggestionState by viewModel.activeSuggestionState.collectAsState()
    val weatherState by viewModel.weatherState.collectAsState()
    val isRefreshingWeather by viewModel.isRefreshingWeather.collectAsState()

    val acceptedMakeupDates by viewModel.acceptedMakeupDatesState.collectAsState()

    val scanErrorMessage by viewModel.scanErrorMessage.collectAsState()

    LaunchedEffect(scanErrorMessage) {
        scanErrorMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearScanError()
        }
    }

    val upcomingHolidays = viewModel.upcomingHolidays
    val allIndianHolidays = viewModel.allIndianHolidays

    var hasExactAlarmPerm by remember { mutableStateOf(viewModel.notifService.canScheduleExactAlarms()) }
    var showWeatherDetailSheet by remember { mutableStateOf(false) }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasExactAlarmPerm = viewModel.notifService.canScheduleExactAlarms()
                viewModel.notifService.verifyAndRescheduleAlarmsIfNeeded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val todayStr = remember { AttendanceService.getCurrentDateYyyyMmDd() }
    val todayRecord = remember(recordsState, todayStr) { recordsState.find { it.dateYyyyMmDd == todayStr } }

    val isTodayWfo = remember(configState) {
        val cal = Calendar.getInstance()
        val workingMask = configState?.workingDaysMask ?: 31
        val wfoMask = configState?.wfoDaysMask ?: 31
        WorkingDays.isWorkingDay(cal, workingMask) && WorkingDays.isWfoDay(cal, wfoMask)
    }
    val isTodayAttended = todayRecord != null

    val installCal = remember(context) { com.urunkarpm.pingpin.service.AppInstallManager.getInstallDateCalendar(context) }

    // Calculate Streak
    val currentStreak = remember(recordsState, configState, installCal) {
        calculateCurrentStreak(recordsState, configState?.workingDaysMask ?: 31, installCal)
    }

    // Selected Hub Tab (0: Weather & Travel, 1: Holidays)
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var isCalendarExpanded by remember { mutableStateOf(false) }


    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    var selectedDayForDialog by remember { mutableStateOf<Pair<Int, String>?>(null) }

    val windowSizeInfo = com.urunkarpm.pingpin.ui.theme.rememberWindowSizeInfo()
    val isWideOrLandscape = windowSizeInfo.useNavRail || windowSizeInfo.isMediumWidth || windowSizeInfo.isExpandedWidth

    Box(modifier = modifier.fillMaxSize()) {
        if (isWideOrLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Left Column: Header & Feature Hub
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Expressive App Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val pinColor = when {
                            isTodayWfo && isTodayAttended -> EmeraldGreen
                            isTodayWfo && !isTodayAttended -> CrimsonRed
                            else -> MaterialTheme.colorScheme.onSurface
                        }

                        Text(
                            text = buildAnnotatedString {
                                append("Ping")
                                withStyle(style = SpanStyle(color = pinColor)) {
                                    append("Pin")
                                }
                            },
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = (-0.8).sp
                        )

                        // Streak Badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isDark) AmberOrangeBgDark else AmberOrangeBgLight,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                AmberOrange.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = "Streak",
                                    tint = AmberOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$currentStreak Days",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberOrange
                                )
                            }
                        }
                    }

                    // Exact Alarm Permission Alert Banner (Android 12+)
                    if (!hasExactAlarmPerm && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = AmberOrange.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AmberOrange.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Exact Alarm Permission Missing",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberOrange
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Clock alarms may be delayed by Android battery optimization. Tap to grant permission.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Button(
                                    onClick = {
                                        try {
                                            val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                                data = android.net.Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberOrange),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Enable", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }

                    // Makeup WFO Suggestion Card
                    activeSuggestionState?.let { suggestion ->
                        if (suggestion.status == "PENDING") {
                            MakeupWfoCard(
                                suggestion = suggestion,
                                onAccept = {
                                    viewModel.acceptSuggestion(
                                        suggestionId = suggestion.id,
                                        dateStr = suggestion.suggestedDateYyyyMmDd,
                                        alarmId = suggestion.alarmId,
                                        portalUrl = configState?.portalUrl ?: ""
                                    )
                                    Toast.makeText(
                                        context,
                                        "7:00 AM Alarm set for ${MakeupWfoManager.formatReadableDate(suggestion.suggestedDateYyyyMmDd)}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                },
                                onDecline = {
                                    viewModel.declineSuggestion(suggestion.id)
                                    Toast.makeText(context, "Suggestion dismissed", Toast.LENGTH_SHORT).show()
                                },
                                onCancelAlarm = {
                                    viewModel.cancelSuggestionAlarm(suggestion.id, suggestion.alarmId)
                                    Toast.makeText(context, "Alarm cancelled", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }

                    // Segmented Control Hub Header & Glass Tab Bar
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.8f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val tabs = listOf(
                                Triple("Weather", Icons.Default.WbSunny, 0),
                                Triple("Holidays", Icons.Default.BeachAccess, 1)
                            )

                            tabs.forEach { (label, icon, index) ->
                                val isSelected = selectedTabIndex == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                        )
                                        .clickable {
                                            selectedTabIndex = index
                                            if (isCalendarExpanded) {
                                                isCalendarExpanded = false
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
                                            imageVector = icon,
                                            contentDescription = label,
                                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = label,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Animated Hub Content Displaying Selected Feature Card
                    AnimatedContent(
                        targetState = selectedTabIndex,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                        },
                        label = "HubCardTransition"
                    ) { targetIndex ->
                        when (targetIndex) {
                            0 -> {
                                WeatherTravelCard(
                                    weatherState = weatherState,
                                    isRefreshing = isRefreshingWeather,
                                    onRefresh = {
                                        viewModel.refreshWeather()
                                    },
                                    onClick = {
                                        showWeatherDetailSheet = true
                                    }
                                )
                            }
                            else -> {
                                UpcomingHolidaysCard(
                                    upcomingHolidays = upcomingHolidays,
                                    allHolidays = allIndianHolidays
                                )
                            }
                        }
                    }
                }

                // Right Column: Calendar Dashboard Card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ExpandableWeeklyCalendarCard(
                        records = recordsState,
                        workingDaysMask = configState?.workingDaysMask ?: 31,
                        wfoDaysMask = configState?.wfoDaysMask ?: 31,
                        acceptedMakeupDates = acceptedMakeupDates,
                        isExpanded = isCalendarExpanded,
                        onExpandedChange = { isCalendarExpanded = it },
                        onDayClick = { dayNum, dateStr ->
                            if (dateStr <= todayStr) {
                                selectedDayForDialog = Pair(dayNum, dateStr)
                            }
                        }
                    )
                }
            }
        } else {
            // Main Scrollable Body Content for Compact Portrait
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 220.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Expressive App Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pinColor = when {
                        isTodayWfo && isTodayAttended -> EmeraldGreen
                        isTodayWfo && !isTodayAttended -> CrimsonRed
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    Text(
                        text = buildAnnotatedString {
                            append("Ping")
                            withStyle(style = SpanStyle(color = pinColor)) {
                                append("Pin")
                            }
                        },
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-0.8).sp
                    )

                    // Streak Badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isDark) AmberOrangeBgDark else AmberOrangeBgLight,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            AmberOrange.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Streak",
                                tint = AmberOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$currentStreak Days",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberOrange
                            )
                        }
                    }
                }

                // Exact Alarm Permission Alert Banner (Android 12+)
                if (!hasExactAlarmPerm && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = AmberOrange.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmberOrange.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Exact Alarm Permission Missing",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberOrange
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Clock alarms may be delayed by Android battery optimization. Tap to grant permission.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(
                                onClick = {
                                    try {
                                        val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                            data = android.net.Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AmberOrange),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Enable", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                // Makeup WFO Suggestion Card
                activeSuggestionState?.let { suggestion ->
                    if (suggestion.status == "PENDING") {
                        MakeupWfoCard(
                            suggestion = suggestion,
                            onAccept = {
                                viewModel.acceptSuggestion(
                                    suggestionId = suggestion.id,
                                    dateStr = suggestion.suggestedDateYyyyMmDd,
                                    alarmId = suggestion.alarmId,
                                    portalUrl = configState?.portalUrl ?: ""
                                )
                                Toast.makeText(
                                    context,
                                    "7:00 AM Alarm set for ${MakeupWfoManager.formatReadableDate(suggestion.suggestedDateYyyyMmDd)}",
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            onDecline = {
                                viewModel.declineSuggestion(suggestion.id)
                                Toast.makeText(context, "Suggestion dismissed", Toast.LENGTH_SHORT).show()
                            },
                            onCancelAlarm = {
                                viewModel.cancelSuggestionAlarm(suggestion.id, suggestion.alarmId)
                                Toast.makeText(context, "Alarm cancelled", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                // Segmented Control Hub Header & Glass Tab Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.8f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val tabs = listOf(
                            Triple("Weather", Icons.Default.WbSunny, 0),
                            Triple("Holidays", Icons.Default.BeachAccess, 1)
                        )

                        tabs.forEach { (label, icon, index) ->
                            val isSelected = selectedTabIndex == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                    )
                                    .clickable {
                                        selectedTabIndex = index
                                        isCalendarExpanded = false
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = label,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Animated Hub Content Displaying Selected Feature Card
                AnimatedContent(
                    targetState = selectedTabIndex,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                    },
                    label = "HubCardTransition"
                ) { targetIndex ->
                    when (targetIndex) {
                        0 -> {
                            WeatherTravelCard(
                                weatherState = weatherState,
                                isRefreshing = isRefreshingWeather,
                                onRefresh = {
                                    viewModel.refreshWeather()
                                },
                                onClick = {
                                    showWeatherDetailSheet = true
                                }
                            )
                        }
                        else -> {
                            UpcomingHolidaysCard(
                                upcomingHolidays = upcomingHolidays,
                                allHolidays = allIndianHolidays
                            )
                        }
                    }
                }
            }

            // Pinned Bottom Expandable Calendar Card for Compact Portrait
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 100.dp)
            ) {
                ExpandableWeeklyCalendarCard(
                    records = recordsState,
                    workingDaysMask = configState?.workingDaysMask ?: 31,
                    wfoDaysMask = configState?.wfoDaysMask ?: 31,
                    acceptedMakeupDates = acceptedMakeupDates,
                    isExpanded = isCalendarExpanded,
                    onExpandedChange = { isCalendarExpanded = it },
                    onDayClick = { dayNum, dateStr ->
                        if (dateStr <= todayStr) {
                            selectedDayForDialog = Pair(dayNum, dateStr)
                        }
                    }
                )
            }
        }
    }

    // Interactive WFO Marking Dialog
    selectedDayForDialog?.let { (_, dateStr) ->
        if (dateStr > todayStr) {
            selectedDayForDialog = null
            return@let
        }
        val isCurrentlyAttended = recordsState.any { it.dateYyyyMmDd == dateStr }

        val formattedDateStr = remember(dateStr) {
            try {
                val inFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val outFormat = SimpleDateFormat("EEEE, d MMM yyyy", Locale.US)
                val parsed = inFormat.parse(dateStr)
                if (parsed != null) outFormat.format(parsed) else dateStr
            } catch (e: Exception) {
                dateStr
            }
        }

        AlertDialog(
            modifier = Modifier.widthIn(max = 400.dp),
            onDismissRequest = { selectedDayForDialog = null },
            title = {
                Text(
                    text = formattedDateStr,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Surface(
                    shape = CircleShape,
                    color = if (isCurrentlyAttended) (if (isDark) EmeraldGreenBgDark else EmeraldGreenBgLight)
                            else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isCurrentlyAttended) EmeraldGreen.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isCurrentlyAttended) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = if (isCurrentlyAttended) "Present (WFO)" else "WFH / Not Marked",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isCurrentlyAttended) (if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857))
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                if (isCurrentlyAttended) {
                    TextButton(
                        onClick = {
                            viewModel.deleteAttendance(dateStr)
                            Toast.makeText(context, "Marked as WFH for $dateStr", Toast.LENGTH_SHORT).show()
                            selectedDayForDialog = null
                        }
                    ) {
                        Text("MARK AS WFH", fontWeight = FontWeight.Bold, color = CrimsonRed)
                    }
                } else {
                    TextButton(
                        onClick = {
                            if (dateStr <= todayStr) {
                                viewModel.markAttendancePresent(dateStr)
                                Toast.makeText(context, "Marked WFO for $dateStr", Toast.LENGTH_SHORT).show()
                                selectedDayForDialog = null
                            }
                        }
                    ) {
                        Text("MARK PRESENT (WFO)", fontWeight = FontWeight.Bold, color = EmeraldGreen)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedDayForDialog = null }) {
                    Text("CANCEL")
                }
            }
        )
    }

    if (showWeatherDetailSheet) {
        WeatherDetailBottomSheet(
            weatherState = weatherState,
            configState = configState,
            todayRecord = todayRecord,
            isTodayWfo = isTodayWfo,
            onDismissRequest = { showWeatherDetailSheet = false }
        )
    }
}

private fun calculateCurrentStreak(records: List<AttendanceRecordEntity>, workingDaysMask: Int, installCal: Calendar): Int {
    val attendedDates = records.map { it.dateYyyyMmDd }.toSet()
    var streak = 0
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val todayStr = AttendanceService.getCurrentDateYyyyMmDd()
    if (!attendedDates.contains(todayStr)) {
        cal.add(Calendar.DAY_OF_YEAR, -1)
    }

    var safetyLimit = 0
    while (safetyLimit < 60) {
        if (cal.before(installCal)) {
            break
        }
        val dateStr = String.format("%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
        if (!WorkingDays.isWorkingDay(cal, workingDaysMask)) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
            safetyLimit++
            continue
        }
        if (attendedDates.contains(dateStr)) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            break
        }
        safetyLimit++
    }
    return streak
}
