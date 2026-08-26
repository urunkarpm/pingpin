package com.urunkarpm.pingpin.ui.components.weather

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.urunkarpm.pingpin.data.model.WeatherCondition
import com.urunkarpm.pingpin.data.model.WeatherState

object WeatherVisuals {

    fun getWeatherConditionIcon(condition: WeatherCondition): ImageVector {
        return when (condition) {
            WeatherCondition.SUNNY -> Icons.Default.WbSunny
            WeatherCondition.PARTLY_CLOUDY -> Icons.Default.WbCloudy
            WeatherCondition.CLOUDY -> Icons.Default.Cloud
            WeatherCondition.RAINY -> Icons.Default.WaterDrop
            WeatherCondition.HEAVY_RAIN -> Icons.Default.Grain
            WeatherCondition.THUNDERSTORM -> Icons.Default.Thunderstorm
            WeatherCondition.WINDY -> Icons.Default.Air
        }
    }

    @Composable
    fun getStatusBadgeColor(weatherState: WeatherState): Color {
        return when {
            weatherState.condition == WeatherCondition.THUNDERSTORM ||
            weatherState.condition == WeatherCondition.HEAVY_RAIN ||
            weatherState.rainChancePercent >= 50 -> MaterialTheme.colorScheme.error
            weatherState.rainChancePercent >= 30 -> Color(0xFFEF6C00) // Warning Orange
            else -> MaterialTheme.colorScheme.primary
        }
    }

    fun getCommuteRainBadgeColor(peakCommuteRainPercent: Int): Color {
        return if (peakCommuteRainPercent >= 40) Color(0xFFC62828) else Color(0xFF0288D1)
    }

    fun getConditionAccentColor(condition: WeatherCondition): Color {
        return when (condition) {
            WeatherCondition.SUNNY -> Color(0xFFF57C00) // Warm Amber/Orange
            WeatherCondition.PARTLY_CLOUDY -> Color(0xFF0288D1) // Sky Blue
            WeatherCondition.CLOUDY -> Color(0xFF607D8B) // Slate Grey
            WeatherCondition.RAINY -> Color(0xFF0288D1) // Rain Blue
            WeatherCondition.HEAVY_RAIN -> Color(0xFF1565C0) // Deep Rain Blue
            WeatherCondition.THUNDERSTORM -> Color(0xFF7B1FA2) // Purple / Violet
            WeatherCondition.WINDY -> Color(0xFF00897B) // Teal Breeze
        }
    }

    // Keep getConditionGlowColor for backwards compatibility
    fun getConditionGlowColor(condition: WeatherCondition): Color = getConditionAccentColor(condition)
}
