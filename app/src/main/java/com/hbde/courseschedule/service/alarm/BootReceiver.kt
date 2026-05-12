package com.hbde.courseschedule.service.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.hbde.courseschedule.data.local.SettingsDataStore
import com.hbde.courseschedule.data.local.dao.CourseDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    @Inject
    lateinit var courseDao: CourseDao

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d(TAG, "开机完成，开始重新设置课程提醒")

        // 使用 goAsync() 允许在后台执行异步操作
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                // 从数据库读取所有课程
                val courses = courseDao.getAllCourses().first()
                // 从 SettingsDataStore 读取 reminderMinutes
                val reminderMinutes = settingsDataStore.reminderMinutes.first()
                // 调用 AlarmScheduler 重新设置所有提醒
                alarmScheduler.scheduleCourseReminders(courses, reminderMinutes)
                Log.d(TAG, "闹钟重新调度完成，共 ${courses.size} 门课程")
            } catch (e: Exception) {
                Log.e(TAG, "重新设置闹钟失败", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
