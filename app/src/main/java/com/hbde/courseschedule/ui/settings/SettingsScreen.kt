package com.hbde.courseschedule.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.automirrored.filled.Output
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val reminderMinutes by viewModel.reminderMinutes.collectAsState()
    val ttsEnabled by viewModel.ttsEnabled.collectAsState()
    val autoSilentEnabled by viewModel.autoSilentEnabled.collectAsState()

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
                    steps = 4, // 5, 10, 15, 20, 25, 30
                    modifier = Modifier.fillMaxWidth()
                )
            }

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
