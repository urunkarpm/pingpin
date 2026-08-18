package com.urunkarpm.pingpin.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urunkarpm.pingpin.service.BleMobileScanResult
import com.urunkarpm.pingpin.service.DetectedMobileDevice
import com.urunkarpm.pingpin.service.DetectedMobileOS
import com.urunkarpm.pingpin.service.ProximityZone
import kotlin.math.cos
import kotlin.math.sin

enum class OccupancyViewMode {
    RADAR_MAP,
    STATS_GRID
}

// Military Tactical Neon Palette (ONLY for the 2D Radar Canvas Map)
private val NeonGreen = Color(0xFF00FF66)
private val NeonGreenGlow = Color(0xFF39FF14)
private val NeonCyan = Color(0xFF00E5FF)
private val DarkTacticalBg = Color(0xFF03140C)
private val TacticalGridColor = Color(0xFF00FF66).copy(alpha = 0.15f)

@Composable
fun OfficeOccupancyCard(
    scanResult: BleMobileScanResult?,
    isScanning: Boolean,
    onStartScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var viewMode by remember { mutableStateOf(OccupancyViewMode.RADAR_MAP) }

    val activeResult = remember(scanResult) {
        scanResult ?: BleMobileScanResult(
            totalCount = 0,
            androidCount = 0,
            iosCount = 0,
            immediateCount = 0,
            nearbyCount = 0,
            perimeterCount = 0,
            devices = emptyList()
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseRadius"
    )

    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarSweep"
    )

    val targetPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "TargetPulse"
    )

    GlassCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Original Standard App Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(46.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isScanning) {
                            val pulseColor = MaterialTheme.colorScheme.primary
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    color = pulseColor.copy(alpha = (1f - pulseRadius).coerceIn(0f, 1f)),
                                    radius = (size.minDimension / 2f) * pulseRadius,
                                    style = Stroke(width = 3.dp.toPx())
                                )
                            }
                        }

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
                                imageVector = Icons.AutoMirrored.Filled.BluetoothSearching,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Office Radar Map",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = (-0.2).sp
                        )
                        Text(
                            text = "Live BLE Mobile Signal Scanner",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Primary Scan BLE Action Button
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onStartScan()
                    },
                    enabled = !isScanning,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scanning", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Scan BLE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                thickness = 1.dp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // View Switcher Tabs & Density Vibe Badge Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Density Vibe Badge
                DensityVibeBadge(deviceCount = activeResult.totalCount)

                // View Mode Segmented Switcher
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ViewModePill(
                        selected = viewMode == OccupancyViewMode.RADAR_MAP,
                        label = "Map",
                        icon = Icons.Default.Map,
                        onClick = { viewMode = OccupancyViewMode.RADAR_MAP }
                    )
                    ViewModePill(
                        selected = viewMode == OccupancyViewMode.STATS_GRID,
                        label = "Stats",
                        icon = Icons.Default.GridOn,
                        onClick = { viewMode = OccupancyViewMode.STATS_GRID }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (viewMode == OccupancyViewMode.RADAR_MAP) {
                // Tactical Military Neon Radar Display Canvas (Exclusive to Radar Map)
                MilitaryRadarMapCanvas(
                    scanResult = activeResult,
                    isScanning = isScanning,
                    sweepAngle = sweepAngle,
                    targetPulseAlpha = targetPulseAlpha,
                    pulseRadius = pulseRadius
                )

                if (activeResult.totalCount == 0 && !isScanning) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Press 'Scan BLE' to sweep for live nearby Bluetooth signals",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                // Stat Grid Breakdown View (Standard App Theme)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CountStatCard(
                            modifier = Modifier.weight(1f),
                            label = "Total Phones",
                            count = "${activeResult.totalCount}",
                            icon = Icons.Default.Smartphone,
                            badgeColor = MaterialTheme.colorScheme.primary
                        )
                        CountStatCard(
                            modifier = Modifier.weight(1f),
                            label = "Android",
                            count = "${activeResult.androidCount}",
                            icon = Icons.Default.PhoneAndroid,
                            badgeColor = Color(0xFF34A853)
                        )
                        CountStatCard(
                            modifier = Modifier.weight(1f),
                            label = "iPhone / iOS",
                            count = "${activeResult.iosCount}",
                            icon = Icons.Default.PhoneIphone,
                            badgeColor = Color(0xFF007AFF)
                        )
                    }

                    // Proximity Zone Breakdown
                    ProximityZoneSummaryRow(scanResult = activeResult)
                }
            }
        }
    }
}

