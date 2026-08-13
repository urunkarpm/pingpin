package com.urunkarpm.pingpin.ui.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urunkarpm.pingpin.data.local.AppDatabase
import com.urunkarpm.pingpin.data.local.entity.AttendanceRecordEntity
import com.urunkarpm.pingpin.data.repository.AttendanceRepository
import com.urunkarpm.pingpin.data.repository.OfficeConfigRepository
import com.urunkarpm.pingpin.service.*
import com.urunkarpm.pingpin.ui.components.GlassCard
import com.urunkarpm.pingpin.ui.components.OfficeOccupancyCard
import com.urunkarpm.pingpin.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val db = remember { AppDatabase.getInstance(context) }
    val officeConfigRepo = remember { OfficeConfigRepository(db.officeConfigDao()) }
    val attendanceRepo = remember { AttendanceRepository(db.attendanceRecordDao()) }

    val configState by officeConfigRepo.configFlow.collectAsState(initial = null)
    val recordsState by attendanceRepo.watchAll().collectAsState(initial = emptyList())

    val wifiService = remember { WifiService(context) }
    val attendanceService = remember { AttendanceService(context, wifiService) }
    val bleScanner = remember { BleLaptopScannerService(context) }

    var currentSsid by remember { mutableStateOf<String?>(null) }
    var isConnectedToOffice by remember { mutableStateOf(false) }
    var isAutoChecking by remember { mutableStateOf(false) }
    var bleResult by remember { mutableStateOf<BleLaptopScanResult?>(null) }
    var isBleScanning by remember { mutableStateOf(false) }

    val todayStr = remember { AttendanceService.getCurrentDateYyyyMmDd() }
    val todayRecord = recordsState.find { it.dateYyyyMmDd == todayStr }

    val installCal = remember(context) { com.urunkarpm.pingpin.service.AppInstallManager.getInstallDateCalendar(context) }

    // Calculate Streak
    val currentStreak = remember(recordsState, configState, installCal) {
        calculateCurrentStreak(recordsState, configState?.workingDaysMask ?: 31, installCal)
    }

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
                    Toast.makeText(context, "Attendance Marked Automatically! ✅", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeScreen", "Auto-marking attendance failed", e)
            } finally {
                isAutoChecking = false
            }
        }
    }

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Expressive App Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PingPin",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.8).sp
                )
                Text(
                    text = "Local-Only Wi-Fi Attendance",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

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
                        text = "$currentStreak Day Streak",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)
                    )
                }
            }
        }

        // Active Status Hero Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    color = if (isConnectedToOffice) {
                                        EmeraldGreenBgLight.copy(alpha = if (isDark) 0.2f else 0.8f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainer
                                    },
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isConnectedToOffice) Icons.Default.Wifi else Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = if (isConnectedToOffice) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = if (isConnectedToOffice) "Office Wi-Fi Connected" else "Not Connected to Office",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = (-0.2).sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (currentSsid.isNullOrEmpty()) "SSID: None" else "Network: $currentSsid",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Attendance Status Banner or Action Button
                if (todayRecord != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) EmeraldGreenBgDark else EmeraldGreenBgLight)
                            .border(
                                width = 1.dp,
                                color = EmeraldGreen.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Attendance Marked for Today",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857)
                                )
                                val timeFormatted = remember(todayRecord.markedAt) {
                                    java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US).format(java.util.Date(todayRecord.markedAt))
                                }
                                Text(
                                    text = "Recorded at $timeFormatted",
                                    fontSize = 11.sp,
                                    color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF065F46)
                                )
                            }
                        }
                    }
                } else if (isAutoChecking) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Auto-marking attendance...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            val config = configState
                            if (config == null || config.ssid.isEmpty()) {
                                Toast.makeText(context, "Please configure Office Wi-Fi SSID in Settings first.", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            scope.launch {
                                val result = attendanceService.checkAndMarkAttendance(
                                    officeConfig = config,
                                    attendanceRepo = attendanceRepo,
                                    onAttendanceMarked = {
                                        NotificationService(context).showAttendanceSuccessNotification()
                                    }
                                )
                                when (result) {
                                    AttendanceCheckResult.SUCCESS -> Toast.makeText(context, "Attendance Marked Successfully!", Toast.LENGTH_SHORT).show()
                                    AttendanceCheckResult.ALREADY_MARKED -> Toast.makeText(context, "Already marked for today.", Toast.LENGTH_SHORT).show()
                                    AttendanceCheckResult.WIFI_MISMATCH -> Toast.makeText(context, "Not connected to Office Wi-Fi (${config.ssid}).", Toast.LENGTH_LONG).show()
                                    AttendanceCheckResult.NON_WORKING_DAY -> Toast.makeText(context, "Today is not a working day.", Toast.LENGTH_SHORT).show()
                                    else -> Toast.makeText(context, "Error marking attendance.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = if (isConnectedToOffice) "Re-check & Mark Attendance Now" else "Check & Mark Attendance Now",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

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
