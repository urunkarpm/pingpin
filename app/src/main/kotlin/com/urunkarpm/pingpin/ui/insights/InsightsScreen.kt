package com.urunkarpm.pingpin.ui.insights

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urunkarpm.pingpin.data.local.entity.AttendanceRecordEntity
import com.urunkarpm.pingpin.service.AppInstallManager
import com.urunkarpm.pingpin.service.WorkingDays
import com.urunkarpm.pingpin.ui.components.GlassCard
import com.urunkarpm.pingpin.ui.components.ProgressRadialRing
import com.urunkarpm.pingpin.ui.theme.EmeraldGreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun InsightsScreen(
    modifier: Modifier = Modifier,
    viewModel: InsightsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val selectedYear by viewModel.selectedYear.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()

    val configState by viewModel.configState.collectAsState()
    val monthlyRecords by viewModel.monthlyRecords.collectAsState()

    val workingDaysMask = configState?.workingDaysMask ?: 31
    val wfoDaysMask = configState?.wfoDaysMask ?: 31

    val monthCalendar = remember(selectedYear, selectedMonth) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    val maxDays = monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val monthTitle = remember(monthCalendar) {
        SimpleDateFormat("MMMM yyyy", Locale.US).format(monthCalendar.time)
    }

    val installCal = remember(context) { AppInstallManager.getInstallDateCalendar(context) }
    val installDateStr = remember(context) { AppInstallManager.getInstallDateYyyyMmDd(context) }

    val todayCal = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    // Comprehensive Calculations
    val metrics = remember(selectedYear, selectedMonth, workingDaysMask, wfoDaysMask, monthlyRecords, todayCal, installCal) {
        var wfoTotal = 0
        var wfoElapsed = 0
        var workTotal = 0
        var workElapsed = 0

        val currentYear = todayCal.get(Calendar.YEAR)
        val currentMonth = todayCal.get(Calendar.MONTH) + 1
        val currentDay = todayCal.get(Calendar.DAY_OF_MONTH)

        val recordsMap = monthlyRecords.associateBy { it.dateYyyyMmDd }

        var attendedWfoCount = 0

        for (day in 1..maxDays) {
            val dateStr = String.format(Locale.US, "%04d-%02d-%02d", selectedYear, selectedMonth, day)
            val c = Calendar.getInstance().apply {
                set(selectedYear, selectedMonth - 1, day, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (c.before(installCal) && !recordsMap.containsKey(dateStr)) {
                continue
            }

            val isPastOrToday = when {
                selectedYear < currentYear -> true
                selectedYear > currentYear -> false
                selectedMonth < currentMonth -> true
                selectedMonth > currentMonth -> false
                else -> day <= currentDay
            }

            val isWork = WorkingDays.isWorkingDay(c, workingDaysMask)
            val isWfo = WorkingDays.isWfoDay(c, wfoDaysMask)

            if (isWork) {
                workTotal++
                if (isPastOrToday) workElapsed++
            }

            if (isWfo) {
                wfoTotal++
                if (isPastOrToday) {
                    wfoElapsed++
                }
                if (recordsMap.containsKey(dateStr)) {
                    attendedWfoCount++
                }
            }
        }

        val missed = (wfoElapsed - attendedWfoCount).coerceAtLeast(0)
        val upcoming = (wfoTotal - wfoElapsed).coerceAtLeast(0)

        MonthMetricsData(
            wfoTargetDaysTotal = wfoTotal,
            wfoTargetDaysElapsed = wfoElapsed,
            workingDaysTotal = workTotal,
            workingDaysElapsed = workElapsed,
            attendedWfoDays = attendedWfoCount,
            missedWfoDays = missed,
            upcomingWfoDays = upcoming
        )
    }

    val (
        wfoTargetDaysTotal,
        wfoTargetDaysElapsed,
        workingDaysTotal,
        workingDaysElapsed,
        attendedWfoDays,
        missedWfoDays,
        upcomingWfoDays
    ) = metrics

    val attendedTotalDays = monthlyRecords.size

    val wfoCompliancePct = if (wfoTargetDaysElapsed > 0) {
        (attendedWfoDays.toFloat() / wfoTargetDaysElapsed * 100f).coerceAtMost(100f)
    } else 0f

    val overallAttendancePct = if (workingDaysElapsed > 0) {
        (attendedTotalDays.toFloat() / workingDaysElapsed * 100f).coerceAtMost(100f)
    } else 0f

    val (onTimeCount, lateCount, punctualityPct) = remember(monthlyRecords) {
        var onTime = 0
        var late = 0
        for (r in monthlyRecords) {
            if (r.status.equals("late", ignoreCase = true)) {
                late++
            } else {
                onTime++
            }
        }
        val pct = if (monthlyRecords.isNotEmpty()) (onTime.toFloat() / monthlyRecords.size * 100f) else 100f
        Triple(onTime, late, pct)
    }

    val avgCheckInTimeStr = remember(monthlyRecords) {
        if (monthlyRecords.isEmpty()) {
            "--:--"
        } else {
            val totalMinutes = monthlyRecords.map { record ->
                val cal = Calendar.getInstance().apply { timeInMillis = record.markedAt }
                cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            }.average().toInt()

            val hour = (totalMinutes / 60) % 24
            val min = totalMinutes % 60
            val amPm = if (hour >= 12) "PM" else "AM"
            val displayHour = if (hour % 12 == 0) 12 else hour % 12
            String.format(Locale.US, "%02d:%02d %s", displayHour, min, amPm)
        }
    }

    val isMonthBeforeInstall = remember(monthCalendar, installCal) {
        val monthEnd = Calendar.getInstance().apply {
            set(selectedYear, selectedMonth - 1, maxDays, 23, 59, 59)
        }
        monthEnd.before(installCal)
    }

    val windowSizeInfo = com.urunkarpm.pingpin.ui.theme.rememberWindowSizeInfo()
    val isWideOrLandscape = windowSizeInfo.useNavRail || windowSizeInfo.isMediumWidth || windowSizeInfo.isExpandedWidth

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Monthly Insights",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.8).sp
                )
                Text(
                    text = "WFO Performance & Attendance Analytics",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Month & Year Selector Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 18.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { viewModel.previousMonth() }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous Month",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = monthTitle,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = { viewModel.nextMonth() }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next Month",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (isMonthBeforeInstall) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "PingPin was installed on $installDateStr. Insights monitoring started from that date.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (isWideOrLandscape) {
            // 2-Column Responsive Layout for Wide/Landscape Screens
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Left Column: WFO Compliance & Core Metrics
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // WFO Compliance Hero Gauge Card
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "WFO TARGET COMPLIANCE",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            letterSpacing = 0.8.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", wfoCompliancePct)}%",
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        letterSpacing = (-1.2).sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$attendedWfoDays of $wfoTargetDaysElapsed required WFO days attended ($wfoTargetDaysTotal target of $workingDaysTotal working days)",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                ProgressRadialRing(
                                    percentage = wfoCompliancePct,
                                    size = 88.dp,
                                    color = if (wfoCompliancePct >= 90f) EmeraldGreen else MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Linear Target Progress Bar
                            val targetRatio = if (wfoTargetDaysTotal > 0) (attendedWfoDays.toFloat() / wfoTargetDaysTotal).coerceIn(0f, 1f) else 0f
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Monthly Goal Progress",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$attendedWfoDays / $wfoTargetDaysTotal Target Days",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { targetRatio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = EmeraldGreen,
                                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }

                    // 4-Grid Core Metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            title = "WFO Attended",
                            value = "$attendedWfoDays / $wfoTargetDaysTotal",
                            subtitle = if (wfoTargetDaysElapsed > 0) "$attendedWfoDays of $wfoTargetDaysElapsed required (${String.format(Locale.US, "%.0f", overallAttendancePct)}% overall)" else "No WFO elapsed",
                            icon = Icons.Default.Business,
                            iconColor = EmeraldGreen
                        )

                        MetricCard(
                            modifier = Modifier.weight(1f),
                            title = "Punctuality",
                            value = "${String.format(Locale.US, "%.0f", punctualityPct)}%",
                            subtitle = "$onTimeCount on-time • $lateCount late",
                            icon = Icons.Default.AccessTime,
                            iconColor = if (punctualityPct >= 80f) EmeraldGreen else Color(0xFFF59E0B)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            title = "Avg Check-In",
                            value = avgCheckInTimeStr,
                            subtitle = if (attendedTotalDays > 0) "Across $attendedTotalDays days" else "No check-ins logged",
                            icon = Icons.Default.Schedule,
                            iconColor = MaterialTheme.colorScheme.primary
                        )

                        MetricCard(
                            modifier = Modifier.weight(1f),
                            title = "Missed WFO",
                            value = "$missedWfoDays Days",
                            subtitle = if (upcomingWfoDays > 0) "$upcomingWfoDays upcoming targets" else "Month targets complete",
                            icon = Icons.Default.Warning,
                            iconColor = if (missedWfoDays == 0) EmeraldGreen else Color(0xFFEF4444)
                        )
                    }

                    // Smart Insights Recommendation Banner
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SMART INSIGHT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.6.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = when {
                                        wfoTargetDaysElapsed == 0 && wfoTargetDaysTotal > 0 -> "Upcoming month with $wfoTargetDaysTotal WFO target days scheduled."
                                        wfoCompliancePct >= 100f -> "Outstanding performance! You have met 100% of your required WFO days so far."
                                        wfoCompliancePct >= 75f -> "Good work! You are on track with ${String.format(Locale.US, "%.0f", wfoCompliancePct)}% WFO compliance."
                                        missedWfoDays > 0 -> "Attention: You have $missedWfoDays missed WFO day(s). Make sure to visit office on upcoming WFO days."
                                        else -> "Keep logged in to maintain accurate attendance records!"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Right Column: Weekday Distribution, Attendance Log & PDF Export
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    WeekdayDistributionCard(
                        year = selectedYear,
                        month = selectedMonth,
                        maxDays = maxDays,
                        workingDaysMask = workingDaysMask,
                        wfoDaysMask = wfoDaysMask,
                        records = monthlyRecords,
                        installCal = installCal
                    )

                    AttendanceLogSummaryCard(records = monthlyRecords)

                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val file = viewModel.generatePdfStatement()
                                    val uri: Uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )

                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "application/pdf")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Open Attendance Statement PDF"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "PDF Export error: ${e.message}", Toast.LENGTH_SHORT).show()
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
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Export Monthly PDF Statement",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            // Single Column Stack for Compact Portrait
            // WFO Compliance Hero Gauge Card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "WFO TARGET COMPLIANCE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.8.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${String.format(Locale.US, "%.1f", wfoCompliancePct)}%",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = (-1.2).sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$attendedWfoDays of $wfoTargetDaysElapsed required WFO days attended ($wfoTargetDaysTotal target of $workingDaysTotal working days)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        ProgressRadialRing(
                            percentage = wfoCompliancePct,
                            size = 88.dp,
                            color = if (wfoCompliancePct >= 90f) EmeraldGreen else MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Linear Target Progress Bar
                    val targetRatio = if (wfoTargetDaysTotal > 0) (attendedWfoDays.toFloat() / wfoTargetDaysTotal).coerceIn(0f, 1f) else 0f
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Monthly Goal Progress",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$attendedWfoDays / $wfoTargetDaysTotal Target Days",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { targetRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = EmeraldGreen,
                            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }

            // 4-Grid Core Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "WFO Attended",
                    value = "$attendedWfoDays / $wfoTargetDaysTotal",
                    subtitle = if (wfoTargetDaysElapsed > 0) "$attendedWfoDays of $wfoTargetDaysElapsed required (${String.format(Locale.US, "%.0f", overallAttendancePct)}% overall)" else "No WFO elapsed",
                    icon = Icons.Default.Business,
                    iconColor = EmeraldGreen
                )

                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Punctuality",
                    value = "${String.format(Locale.US, "%.0f", punctualityPct)}%",
                    subtitle = "$onTimeCount on-time • $lateCount late",
                    icon = Icons.Default.AccessTime,
                    iconColor = if (punctualityPct >= 80f) EmeraldGreen else Color(0xFFF59E0B)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Avg Check-In",
                    value = avgCheckInTimeStr,
                    subtitle = if (attendedTotalDays > 0) "Across $attendedTotalDays days" else "No check-ins logged",
                    icon = Icons.Default.Schedule,
                    iconColor = MaterialTheme.colorScheme.primary
                )

                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Missed WFO",
                    value = "$missedWfoDays Days",
                    subtitle = if (upcomingWfoDays > 0) "$upcomingWfoDays upcoming targets" else "Month targets complete",
                    icon = Icons.Default.Warning,
                    iconColor = if (missedWfoDays == 0) EmeraldGreen else Color(0xFFEF4444)
                )
            }

            // Smart Insights Recommendation Banner
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SMART INSIGHT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.6.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when {
                                wfoTargetDaysElapsed == 0 && wfoTargetDaysTotal > 0 -> "Upcoming month with $wfoTargetDaysTotal WFO target days scheduled."
                                wfoCompliancePct >= 100f -> "Outstanding performance! You have met 100% of your required WFO days so far."
                                wfoCompliancePct >= 75f -> "Good work! You are on track with ${String.format(Locale.US, "%.0f", wfoCompliancePct)}% WFO compliance."
                                missedWfoDays > 0 -> "Attention: You have $missedWfoDays missed WFO day(s). Make sure to visit office on upcoming WFO days."
                                else -> "Keep logged in to maintain accurate attendance records!"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Day of Week Distribution Card
            WeekdayDistributionCard(
                year = selectedYear,
                month = selectedMonth,
                maxDays = maxDays,
                workingDaysMask = workingDaysMask,
                wfoDaysMask = wfoDaysMask,
                records = monthlyRecords,
                installCal = installCal
            )

            // Monthly Attendance Log Summary Card
            AttendanceLogSummaryCard(records = monthlyRecords)

            // Export PDF Button
            Button(
                onClick = {
                    scope.launch {
                        try {
                            val file = viewModel.generatePdfStatement()
                            val uri: Uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )

                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(Intent.createChooser(intent, "Open Attendance Statement PDF"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "PDF Export error: ${e.message}", Toast.LENGTH_SHORT).show()
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
                Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Export Monthly PDF Statement",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        cornerRadius = 16.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeekdayDistributionCard(
    year: Int,
    month: Int,
    maxDays: Int,
    workingDaysMask: Int,
    wfoDaysMask: Int,
    records: List<AttendanceRecordEntity>,
    installCal: Calendar
) {
    val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val recordsMap = remember(records) { records.associateBy { it.dateYyyyMmDd } }

    val weekdayStats = remember(year, month, maxDays, workingDaysMask, wfoDaysMask, recordsMap, installCal) {
        val targets = IntArray(7)
        val attended = IntArray(7)

        val cal = Calendar.getInstance()
        for (day in 1..maxDays) {
            cal.set(year, month - 1, day, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            if (cal.before(installCal)) continue

            if (WorkingDays.isWorkingDay(cal, workingDaysMask) && WorkingDays.isWfoDay(cal, wfoDaysMask)) {
                // Calendar.DAY_OF_WEEK: Sun=1, Mon=2, Tue=3...
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                val idx = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2
                if (idx in 0..6) {
                    targets[idx]++
                    val dateStr = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
                    if (recordsMap.containsKey(dateStr)) {
                        attended[idx]++
                    }
                }
            }
        }
        dayNames.mapIndexed { idx, name ->
            Triple(name, attended[idx], targets[idx])
        }
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ATTENDANCE BY WEEKDAY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "WFO Days",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekdayStats.forEach { (dayName, attended, target) ->
                    val ratio = if (target > 0) (attended.toFloat() / target).coerceIn(0f, 1f) else 0f
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (target > 0) "$attended/$target" else "-",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (attended > 0) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .height(50.dp)
                                .width(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(ratio)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (ratio >= 1.0f) EmeraldGreen else MaterialTheme.colorScheme.primary)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = dayName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceLogSummaryCard(
    records: List<AttendanceRecordEntity>
) {
    val sortedRecords = remember(records) {
        records.sortedByDescending { it.dateYyyyMmDd }
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MONTHLY ATTENDANCE LOG",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "${sortedRecords.size} Logged",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreen
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (sortedRecords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No office check-ins recorded for this month.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val displayRecords = sortedRecords.take(5)
                val sdfTime = remember { SimpleDateFormat("hh:mm a", Locale.US) }
                val sdfInput = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
                val sdfDisplayDate = remember { SimpleDateFormat("EEE, MMM dd", Locale.US) }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    displayRecords.forEach { record ->
                        val dateObj = try { sdfInput.parse(record.dateYyyyMmDd) } catch (e: Exception) { null }
                        val dateFormatted = dateObj?.let { sdfDisplayDate.format(it) } ?: record.dateYyyyMmDd
                        val timeStr = sdfTime.format(Date(record.markedAt))
                        val isLate = record.status.equals("late", ignoreCase = true)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = dateFormatted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = record.ssidSnapshot ?: "Manual Check-in",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = timeStr,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isLate) Color(0xFFFEE2E2) else Color(0xFFDCFCE7))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isLate) "LATE" else "ON TIME",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isLate) Color(0xFFB91C1C) else Color(0xFF15803D)
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

private data class MonthMetricsData(
    val wfoTargetDaysTotal: Int,
    val wfoTargetDaysElapsed: Int,
    val workingDaysTotal: Int,
    val workingDaysElapsed: Int,
    val attendedWfoDays: Int,
    val missedWfoDays: Int,
    val upcomingWfoDays: Int
)

