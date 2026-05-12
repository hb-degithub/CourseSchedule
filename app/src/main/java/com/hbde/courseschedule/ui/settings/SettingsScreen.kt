package com.hbde.courseschedule.ui.settings

import android.net.Uri
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
import androidx.compose.material.icons.automirrored.filled.Output
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val reminderMinutes by viewModel.reminderMinutes.collectAsState()
    val ttsEnabled by viewModel.ttsEnabled.collectAsState()
    val autoSilentEnabled by viewModel.autoSilentEnabled.collectAsState()
    val calendarSyncEnabled by viewModel.calendarSyncEnabled.collectAsState()

    // Appearance states
    val backgroundType by viewModel.backgroundType.collectAsState()
    val backgroundColor by viewModel.backgroundColor.collectAsState()
    val backgroundImageUri by viewModel.backgroundImageUri.collectAsState()
    val backgroundOpacity by viewModel.backgroundOpacity.collectAsState()
    val borderWidth by viewModel.borderWidth.collectAsState()
    val courseNameFontSize by viewModel.courseNameFontSize.collectAsState()
    val classroomFontSize by viewModel.classroomFontSize.collectAsState()
    val themeConfig by viewModel.themeConfig.collectAsState()
    val presetThemes = viewModel.presetThemes

    // Image picker launcher
    // TODO: Add READ_MEDIA_IMAGES or READ_EXTERNAL_STORAGE permission check before launching
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
                .verticalScroll(rememberScrollState())
        ) {
            // Voice broadcast switch
            SettingsSwitchItem(
                title = "语音播报",
                subtitle = "课前自动语音提醒",
                checked = ttsEnabled,
                onCheckedChange = viewModel::onTtsEnabledChange
            )

            HorizontalDivider()

            // Auto mute switch
            SettingsSwitchItem(
                title = "自动静音",
                subtitle = "上课期间自动静音",
                checked = autoSilentEnabled,
                onCheckedChange = viewModel::onAutoSilentEnabledChange
            )

            HorizontalDivider()

            // Calendar sync switch
            SettingsSwitchItem(
                title = "同步到系统日历",
                subtitle = "将课程和日程同步到系统日历",
                checked = calendarSyncEnabled,
                onCheckedChange = viewModel::onCalendarSyncEnabledChange
            )

            HorizontalDivider()

            // Reminder time slider
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
                    Text(
                        text = "课前提醒时间",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "$reminderMinutes 分钟",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Slider(
                    value = reminderMinutes.toFloat(),
                    onValueChange = {
                        val stepped = (it / 5).toInt() * 5
                        viewModel.onReminderMinutesChange(stepped.coerceIn(5, 30))
                    },
                    valueRange = 5f..30f,
                    steps = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            HorizontalDivider()

            // ===== Appearance Settings =====
            AppearanceSettingsCard(
                presetThemes = presetThemes,
                currentTheme = themeConfig,
                backgroundType = backgroundType,
                backgroundColor = backgroundColor,
                backgroundImageUri = backgroundImageUri,
                backgroundOpacity = backgroundOpacity,
                borderWidth = borderWidth,
                courseNameFontSize = courseNameFontSize,
                classroomFontSize = classroomFontSize,
                onApplyPreset = viewModel::applyPresetTheme,
                onBackgroundTypeChange = viewModel::onBackgroundTypeChange,
                onBackgroundColorChange = viewModel::onBackgroundColorChange,
                onPickImage = { imagePickerLauncher.launch("image/*") },
                onBackgroundOpacityChange = viewModel::onBackgroundOpacityChange,
                onBorderWidthChange = viewModel::onBorderWidthChange,
                onCourseNameFontSizeChange = viewModel::onCourseNameFontSizeChange,
                onClassroomFontSizeChange = viewModel::onClassroomFontSizeChange
            )

            HorizontalDivider()

            // Import schedule
            SettingsActionItem(
                title = "导入课表",
                subtitle = "从文件或教务系统导入",
                icon = Icons.AutoMirrored.Filled.Input,
                onClick = viewModel::importSchedule
            )

            HorizontalDivider()

            // Export schedule
            SettingsActionItem(
                title = "导出课表",
                subtitle = "导出为文件",
                icon = Icons.AutoMirrored.Filled.Output,
                onClick = viewModel::exportSchedule
            )

            HorizontalDivider()

            // About
            SettingsActionItem(
                title = "关于",
                subtitle = "课程表 v1.0",
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                onClick = { /* TODO: Show about dialog */ }
            )
        }
    }
}

