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

        // Предыдущий экран — рендерим всегда для сохранения состояния
        if (stablePreviousRoute != null && navController != null) {
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
                .background(LocalAppColors.current.background.copy(alpha = (1f - effectiveCloseProgress * 0.5f).coerceIn(0.5f, 1f)))
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
                            isDraggingToClose = false
                            closeProgress = 0f
                            targetCloseProgress = 0f
                            onClose()
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

        // Зона свайпа вниз для закрытия
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(160.dp)
                .align(Alignment.TopEnd)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { isDraggingToClose = true },
                        onDragEnd = {
                            if (closeProgress >= 0.4f) {
                                // Скрываем контент сразу, потом закрываем
                                closeProgress = 1f
                                isDraggingToClose = false
                                onClose()
                                closeProgress = 0f
                                targetCloseProgress = 0f
                            } else {
                                closeProgress = 0f
                                isDraggingToClose = false
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
    Box(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
        // Затемнение
        Box(
            modifier = androidx.compose.ui.Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
        )
        // Шторка
        Column(
            modifier = androidx.compose.ui.Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
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
                    modifier = androidx.compose.ui.Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(LocalAppColors.current.surface)
                ) {
                    if (!track.imageUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(track.imageUrl).crossfade(false).build(),
                            contentDescription = null,
                            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Column(modifier = androidx.compose.ui.Modifier.weight(1f)) {
                    Text(track.name, color = LocalAppColors.current.onBackground, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                    Text(track.artist, color = LocalAppColors.current.secondary, fontSize = 14.sp)
                }
            }

            Spacer(modifier = androidx.compose.ui.Modifier.height(20.dp))
            Box(modifier = androidx.compose.ui.Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.06f)))
            Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))

            // Информация
            Column(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!track.genre.isNullOrBlank()) {
                    TrackInfoRow(label = "Жанр", value = track.genre)
                }
                if ((track.duration ?: 0) > 0) {
                    val m = (track.duration!! / 60)
                    val s = (track.duration % 60).toString().padStart(2, '0')
                    TrackInfoRow(label = "Длительность", value = "$m:$s")
                }
                if ((track.playCount ?: 0) > 0) {
                    val count = track.playCount!!
                    val formatted = when {
                        count >= 1_000_000 -> "${count / 1_000_000}M прослушиваний"
                        count >= 1_000 -> "${count / 1_000}K прослушиваний"
                        else -> "$count прослушиваний"
                    }
                    TrackInfoRow(label = "Прослушивания", value = formatted)
                }
                if (!track.artistId.isNullOrBlank()) {
                    TrackInfoRow(label = "ID артиста", value = track.artistId)
                }
                TrackInfoRow(label = "ID трека", value = track.id)
                if (!track.trackUrl.isNullOrBlank()) {
                    TrackInfoRow(label = "Источник", value = "Audius")
                }
            }

            Spacer(modifier = androidx.compose.ui.Modifier.height(24.dp))
        }
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
