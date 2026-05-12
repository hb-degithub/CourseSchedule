package com.hbde.courseschedule.ui.campus.grade

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hbde.courseschedule.data.model.GpaAlgorithm
import com.hbde.courseschedule.data.model.GpaCalculator
import com.hbde.courseschedule.data.model.GpaResult
import com.hbde.courseschedule.data.model.Grade

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeScreen(
    viewModel: GradeViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("成绩查询") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
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
        ) {
            // 筛选栏
            FilterBar(
                selectedSemester = uiState.selectedSemester,
                availableSemesters = uiState.availableSemesters,
                selectedAlgorithm = uiState.selectedAlgorithm,
                showSemesterMenu = uiState.showSemesterMenu,
                showAlgorithmMenu = uiState.showAlgorithmMenu,
                onSemesterClick = { viewModel.toggleSemesterMenu() },
                onAlgorithmClick = { viewModel.toggleAlgorithmMenu() },
                onSemesterSelected = { viewModel.selectSemester(it) },
                onAlgorithmSelected = { viewModel.selectAlgorithm(it) },
                onDismiss = { viewModel.dismissMenus() }
            )

            if (uiState.grades.isEmpty()) {
                EmptyGradeState(onImportClick = { /* TODO: 导入成绩 */ })
            } else {
                // 统计卡片
                GpaStatsCard(
                    gpaResult = uiState.gpaResult,
                    algorithmName = GpaCalculator.getAlgorithmName(uiState.selectedAlgorithm),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // 成绩列表
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.grades, key = { it.id }) { grade ->
                        GradeListItem(
                            grade = grade,
                            algorithm = uiState.selectedAlgorithm,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterBar(
    selectedSemester: String,
    availableSemesters: List<String>,
    selectedAlgorithm: GpaAlgorithm,
    showSemesterMenu: Boolean,
    showAlgorithmMenu: Boolean,
    onSemesterClick: () -> Unit,
    onAlgorithmClick: () -> Unit,
    onSemesterSelected: (String) -> Unit,
    onAlgorithmSelected: (GpaAlgorithm) -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 学期选择
        Box(modifier = Modifier.weight(1f)) {
            FilterChip(
                text = selectedSemester,
                onClick = onSemesterClick
            )
            DropdownMenu(
                expanded = showSemesterMenu,
                onDismissRequest = onDismiss
            ) {
                availableSemesters.forEach { semester ->
                    DropdownMenuItem(
                        text = { Text(semester) },
                        onClick = { onSemesterSelected(semester) }
                    )
                }
            }
        }

        // 算法选择
        Box(modifier = Modifier.weight(1f)) {
            FilterChip(
                text = GpaCalculator.getAlgorithmName(selectedAlgorithm),
                onClick = onAlgorithmClick
            )
            DropdownMenu(
                expanded = showAlgorithmMenu,
                onDismissRequest = onDismiss
            ) {
                GpaAlgorithm.entries.forEach { algorithm ->
                    DropdownMenuItem(
                        text = { Text(GpaCalculator.getAlgorithmName(algorithm)) },
                        onClick = { onAlgorithmSelected(algorithm) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GpaStatsCard(
    gpaResult: GpaResult,
    algorithmName: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "GPA 统计 ($algorithmName)",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem(
                    value = String.format("%.2f", gpaResult.gpa),
                    label = "平均绩点",
                    isHighlight = true
                )
                StatItem(
                    value = String.format("%.1f", gpaResult.totalCredits),
                    label = "总学分"
                )
                StatItem(
                    value = gpaResult.totalCourses.toString(),
                    label = "已修课程"
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    isHighlight: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = if (isHighlight) 32.sp else 24.sp,
            fontWeight = FontWeight.Bold,
            color = if (isHighlight) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun GradeListItem(
    grade: Grade,
    algorithm: GpaAlgorithm,
    modifier: Modifier = Modifier
) {
    val gradePoint = GpaCalculator.getGradePoint(grade.score, algorithm)
    val scoreColor = when {
        grade.score >= 90 -> Color(0xFF4CAF50)
        grade.score >= 80 -> Color(0xFF2196F3)
        grade.score >= 70 -> Color(0xFFFF9800)
        grade.score >= 60 -> Color(0xFFFF5722)
        else -> Color(0xFFF44336)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 课程类型指示
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (grade.type == com.hbde.courseschedule.data.model.CourseType.REQUIRED)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.tertiary
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 课程信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = grade.courseName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${grade.semester} · ${grade.credit}学分 · ${if (grade.type == com.hbde.courseschedule.data.model.CourseType.REQUIRED) "必修" else "选修"}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 成绩和绩点
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${grade.score.toInt()}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor
                )
                Text(
                    text = "绩点 ${String.format("%.1f", gradePoint)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyGradeState(
    onImportClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.School,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "暂无成绩数据",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "点击导入成绩",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onImportClick)
            )
        }
    }
}
