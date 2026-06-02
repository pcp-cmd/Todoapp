package com.todoapp.ui.screens.category

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.todoapp.data.entity.Category
import com.todoapp.data.local.AppDatabase
import com.todoapp.data.repository.CategoryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CategoryUiState(
    val topLevelCategories: List<Category> = emptyList(),
    val subCategories: Map<Long, List<Category>> = emptyMap()
)

class CategoryViewModel(application: Application) : AndroidViewModel(application) {
    private val categoryRepository = CategoryRepository(
        AppDatabase.getDatabase(application).categoryDao()
    )

    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            categoryRepository.getTopLevelCategories().collect { categories ->
                val subCategoriesMap = mutableMapOf<Long, List<Category>>()
                categories.forEach { category ->
                    categoryRepository.getSubCategories(category.id).first().let { subs ->
                        subCategoriesMap[category.id] = subs
                    }
                }
                _uiState.value = CategoryUiState(
                    topLevelCategories = categories,
                    subCategories = subCategoriesMap
                )
            }
        }
    }

    fun addCategory(name: String, color: String, parentId: Long? = null) {
        viewModelScope.launch {
            val category = Category(name = name, color = color, parentId = parentId)
            categoryRepository.insertCategory(category)
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch { categoryRepository.updateCategory(category) }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch { categoryRepository.deleteCategory(category) }
    }

    fun updateSortOrder(categoryId: Long, sortOrder: Int) {
        viewModelScope.launch { categoryRepository.updateCategorySortOrder(categoryId, sortOrder) }
    }
}
