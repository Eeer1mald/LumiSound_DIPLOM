package com.example.lumisound.feature.auth.profilesetup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.lumisound.R
import com.example.lumisound.feature.auth.components.GradientButton
import com.example.lumisound.feature.auth.components.LabeledTextField
import com.example.lumisound.ui.theme.ColorAccentSecondary
import com.example.lumisound.ui.theme.GradientEnd
import com.example.lumisound.ui.theme.GradientStart
import com.example.lumisound.ui.theme.LocalAppColors

@Composable
fun ProfileSetupScreen(
    viewModel: ProfileSetupViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // uCrop launcher for image cropping
    val cropLauncher = rememberLauncherForActivityResult(
        contract = com.example.lumisound.feature.auth.util.UCropActivityResultContract()
    ) { croppedUri: Uri? ->
        croppedUri?.let { viewModel.onAvatarSelected(it) }
    }
    
    // Image picker
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { 
            // Запускаем uCrop для обрезки
            cropLauncher.launch(it)
        }
    }
    
    // Animation states
    var showContent by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        showContent = true
    }
    
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is ProfileSetupSideEffect.NavigateToHome -> onNavigateToHome()
                is ProfileSetupSideEffect.ShowSnackbar -> {
                    scope.launch { snackbarHostState.showSnackbar(effect.message) }
                }
            }
        }
    }
    
    androidx.compose.material3.Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
        containerColor = LocalAppColors.current.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(LocalAppColors.current.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 32.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Avatar with camera icon
                AnimatedVisibility(
                    visible = showContent,
                    enter = slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(400)
                    ) + fadeIn(animationSpec = tween(400)),
                    exit = fadeOut(animationSpec = tween(150))
                ) {
                    Box(
                        modifier = Modifier.size(120.dp)
                    ) {
                        // Avatar image
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(
                                    brush = remember {
                                        Brush.linearGradient(
                                            colors = listOf(GradientStart, GradientEnd)
                                        )
                                    }
                                )
                                .border(
                                    width = 4.dp,
                                    color = LocalAppColors.current.background,
                                    shape = CircleShape
                                )
                        ) {
                            if (uiState.avatarUri != null) {
                                SubcomposeAsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(uiState.avatarUri)
                                        .build(),
                                    contentDescription = "Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    success = {
                                        SubcomposeAsyncImageContent()
                                    }
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Avatar placeholder",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(32.dp)
                                )
                            }
                        }
                        
                        // Camera icon button
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(40.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(GradientStart, GradientEnd)
                                    ),
                                    shape = CircleShape
                                )
                                .border(
                                    width = 3.dp,
                                    color = LocalAppColors.current.background,
                                    shape = CircleShape
                                )
                                .clickable { imagePicker.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Change avatar",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Title
                AnimatedVisibility(
                    visible = showContent,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(400, delayMillis = 100)
                    ) + fadeIn(animationSpec = tween(400, delayMillis = 100)),
                    exit = fadeOut(animationSpec = tween(150))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Расскажите о себе",
                            color = LocalAppColors.current.onBackground,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Заполните данные для завершения регистрации",
                            color = LocalAppColors.current.secondary,
                            fontSize = 14.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Username field (required)
                AnimatedVisibility(
                    visible = showContent,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(400, delayMillis = 200)
                    ) + fadeIn(animationSpec = tween(400, delayMillis = 200)),
                    exit = fadeOut(animationSpec = tween(150))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LabeledTextField(
                            value = uiState.username,
                            onValueChange = { viewModel.onUsernameChanged(it) },
                            label = "Никнейм",
                            placeholder = "Введите ваш никнейм",
                            isError = uiState.usernameError != null,
                            errorText = uiState.usernameError,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions.Default,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "profile_setup_username"
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        LabeledTextField(
                            value = uiState.bio,
                            onValueChange = { viewModel.onBioChanged(it) },
                            label = "О себе (необязательно)",
                            placeholder = "Расскажите о себе",
                            isError = false,
                            errorText = null,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions.Default,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "profile_setup_bio"
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        LabeledTextField(
                            value = uiState.favoriteGenre,
                            onValueChange = { viewModel.onFavoriteGenreChanged(it) },
                            label = "Любимый жанр (необязательно)",
                            placeholder = "Например: Рок, Поп, Джаз",
                            isError = false,
                            errorText = null,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions.Default,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "profile_setup_genre"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Submit button
                AnimatedVisibility(
                    visible = showContent,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(400, delayMillis = 300)
                    ) + fadeIn(animationSpec = tween(400, delayMillis = 300)),
                    exit = fadeOut(animationSpec = tween(150))
                ) {
                    GradientButton(
                        text = if (uiState.isSubmitting) "" else "Завершить регистрацию",
                        enabled = uiState.username.isNotBlank() && !uiState.isSubmitting && uiState.usernameError == null,
                        onClick = { viewModel.submitProfile() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        testTag = "profile_setup_submit"
                    )
                }
            }
            
            // Loading overlay
            AnimatedVisibility(
                visible = uiState.isSubmitting,
                enter = fadeIn(animationSpec = tween(150)),
                exit = fadeOut(animationSpec = tween(150))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LocalAppColors.current.background.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = LocalAppColors.current.secondary)
                }
            }
        }
    }
}
