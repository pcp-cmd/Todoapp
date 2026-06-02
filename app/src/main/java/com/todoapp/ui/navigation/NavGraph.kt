package com.todoapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.todoapp.ui.screens.category.CategoryManagementScreen
import com.todoapp.ui.screens.link.LinkViewScreen
import com.todoapp.ui.screens.settings.SettingsScreen
import com.todoapp.ui.screens.task.TaskDetailScreen
import com.todoapp.ui.screens.task.TaskHomeScreen
import com.todoapp.ui.screens.task.TaskViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun NavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    val taskViewModel: TaskViewModel = viewModel()

    NavHost(navController = navController, startDestination = "home", modifier = modifier) {
        composable("home") {
            TaskHomeScreen(
                onTaskClick = { taskId -> navController.navigate("task/$taskId") }
            )
        }
        composable(
            route = "task/{taskId}",
            arguments = listOf(navArgument("taskId") { type = NavType.LongType; defaultValue = -1L }),
            deepLinks = listOf(navDeepLink { uriPattern = "todoapp://task/{taskId}" })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getLong("taskId") ?: -1L
            TaskDetailScreen(taskId = taskId, onNavigateBack = { navController.popBackStack() })
        }
        composable("categories") { CategoryManagementScreen() }
        composable("links") { LinkViewScreen(onTaskClick = { taskId -> navController.navigate("task/$taskId") }) }
        composable("settings") { SettingsScreen(taskViewModel = taskViewModel) }
    }
}
