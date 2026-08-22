package com.urunkarpm.pingpin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urunkarpm.pingpin.data.model.HourlyCommuteForecast
import com.urunkarpm.pingpin.data.model.WeatherCondition
import com.urunkarpm.pingpin.data.model.WeatherState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherTravelCard(
    weatherState: WeatherState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var showDetailSheet by remember { mutableStateOf(false) }

    // Color theme definition
    val headerIcon = when (weatherState.condition) {
        WeatherCondition.SUNNY -> Icons.Default.WbSunny
        WeatherCondition.PARTLY_CLOUDY -> Icons.Default.WbCloudy
        WeatherCondition.CLOUDY -> Icons.Default.Cloud
        WeatherCondition.RAINY -> Icons.Default.WaterDrop
        WeatherCondition.THUNDERSTORM -> Icons.Default.Thunderstorm
        WeatherCondition.WINDY -> Icons.Default.Air
    }

    val statusBadgeColor = when {
        weatherState.rainChancePercent >= 50 || weatherState.condition == WeatherCondition.THUNDERSTORM -> Color(0xFFE53935) // Alert Red
        weatherState.rainChancePercent >= 30 -> Color(0xFFFF9800) // Orange Warning
        else -> MaterialTheme.colorScheme.primary // Standard Blue/Teal Accent
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = {
            showDetailSheet = true
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row (Matches Office Occupancy Card Header Structure)
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
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = headerIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Weather & Travel Radar",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = (-0.2).sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = weatherState.locationName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onRefresh()
                    },
                    enabled = !isRefreshing,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Updating", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Refresh", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                thickness = 1.dp
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 3-Stat Content Cards Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 1: Temp & Condition
                WeatherStatTile(
                    modifier = Modifier.weight(1f),
                    label = weatherState.condition.label,
                    value = "${weatherState.currentTempC}°C",
                    subText = "H:${weatherState.tempHighC}° L:${weatherState.tempLowC}°",
                    icon = headerIcon,
                    badgeColor = MaterialTheme.colorScheme.primary
                )

                // Card 2: Commute Insight
                WeatherStatTile(
                    modifier = Modifier.weight(1f),
                    label = "Travel Status",
                    value = weatherState.insight.travelSafetyScore,
                    subText = if (weatherState.insight.umbrellaNeeded) "Bring Umbrella" else "No Umbrella Needed",
                    icon = if (weatherState.insight.umbrellaNeeded) Icons.Default.Umbrella else Icons.Default.Navigation,
                    badgeColor = statusBadgeColor
                )

                // Card 3: Peak Commute Rain Chance
                val peakCommuteRain = weatherState.hourlyForecast
                    .filter { it.isPeakCommute }
                    .maxOfOrNull { it.rainChancePercent } ?: weatherState.rainChancePercent

                WeatherStatTile(
                    modifier = Modifier.weight(1f),
                    label = "Commute Rain",
                    value = "$peakCommuteRain%",
                    subText = weatherState.insight.recommendedTransport,
                    icon = Icons.Default.WaterDrop,
                    badgeColor = if (peakCommuteRain >= 40) Color(0xFFE53935) else Color(0xFF0078D4)
                )
            }
        }
    }

    // Detailed Travel Insight & Forecast Bottom Sheet
    if (showDetailSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDetailSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Commute Weather Insights",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Location: ${weatherState.locationName}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = statusBadgeColor.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, statusBadgeColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = weatherState.insight.travelSafetyScore,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusBadgeColor,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Detailed Insight Banner
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusBadgeColor.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(statusBadgeColor.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (weatherState.insight.umbrellaNeeded) Icons.Default.Umbrella else Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = statusBadgeColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = weatherState.insight.headline,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = weatherState.insight.detail,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Hourly Commute Timeline
                Text(
                    text = "Peak Commute Hour Forecast",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(weatherState.hourlyForecast) { forecast ->
                        HourlyForecastPill(forecast = forecast)
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun WeatherStatTile(
    label: String,
    value: String,
    subText: String,
    icon: ImageVector,
    badgeColor: Color,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isDark) {
                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.8f)
                }
            )
            .border(
                width = 1.dp,
                color = badgeColor.copy(alpha = 0.25f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color = badgeColor.copy(alpha = 0.15f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = badgeColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.4).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subText,
                fontSize = 9.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HourlyForecastPill(forecast: HourlyCommuteForecast) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    val conditionIcon = when (forecast.condition) {
        WeatherCondition.SUNNY -> Icons.Default.WbSunny
        WeatherCondition.PARTLY_CLOUDY -> Icons.Default.WbCloudy
        WeatherCondition.CLOUDY -> Icons.Default.Cloud
        WeatherCondition.RAINY -> Icons.Default.WaterDrop
        WeatherCondition.THUNDERSTORM -> Icons.Default.Thunderstorm
        WeatherCondition.WINDY -> Icons.Default.Air
    }

    val pillBorder = if (forecast.isPeakCommute) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f),
        border = androidx.compose.foundation.BorderStroke(1.dp, pillBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = forecast.timeLabel,
                fontSize = 12.sp,
                fontWeight = if (forecast.isPeakCommute) FontWeight.Bold else FontWeight.Medium,
                color = if (forecast.isPeakCommute) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Icon(
                imageVector = conditionIcon,
                contentDescription = null,
                tint = if (forecast.rainChancePercent >= 40) Color(0xFF0078D4) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${forecast.tempC}°C",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (forecast.rainChancePercent > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = Color(0xFF0078D4),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${forecast.rainChancePercent}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0078D4)
                    )
                }
            }
        }
    }
}
