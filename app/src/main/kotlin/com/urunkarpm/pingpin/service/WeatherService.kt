package com.urunkarpm.pingpin.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.urunkarpm.pingpin.data.local.AppDatabase
import com.urunkarpm.pingpin.data.model.ForecastStatus
import com.urunkarpm.pingpin.data.model.HourlyCommuteForecast
import com.urunkarpm.pingpin.data.model.LocationStatus
import com.urunkarpm.pingpin.data.model.RadarStatus
import com.urunkarpm.pingpin.data.model.TravelInsight
import com.urunkarpm.pingpin.data.model.WeatherCondition
import com.urunkarpm.pingpin.data.model.WeatherState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class WeatherService(private val context: Context) {

    companion object {
        private const val TAG = "WeatherService"
        // Default coordinates: Bengaluru, India (if no location or office config available)
        private const val DEFAULT_LAT = 12.9716
        private const val DEFAULT_LON = 77.5946
        private const val DEFAULT_CITY = "Bengaluru"
    }

    private val locationManager by lazy { context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager }
    private var cachedState: WeatherState? = null

    private data class LocationResult(
        val lat: Double,
        val lon: Double,
        val cityName: String,
        val status: LocationStatus
    )

    suspend fun fetchWeatherAndTravelInsights(
        checkInTimeStr: String? = null,
        checkOutTimeStr: String? = null
    ): WeatherState = withContext(Dispatchers.IO) {
        val locResult = resolveBestLocation()
        val lat = locResult.lat
        val lon = locResult.lon
        val cityName = locResult.cityName

        val checkInHour = parseHourFromTimeString(checkInTimeStr) ?: 9
        val checkOutHour = parseHourFromTimeString(checkOutTimeStr) ?: 17

        val morningPeakHours = setOf(maxOf(0, checkInHour - 1), checkInHour)
        val eveningPeakHours = setOf(checkOutHour, minOf(23, checkOutHour + 1))

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
                        val hourInt = if (timeStr.length >= 13) {
                            timeStr.substring(11, 13).toIntOrNull() ?: i
                        } else i

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

                        if (hourInt in morningPeakHours) morningRain = maxOf(morningRain, rainChance)
                        if (hourInt in eveningPeakHours) eveningRain = maxOf(eveningRain, rainChance)

                        val isPeak = hourInt in morningPeakHours || hourInt in eveningPeakHours
                        val tag = when (hourInt) {
                            checkInHour -> "Check-in"
                            checkOutHour -> "Check-out"
                            else -> null
                        }

                        hourlyForecasts.add(
                            HourlyCommuteForecast(
                                timeLabel = formattedTime,
                                tempC = tempVal,
                                condition = cond,
                                rainChancePercent = rainChance,
                                isPeakCommute = isPeak,
                                commuteTag = tag
                            )
                        )
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

                val forecastStatus = if (hourlyForecasts.isEmpty()) ForecastStatus.UNAVAILABLE else ForecastStatus.AVAILABLE
                val radarStatus = if (hourlyForecasts.size < 2) RadarStatus.UNAVAILABLE else RadarStatus.AVAILABLE

                val newState = WeatherState(
                    locationName = cityName,
                    currentTempC = currentTemp,
                    feelsLikeC = currentTemp + if (currentCondition == WeatherCondition.SUNNY) 1 else 0,
                    tempHighC = tempHigh,
                    tempLowC = tempLow,
                    condition = currentCondition,
                    rainChancePercent = maxCommuteRain,
                    hourlyForecast = hourlyForecasts,
                    insight = insight,
                    lastUpdatedMillis = System.currentTimeMillis(),
                    hasValidData = true,
                    isInitialLoading = false,
                    isRefreshing = false,
                    isError = false,
                    errorMessage = null,
                    isStale = false,
                    staleAgeMinutes = 0L,
                    locationStatus = locResult.status,
                    forecastStatus = forecastStatus,
                    radarStatus = radarStatus
                )
                cachedState = newState
                return@withContext newState
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch live weather", e)
        }

        // Check if cached state exists for offline/stale handling
        val cached = cachedState
        if (cached != null) {
            val ageMs = System.currentTimeMillis() - cached.lastUpdatedMillis
            val ageMins = maxOf(1L, ageMs / 60000L)
            return@withContext cached.copy(
                isStale = true,
                staleAgeMinutes = ageMins,
                isRefreshing = false,
                isError = false,
                errorMessage = "Offline • Displaying cached weather"
            )
        }

        // Return clean error state when live weather API call fails without cached values
        WeatherState(
            locationName = cityName,
            hasValidData = false,
            isInitialLoading = false,
            isRefreshing = false,
            isError = true,
            errorMessage = "Live weather data unavailable. Check network connection.",
            locationStatus = locResult.status,
            forecastStatus = ForecastStatus.UNAVAILABLE,
            radarStatus = RadarStatus.UNAVAILABLE
        )
    }

    private fun parseHourFromTimeString(timeStr: String?): Int? {
        if (timeStr.isNullOrBlank()) return null
        return try {
            val parts = timeStr.trim().split(":")
            parts[0].toIntOrNull()
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun resolveBestLocation(): LocationResult {
        // 1. Check GPS Location permissions
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            try {
                val lm = locationManager
                val gpsLoc = if (hasFine && lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                    lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                } else null

                val netLoc = if (lm?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) {
                    lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                } else null

                val loc = when {
                    gpsLoc != null && netLoc != null -> if (gpsLoc.time > netLoc.time) gpsLoc else netLoc
                    gpsLoc != null -> gpsLoc
                    else -> netLoc
                }

                if (loc != null && loc.latitude != 0.0 && loc.longitude != 0.0) {
                    val cityName = reverseGeocode(loc.latitude, loc.longitude) ?: DEFAULT_CITY
                    return LocationResult(loc.latitude, loc.longitude, cityName, LocationStatus.RESOLVED)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get last GPS location", e)
            }
        }

        // If location permission denied or GPS failed, try office config
        val isPermDenied = !hasFine && !hasCoarse
        try {
            val db = AppDatabase.getInstance(context)
            val config = db.officeConfigDao().getConfig()
            if (config != null && config.latitude != 0.0 && config.longitude != 0.0) {
                val cityName = reverseGeocode(config.latitude, config.longitude) ?: "Office Area"
                val status = if (isPermDenied) LocationStatus.PERMISSION_DENIED else LocationStatus.OFFICE_FALLBACK
                return LocationResult(config.latitude, config.longitude, cityName, status)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read office config location", e)
        }

        // Default fallback
        val defaultStatus = if (isPermDenied) LocationStatus.PERMISSION_DENIED else LocationStatus.LOCATION_UNAVAILABLE
        return LocationResult(DEFAULT_LAT, DEFAULT_LON, DEFAULT_CITY, defaultStatus)
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

    internal fun parseWmoWeatherCode(code: Int): WeatherCondition {
        return when (code) {
            0 -> WeatherCondition.SUNNY
            1, 2 -> WeatherCondition.PARTLY_CLOUDY
            3 -> WeatherCondition.CLOUDY
            45, 48 -> WeatherCondition.CLOUDY
            51, 53, 55, 61, 63, 80, 81 -> WeatherCondition.RAINY
            65, 82 -> WeatherCondition.HEAVY_RAIN
            95, 96, 99 -> WeatherCondition.THUNDERSTORM
            else -> WeatherCondition.PARTLY_CLOUDY
        }
    }

    internal fun generateTravelInsight(
        currentCondition: WeatherCondition,
        currentTemp: Int,
        morningRainChance: Int,
        eveningRainChance: Int,
        maxRainChance: Int
    ): TravelInsight {
        return when {
            currentCondition == WeatherCondition.THUNDERSTORM || currentCondition == WeatherCondition.HEAVY_RAIN || maxRainChance >= 70 -> {
                TravelInsight(
                    headline = if (currentCondition == WeatherCondition.THUNDERSTORM) "Storm Warning" else "Heavy Rain Alert",
                    detail = "Heavy rain expected during commute hours ($maxRainChance% chance). Expect traffic delays.",
                    umbrellaNeeded = true,
                    recommendedTransport = "Cab / Metro Preferred",
                    travelSafetyScore = if (currentCondition == WeatherCondition.THUNDERSTORM) "Storm Warning" else "Heavy Rain"
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
}
