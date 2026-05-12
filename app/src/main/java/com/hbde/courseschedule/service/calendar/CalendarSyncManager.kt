package com.hbde.courseschedule.service.calendar

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import com.hbde.courseschedule.data.local.SettingsDataStore
import com.hbde.courseschedule.data.local.entity.CourseEntity
import com.hbde.courseschedule.data.local.entity.EventEntity
import com.hbde.courseschedule.data.local.entity.TimeSlot
import com.hbde.courseschedule.data.local.entity.toTimeSlotList
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore
) {

    companion object {
        private const val CALENDAR_NAME = "课程表"
        private const val CALENDAR_ACCOUNT_NAME = "course_schedule_local"
        private const val CALENDAR_ACCOUNT_TYPE = CalendarContract.ACCOUNT_TYPE_LOCAL
        private const val SYNC_PREFIX_COURSE = "course_"
        private const val SYNC_PREFIX_EVENT = "event_"
    }

    private val contentResolver: ContentResolver = context.contentResolver

    /**
     * 检查日历同步是否已启用
     */
    suspend fun isSyncEnabled(): Boolean {
        return settingsDataStore.calendarSyncEnabled.first()
    }

    /**
     * 设置日历同步开关
     */
    suspend fun setSyncEnabled(enabled: Boolean) {
        settingsDataStore.setCalendarSyncEnabled(enabled)
    }

    /**
     * 将课程列表同步到系统日历
     */
    suspend fun syncCoursesToCalendar(courses: List<CourseEntity>, timeSlots: List<TimeSlot>) = withContext(Dispatchers.IO) {
        // TODO: 检查 READ_CALENDAR / WRITE_CALENDAR 权限
        val calendarId = getOrCreateLocalCalendar() ?: return@withContext
        removeCalendarEventsByPrefix(SYNC_PREFIX_COURSE)

        val semesterStartDate = getSemesterStartDate()

        for (course in courses) {
            insertCourseEvent(course, calendarId, semesterStartDate, timeSlots)
        }
    }

    /**
     * 将日程列表同步到系统日历
     */
    suspend fun syncEventsToCalendar(events: List<EventEntity>) = withContext(Dispatchers.IO) {
        // TODO: 检查 READ_CALENDAR / WRITE_CALENDAR 权限
        val calendarId = getOrCreateLocalCalendar() ?: return@withContext
        removeCalendarEventsByPrefix(SYNC_PREFIX_EVENT)

        for (event in events) {
            insertEventEntity(event, calendarId)
        }
    }

    /**
     * 删除所有已同步的课程/日程事件
     */
    suspend fun removeCalendarEvents() = withContext(Dispatchers.IO) {
        // TODO: 检查 READ_CALENDAR / WRITE_CALENDAR 权限
        removeCalendarEventsByPrefix(SYNC_PREFIX_COURSE)
        removeCalendarEventsByPrefix(SYNC_PREFIX_EVENT)
    }

    /**
     * 获取或创建本地日历账户
     */
    private fun getOrCreateLocalCalendar(): Long? {
        val existingId = findLocalCalendarId()
        if (existingId != null) return existingId

        val values = ContentValues().apply {
            put(CalendarContract.Calendars.NAME, CALENDAR_NAME)
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CALENDAR_NAME)
            put(CalendarContract.Calendars.CALENDAR_COLOR, 0xFF2196F3.toInt())
            put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
            put(CalendarContract.Calendars.OWNER_ACCOUNT, CALENDAR_ACCOUNT_NAME)
            put(CalendarContract.Calendars.VISIBLE, 1)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
            put(CalendarContract.Calendars.ACCOUNT_NAME, CALENDAR_ACCOUNT_NAME)
            put(CalendarContract.Calendars.ACCOUNT_TYPE, CALENDAR_ACCOUNT_TYPE)
        }

        val uri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, CALENDAR_ACCOUNT_NAME)
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CALENDAR_ACCOUNT_TYPE)
            .build()

        return try {
            val resultUri = contentResolver.insert(uri, values)
            resultUri?.let { ContentUris.parseId(it) }
        } catch (e: SecurityException) {
            null
        }
    }

    /**
     * 查找本地日历 ID
     */
    private fun findLocalCalendarId(): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.NAME
        )
        val selection = "${CalendarContract.Calendars.NAME} = ?"
        val selectionArgs = arrayOf(CALENDAR_NAME)

        var cursor: Cursor? = null
        return try {
            cursor = contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )
            if (cursor != null && cursor.moveToFirst()) {
                cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
            } else {
                null
            }
        } catch (e: SecurityException) {
            null
        } finally {
            cursor?.close()
        }
    }

    /**
     * 插入单个课程事件到日历
     */
    private fun insertCourseEvent(
        course: CourseEntity,
        calendarId: Long,
        semesterStartDate: Calendar,
        timeSlots: List<TimeSlot>
    ) {
        val startTimeSlot = timeSlots.getOrNull(course.startNode - 1) ?: return
        val endTimeSlot = timeSlots.getOrNull(course.endNode - 1) ?: return

        val eventStartTime = calculateCourseDateTime(semesterStartDate, course.startWeek, course.dayOfWeek, startTimeSlot.startTime)
        val eventEndTime = calculateCourseDateTime(semesterStartDate, course.startWeek, course.dayOfWeek, endTimeSlot.endTime)

        val untilDate = calculateCourseDateTime(semesterStartDate, course.endWeek, course.dayOfWeek, endTimeSlot.endTime)
        val untilStr = formatUntilDate(untilDate)

        val rrule = buildString {
            append("FREQ=WEEKLY")
            append(";BYDAY=${dayOfWeekToRRuleDay(course.dayOfWeek)}")
            append(";UNTIL=$untilStr")
            if (course.weekType.uppercase() == "ODD") {
                append(";INTERVAL=2")
            } else if (course.weekType.uppercase() == "EVEN") {
                append(";INTERVAL=2")
            }
        }

        val description = buildString {
            if (!course.teacher.isNullOrBlank()) append("教师：${course.teacher}")
            if (!course.notes.isNullOrBlank()) {
                if (isNotEmpty()) append("\n")
                append("备注：${course.notes}")
            }
        }

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, course.name)
            put(CalendarContract.Events.DTSTART, eventStartTime.timeInMillis)
            put(CalendarContract.Events.DTEND, eventEndTime.timeInMillis)
            put(CalendarContract.Events.RRULE, rrule)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.EVENT_LOCATION, course.classroom ?: "")
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.SYNC_DATA1, "$SYNC_PREFIX_COURSE${course.id}")
        }

        try {
            contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        } catch (e: SecurityException) {
            // TODO: 处理权限异常
        }
    }

    /**
     * 插入单个日程事件到日历
     */
    private fun insertEventEntity(event: EventEntity, calendarId: Long) {
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, event.title)
            put(CalendarContract.Events.DTSTART, event.startTime)
            put(CalendarContract.Events.DTEND, event.endTime)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.EVENT_LOCATION, event.location ?: "")
            put(CalendarContract.Events.DESCRIPTION, event.notes ?: "")
            put(CalendarContract.Events.SYNC_DATA1, "$SYNC_PREFIX_EVENT${event.id}")
        }

        try {
            contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        } catch (e: SecurityException) {
            // TODO: 处理权限异常
        }
    }

    /**
     * 根据前缀删除已同步的事件
     */
    private fun removeCalendarEventsByPrefix(prefix: String) {
        val selection = "${CalendarContract.Events.SYNC_DATA1} LIKE ?"
        val selectionArgs = arrayOf("$prefix%")

        try {
            contentResolver.delete(CalendarContract.Events.CONTENT_URI, selection, selectionArgs)
        } catch (e: SecurityException) {
            // TODO: 处理权限异常
        }
    }

    /**
     * 计算课程的具体日期时间
     */
    private fun calculateCourseDateTime(
        semesterStartDate: Calendar,
        week: Int,
        dayOfWeek: Int,
        timeStr: String
    ): Calendar {
        val result = semesterStartDate.clone() as Calendar

        // 调整到目标周（semesterStartDate 已经是第1周周一）
        result.add(Calendar.WEEK_OF_YEAR, week - 1)

        // 调整到目标星期几（dayOfWeek: 1=周一, 7=周日）
        val targetDayOfWeek = when (dayOfWeek) {
            1 -> Calendar.MONDAY
            2 -> Calendar.TUESDAY
            3 -> Calendar.WEDNESDAY
            4 -> Calendar.THURSDAY
            5 -> Calendar.FRIDAY
            6 -> Calendar.SATURDAY
            7 -> Calendar.SUNDAY
            else -> Calendar.MONDAY
        }
        result.set(Calendar.DAY_OF_WEEK, targetDayOfWeek)

        // 解析时间字符串（格式：HH:mm）
        val parts = timeStr.split(":")
        if (parts.size == 2) {
            result.set(Calendar.HOUR_OF_DAY, parts[0].toIntOrNull() ?: 0)
            result.set(Calendar.MINUTE, parts[1].toIntOrNull() ?: 0)
        }
        result.set(Calendar.SECOND, 0)
        result.set(Calendar.MILLISECOND, 0)

        return result
    }

    /**
     * 获取学期开始日期（默认当前日期所在周的周一）
     * TODO: 从 SettingsDataStore 读取真实的学期开始日期
     */
    private fun getSemesterStartDate(): Calendar {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar
    }

    /**
     * 将 dayOfWeek 转换为 RRULE 的 BYDAY 格式
     */
    private fun dayOfWeekToRRuleDay(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            1 -> "MO"
            2 -> "TU"
            3 -> "WE"
            4 -> "TH"
            5 -> "FR"
            6 -> "SA"
            7 -> "SU"
            else -> "MO"
        }
    }

    /**
     * 格式化 UNTIL 日期为 RRULE 格式（UTC，yyyyMMdd'T'HHmmss'Z'）
     */
    private fun formatUntilDate(calendar: Calendar): String {
        val utcCalendar = calendar.clone() as Calendar
        utcCalendar.timeZone = TimeZone.getTimeZone("UTC")
        val year = utcCalendar.get(Calendar.YEAR)
        val month = utcCalendar.get(Calendar.MONTH) + 1
        val day = utcCalendar.get(Calendar.DAY_OF_MONTH)
        val hour = utcCalendar.get(Calendar.HOUR_OF_DAY)
        val minute = utcCalendar.get(Calendar.MINUTE)
        val second = utcCalendar.get(Calendar.SECOND)
        return String.format("%04d%02d%02dT%02d%02d%02dZ", year, month, day, hour, minute, second)
    }
}
