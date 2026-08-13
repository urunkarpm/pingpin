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
import androidx.compose.material.icons.filled.Radar
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
import com.urunkarpm.pingpin.data.local.AppDatabase
import com.urunkarpm.pingpin.data.local.entity.AttendanceRecordEntity
import com.urunkarpm.pingpin.data.model.WeatherState
import com.urunkarpm.pingpin.data.repository.AttendanceRepository
import com.urunkarpm.pingpin.data.repository.MakeupWfoRepository
import com.urunkarpm.pingpin.data.repository.OfficeConfigRepository
import com.urunkarpm.pingpin.service.*
import com.urunkarpm.pingpin.ui.components.ExpandableWeeklyCalendarCard
import com.urunkarpm.pingpin.ui.components.GlassCard
import com.urunkarpm.pingpin.ui.components.MakeupWfoCard
import com.urunkarpm.pingpin.ui.components.OfficeOccupancyCard
import com.urunkarpm.pingpin.ui.components.UpcomingHolidaysCard
import com.urunkarpm.pingpin.ui.components.WeatherTravelCard
import com.urunkarpm.pingpin.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val db = remember { AppDatabase.getInstance(context) }
    val officeConfigRepo = remember { OfficeConfigRepository(db.officeConfigDao()) }
    val attendanceRepo = remember { AttendanceRepository(db.attendanceRecordDao()) }
    val makeupRepo = remember { MakeupWfoRepository(db.makeupWfoSuggestionDao()) }

    val configState by officeConfigRepo.configFlow.collectAsState(initial = null)
    val recordsState by attendanceRepo.watchAll().collectAsState(initial = emptyList())
    val activeSuggestionState by makeupRepo.activeSuggestionFlow.collectAsState(initial = null)

    val wifiService = remember { WifiService(context) }
    val attendanceService = remember { AttendanceService(context, wifiService) }
    val bleScanner = remember { BleLaptopScannerService(context) }
    val weatherService = remember { WeatherService(context) }
    val holidayService = remember { IndianHolidayService() }
    val upcomingHolidays = remember { holidayService.getUpcomingHolidays() }
    val allIndianHolidays = remember { holidayService.getAllHolidays() }
    val notifService = remember { NotificationService(context) }
    val makeupManager = remember { MakeupWfoManager(context, makeupRepo, attendanceRepo, wifiService, attendanceService, holidayService) }
    var hasExactAlarmPerm by remember { mutableStateOf(notifService.canScheduleExactAlarms()) }

    var weatherState by remember { mutableStateOf(WeatherState()) }
    var isRefreshingWeather by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            isRefreshingWeather = true
            weatherState = weatherService.fetchWeatherAndTravelInsights()
        } catch (e: Exception) {
            android.util.Log.e("HomeScreen", "Error fetching weather", e)
        } finally {
            isRefreshingWeather = false
        }
    }

    LaunchedEffect(configState, recordsState) {
        val config = configState ?: return@LaunchedEffect
        try {
            makeupManager.evaluateAndSuggestMakeup(
                officeConfig = config,
                onAttendanceAutoMarked = {
                    notifService.showAttendanceSuccessNotification()
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("HomeScreen", "Error evaluating makeup WFO", e)
        }
    }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasExactAlarmPerm = notifService.canScheduleExactAlarms()
                notifService.verifyAndRescheduleAlarmsIfNeeded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var currentSsid by remember { mutableStateOf<String?>(null) }
    var isConnectedToOffice by remember { mutableStateOf(false) }
    var isAutoChecking by remember { mutableStateOf(false) }
    var bleResult by remember { mutableStateOf<BleLaptopScanResult?>(null) }
    var isBleScanning by remember { mutableStateOf(false) }

    val todayStr = remember { AttendanceService.getCurrentDateYyyyMmDd() }
    val todayRecord = recordsState.find { it.dateYyyyMmDd == todayStr }

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

    // Selected Hub Tab (0: Weather & Travel, 1: Holidays, 2: Office Radar)
    var selectedTabIndex by remember { mutableIntStateOf(0) }



    // Dynamic Wi-Fi Status Check Loop
    LaunchedEffect(configState) {
        while (true) {
            currentSsid = wifiService.getWifiSSID()
            val officeSSID = configState?.ssid ?: ""
            isConnectedToOffice = if (officeSSID.isNotEmpty()) {
                wifiService.isConnectedToSSID(officeSSID)
            } else {
                false
            }
            kotlinx.coroutines.delay(2000L)
        }
    }

    // Automatic Attendance Marking Effect when connected to Office Wi-Fi
    LaunchedEffect(isConnectedToOffice, todayRecord, configState) {
        val config = configState ?: return@LaunchedEffect
        if (isConnectedToOffice && todayRecord == null && !isAutoChecking && config.ssid.isNotEmpty()) {
            isAutoChecking = true
            try {
                val result = attendanceService.checkAndMarkAttendance(
                    officeConfig = config,
                    attendanceRepo = attendanceRepo,
                    onAttendanceMarked = {
                        NotificationService(context).showAttendanceSuccessNotification()
                    }
                )
                if (result == AttendanceCheckResult.SUCCESS) {
                    Toast.makeText(context, "Attendance Marked Automatically!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeScreen", "Auto-marking attendance failed", e)
            } finally {
                isAutoChecking = false
            }
        }
    }

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    var selectedDayForDialog by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var longPressDateForNote by remember { mutableStateOf<String?>(null) }
    var noteInputText by remember { mutableStateOf("") }

    Box(modifier = modifier.fillMaxSize()) {
        // Main Scrollable Body Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 140.dp),
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

            // Makeup WFO Suggestion Card (Shows after 2 PM if WFO missed & not on office Wi-Fi)
            activeSuggestionState?.let { suggestion ->
                if (suggestion.status == "PENDING" || suggestion.status == "ACCEPTED") {
                    MakeupWfoCard(
                        suggestion = suggestion,
                        onAccept = {
                            scope.launch {
                                makeupRepo.updateStatus(suggestion.id, "ACCEPTED")
                                notifService.scheduleMakeupAlarm(
                                    targetDateYyyyMmDd = suggestion.suggestedDateYyyyMmDd,
                                    alarmId = suggestion.alarmId,
                                    portalUrl = configState?.portalUrl ?: ""
                                )
                                Toast.makeText(
                                    context,
                                    "7:00 AM Alarm set for ${MakeupWfoManager.formatReadableDate(suggestion.suggestedDateYyyyMmDd)}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        onDecline = {
                            scope.launch {
                                makeupRepo.updateStatus(suggestion.id, "DECLINED")
                                Toast.makeText(context, "Suggestion dismissed", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onCancelAlarm = {
                            scope.launch {
                                notifService.cancelAlarm(suggestion.alarmId)
                                makeupRepo.updateStatus(suggestion.id, "DECLINED")
                                Toast.makeText(context, "Alarm cancelled", Toast.LENGTH_SHORT).show()
                            }
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
                        Triple("Holidays", Icons.Default.BeachAccess, 1),
                        Triple("Radar", Icons.Default.Radar, 2)
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
                                .clickable { selectedTabIndex = index }
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
                        // Weather & Travel Insights Radar Card
                        WeatherTravelCard(
                            weatherState = weatherState,
                            isRefreshing = isRefreshingWeather,
                            onRefresh = {
                                scope.launch {
                                    try {
                                        isRefreshingWeather = true
                                        weatherState = weatherService.fetchWeatherAndTravelInsights()
                                        Toast.makeText(context, "Weather Insights Updated", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Failed to refresh weather", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isRefreshingWeather = false
                                    }
                                }
                            }
                        )
                    }
                    1 -> {
                        // Indian Holiday Radar Card (Next 3 Weeks)
                        UpcomingHolidaysCard(
                            upcomingHolidays = upcomingHolidays,
                            allHolidays = allIndianHolidays
                        )
                    }
                    2 -> {
                        // BLE Office Occupancy Scanner Card
                        OfficeOccupancyCard(
                            scanResult = bleResult,
                            isScanning = isBleScanning,
                            onStartScan = {
                                scope.launch {
                                    try {
                                        isBleScanning = true
                                        bleResult = bleScanner.scanForLaptops(5000L)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, e.message ?: "BLE Scan error", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isBleScanning = false
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // Pinned Bottom Expandable Calendar Card (Week View -> Month View on Swipe Up)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            ExpandableWeeklyCalendarCard(
                records = recordsState,
                workingDaysMask = configState?.workingDaysMask ?: 31,
                wfoDaysMask = configState?.wfoDaysMask ?: 31,
                onDayClick = { dayNum, dateStr ->
                    if (dateStr <= todayStr) {
                        selectedDayForDialog = Pair(dayNum, dateStr)
                    }
                },
                onDayLongClick = { _, dateStr ->
                    longPressDateForNote = dateStr
                    noteInputText = ""
                }
            )
        }
    }




    // Interactive WFO Marking Dialog
    selectedDayForDialog?.let { (_, dateStr) ->
        if (dateStr > todayStr) {
            selectedDayForDialog = null
            return@let
        }
        val isCurrentlyAttended = recordsState.any { it.dateYyyyMmDd == dateStr }

        AlertDialog(
            onDismissRequest = { selectedDayForDialog = null },
            title = {
                Text(
                    text = "WFO Day Marking",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Date: $dateStr",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isCurrentlyAttended) "Status: Currently marked as Present (WFO)" else "Status: Not marked for WFO attendance",
                        fontSize = 13.sp,
                        color = if (isCurrentlyAttended) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Select an option below to update your WFO attendance status for this date.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (dateStr <= todayStr) {
                            scope.launch {
                                attendanceRepo.insertRecord(
                                    dateYyyyMmDd = dateStr,
                                    status = "present"
                                )
                                Toast.makeText(context, "Marked WFO for $dateStr", Toast.LENGTH_SHORT).show()
                                selectedDayForDialog = null
                            }
                        }
                    }
                ) {
                    Text("MARK PRESENT (WFO)", fontWeight = FontWeight.Bold, color = EmeraldGreen)
                }
            },
            dismissButton = {
                Row {
                    if (isCurrentlyAttended) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    attendanceRepo.deleteByDate(dateStr)
                                    Toast.makeText(context, "Cleared attendance for $dateStr", Toast.LENGTH_SHORT).show()
                                    selectedDayForDialog = null
                                }
                            }
                        ) {
                            Text("MARK WFH / OFF", color = CrimsonRed)
                        }
                    }
                    TextButton(onClick = { selectedDayForDialog = null }) {
                        Text("CANCEL")
                    }
                }
            }
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
