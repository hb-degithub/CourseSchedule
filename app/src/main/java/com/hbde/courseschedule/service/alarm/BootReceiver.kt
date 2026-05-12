package com.hbde.courseschedule.service.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d(TAG, "开机完成，开始重新设置课程提醒")

        // 使用 goAsync() 允许在后台执行异步操作
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                // TODO: 从数据库读取所有课程，重新设置闹钟
                // val courses = courseDao.getAllCourses()
                // alarmScheduler.rescheduleAllReminders(courses)

                // 当前先记录日志，等 DAO 实现后取消注释上面的代码
                Log.d(TAG, "闹钟重新调度完成（待 DAO 实现后启用）")
            } catch (e: Exception) {
                Log.e(TAG, "重新设置闹钟失败", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
