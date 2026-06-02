package com.todoapp.util

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.todoapp.data.entity.Category
import com.todoapp.data.entity.Task
import com.todoapp.data.entity.TaskLink
import com.todoapp.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

data class BackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val categories: List<Category>,
    val tasks: List<Task>,
    val taskLinks: List<TaskLink>
)

class BackupManager(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val gson = Gson()

    suspend fun exportToUri(uri: Uri) = withContext(Dispatchers.IO) {
        val categories = database.categoryDao().getAllCategories().first()
        val tasks = database.taskDao().getAllTopLevelTasks().first()

        val backup = BackupData(
            categories = categories,
            tasks = tasks,
            taskLinks = emptyList()
        )

        val json = gson.toJson(backup)
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(json.toByteArray())
        }
    }

    suspend fun importFromUri(uri: Uri) = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val reader = BufferedReader(InputStreamReader(inputStream))
            val json = reader.readText()
            val type = object : TypeToken<BackupData>() {}.type
            val backup: BackupData = gson.fromJson(json, type)

            database.clearAllTables()
            backup.categories.forEach { database.categoryDao().insertCategory(it) }
            backup.tasks.forEach { database.taskDao().insertTask(it) }
            backup.taskLinks.forEach { database.taskLinkDao().insertLink(it) }
        }
    }
}
