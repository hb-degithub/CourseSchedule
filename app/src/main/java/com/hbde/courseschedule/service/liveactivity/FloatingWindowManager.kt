package com.hbde.courseschedule.service.liveactivity

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.hbde.courseschedule.data.local.entity.CourseEntity
import com.hbde.courseschedule.utils.CourseStatusCalculator

/**
 * 悬浮窗 Fallback 管理器
 * 使用 WindowManager.addView() 添加悬浮 View
 * 支持拖拽调整位置，点击展开显示更多信息
 *
 * 需要 SYSTEM_ALERT_WINDOW 权限（已在 Manifest 中声明）
 */
class FloatingWindowManager(private val context: Context) {

    companion object {
        private const val TAG = "FloatingWindowManager"
    }

    private val windowManager: WindowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private var composeView: ComposeView? = null
    private var windowParams: WindowManager.LayoutParams? = null
    private var isExpanded by mutableStateOf(false)
    private var currentCourse by mutableStateOf<CourseEntity?>(null)
    private var currentStatus by mutableStateOf<LiveActivityStatus>(
        LiveActivityStatus.Countdown(0)
    )

    /**
     * 检查是否有悬浮窗权限
     */
    fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    /**
     * 显示悬浮窗
     *
     * TODO: 如果 canDrawOverlays() 返回 false，需要引导用户去设置页面开启权限
     * 示例代码：
     * val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
     *     Uri.parse("package:${context.packageName}"))
     * context.startActivity(intent)
     */
    fun show(course: CourseEntity, minutesUntilStart: Int) {
        if (!canDrawOverlays()) {
            Log.w(TAG, "没有悬浮窗权限，无法显示悬浮窗")
            // TODO: 发送广播或回调通知 UI 层引导用户开启权限
            return
        }

        currentCourse = course
        currentStatus = LiveActivityStatus.Countdown(minutesUntilStart)
        isExpanded = false

        if (composeView == null) {
            createFloatingWindow()
        } else {
            updateFloatingWindow()
        }
    }

    /**
     * 更新悬浮窗状态
     */
    fun update(course: CourseEntity, status: LiveActivityStatus) {
        currentCourse = course
        currentStatus = status
        updateFloatingWindow()
    }

    /**
     * 关闭悬浮窗
     */
    fun dismiss() {
        composeView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                Log.e(TAG, "移除悬浮窗失败", e)
            }
            composeView = null
            windowParams = null
        }
    }

    /**
     * 创建悬浮窗
     */
    private fun createFloatingWindow() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 20 // 距顶部 20px
        }

        windowParams = params

        val view = ComposeView(context).apply {
            setContent {
                FloatingWindowContent(
                    course = currentCourse,
                    status = currentStatus,
                    isExpanded = isExpanded,
                    onToggleExpand = { isExpanded = !isExpanded },
                    onDismiss = { dismiss() }
                )
            }
        }

        // 设置 LifecycleOwner 和 SavedStateRegistryOwner，使 Compose 正常工作
        val lifecycleOwner = FloatingWindowLifecycleOwner()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        view.setViewTreeLifecycleOwner(lifecycleOwner)
        view.setViewTreeSavedStateRegistryOwner(lifecycleOwner)

        // 添加拖拽触摸监听
        view.setOnTouchListener(FloatingWindowTouchListener(params, view))

        composeView = view

        try {
            windowManager.addView(view, params)
        } catch (e: Exception) {
            Log.e(TAG, "添加悬浮窗失败", e)
        }
    }

    /**
     * 更新悬浮窗内容
     */
    private fun updateFloatingWindow() {
        composeView?.let { view ->
            view.setContent {
                FloatingWindowContent(
                    course = currentCourse,
                    status = currentStatus,
                    isExpanded = isExpanded,
                    onToggleExpand = { isExpanded = !isExpanded },
                    onDismiss = { dismiss() }
                )
            }
        }
    }

    // region 悬浮窗拖拽触摸监听

    /**
     * 悬浮窗拖拽触摸监听器
     */
    private inner class FloatingWindowTouchListener(
        private val params: WindowManager.LayoutParams,
        private val view: View
    ) : View.OnTouchListener {

        private var initialX = 0
        private var initialY = 0
        private var touchStartX = 0f
        private var touchStartY = 0f
        private var isDragging = false
        private val dragThreshold = 10f // 拖拽阈值

        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    isDragging = false
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - touchStartX
                    val dy = event.rawY - touchStartY

                    if (!isDragging && (Math.abs(dx) > dragThreshold || Math.abs(dy) > dragThreshold)) {
                        isDragging = true
                    }

                    if (isDragging) {
                        params.x = initialX + dx.toInt()
                        params.y = initialY + dy.toInt()
                        windowManager.updateViewLayout(view, params)
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // 点击事件：切换展开/收起
                        isExpanded = !isExpanded
                        updateFloatingWindow()
                    } else {
                        // 拖拽结束：吸附到屏幕边缘
                        snapToEdge(params, view)
                    }
                    return true
                }
            }
            return false
        }

        /**
         * 吸附到屏幕边缘
         */
        private fun snapToEdge(params: WindowManager.LayoutParams, view: View) {
            val displayMetrics = context.resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val viewWidth = view.width

            val targetX = if (params.x > screenWidth / 2 - viewWidth / 2) {
                // 吸附到右边
                screenWidth / 2 - viewWidth / 2
            } else {
                // 吸附到左边
                -screenWidth / 2 + viewWidth / 2
            }

            val animator = ValueAnimator.ofInt(params.x, targetX).apply {
                duration = 300
                interpolator = DecelerateInterpolator()
                addUpdateListener {
                    params.x = it.animatedValue as Int
                    windowManager.updateViewLayout(view, params)
                }
            }
            animator.start()
        }
    }

    // endregion

    // region LifecycleOwner 实现

    /**
     * 悬浮窗 LifecycleOwner
     * 为 ComposeView 提供生命周期支持
     */
    private class FloatingWindowLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateRegistryController = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

        init {
            savedStateRegistryController.performAttach()
        }

        fun handleLifecycleEvent(event: Lifecycle.Event) {
            lifecycleRegistry.handleLifecycleEvent(event)
        }
    }

    // endregion
}

