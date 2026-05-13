package com.hbde.courseschedule.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.widget.RemoteViews
import com.hbde.courseschedule.MainActivity
import com.hbde.courseschedule.R
import com.hbde.courseschedule.data.local.AppDatabase
import com.hbde.courseschedule.ui.schedule.ScheduleViewModel
import com.hbde.courseschedule.data.local.entity.CourseEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * 4x4 大尺寸 Widget：显示当天课程列表（更详细）
 */
class WeekWidgetProvider : AppWidgetProvider() {

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

        coroutineScope.launch {
            val todayDayOfWeek = getTodayDayOfWeek()
            val currentWeek = ScheduleViewModel.calculateCurrentWeek()

            try {
                courseDao.getCoursesByWeekAndDay(currentWeek, todayDayOfWeek)
                    .collect { courses ->
                        val sorted = courses.sortedBy { it.startNode }
                        val remoteViews = buildRemoteViews(context, sorted)
                        withContext(Dispatchers.Main) {
                            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
                        }
                    }
            } catch (_: Exception) {
                val remoteViews = buildRemoteViews(context, emptyList())
                withContext(Dispatchers.Main) {
                    appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
                }
            }
        }
    }

    private fun buildRemoteViews(
        context: Context,
        courses: List<CourseEntity>
    ): RemoteViews {
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_week)

        // 设置标题日期
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val weekDayNames = arrayOf("", "周一", "周二", "周三", "周四", "周五", "周六", "周日")
        val weekDay = weekDayNames[getTodayDayOfWeek()]
        remoteViews.setTextViewText(R.id.widget_week_date, "$month 月 $day 日 · $weekDay · 第 ${ScheduleViewModel.calculateCurrentWeek()} 周")

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
        remoteViews.setOnClickPendingIntent(R.id.widget_week_root, openAppPendingIntent)

        // 清空课程容器
        remoteViews.removeAllViews(R.id.widget_week_courses_container)

        if (courses.isEmpty()) {
            remoteViews.setViewVisibility(R.id.widget_week_empty_hint, android.view.View.VISIBLE)
        } else {
            remoteViews.setViewVisibility(R.id.widget_week_empty_hint, android.view.View.GONE)

            val colorPalette = listOf(
                0xFF4CAF50.toInt(),
                0xFF2196F3.toInt(),
                0xFFFF9800.toInt(),
                0xFF9C27B0.toInt(),
                0xFFF44336.toInt(),
                0xFF00BCD4.toInt(),
                0xFFFFEB3B.toInt(),
                0xFFFF5722.toInt()
            )

            for ((index, course) in courses.withIndex()) {
                val itemViews = RemoteViews(context.packageName, R.layout.widget_week_item)

                itemViews.setTextViewText(R.id.widget_week_item_name, course.name)
                itemViews.setTextViewText(
                    R.id.widget_week_item_time,
                    "第${course.startNode}-${course.endNode}节"
                )
                itemViews.setTextViewText(
                    R.id.widget_week_item_classroom,
                    course.classroom ?: ""
                )
                if (!course.teacher.isNullOrBlank()) {
                    itemViews.setTextViewText(R.id.widget_week_item_teacher, course.teacher)
                    itemViews.setViewVisibility(R.id.widget_week_item_teacher, android.view.View.VISIBLE)
                } else {
                    itemViews.setViewVisibility(R.id.widget_week_item_teacher, android.view.View.GONE)
                }

                val color = course.color ?: colorPalette[index % colorPalette.size]
                itemViews.setInt(R.id.widget_week_item_color_bar, "setBackgroundColor", color)

                // 点击打开课程详情
                val courseIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(ScheduleWidgetProvider.EXTRA_COURSE_ID, course.id)
                }
                val coursePendingIntent = PendingIntent.getActivity(
                    context,
                    course.id,
                    courseIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                itemViews.setOnClickPendingIntent(R.id.widget_week_item_container, coursePendingIntent)

                remoteViews.addView(R.id.widget_week_courses_container, itemViews)
            }
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
         * 触发所有 4x4 Week Widget 更新
         */
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, WeekWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, WeekWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
