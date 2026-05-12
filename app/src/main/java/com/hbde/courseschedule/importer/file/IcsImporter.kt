package com.hbde.courseschedule.importer.file

import android.content.Context
import android.net.Uri
import com.hbde.courseschedule.importer.ImportResult
import com.hbde.courseschedule.importer.parser.RawCourse
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.Component
import net.fortuna.ical4j.model.Recur
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.DtStart
import net.fortuna.ical4j.model.property.RRule
import net.fortuna.ical4j.model.property.Summary
import net.fortuna.ical4j.model.property.Location
import java.io.InputStream
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * iCalendar (ICS) 课表文件导入器（iCal4j）
 */
class IcsImporter(private val context: Context) {

    companion object {
        private val BYDAY_MAP = mapOf(
            "MO" to 1,
            "TU" to 2,
            "WE" to 3,
            "TH" to 4,
            "FR" to 5,
            "SA" to 6,
            "SU" to 7
        )
    }

    /**
     * 解析 ICS 文件并返回导入结果
     */
    fun parse(fileUri: Uri): ImportResult {
        return try {
            context.contentResolver.openInputStream(fileUri)?.use { stream ->
                val builder = CalendarBuilder()
                val calendar: Calendar = builder.build(stream)

                val courses = mutableListOf<RawCourse>()
                val errors = mutableListOf<String>()

                val semesterStart = getSemesterStartDate(calendar)

                for (component in calendar.components) {
                    if (component is VEvent) {
                        try {
                            val eventCourses = parseVEvent(component, semesterStart)
                            courses.addAll(eventCourses)
                        } catch (e: Exception) {
                            errors.add("解析事件失败: ${e.message}")
                        }
                    }
                }

                ImportResult(
                    success = courses.isNotEmpty(),
                    courses = courses,
                    errors = errors,
                    totalCount = courses.size
                )
            } ?: ImportResult(
                success = false,
                courses = emptyList(),
                errors = listOf("无法打开文件"),
                totalCount = 0
            )
        } catch (e: Exception) {
            ImportResult(
                success = false,
                courses = emptyList(),
                errors = listOf("ICS 解析失败: ${e.message}"),
                totalCount = 0
            )
        }
    }

    private fun parseVEvent(event: VEvent, semesterStart: LocalDate?): List<RawCourse> {
        val summary = event.getProperty<Summary>("SUMMARY")?.value ?: ""
        if (summary.isBlank()) return emptyList()

        val location = event.getProperty<Location>("LOCATION")?.value ?: ""

        val dtStart = event.getProperty<DtStart>("DTSTART")?.date
            ?: return emptyList()

        val startDate = dtStart.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        val startTime = dtStart.toInstant().atZone(ZoneId.systemDefault()).toLocalTime()

        val dtEnd = event.getProperty<net.fortuna.ical4j.model.property.DtEnd>("DTEND")?.date
        val endTime = dtEnd?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalTime()

        // 计算节次（简化映射：假设每节课约45分钟，从第1节8:00开始）
        val startNode = timeToNode(startTime)
        val endNode = endTime?.let { timeToNode(it) } ?: startNode

        val rrule = event.getProperty<RRule>("RRULE")

        return if (rrule != null) {
            parseRecurringEvent(event, summary, location, startNode, endNode, semesterStart)
        } else {
            // 单次事件
            val dayOfWeek = startDate.dayOfWeek.value
            listOf(
                RawCourse(
                    name = summary,
                    classroom = location,
                    teacher = "",
                    dayOfWeek = dayOfWeek,
                    startNode = startNode,
                    endNode = endNode.coerceAtLeast(startNode),
                    startWeek = 1,
                    endWeek = 1,
                    weekType = "all"
                )
            )
        }
    }

