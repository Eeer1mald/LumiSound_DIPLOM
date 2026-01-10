package com.example.lumisound.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Скругления согласно дизайну: 16-20dp для карточек и кнопок
val Shapes = Shapes(
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(20.dp)
)

// Дополнительные формы для компонентов
val ButtonShape = RoundedCornerShape(18.dp)
val CardShape = RoundedCornerShape(20.dp)
val TextFieldShape = RoundedCornerShape(16.dp)