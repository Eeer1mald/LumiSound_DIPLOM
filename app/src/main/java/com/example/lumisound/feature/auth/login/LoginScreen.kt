package com.example.lumisound.feature.auth.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.example.lumisound.R
import com.example.lumisound.feature.auth.components.GradientButton
import com.example.lumisound.feature.auth.components.LabeledTextField
import com.example.lumisound.ui.theme.ColorAccentSecondary
import com.example.lumisound.ui.theme.ColorBackground
import com.example.lumisound.ui.theme.ColorOnBackground
import com.example.lumisound.ui.theme.ColorSecondary
import com.example.lumisound.ui.theme.LumiSoundTheme
import com.example.lumisound.ui.theme.Typography

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit = {},
    onNavigateToProfileSetup: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {},
    onNavigateToForgot: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Google Sign-In setup - безопасная инициализация для устройств без Google Services
    val googleSignInClient: GoogleSignInClient? = remember {
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("720567253667-nseep0m3eahgo2gpe7dr0uvrnp3k35iu.apps.googleusercontent.com")
                .requestEmail()
                .build()
            GoogleSignIn.getClient(context, gso)
        } catch (e: Exception) {
            // Google Services недоступны (например, на Huawei без GMS)
            null
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (googleSignInClient == null) {
            // Google Sign-In недоступен на этом устройстве
            return@rememberLauncherForActivityResult
        }
        
        coroutineScope.launch {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account: GoogleSignInAccount? = task.getResult(ApiException::class.java)
                val idToken = account?.idToken ?: ""
                if (idToken.isNotEmpty()) {
                    viewModel.handleAction(LoginUiAction.GoogleSignIn(idToken))
                } else {
                    snackbarHostState.showSnackbar("Не удалось получить токен Google")
                }
            } catch (e: ApiException) {
                snackbarHostState.showSnackbar("Ошибка входа через Google: ${e.message ?: "Неизвестная ошибка"}")
            } catch (e: Exception) {
                // Обработка других исключений (например, когда Google Services недоступны)
                snackbarHostState.showSnackbar("Google Sign-In недоступен на этом устройстве")
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is LoginSideEffect.NavigateToHome -> onNavigateToHome()
                is LoginSideEffect.NavigateToProfileSetup -> onNavigateToProfileSetup()
                is LoginSideEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    val currentState = uiState
    val isSubmitting = currentState is LoginUiState.Submitting
    
    // Animation states - все сразу видимо
    var showLogo by remember { mutableStateOf(true) }
    var showFields by remember { mutableStateOf(true) }
    var showButton by remember { mutableStateOf(true) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = remember {
                    Brush.verticalGradient(
                        colors = listOf(
                            ColorBackground,
                            Color(0xFF0A0B1A),
                            ColorBackground
                        )
                    )
                }
            ),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(ColorBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(top = paddingValues.calculateTopPadding() + 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .weight(1f, fill = false),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                Spacer(modifier = Modifier.height(48.dp))

                // Logo with animation
                AnimatedVisibility(
                    visible = showLogo,
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(150))
                ) {
                    val logoId = remember { context.resources.getIdentifier("logo", "drawable", context.packageName) }
                    val logoPlaceholderId = remember {
                        context.resources.getIdentifier("ic_logo_foreground", "drawable", context.packageName)
                    }
                    Image(
                        painter = painterResource(
                            id = when {
                                logoId != 0 -> logoId
                                logoPlaceholderId != 0 -> logoPlaceholderId
                                else -> android.R.drawable.ic_menu_gallery
                            }
                        ),
                        contentDescription = "LumiSound logo",
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Email field with animation
                AnimatedVisibility(
                    visible = showFields,
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(150))
                ) {
                    Column {
                        val email = when (currentState) {
                            is LoginUiState.Idle -> currentState.email
                            is LoginUiState.InputChanged -> currentState.email
                            is LoginUiState.Submitting -> currentState.email
                            is LoginUiState.Error -> currentState.email
                            is LoginUiState.Success -> currentState.email
                        }
                        val emailError = when (currentState) {
                            is LoginUiState.Idle -> currentState.emailError
                            is LoginUiState.InputChanged -> currentState.emailError
                            is LoginUiState.Submitting -> null
                            is LoginUiState.Error -> currentState.emailError
                            is LoginUiState.Success -> null
                        }

                        LabeledTextField(
                            value = email,
                            onValueChange = { viewModel.handleAction(LoginUiAction.EmailChanged(it)) },
                            label = stringResource(R.string.email),
                            placeholder = stringResource(R.string.email),
                            isError = emailError != null,
                            errorText = emailError,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "login_email" },
                            testTag = "login_email"
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Password field
                        val password = when (currentState) {
                            is LoginUiState.Idle -> currentState.password
                            is LoginUiState.InputChanged -> currentState.password
                            is LoginUiState.Submitting -> currentState.password
                            is LoginUiState.Error -> currentState.password
                            is LoginUiState.Success -> currentState.password
                        }
                        val isPasswordVisible = when (currentState) {
                            is LoginUiState.Idle -> currentState.isPasswordVisible
                            is LoginUiState.InputChanged -> currentState.isPasswordVisible
                            is LoginUiState.Submitting -> currentState.isPasswordVisible
                            is LoginUiState.Error -> currentState.isPasswordVisible
                            is LoginUiState.Success -> false
                        }
                        val passwordError = when (currentState) {
                            is LoginUiState.Idle -> currentState.passwordError
                            is LoginUiState.InputChanged -> currentState.passwordError
                            is LoginUiState.Submitting -> null
                            is LoginUiState.Error -> currentState.passwordError
                            is LoginUiState.Success -> null
                        }

                        LabeledTextField(
                            value = password,
                            onValueChange = { viewModel.handleAction(LoginUiAction.PasswordChanged(it)) },
                            label = stringResource(R.string.password),
                            placeholder = stringResource(R.string.password),
                            isError = passwordError != null,
                            errorText = passwordError,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { viewModel.handleAction(LoginUiAction.Submit) }
                            ),
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(
                                    onClick = { viewModel.handleAction(LoginUiAction.TogglePasswordVisibility) },
                                    modifier = Modifier.semantics { contentDescription = "Toggle password visibility" }
                                ) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "login_password" },
                            testTag = "login_password"
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Forgot password link
                        Text(
                            text = stringResource(R.string.forgot_password),
                            color = ColorAccentSecondary,
                            style = Typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.End)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { viewModel.handleAction(LoginUiAction.GoToForgot) }
                                .semantics { contentDescription = "login_forgot" },
                            fontSize = 14.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Spacer(modifier = Modifier.height(24.dp))

                // Submit button with animation
                AnimatedVisibility(
                    visible = showButton,
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(150))
                ) {
                    val email = when (currentState) {
                        is LoginUiState.Idle -> currentState.email
                        is LoginUiState.InputChanged -> currentState.email
                        is LoginUiState.Submitting -> currentState.email
                        is LoginUiState.Error -> currentState.email
                        is LoginUiState.Success -> currentState.email
                    }
                    val password = when (currentState) {
                        is LoginUiState.Idle -> currentState.password
                        is LoginUiState.InputChanged -> currentState.password
                        is LoginUiState.Submitting -> currentState.password
                        is LoginUiState.Error -> currentState.password
                        is LoginUiState.Success -> currentState.password
                    }
                    val emailError = when (currentState) {
                        is LoginUiState.Idle -> currentState.emailError
                        is LoginUiState.InputChanged -> currentState.emailError
                        is LoginUiState.Submitting -> null
                        is LoginUiState.Error -> currentState.emailError
                        is LoginUiState.Success -> null
                    }
                    val passwordError = when (currentState) {
                        is LoginUiState.Idle -> currentState.passwordError
                        is LoginUiState.InputChanged -> currentState.passwordError
                        is LoginUiState.Submitting -> null
                        is LoginUiState.Error -> currentState.passwordError
                        is LoginUiState.Success -> null
                    }
                    val isValid = emailError == null && passwordError == null && email.isNotEmpty() && password.isNotEmpty()

                    GradientButton(
                        text = if (isSubmitting) "" else stringResource(R.string.login),
                        enabled = isValid && !isSubmitting,
                        onClick = { viewModel.handleAction(LoginUiAction.Submit) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "login_submit" },
                        testTag = "login_submit"
                    )
                }

                    Spacer(modifier = Modifier.height(32.dp))
                }
                
                // Register link - всегда внизу, не скроллится
                Row(
                    modifier = Modifier
                        .padding(bottom = paddingValues.calculateBottomPadding() + 16.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onNavigateToRegister() }
                        .semantics { contentDescription = "login_register" },
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_account),
                        color = ColorSecondary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = " ${stringResource(R.string.register)}",
                        color = ColorAccentSecondary,
                        fontSize = 14.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                }
            }

            // Loading overlay
            AnimatedVisibility(
                visible = isSubmitting,
                enter = fadeIn(animationSpec = tween(150)), // Уменьшено в 2 раза
                exit = fadeOut(animationSpec = tween(150)) // Уменьшено в 2 раза
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ColorBackground.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ColorSecondary)
                }
            }
        }
    }
}

@Preview(showBackground = false, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoginScreenPreview() {
    LumiSoundTheme {
        LoginScreen()
    }
}

