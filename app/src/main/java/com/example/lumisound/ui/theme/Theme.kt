package com.example.lumisound.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ColorPrimary,
    secondary = ColorSecondary,
    tertiary = ColorAccentSecondary,
    background = ColorBackground,
    surface = ColorSurface,
    onPrimary = ColorOnPrimary,
    onSecondary = ColorOnPrimary,
    onTertiary = ColorOnPrimary,
    onBackground = ColorOnBackground,
    onSurface = ColorOnSurface,
    error = ColorError,
    onError = ColorOnPrimary
)

@Composable
fun LumiSoundTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Отключаем dynamic color для строгого брендинга
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Используем новый API для установки цветов системных баров
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
