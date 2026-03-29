package com.example.lumisound.ui.theme

import androidx.compose.ui.graphics.Color

// LumiSound Brand Palette (Premium Dark Theme - Monochrome Black)
// Основной фон: чистый черный или очень темно-серый
val ColorBackground = Color(0xFF000000) // #000000 - чистый черный

// Основной акцент (кнопки): однотонный акцентный цвет (ранее был градиент)
// Используем средний цвет из градиента #7B6DFF → #FF5C6C для единообразия
val GradientStart = Color(0xFFB93FD9)  // Средний между фиолетовым и коралловым (для совместимости)
val GradientEnd = Color(0xFFB93FD9)    // Тот же цвет вместо градиента

// Основной текст
val ColorOnBackground = Color(0xFFE6E6EB)
val ColorOnPrimary = Color(0xFFFFFFFF)

// Вторичный текст
val ColorSecondary = Color(0xFF9A9AB0)
val ColorOnSurface = Color(0xFF9A9AB0)

// Акценты (сохраняем для кнопок, но используем однотонный)
val ColorPrimary = Color(0xFFB93FD9)  // Средний акцентный цвет
val ColorAccentSecondary = Color(0xFFB93FD9)  // Тот же для единообразия

// Поверхности (темные карточки) - немного светлее основного фона
val ColorSurface = Color(0xFF121212) // #121212 - темно-серый для карточек

// Error colors
val ColorError = Color(0xFFFF5C6C)
