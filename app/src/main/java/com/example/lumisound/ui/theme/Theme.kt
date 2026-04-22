package com.example.lumisound.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

internal val DarkColorScheme = darkColorScheme(
    primary      = ColorPrimary,
    secondary    = ColorSecondaryDark,
    tertiary     = ColorAccentSecondary,
    background   = ColorBackgroundDark,
    surface      = ColorSurfaceDark,
    onPrimary    = ColorOnPrimary,
    onSecondary  = ColorOnPrimary,
    onTertiary   = ColorOnPrimary,
    onBackground = ColorOnBackgroundDark,
    onSurface    = ColorSecondaryDark,
    error        = ColorError,
    onError      = ColorOnPrimary
)

internal val LightColorScheme = lightColorScheme(
    primary      = ColorPrimary,
    secondary    = ColorSecondaryLight,
    tertiary     = ColorAccentSecondary,
    background   = ColorBackgroundLight,
    surface      = ColorSurfaceLight,
    onPrimary    = ColorOnPrimary,
    onSecondary  = ColorOnPrimary,
    onTertiary   = ColorOnPrimary,
    onBackground = ColorOnBackgroundLight,
    onSurface    = ColorSecondaryLight,
    error        = ColorError,
    onError      = ColorOnPrimary
)

@Composable
fun LumiSoundTheme(
    themeMode: String = "dark",
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "light"  -> false
        "system" -> systemDark
        else     -> true
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val appColors   = if (isDark) DarkAppColors   else LightAppColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars     = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = Typography,
            shapes      = Shapes,
            content     = content
        )
    }
}
