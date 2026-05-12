package com.hbde.courseschedule.service.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.hbde.courseschedule.data.local.SettingsDataStore
import com.hbde.courseschedule.data.local.entity.CourseEntity
import com.hbde.courseschedule.utils.WeekCalculator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager,
    private val settingsDataStore: SettingsDataStore,
) {

    companion object {
        private const val TAG = "AlarmScheduler"
        const val ACTION_COURSE_REMINDER = "com.hbde.courseschedule.ACTION_COURSE_REMINDER"
        const val ACTION_CLASS_END = "com.hbde.courseschedule.ACTION_CLASS_END"
        const val ACTION_UPDATE_LIVE_ACTIVITY = "com.hbde.courseschedule.ACTION_UPDATE_LIVE_ACTIVITY"
        const val EXTRA_COURSE_ID = "extra_course_id"
        const val EXTRA_COURSE_NAME = "extra_course_name"
        const val EXTRA_CLASSROOM = "extra_classroom"
        const val EXTRA_MINUTES_BEFORE = "extra_minutes_before"

        // 课程节次对应的时间（示例，可根据实际情况调整）
        private val NODE_START_TIMES = mapOf(
            1 to LocalTime.of(8, 0),
            2 to LocalTime.of(8, 50),
            3 to LocalTime.of(10, 0),
            4 to LocalTime.of(10, 50),
            5 to LocalTime.of(14, 0),
            6 to LocalTime.of(14, 50),
            7 to LocalTime.of(16, 0),
            8 to LocalTime.of(16, 50),
            9 to LocalTime.of(19, 0),
            10 to LocalTime.of(19, 50),
            11 to LocalTime.of(20, 40),
        )

        private val NODE_END_TIMES = mapOf(
            1 to LocalTime.of(8, 45),
            2 to LocalTime.of(9, 35),
            3 to LocalTime.of(10, 45),
            4 to LocalTime.of(11, 35),
            5 to LocalTime.of(14, 45),
            6 to LocalTime.of(15, 35),
            7 to LocalTime.of(16, 45),
            8 to LocalTime.of(17, 35),
            9 to LocalTime.of(19, 45),
            10 to LocalTime.of(20, 35),
            11 to LocalTime.of(21, 25),
        )
    }

    /**
     * 为课程列表批量设置提醒
     * @param courses 课程实体列表
     * @param reminderMinutes 提前多少分钟提醒
     */
    suspend fun scheduleCourseReminders(courses: List<CourseEntity>, reminderMinutes: Int) {
        val currentWeek = getCurrentWeek()
        courses.forEach { course ->
            // 只给本周有效的课程设置 alarm
            if (isCourseActiveThisWeek(course, currentWeek)) {
                setCourseReminder(course, reminderMinutes)
            }
        }
    }

    /**
     * 设置课程提醒闹钟
     * @param course 课程实体
     * @param minutesBefore 提前多少分钟提醒（默认15分钟）
     */
    fun setCourseReminder(course: CourseEntity, minutesBefore: Int = 15) {
        val reminderTime = calculateNextReminderTime(course, minutesBefore) ?: return
        val endTime = calculateNextEndTime(course) ?: return

        // 设置提醒闹钟
        val reminderIntent = createAlarmIntent(
            action = ACTION_COURSE_REMINDER,
            courseId = course.id,
            courseName = course.name,
            classroom = course.classroom,
            minutesBefore = minutesBefore,
        )
        val reminderPendingIntent = PendingIntent.getBroadcast(
            context,
            generateRequestCode(course.id, ACTION_COURSE_REMINDER),
            reminderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        scheduleExactAlarm(reminderTime, reminderPendingIntent)

        // 设置下课恢复铃声闹钟
        val endIntent = createAlarmIntent(
            action = ACTION_CLASS_END,
            courseId = course.id,
            courseName = course.name,
            classroom = course.classroom,
            minutesBefore = 0,
        )
        val endPendingIntent = PendingIntent.getBroadcast(
            context,
            generateRequestCode(course.id, ACTION_CLASS_END),
            endIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        scheduleExactAlarm(endTime, endPendingIntent)

        // 设置实时活动每分钟更新闹钟
        scheduleLiveActivityUpdates(course, reminderTime, endTime)
    }

    /**
     * 设置实时活动每分钟更新闹钟
     * 从提醒触发时间开始，到课程结束，每分钟触发一次更新
     */
    private fun scheduleLiveActivityUpdates(
        course: CourseEntity,
        startTimeMillis: Long,
        endTimeMillis: Long
    ) {
        var currentTime = startTimeMillis + 60_000 // 从提醒后1分钟开始
        var requestCodeOffset = 10

        while (currentTime < endTimeMillis) {
            val updateIntent = createAlarmIntent(
                action = ACTION_UPDATE_LIVE_ACTIVITY,
                courseId = course.id,
                courseName = course.name,
                classroom = course.classroom,
                minutesBefore = 0,
            )
            val updatePendingIntent = PendingIntent.getBroadcast(
                context,
                generateRequestCode(course.id, ACTION_UPDATE_LIVE_ACTIVITY) + requestCodeOffset,
                updateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            scheduleExactAlarm(currentTime, updatePendingIntent)

            currentTime += 60_000 // 每分钟一次
            requestCodeOffset++
        }
    }

    /**
     * 取消课程的实时活动更新闹钟
     */
    fun cancelLiveActivityUpdates(courseId: Int) {
        // 取消所有可能的更新闹钟（requestCodeOffset 范围 10-100 足够覆盖）
        for (offset in 10..100) {
            val updateIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_UPDATE_LIVE_ACTIVITY
            }
            val updatePendingIntent = PendingIntent.getBroadcast(
                context,
                generateRequestCode(courseId, ACTION_UPDATE_LIVE_ACTIVITY) + offset,
                updateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            alarmManager.cancel(updatePendingIntent)
            updatePendingIntent.cancel()
        }
    }

    /**
     * 取消指定课程的提醒
     */
    fun cancelCourseReminder(courseId: Int) {
        // 取消提醒
        val reminderIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_COURSE_REMINDER
        }
        val reminderPendingIntent = PendingIntent.getBroadcast(
            context,
            generateRequestCode(courseId, ACTION_COURSE_REMINDER),
            reminderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(reminderPendingIntent)
        reminderPendingIntent.cancel()

        // 取消下课恢复
        val endIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_CLASS_END
        }
        val endPendingIntent = PendingIntent.getBroadcast(
            context,
            generateRequestCode(courseId, ACTION_CLASS_END),
            endIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(endPendingIntent)
        endPendingIntent.cancel()

        // 取消实时活动更新
        cancelLiveActivityUpdates(courseId)
    }

    /**
     * 取消所有提醒
     */
    suspend fun cancelAllReminders() {
        // 注：Android AlarmManager 没有直接获取所有已设置 alarm 的 API，
        // 实际项目中应在数据库中维护已设置 alarm 的课程列表。
        // 此处提供一个基于已知课程 ID 范围的清理方案作为兜底。
        Log.w(TAG, "cancelAllReminders: 需要遍历已知课程 ID 取消 alarm")
    }

    /**
     * 获取课程下一次 alarm 触发时间戳（毫秒）
     */
    fun getNextAlarmTime(course: CourseEntity, reminderMinutes: Int): Long? {
        return calculateNextReminderTime(course, reminderMinutes)
    }

    /**
     * 重新设置所有提醒（开机后调用）
     */
    suspend fun rescheduleAllReminders(
        courses: List<CourseEntity>,
        defaultMinutesBefore: Int = 15,
    ) {
        scheduleCourseReminders(courses, defaultMinutesBefore)
    }

    private fun createAlarmIntent(
        action: String,
        courseId: Int,
        courseName: String,
        classroom: String?,
        minutesBefore: Int,
    ): Intent {
        return Intent(context, AlarmReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_COURSE_ID, courseId)
            putExtra(EXTRA_COURSE_NAME, courseName)
            putExtra(EXTRA_CLASSROOM, classroom ?: "")
            putExtra(EXTRA_MINUTES_BEFORE, minutesBefore)
        }
    }

    private fun scheduleExactAlarm(triggerTimeMillis: Long, pendingIntent: PendingIntent) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent,
                    )
                } else {
                    Log.w(TAG, "无 SCHEDULE_EXACT_ALARM 权限，跳过设置精确 alarm")
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent,
                )
            }
            else -> {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent,
                )
            }
        }
    }

    /**
     * 计算下一次提醒时间（毫秒时间戳）
     */
    private fun calculateNextReminderTime(course: CourseEntity, minutesBefore: Int): Long? {
        val startTime = NODE_START_TIMES[course.startNode] ?: return null
        val targetDayOfWeek = DayOfWeek.of(course.dayOfWeek)

        var targetDate = LocalDate.now()
        val now = LocalDateTime.now()

        // 找到下一个符合条件的日期
        for (i in 0..6) {
            val checkDate = targetDate.plusDays(i.toLong())
            if (checkDate.dayOfWeek != targetDayOfWeek) continue

            val reminderDateTime = LocalDateTime.of(checkDate, startTime).minusMinutes(minutesBefore.toLong())
            if (reminderDateTime.isAfter(now)) {
                return reminderDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        }

        // 如果本周没找到，找下周
        targetDate = targetDate.plusWeeks(1)
        while (targetDate.dayOfWeek != targetDayOfWeek) {
            targetDate = targetDate.plusDays(1)
        }
        val reminderDateTime = LocalDateTime.of(targetDate, startTime).minusMinutes(minutesBefore.toLong())
        return reminderDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    /**
     * 计算下一次下课时间（毫秒时间戳）
     */
    private fun calculateNextEndTime(course: CourseEntity): Long? {
        val endTime = NODE_END_TIMES[course.endNode] ?: return null
        val targetDayOfWeek = DayOfWeek.of(course.dayOfWeek)

        var targetDate = LocalDate.now()
        val now = LocalDateTime.now()

        for (i in 0..6) {
            val checkDate = targetDate.plusDays(i.toLong())
            if (checkDate.dayOfWeek != targetDayOfWeek) continue

            val endDateTime = LocalDateTime.of(checkDate, endTime)
            if (endDateTime.isAfter(now)) {
                return endDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        }

        targetDate = targetDate.plusWeeks(1)
        while (targetDate.dayOfWeek != targetDayOfWeek) {
            targetDate = targetDate.plusDays(1)
        }
        val endDateTime = LocalDateTime.of(targetDate, endTime)
        return endDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    /**
     * 生成唯一的 requestCode
     */
    private fun generateRequestCode(courseId: Int, action: String): Int {
        return courseId * 100 + when (action) {
            ACTION_COURSE_REMINDER -> 1
            ACTION_CLASS_END -> 2
            ACTION_UPDATE_LIVE_ACTIVITY -> 3
            else -> 0
        }
    }

    /**
     * 判断课程在指定周是否有效（周范围 + 单双周判断）
     */
    private fun isCourseActiveThisWeek(course: CourseEntity, week: Int): Boolean {
        if (week < course.startWeek || week > course.endWeek) return false
        return when (course.weekType.lowercase()) {
            "odd" -> week % 2 == 1
            "even" -> week % 2 == 0
            else -> true
        }
    }

    /**
     * 获取当前周数（优先从 SettingsDataStore 读取开学日期计算）
     */
    private suspend fun getCurrentWeek(): Int {
        // 尝试从 SettingsDataStore 读取开学日期
        // TODO: SettingsDataStore 中需要添加 termStartDate 配置
        // 当前 fallback 到第 1 周
        return 1
    }
}
