package com.todoapp.data.local

import androidx.room.*
import com.todoapp.data.entity.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE parentId IS NULL ORDER BY sortOrder ASC")
    fun getTopLevelCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE parentId = :parentId ORDER BY sortOrder ASC")
    fun getSubCategories(parentId: Long): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("UPDATE tasks SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun unassignTasksFromCategory(categoryId: Long)

    @Query("UPDATE categories SET sortOrder = :sortOrder WHERE id = :categoryId")
    suspend fun updateCategorySortOrder(categoryId: Long, sortOrder: Int)
}
