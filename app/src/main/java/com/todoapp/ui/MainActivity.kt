package com.todoapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.todoapp.data.preferences.UserPreferences
import com.todoapp.ui.navigation.NavGraph
import com.todoapp.ui.theme.TodoAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userPreferences = UserPreferences(applicationContext)
        setContent {
            val themeMode by userPreferences.themeMode
                .collectAsState(initial = "system")

            val forceDarkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> null  // follow system
            }

            TodoAppTheme(forceDarkTheme = forceDarkTheme) {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem("home", "任务", Icons.Default.Home),
        BottomNavItem("categories", "分类", Icons.Default.Menu),
        BottomNavItem("links", "关联", Icons.Default.Share),
        BottomNavItem("settings", "设置", Icons.Default.Settings)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            // Only show FAB on the home page (task list).
            // Category page has its own FAB inside its screen.
            // Other pages (links, settings, task detail) don't need a FAB.
            if (currentRoute == "home") {
                FloatingActionButton(
                    onClick = { navController.navigate("task/-1") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "新建任务")
                }
            }
        }
    ) { padding ->
        NavGraph(navController = navController, modifier = Modifier.padding(padding))
    }
}

data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)
