package com.hbde.courseschedule.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hbde.courseschedule.data.local.dao.TimeTableDao
import com.hbde.courseschedule.data.local.entity.CourseEntity
import com.hbde.courseschedule.data.local.entity.TimeSlot
import com.hbde.courseschedule.data.local.entity.toTimeSlotList
import com.hbde.courseschedule.data.model.CourseListItemStatus
import com.hbde.courseschedule.data.repository.CourseRepository
import com.hbde.courseschedule.utils.CourseStatusCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class DayTab {
    TODAY, TOMORROW
}

/**
 * 课程列表项，包含状态和倒计时信息
 */
data class CourseWithStatus(
    val course: CourseEntity,
    val status: CourseListItemStatus = CourseListItemStatus.UPCOMING,
    val minutesUntilStart: Int = 0,
    val minutesUntilEnd: Int = 0
)

data class TwoDayScheduleUiState(
    val selectedTab: DayTab = DayTab.TODAY,
    val todayCourses: List<CourseWithStatus> = emptyList(),
    val tomorrowCourses: List<CourseWithStatus> = emptyList(),
    val timeSlots: List<TimeSlot> = CourseStatusCalculator.DEFAULT_TIME_SLOTS,
    val isLoading: Boolean = false
)

@HiltViewModel
class TwoDayScheduleViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val timeTableDao: TimeTableDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(TwoDayScheduleUiState(isLoading = true))
    val uiState: StateFlow<TwoDayScheduleUiState> = _uiState.asStateFlow()

    private val _timeSlots = MutableStateFlow<List<TimeSlot>>(CourseStatusCalculator.DEFAULT_TIME_SLOTS)

    init {
        loadDefaultTimeTable()
        loadCourses()
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

    fun selectTab(tab: DayTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun refresh() {
        loadCourses()
    }

    private fun loadCourses() {
        viewModelScope.launch {
            val todayDow = getTodayDayOfWeek()
            val tomorrowDow = if (todayDow == 7) 1 else todayDow + 1

            combine(
                courseRepository.getCoursesByDayOfWeek(todayDow),
                courseRepository.getCoursesByDayOfWeek(tomorrowDow)
            ) { todayList, tomorrowList ->
                val sortedToday = todayList.sortedBy { it.startNode }
                val sortedTomorrow = tomorrowList.sortedBy { it.startNode }

                val todayWithStatus = sortedToday.map { course ->
                    CourseWithStatus(
                        course = course,
                        status = CourseStatusCalculator.calculateItemStatus(course, _timeSlots.value),
                        minutesUntilStart = CourseStatusCalculator.calculateMinutesUntilStart(course, _timeSlots.value),
                        minutesUntilEnd = CourseStatusCalculator.calculateMinutesUntilEnd(course, _timeSlots.value)
                    )
                }

                val tomorrowWithStatus = sortedTomorrow.map { course ->
                    CourseWithStatus(
                        course = course,
                        status = CourseListItemStatus.UPCOMING, // 明天的课都是未开始
                        minutesUntilStart = 0,
                        minutesUntilEnd = 0
                    )
                }

                TwoDayScheduleUiState(
                    selectedTab = _uiState.value.selectedTab,
                    todayCourses = todayWithStatus,
                    tomorrowCourses = tomorrowWithStatus,
                    timeSlots = _timeSlots.value,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
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

    private fun refreshStatus() {
        val currentState = _uiState.value
        val updatedToday = currentState.todayCourses.map { courseWithStatus ->
            courseWithStatus.copy(
                status = CourseStatusCalculator.calculateItemStatus(courseWithStatus.course, currentState.timeSlots),
                minutesUntilStart = CourseStatusCalculator.calculateMinutesUntilStart(courseWithStatus.course, currentState.timeSlots),
                minutesUntilEnd = CourseStatusCalculator.calculateMinutesUntilEnd(courseWithStatus.course, currentState.timeSlots)
            )
        }
        _uiState.value = currentState.copy(todayCourses = updatedToday)
    }

    private fun getTodayDayOfWeek(): Int {
        val calendar = Calendar.getInstance()
        val day = calendar.get(Calendar.DAY_OF_WEEK)
        return if (day == Calendar.SUNDAY) 7 else day - 1
    }
}
