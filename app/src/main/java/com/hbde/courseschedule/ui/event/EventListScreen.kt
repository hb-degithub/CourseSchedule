package com.hbde.courseschedule.ui.event

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hbde.courseschedule.data.model.Event
import com.hbde.courseschedule.data.model.EventPriority
import com.hbde.courseschedule.data.model.EventType
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventListScreen(
    onNavigateToEditor: () -> Unit = {},
    viewModel: EventListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("全部", "考试", "作业", "自定义")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("日程") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToEditor,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加新事件",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = {
                            selectedTabIndex = index
                            val filter = when (index) {
                                0 -> EventFilter.All
                                1 -> EventFilter.Exam
                                2 -> EventFilter.Homework
                                else -> EventFilter.Custom
                            }
                            viewModel.selectFilter(filter)
                        },
                        text = { Text(title) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.events) { event ->
                    EventListItem(event = event)
                }
            }
        }
    }
}

@Composable
private fun EventListItem(event: Event) {
    val priorityColor = when (event.priority) {
        EventPriority.HIGH -> MaterialTheme.colorScheme.error
        EventPriority.MEDIUM -> MaterialTheme.colorScheme.tertiary
        EventPriority.LOW -> MaterialTheme.colorScheme.primary
    }

    val alpha = if (event.isCompleted) 0.5f else 1f
    val textDecoration = if (event.isCompleted) TextDecoration.LineThrough else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 优先级指示条
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 48.dp)
                    .drawBehind {
                        drawRoundRect(
                            color = priorityColor,
                            size = size,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                    }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = textDecoration,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = event.startTime.format(timeFormatter),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    event.location?.let { location ->
                        Text(
                            text = "  ·  $location",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                event.notes?.let { notes ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 类型标签
            val typeLabel = when (event.type) {
                EventType.EXAM -> "考试"
                EventType.HOMEWORK -> "作业"
                EventType.CUSTOM -> "自定义"
            }
            val typeColor = when (event.type) {
                EventType.EXAM -> MaterialTheme.colorScheme.errorContainer
                EventType.HOMEWORK -> MaterialTheme.colorScheme.tertiaryContainer
                EventType.CUSTOM -> MaterialTheme.colorScheme.secondaryContainer
            }
            val typeContentColor = when (event.type) {
                EventType.EXAM -> MaterialTheme.colorScheme.onErrorContainer
                EventType.HOMEWORK -> MaterialTheme.colorScheme.onTertiaryContainer
                EventType.CUSTOM -> MaterialTheme.colorScheme.onSecondaryContainer
            }

            Text(
                text = typeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = typeContentColor,
                modifier = Modifier
                    .drawBehind {
                        drawRoundRect(
                            color = typeColor,
                            size = size,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
