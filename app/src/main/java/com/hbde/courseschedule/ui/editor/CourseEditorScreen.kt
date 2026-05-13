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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.luminance
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorPicker(
    selectedColor: Int?,
    onColorSelected: (Int) -> Unit
) {
    var showCustomPicker by remember { mutableStateOf(false) }

    val colors = listOf(
        0xFFE57373.toInt(), 0xFFF06292.toInt(), 0xFFBA68C8.toInt(), 0xFF9575CD.toInt(),
        0xFF7986CB.toInt(), 0xFF64B5F6.toInt(), 0xFF4FC3F7.toInt(), 0xFF4DD0E1.toInt(),
        0xFF4DB6AC.toInt(), 0xFF81C784.toInt(), 0xFFAED581.toInt(), 0xFFFFB74D.toInt(),
        0xFFFF8A65.toInt(), 0xFF90A4AE.toInt(), 0xFFB0BEC5.toInt()
    )

    Column {
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

        Spacer(modifier = Modifier.height(8.dp))

        // Custom color button
        TextButton(
            onClick = { showCustomPicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("自定义颜色")
        }
    }

    if (showCustomPicker) {
        CustomColorPickerBottomSheet(
            initialColor = selectedColor ?: 0xFF2196F3.toInt(),
            onColorSelected = {
                onColorSelected(it)
                showCustomPicker = false
            },
            onDismiss = { showCustomPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomColorPickerBottomSheet(
    initialColor: Int,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var red by remember { mutableStateOf(Color(initialColor).red * 255) }
    var green by remember { mutableStateOf(Color(initialColor).green * 255) }
    var blue by remember { mutableStateOf(Color(initialColor).blue * 255) }

    val currentColor = Color(
        red = (red / 255).coerceIn(0f, 1f),
        green = (green / 255).coerceIn(0f, 1f),
        blue = (blue / 255).coerceIn(0f, 1f)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "自定义颜色",
                style = MaterialTheme.typography.titleLarge
            )

            // Color preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(currentColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = String.format("#%02X%02X%02X", red.toInt(), green.toInt(), blue.toInt()),
                    color = if (currentColor.luminance() > 0.5f) Color.Black else Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // RGB Sliders
            ColorSlider(
                label = "R",
                value = red,
                onValueChange = { red = it },
                color = Color.Red
            )
            ColorSlider(
                label = "G",
                value = green,
                onValueChange = { green = it },
                color = Color.Green
            )
            ColorSlider(
                label = "B",
                value = blue,
                onValueChange = { blue = it },
                color = Color.Blue
            )

            // HEX input
            var hexInput by remember {
                mutableStateOf(
                    String.format("%02X%02X%02X", red.toInt(), green.toInt(), blue.toInt())
                )
            }
            OutlinedTextField(
                value = hexInput,
                onValueChange = { input ->
                    hexInput = input.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }
                    if (hexInput.length == 6) {
                        try {
                            val parsed = hexInput.toInt(16)
                            red = ((parsed shr 16) and 0xFF).toFloat()
                            green = ((parsed shr 8) and 0xFF).toFloat()
                            blue = (parsed and 0xFF).toFloat()
                        } catch (_: NumberFormatException) {
                            // ignore invalid hex
                        }
                    }
                },
                label = { Text("HEX") },
                prefix = { Text("#") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("取消")
                }
                Button(
                    onClick = {
                        val colorInt = android.graphics.Color.rgb(
                            red.toInt().coerceIn(0, 255),
                            green.toInt().coerceIn(0, 255),
                            blue.toInt().coerceIn(0, 255)
                        )
                        onColorSelected(colorInt)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("确定")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ColorSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.width(20.dp),
            color = color,
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..255f,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${value.toInt()}",
            modifier = Modifier.width(36.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}
