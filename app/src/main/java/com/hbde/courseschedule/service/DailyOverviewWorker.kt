package com.hbde.courseschedule.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hbde.courseschedule.data.local.dao.CourseDao
import com.hbde.courseschedule.data.local.dao.EventDao
import com.hbde.courseschedule.data.local.entity.CourseEntity
import com.hbde.courseschedule.data.local.entity.EventEntity
import com.hbde.courseschedule.service.notification.NotificationHelper
import com.hbde.courseschedule.utils.WeekCalculator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

@HiltWorker
class DailyOverviewWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationHelper: NotificationHelper,
    private val courseDao: CourseDao,
    private val eventDao: EventDao,
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "daily_overview_worker"
    }

    override suspend fun doWork(): Result {
        return try {
            // 获取明天的日期和星期几
            val tomorrow = LocalDate.now().plusDays(1)
            val tomorrowDayOfWeek = tomorrow.dayOfWeek.value // 1=周一 ... 7=周日
            val tomorrowWeek = 1 // TODO: 从 SettingsDataStore 读取开学日期计算当前周数

            // 从数据库查询明天的所有课程（按 startNode 排序）
            val tomorrowCourses = courseDao.getCoursesByWeekAndDay(tomorrowWeek, tomorrowDayOfWeek)
                .first()
                .filter { course ->
                    // 单双周过滤
                    when (course.weekType.lowercase()) {
                        "odd" -> tomorrowWeek % 2 == 1
                        "even" -> tomorrowWeek % 2 == 0
                        else -> true
                    }
                }
                .sortedBy { it.startNode }

            // 从数据库查询明天截止的事件（考试/作业）
            val dayStart = tomorrow.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val dayEnd = tomorrow.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val tomorrowEvents = eventDao.getEventsByTimeRange(dayStart, dayEnd)
                .first()
                .filter { !it.isCompleted }
                .sortedBy { it.endTime }

            // 构建摘要文本
            val dayOfWeekText = getDayOfWeekText(tomorrow.dayOfWeek)
            val content = buildString {
                if (tomorrowCourses.isNotEmpty()) {
                    appendLine("明日（${dayOfWeekText}）有 ${tomorrowCourses.size} 节课")
                    tomorrowCourses.forEach { course ->
                        val timeText = getCourseTimeText(course)
                        appendLine("${timeText} ${course.name} @ ${course.classroom ?: "未知教室"}")
                    }
                } else {
                    appendLine("明日（${dayOfWeekText}）没有课程")
                }

                if (tomorrowEvents.isNotEmpty()) {
                    appendLine()
                    appendLine("待办：")
                    tomorrowEvents.forEach { event ->
                        appendLine("${event.title}（截止明天）")
                    }
                }
            }

            // 发送「明日概览」通知
            notificationHelper.showDailyOverviewNotification(
                title = "明日课程概览",
                content = content.toString(),
                courseCount = tomorrowCourses.size,
            )

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun getDayOfWeekText(dayOfWeek: DayOfWeek): String {
        return when (dayOfWeek) {
            DayOfWeek.MONDAY -> "周一"
            DayOfWeek.TUESDAY -> "周二"
            DayOfWeek.WEDNESDAY -> "周三"
            DayOfWeek.THURSDAY -> "周四"
            DayOfWeek.FRIDAY -> "周五"
            DayOfWeek.SATURDAY -> "周六"
            DayOfWeek.SUNDAY -> "周日"
        }
    }

    private fun getCourseTimeText(course: CourseEntity): String {
        // 节次对应的时间映射（与 AlarmScheduler 保持一致）
        val nodeStartTimes = mapOf(
            1 to "08:00",
            2 to "08:50",
            3 to "10:00",
            4 to "10:50",
            5 to "14:00",
            6 to "14:50",
            7 to "16:00",
            8 to "16:50",
            9 to "19:00",
            10 to "19:50",
            11 to "20:40",
        )
        return nodeStartTimes[course.startNode] ?: "--:--"
    }
}
