package com.hbde.courseschedule.service.countdown

import com.hbde.courseschedule.data.local.entity.CourseEntity
import com.hbde.courseschedule.data.local.entity.TimeSlot
import com.hbde.courseschedule.data.model.CourseStatus
import com.hbde.courseschedule.utils.CourseStatusCalculator
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 课程倒计时管理器
 * 实时计算距离上课/下课还有多久，提供 Flow 供 UI 层订阅显示
 */
@Singleton
class CourseCountdownManager @Inject constructor() {

    companion object {
        private const val UPDATE_INTERVAL_MS = 1000L // 每秒更新一次
    }

    /**
     * 获取当前课程状态流（每秒自动更新）
     * @param courses 当天课程列表（已按单双周过滤）
     * @param timeSlots 作息时间表，null 则使用默认
     */
    fun getCourseStatusFlow(
        courses: List<CourseEntity>,
        timeSlots: List<TimeSlot>? = null
    ): Flow<CourseStatus> = flow {
        while (currentCoroutineContext().isActive) {
            val status = CourseStatusCalculator.calculateCurrentStatus(courses, timeSlots)
            emit(status)
            delay(UPDATE_INTERVAL_MS)
        }
    }

    /**
     * 获取倒计时文本流（每秒自动更新）
     * 返回格式化的倒计时字符串，如 "12:34" 或 "1小时23分"
     * @param courses 当天课程列表（已按单双周过滤）
     * @param timeSlots 作息时间表，null 则使用默认
     */
    fun getCountdownTextFlow(
        courses: List<CourseEntity>,
        timeSlots: List<TimeSlot>? = null
    ): Flow<String> = flow {
        val slots = timeSlots ?: CourseStatusCalculator.DEFAULT_TIME_SLOTS
        while (currentCoroutineContext().isActive) {
            val text = calculateCountdownText(courses, slots)
            emit(text)
            delay(UPDATE_INTERVAL_MS)
        }
    }

    /**
     * 获取下一节课的倒计时秒数流（每秒自动更新）
     * @param courses 当天课程列表（已按单双周过滤）
     * @param timeSlots 作息时间表，null 则使用默认
     */
    fun getNextCourseCountdownFlow(
        courses: List<CourseEntity>,
        timeSlots: List<TimeSlot>? = null
    ): Flow<CountdownInfo> = flow {
        val slots = timeSlots ?: CourseStatusCalculator.DEFAULT_TIME_SLOTS
        while (currentCoroutineContext().isActive) {
            val info = calculateNextCourseCountdown(courses, slots)
            emit(info)
            delay(UPDATE_INTERVAL_MS)
        }
    }

    /**
     * 计算当前倒计时文本
     */
    private fun calculateCountdownText(
        courses: List<CourseEntity>,
        timeSlots: List<TimeSlot>
    ): String {
        val status = CourseStatusCalculator.calculateCurrentStatus(courses, timeSlots)
        return when (status) {
            is CourseStatus.InClass -> {
                val remaining = status.remainingMinutes
                "下课还有 ${formatMinutes(remaining)}"
            }
            is CourseStatus.Break -> {
                val remaining = status.remainingMinutes
                "上课还有 ${formatMinutes(remaining)}"
            }
            is CourseStatus.NoClass -> {
                "今日课程已结束"
            }
        }
    }

    /**
     * 计算下一节课的倒计时信息
     */
    private fun calculateNextCourseCountdown(
        courses: List<CourseEntity>,
        timeSlots: List<TimeSlot>
    ): CountdownInfo {
        val status = CourseStatusCalculator.calculateCurrentStatus(courses, timeSlots)
        return when (status) {
            is CourseStatus.InClass -> {
                CountdownInfo(
                    type = CountdownType.UNTIL_CLASS_END,
                    course = status.course,
                    totalSeconds = status.remainingMinutes * 60,
                    formattedText = "下课还有 ${formatMinutes(status.remainingMinutes)}"
                )
            }
            is CourseStatus.Break -> {
                CountdownInfo(
                    type = CountdownType.UNTIL_CLASS_START,
                    course = status.nextCourse,
                    totalSeconds = status.remainingMinutes * 60,
                    formattedText = "上课还有 ${formatMinutes(status.remainingMinutes)}"
                )
            }
            is CourseStatus.NoClass -> {
                CountdownInfo(
                    type = CountdownType.NO_MORE_CLASSES,
                    course = null,
                    totalSeconds = 0,
                    formattedText = "今日课程已结束"
                )
            }
        }
    }

    /**
     * 格式化分钟数为可读文本
     */
    private fun formatMinutes(minutes: Int): String {
        if (minutes <= 0) return "0分钟"
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}小时${mins}分钟"
            hours > 0 -> "${hours}小时"
            else -> "${mins}分钟"
        }
    }
}

/**
 * 倒计时类型
 */
enum class CountdownType {
    UNTIL_CLASS_START,  // 距离上课
    UNTIL_CLASS_END,    // 距离下课
    NO_MORE_CLASSES     // 今日无课
}

/**
 * 倒计时信息数据类
 */
data class CountdownInfo(
    val type: CountdownType,
    val course: CourseEntity?,
    val totalSeconds: Int,
    val formattedText: String
)
