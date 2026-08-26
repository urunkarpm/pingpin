package com.urunkarpm.pingpin.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.urunkarpm.pingpin.data.model.WeatherState
import com.urunkarpm.pingpin.ui.components.weather.CommuteStatusCard

@Composable
fun WeatherTravelCard(
    weatherState: WeatherState,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    CommuteStatusCard(
        weatherState = weatherState,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        onClick = onClick,
        modifier = modifier
    )
}


