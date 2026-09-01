package com.urunkarpm.pingpin.service

import android.util.Log
import com.urunkarpm.pingpin.data.model.HolidayCategory
import com.urunkarpm.pingpin.data.model.IndianHoliday
import com.urunkarpm.pingpin.data.model.UpcomingHolidayData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

open class IndianHolidayService {

    companion object {
        private const val TAG = "IndianHolidayService"
        const val BASE_URL = "https://holiday2api.vercel.app"
        private const val CONNECT_TIMEOUT_MS = 6000
        private const val READ_TIMEOUT_MS = 6000

        fun mapTypeToCategory(typeStr: String?, stateCode: String?): HolidayCategory {
            val normalized = (typeStr ?: "").trim().lowercase(Locale.ROOT)
            return when {
                normalized.contains("national") -> HolidayCategory.NATIONAL
                normalized.contains("restricted") || normalized.contains("optional") -> HolidayCategory.RESTRICTED
                normalized.contains("regional") -> HolidayCategory.REGIONAL
                normalized.contains("gazetted") || normalized.contains("public") -> {
                    if (stateCode != null && stateCode != "IN" && stateCode.isNotEmpty()) {
                        HolidayCategory.GAZETTED
                    } else {
                        HolidayCategory.GAZETTED
                    }
                }
                else -> HolidayCategory.GAZETTED
            }
        }

        fun parseHolidaysJson(jsonString: String): List<IndianHoliday> {
            val list = mutableListOf<IndianHoliday>()
            val jsonArray = JSONArray(jsonString)
            val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val sdfDay = SimpleDateFormat("EEEE", Locale.US)

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i) ?: continue
                val date = obj.optString("date", "").trim()
                if (date.isEmpty()) continue

                val name = obj.optString("name", "Holiday").trim()
                val type = obj.optString("type", "public").trim()
                val stateCode = obj.optString("state_code", "").trim()
                val description = obj.optString("description", "").trim()

                var dayOfWeek = obj.optString("day_of_week", "").trim()
                if (dayOfWeek.isEmpty()) {
                    try {
                        val parsed = sdfDate.parse(date)
                        if (parsed != null) {
                            dayOfWeek = sdfDay.format(parsed)
                        }
                    } catch (_: Exception) {
                        dayOfWeek = "Day"
                    }
                }

                val cleanId = "api_${date}_${name.replace(Regex("[^a-zA-Z0-9]"), "_").lowercase(Locale.ROOT)}"
                val category = mapTypeToCategory(type, stateCode)

                list.add(
                    IndianHoliday(
                        id = cleanId,
                        name = name,
                        dateYyyyMmDd = date,
                        dayOfWeek = dayOfWeek,
                        category = category,
                        description = description.ifEmpty { "$name observed in India." }
                    )
                )
            }
            return list.sortedBy { it.dateYyyyMmDd }
        }
    }

    @Volatile
    private var activeHolidays: List<IndianHoliday> = emptyList()

    open fun setHolidays(holidays: List<IndianHoliday>) {
        activeHolidays = holidays
    }

    open fun getAllHolidays(): List<IndianHoliday> {
        return activeHolidays.sortedBy { it.dateYyyyMmDd }
    }

    open fun getHolidaysFromPresent(fromDate: Calendar = Calendar.getInstance()): List<IndianHoliday> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = sdf.format(fromDate.time)
        return activeHolidays.filter { it.dateYyyyMmDd >= todayStr }.sortedBy { it.dateYyyyMmDd }
    }

    fun calculateDaysRemaining(
        dateYyyyMmDd: String,
        fromDate: Calendar = Calendar.getInstance()
    ): Int {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val targetDate = sdf.parse(dateYyyyMmDd) ?: return 0
        val startCal = (fromDate.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val targetCal = Calendar.getInstance().apply {
            time = targetDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMillis = targetCal.timeInMillis - startCal.timeInMillis
        return (diffMillis / (1000 * 60 * 60 * 24)).toInt()
    }

    fun getUpcomingHolidays(
        fromDate: Calendar = Calendar.getInstance(),
        daysAhead: Int = 21
    ): List<UpcomingHolidayData> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val startCal = (fromDate.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endCal = (startCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, daysAhead)
        }

        val result = mutableListOf<UpcomingHolidayData>()

        for (holiday in activeHolidays) {
            val parsedDate = sdf.parse(holiday.dateYyyyMmDd) ?: continue
            val holidayCal = Calendar.getInstance().apply {
                time = parsedDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (!holidayCal.before(startCal) && !holidayCal.after(endCal)) {
                val diffMillis = holidayCal.timeInMillis - startCal.timeInMillis
                val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

                val relativeTag = when (diffDays) {
                    0 -> "Today!"
                    1 -> "Tomorrow!"
                    in 2..6 -> "In $diffDays days"
                    7 -> "In 1 week"
                    in 8..13 -> "In $diffDays days"
                    14 -> "In 2 weeks"
                    in 15..20 -> "In $diffDays days"
                    21 -> "In 3 weeks"
                    else -> "In $diffDays days"
                }

                result.add(UpcomingHolidayData(holiday, diffDays, relativeTag))
            }
        }

        return result.sortedBy { it.daysRemaining }
    }

    fun getHolidayForDate(dateYyyyMmDd: String): IndianHoliday? {
        return activeHolidays.find { it.dateYyyyMmDd == dateYyyyMmDd }
    }

    fun getUpcomingLongWeekends(): List<IndianHoliday> {
        return activeHolidays.filter { it.isLongWeekend }
    }

    /**
     * Fetches holidays dynamically from holiday2api.vercel.app for a specific year and optional state code (e.g., "KA", "MH", "DL").
     */
    suspend fun fetchHolidays(
        year: Int,
        stateCode: String? = null
    ): Result<List<IndianHoliday>> = withContext(Dispatchers.IO) {
        try {
            val urlString = if (stateCode.isNullOrEmpty() || stateCode.equals("IN", ignoreCase = true)) {
                "$BASE_URL/api/holidays/$year"
            } else {
                "$BASE_URL/api/holidays/$year/${stateCode.uppercase(Locale.ROOT)}"
            }

            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "PingPin-Android-App/2.2.0")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val parsed = parseHolidaysJson(responseText)
                if (parsed.isNotEmpty()) {
                    Result.success(parsed)
                } else {
                    Result.failure(IllegalStateException("Empty holiday list returned from API"))
                }
            } else {
                Result.failure(IllegalStateException("HTTP error $responseCode from $urlString"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch holidays from holiday2api: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches upcoming holidays dynamically from holiday2api.vercel.app.
     */
    suspend fun fetchUpcomingHolidays(
        stateCode: String? = null,
        limit: Int = 10
    ): Result<List<UpcomingHolidayData>> = withContext(Dispatchers.IO) {
        try {
            val stateParam = if (!stateCode.isNullOrEmpty()) "&state=${stateCode.uppercase(Locale.ROOT)}" else ""
            val urlString = "$BASE_URL/api/holidays/upcoming?limit=$limit$stateParam"

            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "PingPin-Android-App/2.2.0")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val parsedHolidays = parseHolidaysJson(responseText)
                val jsonArray = JSONArray(responseText)

                val result = mutableListOf<UpcomingHolidayData>()
                for (i in 0 until minOf(parsedHolidays.size, jsonArray.length())) {
                    val h = parsedHolidays[i]
                    val obj = jsonArray.optJSONObject(i)
                    val daysUntil = obj?.optInt("days_until", -1) ?: -1
                    val diffDays = if (daysUntil >= 0) daysUntil else calculateDaysRemaining(h.dateYyyyMmDd)

                    val relativeTag = when (diffDays) {
                        0 -> "Today!"
                        1 -> "Tomorrow!"
                        in 2..6 -> "In $diffDays days"
                        7 -> "In 1 week"
                        in 8..13 -> "In $diffDays days"
                        14 -> "In 2 weeks"
                        in 15..20 -> "In $diffDays days"
                        21 -> "In 3 weeks"
                        else -> "In $diffDays days"
                    }
                    result.add(UpcomingHolidayData(h, diffDays, relativeTag))
                }
                Result.success(result.sortedBy { it.daysRemaining })
            } else {
                Result.failure(IllegalStateException("HTTP error $responseCode from $urlString"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch upcoming holidays from holiday2api: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Syncs holidays from holiday2api in the background, updating active in-memory list if successful.
     */
    suspend fun syncHolidays(
        year: Int = Calendar.getInstance().get(Calendar.YEAR),
        stateCode: String? = null
    ): List<IndianHoliday> {
        val result = fetchHolidays(year, stateCode)
        if (result.isSuccess) {
            val list = result.getOrNull()
            if (!list.isNullOrEmpty()) {
                activeHolidays = list
                Log.i(TAG, "Successfully synced ${list.size} holidays from holiday2api for year $year (state: $stateCode)")
                return list
            }
        }
        return activeHolidays
    }
}
