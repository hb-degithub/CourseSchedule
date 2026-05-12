package com.hbde.courseschedule.service.liveactivity

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.hbde.courseschedule.MainActivity
import com.hbde.courseschedule.R
import com.hbde.courseschedule.data.local.entity.CourseEntity
import com.hbde.courseschedule.service.alarm.AlarmScheduler
import com.hbde.courseschedule.service.notification.NotificationHelper
import java.lang.reflect.Method

/**
 * 小米焦点通知实现
 *
 * 小米焦点通知 API 不公开，采用以下策略：
 * 1. 优先尝试反射调用小米焦点通知 API（如果存在）
 * 2. 降级为高优先级通知模拟（IMPORTANCE_HIGH + setFullScreenIntent）
 */
class XiaomiFocusNotification(
    private val context: Context,
    private val notificationHelper: NotificationHelper,
) {

    companion object {
        private const val TAG = "XiaomiFocusNotification"
        private const val NOTIFICATION_ID = LiveActivityManager.NOTIFICATION_ID_LIVE_ACTIVITY
    }

    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    /**
     * 显示小米焦点通知
     */
    fun show(course: CourseEntity, minutesUntilStart: Int) {
        if (tryShowXiaomiFocusNotification(course, minutesUntilStart)) {
            return
        }

        showNotificationFallback(course, minutesUntilStart)
    }

    /**
     * 更新焦点通知状态
     */
    fun update(course: CourseEntity, status: LiveActivityStatus) {
        if (tryUpdateXiaomiFocusNotification(course, status)) {
            return
        }

        updateNotificationFallback(course, status)
    }

    /**
     * 关闭焦点通知
     */
    fun dismiss() {
        if (tryDismissXiaomiFocusNotification()) {
            return
        }

        notificationManager.cancel(NOTIFICATION_ID)
    }

    // region 反射调用小米焦点通知 API（尝试）

    /**
     * 尝试反射调用小米焦点通知 API
     * @return 是否成功
     */
    private fun tryShowXiaomiFocusNotification(course: CourseEntity, minutesUntilStart: Int): Boolean {
        return try {
            // 尝试反射小米 NotificationManager 的扩展方法
            // 小米焦点通知 API 属于 MIUI 私有 API
            val clazz = Class.forName("android.app.MiuiNotification")
            val miuiNotification = clazz.getDeclaredConstructor().newInstance()

            // 设置小米通知特性
            val messageCountField = clazz.getDeclaredField("messageCount")
            messageCountField.isAccessible = true
            messageCountField.set(miuiNotification, 0)

            val extraNotificationField = Notification::class.java.getDeclaredField("extraNotification")
            extraNotificationField.isAccessible = true

            val notification = buildXiaomiFocusNotificationInternal(course, minutesUntilStart)
            extraNotificationField.set(notification, miuiNotification)

            notificationManager.notify(NOTIFICATION_ID, notification)
            true
        } catch (e: ClassNotFoundException) {
            false
        } catch (e: NoSuchFieldException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun tryUpdateXiaomiFocusNotification(course: CourseEntity, status: LiveActivityStatus): Boolean {
        return try {
            val clazz = Class.forName("android.app.MiuiNotification")
            val miuiNotification = clazz.getDeclaredConstructor().newInstance()

            val messageCountField = clazz.getDeclaredField("messageCount")
            messageCountField.isAccessible = true
            messageCountField.set(miuiNotification, 0)

            val extraNotificationField = Notification::class.java.getDeclaredField("extraNotification")
            extraNotificationField.isAccessible = true

            val notification = buildXiaomiFocusNotificationInternal(course, status)
            extraNotificationField.set(notification, miuiNotification)

            notificationManager.notify(NOTIFICATION_ID, notification)
            true
        } catch (e: ClassNotFoundException) {
            false
        } catch (e: NoSuchFieldException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun tryDismissXiaomiFocusNotification(): Boolean {
        return try {
            // 尝试反射取消焦点通知
            val clazz = Class.forName("android.app.NotificationManager")
            val cancelMethod: Method? = clazz.methods.find {
                it.name == "cancel" || it.name == "cancelFocusNotification"
            }
            cancelMethod?.invoke(notificationManager, NOTIFICATION_ID)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun buildXiaomiFocusNotificationInternal(course: CourseEntity, minutesUntilStart: Int): Notification {
        return buildNotificationFallbackInternal(course, LiveActivityStatus.Countdown(minutesUntilStart))
    }

    private fun buildXiaomiFocusNotificationInternal(course: CourseEntity, status: LiveActivityStatus): Notification {
        return buildNotificationFallbackInternal(course, status)
    }

    // endregion

    // region 通知模拟 Fallback

    /**
     * 使用高优先级通知模拟焦点通知
     */
    private fun showNotificationFallback(course: CourseEntity, minutesUntilStart: Int) {
        val notification = buildNotificationFallbackInternal(
            course,
            LiveActivityStatus.Countdown(minutesUntilStart)
        )
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun updateNotificationFallback(course: CourseEntity, status: LiveActivityStatus) {
        val notification = buildNotificationFallbackInternal(course, status)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * 构建通知（内部方法）
     */
    private fun buildNotificationFallbackInternal(
        course: CourseEntity,
        status: LiveActivityStatus
    ): Notification {
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmScheduler.EXTRA_COURSE_ID, course.id)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // 构建自定义 RemoteViews 布局
        val collapsedView = RemoteViews(context.packageName, R.layout.notification_live_activity_collapsed)
        val expandedView = RemoteViews(context.packageName, R.layout.notification_live_activity_expanded)

        // 设置课程颜色条
        val courseColor = course.color ?: context.getColor(R.color.purple_500)
        collapsedView.setInt(R.id.color_bar, "setBackgroundColor", courseColor)
        expandedView.setInt(R.id.color_bar, "setBackgroundColor", courseColor)

        // 设置课程名称
        collapsedView.setTextViewText(R.id.tv_course_name, course.name)
        expandedView.setTextViewText(R.id.tv_course_name, course.name)

        // 设置教室
        val classroomText = course.classroom ?: "未知教室"
        collapsedView.setTextViewText(R.id.tv_classroom, classroomText)
        expandedView.setTextViewText(R.id.tv_classroom, classroomText)

        // 根据状态设置显示内容
        when (status) {
            is LiveActivityStatus.Countdown -> {
                val timeText = if (status.minutes <= 0) "即将开始" else "${status.minutes}分钟后"
                collapsedView.setTextViewText(R.id.tv_status, timeText)
                expandedView.setTextViewText(R.id.tv_status, timeText)
                expandedView.setTextViewText(R.id.tv_detail, "距离上课还有 $timeText")
            }
            is LiveActivityStatus.InClass -> {
                collapsedView.setTextViewText(R.id.tv_status, "正在上课")
                expandedView.setTextViewText(R.id.tv_status, "正在上课")
                expandedView.setTextViewText(R.id.tv_detail, "课程进行中，教室: ${status.classroom ?: "未知"}")
            }
            is LiveActivityStatus.Break -> {
                val timeText = if (status.minutes <= 0) "即将开始" else "${status.minutes}分钟后"
                collapsedView.setTextViewText(R.id.tv_status, "休息中 · $timeText")
                expandedView.setTextViewText(R.id.tv_status, "休息中")
                expandedView.setTextViewText(R.id.tv_detail, "下一节课: ${status.nextCourseName}，${timeText}后开始")
            }
        }

        // 设置教师信息（展开视图）
        expandedView.setTextViewText(R.id.tv_teacher, course.teacher ?: "")

        return NotificationCompat.Builder(context, NotificationHelper.CHANNEL_LIVE_ACTIVITY)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setCustomContentView(collapsedView)
            .setCustomBigContentView(expandedView)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(contentPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(contentPendingIntent, true)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                }
            }
            .build()
    }

    // endregion
}
