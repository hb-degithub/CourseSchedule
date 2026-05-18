package com.hbde.courseschedule.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hbde.courseschedule.data.local.SettingsDataStore
import com.hbde.courseschedule.data.local.dao.TimeTableDao
import com.hbde.courseschedule.data.local.entity.CourseEntity
import com.hbde.courseschedule.data.local.entity.TimeSlot
import com.hbde.courseschedule.data.local.entity.isVisibleInWeek
import com.hbde.courseschedule.data.local.entity.toTimeSlotList
import com.hbde.courseschedule.data.model.CourseStatus
import com.hbde.courseschedule.data.model.Event
import com.hbde.courseschedule.data.repository.CourseRepository
import com.hbde.courseschedule.data.repository.EventRepository
import com.hbde.courseschedule.utils.CourseStatusCalculator
import com.hbde.courseschedule.utils.TermWeekCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class ScheduleWithEventsUiState(
    val currentWeek: Int = 1,
    val courses: List<CourseEntity> = emptyList(),
    val courseStatus: CourseStatus = CourseStatus.NoClass,
    val timeSlots: List<TimeSlot> = CourseStatusCalculator.DEFAULT_TIME_SLOTS,
    val todayEvents: List<Event> = emptyList(),
    val upcomingExams: List<Event> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class ScheduleWithEventsViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val eventRepository: EventRepository,
    private val timeTableDao: TimeTableDao,
    private val settingsDataStore: SettingsDataStore,
) : ViewModel() {

    private val _currentWeek = MutableStateFlow(TermWeekCalculator.calculateCurrentWeek(null))
    private val _allCourses = MutableStateFlow<List<CourseEntity>>(emptyList())
    private val _timeSlots = MutableStateFlow<List<TimeSlot>>(CourseStatusCalculator.DEFAULT_TIME_SLOTS)
    private val _todayEvents = MutableStateFlow<List<Event>>(emptyList())
    private val _upcomingExams = MutableStateFlow<List<Event>>(emptyList())

    private val _uiState = MutableStateFlow(ScheduleWithEventsUiState())
    val uiState: StateFlow<ScheduleWithEventsUiState> = _uiState.asStateFlow()

    init {
        loadDefaultTimeTable()

        // 合并所有数据流
        combine(
            _currentWeek,
            _allCourses,
            _timeSlots,
            _todayEvents,
            _upcomingExams
        ) { week, courses, timeSlots, todayEvents, upcomingExams ->
            val todayDow = CourseStatusCalculator.getTodayDayOfWeek()
            val filtered = courses.filter { it.isVisibleInWeek(week) }
            val todayCourses = filtered.filter { it.dayOfWeek == todayDow }
            val status = CourseStatusCalculator.calculateCurrentStatus(todayCourses, timeSlots)
            ScheduleWithEventsUiState(
                currentWeek = week,
                courses = filtered,
                courseStatus = status,
                timeSlots = timeSlots,
                todayEvents = todayEvents,
                upcomingExams = upcomingExams,
                isLoading = false
            )
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)

        loadCourses()
        loadTodayEvents()
        loadUpcomingExams()
        observeTermStartDate()
        startPeriodicRefresh()
    }

    private fun observeTermStartDate() {
        settingsDataStore.termStartDate
            .onEach { termStartDate ->
                _currentWeek.value = TermWeekCalculator.calculateCurrentWeek(termStartDate)
                refreshStatus()
            }
            .launchIn(viewModelScope)
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

    private fun loadTodayEvents() {
        val todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val todayEnd = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

        eventRepository.getEventsByTimeRange(todayStart, todayEnd)
            .onEach { events ->
                _todayEvents.value = events.sortedBy { it.startTime }
            }
            .launchIn(viewModelScope)
    }

    private fun loadUpcomingExams() {
        val now = System.currentTimeMillis()
        eventRepository.getUpcomingExams(now)
            .onEach { exams ->
                _upcomingExams.value = exams.take(3)
            }
            .launchIn(viewModelScope)
    }

    fun toggleEventCompletion(eventId: Int) {
        viewModelScope.launch {
            val event = _todayEvents.value.find { it.id == eventId }
            event?.let {
                eventRepository.toggleEventCompletion(eventId, !it.isCompleted)
            }
        }
    }

    private fun startPeriodicRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(60000)
                refreshStatus()
            }
        }
    }

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
        loadTodayEvents()
        loadUpcomingExams()
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
        viewModelScope.launch {
            val termStartDate = settingsDataStore.termStartDate.first()
            _currentWeek.value = TermWeekCalculator.calculateCurrentWeek(termStartDate)
            refreshStatus()
        }
    }

    companion object {
        @Deprecated("Use TermWeekCalculator with SettingsDataStore.termStartDate instead.")
        fun calculateCurrentWeek(): Int {
            return TermWeekCalculator.calculateCurrentWeek(null)
        }
    }
}
