package com.example.lumisound.feature.auth.forgotpassword

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.lumisound.feature.auth.login.AuthTextField
import com.example.lumisound.feature.auth.components.GradientButton
import com.example.lumisound.ui.theme.GradientEnd
import com.example.lumisound.ui.theme.GradientStart

@Composable
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }

    val isLoading = state is ForgotPasswordState.Loading
    val isSuccess = state is ForgotPasswordState.Success
    val errorMessage = (state as? ForgotPasswordState.Error)?.message

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0B1A))
            .imePadding()
    ) {
        // Декоративные размытые круги
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(x = (-70).dp, y = (-50).dp)
                .background(
                    Brush.radialGradient(listOf(GradientStart.copy(alpha = 0.2f), Color.Transparent)),
                    CircleShape
                )
                .blur(50.dp)
        )
        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 50.dp, y = 50.dp)
                .background(
                    Brush.radialGradient(listOf(GradientEnd.copy(alpha = 0.15f), Color.Transparent)),
                    CircleShape
                )
                .blur(40.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 28.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Кнопка назад
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Назад",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Логотип
            val logoId = remember { context.resources.getIdentifier("logo", "drawable", context.packageName) }
            val placeholderId = remember { context.resources.getIdentifier("ic_logo_foreground", "drawable", context.packageName) }
            Image(
                painter = painterResource(
                    id = when {
                        logoId != 0 -> logoId
                        placeholderId != 0 -> placeholderId
                        else -> android.R.drawable.ic_menu_gallery
                    }
                ),
                contentDescription = "LumiSound",
                modifier = Modifier.fillMaxWidth(0.7f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (!isSuccess) {
                // Иконка письма
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(GradientStart.copy(alpha = 0.2f), GradientEnd.copy(alpha = 0.1f))
                            ),
                            CircleShape
                        )
                        .border(1.dp, GradientStart.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = null,
                        tint = GradientStart,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    "Восстановление пароля",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Введите email, указанный при регистрации.\nМы отправим ссылку для сброса пароля.",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                AuthTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        if (state is ForgotPasswordState.Error) viewModel.resetState()
                    },
                    label = "Email",
                    placeholder = "your@email.com",
                    isError = errorMessage != null,
                    errorText = errorMessage,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        viewModel.sendResetEmail(email)
                    })
                )

                Spacer(modifier = Modifier.height(24.dp))

                GradientButton(
                    text = if (isLoading) "" else "Отправить письмо",
                    enabled = email.isNotBlank() && !isLoading,
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.sendResetEmail(email)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onNavigateBack() }
                ) {
                    Text("Вспомнили пароль? ", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp)
                    Text("Войти", color = GradientStart, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

            } else {
                // Экран успеха
                val successEmail = (state as ForgotPasswordState.Success).email

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFF4CAF50).copy(alpha = 0.2f), Color.Transparent)
                            ),
                            CircleShape
                        )
                        .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "Письмо отправлено!",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "Мы отправили ссылку для сброса пароля на",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    successEmail,
                    color = GradientStart,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Проверьте папку «Спам», если письмо не пришло.",
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(36.dp))

                GradientButton(
                    text = "Вернуться ко входу",
                    enabled = true,
                    onClick = onNavigateBack,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
