package com.hbde.courseschedule.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.hbde.courseschedule.MainActivity
import com.hbde.courseschedule.R
import com.hbde.courseschedule.data.local.AppDatabase
import com.hbde.courseschedule.ui.schedule.ScheduleViewModel
import com.hbde.courseschedule.data.local.entity.CourseEntity
import com.hbde.courseschedule.data.local.entity.TimeSlot
import com.hbde.courseschedule.data.local.entity.toTimeSlotList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.max

/**
 * 2x2 小尺寸 Widget：显示下一节课信息
 */
class NextCourseWidgetProvider : AppWidgetProvider() {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val database = AppDatabase.getInstance(context)
        val courseDao = database.courseDao()
        val timeTableDao = database.timeTableDao()

        coroutineScope.launch {
            val todayDayOfWeek = getTodayDayOfWeek()
            val currentWeek = ScheduleViewModel.calculateCurrentWeek()
            val currentCalendar = Calendar.getInstance()
            val currentHour = currentCalendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = currentCalendar.get(Calendar.MINUTE)
            val currentTimeMinutes = currentHour * 60 + currentMinute

            // 获取时间段配置
            val timeSlots = try {
                val timeTable = timeTableDao.getDefaultTimeTable()
                timeTable?.timeSlots?.toTimeSlotList() ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }

            try {
                courseDao.getCoursesByWeekAndDay(currentWeek, todayDayOfWeek)
                    .collect { courses ->
                        val nextCourse = findNextCourse(courses, currentTimeMinutes, timeSlots)
                        val remoteViews = buildRemoteViews(context, nextCourse, timeSlots, currentTimeMinutes)
                        withContext(Dispatchers.Main) {
                            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
                        }
                    }
            } catch (_: Exception) {
                val remoteViews = buildRemoteViews(context, null, timeSlots, currentTimeMinutes)
                withContext(Dispatchers.Main) {
                    appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
                }
            }
        }
    }

    private fun findNextCourse(
        courses: List<CourseEntity>,
        currentTimeMinutes: Int,
        timeSlots: List<TimeSlot>
    ): CourseEntity? {
        return courses
            .filter { course ->
                val slotStartMinutes = getSlotStartMinutes(course.startNode, timeSlots)
                slotStartMinutes != null && slotStartMinutes > currentTimeMinutes
            }
            .minByOrNull { course ->
                getSlotStartMinutes(course.startNode, timeSlots) ?: Int.MAX_VALUE
            }
    }

    private fun getSlotStartMinutes(node: Int, timeSlots: List<TimeSlot>): Int? {
        val slot = timeSlots.getOrNull(node - 1) ?: return null
        val parts = slot.startTime.split(":")
        if (parts.size != 2) return null
        return parts[0].toIntOrNull()?.times(60)?.plus(parts[1].toIntOrNull() ?: 0)
    }

    private fun buildRemoteViews(
        context: Context,
        nextCourse: CourseEntity?,
        timeSlots: List<TimeSlot>,
        currentTimeMinutes: Int
    ): RemoteViews {
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_next_course)

        // 点击打开应用
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews.setOnClickPendingIntent(R.id.widget_next_course_root, openAppPendingIntent)

        if (nextCourse != null) {
            remoteViews.setTextViewText(R.id.widget_next_course_name, nextCourse.name)
            remoteViews.setTextViewText(R.id.widget_next_course_classroom, nextCourse.classroom ?: "教室待定")

            val slotStartMinutes = getSlotStartMinutes(nextCourse.startNode, timeSlots)
            val countdownText = if (slotStartMinutes != null) {
                val diffMinutes = slotStartMinutes - currentTimeMinutes
                if (diffMinutes > 0) {
                    val hours = diffMinutes / 60
                    val minutes = diffMinutes % 60
                    if (hours > 0) {
                        "还有 ${hours}小时${minutes}分钟"
                    } else {
                        "还有 ${minutes} 分钟"
                    }
                } else {
                    "即将开始"
                }
            } else {
                "第${nextCourse.startNode}-${nextCourse.endNode}节"
            }
            remoteViews.setTextViewText(R.id.widget_next_course_countdown, countdownText)

            remoteViews.setViewVisibility(R.id.widget_next_course_name, android.view.View.VISIBLE)
            remoteViews.setViewVisibility(R.id.widget_next_course_classroom, android.view.View.VISIBLE)
            remoteViews.setViewVisibility(R.id.widget_next_course_countdown, android.view.View.VISIBLE)
            remoteViews.setViewVisibility(R.id.widget_next_course_empty, android.view.View.GONE)
        } else {
            remoteViews.setViewVisibility(R.id.widget_next_course_name, android.view.View.GONE)
            remoteViews.setViewVisibility(R.id.widget_next_course_classroom, android.view.View.GONE)
            remoteViews.setViewVisibility(R.id.widget_next_course_countdown, android.view.View.GONE)
            remoteViews.setViewVisibility(R.id.widget_next_course_empty, android.view.View.VISIBLE)
        }

        return remoteViews
    }

    private fun getTodayDayOfWeek(): Int {
        val calendar = Calendar.getInstance()
        val day = calendar.get(Calendar.DAY_OF_WEEK)
        return if (day == Calendar.SUNDAY) 7 else day - 1
    }

    companion object {
        /**
         * 触发所有 2x2 Widget 更新
         */
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, NextCourseWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, NextCourseWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
