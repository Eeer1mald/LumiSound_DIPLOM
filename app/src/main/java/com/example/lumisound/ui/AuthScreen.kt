package com.example.lumisound.ui

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lumisound.R
import com.example.lumisound.AuthViewModel
import com.example.lumisound.AuthResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun AuthScreen(modifier: Modifier = Modifier, viewModel: AuthViewModel) {
    val context = LocalContext.current
    val state by viewModel.authState.collectAsState()
    // UI State
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Google Sign-In launcher
    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken("720567253667-nseep0m3eahgo2gpe7dr0uvrnp3k35iu.apps.googleusercontent.com") // Client ID
            .build()
        GoogleSignIn.getClient(context, gso)
    }
    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                // передавайте account?.idToken на ваш backend для JWT-входа или используйте напрямую для Firebase
                viewModel.onGoogleSignIn(account?.idToken ?: account?.email ?: "")
            } catch (e: Exception) {
                error = "Ошибка Google Sign-In: ${e.localizedMessage}"
            }
        } else {
            error = "Google вход отменён пользователем"
        }
    }

    // Реакция на изменения состояния авторизации
    LaunchedEffect(state) {
        when (state) {
            is AuthResult.Error -> error = (state as AuthResult.Error).message
            is AuthResult.Success -> error = null // здесь переход на другой экран
            is AuthResult.Loading -> error = null
            is AuthResult.Idle -> {}
        }
    }
    val isLoading = state is AuthResult.Loading

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF191414), Color(0xFF101418), Color(0xFF0B0F13)),
                    startY = 0f,
                    endY = 1200f
                )
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(64.dp))
            val logoId = remember { context.resources.getIdentifier("logo", "drawable", context.packageName) }
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F1419)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = if (logoId != 0) logoId else android.R.drawable.ic_media_play),
                    contentDescription = "LumiSound Logo",
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Вход в LumiSound",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                singleLine = true,
                label = { Text("Email или логин") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF5C6FFF),
                    unfocusedBorderColor = Color(0xFF2A2F35),
                    focusedLabelColor = Color(0xFF8EA2FF),
                    unfocusedLabelColor = Color(0xFF6B737C),
                    cursorColor = Color(0xFF8EA2FF)
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                singleLine = true,
                label = { Text("Пароль") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF5C6FFF),
                    unfocusedBorderColor = Color(0xFF2A2F35),
                    focusedLabelColor = Color(0xFF8EA2FF),
                    unfocusedLabelColor = Color(0xFF6B737C),
                    cursorColor = Color(0xFF8EA2FF)
                ),
                visualTransformation =
                    if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        val eyeId = context.resources.getIdentifier(
                            if (isPasswordVisible) "visibility" else "visibility_off",
                            "drawable",
                            "android"
                        )
                        Icon(
                            painter = painterResource(id = if (eyeId != 0) eyeId else android.R.drawable.ic_menu_view),
                            contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (error != null) {
                Text(
                    text = error!!,
                    color = Color(0xFFFF4C4C),
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            GradientButton(text = if (isLoading) "" else "Войти", enabled = !isLoading) {
                viewModel.login(email, password)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(Modifier.weight(1f), color = Color(0xFF333333))
                Text("  или  ", color = Color.White, fontSize = 14.sp)
                HorizontalDivider(Modifier.weight(1f), color = Color(0xFF333333))
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = {
                    val signInIntent = googleSignInClient.signInIntent
                    googleLauncher.launch(signInIntent)
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    painter = painterResource(android.R.drawable.ic_dialog_email),
                    contentDescription = "Google Sign-In",
                    tint = Color(0xFF8EA2FF),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Войти через Google")
            }
            Spacer(modifier = Modifier.height(36.dp))
            Text(
                "Еще нет аккаунта? Зарегистрироваться!",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun GradientButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val gradient = Brush.horizontalGradient(listOf(Color(0xFF7758FF), Color(0xFF38BDF8)))
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Color.Transparent)
            .clickable(enabled = enabled) { onClick() },
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(gradient),
            contentAlignment = Alignment.Center
        ) {
            if (text.isNotEmpty()) {
                Text(text = text, color = Color.White, fontSize = 16.sp)
            } else {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }
    }
}
