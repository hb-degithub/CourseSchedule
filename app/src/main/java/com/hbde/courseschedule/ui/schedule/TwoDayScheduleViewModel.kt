package com.hbde.courseschedule.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hbde.courseschedule.data.local.entity.CourseEntity
import com.hbde.courseschedule.data.repository.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class DayTab {
    TODAY, TOMORROW
}

data class CourseWithStatus(
    val course: CourseEntity,
    val isCurrent: Boolean = false
)

data class TwoDayScheduleUiState(
    val selectedTab: DayTab = DayTab.TODAY,
    val todayCourses: List<CourseWithStatus> = emptyList(),
    val tomorrowCourses: List<CourseWithStatus> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class TwoDayScheduleViewModel @Inject constructor(
    private val courseRepository: CourseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TwoDayScheduleUiState(isLoading = true))
    val uiState: StateFlow<TwoDayScheduleUiState> = _uiState.asStateFlow()

    init {
        loadCourses()
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
                        isCurrent = isCourseCurrentlyActive(course)
                    )
                }

                val tomorrowWithStatus = sortedTomorrow.map { course ->
                    CourseWithStatus(
                        course = course,
                        isCurrent = false
                    )
                }

                TwoDayScheduleUiState(
                    selectedTab = _uiState.value.selectedTab,
                    todayCourses = todayWithStatus,
                    tomorrowCourses = tomorrowWithStatus,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    /**
     * Check if a course is currently active based on the current time.
     * This is a simplified check based on startNode/endNode.
     * TODO: Integrate with TimeTableEntity for accurate time mapping.
     */
    private fun isCourseCurrentlyActive(course: CourseEntity): Boolean {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentTimeMinutes = currentHour * 60 + currentMinute

        // Approximate time mapping for nodes (standard Chinese university schedule)
        // TODO: Replace with actual TimeTable lookup when available
        val nodeStartTimes = mapOf(
            1 to 8 * 60 + 0,    // 08:00
            2 to 8 * 60 + 50,   // 08:50
            3 to 10 * 60 + 10,  // 10:10
            4 to 11 * 60 + 0,   // 11:00
            5 to 14 * 60 + 0,   // 14:00
            6 to 14 * 60 + 50,  // 14:50
            7 to 16 * 60 + 10,  // 16:10
            8 to 17 * 60 + 0,   // 17:00
            9 to 19 * 60 + 0,   // 19:00
            10 to 19 * 60 + 50, // 19:50
            11 to 20 * 60 + 40, // 20:40
            12 to 21 * 60 + 30  // 21:30
        )

        val nodeEndTimes = mapOf(
            1 to 8 * 60 + 45,   // 08:45
            2 to 9 * 60 + 35,   // 09:35
            3 to 10 * 60 + 55,  // 10:55
            4 to 11 * 60 + 45,  // 11:45
            5 to 14 * 60 + 45,  // 14:45
            6 to 15 * 60 + 35,  // 15:35
            7 to 16 * 60 + 55,  // 16:55
            8 to 17 * 60 + 45,  // 17:45
            9 to 19 * 60 + 45,  // 19:45
            10 to 20 * 60 + 35, // 20:35
            11 to 21 * 60 + 25, // 21:25
            12 to 22 * 60 + 15  // 22:15
        )

        val startTime = nodeStartTimes[course.startNode] ?: return false
        val endTime = nodeEndTimes[course.endNode] ?: return false

        return currentTimeMinutes in startTime..endTime
    }

    private fun getTodayDayOfWeek(): Int {
        val calendar = Calendar.getInstance()
        val day = calendar.get(Calendar.DAY_OF_WEEK)
        return if (day == Calendar.SUNDAY) 7 else day - 1
    }
}
