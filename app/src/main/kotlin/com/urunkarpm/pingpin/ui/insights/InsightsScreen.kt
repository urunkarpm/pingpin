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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
        var extraWfoCount = 0

        val dayCal = Calendar.getInstance()
        for (day in 1..maxDays) {
            val dateStr = String.format(Locale.US, "%04d-%02d-%02d", selectedYear, selectedMonth, day)
            dayCal.set(selectedYear, selectedMonth - 1, day, 0, 0, 0)
            dayCal.set(Calendar.MILLISECOND, 0)
            if (dayCal.before(installCal) && !recordsMap.containsKey(dateStr)) {
                continue
            }

            val isPastOrToday = when {
                selectedYear < currentYear -> true
                selectedYear > currentYear -> false
                selectedMonth < currentMonth -> true
                selectedMonth > currentMonth -> false
                else -> day <= currentDay
            }

            val isWork = WorkingDays.isWorkingDay(dayCal, workingDaysMask)
            val isWfo = isWork && WorkingDays.isWfoDay(dayCal, wfoDaysMask)

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
            } else if (recordsMap.containsKey(dateStr)) {
                extraWfoCount++
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
            extraWfoDays = extraWfoCount,
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
        extraWfoDays,
        missedWfoDays,
        upcomingWfoDays
    ) = metrics

    val attendedTotalDays = monthlyRecords.size

    val wfoCompliancePct = if (wfoTargetDaysElapsed > 0) {
        (attendedTotalDays.toFloat() / wfoTargetDaysElapsed * 100f)
    } else 0f

    val overallAttendancePct = if (workingDaysElapsed > 0) {
        (attendedTotalDays.toFloat() / workingDaysElapsed * 100f).coerceAtMost(100f)
    } else 0f


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
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 120.dp),
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
                                        text = if (extraWfoDays > 0) {
                                            "$attendedTotalDays of $wfoTargetDaysElapsed required WFO days attended ($attendedWfoDays WFO + $extraWfoDays Extra WFO)"
                                        } else {
                                            "$attendedWfoDays of $wfoTargetDaysElapsed required WFO days attended ($wfoTargetDaysTotal target of $workingDaysTotal working days)"
                                        },
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
                            val targetRatio = if (wfoTargetDaysTotal > 0) (attendedTotalDays.toFloat() / wfoTargetDaysTotal).coerceIn(0f, 1f) else 0f
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
                                        text = if (extraWfoDays > 0) "$attendedTotalDays / $wfoTargetDaysTotal Target Days ($extraWfoDays Extra)" else "$attendedWfoDays / $wfoTargetDaysTotal Target Days",
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
                            modifier = Modifier.fillMaxWidth(),
                            title = "WFO Attended",
                            value = "$attendedTotalDays / $wfoTargetDaysTotal",
                            subtitle = if (extraWfoDays > 0) "$attendedWfoDays scheduled + $extraWfoDays Extra WFO" else if (wfoTargetDaysElapsed > 0) "$attendedWfoDays of $wfoTargetDaysElapsed required (${String.format(Locale.US, "%.0f", overallAttendancePct)}% overall)" else "No WFO elapsed",
                            icon = Icons.Default.Business,
                            iconColor = EmeraldGreen
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
                            subtitle = if (extraWfoDays > 0) "$extraWfoDays extra visits attended" else if (upcomingWfoDays > 0) "$upcomingWfoDays upcoming targets" else "Month targets complete",
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
                                        extraWfoDays > 0 -> "Great initiative! You attended office $extraWfoDays extra day(s) beyond your required WFO schedule."
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

                    AttendanceLogSummaryCard(
                        records = monthlyRecords,
                        workingDaysMask = workingDaysMask,
                        wfoDaysMask = wfoDaysMask
                    )

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
                                text = if (extraWfoDays > 0) {
                                    "$attendedTotalDays of $wfoTargetDaysElapsed required WFO days attended ($attendedWfoDays WFO + $extraWfoDays Extra WFO)"
                                } else {
                                    "$attendedWfoDays of $wfoTargetDaysElapsed required WFO days attended ($wfoTargetDaysTotal target of $workingDaysTotal working days)"
                                },
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
                    val targetRatio = if (wfoTargetDaysTotal > 0) (attendedTotalDays.toFloat() / wfoTargetDaysTotal).coerceIn(0f, 1f) else 0f
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
                                text = if (extraWfoDays > 0) "$attendedTotalDays / $wfoTargetDaysTotal Target Days ($extraWfoDays Extra)" else "$attendedWfoDays / $wfoTargetDaysTotal Target Days",
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
                    modifier = Modifier.fillMaxWidth(),
                    title = "WFO Attended",
                    value = "$attendedTotalDays / $wfoTargetDaysTotal",
                    subtitle = if (extraWfoDays > 0) "$attendedWfoDays scheduled + $extraWfoDays Extra WFO" else if (wfoTargetDaysElapsed > 0) "$attendedWfoDays of $wfoTargetDaysElapsed required (${String.format(Locale.US, "%.0f", overallAttendancePct)}% overall)" else "No WFO elapsed",
                    icon = Icons.Default.Business,
                    iconColor = EmeraldGreen
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
                    subtitle = if (extraWfoDays > 0) "$extraWfoDays extra visits attended" else if (upcomingWfoDays > 0) "$upcomingWfoDays upcoming targets" else "Month targets complete",
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
                                extraWfoDays > 0 -> "Great initiative! You attended office $extraWfoDays extra day(s) beyond your required WFO schedule."
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
            AttendanceLogSummaryCard(
                records = monthlyRecords,
                workingDaysMask = workingDaysMask,
                wfoDaysMask = wfoDaysMask
            )

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

