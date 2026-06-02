package com.todoapp.data.local

import androidx.room.*
import com.todoapp.data.entity.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE parentId IS NULL ORDER BY dueDate ASC, createdAt DESC")
    fun getAllTopLevelTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE parentId IS NULL AND categoryId = :categoryId ORDER BY dueDate ASC")
    fun getTasksByCategory(categoryId: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE parentId = :parentId ORDER BY sortOrder ASC")
    fun getSubTasks(parentId: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): Task?

    @Query("SELECT * FROM tasks WHERE parentId IS NULL AND isCompleted = 0 ORDER BY dueDate ASC")
    fun getPendingTasks(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("UPDATE tasks SET isCompleted = :isCompleted, completedAt = :completedAt WHERE id = :taskId")
    suspend fun updateTaskCompletion(taskId: Long, isCompleted: Boolean, completedAt: Long?)

    @Query("SELECT COUNT(*) FROM tasks WHERE parentId = :parentId AND isCompleted = 1")
    suspend fun countCompletedSubTasks(parentId: Long): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE parentId = :parentId")
    suspend fun countTotalSubTasks(parentId: Long): Int

    @Query("UPDATE tasks SET sortOrder = :sortOrder WHERE id = :taskId")
    suspend fun updateTaskSortOrder(taskId: Long, sortOrder: Int)

    @Query("SELECT * FROM tasks WHERE calendarEventId IS NOT NULL")
    suspend fun getTasksWithCalendarEventsSync(): List<Task>
}
