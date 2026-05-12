package com.hbde.courseschedule.ui.schedule

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hbde.courseschedule.data.model.CourseStatus
import com.hbde.courseschedule.ui.schedule.components.ScheduleGrid
import com.hbde.courseschedule.utils.CourseStatusCalculator
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel = hiltViewModel(),
    onNavigateToEditor: (courseId: Int?, dayOfWeek: Int?, startNode: Int?) -> Unit = { _, _, _ -> },
    onNavigateToImportMethod: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val today = LocalDate.now()
    val currentDayOfWeek = today.dayOfWeek.value // 1..7
    val currentTime = LocalTime.now()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "第${uiState.currentWeek}周",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = today.format(DateTimeFormatter.ofPattern("MM月dd日")),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.previousWeek() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "上一周"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.currentWeek() }) {
                        Text(
                            text = "本周",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    IconButton(onClick = { viewModel.nextWeek() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "下一周"
                        )
                    }
                    IconButton(onClick = { onNavigateToEditor(null, null, null) }) {
                        Icon(Icons.Filled.Add, contentDescription = "添加课程")
                    }
                    IconButton(onClick = onNavigateToImportMethod) {
                        Icon(Icons.Filled.Upload, contentDescription = "导入课表")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 课程状态卡片
            CourseStatusCard(
                status = uiState.courseStatus,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // 课表网格（带周切换动画）
            AnimatedContent(
                targetState = uiState.currentWeek,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                            slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "weekSwitch",
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) { week ->
                ScheduleGrid(
                    courses = uiState.courses,
                    currentDayOfWeek = currentDayOfWeek,
                    currentTime = currentTime,
                    timeSlots = uiState.timeSlots,
                    onEmptyCellClick = { dayOfWeek, startNode ->
                        onNavigateToEditor(null, dayOfWeek, startNode)
                    },
                    onCourseClick = { course ->
                        onNavigateToEditor(course.id, null, null)
                    }
                )
            }
        }
    }
}

@Composable
private fun CourseStatusCard(
    status: CourseStatus,
    modifier: Modifier = Modifier
) {
    val (icon, title, subtitle, containerColor, contentColor) = when (status) {
        is CourseStatus.InClass -> {
            val remainingStr = CourseStatusCalculator.formatDuration(status.remainingMinutes)
            Quintet(
                Icons.Filled.Notifications,
                "正在上课",
                "${status.course.name}，还剩 ${remainingStr}，教室 ${status.course.classroom ?: "未知"}",
                Color(0xFFE8F5E9), // 浅绿色
                Color(0xFF2E7D32)  // 深绿色
            )
        }

        is CourseStatus.Break -> {
            val remainingStr = CourseStatusCalculator.formatDuration(status.remainingMinutes)
            Quintet(
                Icons.Filled.Schedule,
                "课间休息",
                "下一节：${status.nextCourse.name}，还有 ${remainingStr}，教室 ${status.nextCourse.classroom ?: "未知"}",
                Color(0xFFE3F2FD), // 浅蓝色
                Color(0xFF1565C0)  // 深蓝色
            )
        }

        is CourseStatus.NoClass -> {
            Quintet(
                Icons.Filled.CheckCircle,
                "今天没有课",
                "好好休息，享受空闲时光",
                Color(0xFFF5F5F5), // 浅灰色
                Color(0xFF757575)  // 深灰色
            )
        }
    }

    Card(
        modifier = modifier.animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = subtitle,
                    color = contentColor.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 五元组数据类，用于状态卡片解构
 */
private data class Quintet<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
