package com.hbde.courseschedule

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.hbde.courseschedule.service.notification.NotificationHelper
import com.hbde.courseschedule.service.widget.WidgetUpdateWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CourseScheduleApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // 创建通知渠道
        notificationHelper.createChannels()
        // 启动定时 Widget 更新任务
        WidgetUpdateWorker.enqueuePeriodicWork(this)
    }
}
