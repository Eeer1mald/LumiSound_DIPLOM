package com.example.lumisound.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Динамические цвета — меняются при смене темы.
 * Читай через LocalAppColors.current в @Composable функциях.
 */
data class AppColors(
    val background: Color,
    val surface: Color,
    val onBackground: Color,
    val secondary: Color,
    val onSurface: Color,
    val isDark: Boolean
)

val DarkAppColors = AppColors(
    background   = ColorBackgroundDark,
    surface      = ColorSurfaceDark,
    onBackground = ColorOnBackgroundDark,
    secondary    = ColorSecondaryDark,
    onSurface    = ColorSecondaryDark,
    isDark       = true
)

val LightAppColors = AppColors(
    background   = ColorBackgroundLight,
    surface      = ColorSurfaceLight,
    onBackground = ColorOnBackgroundLight,
    secondary    = ColorSecondaryLight,
    onSurface    = ColorSecondaryLight,
    isDark       = false
)

// compositionLocalOf (не static) — позволяет обновлять значение при рекомпозиции
val LocalAppColors = compositionLocalOf<AppColors> { DarkAppColors }
