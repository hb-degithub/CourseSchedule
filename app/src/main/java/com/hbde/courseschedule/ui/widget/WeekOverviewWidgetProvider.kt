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
 * 4x4 大尺寸 Widget：显示本周课程概览（每天课程数量 + 课程点阵）
 */
class WeekOverviewWidgetProvider : AppWidgetProvider() {

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
            val currentWeek = resolveCurrentWeek(context)

            val themeConfig = try {
                themeConfigDao.getThemeConfigSync()
            } catch (_: Exception) {
                null
            }

            // Fetch courses for all days of the week
            val weekCourses = mutableMapOf<Int, List<CourseEntity>>()
            var hasError = false

            for (day in 1..7) {
                try {
                    val courses = courseDao.getCoursesByWeekAndDay(currentWeek, day).first()
                    weekCourses[day] = courses.sortedBy { it.startNode }
                } catch (_: Exception) {
                    hasError = true
                    weekCourses[day] = emptyList()
                }
            }

            val remoteViews = buildRemoteViews(context, weekCourses, themeConfig, currentWeek)
            withContext(Dispatchers.Main) {
                appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
            }
        }
    }

    private fun buildRemoteViews(
        context: Context,
        weekCourses: Map<Int, List<CourseEntity>>,
        themeConfig: ThemeConfigEntity?,
        currentWeek: Int
    ): RemoteViews {
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_week_overview)

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
        remoteViews.setInt(R.id.widget_week_overview_root, "setBackgroundColor", widgetBgColor)

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

        // Set title and week info
        remoteViews.setTextViewText(R.id.widget_week_overview_title, "本周概览")
        remoteViews.setTextColor(R.id.widget_week_overview_title, titleColor)
        remoteViews.setTextViewText(
            R.id.widget_week_overview_week_info,
            "第 ${currentWeek} 周"
        )
        remoteViews.setTextColor(R.id.widget_week_overview_week_info, subtitleColor)

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
        remoteViews.setOnClickPendingIntent(
            R.id.widget_week_overview_root,
            openAppPendingIntent
        )

        // Clear and populate day columns
        remoteViews.removeAllViews(R.id.widget_week_overview_days_container)

        val totalCourses = weekCourses.values.sumOf { it.size }
        if (totalCourses == 0) {
            remoteViews.setViewVisibility(
                R.id.widget_week_overview_empty_hint,
                android.view.View.VISIBLE
            )
        } else {
            remoteViews.setViewVisibility(
                R.id.widget_week_overview_empty_hint,
                android.view.View.GONE
            )

            val colorPalette = listOf(
                0xFF4CAF50.toInt(),
                0xFF2196F3.toInt(),
                0xFFFF9800.toInt(),
                0xFF9C27B0.toInt(),
                0xFFF44336.toInt(),
                0xFF00BCD4.toInt()
            )

            val todayDayOfWeek = getTodayDayOfWeek()

            for (day in 1..7) {
                val courses = weekCourses[day] ?: emptyList()
                val dayColumn = RemoteViews(
                    context.packageName,
                    R.layout.widget_week_overview_day_column
                )

                // Show course count
                val countText = if (courses.isEmpty()) "-" else "${courses.size}"
                dayColumn.setTextViewText(
                    R.id.widget_week_overview_course_count,
                    countText
                )

                // Highlight today with different text color
                val countColor = if (day == todayDayOfWeek) {
                    themeConfig?.primaryColor ?: AndroidColor.parseColor("#FF2196F3")
                } else {
                    subtitleColor
                }
                dayColumn.setTextColor(
                    R.id.widget_week_overview_course_count,
                    countColor
                )

                // Add course dots
                dayColumn.removeAllViews(R.id.widget_week_overview_dots_container)
                val maxDots = minOf(courses.size, 6)
                for (i in 0 until maxDots) {
                    val dot = RemoteViews(
                        context.packageName,
                        R.layout.widget_week_overview_dot
                    )
                    val color = courses[i].color ?: colorPalette[i % colorPalette.size]
                    dot.setInt(
                        R.id.widget_week_overview_dot,
                        "setBackgroundColor",
                        color
                    )
                    dayColumn.addView(
                        R.id.widget_week_overview_dots_container,
                        dot
                    )
                }

                remoteViews.addView(
                    R.id.widget_week_overview_days_container,
                    dayColumn
                )
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
         * 触发所有 4x4 WeekOverview Widget 更新
         */
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, WeekOverviewWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, WeekOverviewWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
