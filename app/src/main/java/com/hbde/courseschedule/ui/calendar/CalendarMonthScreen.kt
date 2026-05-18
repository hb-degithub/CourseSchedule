package com.hbde.courseschedule.ui.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hbde.courseschedule.data.model.Event
import com.hbde.courseschedule.data.model.EventType
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarMonthScreen(
    viewModel: CalendarMonthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("日历") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 年月标题 + 切换按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.previousMonth() }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "上个月",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${uiState.currentMonth.year}年${uiState.currentMonth.monthValue}月",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = { viewModel.nextMonth() }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "下个月",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // 星期标题
            Row(modifier = Modifier.fillMaxWidth()) {
                DayOfWeek.entries.forEach { dayOfWeek ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 日历网格（带动画 + 滑动）
            AnimatedContent(
                targetState = uiState.currentMonth,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                            slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "monthSwitch"
            ) { month ->
                CalendarGrid(
                    month = month,
                    today = uiState.today,
                    datesWithCourses = uiState.datesWithCourses,
                    datesWithEvents = uiState.datesWithEvents,
                    onDateClick = { date ->
                        viewModel.selectDate(date)
                        showBottomSheet = true
                    },
                    onSwipeLeft = { viewModel.nextMonth() },
                    onSwipeRight = { viewModel.previousMonth() }
                )
            }

            // 图例
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LegendItem(
                    color = MaterialTheme.colorScheme.primary,
                    label = "有课程"
                )
                LegendItem(
                    color = MaterialTheme.colorScheme.tertiary,
                    label = "有事件"
                )
                LegendItem(
                    color = MaterialTheme.colorScheme.error,
                    label = "有考试"
                )
            }
        }
    }

    // 底部弹窗：显示当天课程和事件
    if (showBottomSheet && uiState.selectedDate != null) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            DayDetailSheet(
                date = uiState.selectedDate!!,
                courses = uiState.selectedDateCourses,
                events = uiState.selectedDateEvents,
                onDismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showBottomSheet = false
                    }
                }
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    month: YearMonth,
    today: LocalDate,
    datesWithCourses: Set<LocalDate>,
    datesWithEvents: Map<LocalDate, List<EventType>>,
    onDateClick: (LocalDate) -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
) {
    val daysInMonth = month.lengthOfMonth()
    val firstDayOfMonth = month.atDay(1)
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7

    val calendarDays = buildList {
        val prevMonth = month.minusMonths(1)
        val prevMonthDays = prevMonth.lengthOfMonth()
        for (i in firstDayOfWeek - 1 downTo 0) {
            add(CalendarDay(
                date = prevMonth.atDay(prevMonthDays - i),
                isCurrentMonth = false
            ))
        }
        for (day in 1..daysInMonth) {
            add(CalendarDay(
                date = month.atDay(day),
                isCurrentMonth = true
            ))
        }
        val remaining = 42 - size
        val nextMonth = month.plusMonths(1)
        for (day in 1..remaining) {
            add(CalendarDay(
                date = nextMonth.atDay(day),
                isCurrentMonth = false
            ))
        }
    }

    Column(
        modifier = Modifier.pointerInput(month) {
            detectHorizontalDragGestures { change, dragAmount ->
                change.consume()
                if (dragAmount < -50) {
                    onSwipeLeft()
                } else if (dragAmount > 50) {
                    onSwipeRight()
                }
            }
        }
    ) {
        val weeks = calendarDays.chunked(7)
        weeks.forEach { weekDays ->
            Row(modifier = Modifier.fillMaxWidth()) {
                weekDays.forEach { day ->
                    CalendarDayCell(
                        day = day,
                        isToday = day.date == today,
                        hasCourses = datesWithCourses.contains(day.date),
                        eventTypes = datesWithEvents[day.date] ?: emptyList(),
                        onClick = { onDateClick(day.date) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private data class CalendarDay(
    val date: LocalDate,
    val isCurrentMonth: Boolean
)

@Composable
private fun CalendarDayCell(
    day: CalendarDay,
    isToday: Boolean,
    hasCourses: Boolean,
    eventTypes: List<EventType>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isWeekend = day.date.dayOfWeek == DayOfWeek.SATURDAY || day.date.dayOfWeek == DayOfWeek.SUNDAY
    val textColor = when {
        isToday -> MaterialTheme.colorScheme.onPrimary
        !day.isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
        isWeekend -> MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    val hasExam = eventTypes.contains(EventType.EXAM)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .clickable(enabled = day.isCurrentMonth, onClick = onClick)
            .background(
                if (isToday) MaterialTheme.colorScheme.primary
                else Color.Transparent
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
            )

            // 标记点
            if (hasCourses || eventTypes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasCourses) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    if (hasExam) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                        )
                    } else if (eventTypes.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DayDetailSheet(
    date: LocalDate,
    courses: List<com.hbde.courseschedule.data.local.entity.CourseEntity>,
    events: List<Event>,
    onDismiss: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MM月dd日 EEEE", Locale.CHINESE)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 日期标题
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = date.format(dateFormatter),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 课程列表
        if (courses.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "课程 (${courses.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            courses.forEach { course ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = course.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "第${course.startNode}-${course.endNode}节  ${course.classroom ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 事件列表
        if (events.isNotEmpty()) {
            if (courses.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "事件 (${events.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            events.forEach { event ->
                val cardColor = when (event.type) {
                    EventType.EXAM -> MaterialTheme.colorScheme.errorContainer
                    EventType.HOMEWORK -> MaterialTheme.colorScheme.tertiaryContainer
                    EventType.CUSTOM -> MaterialTheme.colorScheme.secondaryContainer
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                textDecoration = if (event.isCompleted) TextDecoration.LineThrough else null
                            )
                            Text(
                                text = "${event.startTime.toLocalTime()}  ${event.location ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val typeLabel = when (event.type) {
                            EventType.EXAM -> "考试"
                            EventType.HOMEWORK -> "作业"
                            EventType.CUSTOM -> "自定义"
                        }
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (courses.isEmpty() && events.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "当天没有课程和事件",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
