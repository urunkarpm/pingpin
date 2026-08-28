package com.urunkarpm.pingpin.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.urunkarpm.pingpin.data.local.AppDatabase
import com.urunkarpm.pingpin.data.local.entity.AttendanceRecordEntity
import com.urunkarpm.pingpin.data.local.entity.MakeupWfoSuggestionEntity
import com.urunkarpm.pingpin.data.local.entity.OfficeConfigEntity
import com.urunkarpm.pingpin.data.model.WeatherState
import com.urunkarpm.pingpin.data.repository.AttendanceRepository
import com.urunkarpm.pingpin.data.repository.MakeupWfoRepository
import com.urunkarpm.pingpin.data.repository.OfficeConfigRepository
import com.urunkarpm.pingpin.service.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val db = AppDatabase.getInstance(context)
    private val officeConfigRepo = OfficeConfigRepository(db.officeConfigDao())
    private val attendanceRepo = AttendanceRepository(db.attendanceRecordDao())
    private val makeupRepo = MakeupWfoRepository(db.makeupWfoSuggestionDao())

    private val wifiService = WifiService(context)
    private val attendanceService = AttendanceService(context, wifiService)
    private val weatherService = WeatherService(context)
    val holidayService = IndianHolidayService()
    val notifService = NotificationService(context)
    val makeupManager = MakeupWfoManager(context, makeupRepo, attendanceRepo, wifiService, attendanceService, holidayService)

    val configState: StateFlow<OfficeConfigEntity?> = officeConfigRepo.configFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recordsState: StateFlow<List<AttendanceRecordEntity>> = attendanceRepo.watchAll()
        .onEach { list ->
            if (list.isNotEmpty()) {
                val minDate = list.minOf { it.dateYyyyMmDd }
                AppInstallManager.adjustInstallDateIfOlderRecordExists(context, minDate)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSuggestionState: StateFlow<MakeupWfoSuggestionEntity?> = makeupRepo.activeSuggestionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val acceptedMakeupDatesState: StateFlow<Set<String>> = makeupRepo.acceptedDatesFlow
        .map { list -> list.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _weatherState = MutableStateFlow(WeatherState(isInitialLoading = true, hasValidData = false))
    val weatherState: StateFlow<WeatherState> = _weatherState.asStateFlow()

    private val _isRefreshingWeather = MutableStateFlow(false)
    val isRefreshingWeather: StateFlow<Boolean> = _isRefreshingWeather.asStateFlow()

    private val isConnectedToOffice = MutableStateFlow(false)
    private var isAutoChecking = false

    val upcomingHolidays = holidayService.getUpcomingHolidays()
    val allIndianHolidays = holidayService.getAllHolidays()

    init {
        // Initial weather load
        refreshWeather()

        // Observe network state reactively via NetworkCallback Flow combined with office config
        viewModelScope.launch(Dispatchers.IO) {
            combine(wifiService.observeWifiState(), configState) { _, config ->
                config
            }.collect { config ->
                val officeSSID = config?.ssid ?: ""
                isConnectedToOffice.value = if (officeSSID.isNotEmpty()) {
                    wifiService.isConnectedToSSID(officeSSID)
                } else false
            }
        }

        // Automatic attendance check when connected to office Wi-Fi
        viewModelScope.launch(Dispatchers.IO) {
            combine(isConnectedToOffice, recordsState, configState) { isConnected, records, config ->
                Triple(isConnected, records, config)
            }.collect { (isConnected, records, config) ->
                val todayStr = AttendanceService.getCurrentDateYyyyMmDd()
                val todayRecord = records.find { it.dateYyyyMmDd == todayStr }
                if (isConnected && todayRecord == null && !isAutoChecking && config != null && config.ssid.isNotEmpty()) {
                    isAutoChecking = true
                    try {
                        val result = attendanceService.checkAndMarkAttendance(
                            officeConfig = config,
                            attendanceRepo = attendanceRepo,
                            onAttendanceMarked = {
                                notifService.showAttendanceSuccessNotification()
                            }
                        )
                        Log.d("HomeViewModel", "Auto attendance result: $result")
                    } catch (e: Exception) {
                        Log.e("HomeViewModel", "Auto-marking attendance failed", e)
                    } finally {
                        isAutoChecking = false
                    }
                }
            }
        }

        // Evaluate makeup WFO suggestions
        viewModelScope.launch(Dispatchers.IO) {
            combine(configState, recordsState) { config, _ -> config }.collect { config ->
                if (config != null) {
                    try {
                        makeupManager.evaluateAndSuggestMakeup(
                            officeConfig = config,
                            onAttendanceAutoMarked = {
                                notifService.showAttendanceSuccessNotification()
                            }
                        )
                    } catch (e: Exception) {
                        Log.e("HomeViewModel", "Error evaluating makeup WFO", e)
                    }
                }
            }
        }
    }

    fun refreshWeather() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isRefreshingWeather.value = true
                if (_weatherState.value.hasValidData) {
                    _weatherState.value = _weatherState.value.copy(isRefreshing = true)
                }
                val config = configState.value
                val fetched = weatherService.fetchWeatherAndTravelInsights(
                    checkInTimeStr = config?.checkInTime,
                    checkOutTimeStr = config?.checkOutTime
                )
                _weatherState.value = fetched
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error fetching weather", e)
                if (!_weatherState.value.hasValidData) {
                    _weatherState.value = WeatherState(
                        hasValidData = false,
                        isInitialLoading = false,
                        isError = true,
                        errorMessage = "Failed to load weather: ${e.localizedMessage ?: "Check connection"}"
                    )
                } else {
                    _weatherState.value = _weatherState.value.copy(
                        isRefreshing = false,
                        isStale = true,
                        errorMessage = "Failed to refresh weather data"
                    )
                }
            } finally {
                _isRefreshingWeather.value = false
            }
        }
    }

    fun markAttendancePresent(dateStr: String) {
        viewModelScope.launch(Dispatchers.IO) {
            attendanceRepo.insertRecord(
                dateYyyyMmDd = dateStr,
                status = "present"
            )
        }
    }

    fun deleteAttendance(dateStr: String) {
        viewModelScope.launch(Dispatchers.IO) {
            attendanceRepo.deleteByDate(dateStr)
        }
    }

    fun acceptSuggestion(suggestionId: Int, dateStr: String, alarmId: Int, portalUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            makeupRepo.updateStatus(suggestionId, "ACCEPTED")
            notifService.scheduleMakeupAlarm(
                targetDateYyyyMmDd = dateStr,
                alarmId = alarmId,
                portalUrl = portalUrl
            )
        }
    }

    fun declineSuggestion(suggestionId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            makeupRepo.updateStatus(suggestionId, "DECLINED")
        }
    }

    fun cancelSuggestionAlarm(suggestionId: Int, alarmId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            notifService.cancelAlarm(alarmId)
            makeupRepo.updateStatus(suggestionId, "DECLINED")
        }
    }
}
