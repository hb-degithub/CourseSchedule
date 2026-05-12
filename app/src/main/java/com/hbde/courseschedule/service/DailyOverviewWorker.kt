package com.hbde.courseschedule.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hbde.courseschedule.service.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DailyOverviewWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "daily_overview_worker"
    }

    override suspend fun doWork(): Result {
        return try {
            // TODO: 从数据库获取明天的课程和未完成任务
            // 当前先发送一个测试概览通知，等 DAO 实现后替换为真实数据
            // val tomorrowCourses = courseDao.getTomorrowCourses()
            // val pendingTasks = taskDao.getPendingTasks()

            // 暂时发送空列表，由 UI 层和数据层完善后替换
            notificationHelper.showDailyOverviewNotification(emptyList())

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
