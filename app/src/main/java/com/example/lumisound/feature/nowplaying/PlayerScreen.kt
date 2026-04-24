package com.example.lumisound.feature.nowplaying

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
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
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.lumisound.data.model.Track
import com.example.lumisound.ui.theme.GradientEnd
import com.example.lumisound.ui.theme.GradientStart
import com.example.lumisound.ui.theme.LocalAppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

// Генерируем псевдо-waveform на основе seed трека (детерминировано для одного трека)
private fun generateWaveform(trackId: String, bars: Int = 80): List<Float> {
    val seed = trackId.hashCode().toLong()
    val rng = Random(seed)
    return List(bars) {
        // Имитируем реальный аудио-профиль: тихое начало, громкая середина, затухание
        val pos = it.toFloat() / bars
        val envelope = when {
            pos < 0.1f -> pos / 0.1f * 0.6f
            pos > 0.85f -> (1f - pos) / 0.15f * 0.5f
            else -> 0.4f + rng.nextFloat() * 0.6f
        }
        // Добавляем синусоидальные волны для реалистичности
        val wave = 0.15f * sin(pos * 20f).toFloat().coerceAtLeast(0f)
        (envelope + wave + rng.nextFloat() * 0.15f).coerceIn(0.05f, 1f)
    }
}