@Composable
private fun DensityVibeBadge(deviceCount: Int) {
    val (text, color) = when {
        deviceCount == 0 -> Pair("Empty Zone", Color(0xFF8E8E93))
        deviceCount < 4 -> Pair("Quiet Focus", Color(0xFF10B981))
        deviceCount < 8 -> Pair("Active Collab", Color(0xFFF59E0B))
        else -> Pair("Peak Buzz Hub", Color(0xFFEF4444))
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun ViewModePill(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MilitaryRadarMapCanvas(
    scanResult: BleMobileScanResult,
    isScanning: Boolean,
    sweepAngle: Float,
    targetPulseAlpha: Float,
    pulseRadius: Float
) {
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(DarkTacticalBg)
            .border(1.5.dp, NeonGreen.copy(alpha = 0.6f), RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = (size.minDimension / 2f) * 0.82f

            // 1. Tactical Grid Lines Background
            val gridStep = 24.dp.toPx()
            var x = gridStep
            while (x < size.width) {
                drawLine(
                    color = TacticalGridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 0.8.dp.toPx()
                )
                x += gridStep
            }
            var y = gridStep
            while (y < size.height) {
                drawLine(
                    color = TacticalGridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 0.8.dp.toPx()
                )
                y += gridStep
            }

            // 2. Corner Tactical Target Brackets
            val bracketLen = 14.dp.toPx()
            val strokeW = 2.dp.toPx()
            val bColor = NeonGreen.copy(alpha = 0.8f)
            val pad = 8.dp.toPx()

            // Top-Left Corner Bracket
            drawLine(bColor, Offset(pad, pad), Offset(pad + bracketLen, pad), strokeW)
            drawLine(bColor, Offset(pad, pad), Offset(pad, pad + bracketLen), strokeW)
            // Top-Right Corner Bracket
            drawLine(bColor, Offset(size.width - pad, pad), Offset(size.width - pad - bracketLen, pad), strokeW)
            drawLine(bColor, Offset(size.width - pad, pad), Offset(size.width - pad, pad + bracketLen), strokeW)
            // Bottom-Left Corner Bracket
            drawLine(bColor, Offset(pad, size.height - pad), Offset(pad + bracketLen, size.height - pad), strokeW)
            drawLine(bColor, Offset(pad, size.height - pad), Offset(pad, size.height - pad - bracketLen), strokeW)
            // Bottom-Right Corner Bracket
            drawLine(bColor, Offset(size.width - pad, size.height - pad), Offset(size.width - pad - bracketLen, size.height - pad), strokeW)
            drawLine(bColor, Offset(size.width - pad, size.height - pad), Offset(size.width - pad, size.height - pad - bracketLen), strokeW)

            // 3. Neon Concentric Range Rings (2m, 5m, 8m)
            val ringRadii = listOf(maxRadius * 0.35f, maxRadius * 0.68f, maxRadius)
            val ringLabels = listOf("2M", "5M", "8M")

            ringRadii.forEachIndexed { i, r ->
                // Glowing outer halo ring
                drawCircle(
                    color = NeonGreen.copy(alpha = 0.12f),
                    radius = r,
                    center = center,
                    style = Stroke(width = 4.dp.toPx())
                )
                // Crisp inner range ring line
                drawCircle(
                    color = NeonGreen.copy(alpha = 0.5f),
                    radius = r,
                    center = center,
                    style = Stroke(
                        width = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                )

                // Range Label on Ring
                drawText(
                    textMeasurer = textMeasurer,
                    text = ringLabels[i],
                    style = TextStyle(
                        color = NeonGreen.copy(alpha = 0.7f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    topLeft = Offset(center.x + 4.dp.toPx(), center.y - r + 2.dp.toPx())
                )
            }

            // 4. Radial Angle Rays & Compass Markings
            for (angle in 0 until 360 step 45) {
                val rad = Math.toRadians(angle.toDouble())
                val endX = center.x + (maxRadius * cos(rad)).toFloat()
                val endY = center.y + (maxRadius * sin(rad)).toFloat()

                drawLine(
                    color = NeonGreen.copy(alpha = 0.25f),
                    start = center,
                    end = Offset(endX, endY),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                )
            }

            // Cardinal Direction Markers ('N', 'E', 'S', 'W')
            val cardinalOffset = maxRadius + 10.dp.toPx()
            val cardinalTextStyle = TextStyle(
                color = NeonGreen,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )

            drawText(textMeasurer, "N", Offset(center.x - 4.dp.toPx(), center.y - cardinalOffset - 6.dp.toPx()), style = cardinalTextStyle)
            drawText(textMeasurer, "S", Offset(center.x - 4.dp.toPx(), center.y + cardinalOffset - 6.dp.toPx()), style = cardinalTextStyle)
            drawText(textMeasurer, "E", Offset(center.x + cardinalOffset - 4.dp.toPx(), center.y - 6.dp.toPx()), style = cardinalTextStyle)
            drawText(textMeasurer, "W", Offset(center.x - cardinalOffset - 6.dp.toPx(), center.y - 6.dp.toPx()), style = cardinalTextStyle)

            // 5. Rotating Radar Sweep Line with Gradient Trailing Sector
            val sweepRad = Math.toRadians(sweepAngle.toDouble())
            val sweepEndX = center.x + (maxRadius * cos(sweepRad)).toFloat()
            val sweepEndY = center.y + (maxRadius * sin(sweepRad)).toFloat()

            // Sweep Arc Phosphor Trail (60-degree fading sector)
            drawArc(
                brush = Brush.sweepGradient(
                    0.0f to Color.Transparent,
                    0.85f to Color.Transparent,
                    0.97f to NeonGreen.copy(alpha = 0.35f),
                    1.0f to NeonGreen.copy(alpha = 0.7f),
                    center = center
                ),
                startAngle = sweepAngle - 60f,
                sweepAngle = 60f,
                useCenter = true,
                topLeft = Offset(center.x - maxRadius, center.y - maxRadius),
                size = Size(maxRadius * 2f, maxRadius * 2f)
            )

            // High-intensity Glowing Sweep Beam Line
            drawLine(
                color = NeonGreen,
                start = center,
                end = Offset(sweepEndX, sweepEndY),
                strokeWidth = 2.5.dp.toPx()
            )

            // 6. Central HQ Anchor Reticle ("YOU ARE HERE")
            if (isScanning) {
                drawCircle(
                    color = NeonGreen.copy(alpha = (1f - pulseRadius).coerceIn(0f, 1f)),
                    radius = maxRadius * pulseRadius,
                    center = center,
                    style = Stroke(width = 1.8.dp.toPx())
                )
            }
            drawCircle(
                color = NeonGreen.copy(alpha = 0.2f),
                radius = 16.dp.toPx(),
                center = center
            )
            drawCircle(
                color = NeonGreen,
                radius = 6.dp.toPx(),
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color = Color.White,
                radius = 2.5.dp.toPx(),
                center = center
            )
            // Center Reticle Crosshair
            val chLen = 10.dp.toPx()
            drawLine(NeonGreen, Offset(center.x - chLen, center.y), Offset(center.x - 3.dp.toPx(), center.y), 1.2.dp.toPx())
            drawLine(NeonGreen, Offset(center.x + 3.dp.toPx(), center.y), Offset(center.x + chLen, center.y), 1.2.dp.toPx())
            drawLine(NeonGreen, Offset(center.x, center.y - chLen), Offset(center.x, center.y - 3.dp.toPx()), 1.2.dp.toPx())
            drawLine(NeonGreen, Offset(center.x, center.y + 3.dp.toPx()), Offset(center.x, center.y + chLen), 1.2.dp.toPx())

            // 7. Render Real Detected Mobile Target Signal Blips
            var androidIdx = 1
            var iosIdx = 1

            scanResult.devices.forEachIndexed { index, device ->
                val distMeters = device.estimatedDistanceMeters.coerceIn(0.8f, 8.0f)
                val normalizedRadius = (distMeters / 8.0f) * maxRadius

                val hashAngle = (Math.abs(device.id.hashCode() + index * 53) % 360).toDouble()
                val angleRad = Math.toRadians(hashAngle)

                val nodeX = center.x + (normalizedRadius * cos(angleRad)).toFloat()
                val nodeY = center.y + (normalizedRadius * sin(angleRad)).toFloat()
                val nodeOffset = Offset(nodeX, nodeY)

                val isAndroid = device.os == DetectedMobileOS.ANDROID
                val targetColor = if (isAndroid) NeonGreen else NeonCyan
                val labelTag = if (device.name.isNotEmpty() && !device.name.startsWith("Android Signal") && !device.name.startsWith("iOS Signal") && !device.name.startsWith("BLE Signal")) {
                    if (device.name.length > 12) device.name.take(10) + ".." else device.name
                } else {
                    if (isAndroid) "ANDROID #${androidIdx++}" else "IOS #${iosIdx++}"
                }

                // Outer Neon Ambient Glow Halo
                drawCircle(
                    color = targetColor.copy(alpha = 0.2f * targetPulseAlpha),
                    radius = 12.dp.toPx(),
                    center = nodeOffset
                )
                // Middle Pulsing Ring
                drawCircle(
                    color = targetColor.copy(alpha = 0.6f * targetPulseAlpha),
                    radius = 7.dp.toPx(),
                    center = nodeOffset,
                    style = Stroke(width = 1.5.dp.toPx())
                )
                // Inner Solid Blip Core
                drawCircle(
                    color = targetColor,
                    radius = 4.dp.toPx(),
                    center = nodeOffset
                )
                // Central Intense Core Spot
                drawCircle(
                    color = Color.White,
                    radius = 1.5.dp.toPx(),
                    center = nodeOffset
                )

                // Tactical Target Callout Tag
                drawText(
                    textMeasurer = textMeasurer,
                    text = labelTag.uppercase(),
                    style = TextStyle(
                        color = targetColor,
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    topLeft = Offset(nodeX + 8.dp.toPx(), nodeY - 6.dp.toPx())
                )
            }
        }

        // Top Tactical HUD Details Overlay
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(DarkTacticalBg.copy(alpha = 0.8f), CircleShape)
                    .border(0.8.dp, NeonGreen.copy(alpha = 0.4f), CircleShape)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(if (isScanning) NeonGreenGlow else NeonGreen, CircleShape)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = if (isScanning) "RADAR // SWEEPING" else "RADAR // STANDBY",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Target Count HUD Tag
            Box(
                modifier = Modifier
                    .background(DarkTacticalBg.copy(alpha = 0.8f), CircleShape)
                    .border(0.8.dp, NeonGreen.copy(alpha = 0.4f), CircleShape)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "TARGETS: ${scanResult.totalCount}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Bottom Tactical Legend Overlay
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .background(DarkTacticalBg.copy(alpha = 0.85f), RoundedCornerShape(10.dp))
                .border(0.8.dp, NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(color = NeonGreen, label = "Android")
            LegendItem(color = NeonCyan, label = "iPhone / iOS")
            LegendItem(color = Color.White, label = "HQ (You)")
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun ProximityZoneSummaryRow(scanResult: BleMobileScanResult) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ZoneStatCard(
            modifier = Modifier.weight(1f),
            zoneName = "Immediate Bay",
            distanceText = "< 2 meters",
            count = scanResult.immediateCount,
            color = Color(0xFF10B981)
        )
        ZoneStatCard(
            modifier = Modifier.weight(1f),
            zoneName = "Adjacent Wing",
            distanceText = "2 - 5 meters",
            count = scanResult.nearbyCount,
            color = Color(0xFF3B82F6)
        )
        ZoneStatCard(
            modifier = Modifier.weight(1f),
            zoneName = "Outer Zone",
            distanceText = "> 5 meters",
            count = scanResult.perimeterCount,
            color = Color(0xFF8B5CF6)
        )
    }
}

@Composable
private fun ZoneStatCard(
    zoneName: String,
    distanceText: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isDark) MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f)
            )
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$count",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = zoneName,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = distanceText,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CountStatCard(
    label: String,
    count: String,
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
                text = count,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
