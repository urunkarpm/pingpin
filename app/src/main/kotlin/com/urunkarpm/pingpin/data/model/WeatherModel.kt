package com.urunkarpm.pingpin.data.model

enum class WeatherCondition {
    SUNNY,
    PARTLY_CLOUDY,
    CLOUDY,
    RAINY,
    HEAVY_RAIN,
    THUNDERSTORM,
    WINDY;

    val label: String
        get() = when (this) {
            SUNNY -> "Sunny"
            PARTLY_CLOUDY -> "Partly Cloudy"
            CLOUDY -> "Overcast"
            RAINY -> "Rainy"
            HEAVY_RAIN -> "Heavy Rain"
            THUNDERSTORM -> "Stormy"
            WINDY -> "Windy"
        }
}

enum class CommuteStatus(val label: String) {
    GOOD("Good"),
    CONSIDER_DELAYING("Consider delaying"),
    DIFFICULT("Difficult");
}

data class HourlyCommuteForecast(
    val timeLabel: String,
    val tempC: Int,
    val condition: WeatherCondition,
    val rainChancePercent: Int,
    val isPeakCommute: Boolean = false,
    val commuteTag: String? = null
)

enum class LocationStatus(val label: String) {
    RESOLVED("GPS Location"),
    PERMISSION_DENIED("Location Permission Denied"),
    LOCATION_UNAVAILABLE("Location Services Unavailable"),
    OFFICE_FALLBACK("Office Area"),
    DEFAULT_FALLBACK("Default Area")
}

enum class ForecastStatus(val label: String) {
    AVAILABLE("Available"),
    UNAVAILABLE("Forecast Unavailable"),
    PARTIAL("Partial Forecast")
}

enum class RadarStatus(val label: String) {
    AVAILABLE("Radar Active"),
    UNAVAILABLE("Radar / Trend Unavailable")
}

data class TravelInsight(
    val headline: String = "",
    val detail: String = "",
    val umbrellaNeeded: Boolean = false,
    val recommendedTransport: String = "",
    val travelSafetyScore: String = ""
)

data class WeatherState(
    val locationName: String = "",
    val currentTempC: Int = 0,
    val feelsLikeC: Int = 0,
    val tempHighC: Int = 0,
    val tempLowC: Int = 0,
    val condition: WeatherCondition = WeatherCondition.PARTLY_CLOUDY,
    val rainChancePercent: Int = 0,
    val hourlyForecast: List<HourlyCommuteForecast> = emptyList(),
    val insight: TravelInsight = TravelInsight(),
    val lastUpdatedMillis: Long = 0L,
    val hasValidData: Boolean = false,
    val isInitialLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String? = null,
    val isStale: Boolean = false,
    val staleAgeMinutes: Long = 0L,
    val locationStatus: LocationStatus = LocationStatus.RESOLVED,
    val forecastStatus: ForecastStatus = ForecastStatus.AVAILABLE,
    val radarStatus: RadarStatus = RadarStatus.AVAILABLE
) {
    val commuteStatus: CommuteStatus
        get() = when {
            condition == WeatherCondition.THUNDERSTORM || condition == WeatherCondition.HEAVY_RAIN || rainChancePercent >= 70 -> CommuteStatus.DIFFICULT
            rainChancePercent >= 40 || condition == WeatherCondition.RAINY || currentTempC >= 34 -> CommuteStatus.CONSIDER_DELAYING
            else -> CommuteStatus.GOOD
        }

    val trafficRisk: String
        get() = when (commuteStatus) {
            CommuteStatus.DIFFICULT -> "High Risk"
            CommuteStatus.CONSIDER_DELAYING -> "Moderate Risk"
            CommuteStatus.GOOD -> "Low Risk"
        }
}


