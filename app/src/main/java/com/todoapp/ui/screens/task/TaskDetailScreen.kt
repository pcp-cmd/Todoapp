package com.todoapp.ui.screens.task

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.todoapp.data.entity.Task
import com.todoapp.ui.components.TaskCheckbox
import com.todoapp.util.DateUtils
import kotlinx.coroutines.launch
import java.util.*

// Explicit FilterChip colors to prevent text from vanishing when selected
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surfaceVariant,
    labelColor = MaterialTheme.colorScheme.onSurface,
    iconColor = MaterialTheme.colorScheme.onSurface,
    selectedContainerColor = MaterialTheme.colorScheme.primary,
    selectedLabelColor = Color.White,
    selectedLeadingIconColor = Color.White,
    selectedTrailingIconColor = Color.White,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: Long,
    onNavigateBack: () -> Unit,
    viewModel: TaskViewModel = viewModel()
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<Long?>(null) }
    var priority by remember { mutableStateOf(0) }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var reminderTime by remember { mutableStateOf<Long?>(null) }
    var loadedTask by remember { mutableStateOf<Task?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var newSubTaskTitle by remember { mutableStateOf("") }
    var showLinkSearch by remember { mutableStateOf(false) }
    var linkSearchQuery by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val categories = uiState.categories.filter { it.parentId == null }
    val scope = rememberCoroutineScope()

    // Use LaunchedEffect to collect subtasks — prevents re-subscribe on every recompose
    var subTasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    if (taskId != -1L) {
        LaunchedEffect(taskId) {
            viewModel.getSubTasks(taskId).collect { subTasks = it }
        }
    }

    LaunchedEffect(taskId) {
        if (taskId != -1L) {
            viewModel.loadTask(taskId)?.let { task ->
                loadedTask = task
                title = task.title
                description = task.description ?: ""
                categoryId = task.categoryId
                priority = task.priority
                dueDate = task.dueDate
                reminderTime = task.reminderTime
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除任务") },
            text = { Text("确定要删除这个任务吗？子任务和关联也会被删除。") },
            confirmButton = {
                TextButton(onClick = {
                    loadedTask?.let { viewModel.deleteTask(it) }
                    showDeleteDialog = false
                    onNavigateBack()
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedMillis ->
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = selectedMillis
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        dueDate = cal.timeInMillis
                        // If no reminder time set, default to due date at 09:00
                        if (reminderTime == null) {
                            val reminderCal = Calendar.getInstance().apply {
                                timeInMillis = cal.timeInMillis
                                set(Calendar.HOUR_OF_DAY, 9)
                                set(Calendar.MINUTE, 0)
                            }
                            reminderTime = reminderCal.timeInMillis
                        }
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time picker dialog (for reminder time)
    if (showTimePicker) {
        val baseTime = reminderTime ?: dueDate ?: System.currentTimeMillis()
        val baseCal = Calendar.getInstance().apply { timeInMillis = baseTime }
        val timePickerState = rememberTimePickerState(
            initialHour = baseCal.get(Calendar.HOUR_OF_DAY),
            initialMinute = baseCal.get(Calendar.MINUTE)
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("提醒时间") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = dueDate ?: System.currentTimeMillis()
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    reminderTime = cal.timeInMillis
                    // Also set dueDate if not set
                    if (dueDate == null) {
                        val dueCal = Calendar.getInstance().apply {
                            timeInMillis = cal.timeInMillis
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                        }
                        dueDate = dueCal.timeInMillis
                    }
                    showTimePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("取消") }
            }
        )
    }

    // Use Column instead of nested Scaffold — guarantees save button stays visible
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
        TopAppBar(
            title = { Text(if (taskId == -1L) "新建任务" else "编辑任务") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                if (taskId != -1L) {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("任务标题 *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("描述（可选）") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                maxLines = 5
            )

            // Due date
            Text("截止日期", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { showDatePicker = true }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val dd = dueDate
                    Text(
                        if (dd != null) DateUtils.formatDate(dd) else "选择日期",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (dd != null)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (dd != null) {
                        Text(
                            DateUtils.formatRelativeDate(dd),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (dueDate != null) {
                    IconButton(onClick = {
                        dueDate = null
                        reminderTime = null
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "清除", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Reminder time
            Text("提醒时间", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { showTimePicker = true }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val rt = reminderTime
                    Text(
                        if (rt != null) DateUtils.formatDateTime(rt) else "选择提醒时间",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (rt != null)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (rt != null) {
                        Text(
                            "到时将收到系统日历通知",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (reminderTime != null) {
                    IconButton(onClick = { reminderTime = null }) {
                        Icon(Icons.Default.Close, contentDescription = "清除", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Category with explicit chip colors
            Text("分类", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = categoryId == null,
                        onClick = { categoryId = null },
                        label = { Text("无分类") },
                        colors = chipColors()
                    )
                }
                items(categories) { cat ->
                    FilterChip(
                        selected = categoryId == cat.id,
                        onClick = { categoryId = cat.id },
                        label = { Text(cat.name) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(cat.color)))
                            )
                        },
                        colors = chipColors()
                    )
                }
            }

            // Priority with explicit chip colors
            Text("优先级", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "无", 1 to "低", 2 to "中", 3 to "高").forEach { (level, label) ->
                    FilterChip(
                        selected = priority == level,
                        onClick = { priority = level },
                        label = { Text(label) },
                        colors = chipColors()
                    )
                }
            }

            // Subtasks — input field FIRST so it stays visible when list grows
            if (taskId != -1L) {
                Text("子任务", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

                // Add subtask input — placed BEFORE the list so it never gets pushed off screen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newSubTaskTitle,
                        onValueChange = { newSubTaskTitle = it },
                        label = { Text("添加子任务") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(onClick = {
                        if (newSubTaskTitle.isNotBlank()) {
                            viewModel.addSubTask(taskId, newSubTaskTitle)
                            newSubTaskTitle = ""
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add subtask")
                    }
                }

                // Subtask list — below the input
                subTasks.forEach { subTask ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TaskCheckbox(
                            isChecked = subTask.isCompleted,
                            onCheckedChange = { viewModel.toggleTaskCompletion(subTask) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = subTask.title,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            textDecoration = if (subTask.isCompleted) TextDecoration.LineThrough else null,
                            color = if (subTask.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { viewModel.deleteTask(subTask) }) {
                            Icon(Icons.Default.Close, contentDescription = "Delete subtask", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Linked tasks (only for existing tasks)
            if (taskId != -1L) {
                Text("关联任务", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

                val linkedTasks by viewModel.getLinkedTasksForTask(taskId).collectAsState(initial = emptyList())
                linkedTasks.forEach { linked ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = linked.title,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            scope.launch { viewModel.removeLink(taskId, linked.id) }
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove link", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                if (showLinkSearch) {
                    OutlinedTextField(
                        value = linkSearchQuery,
                        onValueChange = { linkSearchQuery = it },
                        label = { Text("搜索要关联的任务") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { showLinkSearch = false; linkSearchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel")
                            }
                        }
                    )
                    val searchResults by viewModel.searchTasks(linkSearchQuery, taskId).collectAsState(initial = emptyList())
                    searchResults.take(5).forEach { result ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        viewModel.addLink(taskId, result.id)
                                        linkSearchQuery = ""
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(result.title, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    if (linkSearchQuery.isNotBlank() && searchResults.isEmpty()) {
                        Text(
                            "没有找到匹配的任务",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                } else {
                    TextButton(onClick = { showLinkSearch = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("添加关联")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Save button pinned at bottom — never hidden by keyboard or scroll
        Surface(
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        if (taskId == -1L) {
                            viewModel.addTask(title, categoryId = categoryId, dueDate = dueDate, reminderTime = reminderTime)
                        } else {
                            loadedTask?.let {
                                viewModel.updateTask(it.copy(
                                    title = title,
                                    description = description,
                                    categoryId = categoryId,
                                    priority = priority,
                                    dueDate = dueDate,
                                    reminderTime = reminderTime
                                ))
                            }
                        }
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                enabled = title.isNotBlank(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    "保存",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
