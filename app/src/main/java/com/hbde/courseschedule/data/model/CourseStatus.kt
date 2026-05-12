package com.hbde.courseschedule.data.model

import com.hbde.courseschedule.data.local.entity.CourseEntity

/**
 * 课程实时状态
 */
sealed class CourseStatus {
    /**
     * 正在上课
     * @param course 当前课程
     * @param remainingMinutes 距离下课剩余分钟数
     */
    data class InClass(
        val course: CourseEntity,
        val remainingMinutes: Int
    ) : CourseStatus()

    /**
     * 课间休息
     * @param nextCourse 下一节课
     * @param remainingMinutes 距离上课剩余分钟数
     */
    data class Break(
        val nextCourse: CourseEntity,
        val remainingMinutes: Int
    ) : CourseStatus()

    /**
     * 当天无课
     */
    data object NoClass : CourseStatus()
}

/**
 * 课程列表项状态（用于两日视图）
 */
enum class CourseListItemStatus {
    ONGOING,    // 进行中
    ENDED,      // 已结束
    UPCOMING    // 未开始
}
