package com.hbde.courseschedule.service.liveactivity

import android.content.Context
import android.util.Log
import com.hbde.courseschedule.data.local.entity.CourseEntity
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 实时活动管理器统一入口
 * 根据设备品牌路由到对应的实时活动实现
 */
@Singleton
class LiveActivityManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationHelper: com.hbde.courseschedule.service.notification.NotificationHelper,
) {

    companion object {
        private const val TAG = "LiveActivityManager"
        const val NOTIFICATION_ID_LIVE_ACTIVITY = 88888
    }

    private val brand = DeviceBrandDetector.detectBrand()

    // 各品牌实现（懒加载）
    private val huaweiLiveView by lazy { HuaweiLiveView(context, notificationHelper) }
    private val xiaomiFocusNotification by lazy { XiaomiFocusNotification(context, notificationHelper) }
    private val floatingWindowManager by lazy { FloatingWindowManager(context) }

    /**
     * 显示实时活动
     * @param course 课程实体
     * @param minutesUntilStart 距离课程开始的分钟数
     */
    fun showLiveActivity(course: CourseEntity, minutesUntilStart: Int) {
        Log.d(TAG, "showLiveActivity: brand=$brand, course=${course.name}, minutesUntilStart=$minutesUntilStart")

        // 启动前台服务保活
        startLiveActivityService()

        when (brand) {
            DeviceBrandDetector.DeviceBrand.HUAWEI -> {
                huaweiLiveView.show(course, minutesUntilStart)
            }
            DeviceBrandDetector.DeviceBrand.XIAOMI -> {
                xiaomiFocusNotification.show(course, minutesUntilStart)
            }
            DeviceBrandDetector.DeviceBrand.HONOR -> {
                // Honor 与华为方案类似
                huaweiLiveView.show(course, minutesUntilStart)
            }
            DeviceBrandDetector.DeviceBrand.OPPO,
            DeviceBrandDetector.DeviceBrand.VIVO,
            DeviceBrandDetector.DeviceBrand.OTHER -> {
                // OPPO、VIVO 和其他品牌使用悬浮窗 fallback
                floatingWindowManager.show(course, minutesUntilStart)
            }
        }
    }

    /**
     * 更新实时活动状态
     * @param course 课程实体
     * @param status 当前实时活动状态
     */
    fun updateLiveActivity(course: CourseEntity, status: LiveActivityStatus) {
        Log.d(TAG, "updateLiveActivity: brand=$brand, status=$status")

        when (brand) {
            DeviceBrandDetector.DeviceBrand.HUAWEI -> {
                huaweiLiveView.update(course, status)
            }
            DeviceBrandDetector.DeviceBrand.XIAOMI -> {
                xiaomiFocusNotification.update(course, status)
            }
            DeviceBrandDetector.DeviceBrand.HONOR -> {
                huaweiLiveView.update(course, status)
            }
            DeviceBrandDetector.DeviceBrand.OPPO,
            DeviceBrandDetector.DeviceBrand.VIVO,
            DeviceBrandDetector.DeviceBrand.OTHER -> {
                floatingWindowManager.update(course, status)
            }
        }
    }

    /**
     * 关闭实时活动
     */
    fun dismissLiveActivity() {
        Log.d(TAG, "dismissLiveActivity: brand=$brand")

        // 停止前台服务
        stopLiveActivityService()

        when (brand) {
            DeviceBrandDetector.DeviceBrand.HUAWEI -> {
                huaweiLiveView.dismiss()
            }
            DeviceBrandDetector.DeviceBrand.XIAOMI -> {
                xiaomiFocusNotification.dismiss()
            }
            DeviceBrandDetector.DeviceBrand.HONOR -> {
                huaweiLiveView.dismiss()
            }
            DeviceBrandDetector.DeviceBrand.OPPO,
            DeviceBrandDetector.DeviceBrand.VIVO,
            DeviceBrandDetector.DeviceBrand.OTHER -> {
                floatingWindowManager.dismiss()
            }
        }
    }

    /**
     * 启动前台服务保活
     */
    private fun startLiveActivityService() {
        try {
            val serviceIntent = Intent(context, LiveActivityService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "启动 LiveActivityService 失败", e)
        }
    }

    /**
     * 停止前台服务
     */
    private fun stopLiveActivityService() {
        try {
            val serviceIntent = Intent(context, LiveActivityService::class.java)
            context.stopService(serviceIntent)
        } catch (e: Exception) {
            Log.e(TAG, "停止 LiveActivityService 失败", e)
        }
    }
}
