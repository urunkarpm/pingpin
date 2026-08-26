package com.urunkarpm.pingpin.ui.components.weather

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.urunkarpm.pingpin.data.model.CommuteStatus
import com.urunkarpm.pingpin.data.model.WeatherState

/**
 * Unified Commute Status Card styled with Material 3 design system standards.
 * Focuses on:
 * - Clear Material 3 typography scale (headlineSmall, titleMedium, labelMedium, bodySmall)
 * - Standardized shape tokens (24dp primary card, 16dp status hero, 12dp chips)
 * - 8dp grid spacing system
 * - Clean surface hierarchy (surfaceContainerLow, surfaceContainer, outlineVariant)
 * - Minimum 48dp touch targets for accessibility
 * - Adaptable light/dark mode contrast without visual noise
 */
@Composable
fun CommuteStatusCard(
    weatherState: WeatherState,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    // 1. Initial Loading State: Render Skeleton Shimmer (never show fake default values)
    if (weatherState.isInitialLoading && !weatherState.hasValidData) {
        CommuteStatusCardSkeleton(modifier = modifier)
        return
    }

    // 2. API / Network Error State (No cached data)
    if (weatherState.isError && !weatherState.hasValidData) {
        OutlinedCard(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                CommuteStatusErrorState(
                    errorMessage = weatherState.errorMessage ?: "Live weather data unavailable. Check network connection.",
                    onRetry = onRefresh
                )
            }
        }
        return
    }

    val status = weatherState.commuteStatus
    val condition = weatherState.condition

    // Semantic status tokens using M3 surface containers and content colors
    val (statusContainerColor, onStatusContainerColor, statusIcon) = when (status) {
        CommuteStatus.GOOD -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Default.CheckCircle
        )
        CommuteStatus.CONSIDER_DELAYING -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            Icons.Default.Schedule
        )
        CommuteStatus.DIFFICULT -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            Icons.Default.Warning
        )
    }

    val conditionIcon = WeatherVisuals.getWeatherConditionIcon(condition)
    val conditionAccent = WeatherVisuals.getConditionAccentColor(condition)

    val accessibilityDescription = remember(weatherState, status, condition) {
        val location = weatherState.locationName.ifBlank { "Current Location" }
        val umbrellaText = if (weatherState.insight.umbrellaNeeded) "Umbrella advised" else "Umbrella not needed"
        val advice = listOfNotNull(
            weatherState.insight.recommendedTransport.takeIf { it.isNotBlank() },
            weatherState.insight.detail.takeIf { it.isNotBlank() }
        ).joinToString(". ")
        "Commute Status: ${status.label}. Location: $location. Temperature: ${weatherState.currentTempC} degrees Celsius, ${condition.label}. Rain chance: ${weatherState.rainChancePercent} percent. Traffic risk: ${weatherState.trafficRisk}. $umbrellaText. $advice"
    }

    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityDescription
            }
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Top Subtle Linear Progress Bar while preserving existing data during refresh
            if (isRefreshing || weatherState.isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Stale Cached Data Warning Badge
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
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Offline • Cached data (${weatherState.staleAgeMinutes}m ago)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            TextButton(
                                onClick = onRefresh,
                                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Retry", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Permission Denied / Location Unavailable Notice Banner
                if (weatherState.locationStatus == com.urunkarpm.pingpin.data.model.LocationStatus.PERMISSION_DENIED) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Location permission denied • Using ${weatherState.locationName.ifBlank { "Office Location" }}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else if (weatherState.locationStatus == com.urunkarpm.pingpin.data.model.LocationStatus.LOCATION_UNAVAILABLE) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GpsOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "GPS unavailable • Fallback to ${weatherState.locationName.ifBlank { "Default Area" }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 1. Header Row: Location & Current Weather + Refresh Button (48dp Touch Target)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = conditionIcon,
                                    contentDescription = condition.label,
                                    tint = conditionAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = weatherState.locationName.ifBlank { "Current Location" },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${weatherState.currentTempC}°C • ${condition.label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Refresh Button with 48dp touch target
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onRefresh()
                        },
                        enabled = !isRefreshing && !weatherState.isRefreshing,
                        modifier = Modifier.size(48.dp)
                    ) {
                        if (isRefreshing || weatherState.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Commute Weather",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // 2. Hero Commute Status Banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = statusContainerColor,
                    border = BorderStroke(1.dp, onStatusContainerColor.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "COMMUTE STATUS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = onStatusContainerColor.copy(alpha = 0.8f)
                            )
                            Text(
                                text = status.label,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = onStatusContainerColor
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = onStatusContainerColor.copy(alpha = 0.12f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = statusIcon,
                                    contentDescription = status.label,
                                    tint = onStatusContainerColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // 3. Metric Breakdown Chips (Rain Chance, Traffic Risk, Umbrella)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CommuteMetricChip(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.WaterDrop,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "Rain Chance",
                        value = if (weatherState.rainChancePercent in 0..100) "${weatherState.rainChancePercent}%" else "N/A"
                    )

                    CommuteMetricChip(
                        modifier = Modifier.weight(1.1f),
                        icon = Icons.Default.DirectionsCar,
                        iconTint = if (status == CommuteStatus.GOOD) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        title = "Traffic Risk",
                        value = weatherState.trafficRisk
                    )

                    CommuteMetricChip(
                        modifier = Modifier.weight(1.1f),
                        icon = Icons.Default.Umbrella,
                        iconTint = if (weatherState.insight.umbrellaNeeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                        title = "Umbrella",
                        value = if (weatherState.insight.umbrellaNeeded) "Advised" else "Not Needed"
                    )
                }

                // 4. Human-Readable Explanation Banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = onStatusContainerColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = listOfNotNull(
                                weatherState.insight.recommendedTransport.takeIf { it.isNotBlank() },
                                weatherState.insight.detail.takeIf { it.isNotBlank() }
                            ).joinToString(" • ").ifBlank { "Commute weather forecast is clear." },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommuteMetricChip(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CommuteStatusErrorState(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Weather Data Unavailable",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        TextButton(
            onClick = onRetry,
            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
        ) {
            Text(
                text = "Retry",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
