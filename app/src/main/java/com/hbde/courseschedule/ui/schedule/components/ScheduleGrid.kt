package com.hbde.courseschedule.ui.schedule.components

import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.hbde.courseschedule.data.local.entity.CourseEntity
import com.hbde.courseschedule.data.local.entity.ThemeConfigEntity
import com.hbde.courseschedule.data.local.entity.TimeSlot
import com.hbde.courseschedule.utils.CourseStatusCalculator
import java.time.LocalTime

private const val PERIODS_PER_DAY = 12
private val DAY_LABELS = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

// 默认时间段（可后续从 TimeTableEntity 读取）
private val DEFAULT_TIME_SLOTS = listOf(
    "08:00\n08:45", "08:55\n09:40", "10:00\n10:45", "10:55\n11:40",
    "14:00\n14:45", "14:55\n15:40", "16:00\n16:45", "16:55\n17:40",
    "19:00\n19:45", "19:55\n20:40", "20:50\n21:35", "21:45\n22:30"
)

private val NODE_START_TIMES = listOf(
    LocalTime.of(8, 0), LocalTime.of(8, 55), LocalTime.of(10, 0), LocalTime.of(10, 55),
    LocalTime.of(14, 0), LocalTime.of(14, 55), LocalTime.of(16, 0), LocalTime.of(16, 55),
    LocalTime.of(19, 0), LocalTime.of(19, 55), LocalTime.of(20, 50), LocalTime.of(21, 45)
)

private val NODE_END_TIMES = listOf(
    LocalTime.of(8, 45), LocalTime.of(9, 40), LocalTime.of(10, 45), LocalTime.of(11, 40),
    LocalTime.of(14, 45), LocalTime.of(15, 40), LocalTime.of(16, 45), LocalTime.of(17, 40),
    LocalTime.of(19, 45), LocalTime.of(20, 40), LocalTime.of(21, 35), LocalTime.of(22, 30)
)

@Composable
private fun parseBackgroundColor(backgroundValue: String?): Color {
    val parsed = try {
        backgroundValue?.let { Color(android.graphics.Color.parseColor(it)) }
    } catch (_: Exception) {
        null
    }
    return parsed ?: MaterialTheme.colorScheme.background
}

/**
 * 课表网格主组件
 *
 * @param courses 当前周过滤后的课程列表
 * @param currentDayOfWeek 今天星期几 (1..7)
 * @param currentTime 当前时间，用于绘制时间指示线；null 则不绘制
 * @param timeSlots 作息时间表，用于判断当前课程
 * @param themeConfig 主题配置
 * @param backgroundType 背景类型 "color" / "image"
 * @param backgroundValue 背景值（颜色 hex 或图片 URI）
 * @param backgroundOpacity 背景透明度
 * @param borderWidth 网格边框粗细 dp
 * @param onEmptyCellClick 点击空白格子回调 (dayOfWeek, startNode)
 * @param onCourseClick 点击课程块回调
 */
@Composable
fun ScheduleGrid(
    courses: List<CourseEntity>,
    currentDayOfWeek: Int,
    currentTime: LocalTime? = null,
    timeSlots: List<TimeSlot> = CourseStatusCalculator.DEFAULT_TIME_SLOTS,
    themeConfig: ThemeConfigEntity? = null,
    backgroundType: String = "color",
    backgroundValue: String? = null,
    backgroundOpacity: Float = 1.0f,
    borderWidth: Int = 0,
    onEmptyCellClick: (dayOfWeek: Int, startNode: Int) -> Unit = { _, _ -> },
    onCourseClick: (CourseEntity) -> Unit = {}
) {
    val scale = remember { mutableFloatStateOf(1f) }
    val minScale = 0.6f
    val maxScale = 2.5f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    val newScale = scale.floatValue * zoom
                    scale.floatValue = newScale.coerceIn(minScale, maxScale)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        scale.floatValue = 1f
                    }
                )
            }
    ) {
        // 背景层
        ScheduleBackground(
            backgroundType = backgroundType,
            backgroundValue = backgroundValue,
            backgroundOpacity = backgroundOpacity
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale.floatValue
                    scaleY = scale.floatValue
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
                }
        ) {
            // 星期标题行
            DayHeaderRow(
                currentDayOfWeek = currentDayOfWeek,
                themeConfig = themeConfig,
                borderWidth = borderWidth
            )

            // 课程网格
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(PERIODS_PER_DAY) { nodeIndex ->
                        val node = nodeIndex + 1
                        PeriodRow(
                            node = node,
                            courses = courses,
                            currentDayOfWeek = currentDayOfWeek,
                            timeSlots = timeSlots,
                            themeConfig = themeConfig,
                            borderWidth = borderWidth,
                            onEmptyCellClick = onEmptyCellClick,
                            onCourseClick = onCourseClick
                        )
                    }
                }

                // 当前时间指示线（仅当天显示）
                currentTime?.let { time ->
                    CurrentTimeIndicator(
                        currentTime = time,
                        currentDayOfWeek = currentDayOfWeek,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleBackground(
    backgroundType: String,
    backgroundValue: String?,
    backgroundOpacity: Float
) {
    when (backgroundType) {
        "image" -> {
            if (!backgroundValue.isNullOrBlank()) {
                AsyncImage(
                    model = Uri.parse(backgroundValue),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // 透明度遮罩
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.background.copy(
                            alpha = 1f - backgroundOpacity.coerceIn(0f, 1f)
                        )
                    )
            )
        }
        else -> {
            val bgColor = parseBackgroundColor(backgroundValue)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor.copy(alpha = backgroundOpacity.coerceIn(0f, 1f)))
            )
        }
    }
}

