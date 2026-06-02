package com.todoapp.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.todoapp.data.preferences.UserPreferences
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: String = "system",
    val sortOrder: String = "dueDate",
    val calendarSyncEnabled: Boolean = true
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val userPreferences = UserPreferences(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                userPreferences.themeMode,
                userPreferences.sortOrder,
                userPreferences.calendarSyncEnabled
            ) { theme, sort, calendar ->
                SettingsUiState(themeMode = theme, sortOrder = sort, calendarSyncEnabled = calendar)
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { userPreferences.setThemeMode(mode) }
    }

    fun setSortOrder(order: String) {
        viewModelScope.launch { userPreferences.setSortOrder(order) }
    }

    fun setCalendarSyncEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setCalendarSyncEnabled(enabled) }
    }
}
