package com.hbde.courseschedule.service.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import com.hbde.courseschedule.data.local.SettingsDataStore
import com.hbde.courseschedule.service.audio.AudioModeManager
import com.hbde.courseschedule.service.liveactivity.LiveActivityManager
import com.hbde.courseschedule.service.liveactivity.LiveActivityStatus
import com.hbde.courseschedule.service.notification.NotificationHelper
import com.hbde.courseschedule.service.tts.TtsManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AlarmReceiver：接收 Alarm 广播，触发通知、语音播报、静音模式切换
 */
@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
    }

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var ttsManager: TtsManager

    @Inject
    lateinit var audioModeManager: AudioModeManager

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    @Inject
    lateinit var liveActivityManager: LiveActivityManager

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AlarmScheduler.ACTION_COURSE_REMINDER -> {
                handleCourseReminder(context, intent)
            }
            AlarmScheduler.ACTION_UPDATE_LIVE_ACTIVITY -> {
                handleUpdateLiveActivity(context, intent)
            }
            AlarmScheduler.ACTION_CLASS_END -> {
                handleClassEnd()
            }
            NotificationHelper.ACTION_ACKNOWLEDGE -> {
                val notificationId = intent.getIntExtra(
                    NotificationHelper.EXTRA_NOTIFICATION_ID,
                    -1,
                )
                if (notificationId != -1) {
                    notificationHelper.cancelNotification(notificationId)
                }
            }
            NotificationHelper.ACTION_SNOOZE -> {
                handleSnooze(context, intent)
            }
        }
    }

    /**
     * 处理课程提醒：显示通知 + 语音播报 + 自动静音
     */
    private fun handleCourseReminder(context: Context, intent: Intent) {
        val courseId = intent.getIntExtra(AlarmScheduler.EXTRA_COURSE_ID, -1)
        val courseName = intent.getStringExtra(AlarmScheduler.EXTRA_COURSE_NAME) ?: "未知课程"
        val classroom = intent.getStringExtra(AlarmScheduler.EXTRA_CLASSROOM)
        val minutesBefore = intent.getIntExtra(AlarmScheduler.EXTRA_MINUTES_BEFORE, 15)

        Log.d(TAG, "课程提醒: $courseName, 教室: $classroom, 提前: ${minutesBefore}分钟")

        // 显示通知
        notificationHelper.showCourseNotification(
            courseName = courseName,
            classroom = classroom,
            minutesBefore = minutesBefore,
            courseId = courseId,
        )

        // 显示实时活动（灵动岛/实况窗）
        val course = createCourseEntityFromIntent(intent, courseId)
        liveActivityManager.showLiveActivity(course, minutesBefore)

        // 使用 goAsync() 返回的 pendingResult 生命周期，避免内存泄漏
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                // 如果用户开启了 TTS，调用 TtsManager 播报
                val ttsEnabled = settingsDataStore.ttsEnabled.first()
                if (ttsEnabled) {
                    ttsManager.speakCourseReminder(
                        courseName = courseName,
                        classroom = classroom ?: "",
                        minutes = minutesBefore,
                    )
                }

                // 如果用户开启了自动静音，调用 AudioModeManager 切换静音/振动
                val autoSilentEnabled = settingsDataStore.autoSilentEnabled.first()
                if (autoSilentEnabled) {
                    // 读取用户偏好的静音模式（完全静音或振动）
                    val silentModeType = settingsDataStore.silentModeType.first()
                    val targetMode = when (silentModeType) {
                        "silent" -> AudioManager.RINGER_MODE_SILENT
                        else -> AudioManager.RINGER_MODE_VIBRATE
                    }
                    audioModeManager.enterSilentMode(targetMode)
                }
            } catch (e: Exception) {
                Log.e(TAG, "处理课程提醒失败", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * 处理实时活动每分钟更新
     */
    private fun handleUpdateLiveActivity(context: Context, intent: Intent) {
        val courseId = intent.getIntExtra(AlarmScheduler.EXTRA_COURSE_ID, -1)
        val courseName = intent.getStringExtra(AlarmScheduler.EXTRA_COURSE_NAME) ?: "未知课程"
        val classroom = intent.getStringExtra(AlarmScheduler.EXTRA_CLASSROOM)

        Log.d(TAG, "更新实时活动: $courseName")

        val course = createCourseEntityFromIntent(intent, courseId)

        // 计算当前课程状态
        val minutesUntilStart = com.hbde.courseschedule.utils.CourseStatusCalculator.calculateMinutesUntilStart(course)
        val minutesUntilEnd = com.hbde.courseschedule.utils.CourseStatusCalculator.calculateMinutesUntilEnd(course)
        val isInClass = com.hbde.courseschedule.utils.CourseStatusCalculator.isCurrentlyActive(course)

        val status = when {
            isInClass -> LiveActivityStatus.InClass(courseName, classroom)
            minutesUntilStart > 0 -> LiveActivityStatus.Countdown(minutesUntilStart)
            else -> LiveActivityStatus.Break(courseName, 0)
        }

        liveActivityManager.updateLiveActivity(course, status)
    }

    /**
     * 从 Intent 构建 CourseEntity（用于实时活动）
     */
    private fun createCourseEntityFromIntent(intent: Intent, courseId: Int): com.hbde.courseschedule.data.local.entity.CourseEntity {
        val courseName = intent.getStringExtra(AlarmScheduler.EXTRA_COURSE_NAME) ?: ""
        val classroom = intent.getStringExtra(AlarmScheduler.EXTRA_CLASSROOM)
        return com.hbde.courseschedule.data.local.entity.CourseEntity(
            id = courseId,
            name = courseName,
            classroom = classroom,
            dayOfWeek = 1,
            startNode = 1,
            endNode = 1,
            startWeek = 1,
            endWeek = 1,
            weekType = "all"
        )
    }

    /**
     * 处理课程结束：恢复铃声模式 + 关闭实时活动
     */
    private fun handleClassEnd() {
        Log.d(TAG, "课程结束，恢复铃声模式")

        // 关闭实时活动
        liveActivityManager.dismissLiveActivity()

        // 下课恢复铃声模式
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                audioModeManager.restoreRingerMode()
            } catch (e: Exception) {
                Log.e(TAG, "恢复铃声模式失败", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * 处理延后提醒（5分钟后再次提醒）
     */
    private fun handleSnooze(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(
            NotificationHelper.EXTRA_NOTIFICATION_ID,
            -1,
        )
        val courseId = intent.getIntExtra(AlarmScheduler.EXTRA_COURSE_ID, -1)
        val courseName = intent.getStringExtra(AlarmScheduler.EXTRA_COURSE_NAME) ?: "未知课程"
        val classroom = intent.getStringExtra(AlarmScheduler.EXTRA_CLASSROOM)

        // 取消当前通知
        if (notificationId != -1) {
            notificationHelper.cancelNotification(notificationId)
        }

        // 延后5分钟再次提醒
        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmScheduler.ACTION_COURSE_REMINDER
            putExtra(AlarmScheduler.EXTRA_COURSE_ID, courseId)
            putExtra(AlarmScheduler.EXTRA_COURSE_NAME, courseName)
            putExtra(AlarmScheduler.EXTRA_CLASSROOM, classroom ?: "")
            putExtra(AlarmScheduler.EXTRA_MINUTES_BEFORE, 0)
        }

        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context,
            courseId * 1000 + 500,
            snoozeIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val triggerTime = System.currentTimeMillis() + 5 * 60 * 1000 // 5分钟后

        when {
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S -> {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent,
                    )
                }
            }
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M -> {
                alarmManager.setExactAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent,
                )
            }
            else -> {
                alarmManager.setExact(
                    android.app.AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent,
                )
            }
        }
    }
}
