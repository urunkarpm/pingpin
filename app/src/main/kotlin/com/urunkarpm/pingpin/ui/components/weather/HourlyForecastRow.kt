package com.urunkarpm.pingpin.ui.components.weather

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.urunkarpm.pingpin.data.model.ForecastStatus
import com.urunkarpm.pingpin.data.model.HourlyCommuteForecast
import com.urunkarpm.pingpin.data.model.RadarStatus

@Composable
fun HourlyForecastRow(
    hourlyForecast: List<HourlyCommuteForecast>,
    modifier: Modifier = Modifier,
    forecastStatus: ForecastStatus = ForecastStatus.AVAILABLE,
    radarStatus: RadarStatus = RadarStatus.AVAILABLE,
    isLoading: Boolean = false
) {
    HourlyInteractiveTimeline(
        hourlyForecast = hourlyForecast,
        modifier = modifier,
        forecastStatus = forecastStatus,
        radarStatus = radarStatus,
        isLoading = isLoading
    )
}

