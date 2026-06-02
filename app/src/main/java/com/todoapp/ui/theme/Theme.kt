package com.todoapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = ClaudePrimary,
    onPrimary = Color.White,
    primaryContainer = ClaudeLightSurfaceVar,
    onPrimaryContainer = ClaudeLightOnBg,
    secondary = ClaudeLightSecondary,
    onSecondary = Color.White,
    background = ClaudeLightBg,
    onBackground = ClaudeLightOnBg,
    surface = ClaudeLightSurface,
    onSurface = ClaudeLightOnSurface,
    surfaceVariant = ClaudeLightSurfaceVar,
    onSurfaceVariant = ClaudeLightSecondary,
    outline = ClaudeLightOutline,
)

private val DarkColorScheme = darkColorScheme(
    primary = ClaudePrimaryDark,
    onPrimary = ClaudeDarkBg,
    primaryContainer = ClaudeDarkSurfaceVar,
    onPrimaryContainer = ClaudeDarkOnBg,
    secondary = ClaudeDarkSecondary,
    onSecondary = ClaudeDarkBg,
    background = ClaudeDarkBg,
    onBackground = ClaudeDarkOnBg,
    surface = ClaudeDarkSurface,
    onSurface = ClaudeDarkOnSurface,
    surfaceVariant = ClaudeDarkSurfaceVar,
    onSurfaceVariant = ClaudeDarkSecondary,
    outline = ClaudeDarkOutline,
)

/**
 * @param forceDarkTheme  null = follow system, true = dark, false = light
 */
@Composable
fun TodoAppTheme(
    forceDarkTheme: Boolean? = null,
    content: @Composable () -> Unit
) {
    val darkTheme = forceDarkTheme ?: isSystemInDarkTheme()
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
