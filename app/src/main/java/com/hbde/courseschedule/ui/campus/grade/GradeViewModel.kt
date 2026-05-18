package com.hbde.courseschedule.ui.campus.grade

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hbde.courseschedule.data.local.entity.GradeEntity
import com.hbde.courseschedule.data.model.CourseType
import com.hbde.courseschedule.data.model.Grade
import com.hbde.courseschedule.data.repository.GradeRepository
import com.hbde.courseschedule.utils.GpaAlgorithm
import com.hbde.courseschedule.utils.GpaCalculator
import com.hbde.courseschedule.utils.GpaResult
import com.hbde.courseschedule.utils.SemesterGpa
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GradeUiState(
    val grades: List<Grade> = emptyList(),
    val selectedSemester: String = "全部学期",
    val availableSemesters: List<String> = emptyList(),
    val selectedAlgorithm: GpaAlgorithm = GpaAlgorithm.STANDARD_4_0,
    val gpaResult: GpaResult = GpaResult(0f, 0f, 0, 0f),
    val semesterGpaTrend: List<SemesterGpa> = emptyList(),
    val isLoading: Boolean = false,
    val showAlgorithmMenu: Boolean = false,
    val showSemesterMenu: Boolean = false,
    val showAlgorithmDescription: Boolean = false
)

@HiltViewModel
class GradeViewModel @Inject constructor(
    private val gradeRepository: GradeRepository
) : ViewModel() {

    private val _selectedSemester = MutableStateFlow("全部学期")
    private val _selectedAlgorithm = MutableStateFlow(GpaAlgorithm.STANDARD_4_0)

    private val _uiState = MutableStateFlow(GradeUiState(isLoading = true))
    val uiState: StateFlow<GradeUiState> = _uiState.asStateFlow()

    init {
        // 合并数据流，自动计算 GPA
        combine(
            gradeRepository.getAllGrades(),
            _selectedSemester,
            _selectedAlgorithm
        ) { entities, semester, algorithm ->
            val grades = entities.map { it.toModel() }
            val filtered = if (semester == "全部学期") {
                grades
            } else {
                grades.filter { it.semester == semester }
            }

            val semesters = listOf("全部学期") + grades.map { it.semester }.distinct().sorted()
            val gpaResult = GpaCalculator.calculateGpa(filtered, algorithm)
            val trend = GpaCalculator.calculateSemesterGpaTrend(grades, algorithm)

            GradeUiState(
                grades = filtered,
                selectedSemester = semester,
                availableSemesters = semesters,
                selectedAlgorithm = algorithm,
                gpaResult = gpaResult,
                semesterGpaTrend = trend,
                isLoading = false,
                showAlgorithmMenu = false,
                showSemesterMenu = false
            )
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)
    }

    fun selectSemester(semester: String) {
        _selectedSemester.value = semester
        _uiState.update { it.copy(showSemesterMenu = false) }
    }

    fun selectAlgorithm(algorithm: GpaAlgorithm) {
        _selectedAlgorithm.value = algorithm
        _uiState.update { it.copy(showAlgorithmMenu = false) }
    }

    fun toggleAlgorithmMenu() {
        _uiState.update { it.copy(showAlgorithmMenu = !it.showAlgorithmMenu) }
    }

    fun toggleSemesterMenu() {
        _uiState.update { it.copy(showSemesterMenu = !it.showSemesterMenu) }
    }

    fun dismissMenus() {
        _uiState.update { it.copy(showAlgorithmMenu = false, showSemesterMenu = false) }
    }

    fun toggleAlgorithmDescription() {
        _uiState.update { it.copy(showAlgorithmDescription = !it.showAlgorithmDescription) }
    }

    /**
     * 导入成绩数据
     */
    fun importGrades(grades: List<Grade>) {
        viewModelScope.launch {
            grades.forEach { grade ->
                gradeRepository.insertGrade(grade.toEntity())
            }
        }
    }

    /**
     * 添加单条成绩
     */
    fun addGrade(grade: Grade) {
        viewModelScope.launch {
            gradeRepository.insertGrade(grade.toEntity())
        }
    }

    /**
     * 删除成绩
     */
    fun deleteGrade(grade: Grade) {
        viewModelScope.launch {
            gradeRepository.deleteGrade(grade.toEntity())
        }
    }

    /**
     * 清空所有成绩
     */
    fun clearAllGrades() {
        viewModelScope.launch {
            gradeRepository.deleteAllGrades()
        }
    }

    private fun GradeEntity.toModel(): Grade {
        return Grade(
            id = this.id,
            courseName = this.courseName,
            credit = this.credit,
            score = this.score,
            semester = this.semester,
            type = if (this.type == "ELECTIVE") CourseType.ELECTIVE else CourseType.REQUIRED
        )
    }

    private fun Grade.toEntity(): GradeEntity {
        return GradeEntity(
            id = this.id,
            courseName = this.courseName,
            credit = this.credit,
            score = this.score,
            semester = this.semester,
            type = if (this.type == CourseType.ELECTIVE) "ELECTIVE" else "REQUIRED"
        )
    }
}
