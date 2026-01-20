package com.example.lumisound.feature.nowplaying

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.lumisound.data.model.Track
import com.example.lumisound.ui.theme.ColorAccentSecondary
import com.example.lumisound.ui.theme.ColorBackground
import com.example.lumisound.ui.theme.ColorOnBackground
import com.example.lumisound.ui.theme.ColorSecondary
import com.example.lumisound.ui.theme.ColorSurface
import com.example.lumisound.ui.theme.GradientEnd
import com.example.lumisound.ui.theme.GradientStart

@Composable
fun TestPlayerScreen(
    track: Track,
    onClose: () -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val view = LocalView.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    // Состояние для анимации при нажатии на кнопку закрытия
    var isClosing by remember { mutableStateOf(false) }
    
    // Используем Animatable для ручного управления анимацией закрытия
    val closeAnimation = remember { Animatable(1f) }
    
    // Флаг для предотвращения повторного вызова onClose
    var hasCalledOnClose by remember { mutableStateOf(false) }
    
    // Запускаем анимацию при нажатии на кнопку - только один раз
    LaunchedEffect(isClosing) {
        if (isClosing && !hasCalledOnClose) {
            // Сразу вызываем onClose только один раз, чтобы страница поиска появилась под анимацией
            hasCalledOnClose = true
            onClose()
            
            // Плавная анимация от полного плеера (1f) к мини-плееру (0f)
            closeAnimation.snapTo(1f)
            closeAnimation.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 400,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }
    
    // Сбрасываем флаги при открытии плеера заново (когда isClosing становится false)
    LaunchedEffect(isClosing) {
        if (!isClosing) {
            hasCalledOnClose = false
            closeAnimation.snapTo(1f)
        }
    }
    
    // Оптимизация: используем derivedStateOf правильно - создаем один раз в remember
    val currentProgress = remember {
        androidx.compose.runtime.derivedStateOf {
            if (isClosing) closeAnimation.value else 1f
        }
    }.value
    
    // Если анимация закрытия завершена, не рендерим экран (предотвращает мигание)
    // Используем более строгое условие - только если точно закрываемся и прогресс минимален
    if (isClosing && hasCalledOnClose && currentProgress <= 0.01f) {
        return
    }
    
    // Оптимизация: вычисляем все анимационные значения в одном блоке remember для минимизации рекомпозиций
    // Зависимости: currentProgress меняется на каждом кадре анимации, остальные - константы
    val screenWidthPx = remember(density, configuration) { 
        with(density) { configuration.screenWidthDp.dp.toPx() } 
    }
    val startY = remember(screenHeightPx) { screenHeightPx * 0.85f }
    
    // Вычисляем все значения анимации в одном remember - минимизируем рекомпозиции
    val t = remember(currentProgress) { currentProgress.coerceIn(0f, 1f) }
    val easedProgress = remember(t) { 1f - (1f - t) * (1f - t) }
    val minScale = 0.25f
    val scale = remember(easedProgress) { minScale + (easedProgress * (1f - minScale)) }
    val baseY = remember(easedProgress, startY) { startY * (1f - easedProgress) }
    val playerAlpha = remember(t) { if (t < 0.1f) t / 0.1f else 1f }
    val offsetX = remember(scale, screenWidthPx) { (screenWidthPx * (1f - scale) / 2f) }
    
    // Минимальные настройки status bar
    SideEffect {
        if (!view.isInEditMode && context is Activity) {
            val window = context.window
            val insetsController = WindowCompat.getInsetsController(window, view)
            
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }
    
    // Player state для элементов управления
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    var isLiked by remember { mutableStateOf(false) }
    var userRating by remember { mutableIntStateOf(0) }
    var hoveredRating by remember { mutableIntStateOf(0) }
    
    // Синхронизируем состояние при открытии плеера
    LaunchedEffect(track.id) {
        viewModel.syncPlayerState()
    }
    
    // Фон сразу прозрачный при нажатии, чтобы сразу была видна страница поиска
    val backgroundColor = remember(isClosing) {
        if (isClosing) Color.Transparent else ColorBackground
    }
    
    // Заменён градиент на однотонный фон - используем ColorSurface для небольшого контраста
    val backgroundSolidColor: Color = remember {
        ColorSurface // #121212 - темно-серый для фона
    }
    val scrollState = rememberScrollState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .offset {
                IntOffset(
                    x = offsetX.roundToInt(),
                    y = baseY.roundToInt()
                )
            }
            .scale(scale)
            .alpha(playerAlpha)
    ) {
        // Фоновый однотонный цвет (если не закрываемся) - заменён градиент
        if (!isClosing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundSolidColor)
            )
        }
        
        // Прокручиваемое содержимое
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(24.dp)
                .padding(bottom = 80.dp)
        ) {
            // Header со стрелкой
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (!isClosing) {
                        isClosing = true
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Close",
                        tint = ColorOnBackground,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "Сейчас играет",
                    color = ColorSecondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Album Cover
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(24.dp),
                        spotColor = Color.Black.copy(alpha = 0.6f)
                    )
            ) {
                if (track.hdImageUrl != null && track.hdImageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(track.hdImageUrl)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (track.imageUrl != null && track.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(track.imageUrl)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ColorSurface), // Тёмно-серый вместо 0xFF1A1B2E
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = GradientStart.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Track Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = track.name,
                    color = ColorOnBackground,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = track.artist ?: "",
                    color = ColorSecondary,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Progress Bar
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF1F1F1F)) // Тёмно-серый вместо 0xFF2A2D3E
                ) {
                    val progress = if (duration > 0) {
                        (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(4.dp)
                            .background(GradientStart) // Заменён градиент на однотонный цвет
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPosition),
                        color = ColorSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = formatTime(duration),
                        color = ColorSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { isLiked = !isLiked },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) ColorAccentSecondary else ColorSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = { viewModel.previousTrack() },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = ColorOnBackground,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(0.5f))

                // Заменён градиент на однотонный цвет для кнопки воспроизведения
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = GradientStart, // Однотонный вместо градиента
                            shape = CircleShape
                        )
                        .shadow(
                            elevation = 12.dp,
                            shape = CircleShape,
                            spotColor = GradientStart.copy(alpha = 0.4f)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { viewModel.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(0.5f))

                IconButton(
                    onClick = { viewModel.nextTrack() },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = ColorOnBackground,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = { },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add to playlist",
                        tint = ColorSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Rating Section - заменён градиент на однотонный цвет
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = ColorSurface.copy(alpha = 0.8f), // Темно-серый вместо градиента
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0xFF1F1F1F).copy(alpha = 0.4f), // Тёмно-серый для границы
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Оцените этот трек",
                        color = ColorOnBackground,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Это поможет нам подбирать музыку для вас",
                        color = ColorSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    // Rating Scale 1-10
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (1..10).forEach { rating ->
                            val isActive = userRating == rating
                            val isHovered = hoveredRating >= rating
                            val shouldHighlight = isActive || isHovered

                            Box(
                                modifier = Modifier
                                    .size(width = 32.dp, height = 48.dp)
                                    .background(
                                        color = remember(shouldHighlight) {
                                            if (shouldHighlight) {
                                                GradientStart // Однотонный акцентный цвет
                                            } else {
                                                ColorSurface // Тёмно-серый для неактивных
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (shouldHighlight) {
                                            Color.Transparent
                                        } else {
                                            Color(0xFF1F1F1F).copy(alpha = 0.4f) // Тёмно-серый вместо 0xFF2A2D3E
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .shadow(
                                        elevation = if (shouldHighlight) 4.dp else 0.dp,
                                        shape = RoundedCornerShape(8.dp),
                                        spotColor = if (shouldHighlight) GradientStart.copy(alpha = 0.4f) else Color.Transparent
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        userRating = rating
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$rating",
                                    color = if (shouldHighlight) Color.White else ColorSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = if (shouldHighlight) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    if (userRating > 0) {
                        Text(
                            text = "✨ Оценка: $userRating/10",
                            color = GradientStart,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
