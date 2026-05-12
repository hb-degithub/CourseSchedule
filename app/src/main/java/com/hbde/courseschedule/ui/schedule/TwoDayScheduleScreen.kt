package com.hbde.courseschedule.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hbde.courseschedule.data.local.entity.CourseEntity
import com.hbde.courseschedule.data.model.CourseListItemStatus
import com.hbde.courseschedule.utils.CourseStatusCalculator
import java.util.Calendar

private val DEFAULT_COURSE_COLORS = listOf(
    0xFF4CAF50, 0xFF2196F3, 0xFFFF9800, 0xFF9C27B0,
    0xFFF44336, 0xFF00BCD4, 0xFFFFEB3B, 0xFFFF5722
)

private val NODE_TIME_LABELS = mapOf(
    1 to "08:00", 2 to "08:50", 3 to "10:10", 4 to "11:00",
    5 to "14:00", 6 to "14:50", 7 to "16:10", 8 to "17:00",
    9 to "19:00", 10 to "19:50", 11 to "20:40", 12 to "21:30"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwoDayScheduleScreen(
    viewModel: TwoDayScheduleViewModel = hiltViewModel(),
    onNavigateToEditor: (courseId: Int?) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("两日课程")
                        Text(
                            text = getDateLabel(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToEditor(null) }) {
                        Icon(Icons.Filled.Add, contentDescription = "添加课程")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
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
            // Tab Row
            TabRow(
                selectedTabIndex = if (uiState.selectedTab == DayTab.TODAY) 0 else 1
            ) {
                Tab(
                    selected = uiState.selectedTab == DayTab.TODAY,
                    onClick = { viewModel.selectTab(DayTab.TODAY) },
                    text = {
                        Text(
                            "今天 (${getTodayWeekDayName()})",
                            fontWeight = if (uiState.selectedTab == DayTab.TODAY)
                                FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = uiState.selectedTab == DayTab.TOMORROW,
                    onClick = { viewModel.selectTab(DayTab.TOMORROW) },
                    text = {
                        Text(
                            "明天 (${getTomorrowWeekDayName()})",
                            fontWeight = if (uiState.selectedTab == DayTab.TOMORROW)
                                FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            // Course List
            val courses = if (uiState.selectedTab == DayTab.TODAY)
                uiState.todayCourses else uiState.tomorrowCourses

            if (courses.isEmpty()) {
                EmptyCourseState(
                    dayLabel = if (uiState.selectedTab == DayTab.TODAY) "今天" else "明天"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(courses, key = { it.course.id }) { courseWithStatus ->
                        CourseListItem(
                            courseWithStatus = courseWithStatus,
                            onClick = { onNavigateToEditor(courseWithStatus.course.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseListItem(
    courseWithStatus: CourseWithStatus,
    onClick: () -> Unit
) {
    val course = courseWithStatus.course
    val status = courseWithStatus.status

    val colorIndex = course.id % DEFAULT_COURSE_COLORS.size
    val courseColor = course.color?.let { Color(it) }
        ?: Color(DEFAULT_COURSE_COLORS[colorIndex])

    // 根据状态确定卡片背景色
    val cardBackgroundColor = when (status) {
        CourseListItemStatus.ONGOING -> courseColor.copy(alpha = 0.12f)
        CourseListItemStatus.ENDED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        CourseListItemStatus.UPCOMING -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBackgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = when (status) {
                CourseListItemStatus.ONGOING -> 4.dp
                else -> 1.dp
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(courseColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Time column
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${course.startNode}-${course.endNode}节",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val startTime = NODE_TIME_LABELS[course.startNode] ?: ""
                Text(
                    text = startTime,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Course info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = course.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!course.classroom.isNullOrBlank()) {
                        InfoChip(text = course.classroom)
                    }
                    if (!course.teacher.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        InfoChip(text = course.teacher)
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Status badge
            StatusBadge(
                status = status,
                minutesUntil = when (status) {
                    CourseListItemStatus.ONGOING -> courseWithStatus.minutesUntilEnd
                    CourseListItemStatus.UPCOMING -> courseWithStatus.minutesUntilStart
                    CourseListItemStatus.ENDED -> 0
                }
            )
        }
    }
}

@Composable
private fun StatusBadge(
    status: CourseListItemStatus,
    minutesUntil: Int
) {
    val (text, backgroundColor, textColor) = when (status) {
        CourseListItemStatus.ONGOING -> Triple(
            "进行中",
            Color(0xFFE8F5E9),
            Color(0xFF2E7D32)
        )
        CourseListItemStatus.ENDED -> Triple(
            "已结束",
            Color(0xFFF5F5F5),
            Color(0xFF757575)
        )
        CourseListItemStatus.UPCOMING -> {
            val timeStr = if (minutesUntil > 0) {
                CourseStatusCalculator.formatDuration(minutesUntil) + "后"
            } else ""
            Triple(
                timeStr.ifEmpty { "未开始" },
                Color(0xFFE3F2FD),
                Color(0xFF1565C0)
            )
        }
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun InfoChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyCourseState(dayLabel: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$dayLabel 暂无课程",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "好好享受空闲时光吧",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

private fun getDateLabel(): String {
    val calendar = Calendar.getInstance()
    val month = calendar.get(Calendar.MONTH) + 1
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    return "${month}月${day}日"
}

private fun getTodayWeekDayName(): String {
    val weekDays = arrayOf("", "周一", "周二", "周三", "周四", "周五", "周六", "周日")
    val calendar = Calendar.getInstance()
    val day = calendar.get(Calendar.DAY_OF_WEEK)
    val dow = if (day == Calendar.SUNDAY) 7 else day - 1
    return weekDays[dow]
}

private fun getTomorrowWeekDayName(): String {
    val weekDays = arrayOf("", "周一", "周二", "周三", "周四", "周五", "周六", "周日")
    val calendar = Calendar.getInstance()
    val day = calendar.get(Calendar.DAY_OF_WEEK)
    val dow = if (day == Calendar.SUNDAY) 7 else day - 1
    val tomorrowDow = if (dow == 7) 1 else dow + 1
    return weekDays[tomorrowDow]
}
