package com.hbde.courseschedule.service.liveactivity

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.hbde.courseschedule.R
import com.hbde.courseschedule.service.notification.NotificationHelper

/**
 * 实时活动前台服务
 * 用于保活悬浮窗实时活动，避免被系统清理
 *
 * 启动方式：
 * context.startForegroundService(Intent(context, LiveActivityService::class.java))
 */
class LiveActivityService : Service() {

    companion object {
        private const val TAG = "LiveActivityService"
        private const val FOREGROUND_NOTIFICATION_ID = 77777
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "LiveActivityService onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "LiveActivityService onStartCommand")

        // 启动前台服务，显示一个低侵入性的保活通知
        startForeground(FOREGROUND_NOTIFICATION_ID, createForegroundNotification())

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "LiveActivityService onDestroy")
    }

    /**
     * 创建前台服务通知
     */
    private fun createForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_LIVE_ACTIVITY)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("课程实时活动运行中")
            .setContentText("正在显示课程倒计时和上课状态")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}
