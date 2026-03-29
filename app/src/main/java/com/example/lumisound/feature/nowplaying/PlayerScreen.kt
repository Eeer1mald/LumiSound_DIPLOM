package com.example.lumisound.feature.nowplaying

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    track: Track,
    onClose: () -> Unit = {},
    previousRoute: String? = null,
    navController: androidx.navigation.NavHostController? = null,
    userName: String = "Александр",
    viewModel: PlayerViewModel = hiltViewModel()
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
    var isLiked by remember { mutableStateOf(false) }

    LaunchedEffect(track.id) { viewModel.syncPlayerState() }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val maxDragPx = screenHeightPx * 0.6f

    var closeProgress by remember { mutableStateOf(0f) }
    var isDraggingToClose by remember { mutableStateOf(false) }
    var targetCloseProgress by remember { mutableStateOf(0f) }

    val animatedCloseProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = targetCloseProgress,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "closeProgress"
    )
    val effectiveCloseProgress = if (isDraggingToClose) closeProgress else animatedCloseProgress
    val contentOffsetY = screenHeightPx * 0.6f * effectiveCloseProgress.coerceIn(0f, 1f)
    val contentAlpha = (1f - effectiveCloseProgress.coerceIn(0f, 1f)).coerceIn(0f, 1f)

    val closeScope = rememberCoroutineScope()
    val stablePreviousRoute = remember(previousRoute) { previousRoute }

    Box(modifier = Modifier.fillMaxSize()) {

        // Предыдущий экран при закрытии
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

        // Основной контент плеера
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = with(density) { contentOffsetY.toDp() })
                .alpha(contentAlpha)
                .background(ColorBackground.copy(alpha = (1f - effectiveCloseProgress * 0.5f).coerceIn(0.5f, 1f)))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ── Хедер ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Кнопка закрытия — слева, выровнена по центру хедера
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(40.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.08f),
                            shape = CircleShape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            isDraggingToClose = false
                            targetCloseProgress = 1f
                            closeScope.launch {
                                delay(280)
                                onClose()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Close",
                        tint = ColorOnBackground,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Заголовок — по центру
                Text(
                    text = "Сейчас играет",
                    color = ColorSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // ── Обложка ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .shadow(
                        elevation = 32.dp,
                        shape = RoundedCornerShape(20.dp),
                        spotColor = Color.Black.copy(alpha = 0.7f)
                    )
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
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ColorSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = GradientStart.copy(alpha = 0.4f),
                            modifier = Modifier.size(72.dp)
                        )
                    }
                }
            }

            // ── Нижняя панель — всегда внизу ───────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Название и артист
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = track.name,
                        color = ColorOnBackground,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                    Text(
                        text = track.artist ?: "",
                        color = ColorSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Прогресс-бар
                val progress = if (duration > 0) {
                    (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                } else 0f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(3.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    listOf(GradientStart, GradientEnd)
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = formatTime(currentPosition), color = ColorSecondary, fontSize = 11.sp)
                    Text(text = formatTime(duration), color = ColorSecondary, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Кнопки управления
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Like
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { isLiked = !isLiked },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) GradientStart else ColorSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Previous — без фона, просто иконка
                    Box(
                        modifier = Modifier
                            .size(56.dp)
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
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Play/Pause — большая круглая кнопка с градиентом
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .shadow(
                                elevation = 24.dp,
                                shape = CircleShape,
                                spotColor = GradientStart.copy(alpha = 0.7f)
                            )
                            .background(
                                brush = Brush.linearGradient(listOf(GradientStart, GradientEnd)),
                                shape = CircleShape
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
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Next — без фона, просто иконка
                    Box(
                        modifier = Modifier
                            .size(56.dp)
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
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Add
                    Box(
                        modifier = Modifier
                            .size(44.dp)
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
        }

        // Зона свайпа вниз для закрытия
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .align(Alignment.TopCenter)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            val buttonAreaWidth = with(density) { 70.dp.toPx() }
                            val buttonAreaHeight = with(density) { 80.dp.toPx() }
                            if (offset.x < buttonAreaWidth && offset.y < buttonAreaHeight) return@detectVerticalDragGestures
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
                            val buttonAreaWidth = with(density) { 70.dp.toPx() }
                            val buttonAreaHeight = with(density) { 80.dp.toPx() }
                            if (change.position.x < buttonAreaWidth && change.position.y < buttonAreaHeight) return@detectVerticalDragGestures
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
    }
}
