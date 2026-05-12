package com.hbde.courseschedule.ui.event.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

data class DayEventDot(
    val color: androidx.compose.ui.graphics.Color
)

@Composable
fun MonthCalendarView(
    modifier: Modifier = Modifier,
    eventsByDate: Map<LocalDate, List<DayEventDot>> = emptyMap(),
    onDateClick: (LocalDate) -> Unit = {},
    onMonthChange: (YearMonth) -> Unit = {}
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val today = remember { LocalDate.now() }

    Column(modifier = modifier.fillMaxWidth()) {
        // 年月标题 + 切换按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    currentMonth = currentMonth.minusMonths(1)
                    onMonthChange(currentMonth)
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "上个月",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "${currentMonth.year}年${currentMonth.monthValue}月",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            IconButton(
                onClick = {
                    currentMonth = currentMonth.plusMonths(1)
                    onMonthChange(currentMonth)
                }
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

        // 日历网格
        val daysInMonth = currentMonth.lengthOfMonth()
        val firstDayOfMonth = currentMonth.atDay(1)
        val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // 周一=1, 周日=0

        val calendarDays = buildList {
            // 上月填充
            val prevMonth = currentMonth.minusMonths(1)
            val prevMonthDays = prevMonth.lengthOfMonth()
            for (i in firstDayOfWeek - 1 downTo 0) {
                add(CalendarDay(
                    date = prevMonth.atDay(prevMonthDays - i),
                    isCurrentMonth = false
                ))
            }
            // 当月
            for (day in 1..daysInMonth) {
                add(CalendarDay(
                    date = currentMonth.atDay(day),
                    isCurrentMonth = true
                ))
            }
            // 下月填充（补齐 6 行 x 7 列 = 42 格）
            val remaining = 42 - size
            val nextMonth = currentMonth.plusMonths(1)
            for (day in 1..remaining) {
                add(CalendarDay(
                    date = nextMonth.atDay(day),
                    isCurrentMonth = false
                ))
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth(),
            userScrollEnabled = false
        ) {
            items(calendarDays) { day ->
                CalendarDayCell(
                    day = day,
                    isToday = day.date == today,
                    eventDots = eventsByDate[day.date] ?: emptyList(),
                    onClick = { onDateClick(day.date) }
                )
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
    eventDots: List<DayEventDot>,
    onClick: () -> Unit
) {
    val isWeekend = day.date.dayOfWeek == DayOfWeek.SATURDAY || day.date.dayOfWeek == DayOfWeek.SUNDAY
    val textColor = when {
        isToday -> MaterialTheme.colorScheme.onPrimary
        !day.isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
        isWeekend -> MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .clickable(enabled = day.isCurrentMonth, onClick = onClick)
            .background(
                if (isToday) MaterialTheme.colorScheme.primary
                else androidx.compose.ui.graphics.Color.Transparent
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

            if (eventDots.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    eventDots.take(3).forEach { dot ->
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(dot.color)
                        )
                    }
                }
            }
        }
    }
}
