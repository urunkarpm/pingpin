package com.urunkarpm.pingpin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urunkarpm.pingpin.data.model.HolidayCategory
import com.urunkarpm.pingpin.data.model.IndianHoliday
import com.urunkarpm.pingpin.data.model.UpcomingHolidayData
import com.urunkarpm.pingpin.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpcomingHolidaysCard(
    upcomingHolidays: List<UpcomingHolidayData>,
    allHolidays: List<IndianHoliday>,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    var showFullCalendarSheet by remember { mutableStateOf(false) }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            showFullCalendarSheet = true
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = AmberOrange.copy(alpha = if (isDark) 0.2f else 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventAvailable,
                            contentDescription = null,
                            tint = AmberOrange,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Holiday Radar",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Upcoming in next 3 weeks",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Count Badge Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (upcomingHolidays.isNotEmpty()) EmeraldGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (upcomingHolidays.isNotEmpty()) EmeraldGreen.copy(alpha = 0.4f) else Color.Transparent
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (upcomingHolidays.isNotEmpty()) "${upcomingHolidays.size} Upcoming" else "No Holidays",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (upcomingHolidays.isNotEmpty()) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Content: Upcoming Holidays List
            if (upcomingHolidays.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No official holidays scheduled in the next 3 weeks.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    upcomingHolidays.take(3).forEach { data ->
                        UpcomingHolidayItemRow(data = data, isDark = isDark)
                    }

                    if (upcomingHolidays.size > 3) {
                        Text(
                            text = "+ ${upcomingHolidays.size - 3} more holidays in next 3 weeks",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer / Tap instruction
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "View 2026 Full Radar & Long Weekends",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }

    // Full 2026 Indian Holiday Calendar Bottom Sheet
    if (showFullCalendarSheet) {
        FullHolidayCalendarBottomSheet(
            allHolidays = allHolidays,
            onDismiss = { showFullCalendarSheet = false }
        )
    }
}

@Composable
private fun UpcomingHolidayItemRow(
    data: UpcomingHolidayData,
    isDark: Boolean
) {
    val holiday = data.holiday
    val monthDayStr = remember(holiday.dateYyyyMmDd) {
        try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(holiday.dateYyyyMmDd)
            val month = SimpleDateFormat("MMM", Locale.US).format(date ?: Date()).uppercase()
            val day = SimpleDateFormat("dd", Locale.US).format(date ?: Date())
            Pair(month, day)
        } catch (e: Exception) {
            Pair("AUG", "15")
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) PitchSurfaceContainerHighest else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (holiday.isLongWeekend) AmberOrange.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date Badge Chip
            Box(
                modifier = Modifier
                    .size(width = 46.dp, height = 48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (holiday.isLongWeekend) AmberOrange.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = monthDayStr.first,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (holiday.isLongWeekend) AmberOrange else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = monthDayStr.second,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = holiday.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = holiday.dayOfWeek,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "•",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Category badge
                    Text(
                        text = holiday.category.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = holiday.category.badgeColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Right side: Countdown tag & Long weekend tag
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (data.daysRemaining) {
                        0, 1 -> CrimsonRed.copy(alpha = 0.15f)
                        else -> ElectricBlue.copy(alpha = 0.12f)
                    }
                ) {
                    Text(
                        text = data.relativeTag,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (data.daysRemaining) {
                            0, 1 -> CrimsonRed
                            else -> ElectricBlue
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                if (holiday.isLongWeekend) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Long Weekend",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberOrange
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullHolidayCalendarBottomSheet(
    allHolidays: List<IndianHoliday>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val todayYyyyMmDd = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
    var showUpcomingOnly by remember { mutableStateOf(true) }
    var selectedCategoryFilter by remember { mutableStateOf<HolidayCategory?>(null) }
    var onlyLongWeekendsFilter by remember { mutableStateOf(false) }

    val upcomingCount = remember(allHolidays, todayYyyyMmDd) {
        allHolidays.count { it.dateYyyyMmDd >= todayYyyyMmDd }
    }

    val filteredHolidays = remember(allHolidays, selectedCategoryFilter, onlyLongWeekendsFilter, showUpcomingOnly, todayYyyyMmDd) {
        allHolidays.filter { holiday ->
            val matchesDate = !showUpcomingOnly || holiday.dateYyyyMmDd >= todayYyyyMmDd
            val matchesCategory = selectedCategoryFilter == null || holiday.category == selectedCategoryFilter
            val matchesLongWeekend = !onlyLongWeekendsFilter || holiday.isLongWeekend
            matchesDate && matchesCategory && matchesLongWeekend
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 18.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = AmberOrange.copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = AmberOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Holiday Radar 2026",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (showUpcomingOnly) "Showing holidays from present day onwards" else "Official Gazetted, National & Regional Holidays",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = showUpcomingOnly,
                        onClick = { showUpcomingOnly = true },
                        label = { Text("Upcoming ($upcomingCount)", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldGreen.copy(alpha = 0.2f),
                            selectedLabelColor = EmeraldGreen
                        )
                    )
                }

                item {
                    FilterChip(
                        selected = !showUpcomingOnly,
                        onClick = { showUpcomingOnly = false },
                        label = { Text("All 2026 (${allHolidays.size})", fontSize = 12.sp) }
                    )
                }

                item {
                    FilterChip(
                        selected = onlyLongWeekendsFilter,
                        onClick = {
                            onlyLongWeekendsFilter = !onlyLongWeekendsFilter
                        },
                        label = {
                            val longWeekendCount = allHolidays.count {
                                (if (showUpcomingOnly) it.dateYyyyMmDd >= todayYyyyMmDd else true) && it.isLongWeekend
                            }
                            Text("Long Weekends ($longWeekendCount)", fontSize = 12.sp)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberOrange.copy(alpha = 0.2f),
                            selectedLabelColor = AmberOrange
                        )
                    )
                }

                items(HolidayCategory.values()) { category ->
                    FilterChip(
                        selected = selectedCategoryFilter == category,
                        onClick = {
                            selectedCategoryFilter = if (selectedCategoryFilter == category) null else category
                        },
                        label = { Text(category.label, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Holidays List
            if (filteredHolidays.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (showUpcomingOnly) "No remaining upcoming holidays for 2026." else "No holidays found matching filters.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (showUpcomingOnly) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { showUpcomingOnly = false }) {
                                Text("View All 2026 Holidays", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredHolidays) { holiday ->
                        FullHolidayCardItem(holiday = holiday, todayYyyyMmDd = todayYyyyMmDd)
                    }
                }
            }
        }
    }
}

@Composable
private fun FullHolidayCardItem(
    holiday: IndianHoliday,
    todayYyyyMmDd: String
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val daysRemaining = remember(holiday.dateYyyyMmDd, todayYyyyMmDd) {
        parseDaysDifference(todayYyyyMmDd, holiday.dateYyyyMmDd)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) PitchSurfaceContainerHighest else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (holiday.isLongWeekend) AmberOrange.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = holiday.category.badgeBgColorLight,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = holiday.category.badgeColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = holiday.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${holiday.dateYyyyMmDd} (${holiday.dayOfWeek})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = holiday.category.badgeBgColorLight
                    ) {
                        Text(
                            text = holiday.category.label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = holiday.category.badgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when {
                            daysRemaining < 0 -> MaterialTheme.colorScheme.surfaceVariant
                            daysRemaining in 0..1 -> CrimsonRed.copy(alpha = 0.15f)
                            else -> ElectricBlue.copy(alpha = 0.12f)
                        }
                    ) {
                        Text(
                            text = when {
                                daysRemaining == 0 -> "Today!"
                                daysRemaining == 1 -> "Tomorrow!"
                                daysRemaining > 1 -> "In $daysRemaining days"
                                else -> "Past"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                daysRemaining < 0 -> MaterialTheme.colorScheme.onSurfaceVariant
                                daysRemaining in 0..1 -> CrimsonRed
                                else -> ElectricBlue
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = holiday.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
            )

            if (holiday.isLongWeekend) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AmberOrange.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Long Weekend Opportunity - Plan your leaves / WFO schedule!",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberOrange
                    )
                }
            }
        }
    }
}

private fun parseDaysDifference(fromDateStr: String, toDateStr: String): Int {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val fromDate = sdf.parse(fromDateStr) ?: return 0
        val toDate = sdf.parse(toDateStr) ?: return 0
        ((toDate.time - fromDate.time) / (1000 * 60 * 60 * 24)).toInt()
    } catch (_: Exception) { 0 }
}

