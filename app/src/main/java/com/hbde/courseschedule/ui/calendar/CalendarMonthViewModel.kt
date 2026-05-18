package com.hbde.courseschedule.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hbde.courseschedule.data.local.entity.CourseEntity
import com.hbde.courseschedule.data.local.entity.isVisibleInWeek
import com.hbde.courseschedule.data.model.Event
import com.hbde.courseschedule.data.model.EventType
import com.hbde.courseschedule.data.repository.CourseRepository
import com.hbde.courseschedule.data.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class CalendarMonthUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val today: LocalDate = LocalDate.now(),
    val selectedDate: LocalDate? = null,
    val datesWithCourses: Set<LocalDate> = emptySet(),
    val datesWithEvents: Map<LocalDate, List<EventType>> = emptyMap(),
    val selectedDateCourses: List<CourseEntity> = emptyList(),
    val selectedDateEvents: List<Event> = emptyList()
)

@HiltViewModel
class CalendarMonthViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarMonthUiState())
    val uiState: StateFlow<CalendarMonthUiState> = _uiState.asStateFlow()

    init {
        // 监听课程数据
        courseRepository.getAllCourses()
            .onEach { courses ->
                updateCourseMarkers(courses)
                _uiState.value.selectedDate?.let { date ->
                    updateSelectedDateDetails(date)
                }
            }
            .launchIn(viewModelScope)

        // 监听事件数据
        eventRepository.getAllEvents()
            .onEach { events ->
                updateEventMarkers(events)
                _uiState.value.selectedDate?.let { date ->
                    updateSelectedDateDetails(date)
                }
            }
            .launchIn(viewModelScope)
    }

    fun previousMonth() {
        _uiState.update { it.copy(currentMonth = it.currentMonth.minusMonths(1)) }
    }

    fun nextMonth() {
        _uiState.update { it.copy(currentMonth = it.currentMonth.plusMonths(1)) }
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
        updateSelectedDateDetails(date)
    }

    private fun updateCourseMarkers(courses: List<CourseEntity>) {
        // 假设当前周，将课程映射到日期（简化处理：基于当前周的课程）
        val currentWeek = 1 // TODO: 从设置读取当前周
        val today = LocalDate.now()
        val weekStart = today.minusDays((today.dayOfWeek.value % 7).toLong())

        val datesWithCourses = mutableSetOf<LocalDate>()
        courses.filter { it.isVisibleInWeek(currentWeek) }.forEach { course ->
            // dayOfWeek: 1=周一, 7=周日
            val dayOffset = ((course.dayOfWeek - 1) % 7).toLong()
            val courseDate = weekStart.plusDays(dayOffset)
            datesWithCourses.add(courseDate)
        }
        _uiState.update { it.copy(datesWithCourses = datesWithCourses) }
    }

    private fun updateEventMarkers(events: List<Event>) {
        val datesWithEvents = events.groupBy(
            { it.startTime.toLocalDate() },
            { it.type }
        )
        _uiState.update { it.copy(datesWithEvents = datesWithEvents) }
    }

    private fun updateSelectedDateDetails(date: LocalDate) {
        val currentWeek = 1 // TODO: 从设置读取当前周
        val dayOfWeek = when (date.dayOfWeek.value) {
            7 -> 7 // 周日
            else -> date.dayOfWeek.value
        }

        // 获取当天课程
        // 由于 CourseRepository 没有按天查询的 Flow，我们使用 all courses 过滤
        // 实际项目中可以优化为单独查询
        courseRepository.getAllCourses()
            .onEach { allCourses ->
                val dayCourses = allCourses.filter {
                    it.dayOfWeek == dayOfWeek && it.isVisibleInWeek(currentWeek)
                }.sortedBy { it.startNode }

                _uiState.update { state ->
                    state.copy(selectedDateCourses = dayCourses)
                }
            }
            .launchIn(viewModelScope)

        // 获取当天事件
        eventRepository.getAllEvents()
            .onEach { allEvents ->
                val dayEvents = allEvents.filter {
                    it.startTime.toLocalDate() == date
                }.sortedBy { it.startTime }

                _uiState.update { state ->
                    state.copy(selectedDateEvents = dayEvents)
                }
            }
            .launchIn(viewModelScope)
    }
}
