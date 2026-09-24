package com.urunkarpm.pingpin.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
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

enum class LegendFilterType {
    PRESENT,
    WFO_DAY,
    MAKEUP_WFO,
    EXTRA_WFO,
    MISSED,
    TODAY,
    OFF_DAY
}

private data class LegendItemData(
    val label: String,
    val type: LegendFilterType?,
    val count: Int,
    val color: Color,
    val dotColor: Color,
    val isBorderOnly: Boolean = false
)

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
    val isMakeupWfo: Boolean = false,
    val isExtraWfo: Boolean = false
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
    var activeFilter by remember { mutableStateOf<LegendFilterType?>(null) }

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
            val isExtraWfo = isAttended && !isWfo

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
                    isMakeupWfo = isMakeupWfo,
                    isExtraWfo = isExtraWfo
                )
            )
        }
        list
    }

    // Dynamic Monthly Tally Counts for Interactive Legend Badges
    val presentCount = remember(monthCellsData) { monthCellsData.count { it.isCurrentMonthDay && it.isAttended } }
    val wfoCount = remember(monthCellsData) { monthCellsData.count { it.isCurrentMonthDay && it.isWorking && it.isWfo && !it.isBeforeInstall } }
    val makeupCount = remember(monthCellsData) { monthCellsData.count { it.isCurrentMonthDay && it.isMakeupWfo } }
    val extraWfoCount = remember(monthCellsData) { monthCellsData.count { it.isCurrentMonthDay && it.isExtraWfo } }
    val missedCount = remember(monthCellsData) {
        monthCellsData.count { it.isCurrentMonthDay && it.isWorking && it.isWfo && !it.isAttended && !it.isFuture && !it.isToday && !it.isBeforeInstall }
    }
    val todayCount = remember(monthCellsData) { monthCellsData.count { it.isCurrentMonthDay && it.isToday } }
    val offCount = remember(monthCellsData) { monthCellsData.count { it.isCurrentMonthDay && !it.isWorking } }

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
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous Month",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .semantics {
                        heading()
                        role = Role.Button
                        contentDescription = "$monthTitle. Tap to select month and year."
                    }
                    .clickable { onMonthClick() }
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
                    .size(44.dp)
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

        Spacer(modifier = Modifier.height(12.dp))

        // Day labels header
        Row(modifier = Modifier.fillMaxWidth()) {
            val days = remember { listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun") }
            for ((index, day) in days.withIndex()) {
                val isWeekend = index >= 5
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isWeekend) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Days grid with 7 columns x 6 rows (42 slots) with 100% equal horizontal & vertical 4dp gaps
        for (row in 0 until 6) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (col in 0..6) {
                    val slotIndex = row * 7 + col
                    val cell = monthCellsData[slotIndex]

                    val isMatchingFilter = remember(cell, activeFilter) {
                        isCellMatchingFilter(cell, activeFilter)
                    }

                    MonthDayCellItem(
                        cell = cell,
                        isDark = isDark,
                        isMatchingFilter = isMatchingFilter,
                        isFilterActive = activeFilter != null,
                        onDayClick = onDayClick,
                        onDayLongClick = onDayLongClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        Spacer(modifier = Modifier.height(12.dp))

        // ponytail: In-place scroll wheel selector for calendar legends.
        // Ceiling: Single-slot inline scroll wheel cycling through filters. Upgrade path: Multi-track wheel or chip grid if concurrent multi-select filters are needed.
        val offDayDotColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        val legendWheelItems = remember(presentCount, wfoCount, makeupCount, extraWfoCount, missedCount, todayCount, offCount, isDark, offDayDotColor) {
            listOf(
                LegendItemData("All Legends", null, 0, Color.Transparent, ElectricBlue),
                LegendItemData("Present", LegendFilterType.PRESENT, presentCount, if (isDark) EmeraldGreenBgDark else EmeraldGreenBgLight, EmeraldGreen),
                LegendItemData("WFO Day", LegendFilterType.WFO_DAY, wfoCount, if (isDark) WfoDayPurpleBgDark else WfoDayPurpleBgLight, WfoDayPurple),
                LegendItemData("Makeup WFO", LegendFilterType.MAKEUP_WFO, makeupCount, if (isDark) AmberOrangeBgDark else AmberOrangeBgLight, AmberOrange),
                LegendItemData("Extra WFO", LegendFilterType.EXTRA_WFO, extraWfoCount, ElectricBlue.copy(alpha = 0.25f), ElectricBlue),
                LegendItemData("Missed", LegendFilterType.MISSED, missedCount, if (isDark) CrimsonRedBgDark else CrimsonRedBgLight, CrimsonRed),
                LegendItemData("Today", LegendFilterType.TODAY, todayCount, ElectricBlue, ElectricBlue, isBorderOnly = true),
                LegendItemData("Off-Day", LegendFilterType.OFF_DAY, offCount, Color.Transparent, offDayDotColor)
            )
        }

        var wheelIndex by remember { mutableIntStateOf(0) }
        val haptic = LocalHapticFeedback.current

        val currentWheelItem = legendWheelItems[wheelIndex]
        LaunchedEffect(wheelIndex) {
            activeFilter = currentWheelItem.type
        }
        // ponytail: Clean in-place vertical scroll wheel selector without caret buttons or press shadows.
        // Ceiling: Single-slot inline scroll wheel cycling through filters. Upgrade path: Multi-track wheel or chip grid if concurrent multi-select filters are needed.
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(170.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (currentWheelItem.type != null) {
                            currentWheelItem.color.copy(alpha = 0.25f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = currentWheelItem.dotColor,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        wheelIndex = (wheelIndex + 1) % legendWheelItems.size
                    }
                    .pointerInput(Unit) {
                        var totalDrag = 0f
                        detectVerticalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onDragEnd = {
                                if (totalDrag < -20f) {
                                    // Swipe UP -> Scroll next item
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    wheelIndex = (wheelIndex + 1) % legendWheelItems.size
                                } else if (totalDrag > 20f) {
                                    // Swipe DOWN -> Scroll prev item
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    wheelIndex = if (wheelIndex == 0) legendWheelItems.size - 1 else wheelIndex - 1
                                }
                            },
                            onVerticalDrag = { _, dragAmount -> totalDrag += dragAmount }
                        )
                    }
                    .semantics {
                        role = Role.Button
                        contentDescription = "Vertical legend scroll wheel: ${currentWheelItem.label}. Swipe up or down to scroll."
                    }
            ) {
                AnimatedContent(
                    targetState = currentWheelItem,
                    transitionSpec = {
                        (fadeIn(tween(180)) + slideInVertically(tween(200)) { height -> height / 2 }) togetherWith
                        (fadeOut(tween(140)) + slideOutVertically(tween(180)) { height -> -height / 2 })
                    },
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                    label = "legend_vertical_wheel_scroll"
                ) { item ->
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (item.type != null) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (item.isBorderOnly) {
                                            Modifier.border(1.5.dp, item.color, CircleShape)
                                        } else {
                                            Modifier.background(item.color)
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Text(
                            text = if (item.type != null) "${item.label} (${item.count})" else item.label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scroll position indicator dots below
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                legendWheelItems.forEachIndexed { idx, item ->
                    val isSelected = idx == wheelIndex
                    Box(
                        modifier = Modifier
                            .size(width = if (isSelected) 12.dp else 5.dp, height = 5.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) {
                                    item.dotColor
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                                }
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                wheelIndex = idx
                            }
                    )
                }
            }
        }
    }
}

private fun isCellMatchingFilter(cell: MonthDayCellData, filter: LegendFilterType?): Boolean {
    if (filter == null) return true
    if (!cell.isCurrentMonthDay) return false
    return when (filter) {
        LegendFilterType.PRESENT -> cell.isAttended
        LegendFilterType.WFO_DAY -> cell.isWorking && cell.isWfo && !cell.isBeforeInstall
        LegendFilterType.MAKEUP_WFO -> cell.isMakeupWfo
        LegendFilterType.EXTRA_WFO -> cell.isExtraWfo
        LegendFilterType.MISSED -> cell.isWorking && cell.isWfo && !cell.isAttended && !cell.isFuture && !cell.isToday && !cell.isBeforeInstall
        LegendFilterType.TODAY -> cell.isToday
        LegendFilterType.OFF_DAY -> !cell.isWorking
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MonthDayCellItem(
    cell: MonthDayCellData,
    isDark: Boolean,
    isMatchingFilter: Boolean,
    isFilterActive: Boolean,
    onDayClick: ((dayNum: Int, dateYyyyMmDd: String) -> Unit)?,
    onDayLongClick: ((dayNum: Int, dateYyyyMmDd: String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    // ponytail: Direct graphicsLayer alpha assignment for 42 calendar grid cells instead of 42 concurrent animateFloatAsState state objects. Ceiling: static alpha check. Upgrade: GPU instance rendering.
    val baseSquircleBg = when {
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
        cell.isBeforeInstall -> if (isDark) Color.White else Color.Black
        !cell.isWorking -> if (isDark) Color.White else Color.Black
        !cell.isWfo -> if (isDark) Color.White else Color.Black
        cell.isFuture || (cell.isToday && cell.isWfo) -> if (isDark) Color(0xFFC4B5FD) else Color(0xFF6D28D9)
        cell.isToday -> ElectricBlue
        else -> if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
    }

    val statusDotColor = when {
        !cell.isCurrentMonthDay -> Color.Transparent
        cell.isExtraWfo -> ElectricBlue
        cell.isAttended -> EmeraldGreen
        cell.isMakeupWfo && !cell.isAttended -> AmberOrange
        cell.isBeforeInstall -> Color.Transparent
        !cell.isWorking -> Color.Transparent
        !cell.isWfo -> Color.Transparent
        cell.isFuture || cell.isToday -> WfoDayPurple
        else -> CrimsonRed
    }

    val squircleShape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .graphicsLayer {
                alpha = if (isFilterActive && !isMatchingFilter) 0.25f else 1.0f
            },
        contentAlignment = Alignment.Center
    ) {
        var cellModifier = Modifier
            .fillMaxSize()
            .clip(squircleShape)
            .background(baseSquircleBg)

        if (cell.isToday) {
            cellModifier = cellModifier.border(
                width = 2.5.dp,
                color = ElectricBlue,
                shape = squircleShape
            )
        } else if (cell.isCurrentMonthDay && cell.isExtraWfo) {
            // ponytail: Electric Blue accent border marker for non-WFO office attendance (Extra WFO). Ceiling: 2dp squircle border. Upgrade: Dual-color badge.
            cellModifier = cellModifier.border(
                width = 2.dp,
                color = ElectricBlue,
                shape = squircleShape
            )
        } else if (cell.isCurrentMonthDay && cell.isMakeupWfo && !cell.isAttended) {
            cellModifier = cellModifier.border(
                width = 1.5.dp,
                color = AmberOrange,
                shape = squircleShape
            )
        } else if (cell.isCurrentMonthDay && cell.isWfo && cell.isWorking && !cell.isAttended && cell.isFuture && !cell.isBeforeInstall) {
            cellModifier = cellModifier.border(
                width = 1.2.dp,
                color = WfoDayPurple.copy(alpha = 0.6f),
                shape = squircleShape
            )
        } else if (cell.isCurrentMonthDay && cell.isWfo && cell.isWorking && !cell.isAttended && !cell.isFuture && !cell.isBeforeInstall) {
            cellModifier = cellModifier.border(
                width = 1.dp,
                color = CrimsonRed.copy(alpha = 0.5f),
                shape = squircleShape
            )
        }

        if (isFilterActive && isMatchingFilter && cell.isCurrentMonthDay) {
            cellModifier = cellModifier.border(
                width = 1.8.dp,
                color = statusDotColor.copy(alpha = 0.9f),
                shape = squircleShape
            )
        }

        val cellAccessibilityText = remember(cell) {
            if (!cell.isCurrentMonthDay) ""
            else {
                val statusText = when {
                    cell.isExtraWfo -> "Present (Extra WFO Day)"
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
            cellModifier = cellModifier.combinedClickable(
                onClick = { onDayClick?.invoke(cell.dayNum, cell.dateStr) },
                onLongClick = { onDayLongClick?.invoke(cell.dayNum, cell.dateStr) }
            )
        }

        cellModifier = cellModifier.semantics(mergeDescendants = true) {
            if (cell.isCurrentMonthDay) {
                this.role = Role.Button
                this.contentDescription = cellAccessibilityText
            }
        }

        Box(
            modifier = cellModifier,
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

