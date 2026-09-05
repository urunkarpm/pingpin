package com.urunkarpm.pingpin.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urunkarpm.pingpin.data.local.entity.AttendanceRecordEntity
import com.urunkarpm.pingpin.service.AppInstallManager
import com.urunkarpm.pingpin.service.WorkingDays
import com.urunkarpm.pingpin.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpandableWeeklyCalendarCard(
    records: List<AttendanceRecordEntity>,
    workingDaysMask: Int,
    wfoDaysMask: Int = 31,
    acceptedMakeupDates: Set<String> = emptySet(),
    onDayClick: ((dayNum: Int, dateYyyyMmDd: String) -> Unit)? = null,
    onDayLongClick: ((dayNum: Int, dateYyyyMmDd: String) -> Unit)? = null,
    isExpanded: Boolean = false,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val installCal = remember(context) { AppInstallManager.getInstallDateCalendar(context) }

    var internalIsExpanded by remember { mutableStateOf(false) }
    val currentIsExpanded = if (onExpandedChange != null) isExpanded else internalIsExpanded
    val setExpanded: (Boolean) -> Unit = { newValue ->
        if (onExpandedChange != null) {
            onExpandedChange(newValue)
        } else {
            internalIsExpanded = newValue
        }
    }
    var weekOffset by remember { mutableIntStateOf(0) }

    var selectedYear by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) }

    val todayCal = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    val currentWeekStartCal = remember(weekOffset) {
        Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.WEEK_OF_YEAR, weekOffset)
        }
    }

    val weekEndCal = remember(currentWeekStartCal) {
        (currentWeekStartCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 6)
        }
    }

    val weekRangeTitle = remember(currentWeekStartCal, weekEndCal) {
        val startMonth = SimpleDateFormat("MMM", Locale.US).format(currentWeekStartCal.time)
        val endMonth = SimpleDateFormat("MMM", Locale.US).format(weekEndCal.time)
        val startDay = currentWeekStartCal.get(Calendar.DAY_OF_MONTH)
        val endDay = weekEndCal.get(Calendar.DAY_OF_MONTH)

        if (startMonth == endMonth) {
            "$startMonth $startDay - $endDay"
        } else {
            "$startMonth $startDay - $endMonth $endDay"
        }
    }

    val weekDays = remember(records, workingDaysMask, wfoDaysMask, acceptedMakeupDates, currentWeekStartCal) {
        val list = mutableListOf<WeekDayData>()
        val tempCal = currentWeekStartCal.clone() as Calendar
        val attendedSet = records.map { it.dateYyyyMmDd }.toSet()

        for (i in 0..6) {
            val dateStr = String.format(
                Locale.US,
                "%04d-%02d-%02d",
                tempCal.get(Calendar.YEAR),
                tempCal.get(Calendar.MONTH) + 1,
                tempCal.get(Calendar.DAY_OF_MONTH)
            )
            val dayName = when (tempCal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "Mon"
                Calendar.TUESDAY -> "Tue"
                Calendar.WEDNESDAY -> "Wed"
                Calendar.THURSDAY -> "Thu"
                Calendar.FRIDAY -> "Fri"
                Calendar.SATURDAY -> "Sat"
                else -> "Sun"
            }
            val dayNum = tempCal.get(Calendar.DAY_OF_MONTH)
            val isAttended = attendedSet.contains(dateStr)
            val isBeforeInstall = tempCal.before(installCal) && !isAttended
            val isToday = tempCal.timeInMillis == todayCal.timeInMillis
            val isFuture = tempCal.after(todayCal)
            val isWorking = WorkingDays.isWorkingDay(tempCal, workingDaysMask)
            val isMakeupWfo = acceptedMakeupDates.contains(dateStr)
            val isWfo = WorkingDays.isWfoDay(tempCal, wfoDaysMask) || isMakeupWfo

            list.add(
                WeekDayData(
                    dayName = dayName,
                    dayNum = dayNum,
                    dateStr = dateStr,
                    isToday = isToday,
                    isFuture = isFuture,
                    isBeforeInstall = isBeforeInstall,
                    isWorking = isWorking,
                    isWfo = isWfo,
                    isAttended = isAttended,
                    isMakeupWfo = isMakeupWfo
                )
            )
            tempCal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val weeklyAttendedCount = remember(weekDays) {
        weekDays.count { it.isAttended }
    }
    val weeklyTargetWfoCount = remember(weekDays) {
        weekDays.count { it.isWorking && it.isWfo && !it.isBeforeInstall }
    }

    // Gesture arrow bobbing animation for Week View (up)
    val infiniteTransition = rememberInfiniteTransition(label = "swipe_hint")
    val arrowOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -4f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrow_offset"
    )

    // Gesture arrow bobbing animation for Month View (down)
    val downArrowOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "down_arrow_offset"
    )

    var totalDrag by remember { mutableFloatStateOf(0f) }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(currentIsExpanded) {
                detectVerticalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onDragEnd = {
                        if (!currentIsExpanded && totalDrag < -50f) {
                            setExpanded(true)
                        } else if (currentIsExpanded && totalDrag > 50f) {
                            setExpanded(false)
                        }
                        totalDrag = 0f
                    },
                    onDragCancel = { totalDrag = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        totalDrag += dragAmount
                    }
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .semantics {
                            heading()
                            this.role = Role.Button
                            this.stateDescription = if (currentIsExpanded) "Expanded view" else "Collapsed view"
                            this.contentDescription = if (!currentIsExpanded) "Week $weekRangeTitle. Double tap to expand monthly calendar." else "Monthly Calendar. Double tap to collapse."
                        }
                        .clickable(
                            onClickLabel = if (currentIsExpanded) "Collapse calendar" else "Expand calendar"
                        ) { setExpanded(!currentIsExpanded) }
                ) {
                    AnimatedContent(
                        targetState = currentIsExpanded,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(120))
                        },
                        label = "title_transition"
                    ) { expanded ->
                        Text(
                            text = if (!expanded) "WEEK: $weekRangeTitle" else "MONTHLY CALENDAR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ElectricBlue,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedVisibility(
                        visible = !currentIsExpanded,
                        enter = fadeIn(animationSpec = tween(180)) + expandHorizontally(),
                        exit = fadeOut(animationSpec = tween(120)) + shrinkHorizontally()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { weekOffset-- },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Previous Week",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = if (isDark) EmeraldGreenBgDark else EmeraldGreenBgLight,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    EmeraldGreen.copy(alpha = 0.3f)
                                )
                            ) {
                                Text(
                                    text = "$weeklyAttendedCount / $weeklyTargetWfoCount WFO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            IconButton(
                                onClick = { weekOffset++ },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Next Week",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedContent(
                targetState = currentIsExpanded,
                transitionSpec = {
                    val fadeSpec = tween<Float>(durationMillis = 200, easing = FastOutSlowInEasing)
                    val sizeSpec = tween<androidx.compose.ui.unit.IntSize>(durationMillis = 200, easing = FastOutSlowInEasing)
                    (fadeIn(animationSpec = fadeSpec) togetherWith fadeOut(animationSpec = fadeSpec))
                        .using(SizeTransform(clip = true) { _, _ -> sizeSpec })
                },
                label = "calendar_view_transition"
            ) { expanded ->
                if (!expanded) {
                    // WEEK VIEW
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (day in weekDays) {
                                WeeklyDayItem(
                                    data = day,
                                    isDark = isDark,
                                    onDayClick = { onDayClick?.invoke(day.dayNum, day.dateStr) },
                                    onDayLongClick = { onDayLongClick?.invoke(day.dayNum, day.dateStr) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Gesture Hint (Upward bobbing arrow)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { setExpanded(true) },
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Swipe up hint",
                                tint = ElectricBlue,
                                modifier = Modifier
                                    .size(16.dp)
                                    .graphicsLayer { translationY = arrowOffsetY.dp.toPx() }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Swipe up for full month calendar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    // MONTH VIEW
                    Column(modifier = Modifier.fillMaxWidth()) {
                        MonthlyCalendarView(
                            year = selectedYear,
                            month = selectedMonth,
                            records = records,
                            workingDaysMask = workingDaysMask,
                            wfoDaysMask = wfoDaysMask,
                            acceptedMakeupDates = acceptedMakeupDates,
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
                            onMonthClick = {},
                            onDayClick = { dayNum: Int, dateStr: String ->
                                onDayClick?.invoke(dayNum, dateStr)
                            },
                            onDayLongClick = { dayNum: Int, dateStr: String ->
                                onDayLongClick?.invoke(dayNum, dateStr)
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Gesture Hint (Downward bobbing arrow)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { setExpanded(false) },
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Swipe down hint",
                                tint = ElectricBlue,
                                modifier = Modifier
                                    .size(16.dp)
                                    .graphicsLayer { translationY = downArrowOffsetY.dp.toPx() }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Swipe down for week view",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class WeekDayData(
    val dayName: String,
    val dayNum: Int,
    val dateStr: String,
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
private fun WeeklyDayItem(
    data: WeekDayData,
    isDark: Boolean,
    onDayClick: () -> Unit,
    onDayLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val circleBg = when {
        data.isAttended -> if (isDark) EmeraldGreenBgDark else EmeraldGreenBgLight
        data.isBeforeInstall -> Color.Transparent
        !data.isWorking -> Color.Transparent
        data.isMakeupWfo -> if (isDark) AmberOrangeBgDark.copy(alpha = 0.5f) else AmberOrangeBgLight
        !data.isWfo -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.15f)
        data.isFuture || data.isToday -> if (isDark) WfoDayPurpleBgDark.copy(alpha = 0.55f) else WfoDayPurpleBgLight.copy(alpha = 0.85f)
        else -> if (isDark) CrimsonRedBgDark else CrimsonRedBgLight
    }

    val textColor = when {
        data.isAttended -> if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857)
        data.isMakeupWfo && !data.isAttended -> if (isDark) Color(0xFFFDE68A) else Color(0xFFB45309)
        data.isBeforeInstall -> if (isDark) Color.White else Color.Black
        !data.isWorking -> if (isDark) Color.White else Color.Black
        !data.isWfo -> if (isDark) Color.White else Color.Black
        data.isFuture || (data.isToday && data.isWfo) -> if (isDark) Color(0xFFC4B5FD) else Color(0xFF6D28D9)
        data.isToday -> ElectricBlue
        else -> if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
    }

    val statusDotColor = when {
        data.isAttended -> EmeraldGreen
        data.isMakeupWfo && !data.isAttended -> AmberOrange
        data.isBeforeInstall -> Color.Transparent
        !data.isWorking -> Color.Transparent
        !data.isWfo -> Color.Transparent
        data.isFuture || data.isToday -> WfoDayPurple
        else -> CrimsonRed
    }

    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = data.dayName,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            var circleModifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(circleBg)

            if (data.isToday) {
                circleModifier = circleModifier.border(
                    width = 2.5.dp,
                    color = ElectricBlue,
                    shape = CircleShape
                )
            } else if (data.isMakeupWfo && !data.isAttended) {
                circleModifier = circleModifier.border(
                    width = 1.5.dp,
                    color = AmberOrange,
                    shape = CircleShape
                )
            } else if (data.isWfo && data.isWorking && !data.isAttended && data.isFuture && !data.isBeforeInstall) {
                circleModifier = circleModifier.border(
                    width = 1.2.dp,
                    color = WfoDayPurple.copy(alpha = 0.6f),
                    shape = CircleShape
                )
            } else if (data.isWfo && data.isWorking && !data.isAttended && !data.isFuture && !data.isBeforeInstall) {
                circleModifier = circleModifier.border(
                    width = 1.dp,
                    color = CrimsonRed.copy(alpha = 0.5f),
                    shape = CircleShape
                )
            }

    val haptic = LocalHapticFeedback.current

    if (!data.isFuture) {
        circleModifier = circleModifier.combinedClickable(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onDayClick()
            },
            onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onDayLongClick()
            }
        )
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
                        text = "${data.dayNum}",
                        fontSize = 13.sp,
                        fontWeight = if (data.isToday || data.isAttended || data.isMakeupWfo || data.isWfo) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = textColor
                    )

                    if (data.isAttended || (data.isWorking && !data.isBeforeInstall && (data.isMakeupWfo || data.isWfo || (!data.isFuture && !data.isToday)))) {
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
}
