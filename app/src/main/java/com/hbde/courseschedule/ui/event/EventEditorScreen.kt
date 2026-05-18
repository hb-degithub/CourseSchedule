package com.hbde.courseschedule.ui.event

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hbde.courseschedule.data.model.EventPriority
import com.hbde.courseschedule.data.model.EventType
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditorScreen(
    eventId: Int? = null,
    onNavigateBack: () -> Unit,
    viewModel: EventEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(eventId) {
        eventId?.let { viewModel.loadEvent(it) }
    }

    val title = if (eventId != null) "编辑日程" else "新建日程"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 类型选择
            TypeSelector(
                selectedType = uiState.selectedType,
                onTypeSelected = viewModel::updateType
            )

            // 标题
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::updateTitle,
                label = { Text("标题 *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // 地点
            OutlinedTextField(
                value = uiState.location,
                onValueChange = viewModel::updateLocation,
                label = { Text("地点") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // 开始时间
            DateTimePickerRow(
                label = "开始时间",
                date = uiState.startDate,
                time = uiState.startTime,
                onDateSelected = viewModel::updateStartDate,
                onTimeSelected = viewModel::updateStartTime
            )

            // 结束时间
            DateTimePickerRow(
                label = "结束时间",
                date = uiState.endDate,
                time = uiState.endTime,
                onDateSelected = viewModel::updateEndDate,
                onTimeSelected = viewModel::updateEndTime
            )

            // 优先级
            PrioritySelector(
                selectedPriority = uiState.priority,
                onPrioritySelected = viewModel::updatePriority
            )

            // 提前提醒
            ReminderSlider(
                minutes = uiState.reminderMinutes,
                onMinutesChanged = viewModel::updateReminderMinutes
            )

            // 备注
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::updateNotes,
                label = { Text("备注") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("取消")
                }
                Button(
                    onClick = {
                        viewModel.saveEvent()
                        onNavigateBack()
                    },
                    enabled = uiState.canSave && !uiState.isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("保存")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeSelector(
    selectedType: EventType,
    onTypeSelected: (EventType) -> Unit
) {
    val options = listOf(
        EventType.EXAM to "考试",
        EventType.HOMEWORK to "作业",
        EventType.CUSTOM to "自定义"
    )
    val selectedIndex = options.indexOfFirst { it.first == selectedType }.coerceAtLeast(0)

    Column {
        Text(
            text = "类型",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (type, label) ->
                SegmentedButton(
                    selected = index == selectedIndex,
                    onClick = { onTypeSelected(type) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = options.size
                    )
                ) {
                    Text(label)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimePickerRow(
    label: String,
    date: java.time.LocalDate,
    time: java.time.LocalTime,
    onDateSelected: (java.time.LocalDate) -> Unit,
    onTimeSelected: (java.time.LocalTime) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Text(date.format(dateFormatter))
            }
            OutlinedButton(
                onClick = { showTimePicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Text(time.format(timeFormatter))
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            onDateSelected(selectedDate)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = true
        )
        DatePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedTime = java.time.LocalTime.of(
                            timePickerState.hour,
                            timePickerState.minute
                        )
                        onTimeSelected(selectedTime)
                        showTimePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TimePicker(state = timePickerState)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrioritySelector(
    selectedPriority: EventPriority,
    onPrioritySelected: (EventPriority) -> Unit
) {
    val priorities = listOf(
        EventPriority.LOW to "低",
        EventPriority.MEDIUM to "中",
        EventPriority.HIGH to "高"
    )

    Column {
        Text(
            text = "优先级",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            priorities.forEach { (priority, label) ->
                FilterChip(
                    selected = priority == selectedPriority,
                    onClick = { onPrioritySelected(priority) },
                    label = { Text(label) }
                )
            }
        }
    }
}

@Composable
private fun ReminderSlider(
    minutes: Int,
    onMinutesChanged: (Int) -> Unit
) {
    val reminderLabels = mapOf(
        0 to "不提醒",
        5 to "5分钟",
        15 to "15分钟",
        30 to "30分钟",
        60 to "1小时",
        120 to "2小时",
        1440 to "1天"
    )
    val steps = listOf(0, 5, 15, 30, 60, 120, 1440)
    val currentIndex = steps.indexOf(minutes).coerceAtLeast(0)

    Column {
        Text(
            text = "提前提醒",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = reminderLabels[minutes] ?: "${minutes}分钟",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Slider(
            value = currentIndex.toFloat(),
            onValueChange = { index ->
                onMinutesChanged(steps.getOrNull(index.toInt()) ?: 15)
            },
            valueRange = 0f..(steps.size - 1).toFloat(),
            steps = steps.size - 2
        )
    }
}
