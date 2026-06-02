package com.todoapp.ui.screens.task

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.todoapp.data.entity.Category
import com.todoapp.data.entity.Task
import com.todoapp.data.local.AppDatabase
import com.todoapp.data.preferences.UserPreferences
import com.todoapp.data.repository.CalendarRepository
import com.todoapp.data.repository.CategoryRepository
import com.todoapp.data.repository.TaskRepository
import com.todoapp.domain.RepeatRuleEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "TodoApp-Calendar"

data class TaskUiState(
    val tasks: List<Task> = emptyList(),
    val categories: List<Category> = emptyList(),
    val sortOrder: String = "dueDate",
    val filterCategory: Long? = null,
    val filterPriority: Int? = null,
    val filterCompleted: Boolean? = null,
    val calendarPermissionGranted: Boolean = false,
    val calendarSyncEnabled: Boolean = true
)

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val taskRepository = TaskRepository(database.taskDao())
    private val categoryRepository = CategoryRepository(database.categoryDao())
    private val calendarRepository = CalendarRepository(application)
    private val userPreferences = UserPreferences(application)

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    // One-shot event messages (e.g. sync result feedback)
    private val _calendarSyncMessage = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 5)
    val calendarSyncMessage: SharedFlow<String> = _calendarSyncMessage.asSharedFlow()

    // 收集所有任务的子任务，用于在主列表中显示
    @OptIn(ExperimentalCoroutinesApi::class)
    val allSubTasks: StateFlow<Map<Long, List<Task>>> = taskRepository.getAllTopLevelTasks()
        .flatMapLatest { topLevelTasks ->
            // 为每个顶层任务获取其子任务
            val subTaskFlows = topLevelTasks.map { task ->
                taskRepository.getSubTasks(task.id).map { subTasks ->
                    task.id to subTasks
                }
            }
            // 合并所有子任务 Flow
            combine(subTaskFlows) { pairs ->
                pairs.toMap()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    init {
        // Check calendar permission on init
        viewModelScope.launch {
            val hasPermission = checkCalendarPermission()
            _uiState.update { it.copy(calendarPermissionGranted = hasPermission) }
        }

        viewModelScope.launch {
            combine(
                taskRepository.getAllTopLevelTasks(),
                categoryRepository.getAllCategories(),
                userPreferences.sortOrder,
                userPreferences.calendarSyncEnabled
            ) { tasks, categories, sortOrder, calendarSyncEnabled ->
                TaskUiState(
                    tasks = tasks,
                    categories = categories,
                    sortOrder = sortOrder,
                    calendarPermissionGranted = _uiState.value.calendarPermissionGranted,
                    calendarSyncEnabled = calendarSyncEnabled
                )
            }.collect { state ->
                _uiState.update { current ->
                    current.copy(
                        tasks = state.tasks,
                        categories = state.categories,
                        sortOrder = state.sortOrder,
                        calendarSyncEnabled = state.calendarSyncEnabled
                    )
                }
            }
        }
    }

    fun refreshCalendarPermission() {
        viewModelScope.launch {
            val hasPermission = checkCalendarPermission()
            _uiState.update { it.copy(calendarPermissionGranted = hasPermission) }
        }
    }

    fun addTask(title: String, categoryId: Long? = null, dueDate: Long? = null, reminderTime: Long? = null) {
        viewModelScope.launch {
            val task = Task(title = title, categoryId = categoryId, dueDate = dueDate, reminderTime = reminderTime)
            val id = taskRepository.insertTask(task)
            val savedTask = taskRepository.getTaskById(id) ?: task.copy(id = id)
            syncCalendarEvent(savedTask)
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskRepository.updateTask(task)
            // Get fresh task from DB (ensures we have the correct ID and latest calendarEventId)
            val freshTask = taskRepository.getTaskById(task.id) ?: task
            syncCalendarEvent(freshTask)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            task.calendarEventId?.let { eventId ->
                withContext(Dispatchers.IO) {
                    calendarRepository.deleteCalendarEvent(eventId)
                }
            }
            taskRepository.deleteTask(task)
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val newCompleted = !task.isCompleted
            taskRepository.updateTaskCompletion(task.id, newCompleted)
            if (newCompleted && task.repeatRule != null) {
                createNextRepeatInstance(task)
            }
        }
    }

    fun setFilterCategory(categoryId: Long?) {
        _uiState.update { it.copy(filterCategory = categoryId) }
    }

    fun setFilterPriority(priority: Int?) {
        _uiState.update { it.copy(filterPriority = priority) }
    }

    fun setFilterCompleted(completed: Boolean?) {
        _uiState.update { it.copy(filterCompleted = completed) }
    }

    private suspend fun syncCalendarEvent(task: Task) {
        // Check user preference first
        val syncEnabled = userPreferences.calendarSyncEnabled.first()
        if (!syncEnabled) {
            Log.d(TAG, "syncCalendarEvent skipped: sync disabled")
            return
        }
        if (task.reminderTime == null && task.dueDate == null) {
            Log.d(TAG, "syncCalendarEvent skipped: no dueDate or reminderTime for task '${task.title}'")
            return
        }
        if (!checkCalendarPermission()) {
            Log.w(TAG, "syncCalendarEvent skipped: no calendar permission")
            return
        }

        val deepLinkUri = "todoapp://task/${task.id}"
        try {
            withContext(Dispatchers.IO) {
                if (task.calendarEventId != null) {
                    // Check if event still exists (may have been deleted externally)
                    if (calendarRepository.eventExists(task.calendarEventId)) {
                        calendarRepository.updateCalendarEvent(task.calendarEventId, task, deepLinkUri)
                        Log.d(TAG, "syncCalendarEvent: updated event ${task.calendarEventId} for '${task.title}'")
                    } else {
                        // Event deleted externally — clear and recreate
                        Log.d(TAG, "syncCalendarEvent: event ${task.calendarEventId} deleted externally, recreating")
                        taskRepository.updateTask(task.copy(calendarEventId = null))
                        val newId = calendarRepository.createCalendarEvent(task, deepLinkUri)
                        newId?.let {
                            taskRepository.updateTask(task.copy(calendarEventId = it))
                            Log.d(TAG, "syncCalendarEvent: recreated event $newId for '${task.title}'")
                        }
                    }
                } else {
                    val eventId = calendarRepository.createCalendarEvent(task, deepLinkUri)
                    eventId?.let {
                        taskRepository.updateTask(task.copy(calendarEventId = it))
                        Log.d(TAG, "syncCalendarEvent: created event $eventId for '${task.title}'")
                    } ?: Log.w(TAG, "syncCalendarEvent: createCalendarEvent returned null for '${task.title}'")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "syncCalendarEvent failed for '${task.title}'", e)
        }
    }

    private suspend fun checkCalendarPermission(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_CALENDAR
                ) == PackageManager.PERMISSION_GRANTED
                Log.d(TAG, "checkCalendarPermission: granted=$granted")
                granted
            } catch (e: Exception) {
                Log.e(TAG, "checkCalendarPermission failed", e)
                false
            }
        }
    }

    fun toggleCalendarSync(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setCalendarSyncEnabled(enabled)
            if (enabled) {
                if (!checkCalendarPermission()) {
                    _calendarSyncMessage.emit("没有日历权限，请先授权")
                    return@launch
                }
                val count = recreateAllCalendarEvents()
                _calendarSyncMessage.emit(if (count >= 0) "已同步 $count 个任务到日历" else "日历同步失败")
            } else {
                val count = clearAllCalendarEvents()
                _calendarSyncMessage.emit("已清除 $count 个日历事件")
            }
        }
    }

    /**
     * Clear all calendar events. Returns the number of events cleared, or -1 on error.
     */
    private suspend fun clearAllCalendarEvents(): Int {
        return try {
            val tasks = taskRepository.getTasksWithCalendarEvents()
            var count = 0
            withContext(Dispatchers.IO) {
                for (task in tasks) {
                    try {
                        task.calendarEventId?.let { calendarRepository.deleteCalendarEvent(it) }
                        taskRepository.updateTask(task.copy(calendarEventId = null))
                        count++
                    } catch (e: Exception) {
                        Log.e(TAG, "clearAllCalendarEvents: failed to delete event for '${task.title}'", e)
                    }
                }
            }
            Log.d(TAG, "clearAllCalendarEvents: cleared $count events")
            count
        } catch (e: Exception) {
            Log.e(TAG, "clearAllCalendarEvents failed", e)
            -1
        }
    }

    /**
     * Recreate all calendar events for eligible tasks.
     * Returns the number of events created, or -1 on error.
     */
    private suspend fun recreateAllCalendarEvents(): Int {
        if (!checkCalendarPermission()) {
            Log.w(TAG, "recreateAllCalendarEvents: no permission")
            return -1
        }
        return try {
            val calendarId = withContext(Dispatchers.IO) { calendarRepository.getDefaultCalendarId() }
            if (calendarId < 0) {
                Log.e(TAG, "recreateAllCalendarEvents: no writable calendar found")
                _calendarSyncMessage.emit("找不到可用的日历账户，请先在系统日历中添加账户")
                return -1
            }
            // All incomplete tasks with a dueDate or reminderTime
            val allTasks = taskRepository.getAllTopLevelTasks().first()
            val tasksToSync = allTasks.filter { task ->
                !task.isCompleted && (task.dueDate != null || task.reminderTime != null)
            }
            Log.d(TAG, "recreateAllCalendarEvents: found ${tasksToSync.size} tasks to sync")
            var count = 0
            withContext(Dispatchers.IO) {
                for (task in tasksToSync) {
                    try {
                        val deepLinkUri = "todoapp://task/${task.id}"
                        // If event already exists and is still valid, skip
                        if (task.calendarEventId != null && calendarRepository.eventExists(task.calendarEventId)) {
                            Log.d(TAG, "recreateAllCalendarEvents: event ${task.calendarEventId} already exists, skipping")
                            continue
                        }
                        val eventId = calendarRepository.createCalendarEvent(task, deepLinkUri)
                        eventId?.let {
                            taskRepository.updateTask(task.copy(calendarEventId = it))
                            count++
                            Log.d(TAG, "recreateAllCalendarEvents: created event $eventId for '${task.title}'")
                        } ?: Log.w(TAG, "recreateAllCalendarEvents: createCalendarEvent returned null for '${task.title}'")
                    } catch (e: Exception) {
                        Log.e(TAG, "recreateAllCalendarEvents: failed for '${task.title}'", e)
                    }
                }
            }
            Log.d(TAG, "recreateAllCalendarEvents: created $count events")
            count
        } catch (e: Exception) {
            Log.e(TAG, "recreateAllCalendarEvents failed", e)
            -1
        }
    }

    private suspend fun createNextRepeatInstance(task: Task) {
        val currentDate = task.dueDate ?: task.reminderTime ?: return
        val nextDate = RepeatRuleEngine.calculateNextDate(task.repeatRule!!, currentDate)
        val nextTask = task.copy(
            id = 0,
            dueDate = nextDate,
            reminderTime = task.reminderTime?.let { nextDate + (it - currentDate) },
            isCompleted = false,
            completedAt = null,
            calendarEventId = null,
            createdAt = System.currentTimeMillis()
        )
        val newId = taskRepository.insertTask(nextTask)
        syncCalendarEvent(nextTask.copy(id = newId))
    }

    suspend fun loadTask(taskId: Long): Task? {
        return taskRepository.getTaskById(taskId)
    }

    fun addSubTask(parentId: Long, title: String) {
        viewModelScope.launch {
            val subTask = Task(title = title, parentId = parentId)
            taskRepository.insertTask(subTask)
        }
    }

    fun getSubTasks(parentId: Long): Flow<List<Task>> {
        return taskRepository.getSubTasks(parentId)
    }

    fun getTasksWithLinks(): Flow<Map<Task, List<Task>>> {
        val taskLinkDao = database.taskLinkDao()
        return taskLinkDao.getTasksWithLinks().map { tasks ->
            val result = mutableMapOf<Task, List<Task>>()
            tasks.forEach { task ->
                val linked = taskLinkDao.getLinkedTasks(task.id).first()
                if (linked.isNotEmpty()) {
                    result[task] = linked
                }
            }
            result
        }
    }

    fun getLinkedTasksForTask(taskId: Long): Flow<List<Task>> {
        return database.taskLinkDao().getLinkedTasks(taskId)
    }

    suspend fun addLink(sourceTaskId: Long, targetTaskId: Long) {
        val dao = database.taskLinkDao()
        val existing = dao.getLinkBetweenTasks(sourceTaskId, targetTaskId)
        if (existing == null) {
            dao.insertLink(com.todoapp.data.entity.TaskLink(
                sourceTaskId = sourceTaskId,
                targetTaskId = targetTaskId
            ))
        }
    }

    suspend fun removeLink(sourceTaskId: Long, targetTaskId: Long) {
        val dao = database.taskLinkDao()
        val link = dao.getLinkBetweenTasks(sourceTaskId, targetTaskId)
        link?.let { dao.deleteLink(it) }
    }

    fun searchTasks(query: String, excludeTaskId: Long): Flow<List<Task>> {
        if (query.isBlank()) return flowOf(emptyList())
        return database.taskDao().getAllTopLevelTasks().map { tasks ->
            tasks.filter { it.id != excludeTaskId && it.title.contains(query, ignoreCase = true) }
        }
    }
}
