package com.example.lumisound.feature.auth.login

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
import com.example.lumisound.ui.theme.GradientEnd
import com.example.lumisound.ui.theme.GradientStart

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit = {},
    onNavigateToProfileSetup: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {},
    onNavigateToForgot: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    val isSubmitting = uiState is LoginUiState.Submitting

    val email = when (val s = uiState) {
        is LoginUiState.Idle -> s.email
        is LoginUiState.InputChanged -> s.email
        is LoginUiState.Submitting -> s.email
        is LoginUiState.Error -> s.email
        is LoginUiState.Success -> s.email
    }
    val password = when (val s = uiState) {
        is LoginUiState.Idle -> s.password
        is LoginUiState.InputChanged -> s.password
        is LoginUiState.Submitting -> s.password
        is LoginUiState.Error -> s.password
        is LoginUiState.Success -> s.password
    }
    val isPasswordVisible = when (val s = uiState) {
        is LoginUiState.Idle -> s.isPasswordVisible
        is LoginUiState.InputChanged -> s.isPasswordVisible
        is LoginUiState.Submitting -> s.isPasswordVisible
        is LoginUiState.Error -> s.isPasswordVisible
        is LoginUiState.Success -> false
    }
    val emailError = when (val s = uiState) {
        is LoginUiState.InputChanged -> s.emailError
        is LoginUiState.Error -> s.emailError
        else -> null
    }
    val passwordError = when (val s = uiState) {
        is LoginUiState.InputChanged -> s.passwordError
        is LoginUiState.Error -> s.passwordError
        else -> null
    }
    val globalError = when (val s = uiState) {
        is LoginUiState.Error -> if (s.emailError == null && s.passwordError == null) s.errorMessage else null
        else -> null
    }

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is LoginSideEffect.NavigateToHome -> onNavigateToHome()
                is LoginSideEffect.NavigateToProfileSetup -> onNavigateToProfileSetup()
                is LoginSideEffect.ShowSnackbar -> { /* handled inline */ }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0B1A))
            .imePadding()
    ) {
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
                ) { onNavigateToRegister() }
            ) {
                Text("Нет аккаунта? ", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp)
                Text("Зарегистрироваться", color = GradientStart, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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
            Spacer(modifier = Modifier.height(48.dp))

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
                modifier = Modifier.fillMaxWidth(0.9f)
            )

            Spacer(modifier = Modifier.height(20.dp))
            Text("Войдите в свой аккаунт", color = Color.White.copy(alpha = 0.45f), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(36.dp))

            AuthTextField(
                value = email,
                onValueChange = { viewModel.handleAction(LoginUiAction.EmailChanged(it)) },
                label = "Email",
                placeholder = "your@email.com",
                isError = emailError != null,
                errorText = emailError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )

            Spacer(modifier = Modifier.height(14.dp))

            AuthTextField(
                value = password,
                onValueChange = { viewModel.handleAction(LoginUiAction.PasswordChanged(it)) },
                label = "Пароль",
                placeholder = "Введите пароль",
                isError = passwordError != null,
                errorText = passwordError,
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    viewModel.handleAction(LoginUiAction.Submit)
                }),
                trailingIcon = {
                    IconButton(onClick = { viewModel.handleAction(LoginUiAction.TogglePasswordVisibility) }) {
                        Icon(
                            if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )

            Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.CenterEnd) {
                Text(
                    "Забыли пароль?",
                    color = GradientStart,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onNavigateToForgot() }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            AnimatedVisibility(visible = globalError != null, enter = fadeIn(tween(200)), exit = fadeOut(tween(150))) {
                if (globalError != null) {
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
                            Text(globalError, color = Color(0xFFFF5C6C), fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            val isValid = emailError == null && passwordError == null && email.isNotEmpty() && password.isNotEmpty()
            GradientButton(
                text = if (isSubmitting) "" else "Войти",
                enabled = isValid && !isSubmitting,
                onClick = {
                    focusManager.clearFocus()
                    viewModel.handleAction(LoginUiAction.Submit)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isError: Boolean = false,
    errorText: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color.White.copy(alpha = 0.25f), fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = isError,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            trailingIcon = trailingIcon,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GradientStart.copy(alpha = 0.7f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                errorBorderColor = Color(0xFFFF5C6C).copy(alpha = 0.7f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                errorTextColor = Color.White,
                cursorColor = GradientStart,
                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                errorContainerColor = Color(0xFFFF5C6C).copy(alpha = 0.05f)
            )
        )
        if (isError && errorText != null) {
            Text(errorText, color = Color(0xFFFF5C6C), fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp))
        }
    }
}
