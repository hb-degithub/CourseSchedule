package com.hbde.courseschedule.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val iconBackgroundColor: Color,
    val iconTint: Color
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit = {},
    onSkip: () -> Unit = {}
) {
    val pages = listOf(
        OnboardingPage(
            title = "欢迎使用课程表",
            description = "智能管理你的课程与日程，让学习生活更有条理",
            icon = Icons.Filled.School,
            iconBackgroundColor = Color(0xFFE3F2FD),
            iconTint = Color(0xFF1565C0)
        ),
        OnboardingPage(
            title = "轻松导入课表",
            description = "支持教务系统、文件、图片、分享码等多种导入方式",
            icon = Icons.Filled.CloudUpload,
            iconBackgroundColor = Color(0xFFE8F5E9),
            iconTint = Color(0xFF2E7D32)
        ),
        OnboardingPage(
            title = "智能提醒",
            description = "语音播报、自动静音、课前提醒，贴心守护每一节课",
            icon = Icons.Filled.NotificationsActive,
            iconBackgroundColor = Color(0xFFFFF3E0),
            iconTint = Color(0xFFEF6C00)
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.size - 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Skip button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, end = 16.dp)
            ) {
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text("跳过")
                }
            }

            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPageContent(page = pages[page])
            }

            // Bottom controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pages.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .size(if (index == pagerState.currentPage) 24.dp else 8.dp, 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == pagerState.currentPage)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action button
                if (isLastPage) {
                    Button(
                        onClick = onFinish,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("开始使用", fontSize = 16.sp)
                    }
                } else {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("下一步", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(page.iconBackgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = page.iconTint
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Title
        Text(
            text = page.title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        Text(
            text = page.description,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        // Feature icons for page 2 and 3
        if (page.title.contains("导入")) {
            Spacer(modifier = Modifier.height(32.dp))
            ImportMethodIcons()
        } else if (page.title.contains("提醒")) {
            Spacer(modifier = Modifier.height(32.dp))
            SmartFeatureIcons()
        }
    }
}

@Composable
private fun ImportMethodIcons() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        FeatureIcon(Icons.Filled.School, "教务系统", Color(0xFFE3F2FD), Color(0xFF1565C0))
        FeatureIcon(Icons.Filled.CloudUpload, "文件导入", Color(0xFFE8F5E9), Color(0xFF2E7D32))
        FeatureIcon(Icons.Filled.CameraAlt, "图片导入", Color(0xFFFFF3E0), Color(0xFFEF6C00))
        FeatureIcon(Icons.Filled.Share, "分享码", Color(0xFFF3E5F5), Color(0xFF7B1FA2))
    }
}

@Composable
private fun SmartFeatureIcons() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        FeatureIcon(Icons.Filled.NotificationsActive, "语音提醒", Color(0xFFFFF3E0), Color(0xFFEF6C00))
        FeatureIcon(Icons.Filled.VolumeOff, "自动静音", Color(0xFFE8F5E9), Color(0xFF2E7D32))
        FeatureIcon(Icons.Filled.CalendarMonth, "课前提醒", Color(0xFFE3F2FD), Color(0xFF1565C0))
    }
}

@Composable
private fun FeatureIcon(
    icon: ImageVector,
    label: String,
    backgroundColor: Color,
    iconTint: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp),
                tint = iconTint
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
