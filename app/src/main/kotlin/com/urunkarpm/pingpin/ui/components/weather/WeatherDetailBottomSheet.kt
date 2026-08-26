package com.urunkarpm.pingpin.ui.components.weather

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urunkarpm.pingpin.data.local.entity.AttendanceRecordEntity
import com.urunkarpm.pingpin.data.local.entity.OfficeConfigEntity
import com.urunkarpm.pingpin.data.model.CommuteStatus
import com.urunkarpm.pingpin.data.model.HourlyCommuteForecast
import com.urunkarpm.pingpin.data.model.WeatherCondition
import com.urunkarpm.pingpin.data.model.WeatherState

/**
 * Premium "Commute Intelligence" Draggable Bottom Sheet styled with Material 3 design system.
 *
 * Structure:
 * 1. Header: Commute Intelligence + location + risk badge.
 * 2. Primary alert: Heavy rain / rain expected / clear commute conditions.
 * 3. Expected time window (rain forecast window & commute peak windows).
 * 4. User commute section (check-in / check-out schedule & attendance status).
 * 5. Practical recommendations (umbrella, transport mode, weather delay advisory, heat alert).
 * 6. Interactive 24-hour timeline.
 * 7. Action buttons with minimum 48dp touch targets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherDetailBottomSheet(
    weatherState: WeatherState,
    configState: OfficeConfigEntity? = null,
    todayRecord: AttendanceRecordEntity? = null,
    isTodayWfo: Boolean = true,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isTodayAttended = todayRecord != null
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier
    ) {
        if (weatherState.isInitialLoading && !weatherState.hasValidData) {
            WeatherDetailBottomSheetSkeleton()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Top Linear Progress bar during background refresh
                if (weatherState.isRefreshing) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Stale Cached Data Alert Banner
                if (weatherState.isStale) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Offline • Displaying cached weather (${weatherState.staleAgeMinutes} mins ago)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                // 1. HEADER: Commute Intelligence + Location + Risk Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Commute Intelligence",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = weatherState.locationName.ifBlank { "Current Location" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "• ${weatherState.currentTempC}°C ${weatherState.condition.label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Traffic Risk Badge
                    val (riskBgColor, riskTextColor) = when (weatherState.commuteStatus) {
                        CommuteStatus.GOOD -> Pair(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        CommuteStatus.CONSIDER_DELAYING -> Pair(
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        CommuteStatus.DIFFICULT -> Pair(
                            MaterialTheme.colorScheme.errorContainer,
                            MaterialTheme.colorScheme.onErrorContainer
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = riskBgColor,
                        border = BorderStroke(1.dp, riskTextColor.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(riskTextColor, CircleShape)
                            )
                            Text(
                                text = weatherState.trafficRisk,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = riskTextColor
                            )
                        }
                    }
                }

            // 2. PRIMARY ALERT: Heavy Rain / Rain Expected / Clear Commute
            val alertCategory = getRainAlertCategory(weatherState)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = alertCategory.containerColor,
                border = BorderStroke(1.dp, alertCategory.contentColor.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = alertCategory.contentColor.copy(alpha = 0.12f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = alertCategory.icon,
                                contentDescription = null,
                                tint = alertCategory.contentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = alertCategory.title.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = alertCategory.contentColor
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = alertCategory.contentColor.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "${weatherState.rainChancePercent}% Rain",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = alertCategory.contentColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = alertCategory.description,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 3. EXPECTED TIME WINDOW
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Forecast Time Windows",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Expected Rain Window
                    val expectedRainWindow = computeExpectedRainWindow(weatherState.hourlyForecast)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Expected Rain Window",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = expectedRainWindow,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Peak Commute Windows Overview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Commute Hours",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val checkInStr = configState?.checkInTime ?: "09:30"
                        val checkOutStr = configState?.checkOutTime ?: "17:30"
                        Text(
                            text = "Morning ($checkInStr) • Evening ($checkOutStr)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 4. USER COMMUTE SECTION (Check-In / Check-Out & Attendance)
            Text(
                text = "Your Commute Schedule",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Check-in Schedule Card
                val checkInStr = configState?.checkInTime ?: "09:30"
                val checkInForecast = findForecastForHourStr(checkInStr, weatherState.hourlyForecast)
                CommuteScheduleCard(
                    modifier = Modifier.weight(1f),
                    title = "Check-in Target",
                    timeStr = checkInStr,
                    forecast = checkInForecast,
                    icon = Icons.AutoMirrored.Filled.Login
                )

                // Check-out Schedule Card
                val checkOutStr = configState?.checkOutTime ?: "17:30"
                val checkOutForecast = findForecastForHourStr(checkOutStr, weatherState.hourlyForecast)
                CommuteScheduleCard(
                    modifier = Modifier.weight(1f),
                    title = "Check-out Target",
                    timeStr = checkOutStr,
                    forecast = checkOutForecast,
                    icon = Icons.AutoMirrored.Filled.Logout
                )
            }

            // Attendance Status Banner for Today
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = when {
                    isTodayAttended -> MaterialTheme.colorScheme.primaryContainer
                    isTodayWfo -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.surfaceContainer
                },
                border = BorderStroke(
                    1.dp,
                    when {
                        isTodayAttended -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                        isTodayWfo -> MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isTodayAttended) Icons.Default.CheckCircle else Icons.Default.EventAvailable,
                            contentDescription = null,
                            tint = when {
                                isTodayAttended -> MaterialTheme.colorScheme.onPrimaryContainer
                                isTodayWfo -> MaterialTheme.colorScheme.onTertiaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(20.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Today's Attendance Status",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = when {
                                    isTodayAttended -> "Marked Present (WFO) ✓"
                                    isTodayWfo -> "Scheduled WFO Day • Pending Check-in"
                                    else -> "Work From Home / Off Day"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isTodayAttended -> MaterialTheme.colorScheme.onPrimaryContainer
                                    isTodayWfo -> MaterialTheme.colorScheme.onTertiaryContainer
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }

            // 5. PRACTICAL RECOMMENDATIONS
            Text(
                text = "Practical Recommendations",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RecommendationTile(
                    icon = Icons.Default.Umbrella,
                    iconTint = if (weatherState.insight.umbrellaNeeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    title = "Umbrella Status",
                    description = if (weatherState.insight.umbrellaNeeded) "Advised: Carry an umbrella due to rain risk." else "Not Needed: Rain risk is minimal."
                )

                RecommendationTile(
                    icon = Icons.Default.DirectionsCar,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "Preferred Transport",
                    description = weatherState.insight.recommendedTransport.ifBlank { "Standard transit mode." }
                )

                val delayAdvisory = getCommuteDelayAdvisory(weatherState)
                RecommendationTile(
                    icon = Icons.Default.AccessTime,
                    iconTint = if (weatherState.commuteStatus == CommuteStatus.DIFFICULT) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                    title = "Expected Delay Advisory",
                    description = delayAdvisory
                )

                if (weatherState.currentTempC >= 34) {
                    RecommendationTile(
                        icon = Icons.Default.Thermostat,
                        iconTint = MaterialTheme.colorScheme.error,
                        title = "High Temperature Alert",
                        description = "Peak heat of ${weatherState.currentTempC}°C today. Hydrate well and use AC transit."
                    )
                }
            }

            // 6. INTERACTIVE 24-HOUR TIMELINE
            Text(
                text = "24-Hour Commute Forecast",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            HourlyInteractiveTimeline(
                hourlyForecast = weatherState.hourlyForecast,
                forecastStatus = weatherState.forecastStatus,
                radarStatus = weatherState.radarStatus,
                isLoading = weatherState.isRefreshing
            )

            // 7. ACTION BUTTONS (48dp Touch Targets)
            val portalUrl = configState?.portalUrl
            if (!portalUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(portalUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open Office Portal",
                        style = MaterialTheme.typography.labelLarge
                    )
            }
        }
    }
}
}
}

@Composable
private fun CommuteScheduleCard(
    title: String,
    timeStr: String,
    forecast: HourlyCommuteForecast?,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = timeStr,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (forecast != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = WeatherVisuals.getWeatherConditionIcon(forecast.condition),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${forecast.tempC}°C • ${forecast.condition.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (forecast.rainChancePercent > 0) {
                    Text(
                        text = "Rain chance: ${forecast.rainChancePercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (forecast.rainChancePercent >= 40) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Text(
                    text = "Forecast available hourly",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun RecommendationTile(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.12f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun computeExpectedRainWindow(hourlyForecast: List<HourlyCommuteForecast>): String {
    if (hourlyForecast.isEmpty()) return "No forecast data"

    val rainHours = hourlyForecast.filter { it.rainChancePercent >= 30 }
    if (rainHours.isEmpty()) {
        val maxRain = hourlyForecast.maxOfOrNull { it.rainChancePercent } ?: 0
        return "All day clear (Max $maxRain% rain chance)"
    }

    val startHour = rainHours.first().timeLabel
    val endHour = rainHours.last().timeLabel
    val maxRainInWindow = rainHours.maxOf { it.rainChancePercent }

    return if (startHour == endHour) {
        "Around $startHour ($maxRainInWindow% peak chance)"
    } else {
        "$startHour – $endHour ($maxRainInWindow% peak chance)"
    }
}

private fun findForecastForHourStr(timeStr: String?, hourlyForecast: List<HourlyCommuteForecast>): HourlyCommuteForecast? {
    if (timeStr.isNullOrBlank()) return null
    val hourInt = timeStr.trim().split(":").getOrNull(0)?.toIntOrNull() ?: return null

    return hourlyForecast.find { forecast ->
        val forecastHour = forecast.timeLabel.split(" ").getOrNull(0)?.toIntOrNull()
        val isPm = forecast.timeLabel.contains("PM", ignoreCase = true)
        val isAm = forecast.timeLabel.contains("AM", ignoreCase = true)
        val forecast24 = when {
            isPm && forecastHour != null && forecastHour != 12 -> forecastHour + 12
            isAm && forecastHour == 12 -> 0
            else -> forecastHour ?: -1
        }
        forecast24 == hourInt
    }
}

private fun getCommuteDelayAdvisory(weatherState: WeatherState): String {
    return when {
        weatherState.condition == WeatherCondition.THUNDERSTORM ->
            "Storm warning. High risk of severe rain, waterlogging & travel delays."
        weatherState.condition == WeatherCondition.HEAVY_RAIN || weatherState.rainChancePercent >= 70 ->
            "Heavy rain expected. Plan for 15–30 min travel delays."
        weatherState.rainChancePercent >= 40 || weatherState.condition == WeatherCondition.RAINY ->
            "Showers expected. Possible 5–15 min commute delays."
        else ->
            "Optimal weather conditions. No weather delays expected."
    }
}

private data class RainAlertCategory(
    val title: String,
    val description: String,
    val containerColor: Color,
    val contentColor: Color,
    val icon: ImageVector
)

@Composable
private fun getRainAlertCategory(weatherState: WeatherState): RainAlertCategory {
    val colorScheme = MaterialTheme.colorScheme
    return when {
        weatherState.condition == WeatherCondition.THUNDERSTORM ||
        weatherState.condition == WeatherCondition.HEAVY_RAIN ||
        weatherState.rainChancePercent >= 70 -> {
            RainAlertCategory(
                title = if (weatherState.condition == WeatherCondition.THUNDERSTORM) "Storm Warning" else "Heavy Rain Alert",
                description = "Heavy precipitation predicted during commute hours (${weatherState.rainChancePercent}% chance).",
                containerColor = colorScheme.errorContainer,
                contentColor = colorScheme.onErrorContainer,
                icon = if (weatherState.condition == WeatherCondition.THUNDERSTORM) Icons.Default.Thunderstorm else Icons.Default.Grain
            )
        }
        weatherState.rainChancePercent >= 30 || weatherState.condition == WeatherCondition.RAINY -> {
            RainAlertCategory(
                title = "Rain Expected",
                description = "Precipitation anticipated along your route (${weatherState.rainChancePercent}% chance). Carry rain protection.",
                containerColor = colorScheme.tertiaryContainer,
                contentColor = colorScheme.onTertiaryContainer,
                icon = Icons.Default.WaterDrop
            )
        }
        else -> {
            RainAlertCategory(
                title = "Clear Commute Conditions",
                description = "Clear to mild weather forecast (${weatherState.rainChancePercent}% rain chance). Optimal commute conditions.",
                containerColor = colorScheme.primaryContainer,
                contentColor = colorScheme.onPrimaryContainer,
                icon = Icons.Default.CheckCircle
            )
        }
    }
}
