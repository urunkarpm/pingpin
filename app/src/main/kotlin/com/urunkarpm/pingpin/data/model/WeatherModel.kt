package com.urunkarpm.pingpin.data.model

import androidx.compose.ui.graphics.vector.ImageVector

enum class WeatherCondition {
    SUNNY,
    PARTLY_CLOUDY,
    CLOUDY,
    RAINY,
    THUNDERSTORM,
    WINDY;

    val label: String
        get() = when (this) {
            SUNNY -> "Sunny"
            PARTLY_CLOUDY -> "Partly Cloudy"
            CLOUDY -> "Overcast"
            RAINY -> "Rainy"
            THUNDERSTORM -> "Stormy"
            WINDY -> "Windy"
        }
}

data class HourlyCommuteForecast(
    val timeLabel: String,
    val tempC: Int,
    val condition: WeatherCondition,
    val rainChancePercent: Int,
    val isPeakCommute: Boolean = false
)

data class TravelInsight(
    val headline: String,
    val detail: String,
    val umbrellaNeeded: Boolean,
    val recommendedTransport: String,
    val travelSafetyScore: String // e.g. "Optimal", "Exercise Caution", "Storm Warning"
)

data class WeatherState(
    val locationName: String = "Bengaluru",
    val currentTempC: Int = 26,
    val feelsLikeC: Int = 27,
    val tempHighC: Int = 29,
    val tempLowC: Int = 20,
    val condition: WeatherCondition = WeatherCondition.PARTLY_CLOUDY,
    val rainChancePercent: Int = 15,
    val hourlyForecast: List<HourlyCommuteForecast> = emptyList(),
    val insight: TravelInsight = TravelInsight(
        headline = "Ideal Commute Weather",
        detail = "Clear skies during commute hours. Traffic & visibility optimal.",
        umbrellaNeeded = false,
        recommendedTransport = "Bike / Open Travel",
        travelSafetyScore = "Optimal"
    )
)
