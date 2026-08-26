package com.urunkarpm.pingpin.ui.components.weather

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.urunkarpm.pingpin.data.model.ForecastStatus
import com.urunkarpm.pingpin.data.model.HourlyCommuteForecast
import com.urunkarpm.pingpin.data.model.RadarStatus
import com.urunkarpm.pingpin.data.model.WeatherCondition
import com.urunkarpm.pingpin.ui.theme.rememberIsReduceMotionEnabled
import java.util.Calendar

/**
 * Horizontally interactive 24-hour weather timeline styled with Material 3 design system.
 * Features:
 * - Horizontally scrollable timeline with auto-scroll to current hour
 * - Continuous visual rain-probability trend curve drawn with custom Compose Canvas
 * - Selected hour detail card with smooth M3 crossfade animations
 * - Minimum 48dp touch target accessibility for all interactive nodes
 * - Clean surface hierarchy (surfaceContainerLow, surfaceContainer, outlineVariant)
 */
@Composable
fun HourlyInteractiveTimeline(
    hourlyForecast: List<HourlyCommuteForecast>,
    modifier: Modifier = Modifier,
    initialSelectedIndex: Int? = null,
    forecastStatus: ForecastStatus = ForecastStatus.AVAILABLE,
    radarStatus: RadarStatus = RadarStatus.AVAILABLE,
    isLoading: Boolean = false,
    onHourSelected: (HourlyCommuteForecast) -> Unit = {}
) {
    if (isLoading) {
        HourlyInteractiveTimelineSkeleton(modifier = modifier)
        return
    }

    if (forecastStatus == com.urunkarpm.pingpin.data.model.ForecastStatus.UNAVAILABLE || hourlyForecast.isEmpty()) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Hourly Forecast Unavailable",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Detailed hourly weather breakdown is currently unavailable.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        return
    }

    val haptic = LocalHapticFeedback.current

    // Calculate default selected index: use peak commute or current hour matching local time
    val defaultIndex = remember(hourlyForecast, initialSelectedIndex) {
        initialSelectedIndex ?: run {
            val currentHourInt = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val indexMatch = hourlyForecast.indexOfFirst { forecast ->
                val hour = forecast.timeLabel.split(" ")[0].toIntOrNull()
                val isPm = forecast.timeLabel.contains("PM", ignoreCase = true)
                val isAm = forecast.timeLabel.contains("AM", ignoreCase = true)
                val forecastHour24 = when {
                    isPm && hour != null && hour != 12 -> hour + 12
                    isAm && hour == 12 -> 0
                    else -> hour ?: -1
                }
                forecastHour24 == currentHourInt
            }
            if (indexMatch >= 0) indexMatch else (hourlyForecast.indexOfFirst { it.isPeakCommute }.takeIf { it >= 0 } ?: 0)
        }
    }

    var selectedIndex by remember(hourlyForecast) { mutableIntStateOf(defaultIndex.coerceIn(0, hourlyForecast.lastIndex)) }
    val selectedForecast = hourlyForecast.getOrNull(selectedIndex) ?: hourlyForecast[0]

    val listState = rememberLazyListState()

    // Scroll to initial index on launch
    LaunchedEffect(defaultIndex) {
        if (defaultIndex > 0 && defaultIndex < hourlyForecast.size) {
            listState.animateScrollToItem(maxOf(0, defaultIndex - 1))
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Dynamic Detail Card for Selected Hour
        SelectedHourDetailCard(forecast = selectedForecast)

        // 2. Rain Probability Trend Line & Interactive Timeline Container
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                // Rain trend header title & max rain indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Rain Probability Trend",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val maxRainInList = remember(hourlyForecast) { hourlyForecast.maxOfOrNull { it.rainChancePercent } ?: 0 }
                    Text(
                        text = if (radarStatus == com.urunkarpm.pingpin.data.model.RadarStatus.UNAVAILABLE) "Radar Off" else "Peak: $maxRainInList%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (maxRainInList >= 40) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Rain Probability Canvas Graph + Scrollable Timeline
                Box(modifier = Modifier.fillMaxWidth()) {
                    val isRadarAvailable = radarStatus == com.urunkarpm.pingpin.data.model.RadarStatus.AVAILABLE && hourlyForecast.size >= 2

                    if (isRadarAvailable) {
                        val rainColor = MaterialTheme.colorScheme.primary
                        val graphHeightDp = 44.dp

                        val maxRainInListForSemantics = remember(hourlyForecast) { hourlyForecast.maxOfOrNull { it.rainChancePercent } ?: 0 }

                        val path = remember { Path() }
                        val fillPath = remember { Path() }

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(graphHeightDp)
                                .padding(horizontal = 16.dp)
                                .semantics {
                                    contentDescription = "Rain probability trend curve across 24 hours. Peak rain chance is $maxRainInListForSemantics percent."
                                }
                        ) {
                        if (hourlyForecast.size < 2) return@Canvas

                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val stepX = canvasWidth / (hourlyForecast.size - 1)

                        path.reset()
                        fillPath.reset()

                        for (i in hourlyForecast.indices) {
                            val rainPct = hourlyForecast[i].rainChancePercent.coerceIn(0, 100)
                            val x = i * stepX
                            val y = canvasHeight - (rainPct / 100f * (canvasHeight - 12f)) - 6f

                            if (i == 0) {
                                path.moveTo(x, y)
                                fillPath.moveTo(x, canvasHeight)
                                fillPath.lineTo(x, y)
                            } else {
                                val prevX = (i - 1) * stepX
                                val prevRain = hourlyForecast[i - 1].rainChancePercent.coerceIn(0, 100)
                                val prevY = canvasHeight - (prevRain / 100f * (canvasHeight - 12f)) - 6f
                                val controlX1 = prevX + stepX / 2f
                                val controlX2 = prevX + stepX / 2f
                                path.cubicTo(controlX1, prevY, controlX2, y, x, y)
                                fillPath.cubicTo(controlX1, prevY, controlX2, y, x, y)
                            }
                        }

                        fillPath.lineTo(canvasWidth, canvasHeight)
                        fillPath.close()

                        // Draw area gradient under rain trend
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    rainColor.copy(alpha = 0.25f),
                                    rainColor.copy(alpha = 0.02f)
                                )
                            )
                        )

                        // Draw main rain trendline
                        drawPath(
                            path = path,
                            color = rainColor.copy(alpha = 0.85f),
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Highlight dots for each hour node
                        for (i in hourlyForecast.indices) {
                            val rainPct = hourlyForecast[i].rainChancePercent.coerceIn(0, 100)
                            val x = i * stepX
                            val y = canvasHeight - (rainPct / 100f * (canvasHeight - 12f)) - 6f
                            val isSelected = i == selectedIndex

                            if (isSelected) {
                                drawCircle(
                                    color = rainColor,
                                    radius = 5.5.dp.toPx(),
                                    center = androidx.compose.ui.geometry.Offset(x, y)
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 2.5.dp.toPx(),
                                    center = androidx.compose.ui.geometry.Offset(x, y)
                                )
                            } else if (rainPct > 0) {
                                drawCircle(
                                    color = rainColor.copy(alpha = 0.5f),
                                    radius = 3.dp.toPx(),
                                    center = androidx.compose.ui.geometry.Offset(x, y)
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = "Rain trend curve unavailable for this location",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                    // Scrollable Hourly Cards Timeline
                    LazyRow(
                        state = listState,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 36.dp)
                    ) {
                        itemsIndexed(
                            items = hourlyForecast,
                            key = { _, item -> item.timeLabel }
                        ) { index, forecast ->
                            val isSelected = index == selectedIndex

                            TimelineHourNode(
                                forecast = forecast,
                                isSelected = isSelected,
                                onClick = {
                                    if (selectedIndex != index) {
                                        selectedIndex = index
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onHourSelected(forecast)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Header detail card showing complete metrics & advisory for the selected hour.
 */
@Composable
private fun SelectedHourDetailCard(
    forecast: HourlyCommuteForecast
) {
    val conditionIcon = WeatherVisuals.getWeatherConditionIcon(forecast.condition)
    val conditionAccent = WeatherVisuals.getConditionAccentColor(forecast.condition)

    val rainSeverityColor = when {
        forecast.rainChancePercent >= 60 -> MaterialTheme.colorScheme.error
        forecast.rainChancePercent >= 30 -> Color(0xFFEF6C00)
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        AnimatedContent(
            targetState = forecast,
            transitionSpec = {
                (fadeIn(animationSpec = tween(160)) togetherWith fadeOut(animationSpec = tween(120)))
                    .using(androidx.compose.animation.SizeTransform(clip = false) { _, _ -> tween(0) })
            },
            label = "SelectedHourDetail"
        ) { targetForecast ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = conditionIcon,
                                    contentDescription = null,
                                    tint = conditionAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = targetForecast.timeLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (targetForecast.commuteTag != null) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = targetForecast.commuteTag,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = targetForecast.condition.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Temperature & Rain Chance Info
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${targetForecast.tempC}°C",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${targetForecast.rainChancePercent}% Rain",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = rainSeverityColor
                            )
                        }
                    }
                }

                // Hourly Commute Recommendation Note
                val advisoryNote = when {
                    targetForecast.condition == WeatherCondition.THUNDERSTORM || targetForecast.rainChancePercent >= 70 ->
                        "Severe weather predicted. Avoid open travel during this hour."
                    targetForecast.rainChancePercent >= 40 ->
                        "High rain chance. Carry an umbrella or delay check-out."
                    targetForecast.isPeakCommute ->
                        "Peak commute hour. Weather conditions optimal for transit."
                    else ->
                        "Clear weather forecast for this hour."
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = rainSeverityColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = advisoryNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual hourly node item in horizontal timeline row.
 * Minimum dimensions 72dp x 112dp fulfill minimum 48dp touch target requirements.
 */
@Composable
private fun TimelineHourNode(
    forecast: HourlyCommuteForecast,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isReduceMotion = rememberIsReduceMotionEnabled()
    val conditionIcon = WeatherVisuals.getWeatherConditionIcon(forecast.condition)

    val scale by animateFloatAsState(
        targetValue = if (isSelected && !isReduceMotion) 1.04f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "NodeScale"
    )

    val bgColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        animationSpec = tween(if (isReduceMotion) 0 else 150),
        label = "NodeBgColor"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primary
            forecast.isPeakCommute -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        },
        animationSpec = tween(if (isReduceMotion) 0 else 150),
        label = "NodeBorderColor"
    )

    val borderThickness by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 1.dp,
        animationSpec = tween(if (isReduceMotion) 0 else 150),
        label = "NodeBorderWidth"
    )

    val nodeSemanticsText = remember(forecast, isSelected) {
        val peakTag = if (forecast.isPeakCommute) "Peak Commute Hour. " else ""
        val tag = forecast.commuteTag?.let { "$it. " } ?: ""
        "Forecast for ${forecast.timeLabel}: ${forecast.tempC} degrees Celsius, ${forecast.condition.label}, ${forecast.rainChancePercent} percent rain. $peakTag$tag${if (isSelected) "Selected" else ""}"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .width(72.dp)
            .height(112.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(borderThickness, borderColor, RoundedCornerShape(12.dp))
            .semantics(mergeDescendants = true) {
                role = Role.Button
                selected = isSelected
                contentDescription = nodeSemanticsText
            }
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        // Tag badge slot
        Box(
            modifier = Modifier.height(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (forecast.commuteTag != null) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = forecast.commuteTag,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        maxLines = 1
                    )
                }
            }
        }

        // Time label
        Text(
            text = forecast.timeLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected || forecast.isPeakCommute) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )

        // Weather condition icon
        Icon(
            imageVector = conditionIcon,
            contentDescription = null,
            tint = WeatherVisuals.getConditionAccentColor(forecast.condition),
            modifier = Modifier.size(18.dp)
        )

        // Temperature
        Text(
            text = "${forecast.tempC}°",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Rain percentage chip slot
        Box(
            modifier = Modifier.height(14.dp),
            contentAlignment = Alignment.Center
        ) {
            if (forecast.rainChancePercent > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(1.dp))
                    Text(
                        text = "${forecast.rainChancePercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Selected dot indicator
        Box(
            modifier = Modifier.height(6.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
            }
        }
    }
}
