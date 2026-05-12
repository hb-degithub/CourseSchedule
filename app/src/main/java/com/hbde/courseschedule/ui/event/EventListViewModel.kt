package com.hbde.courseschedule.ui.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hbde.courseschedule.data.model.Event
import com.hbde.courseschedule.data.model.EventPriority
import com.hbde.courseschedule.data.model.EventType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

sealed class EventFilter {
    data object All : EventFilter()
    data object Exam : EventFilter()
    data object Homework : EventFilter()
    data object Custom : EventFilter()
}

data class EventListUiState(
    val events: List<Event> = emptyList(),
    val isLoading: Boolean = false,
    val selectedFilter: EventFilter = EventFilter.All,
    val errorMessage: String? = null
)

@HiltViewModel
class EventListViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(EventListUiState())
    val uiState: StateFlow<EventListUiState> = _uiState.asStateFlow()

    // TODO: 替换为从 Repository 获取真实数据
    private val _eventsFlow = MutableStateFlow(generateFakeEvents())

    init {
        viewModelScope.launch {
            _eventsFlow.collect { events ->
                _uiState.value = _uiState.value.copy(events = events)
            }
        }
    }

    fun selectFilter(filter: EventFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
        // TODO: 根据筛选条件过滤 events
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            // TODO: 调用 Repository 刷新数据
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun onEventCompleted(eventId: Int, completed: Boolean) {
        // TODO: 更新事件完成状态
    }

    fun deleteEvent(eventId: Int) {
        // TODO: 删除事件
    }

    private fun generateFakeEvents(): List<Event> {
        val now = LocalDateTime.now()
        return listOf(
            Event(
                id = 1,
                title = "高等数学期中考试",
                type = EventType.EXAM,
                location = "教学楼 A-101",
                startTime = now.plusDays(3).withHour(9).withMinute(0),
                endTime = now.plusDays(3).withHour(11).withMinute(0),
                reminderMinutes = 30,
                priority = EventPriority.HIGH,
                notes = "带学生证和计算器",
                isCompleted = false
            ),
            Event(
                id = 2,
                title = "英语作文作业",
                type = EventType.HOMEWORK,
                location = null,
                startTime = now.plusDays(1).withHour(23).withMinute(59),
                endTime = null,
                reminderMinutes = 60,
                priority = EventPriority.MEDIUM,
                notes = "不少于500词",
                isCompleted = false
            ),
            Event(
                id = 3,
                title = "小组讨论",
                type = EventType.CUSTOM,
                location = "图书馆 3楼",
                startTime = now.plusDays(2).withHour(14).withMinute(0),
                endTime = now.plusDays(2).withHour(16).withMinute(0),
                reminderMinutes = 15,
                priority = EventPriority.LOW,
                notes = null,
                isCompleted = true
            ),
            Event(
                id = 4,
                title = "物理实验报告",
                type = EventType.HOMEWORK,
                location = null,
                startTime = now.plusDays(5).withHour(12).withMinute(0),
                endTime = null,
                reminderMinutes = 120,
                priority = EventPriority.HIGH,
                notes = "包含数据分析和图表",
                isCompleted = false
            )
        )
    }
}