@Composable
private fun DayHeaderRow(
    currentDayOfWeek: Int,
    themeConfig: ThemeConfigEntity?,
    borderWidth: Int
) {
    val borderWidthDp = borderWidth.coerceIn(0, 2).dp
    val gridLineColor = themeConfig?.let {
        val primary = Color(it.primaryColor)
        if (primary.luminance() > 0.5f) Color.Black.copy(alpha = 0.15f)
        else Color.White.copy(alpha = 0.2f)
    } ?: MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // 左上角空白角标
        Box(
            modifier = Modifier
                .width(52.dp)
                .fillMaxHeight()
                .border(borderWidthDp, gridLineColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "节次",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DAY_LABELS.forEachIndexed { index, label ->
            val dayNum = index + 1
            val isToday = dayNum == currentDayOfWeek
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (isToday) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(borderWidthDp, gridLineColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PeriodRow(
    node: Int,
    courses: List<CourseEntity>,
    currentDayOfWeek: Int,
    timeSlots: List<TimeSlot>,
    themeConfig: ThemeConfigEntity?,
    borderWidth: Int,
    onEmptyCellClick: (dayOfWeek: Int, startNode: Int) -> Unit,
    onCourseClick: (CourseEntity) -> Unit
) {
    val rowHeight = 64.dp
    val nodeIndex = node - 1
    val borderWidthDp = borderWidth.coerceIn(0, 2).dp
    val gridLineColor = themeConfig?.let {
        val primary = Color(it.primaryColor)
        if (primary.luminance() > 0.5f) Color.Black.copy(alpha = 0.15f)
        else Color.White.copy(alpha = 0.2f)
    } ?: MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight)
    ) {
        // 左侧节次 + 时间
        Box(
            modifier = Modifier
                .width(52.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .border(borderWidthDp, gridLineColor),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = node.toString(),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                val timeLabel = DEFAULT_TIME_SLOTS.getOrNull(nodeIndex)?.replace("\n", "-") ?: ""
                Text(
                    text = timeLabel,
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 10.sp
                )
            }
        }

        // 7 天格子
        (1..7).forEach { day ->
            val isToday = day == currentDayOfWeek
            val dayCourses = courses.filter { it.dayOfWeek == day }

            // 找出在当前节次有显示需求的课程（startNode <= node <= endNode）
            val coursesInNode = dayCourses.filter { it.startNode <= node && it.endNode >= node }

            // 按重叠分组：key = (startNode, endNode) 相同视为同一组？
            // 实际应按时间重叠分组：找出所有与当前 node 重叠的课程，然后计算每个课程的重叠索引
            val overlappingCourses = dayCourses.filter {
                it.startNode <= node && it.endNode >= node
            }

            // 计算每个课程在此 node 的"显示优先级"：只有 startNode == node 时才渲染整块
            val coursesToRender = overlappingCourses.filter { it.startNode == node }

            // 计算重叠：找到所有与这些课程时间重叠的其他课程
            val allOverlappingAtDay = dayCourses.filter { courseA ->
                coursesToRender.any { courseB ->
                    courseA != courseB &&
                            courseA.startNode <= courseB.endNode &&
                            courseA.endNode >= courseB.startNode
                }
            } + coursesToRender

            // 为每个要渲染的课程计算重叠索引
            val overlapGroups = buildOverlapGroups(dayCourses)

            val cellModifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(
                    when {
                        isToday -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    }
                )
                .border(borderWidthDp, gridLineColor)
                .clickable(enabled = coursesInNode.isEmpty()) {
                    onEmptyCellClick(day, node)
                }

            Box(modifier = cellModifier) {
                // 渲染课程块（仅在该课程 startNode 处渲染，跨节次合并）
                coursesToRender.forEach { course ->
                    val group = overlapGroups.find { it.contains(course) } ?: listOf(course)
                    val indexInGroup = group.indexOf(course)
                    val count = group.size

                    // 计算该课程块应占的高度 = (endNode - startNode + 1) * rowHeight
                    val span = course.endNode - course.startNode + 1

                    // 判断是否是当前正在上的课程
                    val isCurrentlyActive = isToday && CourseStatusCalculator.isCurrentlyActive(course, timeSlots)

                    CourseBlockItem(
                        course = course,
                        span = span,
                        overlapIndex = indexInGroup,
                        overlapCount = count,
                        isCurrentlyActive = isCurrentlyActive,
                        themeConfig = themeConfig,
                        onClick = onCourseClick,
                        modifier = Modifier.zIndex(1f)
                    )
                }
            }
        }
    }
}

/**
 * 构建重叠组：将时间上有重叠的课程分到同一组
 */
private fun buildOverlapGroups(courses: List<CourseEntity>): List<List<CourseEntity>> {
    if (courses.isEmpty()) return emptyList()
    val sorted = courses.sortedBy { it.startNode }
    val groups = mutableListOf<MutableList<CourseEntity>>()

    sorted.forEach { course ->
        var added = false
        for (group in groups) {
            // 如果与组内任一课程重叠，则加入
            if (group.any {
                    course.startNode <= it.endNode && course.endNode >= it.startNode
                }) {
                group.add(course)
                added = true
                break
            }
        }
        if (!added) {
            groups.add(mutableListOf(course))
        }
    }
    return groups
}

/**
 * 单个课程块渲染（支持跨节次高度和重叠宽度）
 */
@Composable
private fun CourseBlockItem(
    course: CourseEntity,
    span: Int,
    overlapIndex: Int,
    overlapCount: Int,
    isCurrentlyActive: Boolean,
    themeConfig: ThemeConfigEntity?,
    onClick: (CourseEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val baseColor = course.color?.let { Color(it) } ?: MaterialTheme.colorScheme.primaryContainer
    val textColor = if (baseColor.luminance() > 0.5f) Color.Black else Color.White
    val rowHeight = 64.dp

    // 呼吸动画效果（仅当前课程）
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = InfiniteRepeatableSpec(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val cornerRadius = themeConfig?.cornerRadius?.dp ?: 4.dp
    val courseNameSize = themeConfig?.fontSize?.sp ?: 11.sp
    val classroomSize = ((themeConfig?.fontSize ?: 14) - 2).coerceAtLeast(9).sp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(rowHeight * span)
            .padding(1.dp)
            .then(
                if (overlapCount > 1) {
                    Modifier.fillMaxWidth(1f / overlapCount)
                        .offset(x = (overlapIndex * (100 / overlapCount)).dp)
                } else Modifier
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(cornerRadius))
                .then(
                    if (isCurrentlyActive) {
                        Modifier
                            .border(
                                width = 3.dp,
                                color = Color(0xFF4CAF50).copy(alpha = pulseAlpha),
                                shape = RoundedCornerShape(cornerRadius)
                            )
                            .drawWithContent {
                                drawContent()
                                // 绘制外发光效果
                                drawRect(
                                    color = Color(0xFF4CAF50).copy(alpha = (0.15f * pulseAlpha).coerceIn(0f, 1f)),
                                    size = size
                                )
                            }
                    } else Modifier
                )
                .background(baseColor)
                .clickable { onClick(course) }
                .padding(horizontal = 3.dp, vertical = 2.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = course.name,
                    color = textColor,
                    fontSize = courseNameSize,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = (courseNameSize.value + 2).sp,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!course.classroom.isNullOrBlank()) {
                    Text(
                        text = course.classroom,
                        color = textColor.copy(alpha = 0.85f),
                        fontSize = classroomSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * 当前时间指示线（红色横线）
 */
@Composable
private fun CurrentTimeIndicator(
    currentTime: LocalTime,
    currentDayOfWeek: Int,
    modifier: Modifier = Modifier
) {
    // 计算当前时间位于哪个节次区间
    val nodeIndex = NODE_START_TIMES.indexOfFirst { currentTime < it }
        .let { if (it == -1) PERIODS_PER_DAY - 1 else it - 1 }
        .coerceIn(0, PERIODS_PER_DAY - 1)

    val startTime = NODE_START_TIMES[nodeIndex]
    val endTime = NODE_END_TIMES[nodeIndex]
    val durationMinutes = java.time.Duration.between(startTime, endTime).toMinutes()
    val elapsedMinutes = java.time.Duration.between(startTime, currentTime).toMinutes()
    val fraction = elapsedMinutes.toFloat() / durationMinutes.toFloat()

    val rowHeight = 64.dp
    val headerHeight = 40.dp
    val offsetY = headerHeight + rowHeight * nodeIndex + rowHeight * fraction

    // 只在当天列上显示红线（跳过左侧节次列，只覆盖 7 天区域）
    // 这里简单处理：在整个网格上画一条横线
    Box(
        modifier = modifier
            .padding(start = 52.dp) // 跳过左侧节次列
    ) {
        // 计算当天列的横向偏移
        val dayIndex = currentDayOfWeek - 1
        // 使用 BoxWithConstraints 获取宽度再计算更精确，这里用 weight 逻辑近似
        // 由于 LazyColumn 内部无法直接获取精确宽度，我们用全宽红线 + 当天高亮的方式
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = offsetY)
                .height(2.dp)
                .background(Color.Red)
                .zIndex(2f)
        )

        // 在当天列加一个小圆点标记
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val cellWidth = maxWidth / 7f
            Box(
                modifier = Modifier
                    .offset(
                        x = cellWidth * dayIndex,
                        y = offsetY - 4.dp
                    )
                    .width(cellWidth)
                    .height(10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .height(8.dp)
                        .background(Color.Red, RoundedCornerShape(50))
                )
            }
        }
    }
}
