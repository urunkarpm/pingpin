package com.urunkarpm.pingpin.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.urunkarpm.pingpin.data.local.AppDatabase
import com.urunkarpm.pingpin.data.model.HourlyCommuteForecast
import com.urunkarpm.pingpin.data.model.TravelInsight
import com.urunkarpm.pingpin.data.model.WeatherCondition
import com.urunkarpm.pingpin.data.model.WeatherState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.Locale

class WeatherService(private val context: Context) {

    companion object {
        private const val TAG = "WeatherService"
        // Default coordinates: Bengaluru, India (if no location or office config available)
        private const val DEFAULT_LAT = 12.9716
        private const val DEFAULT_LON = 77.5946
        private const val DEFAULT_CITY = "Bengaluru"
    }

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    suspend fun fetchWeatherAndTravelInsights(): WeatherState = withContext(Dispatchers.IO) {
        val (lat, lon, cityName) = resolveBestLocation()

        try {
            val apiUrl = "https://api.open-meteo.com/v1/forecast?" +
                    "latitude=$lat&longitude=$lon" +
                    "&current_weather=true" +
                    "&hourly=temperature_2m,precipitation_probability,weathercode" +
                    "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
                    "&timezone=auto"

            val url = URL(apiUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
            }

            if (connection.responseCode == 200) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonString)

                val currentWeather = root.optJSONObject("current_weather")
                val currentTemp = currentWeather?.optDouble("temperature", 26.0)?.toInt() ?: 26
                val weatherCode = currentWeather?.optInt("weathercode", 0) ?: 0
                val currentCondition = parseWmoWeatherCode(weatherCode)

                val daily = root.optJSONObject("daily")
                val tempHigh = daily?.optJSONArray("temperature_2m_max")?.optDouble(0, currentTemp + 3.0)?.toInt() ?: (currentTemp + 3)
                val tempLow = daily?.optJSONArray("temperature_2m_min")?.optDouble(0, currentTemp - 4.0)?.toInt() ?: (currentTemp - 4)
                val rainMaxChance = daily?.optJSONArray("precipitation_probability_max")?.optInt(0, 10) ?: 10

                val hourly = root.optJSONObject("hourly")
                val times = hourly?.optJSONArray("time")
                val temps = hourly?.optJSONArray("temperature_2m")
                val rainChances = hourly?.optJSONArray("precipitation_probability")
                val codes = hourly?.optJSONArray("weathercode")

                val hourlyForecasts = mutableListOf<HourlyCommuteForecast>()
                var morningRain = 0
                var eveningRain = 0

                if (times != null && temps != null && rainChances != null) {
                    val count = minOf(times.length(), 24)
                    for (i in 0 until count) {
                        val timeStr = times.optString(i, "")
                        // Format e.g. "2026-08-14T08:00"
                        val hourInt = if (timeStr.length >= 13) {
                            timeStr.substring(11, 13).toIntOrNull() ?: i
                        } else i

                        // Filter key commute & benchmark hours: 8 AM, 9 AM, 1 PM, 5 PM, 6 PM, 8 PM
                        if (hourInt in listOf(8, 9, 13, 17, 18, 20)) {
                            val tempVal = temps.optDouble(i, 25.0).toInt()
                            val rainChance = rainChances.optInt(i, 0)
                            val wCode = codes?.optInt(i, 0) ?: 0
                            val cond = parseWmoWeatherCode(wCode)

                            val formattedTime = when (hourInt) {
                                0 -> "12 AM"
                                12 -> "12 PM"
                                in 1..11 -> "$hourInt AM"
                                else -> "${hourInt - 12} PM"
                            }

                            if (hourInt in 8..9) morningRain = maxOf(morningRain, rainChance)
                            if (hourInt in 17..18) eveningRain = maxOf(eveningRain, rainChance)

                            hourlyForecasts.add(
                                HourlyCommuteForecast(
                                    timeLabel = formattedTime,
                                    tempC = tempVal,
                                    condition = cond,
                                    rainChancePercent = rainChance,
                                    isPeakCommute = hourInt in listOf(8, 9, 17, 18)
                                )
                            )
                        }
                    }
                }

                val maxCommuteRain = maxOf(morningRain, eveningRain, rainMaxChance)
                val insight = generateTravelInsight(
                    currentCondition = currentCondition,
                    currentTemp = currentTemp,
                    morningRainChance = morningRain,
                    eveningRainChance = eveningRain,
                    maxRainChance = maxCommuteRain
                )

                return@withContext WeatherState(
                    locationName = cityName,
                    currentTempC = currentTemp,
                    feelsLikeC = currentTemp + if (currentCondition == WeatherCondition.SUNNY) 1 else 0,
                    tempHighC = tempHigh,
                    tempLowC = tempLow,
                    condition = currentCondition,
                    rainChancePercent = maxCommuteRain,
                    hourlyForecast = hourlyForecasts.ifEmpty { getDefaultHourlyForecast() },
                    insight = insight
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch live weather, falling back to local simulation", e)
        }

        // Fallback simulation if network or API unavailable
        getSimulatedWeatherState(cityName)
    }

