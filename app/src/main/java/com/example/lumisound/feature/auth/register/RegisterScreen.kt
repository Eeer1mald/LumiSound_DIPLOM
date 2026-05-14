package com.example.lumisound.feature.auth.register

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.lumisound.feature.auth.components.GradientButton
import com.example.lumisound.feature.auth.login.AuthTextField
import com.example.lumisound.ui.theme.GradientEnd
import com.example.lumisound.ui.theme.GradientStart

@Composable
fun RegisterScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToProfileSetup: () -> Unit,
    onNavigateToVerifyEmail: () -> Unit = {},
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val username = rememberSaveable { mutableStateOf("") }
    val email = rememberSaveable { mutableStateOf("") }
    val password = rememberSaveable { mutableStateOf("") }
    val confirmPassword = rememberSaveable { mutableStateOf("") }
    val showPassword = rememberSaveable { mutableStateOf(false) }
    val showConfirmPassword = rememberSaveable { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showTerms by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    BackHandler(onBack = onNavigateBack)

    // Диалог пользовательского соглашения
    if (showTerms) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTerms = false },
            title = {
                Text(
                    "Условия использования",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                val scroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(scroll),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        TermsSection(
                            title = "Авторские права на музыку",
                            text = "Все треки, исполнители и обложки предоставлены платформой Audius. " +
                                "Авторские права принадлежат соответствующим правообладателям. " +
                                "LumiSound использует открытый API Audius в некоммерческих целях."
                        )
                        TermsSection(
                            title = "Политика конфиденциальности",
                            text = "LumiSound собирает минимальные данные: email, имя пользователя и аватар. " +
                                "Данные хранятся в защищённой базе Supabase и не передаются третьим лицам."
                        )
                        TermsSection(
                            title = "Правила сообщества",
                            text = "Запрещены оскорбления, спам и нарушение авторских прав. " +
                                "Нарушение правил может привести к блокировке аккаунта."
                        )
                        TermsSection(
                            title = "Ограничение ответственности",
                            text = "Приложение предоставляется «как есть». " +
                                "LumiSound не несёт ответственности за контент третьих сторон."
                        )
                        Text(
                            "© 2025 LumiSound. Все права защищены.",
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 11.sp
                        )
                    }
            },
            confirmButton = {
                TextButton(onClick = { showTerms = false }) {
                    Text("Понятно", color = GradientStart, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = Color(0xFF1A1A2E)
        )
    }

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            isLoading = false
            when (effect) {
                is RegisterSideEffect.NavigateToProfileSetup -> onNavigateToProfileSetup()
                is RegisterSideEffect.NavigateToHome -> onNavigateToHome()
                is RegisterSideEffect.NavigateToLogin -> onNavigateToLogin()
                is RegisterSideEffect.NavigateToVerifyEmail -> onNavigateToVerifyEmail()
                is RegisterSideEffect.ShowSnackbar -> errorMessage = effect.message
            }
        }
    }

    val passwordTooShort = password.value.isNotEmpty() && password.value.length < 8
    val passwordHasLetter = password.value.any { it.isLetter() }
    val passwordHasDigit = password.value.any { it.isDigit() }
    val passwordValid = password.value.isEmpty() || (password.value.length >= 8 && passwordHasLetter && passwordHasDigit)
    val passwordError = when {
        password.value.isEmpty() -> null
        password.value.length < 8 -> "Минимум 8 символов"
        !passwordHasLetter -> "Добавьте хотя бы одну букву"
        !passwordHasDigit -> "Добавьте хотя бы одну цифру"
        else -> null
    }
    val passwordsMatch = password.value == confirmPassword.value
    val showPasswordMismatch = confirmPassword.value.isNotEmpty() && !passwordsMatch
    val isEmailValid = email.value.contains("@") && email.value.contains(".")
    val canRegister = username.value.isNotBlank() &&
        email.value.isNotBlank() && isEmailValid &&
        passwordValid && password.value.isNotEmpty() &&
        passwordsMatch && confirmPassword.value.isNotEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0B1A))
            .imePadding()
    ) {
        Box(
            modifier = Modifier
                .size(260.dp)
                .offset(x = (-60).dp, y = (-40).dp)
                .background(
                    Brush.radialGradient(listOf(GradientStart.copy(alpha = 0.18f), Color.Transparent)),
                    CircleShape
                )
                .blur(50.dp)
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 40.dp, y = 40.dp)
                .background(
                    Brush.radialGradient(listOf(GradientEnd.copy(alpha = 0.15f), Color.Transparent)),
                    CircleShape
                )
                .blur(40.dp)
        )

        // Закреплённая ссылка внизу с разделителем
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.1f))
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onNavigateToLogin() }
            ) {
                Text("Уже есть аккаунт? ", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp)
                Text("Войти", color = GradientStart, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 28.dp)
                .padding(bottom = 56.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

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
                modifier = Modifier.fillMaxWidth(0.8f)
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Присоединяйтесь к LumiSound", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(28.dp))

            AuthTextField(
                value = username.value,
                onValueChange = { username.value = it; errorMessage = null },
                label = "Имя пользователя",
                placeholder = "Как вас называть?",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )

            Spacer(modifier = Modifier.height(14.dp))

            AuthTextField(
                value = email.value,
                onValueChange = { email.value = it; errorMessage = null },
                label = "Email",
                placeholder = "your@email.com",
                isError = email.value.isNotEmpty() && !isEmailValid,
                errorText = if (email.value.isNotEmpty() && !isEmailValid) "Введите корректный email" else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )

            Spacer(modifier = Modifier.height(14.dp))

            AuthTextField(
                value = password.value,
                onValueChange = { password.value = it; errorMessage = null },
                label = "Пароль",
                placeholder = "Введите пароль",
                isError = passwordError != null,
                errorText = passwordError,
                visualTransformation = if (showPassword.value) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                trailingIcon = {
                    IconButton(onClick = { showPassword.value = !showPassword.value }) {
                        Icon(
                            if (showPassword.value) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )

            // Индикатор сложности пароля
            if (password.value.isNotEmpty()) {
                val strength = when {
                    password.value.length >= 8 && passwordHasLetter && passwordHasDigit &&
                        password.value.any { !it.isLetterOrDigit() } -> 3 // надёжный
                    password.value.length >= 8 && passwordHasLetter && passwordHasDigit -> 2 // хороший
                    else -> 1 // слабый
                }
                val strengthLabel = when (strength) {
                    3 -> "Надёжный"
                    2 -> "Хороший"
                    else -> "Слабый"
                }
                val strengthColor = when (strength) {
                    3 -> Color(0xFF4CAF50)
                    2 -> Color(0xFFFF9800)
                    else -> Color(0xFFFF5C6C)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { index ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .background(
                                    if (index < strength) strengthColor else Color.White.copy(alpha = 0.12f),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strengthLabel, color = strengthColor, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                // Подсказки что ещё нужно
                if (!passwordHasLetter || !passwordHasDigit || password.value.length < 8) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (password.value.length < 8)
                            Text("• Минимум 8 символов", color = Color.White.copy(alpha = 0.35f), fontSize = 11.sp)
                        if (!passwordHasLetter)
                            Text("• Добавьте буквы", color = Color.White.copy(alpha = 0.35f), fontSize = 11.sp)
                        if (!passwordHasDigit)
                            Text("• Добавьте цифры", color = Color.White.copy(alpha = 0.35f), fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            AuthTextField(
                value = confirmPassword.value,
                onValueChange = { confirmPassword.value = it; errorMessage = null },
                label = "Подтвердите пароль",
                placeholder = "Повторите пароль",
                isError = showPasswordMismatch,
                errorText = if (showPasswordMismatch) "Пароли не совпадают" else null,
                visualTransformation = if (showConfirmPassword.value) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    if (canRegister) {
                        isLoading = true
                        errorMessage = null
                        viewModel.signUpCreateProfileAndLogin(username.value.trim(), email.value.trim(), password.value)
                    }
                }),
                trailingIcon = {
                    IconButton(onClick = { showConfirmPassword.value = !showConfirmPassword.value }) {
                        Icon(
                            if (showConfirmPassword.value) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            AnimatedVisibility(visible = errorMessage != null, enter = fadeIn(tween(200)), exit = fadeOut(tween(150))) {
                if (errorMessage != null) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFF5C6C).copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFFF5C6C).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, null, tint = Color(0xFFFF5C6C), modifier = Modifier.size(16.dp))
                            Text(errorMessage!!, color = Color(0xFFFF5C6C), fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            GradientButton(
                text = if (isLoading) "" else "Создать аккаунт",
                enabled = canRegister && !isLoading,
                onClick = {
                    focusManager.clearFocus()
                    isLoading = true
                    errorMessage = null
                    viewModel.signUpCreateProfileAndLogin(username.value.trim(), email.value.trim(), password.value)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Пользовательское соглашение
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Регистрируясь, вы принимаете ", color = Color.White.copy(alpha = 0.35f), fontSize = 11.sp)
                Text(
                    "условия использования",
                    color = GradientStart.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showTerms = true }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TermsSection(title: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            color = GradientStart,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}
