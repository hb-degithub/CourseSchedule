package com.hbde.courseschedule.utils

import com.hbde.courseschedule.data.local.entity.CourseEntity
import com.hbde.courseschedule.data.local.entity.TimeSlot
import com.hbde.courseschedule.data.model.CourseListItemStatus
import com.hbde.courseschedule.data.model.CourseStatus
import java.time.LocalTime
import java.util.Calendar

/**
 * 课程状态计算器
 * 根据当前时间和作息时间表计算课程实时状态
 */
object CourseStatusCalculator {

    // 默认时间段（12节课的标准大学作息）
    val DEFAULT_TIME_SLOTS = listOf(
        TimeSlot("08:00", "08:45"),
        TimeSlot("08:55", "09:40"),
        TimeSlot("10:00", "10:45"),
        TimeSlot("10:55", "11:40"),
        TimeSlot("14:00", "14:45"),
        TimeSlot("14:55", "15:40"),
        TimeSlot("16:00", "16:45"),
        TimeSlot("16:55", "17:40"),
        TimeSlot("19:00", "19:45"),
        TimeSlot("19:55", "20:40"),
        TimeSlot("20:50", "21:35"),
        TimeSlot("21:45", "22:30")
    )

    /**
     * 计算当前课程状态
     *
     * @param courses 当天课程列表（已按单双周过滤）
     * @param timeSlots 作息时间表，null 则使用默认
     * @return CourseStatus
     */
    fun calculateCurrentStatus(
        courses: List<CourseEntity>,
        timeSlots: List<TimeSlot>? = null
    ): CourseStatus {
        val slots = timeSlots ?: DEFAULT_TIME_SLOTS
        val currentTime = getCurrentTimeMinutes()

        // 按开始节次排序
        val sortedCourses = courses.sortedBy { it.startNode }

        // 检查是否正在上课
        for (course in sortedCourses) {
            val startTime = getNodeStartTime(course.startNode, slots)
            val endTime = getNodeEndTime(course.endNode, slots)

            if (currentTime in startTime..endTime) {
                val remaining = endTime - currentTime
                return CourseStatus.InClass(course, remaining.coerceAtLeast(0))
            }
        }

        // 检查下一节课
        for (course in sortedCourses) {
            val startTime = getNodeStartTime(course.startNode, slots)
            if (currentTime < startTime) {
                val remaining = startTime - currentTime
                return CourseStatus.Break(course, remaining.coerceAtLeast(0))
            }
        }

        // 当天无课或所有课已结束
        return CourseStatus.NoClass
    }

    /**
     * 判断课程列表项状态（用于两日视图）
     */
    fun calculateItemStatus(
        course: CourseEntity,
        timeSlots: List<TimeSlot>? = null
    ): CourseListItemStatus {
        val slots = timeSlots ?: DEFAULT_TIME_SLOTS
        val currentTime = getCurrentTimeMinutes()
        val startTime = getNodeStartTime(course.startNode, slots)
        val endTime = getNodeEndTime(course.endNode, slots)

        return when {
            currentTime in startTime..endTime -> CourseListItemStatus.ONGOING
            currentTime > endTime -> CourseListItemStatus.ENDED
            else -> CourseListItemStatus.UPCOMING
        }
    }

    /**
     * 计算距离课程开始的剩余分钟数
     */
    fun calculateMinutesUntilStart(
        course: CourseEntity,
        timeSlots: List<TimeSlot>? = null
    ): Int {
        val slots = timeSlots ?: DEFAULT_TIME_SLOTS
        val currentTime = getCurrentTimeMinutes()
        val startTime = getNodeStartTime(course.startNode, slots)
        return (startTime - currentTime).coerceAtLeast(0)
    }

    /**
     * 计算距离课程结束的剩余分钟数
     */
    fun calculateMinutesUntilEnd(
        course: CourseEntity,
        timeSlots: List<TimeSlot>? = null
    ): Int {
        val slots = timeSlots ?: DEFAULT_TIME_SLOTS
        val currentTime = getCurrentTimeMinutes()
        val endTime = getNodeEndTime(course.endNode, slots)
        return (endTime - currentTime).coerceAtLeast(0)
    }

    /**
     * 判断课程是否正在上课（用于网格高亮）
     */
    fun isCurrentlyActive(
        course: CourseEntity,
        timeSlots: List<TimeSlot>? = null
    ): Boolean {
        val slots = timeSlots ?: DEFAULT_TIME_SLOTS
        val currentTime = getCurrentTimeMinutes()
        val startTime = getNodeStartTime(course.startNode, slots)
        val endTime = getNodeEndTime(course.endNode, slots)
        return currentTime in startTime..endTime
    }

    /**
     * 获取某节课的开始时间（分钟数）
     */
    fun getNodeStartTime(node: Int, timeSlots: List<TimeSlot>): Int {
        val index = (node - 1).coerceIn(0, timeSlots.size - 1)
        return parseTimeToMinutes(timeSlots[index].startTime)
    }

    /**
     * 获取某节课的结束时间（分钟数）
     */
    fun getNodeEndTime(node: Int, timeSlots: List<TimeSlot>): Int {
        val index = (node - 1).coerceIn(0, timeSlots.size - 1)
        return parseTimeToMinutes(timeSlots[index].endTime)
    }

    /**
     * 解析时间字符串 "HH:mm" 为当天分钟数
     */
    fun parseTimeToMinutes(timeStr: String): Int {
        val parts = timeStr.split(":")
        if (parts.size != 2) return 0
        val hour = parts[0].toIntOrNull() ?: 0
        val minute = parts[1].toIntOrNull() ?: 0
        return hour * 60 + minute
    }

    /**
     * 获取当前时间的分钟数（从 00:00 开始）
     */
    fun getCurrentTimeMinutes(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    }

    /**
     * 获取今天是星期几（1-7，周一=1）
     */
    fun getTodayDayOfWeek(): Int {
        val calendar = Calendar.getInstance()
        val day = calendar.get(Calendar.DAY_OF_WEEK)
        return if (day == Calendar.SUNDAY) 7 else day - 1
    }

    /**
     * 格式化分钟数为 "X小时Y分钟" 或 "X分钟"
     */
    fun formatDuration(minutes: Int): String {
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