@Composable
fun PlayerScreen(
    track: Track,
    onClose: () -> Unit = {},
    previousRoute: String? = null,
    navController: androidx.navigation.NavHostController? = null,
    userName: String = "Александр",
    viewModel: PlayerViewModel = hiltViewModel(),
    reviewViewModel: com.example.lumisound.feature.ratings.ReviewViewModel = hiltViewModel()
) {
    val view = LocalView.current
    val context = LocalContext.current

    DisposableEffect(Unit) {
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

    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()

    LaunchedEffect(track.id) {
        viewModel.syncPlayerState()
        reviewViewModel.loadForTrack(track.id)
    }

    // Плавающие комментарии
    val reviewState by reviewViewModel.state.collectAsState()
    var floatingCommentIndex by remember { mutableIntStateOf(-1) }
    var showFloatingComment by remember { mutableStateOf(false) }

    LaunchedEffect(reviewState.comments) {
        if (reviewState.comments.isNotEmpty() && viewModel.showFloatingComments) {
            while (true) {
                delay(4000)
                if (!viewModel.showFloatingComments) { showFloatingComment = false; break }
                val idx = (0 until reviewState.comments.size).random()
                floatingCommentIndex = idx
                showFloatingComment = true
                delay(3000)
                showFloatingComment = false
                delay(1000)
            }
        }
    }

    // Waveform — генерируем один раз для трека
    val waveform = remember(track.id) { generateWaveform(track.id) }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val maxDragPx = screenHeightPx * 0.6f

    var closeProgress by remember { mutableStateOf(0f) }
    var isDraggingToClose by remember { mutableStateOf(false) }
    var targetCloseProgress by remember { mutableStateOf(0f) }
    var isClosing by remember { mutableStateOf(false) }
    var showAddToPlaylist by remember { mutableStateOf(false) }
    var showTrackInfo by remember { mutableStateOf(false) }

    // Когда isClosing=true — запускаем анимацию до конца, потом вызываем onClose
    LaunchedEffect(isClosing) {
        if (isClosing) {
            // Ждём один кадр чтобы closeProgress=1f успел отрендериться
            kotlinx.coroutines.delay(32)
            onClose()
            isClosing = false
            closeProgress = 0f
            targetCloseProgress = 0f
            isDraggingToClose = false
        }
    }

    // Горизонтальный свайп — Animatable для плавного контроля
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val swipeAnim = remember { Animatable(0f) }
    var isDraggingHorizontal by remember { mutableStateOf(false) }
    val swipeScope = rememberCoroutineScope()

    // Соседние треки для отображения при свайпе
    val playlist by viewModel.playerStateHolder.playlist.collectAsState()
    val currentIndex by viewModel.playerStateHolder.currentIndex.collectAsState()
    val prevTrack = playlist.getOrNull(currentIndex - 1)
    val nextTrack = playlist.getOrNull(currentIndex + 1)

    // Текущее смещение (px) — используем Animatable.value напрямую через State
    val swipeOffsetPx = swipeAnim.value

    val animatedCloseProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = targetCloseProgress,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
        ),
        label = "closeProgress"
    )
    val effectiveCloseProgress = if (isDraggingToClose) closeProgress else animatedCloseProgress
    val contentOffsetY = screenHeightPx * 0.6f * effectiveCloseProgress.coerceIn(0f, 1f)
    val contentAlpha = (1f - effectiveCloseProgress.coerceIn(0f, 1f)).coerceIn(0f, 1f)

    val closeScope = rememberCoroutineScope()
    val stablePreviousRoute = remember(previousRoute) { previousRoute }

    Box(modifier = Modifier.fillMaxSize()) {

        // Предыдущий экран — рендерим только при активном свайпе закрытия
        // чтобы показать его проявление под плеером
        if (stablePreviousRoute != null && navController != null && effectiveCloseProgress > 0.01f) {
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

        // Предыдущий трек — виден слева при свайпе вправо
        if (prevTrack != null && swipeOffsetPx > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(x = (swipeOffsetPx - screenWidthPx).toInt(), y = 0) }
                    .background(LocalAppColors.current.background)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                PlayerPageContent(track = prevTrack, context = context)
            }
        }

        // Следующий трек — виден справа при свайпе влево
        if (nextTrack != null && swipeOffsetPx < 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(x = (swipeOffsetPx + screenWidthPx).toInt(), y = 0) }
                    .background(LocalAppColors.current.background)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                PlayerPageContent(track = nextTrack, context = context)
            }
        }

        // Основной контент плеера
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(x = swipeOffsetPx.toInt(), y = contentOffsetY.toInt()) }
                .alpha(contentAlpha)
                .background(LocalAppColors.current.background)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ── Хедер ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            // Кнопка — мгновенное закрытие без анимации
                            isDraggingToClose = true
                            closeProgress = 1f
                            isClosing = true
                        },                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Close",
                        tint = LocalAppColors.current.onBackground,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "Сейчас играет",
                    color = LocalAppColors.current.secondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.Center)
                )
                // Троеточие справа — информация о треке
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showTrackInfo = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Информация",
                        tint = LocalAppColors.current.onBackground,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // ── Обложка ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .pointerInput(Unit) {
                        val velocityTracker = VelocityTracker()
                        detectHorizontalDragGestures(
                            onDragStart = {
                                isDraggingHorizontal = true
                                velocityTracker.resetTracking()
                            },
                            onDragEnd = {
                                isDraggingHorizontal = false
                                val velocity = velocityTracker.calculateVelocity().x
                                val offset = swipeAnim.value
                                val threshold = screenWidthPx * 0.25f
                                val flingThreshold = 800f
                                swipeScope.launch {
                                    when {
                                        offset < -threshold || velocity < -flingThreshold -> {
                                            swipeAnim.animateTo(-screenWidthPx, SpringSpec(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow))
                                            viewModel.nextTrack()
                                            swipeAnim.snapTo(0f)
                                        }
                                        (offset > threshold || velocity > flingThreshold) && viewModel.hasPrevious -> {
                                            swipeAnim.animateTo(screenWidthPx, SpringSpec(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow))
                                            viewModel.previousTrack()
                                            swipeAnim.snapTo(0f)
                                        }
                                        else -> {
                                            swipeAnim.animateTo(0f, SpringSpec(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                                        }
                                    }
                                }
                            },
                            onDragCancel = {
                                isDraggingHorizontal = false
                                swipeScope.launch {
                                    swipeAnim.animateTo(0f, SpringSpec(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                velocityTracker.addPointerInputChange(change)
                                val newOffset = swipeAnim.value + dragAmount
                                val clamped = when {
                                    newOffset > 0 && !viewModel.hasPrevious -> (newOffset * 0.15f).coerceAtMost(40f)
                                    newOffset < 0 && nextTrack == null -> (newOffset * 0.15f).coerceAtLeast(-40f)
                                    else -> newOffset
                                }
                                swipeScope.launch { swipeAnim.snapTo(clamped) }
                            }
                        )
                    }
            ) {
                if (!track.hdImageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(track.hdImageUrl).crossfade(false).build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (!track.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(track.imageUrl).crossfade(false).build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(LocalAppColors.current.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MusicNote, null, tint = GradientStart.copy(alpha = 0.4f), modifier = Modifier.size(72.dp))
                    }
                }
            }

            // ── Нижняя панель ───────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 24.dp)
            ) {
                // Название и артист
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = track.name,
                        color = LocalAppColors.current.onBackground,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                    Text(
                        text = track.artist ?: "",
                        color = LocalAppColors.current.secondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            // Переходим всегда — ArtistProfileViewModel сам найдёт по имени если нет ID
                            navController?.navigate(
                                com.example.lumisound.navigation.MainDestination.Artist()
                                    .createRoute(track.artistId, track.artist ?: "", track.artistImageUrl)
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── Плавающий комментарий ──────────────────────────────
                val floatingComment = if (floatingCommentIndex >= 0 && floatingCommentIndex < reviewState.comments.size)
                    reviewState.comments[floatingCommentIndex] else null
                val commentAlpha by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (showFloatingComment && floatingComment != null) 1f else 0f,
                    animationSpec = androidx.compose.animation.core.tween(600),
                    label = "commentAlpha"
                )

                Box(
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (floatingComment != null) {
                        Row(
                            modifier = Modifier
                                .alpha(commentAlpha)
                                .background(LocalAppColors.current.surface.copy(alpha = 0.9f), RoundedCornerShape(18.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(20.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!floatingComment.userAvatarUrl.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(floatingComment.userAvatarUrl).crossfade(false).build(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.MusicNote, null, tint = LocalAppColors.current.secondary, modifier = Modifier.size(10.dp))
                                }
                            }
                            Text(
                                text = floatingComment.comment.take(45) + if (floatingComment.comment.length > 45) "..." else "",
                                color = LocalAppColors.current.onBackground,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ── Waveform прогресс-бар ──────────────────────────────
                val progress = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
                val inactiveColor = Color.White.copy(alpha = 0.18f)
                val barCount = waveform.size

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .pointerInput(duration) {
                            detectTapGestures { offset ->
                                if (duration > 0) {
                                    val seekProgress = (offset.x / size.width).coerceIn(0f, 1f)
                                    viewModel.seekTo((seekProgress * duration).toLong())
                                }
                            }
                        }
                        .pointerInput(duration) {
                            detectHorizontalDragGestures { change, _ ->
                                if (duration > 0) {
                                    val seekProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                                    viewModel.seekTo((seekProgress * duration).toLong())
                                    change.consume()
                                }
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val totalWidth = size.width
                        val totalHeight = size.height
                        val barWidth = (totalWidth / barCount) * 0.55f
                        val gap = (totalWidth / barCount) * 0.45f
                        val progressX = totalWidth * progress
                        waveform.forEachIndexed { i, amplitude ->
                            val x = i * (barWidth + gap)
                            val barHeight = (amplitude * totalHeight * 0.85f).coerceAtLeast(3.dp.toPx())
                            val top = (totalHeight - barHeight) / 2f
                            val isPlayed = (x + barWidth / 2) <= progressX
                            drawRoundRect(
                                color = if (isPlayed) GradientStart else inactiveColor,
                                topLeft = Offset(x, top),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(barWidth / 2)
                            )
                        }
                    }
                }

                // Маркер + таймер
                Box(modifier = Modifier.fillMaxWidth()) {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(8.dp)) {
                        val markerFraction = progress.coerceIn(0f, 1f)
                        val markerOffsetDp = (maxWidth - 3.dp) * markerFraction
                        Box(modifier = Modifier.padding(start = markerOffsetDp).size(width = 3.dp, height = 8.dp).background(Color.White, RoundedCornerShape(2.dp)))
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val textWidthDp = 32.dp
                    val leftOffset = ((maxWidth - textWidthDp) * progress.coerceIn(0f, 1f)).coerceIn(0.dp, maxWidth - textWidthDp)
                    Text(text = formatTime(currentPosition), color = Color.White, fontSize = 11.sp, maxLines = 1, modifier = Modifier.padding(start = leftOffset))
                    Text(text = formatTime(duration), color = LocalAppColors.current.secondary, fontSize = 11.sp, maxLines = 1, modifier = Modifier.align(Alignment.CenterEnd))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Кнопки управления — внизу ──────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(44.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showAddToPlaylist = true }, contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = LocalAppColors.current.secondary, modifier = Modifier.size(24.dp))
                    }
                    Box(modifier = Modifier.size(56.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { viewModel.previousTrack() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.SkipPrevious, "Previous", tint = LocalAppColors.current.onBackground, modifier = Modifier.size(36.dp))
                    }
                    Box(
                        modifier = Modifier.size(72.dp).shadow(elevation = 20.dp, shape = CircleShape, spotColor = GradientStart.copy(alpha = 0.6f)).background(brush = Brush.linearGradient(listOf(GradientStart, GradientEnd)), shape = CircleShape).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { viewModel.togglePlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = if (isPlaying) "Pause" else "Play", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                    Box(modifier = Modifier.size(56.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { viewModel.nextTrack() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.SkipNext, "Next", tint = LocalAppColors.current.onBackground, modifier = Modifier.size(36.dp))
                    }
                    Box(modifier = Modifier.size(44.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        navController?.navigate(com.example.lumisound.navigation.MainDestination.Review().createRoute(track.id, track.name, track.artist ?: ""))
                    }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.ChatBubbleOutline, "Review", tint = LocalAppColors.current.secondary, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }

        // Зона свайпа вниз для закрытия — от самого верха, но с отступами по бокам
        // чтобы не перекрывать кнопку закрытия (слева) и троеточие (справа)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .align(Alignment.TopCenter)
                .padding(start = 60.dp, end = 60.dp) // оставляем место для кнопок хедера
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { isDraggingToClose = true },
                        onDragEnd = {
                            if (closeProgress >= 0.4f) {
                                // Выставляем closeProgress=1f (плеер полностью скрыт)
                                // isDraggingToClose остаётся true — effectiveCloseProgress = closeProgress
                                closeProgress = 1f
                                // isClosing запустит LaunchedEffect: ждёт 2 кадра → onClose()
                                isClosing = true
                            } else {
                                isDraggingToClose = false
                                closeProgress = 0f
                                targetCloseProgress = 0f
                            }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            if (dragAmount > 0) {
                                closeProgress = (closeProgress + dragAmount / maxDragPx).coerceIn(0f, 1f)
                                change.consume()
                            } else if (dragAmount < 0 && closeProgress > 0f) {
                                closeProgress = (closeProgress + dragAmount / maxDragPx).coerceIn(0f, 1f)
                                change.consume()
                            }
                        }
                    )
                }
        )

        // Шторка добавления в плейлист
        if (showAddToPlaylist) {
            com.example.lumisound.feature.playlist.AddToPlaylistOverlay(
                track = track,
                onDismiss = { showAddToPlaylist = false }
            )
        }

        // Информация о треке
        if (showTrackInfo) {
            TrackInfoSheet(track = track, onDismiss = { showTrackInfo = false })
        }
    } // закрываем корневой Box
}

@Composable
private fun PlayerPageContent(track: com.example.lumisound.data.model.Track, context: android.content.Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalAppColors.current.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Хедер-заглушка (чтобы высота совпадала с основным экраном)
        Spacer(modifier = Modifier.height(72.dp))
        // Обложка
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
        ) {
            val imageUrl = track.hdImageUrl ?: track.imageUrl
            if (!imageUrl.isNullOrEmpty()) {
                coil.compose.AsyncImage(
                    model = coil.request.ImageRequest.Builder(context).data(imageUrl).crossfade(false).build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(LocalAppColors.current.surface), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MusicNote, null, tint = GradientStart.copy(alpha = 0.4f), modifier = Modifier.size(72.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(track.name, color = LocalAppColors.current.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        Spacer(modifier = Modifier.height(6.dp))
        Text(track.artist, color = LocalAppColors.current.secondary, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
private fun TrackInfoSheet(track: Track, onDismiss: () -> Unit) {
    // Загружаем полные данные трека через API
    val audiusApi: com.example.lumisound.data.remote.AudiusApiService = androidx.hilt.navigation.compose.hiltViewModel<com.example.lumisound.feature.nowplaying.PlayerViewModel>().audiusApi
    var fullTrack by remember { mutableStateOf<com.example.lumisound.data.remote.AudiusTrack?>(null) }
    var fullArtist by remember { mutableStateOf<com.example.lumisound.data.remote.AudiusArtistFull?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(track.id) {
        isLoading = true
        // Загружаем трек и артиста параллельно
        val trackResult = audiusApi.getTrackById(track.id)
        fullTrack = trackResult.getOrNull()
        val artistId = fullTrack?.artist?.id ?: track.artistId
        if (!artistId.isNullOrBlank()) {
            fullArtist = audiusApi.getArtist(artistId).getOrNull()
        }
        isLoading = false
    }

    Box(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
        // Затемнение
        Box(
            modifier = androidx.compose.ui.Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() }
        )
        // Шторка
        Column(
            modifier = androidx.compose.ui.Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .background(Color(0xFF1A1A1A), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .navigationBarsPadding()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
        ) {
            // Ручка
            Box(
                modifier = androidx.compose.ui.Modifier
                    .padding(top = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))

            // Обложка + название
            Row(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = androidx.compose.ui.Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)).background(LocalAppColors.current.surface)
                ) {
                    val coverUrl = track.hdImageUrl ?: track.imageUrl
                    if (!coverUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(coverUrl).crossfade(false).build(),
                            contentDescription = null,
                            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(modifier = androidx.compose.ui.Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.MusicNote, null, tint = GradientStart.copy(alpha = 0.5f), modifier = androidx.compose.ui.Modifier.size(32.dp))
                        }
                    }
                }
                Column(modifier = androidx.compose.ui.Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(track.name, color = LocalAppColors.current.onBackground, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Text(track.artist, color = GradientStart, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    if (!track.genre.isNullOrBlank()) {
                        Text(track.genre, color = LocalAppColors.current.secondary, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
            Box(modifier = androidx.compose.ui.Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.06f)))

            if (isLoading) {
                Box(modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator(color = GradientStart, modifier = androidx.compose.ui.Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // ── Трек ──
                    item {
                        TrackInfoSection(title = "О треке") {
                            if ((track.duration ?: 0) > 0) {
                                val d = track.duration!!
                                val h = d / 3600
                                val m = (d % 3600) / 60
                                val s = (d % 60).toString().padStart(2, '0')
                                val formatted = if (h > 0) "$h:${m.toString().padStart(2,'0')}:$s" else "$m:$s"
                                TrackInfoRow(label = "Длительность", value = formatted)
                            }
                            val genre = fullTrack?.genre ?: track.genre
                            if (!genre.isNullOrBlank()) TrackInfoRow(label = "Жанр", value = genre)
                            fullTrack?.mood?.takeIf { it.isNotBlank() }?.let {
                                TrackInfoRow(label = "Настроение", value = it)
                            }
                            fullTrack?.tags?.takeIf { it.isNotBlank() }?.let { tags ->
                                val cleaned = tags.split(",").map { it.trim() }.filter { it.isNotBlank() }.joinToString(" · ")
                                if (cleaned.isNotBlank()) TrackInfoRow(label = "Теги", value = cleaned)
                            }
                            fullTrack?.releaseDate?.takeIf { it.isNotBlank() }?.let { date ->
                                TrackInfoRow(label = "Дата релиза", value = date.substringBefore("T").ifBlank { date })
                            }
                            fullTrack?.description?.takeIf { it.isNotBlank() }?.let { desc ->
                                Spacer(modifier = androidx.compose.ui.Modifier.height(4.dp))
                                Text(
                                    desc.take(300) + if (desc.length > 300) "..." else "",
                                    color = LocalAppColors.current.secondary, fontSize = 13.sp, lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    // ── Артист ──
                    if (fullArtist != null || !track.artistId.isNullOrBlank()) {
                        item {
                            TrackInfoSection(title = "Об артисте") {
                                // Аватарка + имя
                                val artistAvatar = fullArtist?.profilePicture?.let {
                                    com.example.lumisound.data.remote.AudiusApiServiceHelper.getProfilePictureUrl(it)
                                } ?: track.artistImageUrl
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = androidx.compose.ui.Modifier.padding(bottom = 8.dp)
                                ) {
                                    Box(modifier = androidx.compose.ui.Modifier.size(40.dp).clip(CircleShape).background(LocalAppColors.current.surface)) {
                                        if (!artistAvatar.isNullOrEmpty()) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current).data(artistAvatar).crossfade(false).build(),
                                                contentDescription = null,
                                                modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(modifier = androidx.compose.ui.Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.MusicNote, null, tint = LocalAppColors.current.secondary, modifier = androidx.compose.ui.Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(track.artist, color = LocalAppColors.current.onBackground, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                            if (fullArtist?.isVerified == true) {
                                                Icon(Icons.Default.CheckCircle, null, tint = GradientStart, modifier = androidx.compose.ui.Modifier.size(14.dp))
                                            }
                                        }
                                        fullArtist?.location?.takeIf { it.isNotBlank() }?.let {
                                            Text(it, color = LocalAppColors.current.secondary, fontSize = 12.sp)
                                        }
                                    }
                                }
                                fullArtist?.bio?.takeIf { it.isNotBlank() }?.let { bio ->
                                    Text(
                                        bio.take(200) + if (bio.length > 200) "..." else "",
                                        color = LocalAppColors.current.secondary, fontSize = 13.sp,
                                        modifier = androidx.compose.ui.Modifier.padding(bottom = 8.dp)
                                    )
                                }
                                fullArtist?.followerCount?.takeIf { it > 0 }?.let { followers ->
                                    val fmt = when {
                                        followers >= 1_000_000 -> "${String.format("%.1f", followers / 1_000_000f)}M"
                                        followers >= 1_000 -> "${String.format("%.1f", followers / 1_000f)}K"
                                        else -> "$followers"
                                    }
                                    TrackInfoRow(label = "Подписчиков", value = fmt)
                                }
                                fullArtist?.trackCount?.takeIf { it > 0 }?.let {
                                    TrackInfoRow(label = "Треков", value = "$it")
                                }
                                fullArtist?.twitterHandle?.takeIf { it.isNotBlank() }?.let {
                                    TrackInfoRow(label = "Twitter", value = "@$it")
                                }
                                fullArtist?.instagramHandle?.takeIf { it.isNotBlank() }?.let {
                                    TrackInfoRow(label = "Instagram", value = "@$it")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackInfoSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = LocalAppColors.current.secondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp, modifier = androidx.compose.ui.Modifier.padding(bottom = 2.dp))
        content()
    }
}

@Composable
private fun TrackInfoRow(label: String, value: String) {
    Row(
        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = LocalAppColors.current.secondary, fontSize = 13.sp)
        Text(value, color = LocalAppColors.current.onBackground, fontSize = 13.sp, fontWeight = FontWeight.Medium,
            maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = androidx.compose.ui.Modifier.weight(1f, fill = false).padding(start = 16.dp))
    }
}

private fun formatCount(count: Int): String = when {
    count >= 1_000_000 -> "${String.format("%.1f", count / 1_000_000f)}M"
    count >= 1_000 -> "${String.format("%.1f", count / 1_000f)}K"
    else -> "$count"
}
