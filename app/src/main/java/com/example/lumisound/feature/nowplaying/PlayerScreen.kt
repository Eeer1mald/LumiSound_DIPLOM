package com.example.lumisound.feature.nowplaying

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
fun PlayerScreen(
    track: Track,
    onClose: () -> Unit = {},
    previousRoute: String? = null, // Маршрут предыдущего экрана для показа под плеером
    navController: androidx.navigation.NavHostController? = null, // Для отображения предыдущего экрана
    userName: String = "Александр", // Для отображения предыдущего экрана
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val view = LocalView.current
    val context = LocalContext.current
    
    // Настройки status bar — только один раз при входе
    androidx.compose.runtime.DisposableEffect(Unit) {
        if (!view.isInEditMode && context is Activity) {
            val window = context.window
            val insetsController = WindowCompat.getInsetsController(window, view)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
        onDispose { }
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
    
    // Заменён градиент на однотонный фон - используем ColorSurface для небольшого контраста
    val backgroundSolidColor: Color = remember {
        ColorSurface // #121212 - темно-серый для фона
    }


    // --- Состояние и параметры для анимации закрытия свайпом вниз ---
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val maxDragPx = screenHeightPx * 0.6f

    var closeProgress by remember { mutableStateOf(0f) }
    var isDraggingToClose by remember { mutableStateOf(false) }
    var targetCloseProgress by remember { mutableStateOf(0f) }

    // Анимированный прогресс: во время drag — прямой, после — spring
    val animatedCloseProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = targetCloseProgress,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "closeProgress"
    )
    val effectiveCloseProgress = if (isDraggingToClose) closeProgress else animatedCloseProgress

    // Смещение и прозрачность — inline вычисления без remember
    val contentOffsetY = screenHeightPx * 0.6f * effectiveCloseProgress.coerceIn(0f, 1f)
    val contentAlpha = (1f - effectiveCloseProgress.coerceIn(0f, 1f)).coerceIn(0f, 1f)

    val closeScope = rememberCoroutineScope()

    // Корневой контейнер - показываем предыдущий экран под плеером во время закрытия
    // Используем правильное кэширование, чтобы экран не перезагружался
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Показываем предыдущий экран под плеером во время свайпа вниз
        // Это нужно, чтобы вместо белого фона был виден реальный экран
        // Используем remember для стабильности previousRoute и правильное кэширование
        val stablePreviousRoute = remember(previousRoute) { previousRoute }
        
        // Показываем предыдущий экран когда начинается закрытие (effectiveCloseProgress > 0.1)
        if (stablePreviousRoute != null && navController != null && effectiveCloseProgress > 0.1f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(effectiveCloseProgress.coerceIn(0f, 1f))
            ) {
                key(stablePreviousRoute) {
                    com.example.lumisound.navigation.ScreenContent(
                        route = stablePreviousRoute,
                        navController = navController,
                        userName = userName
                    )
                }
            }
        }

        // Прокручиваемое содержимое плеера как «шторка», которая выезжает снизу
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = with(density) { contentOffsetY.toDp() })
                .alpha(contentAlpha)
                // Фон только у самой «шторки», а не на весь экран.
                // При закрытии делаем фон прозрачным, чтобы был виден предыдущий экран из Navigation back stack
                .background(backgroundSolidColor.copy(alpha = (1f - effectiveCloseProgress * 0.5f).coerceIn(0.5f, 1f)))
                .statusBarsPadding()
                .padding(24.dp)
                .padding(bottom = 80.dp)
        ) {
            // Header со стрелкой слева (как в примере)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Заглушка для выравнивания - кнопка будет размещена отдельно, но синхронизирована с контентом
                Spacer(modifier = Modifier.size(48.dp))
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
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { isLiked = !isLiked },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) ColorAccentSecondary else ColorSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Previous — круглая кнопка
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(color = ColorSurface, shape = CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { viewModel.previousTrack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = ColorOnBackground,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Play/Pause — большая круглая кнопка
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .shadow(elevation = 16.dp, shape = CircleShape, spotColor = GradientStart.copy(alpha = 0.5f))
                        .background(color = GradientStart, shape = CircleShape)
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
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Next — круглая кнопка
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(color = ColorSurface, shape = CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { viewModel.nextTrack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = ColorOnBackground,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Add
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add to playlist",
                        tint = ColorSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Невидимая широкая зона вверху для свайпа вниз, как на примере.
        // Размещаем ЕЁ ПОСЛЕ контента и с align, чтобы она была "поверх" и ловила жесты.
        // НО исключаем область Row с кнопкой (первые ~80dp по ширине и ~60dp по высоте), чтобы не мешать кликам
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .align(Alignment.TopCenter)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            // Игнорируем жесты, которые начинаются в области Row с кнопкой
                            // Row с кнопкой находится слева, примерно первые 80dp по ширине
                            // и первые 60dp по высоте (с учетом padding и status bar)
                            val buttonAreaWidth = with(density) { 80.dp.toPx() }
                            val buttonAreaHeight = with(density) { 60.dp.toPx() }
                            if (offset.x < buttonAreaWidth && offset.y < buttonAreaHeight) {
                                return@detectVerticalDragGestures // Не обрабатываем жесты в области кнопки
                            }
                            isDraggingToClose = true
                        },
                        onDragEnd = {
                            if (closeProgress >= 0.4f) {
                                isDraggingToClose = false
                                onClose()
                            } else {
                                closeProgress = 0f
                                isDraggingToClose = false
                                targetCloseProgress = 0f
                            }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            // Игнорируем жесты в области Row с кнопкой
                            val buttonAreaWidth = with(density) { 80.dp.toPx() }
                            val buttonAreaHeight = with(density) { 60.dp.toPx() }
                            if (change.position.x < buttonAreaWidth && change.position.y < buttonAreaHeight) {
                                return@detectVerticalDragGestures // Не обрабатываем в области кнопки
                            }
                            
                            if (dragAmount > 0) { // тянем вниз
                                val newProgress = (closeProgress + (dragAmount / maxDragPx)).coerceIn(0f, 1f)
                                closeProgress = newProgress
                                change.consume()
                            } else if (dragAmount < 0 && closeProgress > 0f) {
                                // Можно немного вернуть вверх
                                val newProgress = (closeProgress + (dragAmount / maxDragPx)).coerceIn(0f, 1f)
                                closeProgress = newProgress
                                change.consume()
                            }
                        }
                    )
                }
        )
        
        // Кнопка стрелки размещена ОТДЕЛЬНО поверх всего, но синхронизирована с позицией контента
        // Это позволяет ей получать клики (быть поверх зоны свайпа) и двигаться вместе с контентом
        // Column имеет: statusBarsPadding() + padding(24.dp) + padding(vertical = 12.dp)
        // Кнопка должна быть на той же позиции, что и в Row внутри Column
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding() // Те же отступы, что и в Column
                // Синхронизируем offset с контентом - кнопка двигается вместе с ним
                // Добавляем небольшой отступ вниз, чтобы кнопка была вровень с текстом "Сейчас играет"
                .offset(
                    x = 24.dp,
                    y = 32.dp + with(density) { contentOffsetY.toDp() }
                )
                .alpha(contentAlpha)
                .size(48.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    // Анимация закрытия через spring — плавно и без delay
                    isDraggingToClose = false
                    targetCloseProgress = 1f
                    closeScope.launch {
                        delay(300) // ждём завершения spring анимации
                        onClose()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = "Close",
                tint = ColorOnBackground,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
