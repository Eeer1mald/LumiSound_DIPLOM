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
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
    
    // Заменён градиент на однотонный фон - используем ColorSurface для небольшого контраста
    val backgroundSolidColor: Color = remember {
        ColorSurface // #121212 - темно-серый для фона
    }


    // --- Состояние и параметры для анимации закрытия свайпом вниз ---
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val maxDragPx = remember(screenHeightPx) { screenHeightPx * 0.6f } // сколько тянуть до полного закрытия

    // Прогресс закрытия от 0f до 1f. 0f — плеер полностью открыт, 1f — готов к закрытию.
    var closeProgress by remember { mutableStateOf(0f) }
    var isDraggingToClose by remember { mutableStateOf(false) }

    // Смещение и прозрачность содержимого плеера при закрытии
    val contentOffsetY = remember(closeProgress, screenHeightPx) {
        (screenHeightPx * 0.6f * closeProgress.coerceIn(0f, 1f))
    }
    val contentAlpha = remember(closeProgress) { (1f - closeProgress.coerceIn(0f, 1f)).coerceIn(0f, 1f) }

    // Scope для анимации закрытия по кнопке (стрелка)
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
        
        // Показываем предыдущий экран когда начинается закрытие (closeProgress > 0.1)
        // Это ускоряет загрузку страницы и делает анимацию кнопки стрелки видимой
        // Используем alpha для плавного появления
        if (stablePreviousRoute != null && navController != null && closeProgress > 0.1f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(closeProgress.coerceIn(0f, 1f))
            ) {
                // Отображаем предыдущий экран по маршруту
                // Используем key для стабильности, чтобы Compose не пересоздавал экран
                // и remember для кэширования, чтобы состояние сохранялось
                key(stablePreviousRoute) {
                    // Используем LaunchedEffect для сохранения состояния экрана
                    LaunchedEffect(stablePreviousRoute) {
                        // Сохраняем состояние экрана в savedStateHandle для предотвращения перезагрузки
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("cachedRoute", stablePreviousRoute)
                    }
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
                .background(backgroundSolidColor.copy(alpha = (1f - closeProgress * 0.5f).coerceIn(0.5f, 1f)))
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
                            // Если протянули достаточно далеко — сразу закрываем экран.
                            // ВАЖНО: НЕ сбрасываем closeProgress перед onClose, чтобы не было
                            // лишнего кадра с полностью открытым плеером.
                            if (closeProgress >= 0.4f) {
                                isDraggingToClose = false
                                onClose()
                            } else {
                                // Иначе просто возвращаем в исходное состояние.
                                closeProgress = 0f
                                isDraggingToClose = false
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
                    x = 24.dp, // padding Column
                    y = 32.dp + with(density) { contentOffsetY.toDp() } // опустили стрелочку ниже
                )
                // Синхронизируем alpha с контентом - кнопка исчезает вместе с ним
                .alpha(contentAlpha)
                .size(48.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    // Быстрая анимация свайпа вниз при нажатии на стрелочку
                    // Замена ручного свайпа - быстрая анимация закрытия с показом предыдущего экрана
                    closeScope.launch {
                        isDraggingToClose = true
                        // Быстрая анимация: плавное закрытие с показом предыдущего экрана
                        val steps = 20
                        val duration = 250L // Длительность для плавной анимации
                        val stepDelay = duration / steps
                        
                        // Анимация с ускорением (easing) для более естественного движения
                        // Используем более плавное easing для лучшей видимости анимации
                        repeat(steps) { i ->
                            val t = (i + 1).toFloat() / steps
                            // Используем кубическое easing для более плавного движения
                            val easedT = t * t * (3f - 2f * t) // Smoothstep easing
                            closeProgress = easedT.coerceIn(0f, 1f)
                            delay(stepDelay)
                        }
                        
                        // Убеждаемся, что прогресс достиг максимума перед закрытием
                        closeProgress = 1f
                        isDraggingToClose = false
                        
                        // Небольшая задержка для завершения анимации
                        delay(50)
                        
                        onClose()
                        // closeProgress сбрасывать не нужно — экран будет уничтожен навигацией
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
