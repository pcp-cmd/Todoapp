package com.todoapp.data.repository

import com.todoapp.data.entity.Task
import com.todoapp.data.local.TaskDao
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    fun getAllTopLevelTasks(): Flow<List<Task>> = taskDao.getAllTopLevelTasks()

    fun getTasksByCategory(categoryId: Long): Flow<List<Task>> = taskDao.getTasksByCategory(categoryId)

    fun getSubTasks(parentId: Long): Flow<List<Task>> = taskDao.getSubTasks(parentId)

    suspend fun getTaskById(id: Long): Task? = taskDao.getTaskById(id)

    fun getPendingTasks(): Flow<List<Task>> = taskDao.getPendingTasks()

    suspend fun insertTask(task: Task): Long = taskDao.insertTask(task)

    suspend fun updateTask(task: Task) = taskDao.updateTask(task)

    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)

    suspend fun updateTaskCompletion(taskId: Long, isCompleted: Boolean) {
        val completedAt = if (isCompleted) System.currentTimeMillis() else null
        taskDao.updateTaskCompletion(taskId, isCompleted, completedAt)
    }

    suspend fun getSubTaskProgress(parentId: Long): Pair<Int, Int> {
        val completed = taskDao.countCompletedSubTasks(parentId)
        val total = taskDao.countTotalSubTasks(parentId)
        return Pair(completed, total)
    }

    suspend fun updateTaskSortOrder(taskId: Long, sortOrder: Int) {
        taskDao.updateTaskSortOrder(taskId, sortOrder)
    }

    suspend fun getTasksWithCalendarEvents(): List<Task> = taskDao.getTasksWithCalendarEventsSync()
}
