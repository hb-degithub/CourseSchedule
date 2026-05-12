package com.hbde.courseschedule.ui.campus.grade

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hbde.courseschedule.data.model.GpaAlgorithm
import com.hbde.courseschedule.data.model.GpaCalculator
import com.hbde.courseschedule.data.model.GpaResult
import com.hbde.courseschedule.data.model.Grade
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class GradeUiState(
    val grades: List<Grade> = emptyList(),
    val selectedSemester: String = "全部学期",
    val availableSemesters: List<String> = emptyList(),
    val selectedAlgorithm: GpaAlgorithm = GpaAlgorithm.STANDARD_4_0,
    val gpaResult: GpaResult = GpaResult(0f, 0f, 0, 0f),
    val isLoading: Boolean = false,
    val showAlgorithmMenu: Boolean = false,
    val showSemesterMenu: Boolean = false
)

@HiltViewModel
class GradeViewModel @Inject constructor() : ViewModel() {

    private val _allGrades = MutableStateFlow<List<Grade>>(emptyList())
    private val _selectedSemester = MutableStateFlow("全部学期")
    private val _selectedAlgorithm = MutableStateFlow(GpaAlgorithm.STANDARD_4_0)

    private val _uiState = MutableStateFlow(GradeUiState(isLoading = true))
    val uiState: StateFlow<GradeUiState> = _uiState.asStateFlow()

    init {
        // 合并数据流，自动计算 GPA
        combine(_allGrades, _selectedSemester, _selectedAlgorithm) { grades, semester, algorithm ->
            val filtered = if (semester == "全部学期") {
                grades
            } else {
                grades.filter { it.semester == semester }
            }

            val semesters = listOf("全部学期") + grades.map { it.semester }.distinct().sorted()
            val gpaResult = GpaCalculator.calculateGpa(filtered, algorithm)

            GradeUiState(
                grades = filtered,
                selectedSemester = semester,
                availableSemesters = semesters,
                selectedAlgorithm = algorithm,
                gpaResult = gpaResult,
                isLoading = false,
                showAlgorithmMenu = false,
                showSemesterMenu = false
            )
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)

        // 加载模拟数据
        loadMockData()
    }

    private fun loadMockData() {
        _allGrades.value = listOf(
            Grade(
                id = 1,
                courseName = "高等数学（上）",
                credit = 5.0f,
                score = 88f,
                semester = "2024-2025-1",
                type = com.hbde.courseschedule.data.model.CourseType.REQUIRED
            ),
            Grade(
                id = 2,
                courseName = "大学英语（一）",
                credit = 3.0f,
                score = 92f,
                semester = "2024-2025-1",
                type = com.hbde.courseschedule.data.model.CourseType.REQUIRED
            ),
            Grade(
                id = 3,
                courseName = "程序设计基础",
                credit = 4.0f,
                score = 85f,
                semester = "2024-2025-1",
                type = com.hbde.courseschedule.data.model.CourseType.REQUIRED
            ),
            Grade(
                id = 4,
                courseName = "线性代数",
                credit = 3.5f,
                score = 79f,
                semester = "2024-2025-1",
                type = com.hbde.courseschedule.data.model.CourseType.REQUIRED
            ),
            Grade(
                id = 5,
                courseName = "体育（一）",
                credit = 1.0f,
                score = 95f,
                semester = "2024-2025-1",
                type = com.hbde.courseschedule.data.model.CourseType.REQUIRED
            ),
            Grade(
                id = 6,
                courseName = "思想道德与法治",
                credit = 3.0f,
                score = 82f,
                semester = "2024-2025-1",
                type = com.hbde.courseschedule.data.model.CourseType.REQUIRED
            ),
            Grade(
                id = 7,
                courseName = "高等数学（下）",
                credit = 5.0f,
                score = 76f,
                semester = "2024-2025-2",
                type = com.hbde.courseschedule.data.model.CourseType.REQUIRED
            ),
            Grade(
                id = 8,
                courseName = "大学英语（二）",
                credit = 3.0f,
                score = 89f,
                semester = "2024-2025-2",
                type = com.hbde.courseschedule.data.model.CourseType.REQUIRED
            ),
            Grade(
                id = 9,
                courseName = "数据结构",
                credit = 4.0f,
                score = 91f,
                semester = "2024-2025-2",
                type = com.hbde.courseschedule.data.model.CourseType.REQUIRED
            ),
            Grade(
                id = 10,
                courseName = "心理学导论",
                credit = 2.0f,
                score = 87f,
                semester = "2024-2025-2",
                type = com.hbde.courseschedule.data.model.CourseType.ELECTIVE
            )
        )
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

    /**
     * 导入成绩数据（后续对接教务系统）
     */
    fun importGrades(grades: List<Grade>) {
        _allGrades.value = _allGrades.value + grades
    }
}
