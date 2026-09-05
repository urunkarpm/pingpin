package com.urunkarpm.pingpin.ui.insights

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.urunkarpm.pingpin.data.local.AppDatabase
import com.urunkarpm.pingpin.data.local.entity.AttendanceRecordEntity
import com.urunkarpm.pingpin.data.local.entity.OfficeConfigEntity
import com.urunkarpm.pingpin.data.local.entity.UserProfileEntity
import com.urunkarpm.pingpin.data.repository.AttendanceRepository
import com.urunkarpm.pingpin.data.repository.OfficeConfigRepository
import com.urunkarpm.pingpin.data.repository.UserProfileRepository
import com.urunkarpm.pingpin.service.PdfExportService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.Calendar

class InsightsViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val db = AppDatabase.getInstance(context)
    private val officeConfigRepo = OfficeConfigRepository(db.officeConfigDao())
    private val attendanceRepo = AttendanceRepository(db.attendanceRecordDao())
    private val profileRepo = UserProfileRepository(db.userProfileDao())
    val pdfExportService = PdfExportService(context)

    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    val configState: StateFlow<OfficeConfigEntity?> = officeConfigRepo.configFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val profileState: StateFlow<UserProfileEntity?> = profileRepo.profileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthlyRecords: StateFlow<List<AttendanceRecordEntity>> = combine(selectedYear, selectedMonth) { year, month ->
        Pair(year, month)
    }.flatMapLatest { (year, month) ->
        attendanceRepo.watchForMonth(year, month)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedMonthYear(year: Int, month: Int) {
        _selectedYear.value = year
        _selectedMonth.value = month
    }

    fun previousMonth() {
        if (_selectedMonth.value == 1) {
            _selectedMonth.value = 12
            _selectedYear.value -= 1
        } else {
            _selectedMonth.value -= 1
        }
    }

    fun nextMonth() {
        if (_selectedMonth.value == 12) {
            _selectedMonth.value = 1
            _selectedYear.value += 1
        } else {
            _selectedMonth.value += 1
        }
    }

    suspend fun generatePdfStatement(): File {
        val year = selectedYear.value
        val month = selectedMonth.value
        val profile = profileRepo.getProfile() ?: profileState.value ?: UserProfileEntity()
        val records = monthlyRecords.value
        val config = configState.value
        val workingDaysMask = config?.workingDaysMask ?: 31
        val wfoDaysMask = config?.wfoDaysMask ?: 31

        return pdfExportService.generateAttendancePdf(
            year = year,
            month = month,
            profile = profile,
            records = records,
            workingDaysMask = workingDaysMask,
            wfoDaysMask = wfoDaysMask,
            officeConfig = config
        )
    }
}
