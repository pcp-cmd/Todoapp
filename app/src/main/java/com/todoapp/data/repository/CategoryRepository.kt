package com.todoapp.data.repository

import com.todoapp.data.entity.Category
import com.todoapp.data.local.CategoryDao
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val categoryDao: CategoryDao) {
    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()

    fun getTopLevelCategories(): Flow<List<Category>> = categoryDao.getTopLevelCategories()

    fun getSubCategories(parentId: Long): Flow<List<Category>> = categoryDao.getSubCategories(parentId)

    suspend fun getCategoryById(id: Long): Category? = categoryDao.getCategoryById(id)

    suspend fun insertCategory(category: Category): Long = categoryDao.insertCategory(category)

    suspend fun updateCategory(category: Category) = categoryDao.updateCategory(category)

    suspend fun deleteCategory(category: Category, reassignTo: Long? = null) {
        if (reassignTo == null) {
            categoryDao.unassignTasksFromCategory(category.id)
        }
        categoryDao.deleteCategory(category)
    }

    suspend fun updateCategorySortOrder(categoryId: Long, sortOrder: Int) {
        categoryDao.updateCategorySortOrder(categoryId, sortOrder)
    }
}