    private suspend fun resolveBestLocation(): Triple<Double, Double, String> {
        // 1. Try GPS Location if permissions granted
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            try {
                @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
                val loc: Location? = suspendCancellableCoroutine { continuation ->
                    fusedLocationClient.lastLocation
                        .addOnSuccessListener { location ->
                            if (continuation.isActive) continuation.resume(location, onCancellation = null)
                        }
                        .addOnFailureListener {
                            if (continuation.isActive) continuation.resume(null, onCancellation = null)
                        }
                }
                if (loc != null && loc.latitude != 0.0 && loc.longitude != 0.0) {
                    val cityName = reverseGeocode(loc.latitude, loc.longitude) ?: DEFAULT_CITY
                    return Triple(loc.latitude, loc.longitude, cityName)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get last GPS location", e)
            }
        }

        // 2. Try Office Config lat/lon from database
        try {
            val db = AppDatabase.getInstance(context)
            val config = db.officeConfigDao().getConfig()
            if (config != null && config.latitude != 0.0 && config.longitude != 0.0) {
                val cityName = reverseGeocode(config.latitude, config.longitude) ?: "Office Area"
                return Triple(config.latitude, config.longitude, cityName)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read office config location", e)
        }

        // 3. Smart default fallback
        return Triple(DEFAULT_LAT, DEFAULT_LON, DEFAULT_CITY)
    }

    private fun reverseGeocode(lat: Double, lon: Double): String? {
        return try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: addr.featureName
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun parseWmoWeatherCode(code: Int): WeatherCondition {
        return when (code) {
            0 -> WeatherCondition.SUNNY
            1, 2 -> WeatherCondition.PARTLY_CLOUDY
            3 -> WeatherCondition.CLOUDY
            45, 48 -> WeatherCondition.CLOUDY
            51, 53, 55, 61, 63, 65, 80, 81, 82 -> WeatherCondition.RAINY
            95, 96, 99 -> WeatherCondition.THUNDERSTORM
            else -> WeatherCondition.PARTLY_CLOUDY
        }
    }

    private fun generateTravelInsight(
        currentCondition: WeatherCondition,
        currentTemp: Int,
        morningRainChance: Int,
        eveningRainChance: Int,
        maxRainChance: Int
    ): TravelInsight {
        return when {
            currentCondition == WeatherCondition.THUNDERSTORM || maxRainChance >= 70 -> {
                TravelInsight(
                    headline = "Severe Weather Alert",
                    detail = "Heavy rain expected during commute hours ($maxRainChance% chance). Expect traffic delays.",
                    umbrellaNeeded = true,
                    recommendedTransport = "Cab / Metro Preferred",
                    travelSafetyScore = "Rain Warning"
                )
            }
            eveningRainChance >= 45 -> {
                TravelInsight(
                    headline = "Evening Commute Rain Alert",
                    detail = "Shower probability around 5 PM - 6 PM ($eveningRainChance%). Carry rain protection.",
                    umbrellaNeeded = true,
                    recommendedTransport = "Covered Vehicle",
                    travelSafetyScore = "Umbrella Advised"
                )
            }
            morningRainChance >= 45 -> {
                TravelInsight(
                    headline = "Morning Commute Rain Alert",
                    detail = "Morning showers expected ($morningRainChance%). Leave 15 mins early for work.",
                    umbrellaNeeded = true,
                    recommendedTransport = "Cab / Metro",
                    travelSafetyScore = "Early Start"
                )
            }
            currentTemp >= 34 -> {
                TravelInsight(
                    headline = "High Temperature Warning",
                    detail = "Peak heat of $currentTemp°C today. Stay hydrated and use air-conditioned transit.",
                    umbrellaNeeded = false,
                    recommendedTransport = "AC Transit",
                    travelSafetyScore = "High Heat"
                )
            }
            else -> {
                TravelInsight(
                    headline = "Optimal Commute Conditions",
                    detail = "Pleasant weather along your route. Roads and commute visibility are clear.",
                    umbrellaNeeded = false,
                    recommendedTransport = "Smooth Commute",
                    travelSafetyScore = "Clear Travel"
                )
            }
        }
    }

    private fun getSimulatedWeatherState(cityName: String): WeatherState {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val isRainyTime = hour in 16..19

        val cond = if (isRainyTime) WeatherCondition.RAINY else WeatherCondition.PARTLY_CLOUDY
        val temp = if (hour in 12..16) 28 else 24
        val rainChance = if (isRainyTime) 65 else 15

        return WeatherState(
            locationName = cityName,
            currentTempC = temp,
            feelsLikeC = temp + 1,
            tempHighC = 29,
            tempLowC = 20,
            condition = cond,
            rainChancePercent = rainChance,
            hourlyForecast = getDefaultHourlyForecast(),
            insight = generateTravelInsight(cond, temp, 10, rainChance, rainChance)
        )
    }

    private fun getDefaultHourlyForecast(): List<HourlyCommuteForecast> {
        return listOf(
            HourlyCommuteForecast("8 AM", 23, WeatherCondition.PARTLY_CLOUDY, 10, isPeakCommute = true),
            HourlyCommuteForecast("9 AM", 25, WeatherCondition.SUNNY, 10, isPeakCommute = true),
            HourlyCommuteForecast("1 PM", 28, WeatherCondition.SUNNY, 20, isPeakCommute = false),
            HourlyCommuteForecast("5 PM", 27, WeatherCondition.RAINY, 60, isPeakCommute = true),
            HourlyCommuteForecast("6 PM", 25, WeatherCondition.RAINY, 55, isPeakCommute = true),
            HourlyCommuteForecast("8 PM", 23, WeatherCondition.PARTLY_CLOUDY, 15, isPeakCommute = false)
        )
    }
}
