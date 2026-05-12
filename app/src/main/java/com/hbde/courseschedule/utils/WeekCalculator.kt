package com.hbde.courseschedule.utils

import java.util.concurrent.TimeUnit

object WeekCalculator {

    /**
     * 计算从开学日期到今天过了几周（从第 1 周开始）
     *
     * @param startDate 开学日期的时间戳（毫秒）
     * @return 当前是第几周，至少为 1
     */
    fun getCurrentWeek(startDate: Long): Int {
        val now = System.currentTimeMillis()
        val diffMillis = now - startDate
        val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)
        val week = (diffDays / 7) + 1
        return week.toInt().coerceAtLeast(1)
    }

    /**
     * 判断是否为单周
     */
    fun isOddWeek(week: Int): Boolean = week % 2 == 1

    /**
     * 判断是否为双周
     */
    fun isEvenWeek(week: Int): Boolean = week % 2 == 0

    /**
     * 判断指定周是否在课程的周次范围内
     */
    fun isWeekInRange(week: Int, startWeek: Int, endWeek: Int): Boolean {
        return week in startWeek..endWeek
    }
}