@Composable
private fun AppearanceSettingsCard(
    presetThemes: List<ThemeConfigEntity>,
    currentTheme: ThemeConfigEntity,
    backgroundType: String,
    backgroundColor: String,
    backgroundImageUri: String?,
    backgroundOpacity: Float,
    borderWidth: Int,
    courseNameFontSize: Int,
    classroomFontSize: Int,
    onApplyPreset: (ThemeConfigEntity) -> Unit,
    onBackgroundTypeChange: (String) -> Unit,
    onBackgroundColorChange: (String) -> Unit,
    onPickImage: () -> Unit,
    onBackgroundOpacityChange: (Float) -> Unit,
    onBorderWidthChange: (Int) -> Unit,
    onCourseNameFontSizeChange: (Int) -> Unit,
    onClassroomFontSizeChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "外观设置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider()

            // Theme Presets
            Text(
                text = "主题预设",
                style = MaterialTheme.typography.titleSmall
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(presetThemes) { preset ->
                    PresetThemeCard(
                        preset = preset,
                        isSelected = preset.primaryColor == currentTheme.primaryColor
                                && preset.backgroundImage == currentTheme.backgroundImage,
                        onClick = { onApplyPreset(preset) }
                    )
                }
            }

            HorizontalDivider()

            // Course Color Management
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* TODO: Navigate to color management page or show BottomSheet */ }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ColorLens,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column {
                        Text(
                            text = "课程颜色管理",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "管理课程预设颜色",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "去设置",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            HorizontalDivider()

            // Background Settings
            Text(
                text = "背景设置",
                style = MaterialTheme.typography.titleSmall
            )

            // Background type selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BackgroundTypeButton(
                    text = "纯色",
                    icon = Icons.Default.ColorLens,
                    isSelected = backgroundType == "color",
                    onClick = { onBackgroundTypeChange("color") },
                    modifier = Modifier.weight(1f)
                )
                BackgroundTypeButton(
                    text = "图片",
                    icon = Icons.Default.Image,
                    isSelected = backgroundType == "image",
                    onClick = { onBackgroundTypeChange("image") },
                    modifier = Modifier.weight(1f)
                )
            }

            when (backgroundType) {
                "color" -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    SimpleColorPicker(
                        selectedColor = backgroundColor,
                        onColorSelected = onBackgroundColorChange
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
                        // TODO: Add READ_MEDIA_IMAGES or READ_EXTERNAL_STORAGE permission check
                        androidx.compose.material3.TextButton(onClick = onPickImage) {
                            Text(if (backgroundImageUri != null) "更换" else "选择图片")
                        }
                    }
                }
            }

            // Opacity slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("背景透明度", style = MaterialTheme.typography.bodyMedium)
                    Text("${(backgroundOpacity * 100).toInt()}%", color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = backgroundOpacity,
                    onValueChange = onBackgroundOpacityChange,
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            HorizontalDivider()

            // Grid Style
            Text(
                text = "格子样式",
                style = MaterialTheme.typography.titleSmall
            )

            // Border width slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("边框粗细", style = MaterialTheme.typography.bodyMedium)
                    Text("${borderWidth}dp", color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = borderWidth.toFloat(),
                    onValueChange = { onBorderWidthChange(it.toInt()) },
                    valueRange = 0f..2f,
                    steps = 1,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            HorizontalDivider()

            // Font Size
            Text(
                text = "字体大小",
                style = MaterialTheme.typography.titleSmall
            )

            // Course name font size
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("课程名字体", style = MaterialTheme.typography.bodyMedium)
                    Text("${courseNameFontSize}sp", color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = courseNameFontSize.toFloat(),
                    onValueChange = { onCourseNameFontSizeChange(it.toInt()) },
                    valueRange = 12f..20f,
                    steps = 7,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Classroom font size
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("教室/教师字体", style = MaterialTheme.typography.bodyMedium)
                    Text("${classroomFontSize}sp", color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = classroomFontSize.toFloat(),
                    onValueChange = { onClassroomFontSizeChange(it.toInt()) },
                    valueRange = 10f..16f,
                    steps = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
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
private fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
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
        modifier = Modifier.clickable(onClick = onClick)
    )
}
