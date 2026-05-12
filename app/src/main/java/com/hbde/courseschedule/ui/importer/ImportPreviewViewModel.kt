package com.hbde.courseschedule.ui.importer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hbde.courseschedule.data.local.CourseConflictChecker
import com.hbde.courseschedule.data.local.entity.CourseEntity
import com.hbde.courseschedule.data.repository.CourseRepository
import com.hbde.courseschedule.importer.parser.RawCourse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ImportPreviewItem(
    val rawCourse: RawCourse,
    val isSelected: Boolean = true,
    val hasConflict: Boolean = false
)

sealed class ImportPreviewUiState {
    data object Loading : ImportPreviewUiState()
    data class Success(
        val items: List<ImportPreviewItem>,
        val allSelected: Boolean = true,
        val importResult: ImportResult? = null
    ) : ImportPreviewUiState()
    data class Error(val message: String) : ImportPreviewUiState()
}

sealed class ImportResult {
    data object Idle : ImportResult()
    data class Success(val importedCount: Int) : ImportResult()
    data class Error(val message: String) : ImportResult()
}

@HiltViewModel
class ImportPreviewViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val courseRepository: CourseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ImportPreviewUiState>(ImportPreviewUiState.Loading)
    val uiState: StateFlow<ImportPreviewUiState> = _uiState.asStateFlow()

    private val gson = Gson()

    init {
        loadCourses()
    }

    private fun loadCourses() {
        viewModelScope.launch {
            try {
                val coursesJson: String = savedStateHandle["courses_json"] ?: ""
                if (coursesJson.isBlank()) {
                    _uiState.value = ImportPreviewUiState.Error("没有可导入的课程数据")
                    return@launch
                }

                val type = object : TypeToken<List<RawCourse>>() {}.type
                val rawCourses: List<RawCourse> = gson.fromJson(coursesJson, type) ?: emptyList()

                if (rawCourses.isEmpty()) {
                    _uiState.value = ImportPreviewUiState.Error("解析结果为空")
                    return@launch
                }

                // 检测冲突
                val existingCourses = courseRepository.getAllCourses().first()
                val items = rawCourses.map { rawCourse ->
                    val entity = rawCourseToEntity(rawCourse)
                    val sameDayCourses = existingCourses.filter { it.dayOfWeek == rawCourse.dayOfWeek }
                    val hasConflict = CourseConflictChecker.checkConflict(entity, sameDayCourses)
                    ImportPreviewItem(
                        rawCourse = rawCourse,
                        isSelected = !hasConflict,
                        hasConflict = hasConflict
                    )
                }

                _uiState.value = ImportPreviewUiState.Success(
                    items = items,
                    allSelected = items.all { it.isSelected }
                )
            } catch (e: Exception) {
                _uiState.value = ImportPreviewUiState.Error("加载失败: ${e.message}")
            }
        }
    }

    fun toggleSelection(index: Int) {
        val currentState = _uiState.value as? ImportPreviewUiState.Success ?: return
        val updatedItems = currentState.items.toMutableList()
        val item = updatedItems[index]
        updatedItems[index] = item.copy(isSelected = !item.isSelected)
        _uiState.value = currentState.copy(
            items = updatedItems,
            allSelected = updatedItems.all { it.isSelected }
        )
    }

    fun toggleSelectAll() {
        val currentState = _uiState.value as? ImportPreviewUiState.Success ?: return
        val newAllSelected = !currentState.allSelected
        val updatedItems = currentState.items.map { it.copy(isSelected = newAllSelected) }
        _uiState.value = currentState.copy(
            items = updatedItems,
            allSelected = newAllSelected
        )
    }

    fun importSelected() {
        val currentState = _uiState.value as? ImportPreviewUiState.Success ?: return
        val selectedItems = currentState.items.filter { it.isSelected }

        if (selectedItems.isEmpty()) {
            _uiState.value = currentState.copy(importResult = ImportResult.Error("未选择任何课程"))
            return
        }

        viewModelScope.launch {
            try {
                var importedCount = 0
                val errors = mutableListOf<String>()

                for (item in selectedItems) {
                    try {
                        val entity = rawCourseToEntity(item.rawCourse)
                        courseRepository.insertCourse(entity)
                        importedCount++
                    } catch (e: Exception) {
                        errors.add("${item.rawCourse.name}: ${e.message}")
                    }
                }

                _uiState.value = currentState.copy(
                    importResult = if (importedCount > 0) {
                        ImportResult.Success(importedCount)
                    } else {
                        ImportResult.Error(errors.firstOrNull() ?: "导入失败")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    importResult = ImportResult.Error("导入失败: ${e.message}")
                )
            }
        }
    }

    fun clearImportResult() {
        val currentState = _uiState.value as? ImportPreviewUiState.Success ?: return
        _uiState.value = currentState.copy(importResult = ImportResult.Idle)
    }

    private fun rawCourseToEntity(rawCourse: RawCourse): CourseEntity {
        return CourseEntity(
            name = rawCourse.name,
            classroom = rawCourse.classroom.takeIf { it.isNotBlank() },
            teacher = rawCourse.teacher.takeIf { it.isNotBlank() },
            dayOfWeek = rawCourse.dayOfWeek,
            startNode = rawCourse.startNode,
            endNode = rawCourse.endNode,
            startWeek = rawCourse.startWeek,
            endWeek = rawCourse.endWeek,
            weekType = rawCourse.weekType
        )
    }
}
