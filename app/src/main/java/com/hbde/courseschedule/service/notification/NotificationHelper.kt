package com.hbde.courseschedule.service.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.hbde.courseschedule.MainActivity
import com.hbde.courseschedule.R
import com.hbde.courseschedule.data.local.entity.CourseEntity
import com.hbde.courseschedule.service.alarm.AlarmReceiver
import com.hbde.courseschedule.service.alarm.AlarmScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    companion object {
        const val CHANNEL_IMPORTANT = "channel_important"
        const val CHANNEL_NORMAL = "channel_normal"
        const val CHANNEL_DAILY_OVERVIEW = "channel_daily_overview"
        const val CHANNEL_LIVE_ACTIVITY = "live_activity_channel"

        const val ACTION_ACKNOWLEDGE = "com.hbde.courseschedule.ACTION_ACKNOWLEDGE"
        const val ACTION_SNOOZE = "com.hbde.courseschedule.ACTION_SNOOZE"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }

    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    /**
     * 创建通知渠道
     */
    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        // 重要提醒渠道（课程提醒）
        val importantChannel = NotificationChannel(
            CHANNEL_IMPORTANT,
            "课程重要提醒",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "上课前的重要提醒通知，包含声音和振动"
            enableVibration(true)
            enableLights(true)
            setSound(
                android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }

        // 普通提醒渠道
        val normalChannel = NotificationChannel(
            CHANNEL_NORMAL,
            "普通提醒",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "一般的课程相关提醒"
        }

        // 每日概览渠道
        val dailyOverviewChannel = NotificationChannel(
            CHANNEL_DAILY_OVERVIEW,
            "每日课程概览",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "每天早上发送的当日课程概览"
        }

        // 实时活动通知渠道（灵动岛/实况窗/焦点通知）
        val liveActivityChannel = NotificationChannel(
            CHANNEL_LIVE_ACTIVITY,
            "实时活动",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "课程实时活动通知，显示课程倒计时和上课状态"
            enableVibration(false)
            enableLights(false)
            setSound(null, null)
        }

        notificationManager.createNotificationChannels(
            listOf(importantChannel, normalChannel, dailyOverviewChannel, liveActivityChannel),
        )
    }

    /**
     * 显示课程提醒通知
     */
    fun showCourseNotification(
        courseName: String,
        classroom: String?,
        minutesBefore: Int,
        courseId: Int = 0,
    ) {
        val notificationId = generateNotificationId(courseId)

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmScheduler.EXTRA_COURSE_ID, courseId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val timeText = when {
            minutesBefore <= 0 -> "即将开始"
            minutesBefore < 60 -> "${minutesBefore}分钟后开始"
            else -> "${minutesBefore / 60}小时${minutesBefore % 60}分钟后开始"
        }

        val acknowledgeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ACKNOWLEDGE
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val acknowledgePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 100 + 1,
            acknowledgeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(AlarmScheduler.EXTRA_COURSE_ID, courseId)
            putExtra(AlarmScheduler.EXTRA_COURSE_NAME, courseName)
            putExtra(AlarmScheduler.EXTRA_CLASSROOM, classroom ?: "")
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 100 + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_IMPORTANT)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(courseName)
            .setContentText("${classroom ?: "未知教室"} · $timeText")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("课程：$courseName\n教室：${classroom ?: "未知教室"}\n时间：$timeText"),
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "我知道了", acknowledgePendingIntent)
            .addAction(0, "延后5分钟", snoozePendingIntent)

        notificationManager.notify(notificationId, builder.build())
    }

    /**
     * 显示每日概览通知
     */
    fun showDailyOverviewNotification(todayCourses: List<CourseEntity>) {
        val notificationId = 99999

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val content = if (todayCourses.isEmpty()) {
            "今天没有课程，好好休息吧！"
        } else {
            val courseList = todayCourses.joinToString("\n") { course ->
                val timeText = "第${course.startNode}-${course.endNode}节"
                "• ${course.name} ${course.classroom?.let { "($it)" } ?: ""} $timeText"
            }
            "今天共有 ${todayCourses.size} 门课程\n$courseList"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_DAILY_OVERVIEW)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("今日课程概览")
            .setContentText(if (todayCourses.isEmpty()) "今天没有课程" else "今天共有 ${todayCourses.size} 门课程")
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        notificationManager.notify(notificationId, builder.build())
    }

    /**
     * 取消通知
     */
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    private fun generateNotificationId(courseId: Int): Int {
        return courseId * 1000 + 100
    }
}
