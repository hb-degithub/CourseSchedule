package com.hbde.courseschedule.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hbde.courseschedule.data.local.dao.TimeTableDao
import com.hbde.courseschedule.data.local.entity.CourseEntity
import com.hbde.courseschedule.data.local.entity.TimeSlot
import com.hbde.courseschedule.data.local.entity.toTimeSlotList
import com.hbde.courseschedule.data.model.CourseStatus
import com.hbde.courseschedule.data.repository.CourseRepository
import com.hbde.courseschedule.utils.CourseStatusCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class WeekType {
    ALL, ODD, EVEN
}

data class ScheduleUiState(
    val currentWeek: Int = 1,
    val courses: List<CourseEntity> = emptyList(),
    val courseStatus: CourseStatus = CourseStatus.NoClass,
    val timeSlots: List<TimeSlot> = CourseStatusCalculator.DEFAULT_TIME_SLOTS,
    val isLoading: Boolean = false
)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val timeTableDao: TimeTableDao
) : ViewModel() {

    private val _currentWeek = MutableStateFlow(calculateCurrentWeek())
    private val _allCourses = MutableStateFlow<List<CourseEntity>>(emptyList())
    private val _timeSlots = MutableStateFlow<List<TimeSlot>>(CourseStatusCalculator.DEFAULT_TIME_SLOTS)

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        // 加载默认作息时间表
        loadDefaultTimeTable()

        // 合并 currentWeek 和 allCourses，自动过滤当前周课程
        combine(_currentWeek, _allCourses, _timeSlots) { week, courses, timeSlots ->
            val todayDow = CourseStatusCalculator.getTodayDayOfWeek()
            val filtered = courses.filter { it.isVisibleInWeek(week) }
            val todayCourses = filtered.filter { it.dayOfWeek == todayDow }
            val status = CourseStatusCalculator.calculateCurrentStatus(todayCourses, timeSlots)
            ScheduleUiState(
                currentWeek = week,
                courses = filtered,
                courseStatus = status,
                timeSlots = timeSlots,
                isLoading = false
            )
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)

        // 从 Repository 加载课程
        loadCourses()

        // 启动定时刷新（每分钟）
        startPeriodicRefresh()
    }

    private fun loadDefaultTimeTable() {
        viewModelScope.launch {
            try {
                val timeTable = timeTableDao.getDefaultTimeTable()
                timeTable?.let {
                    val slots = it.timeSlots.toTimeSlotList()
                    if (slots.isNotEmpty()) {
                        _timeSlots.value = slots
                    }
                }
            } catch (_: Exception) {
                // 使用默认时间表
            }
        }
    }

    private fun loadCourses() {
        _uiState.update { it.copy(isLoading = true) }
        courseRepository.getAllCourses()
            .onEach { courses ->
                _allCourses.value = courses
            }
            .launchIn(viewModelScope)
    }

    /**
     * 每分钟刷新一次课程状态
     */
    private fun startPeriodicRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(60000) // 每分钟刷新
                refreshStatus()
            }
        }
    }

    /**
     * 手动刷新当前状态（可用于 UI 触发）
     */
    fun refreshStatus() {
        val week = _currentWeek.value
        val todayDow = CourseStatusCalculator.getTodayDayOfWeek()
        val todayCourses = _allCourses.value.filter {
            it.isVisibleInWeek(week) && it.dayOfWeek == todayDow
        }
        val status = CourseStatusCalculator.calculateCurrentStatus(todayCourses, _timeSlots.value)
        _uiState.update { it.copy(courseStatus = status) }
    }

    fun refresh() {
        loadCourses()
        refreshStatus()
    }

    fun previousWeek() {
        _currentWeek.value = (_currentWeek.value - 1).coerceAtLeast(1)
        refreshStatus()
    }

    fun nextWeek() {
        _currentWeek.value = (_currentWeek.value + 1).coerceAtMost(25)
        refreshStatus()
    }

    fun currentWeek() {
        _currentWeek.value = calculateCurrentWeek()
        refreshStatus()
    }

    fun setCurrentWeek(week: Int) {
        _currentWeek.value = week.coerceIn(1, 25)
        refreshStatus()
    }

    companion object {
        /**
         * 计算当前是第几周（基于学期开始日期）
         * TODO: 从 SettingsDataStore 读取学期开始日期
         */
        fun calculateCurrentWeek(): Int {
            // 默认假设学期开始日期为当前日期所在周周一，则当前周为第 1 周
            // 后续应从设置中读取学期开始日期
            return 1
        }
    }
}

/**
 * 判断课程在某周是否可见（周范围 + 单双周判断）
 */
fun CourseEntity.isVisibleInWeek(week: Int): Boolean {
    if (week < startWeek || week > endWeek) return false
    return when (weekType.uppercase()) {
        "ODD" -> week % 2 == 1
        "EVEN" -> week % 2 == 0
        else -> true
    }
}