private data class WeekdayStat(
    val name: String,
    val isWfoDay: Boolean,
    val attended: Int,
    val missed: Int,
    val upcoming: Int,
    val isExtra: Boolean
) {
    val evaluated: Int get() = attended + missed
    val totalScheduled: Int get() = evaluated + upcoming
    val complianceRate: Float get() = if (evaluated > 0) attended.toFloat() / evaluated else 0f
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

    val todayCal = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    val currentYear = todayCal.get(Calendar.YEAR)
    val currentMonth = todayCal.get(Calendar.MONTH) + 1
    val currentDay = todayCal.get(Calendar.DAY_OF_MONTH)

    val isPastMonth = (year < currentYear) || (year == currentYear && month < currentMonth)
    val isFutureMonth = (year > currentYear) || (year == currentYear && month > currentMonth)

    val weekdayStats = remember(year, month, maxDays, workingDaysMask, wfoDaysMask, recordsMap, installCal) {
        val attended = IntArray(7)
        val missed = IntArray(7)
        val upcoming = IntArray(7)

        val cal = Calendar.getInstance()
        for (day in 1..maxDays) {
            cal.set(year, month - 1, day, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)

            val dateStr = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
            val isAttended = recordsMap.containsKey(dateStr)
            val isAfterOrOnInstall = !cal.before(installCal) || isAttended
            if (!isAfterOrOnInstall) continue

            val isPastOrToday = when {
                isPastMonth -> true
                isFutureMonth -> false
                else -> day <= currentDay
            }

            val isWork = WorkingDays.isWorkingDay(cal, workingDaysMask)
            val isWfo = isWork && WorkingDays.isWfoDay(cal, wfoDaysMask)

            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val idx = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2

            if (idx in 0..6) {
                if (isAttended) {
                    attended[idx]++
                } else if (isWfo) {
                    if (isPastOrToday) {
                        missed[idx]++
                    } else {
                        upcoming[idx]++
                    }
                }
            }
        }

        // ponytail: Static 7-day Monday-Sunday representation; upgrade path: customizable first day of week.
        dayNames.mapIndexed { idx, name ->
            val dayShift = idx
            val isWfoConfigured = (wfoDaysMask and (1 shl dayShift)) != 0 && (workingDaysMask and (1 shl dayShift)) != 0
            val isExtra = !isWfoConfigured && attended[idx] > 0
            WeekdayStat(
                name = name,
                isWfoDay = isWfoConfigured,
                attended = attended[idx],
                missed = missed[idx],
                upcoming = upcoming[idx],
                isExtra = isExtra
            )
        }
    }

    val bestDays = remember(weekdayStats) {
        weekdayStats.filter { it.isWfoDay && it.evaluated > 0 && it.complianceRate >= 1.0f }.map { it.name }
    }
    val missedDays = remember(weekdayStats) {
        weekdayStats.filter { it.isWfoDay && it.missed > 0 }.map { it.name }
    }
    val totalEvaluated = remember(weekdayStats) { weekdayStats.sumOf { it.evaluated } }
    val totalAttended = remember(weekdayStats) { weekdayStats.sumOf { it.attended } }

    val insightText = remember(bestDays, missedDays, totalEvaluated, totalAttended) {
        when {
            totalEvaluated == 0 -> "Upcoming month • No evaluated office days yet."
            missedDays.isEmpty() && bestDays.isNotEmpty() -> "100% attendance on all evaluated weekdays! 🎉"
            bestDays.isNotEmpty() && missedDays.isNotEmpty() -> "Most consistent on ${bestDays.joinToString(", ")} • Missed on ${missedDays.joinToString(", ")}"
            bestDays.isNotEmpty() -> "Consistently attended on ${bestDays.joinToString(", ")}"
            missedDays.isNotEmpty() -> "Missed scheduled days on ${missedDays.joinToString(", ")}"
            else -> "$totalAttended of $totalEvaluated scheduled office days completed."
        }
    }

    val maxSlots = remember(weekdayStats) {
        maxOf(weekdayStats.maxOf { maxOf(it.totalScheduled, it.attended) }, 1)
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
                    text = "$totalAttended / $totalEvaluated Completed",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Behavioral Insight Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = insightText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 7 Weekday Columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekdayStats.forEach { stat ->
                    val topLabel = when {
                        stat.evaluated > 0 -> "${stat.attended}/${stat.evaluated}"
                        stat.isExtra -> "+${stat.attended}"
                        stat.upcoming > 0 -> "${stat.upcoming} left"
                        else -> "—"
                    }
                    val topLabelColor = when {
                        stat.isExtra -> com.urunkarpm.pingpin.ui.theme.ElectricBlue
                        stat.evaluated > 0 && stat.complianceRate >= 1.0f -> EmeraldGreen
                        stat.evaluated > 0 && stat.complianceRate > 0f -> Color(0xFFD97706)
                        stat.evaluated > 0 -> Color(0xFFEF4444)
                        stat.upcoming > 0 -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = topLabel,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = topLabelColor
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Stacked / Segmented Vertical Bar (56dp height)
                        Box(
                            modifier = Modifier
                                .height(56.dp)
                                .width(14.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                if (stat.upcoming > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp * (stat.upcoming.toFloat() / maxSlots))
                                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                                    )
                                }
                                if (stat.missed > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp * (stat.missed.toFloat() / maxSlots))
                                            .background(Color(0xFFF87171))
                                    )
                                }
                                if (stat.attended > 0) {
                                    val attColor = if (stat.isExtra) com.urunkarpm.pingpin.ui.theme.ElectricBlue else EmeraldGreen
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp * (stat.attended.toFloat() / maxSlots))
                                            .background(attColor)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = stat.name,
                            fontSize = 11.sp,
                            fontWeight = if (stat.isWfoDay) FontWeight.Bold else FontWeight.Normal,
                            color = if (stat.isWfoDay) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )

                        Text(
                            text = if (stat.isWfoDay) "WFO" else if (stat.isExtra) "Extra" else "Off",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (stat.isWfoDay) MaterialTheme.colorScheme.primary else if (stat.isExtra) com.urunkarpm.pingpin.ui.theme.ElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mini Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(EmeraldGreen)
                    )
                    Text(
                        text = "Completed",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF87171))
                    )
                    Text(
                        text = "Missed",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                    )
                    Text(
                        text = "Upcoming",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (weekdayStats.any { it.isExtra }) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                            .background(com.urunkarpm.pingpin.ui.theme.ElectricBlue)
                        )
                        Text(
                            text = "Extra",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceLogSummaryCard(
    records: List<AttendanceRecordEntity>,
    workingDaysMask: Int = 31,
    wfoDaysMask: Int = 31
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
                val formattedRecords = remember(sortedRecords, workingDaysMask, wfoDaysMask) {
                    val sdfTime = SimpleDateFormat("hh:mm a", Locale.US)
                    val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val sdfDisplayDate = SimpleDateFormat("EEE, MMM dd", Locale.US)
                    val cal = Calendar.getInstance()
                    sortedRecords.take(5).map { record ->
                        val dateObj = try { sdfInput.parse(record.dateYyyyMmDd) } catch (e: Exception) { null }
                        val dateFormatted = dateObj?.let { sdfDisplayDate.format(it) } ?: record.dateYyyyMmDd
                        val timeStr = sdfTime.format(Date(record.markedAt))
                        val isLate = record.status.equals("late", ignoreCase = true)
                        val isWfo = if (dateObj != null) {
                            cal.time = dateObj
                            WorkingDays.isWorkingDay(cal, workingDaysMask) && WorkingDays.isWfoDay(cal, wfoDaysMask)
                        } else true
                        val isExtraWfo = !isWfo

                        Triple(record, dateFormatted, Triple(timeStr, isLate, isExtraWfo))
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    formattedRecords.forEach { (record, dateFormatted, timeAndStatus) ->
                        val (timeStr, isLate, isExtraWfo) = timeAndStatus

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
                                val isDark = MaterialTheme.colorScheme.background.red < 0.5f
                                val badgeText = when {
                                    isExtraWfo && isLate -> "EXTRA (LATE)"
                                    isExtraWfo -> "EXTRA WFO"
                                    isLate -> "LATE"
                                    else -> "ON TIME"
                                }
                                val badgeBg = when {
                                    isExtraWfo -> if (isDark) com.urunkarpm.pingpin.ui.theme.ElectricBlue.copy(alpha = 0.25f) else com.urunkarpm.pingpin.ui.theme.ElectricBlue.copy(alpha = 0.15f)
                                    isLate -> if (isDark) com.urunkarpm.pingpin.ui.theme.CrimsonRedBgDark else com.urunkarpm.pingpin.ui.theme.CrimsonRedBgLight
                                    else -> if (isDark) com.urunkarpm.pingpin.ui.theme.EmeraldGreenBgDark else com.urunkarpm.pingpin.ui.theme.EmeraldGreenBgLight
                                }
                                val badgeTextColor = when {
                                    isExtraWfo -> com.urunkarpm.pingpin.ui.theme.ElectricBlue
                                    isLate -> com.urunkarpm.pingpin.ui.theme.CrimsonRed
                                    else -> com.urunkarpm.pingpin.ui.theme.EmeraldGreen
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(badgeBg)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = badgeText,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = badgeTextColor
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
    val extraWfoDays: Int,
    val missedWfoDays: Int,
    val upcomingWfoDays: Int
)

