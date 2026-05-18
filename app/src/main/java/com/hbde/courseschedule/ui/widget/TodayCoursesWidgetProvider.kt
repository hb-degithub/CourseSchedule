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
import com.hbde.courseschedule.data.local.entity.CourseEntity
import com.hbde.courseschedule.data.local.entity.ThemeConfigEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * 4x2 中尺寸 Widget：显示今日课程列表
 */
class TodayCoursesWidgetProvider : AppWidgetProvider() {

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
        val themeConfigDao = database.themeConfigDao()

        coroutineScope.launch {
            val todayDayOfWeek = getTodayDayOfWeek()
            val currentWeek = resolveCurrentWeek(context)

            val themeConfig = try {
                themeConfigDao.getThemeConfigSync()
            } catch (_: Exception) {
                null
            }

            try {
                val courses = courseDao.getCoursesByWeekAndDay(currentWeek, todayDayOfWeek).first()
                val sorted = courses.sortedBy { it.startNode }
                val remoteViews = buildRemoteViews(context, sorted, themeConfig, currentWeek)
                withContext(Dispatchers.Main) {
                    appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
                }
            } catch (_: Exception) {
                val remoteViews = buildRemoteViews(context, emptyList(), themeConfig, currentWeek)
                withContext(Dispatchers.Main) {
                    appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
                }
            }
        }
    }

    private fun buildRemoteViews(
        context: Context,
        courses: List<CourseEntity>,
        themeConfig: ThemeConfigEntity?,
        currentWeek: Int,
    ): RemoteViews {
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_today_courses)

        // Apply theme colors
        val widgetBgColor = if (themeConfig != null) {
            val bgValue = themeConfig.backgroundImage
            if (!bgValue.isNullOrBlank() && bgValue.startsWith("#")) {
                try {
                    AndroidColor.parseColor(bgValue)
                } catch (_: Exception) {
                    AndroidColor.parseColor("#FFF5F5F5")
                }
            } else {
                AndroidColor.parseColor("#FFF5F5F5")
            }
        } else {
            AndroidColor.parseColor("#FFF5F5F5")
        }
        remoteViews.setInt(R.id.widget_today_courses_root, "setBackgroundColor", widgetBgColor)

        val titleColor = themeConfig?.let {
            val primary = it.primaryColor
            val luminance = (0.299 * AndroidColor.red(primary) +
                    0.587 * AndroidColor.green(primary) +
                    0.114 * AndroidColor.blue(primary)) / 255
            if (luminance > 0.5f) AndroidColor.parseColor("#FF000000")
            else AndroidColor.parseColor("#FFFFFFFF")
        } ?: AndroidColor.parseColor("#FFFFFFFF")

        val subtitleColor = if (titleColor == AndroidColor.parseColor("#FF000000")) {
            AndroidColor.parseColor("#80000000")
        } else {
            AndroidColor.parseColor("#B3FFFFFF")
        }

        // Set title and date
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val weekDayNames = arrayOf("", "周一", "周二", "周三", "周四", "周五", "周六", "周日")
        val weekDay = weekDayNames[getTodayDayOfWeek()]
        remoteViews.setTextViewText(R.id.widget_today_title, "今日课程")
        remoteViews.setTextColor(R.id.widget_today_title, titleColor)
        remoteViews.setTextViewText(
            R.id.widget_today_date,
            "$month 月 $day 日 · $weekDay · 第 ${currentWeek} 周"
        )
        remoteViews.setTextColor(R.id.widget_today_date, subtitleColor)

        // Click to open app
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews.setOnClickPendingIntent(R.id.widget_today_courses_root, openAppPendingIntent)

        // Clear and populate courses
        remoteViews.removeAllViews(R.id.widget_today_courses_container)

        if (courses.isEmpty()) {
            remoteViews.setViewVisibility(R.id.widget_today_empty_hint, android.view.View.VISIBLE)
            remoteViews.setViewVisibility(R.id.widget_today_more_hint, android.view.View.GONE)
        } else {
            remoteViews.setViewVisibility(R.id.widget_today_empty_hint, android.view.View.GONE)

            val displayCount = minOf(courses.size, MAX_DISPLAY_COURSES)
            val colorPalette = listOf(
                0xFF4CAF50.toInt(),
                0xFF2196F3.toInt(),
                0xFFFF9800.toInt(),
                0xFF9C27B0.toInt(),
                0xFFF44336.toInt(),
                0xFF00BCD4.toInt()
            )

            val itemTextColor = themeConfig?.let {
                val primary = it.primaryColor
                val luminance = (0.299 * AndroidColor.red(primary) +
                        0.587 * AndroidColor.green(primary) +
                        0.114 * AndroidColor.blue(primary)) / 255
                if (luminance > 0.5f) AndroidColor.parseColor("#FF000000")
                else AndroidColor.parseColor("#FFFFFFFF")
            } ?: AndroidColor.parseColor("#FFFFFFFF")

            val secondaryTextColor = if (itemTextColor == AndroidColor.parseColor("#FF000000")) {
                AndroidColor.parseColor("#B3000000")
            } else {
                AndroidColor.parseColor("#B3FFFFFF")
            }

            for (i in 0 until displayCount) {
                val course = courses[i]
                val itemViews = RemoteViews(context.packageName, R.layout.widget_today_course_item)

                itemViews.setTextViewText(R.id.widget_today_item_name, course.name)
                itemViews.setTextColor(R.id.widget_today_item_name, itemTextColor)
                itemViews.setTextViewText(
                    R.id.widget_today_item_time,
                    "第${course.startNode}-${course.endNode}节"
                )
                itemViews.setTextColor(R.id.widget_today_item_time, secondaryTextColor)
                itemViews.setTextViewText(
                    R.id.widget_today_item_classroom,
                    course.classroom ?: ""
                )
                itemViews.setTextColor(R.id.widget_today_item_classroom, secondaryTextColor)

                val color = course.color ?: colorPalette[i % colorPalette.size]
                itemViews.setInt(R.id.widget_today_item_color_bar, "setBackgroundColor", color)

                // Click to open course detail
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
                itemViews.setOnClickPendingIntent(
                    R.id.widget_today_item_container,
                    coursePendingIntent
                )

                remoteViews.addView(R.id.widget_today_courses_container, itemViews)
            }

            if (courses.size > MAX_DISPLAY_COURSES) {
                val moreCount = courses.size - MAX_DISPLAY_COURSES
                remoteViews.setTextViewText(
                    R.id.widget_today_more_hint,
                    "还有 ${moreCount} 门课程..."
                )
                remoteViews.setTextColor(R.id.widget_today_more_hint, subtitleColor)
                remoteViews.setViewVisibility(R.id.widget_today_more_hint, android.view.View.VISIBLE)
            } else {
                remoteViews.setViewVisibility(R.id.widget_today_more_hint, android.view.View.GONE)
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
        private const val MAX_DISPLAY_COURSES = 4

        /**
         * 触发所有 4x2 TodayCourses Widget 更新
         */
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, TodayCoursesWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, TodayCoursesWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
