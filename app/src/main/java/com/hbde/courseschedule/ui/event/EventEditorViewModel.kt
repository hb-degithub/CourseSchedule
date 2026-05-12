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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

data class EventEditorUiState(
    val title: String = "",
    val selectedType: EventType = EventType.CUSTOM,
    val location: String = "",
    val notes: String = "",
    val startDate: LocalDate = LocalDate.now(),
    val startTime: LocalTime = LocalTime.now().withMinute(0).withSecond(0).withNano(0),
    val endDate: LocalDate = LocalDate.now(),
    val endTime: LocalTime = LocalTime.now().withMinute(0).withSecond(0).withNano(0).plusHours(1),
    val reminderMinutes: Int = 15,
    val priority: EventPriority = EventPriority.MEDIUM,
    val isSaving: Boolean = false,
    val saveCompleted: Boolean = false,
    val errorMessage: String? = null
) {
    val canSave: Boolean
        get() = title.isNotBlank()
}

@HiltViewModel
class EventEditorViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(EventEditorUiState())
    val uiState: StateFlow<EventEditorUiState> = _uiState.asStateFlow()

    private var editingEventId: Int? = null

    fun loadEvent(event: Event) {
        editingEventId = event.id
        _uiState.value = EventEditorUiState(
            title = event.title,
            selectedType = event.type,
            location = event.location ?: "",
            notes = event.notes ?: "",
            startDate = event.startTime.toLocalDate(),
            startTime = event.startTime.toLocalTime(),
            endDate = event.endTime?.toLocalDate() ?: event.startTime.toLocalDate(),
            endTime = event.endTime?.toLocalTime() ?: event.startTime.toLocalTime().plusHours(1),
            reminderMinutes = event.reminderMinutes,
            priority = event.priority
        )
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun updateType(type: EventType) {
        _uiState.update { it.copy(selectedType = type) }
    }

    fun updateLocation(location: String) {
        _uiState.update { it.copy(location = location) }
    }

    fun updateNotes(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun updateStartDate(date: LocalDate) {
        _uiState.update { it.copy(startDate = date) }
    }

    fun updateStartTime(time: LocalTime) {
        _uiState.update { it.copy(startTime = time) }
    }

    fun updateEndDate(date: LocalDate) {
        _uiState.update { it.copy(endDate = date) }
    }

    fun updateEndTime(time: LocalTime) {
        _uiState.update { it.copy(endTime = time) }
    }

    fun updateReminderMinutes(minutes: Int) {
        _uiState.update { it.copy(reminderMinutes = minutes) }
    }

    fun updatePriority(priority: EventPriority) {
        _uiState.update { it.copy(priority = priority) }
    }

    fun saveEvent() {
        val state = _uiState.value
        if (!state.canSave) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            val startDateTime = LocalDateTime.of(state.startDate, state.startTime)
            val endDateTime = if (state.endDate != null && state.endTime != null) {
                LocalDateTime.of(state.endDate, state.endTime)
            } else null

            val event = Event(
                id = editingEventId ?: 0,
                title = state.title.trim(),
                type = state.selectedType,
                location = state.location.trim().takeIf { it.isNotEmpty() },
                startTime = startDateTime,
                endTime = endDateTime,
                reminderMinutes = state.reminderMinutes,
                priority = state.priority,
                notes = state.notes.trim().takeIf { it.isNotEmpty() },
                isCompleted = false
            )

            // TODO: 调用 Repository 保存事件
            // eventRepository.saveEvent(event)

            _uiState.update { it.copy(isSaving = false, saveCompleted = true) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
