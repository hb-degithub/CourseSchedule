package com.hbde.courseschedule.service.widget

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.hbde.courseschedule.ui.widget.NextCourseWidgetProvider
import com.hbde.courseschedule.ui.widget.ScheduleWidgetProvider
import com.hbde.courseschedule.ui.widget.TodayCoursesWidgetProvider
import com.hbde.courseschedule.ui.widget.WeekOverviewWidgetProvider
import com.hbde.courseschedule.ui.widget.WeekWidgetProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * 定时更新所有 Widget 的 Worker
 */
@HiltWorker
class WidgetUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "widget_update_worker"

        /**
         * 注册定时 Widget 更新任务（每 15 分钟执行一次）
         */
        fun enqueuePeriodicWork(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        /**
         * 取消定时 Widget 更新任务
         */
        fun cancelPeriodicWork(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        return try {
            // 触发所有 Widget 更新
            updateAllWidgets(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}

/**
 * 更新所有类型的 Widget
 */
fun updateAllWidgets(context: Context) {
    ScheduleWidgetProvider.updateAllWidgets(context)
    NextCourseWidgetProvider.updateAllWidgets(context)
    WeekWidgetProvider.updateAllWidgets(context)
    TodayCoursesWidgetProvider.updateAllWidgets(context)
    WeekOverviewWidgetProvider.updateAllWidgets(context)
}
