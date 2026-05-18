package com.hbde.courseschedule.utils

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object TermWeekCalculator {
    private const val DEFAULT_WEEK = 1

    fun calculateCurrentWeek(
        termStartDate: LocalDate?,
        today: LocalDate = LocalDate.now(),
        maxWeek: Int = 25,
    ): Int {
        if (termStartDate == null || today.isBefore(termStartDate)) {
            return DEFAULT_WEEK
        }

        val daysSinceStart = ChronoUnit.DAYS.between(termStartDate, today)
        return (daysSinceStart / 7 + 1).toInt().coerceIn(DEFAULT_WEEK, maxWeek)
    }
}
