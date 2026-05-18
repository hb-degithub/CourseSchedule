package com.hbde.courseschedule.ui.importer

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPreviewScreen(
    viewModel: ImportPreviewViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showConflictDialog by remember { mutableStateOf(false) }
    var conflictItem by remember { mutableStateOf<ImportPreviewItem?>(null) }

    LaunchedEffect(uiState) {
        val state = uiState as? ImportPreviewUiState.Success ?: return@LaunchedEffect
        when (val result = state.importResult) {
            is ImportResult.Success -> {
                snackbarHostState.showSnackbar("成功导入 ${result.importedCount} 门课程")
                viewModel.clearImportResult()
            }
            is ImportResult.Error -> {
                snackbarHostState.showSnackbar(result.message)
                viewModel.clearImportResult()
            }
            else -> {}
        }
    }

    // 冲突详情对话框
    if (showConflictDialog && conflictItem != null) {
        ConflictDetailDialog(
            item = conflictItem!!,
            onDismiss = { showConflictDialog = false; conflictItem = null },
            onImportAnyway = {
                val index = (uiState as? ImportPreviewUiState.Success)?.items?.indexOf(conflictItem!!)
                index?.let { viewModel.forceSelect(it) }
                showConflictDialog = false
                conflictItem = null
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("导入预览") },
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
        },
        bottomBar = {
            ImportPreviewBottomBar(
                uiState = uiState,
                onToggleSelectAll = viewModel::toggleSelectAll,
                onImport = viewModel::importSelected
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is ImportPreviewUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is ImportPreviewUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                is ImportPreviewUiState.Success -> {
                    if (state.items.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "没有可导入的课程",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(state.items) { index, item ->
                                ImportPreviewItemCard(
                                    item = item,
                                    onToggle = { viewModel.toggleSelection(index) },
                                    onConflictClick = {
                                        conflictItem = item
                                        showConflictDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConflictDetailDialog(
    item: ImportPreviewItem,
    onDismiss: () -> Unit,
    onImportAnyway: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("课程时间冲突") },
        text = {
            Column {
                Text(
                    text = "课程「${item.rawCourse.name}」与已有课程存在时间冲突：",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "· 周${getDayName(item.rawCourse.dayOfWeek)} 第${item.rawCourse.startNode}-${item.rawCourse.endNode}节",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "· ${item.rawCourse.startWeek}-${item.rawCourse.endWeek}周",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "强制导入可能导致课程重叠显示，是否继续？",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onImportAnyway) {
                Text("强制导入", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun ImportPreviewItemCard(
    item: ImportPreviewItem,
    onToggle: () -> Unit,
    onConflictClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (item.hasConflict) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Checkbox(
                    checked = item.isSelected,
                    onCheckedChange = { onToggle() }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.rawCourse.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = buildString {
                            append("周${getDayName(item.rawCourse.dayOfWeek)}")
                            append("  第${item.rawCourse.startNode}-${item.rawCourse.endNode}节")
                            append("  ${item.rawCourse.startWeek}-${item.rawCourse.endWeek}周")
                            if (item.rawCourse.teacher.isNotBlank()) {
                                append("  ${item.rawCourse.teacher}")
                            }
                            if (item.rawCourse.classroom.isNotBlank()) {
                                append("  ${item.rawCourse.classroom}")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (item.hasConflict) {
                TextButton(onClick = onConflictClick) {
                    Text(
                        text = "冲突",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportPreviewBottomBar(
    uiState: ImportPreviewUiState,
    onToggleSelectAll: () -> Unit,
    onImport: () -> Unit
) {
    val state = uiState as? ImportPreviewUiState.Success ?: return
    val selectedCount = state.items.count { it.isSelected }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onToggleSelectAll) {
                Text(if (state.allSelected) "取消全选" else "全选")
            }
            Text(
                text = "已选 $selectedCount / ${state.items.size} 门",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onImport,
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedCount > 0
        ) {
            Text("导入选中课程")
        }
    }
}

private fun getDayName(dayOfWeek: Int): String {
    return when (dayOfWeek) {
        1 -> "一"
        2 -> "二"
        3 -> "三"
        4 -> "四"
        5 -> "五"
        6 -> "六"
        7 -> "日"
        else -> "?"
    }
}
