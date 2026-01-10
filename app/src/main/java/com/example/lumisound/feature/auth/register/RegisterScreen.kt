package com.example.lumisound.feature.auth.register

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.lumisound.R
import com.example.lumisound.feature.auth.components.GradientButton
import com.example.lumisound.feature.auth.components.LabeledTextField
import com.example.lumisound.ui.theme.ColorAccentSecondary
import com.example.lumisound.ui.theme.ColorBackground
import com.example.lumisound.ui.theme.ColorOnBackground

@Composable
fun RegisterScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToProfileSetup: () -> Unit,
    onNavigateToVerifyEmail: () -> Unit = {},
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val usernameState = rememberSaveable { mutableStateOf("") }
    val emailState = rememberSaveable { mutableStateOf("") }
    val passwordState = rememberSaveable { mutableStateOf("") }
    val confirmPasswordState = rememberSaveable { mutableStateOf("") }
    val passwordVisibility = rememberSaveable { mutableStateOf(false) }
    val confirmPasswordVisibility = rememberSaveable { mutableStateOf(false) }
    val acceptedTerms = rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    BackHandler(onBack = onNavigateBack)

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is RegisterSideEffect.NavigateToProfileSetup -> onNavigateToProfileSetup()
                is RegisterSideEffect.NavigateToHome -> onNavigateToHome()
                is RegisterSideEffect.NavigateToLogin -> onNavigateToLogin()
                is RegisterSideEffect.NavigateToVerifyEmail -> onNavigateToVerifyEmail()
                is RegisterSideEffect.ShowSnackbar -> {
                    scope.launch { snackbarHostState.showSnackbar(effect.message) }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.ime
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
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
        Spacer(modifier = Modifier.height(8.dp))

        val context = LocalContext.current
        val logoId = remember { context.resources.getIdentifier("logo", "drawable", context.packageName) }
        val placeholderId = remember { context.resources.getIdentifier("ic_logo_foreground", "drawable", context.packageName) }

        androidx.compose.foundation.Image(
            painter = painterResource(
                id = when {
                    logoId != 0 -> logoId
                    placeholderId != 0 -> placeholderId
                    else -> android.R.drawable.ic_menu_gallery
                }
            ),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier
                .size(280.dp)
                .clip(RoundedCornerShape(20.dp))
        )

        Spacer(modifier = Modifier.height(12.dp))

        LabeledTextField(
            value = usernameState.value,
            onValueChange = { usernameState.value = it },
            label = stringResource(R.string.username),
            placeholder = stringResource(R.string.username_placeholder),
            isError = false,
            errorText = null,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
            keyboardActions = androidx.compose.foundation.text.KeyboardActions.Default,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        LabeledTextField(
            value = emailState.value,
            onValueChange = { emailState.value = it },
            label = stringResource(R.string.email),
            placeholder = stringResource(R.string.email),
            isError = false,
            errorText = null,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
                imeAction = androidx.compose.ui.text.input.ImeAction.Next
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions.Default,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        val passwordTooShort = passwordState.value.isNotEmpty() && passwordState.value.length < 8
        PasswordField(
            label = stringResource(R.string.password),
            placeholder = stringResource(R.string.password_placeholder),
            state = passwordState,
            visibilityState = passwordVisibility,
            onImeAction = {},
            isError = passwordTooShort,
            errorText = if (passwordTooShort) stringResource(id = R.string.password_invalid) else null
        )

        Spacer(modifier = Modifier.height(16.dp))

        val passwordsMatch = passwordState.value == confirmPasswordState.value
        val showPasswordMismatch = confirmPasswordState.value.isNotEmpty() && !passwordsMatch
        PasswordField(
            label = stringResource(R.string.confirm_password),
            placeholder = stringResource(R.string.confirm_password_placeholder),
            state = confirmPasswordState,
            visibilityState = confirmPasswordVisibility,
            onImeAction = {},
            isError = showPasswordMismatch || passwordTooShort,
            errorText = when {
                passwordTooShort -> stringResource(id = R.string.password_invalid)
                showPasswordMismatch -> stringResource(R.string.passwords_do_not_match)
                else -> null
            }
        )

        if (showPasswordMismatch) {
            Text(
                text = stringResource(R.string.passwords_do_not_match),
                color = ColorAccentSecondary,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = acceptedTerms.value,
                onCheckedChange = { acceptedTerms.value = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = ColorAccentSecondary,
                    uncheckedColor = Color(0xFF9A9AB0).copy(alpha = 0.6f),
                    checkmarkColor = Color(0xFFE6E6EB)
                )
            )
            Text(
                text = stringResource(R.string.accept_terms),
                color = Color(0xFF9A9AB0),
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        val canRegister = usernameState.value.isNotBlank() &&
            emailState.value.isNotBlank() &&
            !passwordTooShort &&
            passwordsMatch &&
            acceptedTerms.value

        GradientButton(
            text = stringResource(R.string.create_account),
            enabled = canRegister,
            onClick = {
                viewModel.signUpCreateProfileAndLogin(
                    username = usernameState.value.trim(),
                    email = emailState.value.trim(),
                    password = passwordState.value
                )
            },
            modifier = Modifier.fillMaxWidth()
        )

                    Spacer(modifier = Modifier.height(24.dp))
                }
                
                // Login link - всегда внизу, не скроллится
                Row(
                    modifier = Modifier
                        .padding(bottom = paddingValues.calculateBottomPadding() + 16.dp)
                        .clickable { onNavigateToLogin() },
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.already_have_account),
                        color = Color(0xFF9A9AB0),
                        fontSize = 14.sp
                    )
                    Text(
                        text = " ${stringResource(R.string.login)}",
                        color = ColorAccentSecondary,
                        fontSize = 14.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun PasswordField(
    label: String,
    placeholder: String,
    state: MutableState<String>,
    visibilityState: MutableState<Boolean>,
    onImeAction: () -> Unit,
    isError: Boolean,
    errorText: String?
) {
    LabeledTextField(
        value = state.value,
        onValueChange = { state.value = it },
        label = label,
        placeholder = placeholder,
        isError = isError,
        errorText = errorText,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
            imeAction = androidx.compose.ui.text.input.ImeAction.Next
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = { onImeAction() }),
        visualTransformation = if (visibilityState.value) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visibilityState.value = !visibilityState.value }) {
                Icon(
                    imageVector = if (visibilityState.value) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = null
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}
