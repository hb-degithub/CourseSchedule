package com.hbde.courseschedule.ui.importer

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportMethodScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToWebViewImporter: () -> Unit = {},
    onNavigateToFilePicker: () -> Unit = {},
    onNavigateToImagePicker: () -> Unit = {},
    onNavigateToShareCode: () -> Unit = {},
    onNavigateToManualAdd: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("导入课表") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "选择导入方式",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            ImportMethodCard(
                title = "从教务系统导入",
                subtitle = "登录学校教务系统，自动解析课表",
                icon = Icons.Filled.School,
                backgroundColor = Color(0xFFE3F2FD),
                iconBackgroundColor = Color(0xFF1565C0),
                onClick = onNavigateToWebViewImporter
            )

            ImportMethodCard(
                title = "从文件导入",
                subtitle = "支持 Excel、CSV、ICS 格式文件",
                icon = Icons.Filled.CloudUpload,
                backgroundColor = Color(0xFFE8F5E9),
                iconBackgroundColor = Color(0xFF2E7D32),
                onClick = onNavigateToFilePicker
            )

            ImportMethodCard(
                title = "从图片导入",
                subtitle = "选择课表截图，自动识别课程信息",
                icon = Icons.Filled.CameraAlt,
                backgroundColor = Color(0xFFFFF3E0),
                iconBackgroundColor = Color(0xFFEF6C00),
                onClick = onNavigateToImagePicker
            )

            ImportMethodCard(
                title = "从分享码导入",
                subtitle = "输入他人分享的课表码",
                icon = Icons.Filled.QrCode,
                backgroundColor = Color(0xFFF3E5F5),
                iconBackgroundColor = Color(0xFF7B1FA2),
                onClick = onNavigateToShareCode
            )

            ImportMethodCard(
                title = "手动添加",
                subtitle = "逐条添加课程信息",
                icon = Icons.Filled.Edit,
                backgroundColor = Color(0xFFF5F5F5),
                iconBackgroundColor = Color(0xFF616161),
                onClick = onNavigateToManualAdd
            )
        }
    }
}

@Composable
private fun ImportMethodCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    backgroundColor: Color,
    iconBackgroundColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBackgroundColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = iconBackgroundColor
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
