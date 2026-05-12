package com.hbde.courseschedule.service.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.hbde.courseschedule.data.local.SettingsDataStore
import com.hbde.courseschedule.service.SilentModeManager
import com.hbde.courseschedule.service.notification.NotificationHelper
import com.hbde.courseschedule.service.tts.TtsManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    lateinit var silentModeManager: SilentModeManager

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AlarmScheduler.ACTION_COURSE_REMINDER -> {
                handleCourseReminder(context, intent)
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

                // 如果用户开启了自动静音，调用 SilentModeManager 切换静音/振动
                val autoSilentEnabled = settingsDataStore.autoSilentEnabled.first()
                if (autoSilentEnabled) {
                    silentModeManager.enterSilentMode(android.media.AudioManager.RINGER_MODE_VIBRATE)
                }
            } catch (e: Exception) {
                Log.e(TAG, "处理课程提醒失败", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleClassEnd() {
        Log.d(TAG, "课程结束，恢复铃声模式")

        // 下课恢复铃声模式
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                silentModeManager.restoreRingerMode()
            } catch (e: Exception) {
                Log.e(TAG, "恢复铃声模式失败", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

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
