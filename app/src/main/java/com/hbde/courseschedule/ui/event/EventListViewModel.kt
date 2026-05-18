package com.hbde.courseschedule.ui.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hbde.courseschedule.data.model.Event
import com.hbde.courseschedule.data.model.EventType
import com.hbde.courseschedule.data.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
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
class EventListViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventListUiState())
    val uiState: StateFlow<EventListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                eventRepository.getAllEvents(),
                _uiState
            ) { allEvents, state ->
                val filtered = when (state.selectedFilter) {
                    EventFilter.All -> allEvents
                    EventFilter.Exam -> allEvents.filter { it.type == EventType.EXAM }
                    EventFilter.Homework -> allEvents.filter { it.type == EventType.HOMEWORK }
                    EventFilter.Custom -> allEvents.filter { it.type == EventType.CUSTOM }
                }
                state.copy(
                    events = filtered.sortedBy { it.startTime },
                    isLoading = false
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun selectFilter(filter: EventFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun onEventCompleted(eventId: Int, completed: Boolean) {
        viewModelScope.launch {
            eventRepository.toggleEventCompletion(eventId, completed)
        }
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch {
            eventRepository.deleteEvent(event)
        }
    }
}
