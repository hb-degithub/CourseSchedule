package com.hbde.courseschedule.data.local

import com.hbde.courseschedule.data.local.entity.CourseEntity

object CourseConflictChecker {

    /**
     * 检查新课程是否与已有课程存在时间冲突
     *
     * @param newCourse 要插入/更新的课程
     * @param existingCourses 同一星期几的已有课程列表
     * @return true 表示存在冲突，false 表示无冲突
     */
    fun checkConflict(
        newCourse: CourseEntity,
        existingCourses: List<CourseEntity>
    ): Boolean {
        return existingCourses.any { existing ->
            isTimeOverlap(newCourse, existing) && isWeekOverlap(newCourse, existing)
        }
    }

    /**
     * 判断两节课的节次是否重叠
     */
    private fun isTimeOverlap(a: CourseEntity, b: CourseEntity): Boolean {
        return a.startNode <= b.endNode && a.endNode >= b.startNode
    }

    /**
     * 判断两节课的周次是否重叠，同时考虑单双周类型
     */
    private fun isWeekOverlap(a: CourseEntity, b: CourseEntity): Boolean {
        // 先判断周次范围是否有交集
        val rangeOverlap = a.startWeek <= b.endWeek && a.endWeek >= b.startWeek
        if (!rangeOverlap) return false

        // 再判断单双周是否兼容
        return when {
            a.weekType == "all" || b.weekType == "all" -> true
            a.weekType == b.weekType -> true
            else -> false
        }
    }
}
