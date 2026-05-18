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
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_week)

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
        remoteViews.setInt(R.id.widget_week_root, "setBackgroundColor", widgetBgColor)

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

        // 设置标题日期
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val weekDayNames = arrayOf("", "周一", "周二", "周三", "周四", "周五", "周六", "周日")
        val weekDay = weekDayNames[getTodayDayOfWeek()]
        remoteViews.setTextViewText(R.id.widget_week_date, "$month 月 $day 日 · $weekDay · 第 ${currentWeek} 周")
        remoteViews.setTextColor(R.id.widget_week_title, titleColor)
        remoteViews.setTextColor(R.id.widget_week_date, subtitleColor)

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
            remoteViews.setTextColor(R.id.widget_week_empty_hint, subtitleColor)
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

            for ((index, course) in courses.withIndex()) {
                val itemViews = RemoteViews(context.packageName, R.layout.widget_week_item)

                itemViews.setTextViewText(R.id.widget_week_item_name, course.name)
                itemViews.setTextColor(R.id.widget_week_item_name, itemTextColor)
                itemViews.setTextViewText(
                    R.id.widget_week_item_time,
                    "第${course.startNode}-${course.endNode}节"
                )
                itemViews.setTextColor(R.id.widget_week_item_time, secondaryTextColor)
                itemViews.setTextViewText(
                    R.id.widget_week_item_classroom,
                    course.classroom ?: ""
                )
                itemViews.setTextColor(R.id.widget_week_item_classroom, secondaryTextColor)
                if (!course.teacher.isNullOrBlank()) {
                    itemViews.setTextViewText(R.id.widget_week_item_teacher, course.teacher)
                    itemViews.setTextColor(R.id.widget_week_item_teacher, secondaryTextColor)
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
