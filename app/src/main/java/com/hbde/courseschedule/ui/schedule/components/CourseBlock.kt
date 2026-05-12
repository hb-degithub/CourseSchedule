package com.hbde.courseschedule.ui.schedule.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hbde.courseschedule.data.local.entity.CourseEntity
import com.hbde.courseschedule.data.local.entity.ThemeConfigEntity

/**
 * 课程块组件
 * 根据 CourseEntity 渲染，支持自动文字颜色对比
 *
 * @param course 课程实体
 * @param overlapIndex 在重叠组中的索引
 * @param overlapCount 重叠组大小
 * @param themeConfig 主题配置，用于圆角和字体大小
 * @param onClick 点击回调
 */
@Composable
fun CourseBlock(
    course: CourseEntity,
    overlapIndex: Int = 0,
    overlapCount: Int = 1,
    themeConfig: ThemeConfigEntity? = null,
    onClick: (CourseEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val baseColor = course.color?.let { Color(it) } ?: MaterialTheme.colorScheme.primaryContainer
    val textColor = baseColor.autoTextColor()

    val cornerRadius = themeConfig?.cornerRadius?.dp ?: 4.dp
    val courseNameSize = themeConfig?.fontSize?.sp ?: 11.sp
    val classroomSize = ((themeConfig?.fontSize ?: 14) - 2).coerceAtLeast(9).sp

    val fraction = 1f / overlapCount
    val startFraction = overlapIndex * fraction

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(1.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .clickable { onClick(course) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = if (overlapCount > 1) {
                        (startFraction * 100).dp
                    } else 0.dp,
                    end = if (overlapCount > 1) {
                        ((1f - startFraction - fraction) * 100).dp
                    } else 0.dp
                )
                .clip(RoundedCornerShape(cornerRadius))
        ) {
            // 背景色填充
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(cornerRadius))
                    .then(
                        if (overlapCount > 1) {
                            Modifier.fillMaxWidth(fraction)
                        } else Modifier
                    )
            ) {
                androidx.compose.foundation.background(baseColor)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 2.dp, vertical = 2.dp)
                ) {
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
}

/**
 * 计算文字颜色：深色背景用白字，浅色背景用黑字
 */
fun Color.autoTextColor(): Color {
    return if (this.luminance() > 0.5f) Color.Black else Color.White
}
