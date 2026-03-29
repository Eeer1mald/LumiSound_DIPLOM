package com.example.lumisound.feature.nowplaying

import android.app.Activity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.lumisound.data.model.Track
import com.example.lumisound.feature.home.components.BottomNavigationBar
import com.example.lumisound.ui.theme.ColorAccentSecondary
import com.example.lumisound.ui.theme.ColorBackground
import com.example.lumisound.ui.theme.ColorOnBackground
import com.example.lumisound.ui.theme.ColorSecondary
import com.example.lumisound.ui.theme.GradientEnd
import com.example.lumisound.ui.theme.GradientStart

fun formatTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000).toInt()
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return "$mins:${secs.toString().padStart(2, '0')}"
}

@Composable
fun NowPlayingScreen(
    track: Track,
    onClose: () -> Unit,
    onNavigate: (String) -> Unit = {},
    onArtistClick: (String, String?) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel(),
    onScrollStateChange: ((Int) -> Unit)? = null,
    nestedScrollConnection: androidx.compose.ui.input.nestedscroll.NestedScrollConnection? = null
) {
    val view = LocalView.current
    val context = LocalContext.current
    
    // КРИТИЧНО: Устанавливаем настройки status bar также в NowPlayingScreen
    // Это гарантирует, что настройки применяются сразу при открытии плеера
    // Используем DisposableEffect для гарантированного применения при входе
    androidx.compose.runtime.DisposableEffect(Unit) {
        if (!view.isInEditMode && context is Activity) {
            val window = context.window
            val insetsController = WindowCompat.getInsetsController(window, view)
            
            // Устанавливаем прозрачный фон для status bar
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            
            // Устанавливаем светлые иконки на темном фоне
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
            
            // Гарантируем edge-to-edge
            WindowCompat.setDecorFitsSystemWindows(window, false)
            
            onDispose {
                // Оставляем настройки при выходе
            }
        } else {
            onDispose { }
        }
    }
    
    
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    var isLiked by remember { mutableStateOf(false) }
    var userRating by remember { mutableIntStateOf(0) }
    var hoveredRating by remember { mutableIntStateOf(0) }
    
    // Синхронизируем состояние при открытии плеера, но не перезапускаем трек
    LaunchedEffect(track.id) {
        viewModel.syncPlayerState()
    }

    // Оптимизация: используем remember для градиента и scrollState
    val backgroundGradient = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1A1B2E),
                ColorBackground,
                ColorBackground
            )
        )
    }
    val scrollState = rememberScrollState()
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding() // Отступ для статус-бара
            .background(brush = backgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 80.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
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

            // Album Cover
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp)
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
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(track.hdImageUrl)
                            .crossfade(false) // Отключено для лучшей производительности
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (track.imageUrl != null && track.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(track.imageUrl)
                            .crossfade(false) // Отключено для лучшей производительности
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val placeholderGradient = remember {
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF1A1B2E), Color(0xFF16182A))
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(brush = placeholderGradient),
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
                val overlayGradient = remember {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            ColorBackground.copy(alpha = 0.4f)
                        )
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(brush = overlayGradient)
                )
            }

            // Track Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
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
                    text = track.artist,
                    color = ColorSecondary,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable {
                        onArtistClick(track.artist, track.artistImageUrl)
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Progress Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                val progress = if (duration > 0) {
                    (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                } else 0f
                val progressBarGradient = remember {
                    Brush.horizontalGradient(colors = listOf(GradientStart, GradientEnd))
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF2A2D3E))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            // TODO: Seek on click
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(4.dp)
                            .background(brush = progressBarGradient)
                    )
                }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
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

                val playButtonGradient = remember {
                    Brush.linearGradient(
                        colors = listOf(GradientStart, GradientEnd)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            brush = playButtonGradient,
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

            // Rating Section
            val ratingSectionGradient = remember {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1A1B2E).copy(alpha = 0.8f),
                        Color(0xFF16182A).copy(alpha = 0.8f)
                    )
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .background(
                        brush = ratingSectionGradient,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0xFF2A2D3E).copy(alpha = 0.4f),
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
                    val activeGradient = remember {
                        Brush.linearGradient(colors = listOf(GradientStart, GradientEnd))
                    }
                    val inactiveGradient = remember {
                        Brush.linearGradient(colors = listOf(Color(0xFF1A1B2E), Color(0xFF1A1B2E)))
                    }
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
                                        brush = if (shouldHighlight) activeGradient else inactiveGradient,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (shouldHighlight) {
                                            Color.Transparent
                                        } else {
                                            Color(0xFF2A2D3E).copy(alpha = 0.4f)
                                        },
                                        shape = RoundedCornerShape(8.dp)
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

