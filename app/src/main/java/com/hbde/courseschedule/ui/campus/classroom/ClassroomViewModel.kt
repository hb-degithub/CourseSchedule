package com.hbde.courseschedule.ui.campus.classroom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hbde.courseschedule.data.model.Classroom
import com.hbde.courseschedule.data.model.ClassroomStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class ClassroomUiState(
    val classrooms: List<ClassroomStatus> = emptyList(),
    val filteredClassrooms: List<ClassroomStatus> = emptyList(),
    val selectedBuilding: String = "全部教学楼",
    val selectedDayOfWeek: Int = getCurrentDayOfWeek(),
    val startNode: Int = 1,
    val endNode: Int = 2,
    val favoriteClassrooms: Set<String> = emptySet(),
    val availableBuildings: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val showBuildingMenu: Boolean = false
)

@HiltViewModel
class ClassroomViewModel @Inject constructor() : ViewModel() {

    private val _allClassrooms = MutableStateFlow<List<Classroom>>(emptyList())
    private val _selectedBuilding = MutableStateFlow("全部教学楼")
    private val _selectedDayOfWeek = MutableStateFlow(getCurrentDayOfWeek())
    private val _startNode = MutableStateFlow(1)
    private val _endNode = MutableStateFlow(2)
    private val _favoriteClassrooms = MutableStateFlow<Set<String>>(emptySet())

    private val _uiState = MutableStateFlow(ClassroomUiState(isLoading = true))
    val uiState: StateFlow<ClassroomUiState> = _uiState.asStateFlow()

    init {
        // 合并筛选条件，自动过滤
        combine(
            _allClassrooms,
            _selectedBuilding,
            _selectedDayOfWeek,
            _startNode,
            _endNode,
            _favoriteClassrooms
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            val classrooms = values[0] as List<Classroom>
            val building = values[1] as String
            val start = values[3] as Int
            val end = values[4] as Int
            val favorites = values[5] as Set<String>

            val buildings = listOf("全部教学楼") + classrooms.map { it.building }.distinct().sorted()

            val filtered = classrooms.filter { c ->
                building == "全部教学楼" || c.building == building
            }.map { c ->
                val isOccupied = isClassroomOccupied(c.id, start, end)
                ClassroomStatus(
                    classroom = c,
                    isOccupied = isOccupied,
                    occupiedNodes = if (isOccupied) (start..end).toList() else emptyList()
                )
            }.filter { !it.isOccupied }

            val sorted = filtered.sortedWith(
                compareByDescending<ClassroomStatus> { favorites.contains(it.classroom.id) }
                    .thenBy { it.classroom.building }
                    .thenBy { it.classroom.floor }
                    .thenBy { it.classroom.name }
            )

            ClassroomUiState(
                classrooms = filtered,
                filteredClassrooms = sorted,
                selectedBuilding = building,
                selectedDayOfWeek = _selectedDayOfWeek.value,
                startNode = start,
                endNode = end,
                favoriteClassrooms = favorites,
                availableBuildings = buildings,
                isLoading = false,
                showBuildingMenu = false
            )
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)

        loadMockData()
    }

    private fun loadMockData() {
        _allClassrooms.value = listOf(
            Classroom("A101", "A101", "第一教学楼", 1, 120),
            Classroom("A102", "A102", "第一教学楼", 1, 80),
            Classroom("A201", "A201", "第一教学楼", 2, 150, hasAirConditioner = false),
            Classroom("A202", "A202", "第一教学楼", 2, 60),
            Classroom("A301", "A301", "第一教学楼", 3, 200),
            Classroom("B101", "B101", "第二教学楼", 1, 100),
            Classroom("B102", "B102", "第二教学楼", 1, 90),
            Classroom("B201", "B201", "第二教学楼", 2, 130),
            Classroom("B202", "B202", "第二教学楼", 2, 70, hasProjector = false),
            Classroom("B301", "B301", "第二教学楼", 3, 180),
            Classroom("C101", "C101", "图书馆", 1, 50),
            Classroom("C102", "C102", "图书馆", 1, 40, hasProjector = false),
            Classroom("C201", "C201", "图书馆", 2, 60),
            Classroom("D101", "D101", "实验楼", 1, 45),
            Classroom("D102", "D102", "实验楼", 1, 45)
        )
    }

    /**
     * 模拟教室占用状态（后续对接教务系统接口）
     */
    private fun isClassroomOccupied(classroomId: String, startNode: Int, endNode: Int): Boolean {
        // 模拟数据：部分教室在特定节次被占用
        val occupiedMap = mapOf(
            "A101" to listOf(1, 2, 3, 4),
            "A201" to listOf(5, 6, 7, 8),
            "B101" to listOf(1, 2),
            "B201" to listOf(3, 4, 5, 6),
            "C101" to listOf(9, 10, 11),
            "D101" to listOf(1, 2, 3, 4, 5)
        )
        val occupiedNodes = occupiedMap[classroomId] ?: emptyList()
        return (startNode..endNode).any { it in occupiedNodes }
    }

    fun selectBuilding(building: String) {
        _selectedBuilding.value = building
        _uiState.update { it.copy(showBuildingMenu = false) }
    }

    fun selectDayOfWeek(day: Int) {
        _selectedDayOfWeek.value = day.coerceIn(1, 7)
    }

    fun setNodeRange(start: Int, end: Int) {
        val validStart = start.coerceIn(1, 12)
        val validEnd = end.coerceIn(validStart, 12)
        _startNode.value = validStart
        _endNode.value = validEnd
    }

    fun toggleFavorite(classroomId: String) {
        _favoriteClassrooms.update { current ->
            if (current.contains(classroomId)) {
                current - classroomId
            } else {
                current + classroomId
            }
        }
    }

    fun toggleBuildingMenu() {
        _uiState.update { it.copy(showBuildingMenu = !it.showBuildingMenu) }
    }

    fun dismissMenu() {
        _uiState.update { it.copy(showBuildingMenu = false) }
    }

    companion object
}

private fun getCurrentDayOfWeek(): Int {
    val calendar = java.util.Calendar.getInstance()
    val day = calendar.get(java.util.Calendar.DAY_OF_WEEK)
    return if (day == java.util.Calendar.SUNDAY) 7 else day - 1
}
