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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class ScheduleWidgetProvider : AppWidgetProvider() {

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

    override fun onEnabled(context: Context) {
        // First widget created — nothing special needed
    }

    override fun onDisabled(context: Context) {
        // Last widget removed — nothing special needed
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

            // 读取主题配置（同步查询，取第一条）
            val themeConfig = try {
                themeConfigDao.getThemeConfigSync()
            } catch (_: Exception) {
                null
            }

            // Collect one emission from the Flow
            val courses = try {
                courseDao.getCoursesByDayOfWeek(todayDayOfWeek)
                    .collect { list ->
                        val sorted = list.sortedBy { it.startNode }
                        val remoteViews = buildRemoteViews(context, sorted, themeConfig)
                        withContext(Dispatchers.Main) {
                            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
                        }
                    }
            } catch (_: Exception) {
                // If Flow fails, show empty state
                val remoteViews = buildRemoteViews(context, emptyList(), themeConfig)
                withContext(Dispatchers.Main) {
                    appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
                }
            }
        }
    }

    private fun buildRemoteViews(
        context: Context,
        courses: List<CourseEntity>,
        themeConfig: ThemeConfigEntity?
    ): RemoteViews {
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_schedule)

        // 根据主题配置设置 Widget 背景
        val widgetBgColor = if (themeConfig != null) {
            // 如果主题使用图片背景，Widget 保持默认纯色（RemoteViews 不支持复杂图片处理）
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
        remoteViews.setInt(R.id.widget_root, "setBackgroundColor", widgetBgColor)

        // 根据主题配置设置标题颜色
        val titleColor = themeConfig?.let {
            val primary = it.primaryColor
            // 深色主色用白字，浅色用黑字
            val luminance = (0.299 * AndroidColor.red(primary) +
                    0.587 * AndroidColor.green(primary) +
                    0.114 * AndroidColor.blue(primary)) / 255
            if (luminance > 0.5f) AndroidColor.parseColor("#FF000000")
            else AndroidColor.parseColor("#FFFFFFFF")
        } ?: AndroidColor.parseColor("#FF000000")

        // Set title with today's date
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val weekDayNames = arrayOf("", "周日", "周一", "周二", "周三", "周四", "周五", "周六")
        val weekDay = weekDayNames[calendar.get(Calendar.DAY_OF_WEEK)]
        remoteViews.setTextViewText(R.id.widget_date, "${month}月${day}日 ${weekDay}")
        remoteViews.setTextColor(R.id.widget_date, titleColor)

        // Setup click on root to open app
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)

        // Clear previous course items
        remoteViews.removeAllViews(R.id.widget_courses_container)

        if (courses.isEmpty()) {
            remoteViews.setViewVisibility(R.id.widget_empty_hint, android.view.View.VISIBLE)
            remoteViews.setViewVisibility(R.id.widget_more_hint, android.view.View.GONE)
        } else {
            remoteViews.setViewVisibility(R.id.widget_empty_hint, android.view.View.GONE)

            val displayCount = minOf(courses.size, MAX_DISPLAY_COURSES)
            val colorPalette = listOf(
                0xFF4CAF50.toInt(), // Green
                0xFF2196F3.toInt(), // Blue
                0xFFFF9800.toInt(), // Orange
                0xFF9C27B0.toInt(), // Purple
                0xFFF44336.toInt(), // Red
                0xFF00BCD4.toInt(), // Cyan
                0xFFFFEB3B.toInt(), // Yellow
                0xFFFF5722.toInt()  // Deep Orange
            )

            for (i in 0 until displayCount) {
                val course = courses[i]
                val itemViews = RemoteViews(context.packageName, R.layout.widget_schedule_item)

                itemViews.setTextViewText(R.id.widget_item_name, course.name)
                itemViews.setTextViewText(
                    R.id.widget_item_time,
                    "第${course.startNode}-${course.endNode}节"
                )
                itemViews.setTextViewText(
                    R.id.widget_item_classroom,
                    course.classroom ?: ""
                )

                // 根据主题配置调整文字颜色
                val itemTextColor = themeConfig?.let {
                    val primary = it.primaryColor
                    val luminance = (0.299 * AndroidColor.red(primary) +
                            0.587 * AndroidColor.green(primary) +
                            0.114 * AndroidColor.blue(primary)) / 255
                    if (luminance > 0.5f) AndroidColor.parseColor("#FF000000")
                    else AndroidColor.parseColor("#FFFFFFFF")
                } ?: AndroidColor.parseColor("#FF000000")
                itemViews.setTextColor(R.id.widget_item_name, itemTextColor)
                itemViews.setTextColor(R.id.widget_item_time, itemTextColor)
                itemViews.setTextColor(R.id.widget_item_classroom, itemTextColor)

                // Set color bar
                val color = course.color ?: colorPalette[i % colorPalette.size]
                itemViews.setInt(R.id.widget_item_color_bar, "setBackgroundColor", color)

                // Click to open app with course id
                val courseIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(EXTRA_COURSE_ID, course.id)
                }
                val coursePendingIntent = PendingIntent.getActivity(
                    context,
                    course.id,
                    courseIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                itemViews.setOnClickPendingIntent(R.id.widget_item_name, coursePendingIntent)

                remoteViews.addView(R.id.widget_courses_container, itemViews)
            }

            // Show "more" hint if there are additional courses
            if (courses.size > MAX_DISPLAY_COURSES) {
                val moreCount = courses.size - MAX_DISPLAY_COURSES
                remoteViews.setTextViewText(R.id.widget_more_hint, "还有 ${moreCount} 门课程...")
                remoteViews.setViewVisibility(R.id.widget_more_hint, android.view.View.VISIBLE)
            } else {
                remoteViews.setViewVisibility(R.id.widget_more_hint, android.view.View.GONE)
            }
        }

        return remoteViews
    }

    private fun getTodayDayOfWeek(): Int {
        val calendar = Calendar.getInstance()
        // Calendar.DAY_OF_WEEK: Sunday=1, Monday=2, ..., Saturday=7
        // Convert to: Monday=1, Tuesday=2, ..., Sunday=7
        val day = calendar.get(Calendar.DAY_OF_WEEK)
        return if (day == Calendar.SUNDAY) 7 else day - 1
    }

    companion object {
        private const val MAX_DISPLAY_COURSES = 4
        const val EXTRA_COURSE_ID = "extra_course_id"

        /**
         * Trigger widget update from anywhere in the app.
         */
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, ScheduleWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_courses_container)
                // Also trigger onUpdate
                val intent = Intent(context, ScheduleWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
