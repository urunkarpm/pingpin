package com.urunkarpm.pingpin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urunkarpm.pingpin.data.local.entity.AttendanceRecordEntity
import com.urunkarpm.pingpin.service.WorkingDays
import com.urunkarpm.pingpin.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import com.urunkarpm.pingpin.service.AppInstallManager
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MonthlyCalendarView(
    year: Int,
    month: Int,
    records: List<AttendanceRecordEntity>,
    workingDaysMask: Int,
    wfoDaysMask: Int = 31,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onMonthClick: () -> Unit,
    onDayClick: ((dayNum: Int, dateYyyyMmDd: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val installCal = remember(context) { AppInstallManager.getInstallDateCalendar(context) }
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, 1)
    }

    val monthTitle = SimpleDateFormat("MMMM yyyy", Locale.US).format(cal.time)
    val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday = 0

    val attendedSet = records.map { it.dateYyyyMmDd }.toSet()

    val todayCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header with circular stepping buttons & Month Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPreviousMonth,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Prev Month",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
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
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next Month",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Day labels header
            Row(modifier = Modifier.fillMaxWidth()) {
                val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
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

            // Days grid with Circle Aesthetics
            val totalSlots = firstDayOfWeek + maxDays
            val rows = (totalSlots + 6) / 7

            for (row in 0 until rows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (col in 0..6) {
                        val dayNum = row * 7 + col - firstDayOfWeek + 1
                        if (dayNum in 1..maxDays) {
                            val dayCal = Calendar.getInstance().apply {
                                set(year, month - 1, dayNum, 0, 0, 0)
                                set(Calendar.MILLISECOND, 0)
                            }

                            val isBeforeInstall = dayCal.before(installCal)
                            val isToday = dayCal.timeInMillis == todayCal.timeInMillis
                            val isWorking = WorkingDays.isWorkingDay(dayCal, workingDaysMask)
                            val isWfo = WorkingDays.isWfoDay(dayCal, wfoDaysMask)
                            val dateStr = String.format(Locale.US, "%04d-%02d-%02d", year, month, dayNum)
                            val isAttended = attendedSet.contains(dateStr)
                            val isFuture = dayCal.after(todayCal)

                            val circleBg = when {
                                isAttended -> if (isDark) EmeraldGreenBgDark else EmeraldGreenBgLight
                                isBeforeInstall -> Color.Transparent
                                !isWorking -> Color.Transparent
                                !isWfo -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.25f)
                                isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                isFuture -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.35f)
                                else -> if (isDark) CrimsonRedBgDark else CrimsonRedBgLight
                            }

                            val textColor = when {
                                isAttended -> if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857)
                                isBeforeInstall -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                !isWorking -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                !isWfo -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                isToday -> MaterialTheme.colorScheme.primary
                                isFuture -> MaterialTheme.colorScheme.onSurfaceVariant
                                else -> if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
                            }

                            val statusDotColor = when {
                                isAttended -> EmeraldGreen
                                isBeforeInstall -> Color.Transparent
                                !isWorking -> Color.Transparent
                                !isWfo -> Color.Transparent
                                isToday -> Color.Transparent
                                isFuture -> Color.Transparent
                                else -> CrimsonRed
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                var circleModifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(circleBg)

                                if (isToday) {
                                    circleModifier = circleModifier.border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                                } else if (isWfo && isWorking && !isAttended && !isFuture && !isBeforeInstall) {
                                    circleModifier = circleModifier.border(
                                        width = 1.dp,
                                        color = CrimsonRed.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                                }

                                circleModifier = circleModifier.clickable {
                                    onDayClick?.invoke(dayNum, dateStr)
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
                                            text = "$dayNum",
                                            fontSize = 13.sp,
                                            fontWeight = if (isToday || isAttended) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            color = textColor
                                        )
                                        
                                        // Status dot under date for active days
                                        if (isWorking && !isBeforeInstall && (isAttended || (!isFuture && !isToday))) {
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
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
            Spacer(modifier = Modifier.height(14.dp))

            // Calendar Legend Bar with Circle aesthetics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(
                    color = if (isDark) EmeraldGreenBgDark else EmeraldGreenBgLight,
                    dotColor = EmeraldGreen,
                    label = "Present"
                )
                LegendItem(
                    color = if (isDark) CrimsonRedBgDark else CrimsonRedBgLight,
                    dotColor = CrimsonRed,
                    label = "Missed"
                )
                LegendItem(
                    color = MaterialTheme.colorScheme.primary,
                    dotColor = MaterialTheme.colorScheme.primary,
                    label = "Today",
                    isBorderOnly = true
                )
                LegendItem(
                    color = Color.Transparent,
                    dotColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    label = "Off-Day"
                )
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