// region Compose UI

/**
 * 悬浮窗 Compose UI
 */
@Composable
private fun FloatingWindowContent(
    course: CourseEntity?,
    status: LiveActivityStatus,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDismiss: () -> Unit
) {
    if (course == null) return

    val courseColor = course.color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .clickable { onToggleExpand() },
        contentAlignment = Alignment.Center
    ) {
        if (isExpanded) {
            ExpandedContent(
                course = course,
                status = status,
                courseColor = courseColor,
                onDismiss = onDismiss
            )
        } else {
            CollapsedContent(
                course = course,
                status = status,
                courseColor = courseColor
            )
        }
    }
}

/**
 * 收起状态（类似灵动岛胶囊）
 */
@Composable
private fun CollapsedContent(
    course: CourseEntity,
    status: LiveActivityStatus,
    courseColor: Color
) {
    val statusText = when (status) {
        is LiveActivityStatus.Countdown -> {
            if (status.minutes <= 0) "即将开始" else "${status.minutes}分钟后"
        }
        is LiveActivityStatus.InClass -> "正在上课"
        is LiveActivityStatus.Break -> {
            if (status.minutes <= 0) "即将开始" else "${status.minutes}分钟后"
        }
    }

    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 课程颜色条
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(courseColor)
        )

        // 课程名称
        Text(
            text = course.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(120.dp)
        )

        // 状态/倒计时
        Text(
            text = statusText,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 展开状态（显示更多信息）
 */
@Composable
private fun ExpandedContent(
    course: CourseEntity,
    status: LiveActivityStatus,
    courseColor: Color,
    onDismiss: () -> Unit
) {
    val (title, detail) = when (status) {
        is LiveActivityStatus.Countdown -> {
            val timeText = if (status.minutes <= 0) "即将开始" else "${status.minutes}分钟后"
            "距离上课还有 $timeText" to "教室: ${course.classroom ?: "未知教室"}"
        }
        is LiveActivityStatus.InClass -> {
            "正在上课" to "教室: ${status.classroom ?: "未知教室"}"
        }
        is LiveActivityStatus.Break -> {
            val timeText = if (status.minutes <= 0) "即将开始" else "${status.minutes}分钟后"
            "下一节课: ${status.nextCourseName}" to "${timeText}后开始"
        }
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        // 顶部：颜色条 + 课程名 + 关闭按钮
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(courseColor)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = course.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 状态详情
        Text(
            text = title,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = detail,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 教师信息
        course.teacher?.let { teacher ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "教师: $teacher",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 节次信息
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "第${course.startNode}-${course.endNode}节",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// endregion
