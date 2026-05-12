package com.hbde.courseschedule.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hbde.courseschedule.data.local.entity.CourseEntity
import com.hbde.courseschedule.data.repository.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import javax.inject.Inject

enum class WeekType {
    ALL, ODD, EVEN
}

data class ScheduleUiState(
    val currentWeek: Int = 1,
    val courses: List<CourseEntity> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val courseRepository: CourseRepository
) : ViewModel() {

    private val _currentWeek = MutableStateFlow(calculateCurrentWeek())
    private val _allCourses = MutableStateFlow<List<CourseEntity>>(emptyList())

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        // 合并 currentWeek 和 allCourses，自动过滤当前周课程
        combine(_currentWeek, _allCourses) { week, courses ->
            val filtered = courses.filter { it.isVisibleInWeek(week) }
            ScheduleUiState(
                currentWeek = week,
                courses = filtered,
                isLoading = false
            )
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)

        // 从 Repository 加载课程
        loadCourses()
    }

    private fun loadCourses() {
        _uiState.update { it.copy(isLoading = true) }
        courseRepository.getAllCourses()
            .onEach { courses ->
                _allCourses.value = courses
            }
            .launchIn(viewModelScope)
    }

    fun refresh() {
        loadCourses()
    }

    fun previousWeek() {
        _currentWeek.value = (_currentWeek.value - 1).coerceAtLeast(1)
    }

    fun nextWeek() {
        _currentWeek.value = (_currentWeek.value + 1).coerceAtMost(25)
    }

    fun currentWeek() {
        _currentWeek.value = calculateCurrentWeek()
    }

    fun setCurrentWeek(week: Int) {
        _currentWeek.value = week.coerceIn(1, 25)
    }

    companion object {
        /**
         * 计算当前是第几周（基于学期开始日期）
         * TODO: 从 SettingsDataStore 读取学期开始日期
         */
        fun calculateCurrentWeek(): Int {
            // 默认假设学期开始日期为当前日期所在周周一，则当前周为第 1 周
            // 后续应从设置中读取学期开始日期
            return 1
        }
    }
}

/**
 * 判断课程在某周是否可见（周范围 + 单双周判断）
 */
fun CourseEntity.isVisibleInWeek(week: Int): Boolean {
    if (week < startWeek || week > endWeek) return false
    return when (weekType.uppercase()) {
        "ODD" -> week % 2 == 1
        "EVEN" -> week % 2 == 0
        else -> true
    }
}
