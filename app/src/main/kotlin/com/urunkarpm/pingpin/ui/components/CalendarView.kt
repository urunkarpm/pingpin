package com.urunkarpm.pingpin.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urunkarpm.pingpin.data.local.entity.AttendanceRecordEntity
import com.urunkarpm.pingpin.service.AppInstallManager
import com.urunkarpm.pingpin.service.WorkingDays
import com.urunkarpm.pingpin.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

private data class CellDateInfo(
    val year: Int,
    val month: Int,
    val day: Int,
    val isCurrentMonth: Boolean
)

private data class MonthDayCellData(
    val dayNum: Int,
    val dateStr: String,
    val isCurrentMonthDay: Boolean,
    val isToday: Boolean,
    val isFuture: Boolean,
    val isBeforeInstall: Boolean,
    val isWorking: Boolean,
    val isWfo: Boolean,
    val isAttended: Boolean,
    val isMakeupWfo: Boolean = false
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MonthlyCalendarView(
    year: Int,
    month: Int,
    records: List<AttendanceRecordEntity>,
    workingDaysMask: Int,
    wfoDaysMask: Int = 31,
    acceptedMakeupDates: Set<String> = emptySet(),
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onMonthClick: () -> Unit,
    onDayClick: ((dayNum: Int, dateYyyyMmDd: String) -> Unit)? = null,
    onDayLongClick: ((dayNum: Int, dateYyyyMmDd: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val installCal = remember(context) { AppInstallManager.getInstallDateCalendar(context) }
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    val monthTitle = remember(year, month) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        SimpleDateFormat("MMMM yyyy", Locale.US).format(cal.time)
    }

    val monthCellsData = remember(year, month, records, workingDaysMask, wfoDaysMask, acceptedMakeupDates, installCal) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Mon = 0
        val prevCal = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val prevMaxDays = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val nextCal = (cal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }

        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val attendedSet = records.map { it.dateYyyyMmDd }.toSet()

        val list = ArrayList<MonthDayCellData>(42)
        val cellCal = Calendar.getInstance()

        for (slotIndex in 0 until 42) {
            val isPreceding = slotIndex < firstDayOfWeek
            val isFollowing = slotIndex >= (firstDayOfWeek + maxDays)

            val (cYear, cMonth, cDay, isCurrentMonthDay) = when {
                isPreceding -> {
                    val pDay = prevMaxDays - (firstDayOfWeek - 1 - slotIndex)
                    CellDateInfo(prevCal.get(Calendar.YEAR), prevCal.get(Calendar.MONTH) + 1, pDay, false)
                }
                isFollowing -> {
                    val nDay = slotIndex - (firstDayOfWeek + maxDays) + 1
                    CellDateInfo(nextCal.get(Calendar.YEAR), nextCal.get(Calendar.MONTH) + 1, nDay, false)
                }
                else -> {
                    val cDayNum = slotIndex - firstDayOfWeek + 1
                    CellDateInfo(year, month, cDayNum, true)
                }
            }

            val dateStr = String.format(Locale.US, "%04d-%02d-%02d", cYear, cMonth, cDay)
            cellCal.set(cYear, cMonth - 1, cDay, 0, 0, 0)
            cellCal.set(Calendar.MILLISECOND, 0)

            val isAttended = attendedSet.contains(dateStr)
            val isBeforeInstall = cellCal.before(installCal) && !isAttended
            val isToday = cellCal.timeInMillis == todayCal.timeInMillis
            val isWorking = WorkingDays.isWorkingDay(cellCal, workingDaysMask)
            val isMakeupWfo = acceptedMakeupDates.contains(dateStr)
            val isWfo = WorkingDays.isWfoDay(cellCal, wfoDaysMask) || isMakeupWfo
            val isFuture = cellCal.after(todayCal)

            list.add(
                MonthDayCellData(
                    dayNum = cDay,
                    dateStr = dateStr,
                    isCurrentMonthDay = isCurrentMonthDay,
                    isToday = isToday,
                    isFuture = isFuture,
                    isBeforeInstall = isBeforeInstall,
                    isWorking = isWorking,
                    isWfo = isWfo,
                    isAttended = isAttended,
                    isMakeupWfo = isMakeupWfo
                )
            )
        }
        list
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Header with circular stepping buttons & Month Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPreviousMonth,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous Month",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.clickable { onMonthClick() }
            ) {
                Text(
                    text = monthTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            IconButton(
                onClick = onNextMonth,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next Month",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Day labels header
        Row(modifier = Modifier.fillMaxWidth()) {
            val days = remember { listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun") }
            for (day in days) {
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Days grid with 7 columns x 6 rows (42 slots)
        for (row in 0 until 6) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (col in 0..6) {
                    val slotIndex = row * 7 + col
                    val cell = monthCellsData[slotIndex]

                    MonthDayCellItem(
                        cell = cell,
                        isDark = isDark,
                        onDayClick = onDayClick,
                        onDayLongClick = onDayLongClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        Spacer(modifier = Modifier.height(14.dp))

        // Calendar Legend Bar with Circle aesthetics
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    LegendItem(
                        color = if (isDark) EmeraldGreenBgDark else EmeraldGreenBgLight,
                        dotColor = EmeraldGreen,
                        label = "Present"
                    )
                }
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    LegendItem(
                        color = if (isDark) WfoDayPurpleBgDark else WfoDayPurpleBgLight,
                        dotColor = WfoDayPurple,
                        label = "WFO Day"
                    )
                }
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    LegendItem(
                        color = if (isDark) AmberOrangeBgDark else AmberOrangeBgLight,
                        dotColor = AmberOrange,
                        label = "Makeup WFO"
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    LegendItem(
                        color = if (isDark) CrimsonRedBgDark else CrimsonRedBgLight,
                        dotColor = CrimsonRed,
                        label = "Missed"
                    )
                }
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    LegendItem(
                        color = ElectricBlue,
                        dotColor = ElectricBlue,
                        label = "Today",
                        isBorderOnly = true
                    )
                }
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    LegendItem(
                        color = Color.Transparent,
                        dotColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        label = "Off-Day"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MonthDayCellItem(
    cell: MonthDayCellData,
    isDark: Boolean,
    onDayClick: ((dayNum: Int, dateYyyyMmDd: String) -> Unit)?,
    onDayLongClick: ((dayNum: Int, dateYyyyMmDd: String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val circleBg = when {
        !cell.isCurrentMonthDay -> Color.Transparent
        cell.isAttended -> if (isDark) EmeraldGreenBgDark else EmeraldGreenBgLight
        cell.isBeforeInstall -> Color.Transparent
        !cell.isWorking -> Color.Transparent
        cell.isMakeupWfo -> if (isDark) AmberOrangeBgDark.copy(alpha = 0.5f) else AmberOrangeBgLight
        !cell.isWfo -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.15f)
        cell.isFuture || cell.isToday -> if (isDark) WfoDayPurpleBgDark.copy(alpha = 0.55f) else WfoDayPurpleBgLight.copy(alpha = 0.85f)
        else -> if (isDark) CrimsonRedBgDark else CrimsonRedBgLight
    }

    val textColor = when {
        !cell.isCurrentMonthDay -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        cell.isAttended -> if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857)
        cell.isMakeupWfo && !cell.isAttended -> if (isDark) Color(0xFFFDE68A) else Color(0xFFB45309)
        cell.isBeforeInstall -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        !cell.isWorking -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        !cell.isWfo -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        cell.isFuture || (cell.isToday && cell.isWfo) -> if (isDark) Color(0xFFC4B5FD) else Color(0xFF6D28D9)
        cell.isToday -> ElectricBlue
        else -> if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
    }

    val statusDotColor = when {
        !cell.isCurrentMonthDay -> Color.Transparent
        cell.isAttended -> EmeraldGreen
        cell.isMakeupWfo && !cell.isAttended -> AmberOrange
        cell.isBeforeInstall -> Color.Transparent
        !cell.isWorking -> Color.Transparent
        !cell.isWfo -> Color.Transparent
        cell.isFuture || cell.isToday -> WfoDayPurple
        else -> CrimsonRed
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        var circleModifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(circleBg)

        if (cell.isToday) {
            circleModifier = circleModifier.border(
                width = 2.5.dp,
                color = ElectricBlue,
                shape = CircleShape
            )
        } else if (cell.isCurrentMonthDay && cell.isMakeupWfo && !cell.isAttended) {
            circleModifier = circleModifier.border(
                width = 1.5.dp,
                color = AmberOrange,
                shape = CircleShape
            )
        } else if (cell.isCurrentMonthDay && cell.isWfo && cell.isWorking && !cell.isAttended && cell.isFuture && !cell.isBeforeInstall) {
            circleModifier = circleModifier.border(
                width = 1.2.dp,
                color = WfoDayPurple.copy(alpha = 0.6f),
                shape = CircleShape
            )
        } else if (cell.isCurrentMonthDay && cell.isWfo && cell.isWorking && !cell.isAttended && !cell.isFuture && !cell.isBeforeInstall) {
            circleModifier = circleModifier.border(
                width = 1.dp,
                color = CrimsonRed.copy(alpha = 0.5f),
                shape = CircleShape
            )
        }

        val cellAccessibilityText = remember(cell) {
            if (!cell.isCurrentMonthDay) ""
            else {
                val statusText = when {
                    cell.isAttended -> "Present WFO"
                    cell.isMakeupWfo -> "Makeup WFO Scheduled"
                    cell.isWfo && cell.isWorking -> if (cell.isFuture) "Scheduled WFO Day" else "Missed WFO Day"
                    cell.isWorking -> "Off-site or WFH Working Day"
                    else -> "Non-working Day"
                }
                val todayTag = if (cell.isToday) ", Today" else ""
                "Date ${cell.dateStr}: $statusText$todayTag"
            }
        }

        if (!cell.isFuture) {
            circleModifier = circleModifier.combinedClickable(
                onClick = { onDayClick?.invoke(cell.dayNum, cell.dateStr) },
                onLongClick = { onDayLongClick?.invoke(cell.dayNum, cell.dateStr) }
            )
        }

        circleModifier = circleModifier.semantics(mergeDescendants = true) {
            if (cell.isCurrentMonthDay) {
                this.role = Role.Button
                this.contentDescription = cellAccessibilityText
            }
        }

        Box(
            modifier = circleModifier,
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${cell.dayNum}",
                    fontSize = 12.sp,
                    fontWeight = if (cell.isToday || cell.isAttended || cell.isMakeupWfo || cell.isWfo) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = textColor
                )

                if (cell.isCurrentMonthDay && (cell.isAttended || (cell.isWorking && !cell.isBeforeInstall && (cell.isMakeupWfo || cell.isWfo || (!cell.isFuture && !cell.isToday))))) {
                    Spacer(modifier = Modifier.height(1.dp))
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(statusDotColor)
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    dotColor: Color,
    label: String,
    isBorderOnly: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .then(
                    if (isBorderOnly) {
                        Modifier.border(2.dp, color, CircleShape)
                    } else {
                        Modifier.background(color)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!isBorderOnly && dotColor != Color.Transparent) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