    private fun parseRecurringEvent(
        event: VEvent,
        summary: String,
        location: String,
        startNode: Int,
        endNode: Int,
        semesterStart: LocalDate?
    ): List<RawCourse> {
        val rrule = event.getProperty<RRule>("RRULE")
            ?: return emptyList()

        val recur = rrule.recur
        val freq = recur.frequency

        if (freq != Recur.Frequency.WEEKLY) {
            // 非周重复的事件，作为单次事件处理
            val dtStart = event.getProperty<DtStart>("DTSTART")?.date
                ?: return emptyList()
            val startDate = dtStart.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            return listOf(
                RawCourse(
                    name = summary,
                    classroom = location,
                    teacher = "",
                    dayOfWeek = startDate.dayOfWeek.value,
                    startNode = startNode,
                    endNode = endNode.coerceAtLeast(startNode),
                    startWeek = 1,
                    endWeek = 1,
                    weekType = "all"
                )
            )
        }

        // 解析 BYDAY
        val byDayList = recur.dayList
        val dayOfWeek = if (byDayList.isNotEmpty()) {
            val dayCode = byDayList.first().toString().uppercase()
            BYDAY_MAP[dayCode] ?: 1
        } else {
            val dtStart = event.getProperty<DtStart>("DTSTART")?.date
                ?: return emptyList()
            dtStart.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().dayOfWeek.value
        }

        // 解析周次范围
        val (startWeek, endWeek, weekType) = calculateWeekRange(event, recur, semesterStart)

        return listOf(
            RawCourse(
                name = summary,
                classroom = location,
                teacher = "",
                dayOfWeek = dayOfWeek,
                startNode = startNode,
                endNode = endNode.coerceAtLeast(startNode),
                startWeek = startWeek,
                endWeek = endWeek,
                weekType = weekType
            )
        )
    }

    private fun calculateWeekRange(
        event: VEvent,
        recur: Recur,
        semesterStart: LocalDate?
    ): Triple<Int, Int, String> {
        val dtStart = event.getProperty<DtStart>("DTSTART")?.date
            ?: return Triple(1, 20, "all")

        val startDate = dtStart.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        val baseStartWeek = 1

        // 尝试从 UNTIL 获取结束日期
        val untilDate = recur.until?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()

        val endWeek = if (untilDate != null && semesterStart != null) {
            val weeksBetween = ChronoUnit.WEEKS.between(semesterStart, untilDate).toInt()
            (weeksBetween + 1).coerceIn(1, 25)
        } else {
            // 默认 20 周
            20
        }

        // 解析间隔（单双周）
        val interval = recur.interval
        val weekType = when (interval) {
            2 -> {
                // 根据开始日期判断是单周还是双周
                val startWeekNum = if (semesterStart != null) {
                    ChronoUnit.WEEKS.between(semesterStart, startDate).toInt() + 1
                } else 1
                if (startWeekNum % 2 == 1) "odd" else "even"
            }
            else -> "all"
        }

        val startWeek = if (semesterStart != null) {
            val weeksBetween = ChronoUnit.WEEKS.between(semesterStart, startDate).toInt()
            (weeksBetween + 1).coerceIn(1, endWeek)
        } else {
            baseStartWeek
        }

        return Triple(startWeek, endWeek, weekType)
    }

    private fun getSemesterStartDate(calendar: Calendar): LocalDate? {
        // 尝试从日历的 X-WR-TIMEZONE 或其他属性推断学期开始日期
        // 如果没有，返回 null，使用默认周次
        return null
    }

    private fun timeToNode(time: java.time.LocalTime): Int {
        // 简化的时间到节次映射
        // 假设：第1节 8:00，每节约45分钟，课间休息
        return when {
            time.hour < 8 -> 1
            time.hour == 8 && time.minute < 30 -> 1
            time.hour == 8 -> 2
            time.hour == 9 && time.minute < 45 -> 2
            time.hour == 9 -> 3
            time.hour == 10 && time.minute < 30 -> 3
            time.hour == 10 -> 4
            time.hour == 11 && time.minute < 45 -> 4
            time.hour == 11 -> 5
            time.hour == 12 -> 5
            time.hour == 13 -> 6
            time.hour == 14 && time.minute < 30 -> 6
            time.hour == 14 -> 7
            time.hour == 15 && time.minute < 45 -> 7
            time.hour == 15 -> 8
            time.hour == 16 && time.minute < 30 -> 8
            time.hour == 16 -> 9
            time.hour == 17 && time.minute < 45 -> 9
            time.hour == 17 -> 10
            time.hour == 18 -> 10
            time.hour == 19 -> 11
            time.hour == 20 -> 12
            else -> 12
        }
    }
}
