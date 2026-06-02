package com.todoapp.ui.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.todoapp.ui.screens.task.TaskViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    taskViewModel: TaskViewModel? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val fallbackState = remember { kotlinx.coroutines.flow.MutableStateFlow(com.todoapp.ui.screens.task.TaskUiState()) }
    val taskState by (taskViewModel?.uiState ?: fallbackState).collectAsState()
    val hasCalendarPermission = taskState.calendarPermissionGranted
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Observe one-shot calendar sync result messages
    LaunchedEffect(taskViewModel) {
        taskViewModel?.calendarSyncMessage?.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Permission launcher for calendar sync
    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            taskViewModel?.refreshCalendarPermission()
            taskViewModel?.toggleCalendarSync(true)
        } else {
            // Revert the switch if permission denied
            viewModel.setCalendarSyncEnabled(false)
            scope.launch {
                snackbarHostState.showSnackbar("需要日历权限才能同步任务到系统日历")
            }
        }
    }

    // Track pending state to avoid switch flicker during permission dialog
    var pendingSyncState by remember { mutableStateOf<Boolean?>(null) }
    val switchChecked = pendingSyncState ?: uiState.calendarSyncEnabled

    // Reset pending state when actual state updates
    LaunchedEffect(uiState.calendarSyncEnabled) {
        pendingSyncState = null
    }

    fun handleCalendarToggle(newValue: Boolean) {
        pendingSyncState = newValue
        if (newValue) {
            // Check if permission already granted
            val hasPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_CALENDAR
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                taskViewModel?.toggleCalendarSync(true)
            } else {
                // Request permission
                calendarPermissionLauncher.launch(
                    arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
                )
            }
        } else {
            taskViewModel?.toggleCalendarSync(false)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "设置",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ── Theme ──
            SettingsSection(title = "外观") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ThemeOption(
                        label = "浅色",
                        icon = Icons.Default.Check, // LightbulbOutline not available
                        selected = uiState.themeMode == "light",
                        onClick = { viewModel.setThemeMode("light") },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOption(
                        label = "深色",
                        icon = Icons.Default.Close, // placeholder
                        selected = uiState.themeMode == "dark",
                        onClick = { viewModel.setThemeMode("dark") },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOption(
                        label = "系统",
                        icon = Icons.Default.Settings,
                        selected = uiState.themeMode == "system",
                        onClick = { viewModel.setThemeMode("system") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Sort order ──
            SettingsSection(title = "排序方式") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "dueDate" to "按时间",
                        "priority" to "按优先级",
                        "createdAt" to "按创建"
                    ).forEach { (order, label) ->
                        FilterChip(
                            selected = uiState.sortOrder == order,
                            onClick = { viewModel.setSortOrder(order) },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // ── Calendar sync ──
            SettingsSection(title = "日历同步") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "同步到系统日历",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        val statusText = when {
                            !uiState.calendarSyncEnabled -> "已关闭"
                            hasCalendarPermission -> "已开启"
                            else -> "需要授权"
                        }
                        Text(
                            statusText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (hasCalendarPermission && uiState.calendarSyncEnabled)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Switch(
                        checked = switchChecked,
                        onCheckedChange = { handleCalendarToggle(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ── Reusable section wrapper ──────────────────────────────────────────
@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        content()
    }
}

// ── Theme option card ─────────────────────────────────────────────────
@Composable
private fun ThemeOption(
    label: String,
    @Suppress("UNUSED_PARAMETER") icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline

    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) primary else outline,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (selected) primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        if (label == "深色") Color(0xFF1A1917) else if (label == "浅色") Color(0xFFFAF9F5) else Color(0xFF8A857D),
                        CircleShape
                    )
                    .border(1.dp, outline, CircleShape)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
