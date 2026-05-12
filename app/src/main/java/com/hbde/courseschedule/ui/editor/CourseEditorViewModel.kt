package com.hbde.courseschedule.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hbde.courseschedule.data.local.entity.CourseEntity
import com.hbde.courseschedule.data.repository.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class WeekType {
    ALL, ODD, EVEN
}

sealed class SaveResult {
    data object Idle : SaveResult()
    data object Success : SaveResult()
    data class Error(val message: String) : SaveResult()
}

data class CourseEditorUiState(
    val courseName: String = "",
    val classroom: String = "",
    val teacher: String = "",
    val dayOfWeek: Int = 1, // 1..7
    val startNode: Int = 1, // 1..12
    val endNode: Int = 2,
    val startWeek: Int = 1,
    val endWeek: Int = 20,
    val weekType: WeekType = WeekType.ALL,
    val color: Int? = null,
    val notes: String = "",
    val isSaving: Boolean = false,
    val saveResult: SaveResult = SaveResult.Idle,
    val nameError: String? = null,
    val nodeError: String? = null,
    val weekError: String? = null
)

@HiltViewModel
class CourseEditorViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val courseRepository: CourseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CourseEditorUiState())
    val uiState: StateFlow<CourseEditorUiState> = _uiState.asStateFlow()

    private var courseId: Int? = null

    init {
        val id: Int = savedStateHandle["courseId"] ?: -1
        val dow: Int = savedStateHandle["dayOfWeek"] ?: -1
        val node: Int = savedStateHandle["startNode"] ?: -1

        if (id > 0) {
            courseId = id
            loadCourse(id)
        } else {
            // Pre-fill from empty cell click
            val initialDay = if (dow > 0) dow else 1
            val initialNode = if (node > 0) node else 1
            _uiState.value = CourseEditorUiState(
                dayOfWeek = initialDay.coerceIn(1, 7),
                startNode = initialNode.coerceIn(1, 12),
                endNode = (initialNode + 1).coerceIn(1, 12)
            )
        }
    }

    private fun loadCourse(id: Int) {
        viewModelScope.launch {
            val course = courseRepository.getCourseById(id).first()
            course?.let { c ->
                _uiState.update {
                    it.copy(
                        courseName = c.name,
                        classroom = c.classroom ?: "",
                        teacher = c.teacher ?: "",
                        dayOfWeek = c.dayOfWeek,
                        startNode = c.startNode,
                        endNode = c.endNode,
                        startWeek = c.startWeek,
                        endWeek = c.endWeek,
                        weekType = runCatching { WeekType.valueOf(c.weekType) }.getOrDefault(WeekType.ALL),
                        color = c.color,
                        notes = c.notes ?: ""
                    )
                }
            }
        }
    }

    fun onCourseNameChange(name: String) {
        _uiState.update { it.copy(courseName = name, nameError = null) }
    }

    fun onClassroomChange(classroom: String) {
        _uiState.update { it.copy(classroom = classroom) }
    }

    fun onTeacherChange(teacher: String) {
        _uiState.update { it.copy(teacher = teacher) }
    }

    fun onDayOfWeekChange(day: Int) {
        _uiState.update { it.copy(dayOfWeek = day) }
    }

    fun onStartNodeChange(node: Int) {
        _uiState.update {
            val newEnd = if (it.endNode < node) node else it.endNode
            it.copy(startNode = node, endNode = newEnd, nodeError = null)
        }
    }

    fun onEndNodeChange(node: Int) {
        _uiState.update { it.copy(endNode = node, nodeError = null) }
    }

    fun onStartWeekChange(week: Int) {
        _uiState.update {
            val newEnd = if (it.endWeek < week) week else it.endWeek
            it.copy(startWeek = week, endWeek = newEnd, weekError = null)
        }
    }

    fun onEndWeekChange(week: Int) {
        _uiState.update { it.copy(endWeek = week, weekError = null) }
    }

    fun onWeekTypeChange(type: WeekType) {
        _uiState.update { it.copy(weekType = type) }
    }

    fun onColorChange(color: Int) {
        _uiState.update { it.copy(color = color) }
    }

    fun onNotesChange(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun clearSaveResult() {
        _uiState.update { it.copy(saveResult = SaveResult.Idle) }
    }

    fun save() {
        val state = _uiState.value

        // Validation
        var nameError: String? = null
        var nodeError: String? = null
        var weekError: String? = null

        if (state.courseName.isBlank()) {
            nameError = "课程名不能为空"
        }
        if (state.endNode < state.startNode) {
            nodeError = "结束节次不能小于起始节次"
        }
        if (state.endWeek < state.startWeek) {
            weekError = "结束周不能小于起始周"
        }

        if (nameError != null || nodeError != null || weekError != null) {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    nodeError = nodeError,
                    weekError = weekError
                )
            }
            return
        }

        _uiState.update { it.copy(isSaving = true, saveResult = SaveResult.Idle) }

        viewModelScope.launch {
            try {
                val entity = CourseEntity(
                    id = courseId ?: 0,
                    name = state.courseName.trim(),
                    classroom = state.classroom.trim().takeIf { it.isNotEmpty() },
                    teacher = state.teacher.trim().takeIf { it.isNotEmpty() },
                    dayOfWeek = state.dayOfWeek,
                    startNode = state.startNode,
                    endNode = state.endNode,
                    startWeek = state.startWeek,
                    endWeek = state.endWeek,
                    weekType = state.weekType.name,
                    color = state.color,
                    notes = state.notes.trim().takeIf { it.isNotEmpty() }
                )

                if (courseId != null) {
                    courseRepository.updateCourse(entity)
                } else {
                    courseRepository.insertCourse(entity)
                }
                _uiState.update { it.copy(isSaving = false, saveResult = SaveResult.Success) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveResult = SaveResult.Error(e.message ?: "保存失败")
                    )
                }
            }
        }
    }
}
