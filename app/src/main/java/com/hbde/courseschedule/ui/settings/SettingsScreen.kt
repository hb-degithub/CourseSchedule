package com.hbde.courseschedule.ui.settings

import android.net.Uri
import android.app.DatePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hbde.courseschedule.data.local.entity.ThemeConfigEntity
import com.hbde.courseschedule.utils.TermWeekCalculator
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val reminderMinutes by viewModel.reminderMinutes.collectAsState()
    val ttsEnabled by viewModel.ttsEnabled.collectAsState()
    val autoSilentEnabled by viewModel.autoSilentEnabled.collectAsState()
    val calendarSyncEnabled by viewModel.calendarSyncEnabled.collectAsState()
    val termStartDate by viewModel.termStartDate.collectAsState()

    val backgroundType by viewModel.backgroundType.collectAsState()
    val backgroundColor by viewModel.backgroundColor.collectAsState()
    val backgroundImageUri by viewModel.backgroundImageUri.collectAsState()
    val backgroundOpacity by viewModel.backgroundOpacity.collectAsState()
    val borderWidth by viewModel.borderWidth.collectAsState()
    val courseNameFontSize by viewModel.courseNameFontSize.collectAsState()
    val classroomFontSize by viewModel.classroomFontSize.collectAsState()
    val themeConfig by viewModel.themeConfig.collectAsState()
    val presetThemes = viewModel.presetThemes

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onBackgroundImageUriChange(it.toString()) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ===== 提醒设置 =====
            SettingsSection(title = "提醒设置") {
                SettingsSwitchItem(
                    title = "语音播报",
                    subtitle = "课前自动语音提醒",
                    icon = Icons.Filled.Notifications,
                    checked = ttsEnabled,
                    onCheckedChange = viewModel::onTtsEnabledChange
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                SettingsSwitchItem(
                    title = "自动静音",
                    subtitle = "上课期间自动静音",
                    icon = Icons.Filled.VolumeOff,
                    checked = autoSilentEnabled,
                    onCheckedChange = viewModel::onAutoSilentEnabledChange
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                SettingsSwitchItem(
                    title = "同步到系统日历",
                    subtitle = "将课程和日程同步到系统日历",
                    icon = Icons.Filled.CalendarMonth,
                    checked = calendarSyncEnabled,
                    onCheckedChange = viewModel::onCalendarSyncEnabledChange
                )
            }

            // ===== 学期设置 =====
            SettingsSection(title = "学期设置") {
                TermStartDateItem(
                    termStartDate = termStartDate,
                    onDateSelected = viewModel::onTermStartDateChange
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                SettingsSliderItem(
                    title = "课前提醒时间",
                    value = reminderMinutes.toFloat(),
                    valueRange = 5f..30f,
                    steps = 4,
                    valueLabel = "$reminderMinutes 分钟",
                    onValueChange = {
                        val stepped = (it / 5).toInt() * 5
                        viewModel.onReminderMinutesChange(stepped.coerceIn(5, 30))
                    }
                )
            }

            // ===== 外观设置 =====
            SettingsSection(title = "外观设置") {
                // 主题预设
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "主题预设",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(presetThemes) { preset ->
                            PresetThemeCard(
                                preset = preset,
                                isSelected = preset.primaryColor == themeConfig.primaryColor
                                        && preset.backgroundImage == themeConfig.backgroundImage,
                                onClick = { viewModel.applyPresetTheme(preset) }
                            )
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

                // 背景设置
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "背景设置",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BackgroundTypeButton(
                            text = "纯色",
                            icon = Icons.Default.ColorLens,
                            isSelected = backgroundType == "color",
                            onClick = { viewModel.onBackgroundTypeChange("color") },
                            modifier = Modifier.weight(1f)
                        )
                        BackgroundTypeButton(
                            text = "图片",
                            icon = Icons.Default.Image,
                            isSelected = backgroundType == "image",
                            onClick = { viewModel.onBackgroundTypeChange("image") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    when (backgroundType) {
                        "color" -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            SimpleColorPicker(
                                selectedColor = backgroundColor,
                                onColorSelected = { viewModel.onBackgroundColorChange(it) }
                            )
                        }
                        "image" -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (backgroundImageUri != null) "已选择图片" else "未选择图片",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                androidx.compose.material3.TextButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                                    Text(if (backgroundImageUri != null) "更换" else "选择图片")
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("背景透明度", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${(backgroundOpacity * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = backgroundOpacity,
                        onValueChange = { viewModel.onBackgroundOpacityChange(it) },
                        valueRange = 0f..1f
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

                // 格子样式
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "格子样式",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("边框粗细", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${borderWidth}dp",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = borderWidth.toFloat(),
                        onValueChange = { viewModel.onBorderWidthChange(it.toInt()) },
                        valueRange = 0f..2f,
                        steps = 1
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

                // 字体大小
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "字体大小",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("课程名字体", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${courseNameFontSize}sp",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = courseNameFontSize.toFloat(),
                        onValueChange = { viewModel.onCourseNameFontSizeChange(it.toInt()) },
                        valueRange = 12f..20f,
                        steps = 7
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("教室/教师字体", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${classroomFontSize}sp",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = classroomFontSize.toFloat(),
                        onValueChange = { viewModel.onClassroomFontSizeChange(it.toInt()) },
                        valueRange = 10f..16f,
                        steps = 5
                    )
                }
            }

            // ===== 数据管理 =====
            SettingsSection(title = "数据管理") {
                SettingsActionItem(
                    title = "导入课表",
                    subtitle = "从文件或教务系统导入",
                    icon = Icons.AutoMirrored.Filled.Input,
                    onClick = viewModel::importSchedule
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                SettingsActionItem(
                    title = "导出课表",
                    subtitle = "导出为文件",
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    onClick = viewModel::exportSchedule
                )
            }

            // ===== 关于 =====
            SettingsSection(title = "关于") {
                SettingsActionItem(
                    title = "关于",
                    subtitle = "课程表 v1.0",
                    icon = Icons.AutoMirrored.Filled.HelpOutline,
                    onClick = { /* TODO: Show about dialog */ }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
            )
            content()
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
        },
        supportingContent = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        },
        modifier = Modifier.clickable { onCheckedChange(!checked) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun SettingsSliderItem(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: String,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun TermStartDateItem(
    termStartDate: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val initialDate = termStartDate ?: LocalDate.now()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy年MM月dd日") }
    val currentWeek = TermWeekCalculator.calculateCurrentWeek(termStartDate)

    ListItem(
        headlineContent = {
            Text(text = "开学日期", style = MaterialTheme.typography.bodyLarge)
        },
        supportingContent = {
            Text(
                text = if (termStartDate == null) {
                    "未设置，当前按第 1 周显示"
                } else {
                    "${termStartDate.format(dateFormatter)}，当前第 ${currentWeek} 周"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Filled.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier.clickable {
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
                },
                initialDate.year,
                initialDate.monthValue - 1,
                initialDate.dayOfMonth,
            ).show()
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun PresetThemeCard(
    preset: ThemeConfigEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val primaryColor = Color(preset.primaryColor)
    val bgColor = try {
        Color(android.graphics.Color.parseColor(preset.backgroundImage))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.surface
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(preset.cornerRadius.dp))
                .background(bgColor)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) primaryColor else Color.Transparent,
                    shape = RoundedCornerShape(preset.cornerRadius.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(primaryColor)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        val name = when (preset.primaryColor) {
            0xFF2196F3.toInt() -> "默认"
            0xFF1A237E.toInt() -> "学术"
            0xFF43A047.toInt() -> "清新"
            0xFFFF6F00.toInt() -> "暖橙"
            else -> "预设"
        }
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun BackgroundTypeButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SimpleColorPicker(
    selectedColor: String,
    onColorSelected: (String) -> Unit
) {
    val colors = listOf(
        "#FFF5F5F5", "#FFFFFFFF", "#FFF1F8E9", "#FFFFF3E0",
        "#FFE3F2FD", "#FFF3E5F5", "#FFFFEBEE", "#FFE0F2F1",
        "#FF263238", "#FF0D1B2A", "#FF1A237E", "#FF004D40"
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(colors) { colorStr ->
            val color = try {
                Color(android.graphics.Color.parseColor(colorStr))
            } catch (_: Exception) {
                Color.LightGray
            }
            val isSelected = selectedColor == colorStr
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else Color.Gray.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onColorSelected(colorStr) }
            )
        }
    }
}

@Composable
private fun SettingsActionItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
        },
        supportingContent = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
