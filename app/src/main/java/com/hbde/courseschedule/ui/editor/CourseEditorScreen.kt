package com.hbde.courseschedule.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseEditorScreen(
    courseId: Int? = null,
    initialDayOfWeek: Int? = null,
    initialStartNode: Int? = null,
    viewModel: CourseEditorViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    // ViewModel 通过 SavedStateHandle 读取导航参数，无需额外处理。
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.saveResult) {
        when (val result = uiState.saveResult) {
            is SaveResult.Success -> onNavigateBack()
            is SaveResult.Error -> {
                snackbarHostState.showSnackbar(result.message)
                viewModel.clearSaveResult()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.saveResult == SaveResult.Idle) "编辑课程" else "编辑课程") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Course name
            OutlinedTextField(
                value = uiState.courseName,
                onValueChange = viewModel::onCourseNameChange,
                label = { Text("课程名") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.nameError != null,
                supportingText = {
                    if (uiState.nameError != null) {
                        Text(uiState.nameError!!, color = MaterialTheme.colorScheme.error)
                    }
                }
            )

            // Classroom
            OutlinedTextField(
                value = uiState.classroom,
                onValueChange = viewModel::onClassroomChange,
                label = { Text("教室") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Teacher
            OutlinedTextField(
                value = uiState.teacher,
                onValueChange = viewModel::onTeacherChange,
                label = { Text("教师") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Day of week
            DropdownSelector(
                label = "星期",
                options = (1..7).map { "周${listOf("一", "二", "三", "四", "五", "六", "日")[it - 1]}" },
                selectedIndex = uiState.dayOfWeek - 1,
                onSelect = { viewModel.onDayOfWeekChange(it + 1) }
            )

            // Node range
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DropdownSelector(
                    label = "起始节次",
                    options = (1..12).map { "第${it}节" },
                    selectedIndex = uiState.startNode - 1,
                    onSelect = { viewModel.onStartNodeChange(it + 1) },
                    modifier = Modifier.weight(1f),
                    isError = uiState.nodeError != null
                )
                DropdownSelector(
                    label = "结束节次",
                    options = (1..12).map { "第${it}节" },
                    selectedIndex = uiState.endNode - 1,
                    onSelect = { viewModel.onEndNodeChange(it + 1) },
                    modifier = Modifier.weight(1f),
                    isError = uiState.nodeError != null
                )
            }
            if (uiState.nodeError != null) {
                Text(
                    text = uiState.nodeError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // Week range
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.startWeek.toString(),
                    onValueChange = {
                        it.toIntOrNull()?.let { w ->
                            if (w in 1..25) viewModel.onStartWeekChange(w)
                        }
                    },
                    label = { Text("起始周") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = uiState.weekError != null
                )
                OutlinedTextField(
                    value = uiState.endWeek.toString(),
                    onValueChange = {
                        it.toIntOrNull()?.let { w ->
                            if (w in 1..25) viewModel.onEndWeekChange(w)
                        }
                    },
                    label = { Text("结束周") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = uiState.weekError != null
                )
            }
            if (uiState.weekError != null) {
                Text(
                    text = uiState.weekError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // Week type
            Text(
                text = "周次类型",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 4.dp)
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                WeekType.entries.forEachIndexed { index, type ->
                    val label = when (type) {
                        WeekType.ALL -> "全部"
                        WeekType.ODD -> "单周"
                        WeekType.EVEN -> "双周"
                    }
                    SegmentedButton(
                        selected = uiState.weekType == type,
                        onClick = { viewModel.onWeekTypeChange(type) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = WeekType.entries.size
                        )
                    ) {
                        Text(label)
                    }
                }
            }

            // Color picker
            Text(
                text = "课程颜色",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 4.dp)
            )
            ColorPicker(
                selectedColor = uiState.color,
                onColorSelected = viewModel::onColorChange
            )

            // Notes
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("备注") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("取消")
                }
                Button(
                    onClick = viewModel::save,
                    enabled = !uiState.isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (uiState.isSaving) "保存中..." else "保存")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSelector(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = options.getOrElse(selectedIndex) { "" },
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            isError = isError
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ColorPicker(
    selectedColor: Int?,
    onColorSelected: (Int) -> Unit
) {
    val colors = listOf(
        0xFFE57373.toInt(), 0xFFF06292.toInt(), 0xFFBA68C8.toInt(), 0xFF9575CD.toInt(),
        0xFF7986CB.toInt(), 0xFF64B5F6.toInt(), 0xFF4FC3F7.toInt(), 0xFF4DD0E1.toInt(),
        0xFF4DB6AC.toInt(), 0xFF81C784.toInt(), 0xFFAED581.toInt(), 0xFFFFB74D.toInt(),
        0xFFFF8A65.toInt(), 0xFF90A4AE.toInt(), 0xFFB0BEC5.toInt()
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        colors.forEach { colorInt ->
            val isSelected = selectedColor == colorInt
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(colorInt))
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(colorInt) }
            )
        }
    }
}
