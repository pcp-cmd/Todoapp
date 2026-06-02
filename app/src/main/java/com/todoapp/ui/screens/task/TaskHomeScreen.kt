package com.todoapp.ui.screens.task

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.todoapp.ui.components.TaskCard
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskHomeScreen(
    onTaskClick: (Long) -> Unit,
    viewModel: TaskViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var viewMode by remember { mutableStateOf("timeline") }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp)
            ) {
                Text(
                    text = "我的任务",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${SimpleDateFormat("M月d日", Locale.getDefault()).format(Date())} · ${uiState.tasks.count { !it.isCompleted }} 个待办",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Tab bar — both tabs always visible with high contrast
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("timeline" to "时间线", "category" to "分类").forEach { (mode, label) ->
                        val selected = viewMode == mode
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewMode = mode }
                                .padding(vertical = 12.dp),
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // Underline indicator
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.4f)
                                    .height(3.dp)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f),
                                        androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }
        // FAB removed — managed by MainActivity
    ) { padding ->
        // 收集所有子任务
        val subTasksMap by viewModel.allSubTasks.collectAsState()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // 根据排序方式排序任务
            val sortedTasks = when (uiState.sortOrder) {
                "dueDate" -> uiState.tasks.sortedBy { it.dueDate ?: Long.MAX_VALUE }
                "priority" -> uiState.tasks.sortedByDescending { it.priority }
                "createdAt" -> uiState.tasks.sortedByDescending { it.createdAt }
                else -> uiState.tasks
            }

            if (viewMode == "category") {
                // 分类视图：按分类分组显示
                val tasksByCategory = sortedTasks.groupBy { it.categoryId }

                // 先显示有分类的任务
                uiState.categories.forEach { category ->
                    val tasksInCategory = tasksByCategory[category.id] ?: emptyList()
                    if (tasksInCategory.isNotEmpty()) {
                        // 分类标题
                        item(key = "category_header_${category.id}") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(category.color)))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "(${tasksInCategory.size})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // 显示该分类下的任务
                        tasksInCategory.forEach { task ->
                            item(key = "task_${task.id}") {
                                TaskCard(
                                    task = task,
                                    category = category,
                                    subTaskProgress = null,
                                    onClick = { onTaskClick(task.id) },
                                    onToggleComplete = { viewModel.toggleTaskCompletion(task) }
                                )
                            }

                            // 显示子任务
                            val subTasks = subTasksMap[task.id] ?: emptyList()
                            subTasks.forEach { subTask ->
                                item(key = "subtask_${subTask.id}") {
                                    val subCategory = uiState.categories.find { it.id == subTask.categoryId }
                                    TaskCard(
                                        task = subTask,
                                        category = subCategory,
                                        subTaskProgress = null,
                                        onClick = { onTaskClick(subTask.id) },
                                        onToggleComplete = { viewModel.toggleTaskCompletion(subTask) },
                                        isSubTask = true,
                                        depth = 1
                                    )
                                }
                            }
                        }

                        // 分类之间添加间距
                        item(key = "category_spacer_${category.id}") {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                // 最后显示无分类的任务
                val uncategorizedTasks = tasksByCategory[null] ?: emptyList()
                if (uncategorizedTasks.isNotEmpty()) {
                    item(key = "category_header_uncategorized") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "无分类",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "(${uncategorizedTasks.size})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    uncategorizedTasks.forEach { task ->
                        item(key = "task_${task.id}") {
                            TaskCard(
                                task = task,
                                category = null,
                                subTaskProgress = null,
                                onClick = { onTaskClick(task.id) },
                                onToggleComplete = { viewModel.toggleTaskCompletion(task) }
                            )
                        }

                        val subTasks = subTasksMap[task.id] ?: emptyList()
                        subTasks.forEach { subTask ->
                            item(key = "subtask_${subTask.id}") {
                                TaskCard(
                                    task = subTask,
                                    category = null,
                                    subTaskProgress = null,
                                    onClick = { onTaskClick(subTask.id) },
                                    onToggleComplete = { viewModel.toggleTaskCompletion(subTask) },
                                    isSubTask = true,
                                    depth = 1
                                )
                            }
                        }
                    }
                }
            } else {
                // 时间线视图：按排序方式显示所有任务
                sortedTasks.forEach { task ->
                    item(key = "task_${task.id}") {
                        val category = uiState.categories.find { it.id == task.categoryId }
                        TaskCard(
                            task = task,
                            category = category,
                            subTaskProgress = null,
                            onClick = { onTaskClick(task.id) },
                            onToggleComplete = { viewModel.toggleTaskCompletion(task) }
                        )
                    }

                    // 显示子任务
                    val subTasks = subTasksMap[task.id] ?: emptyList()
                    subTasks.forEach { subTask ->
                        item(key = "subtask_${subTask.id}") {
                            val subCategory = uiState.categories.find { it.id == subTask.categoryId }
                            TaskCard(
                                task = subTask,
                                category = subCategory,
                                subTaskProgress = null,
                                onClick = { onTaskClick(subTask.id) },
                                onToggleComplete = { viewModel.toggleTaskCompletion(subTask) },
                                isSubTask = true,
                                depth = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
