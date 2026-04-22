package com.example.lumisound.feature.auth.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lumisound.ui.theme.ColorAccentSecondary
import com.example.lumisound.ui.theme.ColorError
import com.example.lumisound.ui.theme.LumiSoundTheme
import com.example.lumisound.ui.theme.LocalAppColors
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush

@Composable
fun LabeledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isError: Boolean,
    errorText: String?,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    visualTransformation: VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    // Цвета согласно дизайну Figma - точные значения
    val textColor = Color(0xFFE6E6EB) // Основной текст - светлый серый
    val placeholderColor = Color(0xFF9A9AB0) // Placeholder - серый
    val containerColor = Color(0xFF1A1B2E).copy(alpha = 0.8f) // Фон поля - темный индиго
    val borderColorUnfocused = Color(0xFF2A2D3E).copy(alpha = 0.6f) // Граница неактивная
    val borderColorFocused = ColorAccentSecondary.copy(alpha = 0.8f) // Граница активная - фиолетовый
    
    Column(modifier = modifier) {
        // Принудительно устанавливаем цвет и стиль контента для всего поля
        CompositionLocalProvider(
            LocalContentColor provides textColor,
            LocalTextStyle provides TextStyle(
                color = textColor,
                fontSize = 16.sp
            )
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                // Используем label с анимацией, но с правильными цветами
                label = { 
                    Text(
                        text = label, 
                        fontSize = 14.sp,
                        color = if (isError) ColorError else Color(0xFF9A9AB0)
                    ) 
                },
                placeholder = { 
                    Text(
                        text = placeholder, 
                        color = placeholderColor,
                        fontSize = 16.sp
                    ) 
                },
                singleLine = true,
                isError = isError,
                visualTransformation = visualTransformation,
                trailingIcon = trailingIcon,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp) // Добавляем padding сверху, чтобы label не перекрывал контент
                    .height(72.dp)
                    .shadow(
                        elevation = if (isError) 0.dp else 2.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = if (isError) ColorError else ColorAccentSecondary.copy(alpha = 0.3f)
                    )
                    .then(if (testTag != null) Modifier.semantics { contentDescription = testTag } else Modifier),
                shape = RoundedCornerShape(16.dp),
                // Принудительно устанавливаем стиль текста - это критично!
                textStyle = TextStyle(
                    color = textColor,
                    fontSize = 16.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    // Границы
                    focusedBorderColor = if (isError) ColorError else borderColorFocused,
                    unfocusedBorderColor = if (isError) ColorError else borderColorUnfocused,
                    errorBorderColor = ColorError,
                    disabledBorderColor = borderColorUnfocused,
                    
                    // Лейблы - правильные цвета для анимации
                    focusedLabelColor = if (isError) ColorError else ColorAccentSecondary,
                    unfocusedLabelColor = if (isError) ColorError else Color(0xFF9A9AB0),
                    errorLabelColor = ColorError,
                    disabledLabelColor = Color(0xFF9A9AB0),
                    
                    // Текст ввода - КРИТИЧНО! Принудительно устанавливаем цвет
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    errorTextColor = textColor,
                    disabledTextColor = textColor.copy(alpha = 0.6f),
                    
                    // Курсор
                    cursorColor = ColorAccentSecondary,
                    
                    // Placeholder - принудительно устанавливаем цвет
                    focusedPlaceholderColor = placeholderColor,
                    unfocusedPlaceholderColor = placeholderColor,
                    errorPlaceholderColor = placeholderColor,
                    disabledPlaceholderColor = placeholderColor.copy(alpha = 0.5f),
                    
                    // Фон контейнера - принудительно устанавливаем цвет
                    focusedContainerColor = containerColor,
                    unfocusedContainerColor = containerColor,
                    errorContainerColor = containerColor,
                    disabledContainerColor = containerColor.copy(alpha = 0.5f),
                    
                    // Иконки
                    focusedTrailingIconColor = Color(0xFF9A9AB0),
                    unfocusedTrailingIconColor = Color(0xFF9A9AB0),
                    errorTrailingIconColor = ColorError,
                    disabledTrailingIconColor = Color(0xFF9A9AB0).copy(alpha = 0.5f),
                    
                    focusedLeadingIconColor = Color(0xFF9A9AB0),
                    unfocusedLeadingIconColor = Color(0xFF9A9AB0),
                    errorLeadingIconColor = ColorError,
                    disabledLeadingIconColor = Color(0xFF9A9AB0).copy(alpha = 0.5f)
                )
            )
        }
        AnimatedVisibility(
            visible = errorText != null,
            enter = expandVertically(animationSpec = tween(150)) + fadeIn(animationSpec = tween(150)), // Уменьшено в 2 раза
            exit = shrinkVertically(animationSpec = tween(150)) + fadeOut(animationSpec = tween(150)) // Уменьшено в 2 раза
        ) {
            if (errorText != null) {
                Text(
                    text = errorText,
                    color = ColorError,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        }
    }
}

@Preview(showBackground = false, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LabeledTextFieldPreview() {
    LumiSoundTheme {
        LabeledTextField(
            value = "",
            onValueChange = {},
            label = "Email",
            placeholder = "Введите email",
            isError = false,
            errorText = null,
            keyboardOptions = KeyboardOptions.Default,
            keyboardActions = KeyboardActions.Default,
            modifier = Modifier.padding(16.dp)
        )
    }
}

