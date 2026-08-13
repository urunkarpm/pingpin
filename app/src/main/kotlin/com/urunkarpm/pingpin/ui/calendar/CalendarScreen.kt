package com.urunkarpm.pingpin.ui.calendar

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.urunkarpm.pingpin.data.local.AppDatabase
import com.urunkarpm.pingpin.data.local.entity.UserProfileEntity
import com.urunkarpm.pingpin.data.repository.AttendanceRepository
import com.urunkarpm.pingpin.data.repository.OfficeConfigRepository
import com.urunkarpm.pingpin.data.repository.UserProfileRepository
import com.urunkarpm.pingpin.service.PdfExportService
import com.urunkarpm.pingpin.service.WorkingDays
import com.urunkarpm.pingpin.ui.components.GlassCard
import com.urunkarpm.pingpin.ui.components.MonthlyCalendarView
import com.urunkarpm.pingpin.ui.theme.CrimsonRed
import com.urunkarpm.pingpin.ui.theme.EmeraldGreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val db = remember { AppDatabase.getInstance(context) }
    val officeConfigRepo = remember { OfficeConfigRepository(db.officeConfigDao()) }
    val attendanceRepo = remember { AttendanceRepository(db.attendanceRecordDao()) }
    val profileRepo = remember { UserProfileRepository(db.userProfileDao()) }

    var selectedYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) }

    val configState by officeConfigRepo.configFlow.collectAsState(initial = null)
    val profileState by profileRepo.profileFlow.collectAsState(initial = null)
    val monthlyRecords by attendanceRepo.watchForMonth(selectedYear, selectedMonth).collectAsState(initial = emptyList())

    val workingDaysMask = configState?.workingDaysMask ?: 31
    val wfoDaysMask = configState?.wfoDaysMask ?: 31

    var selectedDayForDialog by remember { mutableStateOf<Pair<Int, String>?>(null) }

    val cal = remember(selectedYear, selectedMonth) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth - 1)
        }
    }
    val monthTitle = remember(cal) {
        SimpleDateFormat("MMMM yyyy", Locale.US).format(cal.time)
    }

    val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val installCal = remember(context) { com.urunkarpm.pingpin.service.AppInstallManager.getInstallDateCalendar(context) }
    val installDateStr = remember(context) { com.urunkarpm.pingpin.service.AppInstallManager.getInstallDateYyyyMmDd(context) }

    val todayCal = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    val (_, elapsedWorkingDays, targetWfoDays) = remember(selectedYear, selectedMonth, workingDaysMask, wfoDaysMask, todayCal, installCal) {
        var totalWork = 0
        var elapsed = 0
        var totalWfo = 0
        val currentYear = todayCal.get(Calendar.YEAR)
        val currentMonth = todayCal.get(Calendar.MONTH) + 1
        val currentDay = todayCal.get(Calendar.DAY_OF_MONTH)

        for (day in 1..maxDays) {
            val c = Calendar.getInstance().apply {
                set(selectedYear, selectedMonth - 1, day, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (c.before(installCal)) {
                continue
            }
            if (WorkingDays.isWorkingDay(c, workingDaysMask)) {
                totalWork++
                val isPastOrToday = when {
                    selectedYear < currentYear -> true
                    selectedYear > currentYear -> false
                    selectedMonth < currentMonth -> true
                    selectedMonth > currentMonth -> false
                    else -> day <= currentDay
                }
                if (isPastOrToday) {
                    elapsed++
                }
            }
            if (WorkingDays.isWfoDay(c, wfoDaysMask)) {
                totalWfo++
            }
        }
        Triple(totalWork, elapsed, totalWfo)
    }

    val attendedDays = monthlyRecords.count { it.dateYyyyMmDd >= installDateStr }
    val missedDays = (elapsedWorkingDays - attendedDays).coerceAtLeast(0)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column {
            Text(
                text = "Attendance Calendar",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.8).sp
            )
            Text(
                text = "Tap any date to mark WFO / WFH attendance",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Monthly Calendar View
        MonthlyCalendarView(
            year = selectedYear,
            month = selectedMonth,
            records = monthlyRecords,
            workingDaysMask = workingDaysMask,
            wfoDaysMask = wfoDaysMask,
            onPreviousMonth = {
                if (selectedMonth == 1) {
                    selectedMonth = 12
                    selectedYear--
                } else {
                    selectedMonth--
                }
            },
            onNextMonth = {
                if (selectedMonth == 12) {
                    selectedMonth = 1
                    selectedYear++
                } else {
                    selectedMonth++
                }
            },
            onMonthClick = { },
            onDayClick = { dayNum, dateStr ->
                selectedDayForDialog = Pair(dayNum, dateStr)
            }
        )

        // Monthly Summary Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "$monthTitle SUMMARY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.8.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CalendarStatChip(
                        modifier = Modifier.weight(1f),
                        label = "Attended WFO",
                        value = "$attendedDays",
                        color = EmeraldGreen
                    )
                    CalendarStatChip(
                        modifier = Modifier.weight(1f),
                        label = "Target WFO",
                        value = "$targetWfoDays",
                        color = MaterialTheme.colorScheme.primary
                    )
                    CalendarStatChip(
                        modifier = Modifier.weight(1f),
                        label = "Missed",
                        value = "$missedDays",
                        color = CrimsonRed
                    )
                }
            }
        }

        // Export PDF Button
        Button(
            onClick = {
                scope.launch {
                    try {
                        val pdfService = PdfExportService(context)
                        val profile = profileState ?: UserProfileEntity(fullName = "PingPin User", designation = "Team Member")
                        val file = pdfService.generateAttendancePdf(
                            year = selectedYear,
                            month = selectedMonth,
                            profile = profile,
                            records = monthlyRecords,
                            workingDaysMask = workingDaysMask,
                            wfoDaysMask = wfoDaysMask
                        )

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
                text = "Export Monthly PDF Report",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    // Interactive WFO Marking Dialog
    selectedDayForDialog?.let { (_, dateStr) ->
        val isCurrentlyAttended = monthlyRecords.any { it.dateYyyyMmDd == dateStr }

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
                        scope.launch {
                            attendanceRepo.insertRecord(
                                dateYyyyMmDd = dateStr,
                                status = "present"
                            )
                            Toast.makeText(context, "Marked WFO for $dateStr", Toast.LENGTH_SHORT).show()
                            selectedDayForDialog = null
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

@Composable
private fun CalendarStatChip(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(vertical = 12.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
