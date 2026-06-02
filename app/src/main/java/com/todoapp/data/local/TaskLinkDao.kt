package com.todoapp.data.local

import androidx.room.*
import com.todoapp.data.entity.Task
import com.todoapp.data.entity.TaskLink
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskLinkDao {
    @Query("SELECT t.* FROM tasks t INNER JOIN task_links tl ON (tl.sourceTaskId = t.id OR tl.targetTaskId = t.id) WHERE (tl.sourceTaskId = :taskId OR tl.targetTaskId = :taskId) AND t.id != :taskId")
    fun getLinkedTasks(taskId: Long): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(link: TaskLink): Long

    @Delete
    suspend fun deleteLink(link: TaskLink)

    @Query("DELETE FROM task_links WHERE (sourceTaskId = :taskId OR targetTaskId = :taskId)")
    suspend fun deleteAllLinksForTask(taskId: Long)

    @Query("SELECT * FROM task_links WHERE (sourceTaskId = :taskId1 AND targetTaskId = :taskId2) OR (sourceTaskId = :taskId2 AND targetTaskId = :taskId1)")
    suspend fun getLinkBetweenTasks(taskId1: Long, taskId2: Long): TaskLink?

    @Query("SELECT DISTINCT t.* FROM tasks t INNER JOIN task_links tl ON (tl.sourceTaskId = t.id OR tl.targetTaskId = t.id)")
    fun getTasksWithLinks(): Flow<List<Task>>
}
