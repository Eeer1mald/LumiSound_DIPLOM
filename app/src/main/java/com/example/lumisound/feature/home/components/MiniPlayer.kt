package com.example.lumisound.feature.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lumisound.data.model.Track
import com.example.lumisound.ui.theme.GradientStart
import com.example.lumisound.ui.theme.LocalAppColors
import kotlin.math.abs

@Composable
fun MiniPlayer(
    currentTrack: Track?,
    isPlaying: Boolean,
    progress: Float,
    onPlayPauseClick: () -> Unit,
    onTrackClick: () -> Unit,
    onAddClick: () -> Unit = {},
    onLikeClick: () -> Unit = {},
    onArtistClick: ((artistId: String?, artistName: String, artistImageUrl: String?) -> Unit)? = null,
    onNextTrack: (() -> Unit)? = null,
    onPreviousTrack: (() -> Unit)? = null,
    hasPrevious: Boolean = false,
    nextTrackInfo: com.example.lumisound.data.model.Track? = null,
    prevTrackInfo: com.example.lumisound.data.model.Track? = null,
    avgScore: Float? = null,
    isLiked: Boolean = false,
    animationProgress: Float = 0f,
    onAnimationProgressChange: (Float) -> Unit = {},
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragVelocityChange: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (currentTrack == null) {
        return
    }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val alpha = (1f - animationProgress.coerceIn(0f, 1f)).coerceIn(0f, 1f)

    val miniPlayerTotalHeightPx = with(density) { (72.dp + 16.dp).toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val miniPlayerTravelPx = with(density) {
        val bottomBarHeightPx = 56.dp.toPx()
        (screenHeightPx - miniPlayerTotalHeightPx - bottomBarHeightPx).coerceAtLeast(0f)
    }
    val miniPlayerOffsetY = -(miniPlayerTravelPx * animationProgress.coerceIn(0f, 1f))

    // Локальный аккумулятор драга — не вызывает рекомпозицию
    var totalDrag by remember { mutableFloatStateOf(0f) }

    // rememberUpdatedState — гарантирует что pointerInput(Unit) всегда видит актуальные колбэки
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnDragVelocityChange by rememberUpdatedState(onDragVelocityChange)
    val currentOnAnimationProgressChange by rememberUpdatedState(onAnimationProgressChange)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentScreenHeightPx by rememberUpdatedState(screenHeightPx)

    // Горизонтальный свайп — Animatable для плавного контроля
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val hSwipeAnim = remember { Animatable(0f) }
    val currentOnNextTrack by rememberUpdatedState(onNextTrack)
    val currentOnPreviousTrack by rememberUpdatedState(onPreviousTrack)
    val currentHasPrevious by rememberUpdatedState(hasPrevious)
    val scope = rememberCoroutineScope()
    val hSwipeOffset = hSwipeAnim.value

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .graphicsLayer {
                translationY = miniPlayerOffsetY
                this.alpha = alpha
            }
            .background(color = LocalAppColors.current.surface.copy(alpha = 0.8f), shape = RoundedCornerShape(20.dp))
            .border(width = 1.dp, color = Color(0xFF1F1F1F).copy(alpha = 0.5f), shape = RoundedCornerShape(20.dp))
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.3f))
            // Свайп вверх для открытия плеера
            .pointerInput(Unit) {
                val velocityTracker = VelocityTracker()
                detectVerticalDragGestures(
                    onDragStart = {
                        totalDrag = 0f
                        velocityTracker.resetTracking()
                        currentOnDragStart()
                    },
                    onDragEnd = {
                        val vel = velocityTracker.calculateVelocity()
                        val upVelocity = (-vel.y / 1000f).coerceAtLeast(0f)
                        currentOnDragVelocityChange(upVelocity)
                        currentOnDragEnd()
                        totalDrag = 0f
                    },
                    onDragCancel = {
                        currentOnDragVelocityChange(0f)
                        currentOnDragEnd()
                        totalDrag = 0f
                    },
                    onVerticalDrag = { change, dragAmount ->
                        velocityTracker.addPointerInputChange(change)
                        if (dragAmount < 0) {
                            totalDrag += abs(dragAmount)
                            currentOnAnimationProgressChange((totalDrag / currentScreenHeightPx).coerceIn(0f, 1f))
                            change.consume()
                        } else if (dragAmount > 0 && totalDrag > 0) {
                            totalDrag = (totalDrag - dragAmount).coerceAtLeast(0f)
                            currentOnAnimationProgressChange((totalDrag / currentScreenHeightPx).coerceIn(0f, 1f))
                            change.consume()
                        }
                    }
                )
            }
            // Тап для открытия плеера
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    if (animationProgress < 0.05f) onTrackClick()
                })
            }
    ) {
        // Горизонтальный свайп — весь контент мини-плеера двигается
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Предыдущий трек — виден слева при свайпе вправо
            if (prevTrackInfo != null && hSwipeOffset > 0) {
                MiniTrackGhost(
                    track = prevTrackInfo,
                    offsetX = hSwipeOffset - screenWidthPx
                )
            }
            // Следующий трек — виден справа при свайпе влево
            if (nextTrackInfo != null && hSwipeOffset < 0) {
                MiniTrackGhost(
                    track = nextTrackInfo,
                    offsetX = hSwipeOffset + screenWidthPx
                )
            }
            // Текущий контент
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(x = hSwipeOffset.toInt(), y = 0) }
                    .pointerInput(Unit) {
                        val velocityTracker = VelocityTracker()
                        detectHorizontalDragGestures(
                            onDragStart = { velocityTracker.resetTracking() },
                            onDragEnd = {
                                val velocity = velocityTracker.calculateVelocity().x
                                val offset = hSwipeAnim.value
                                val threshold = screenWidthPx * 0.2f
                                val flingThreshold = 600f
                                scope.launch {
                                    when {
                                        offset < -threshold || velocity < -flingThreshold -> {
                                            hSwipeAnim.animateTo(
                                                -screenWidthPx,
                                                SpringSpec(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                                            )
                                            currentOnNextTrack?.invoke()
                                            hSwipeAnim.snapTo(0f)
                                        }
                                        (offset > threshold || velocity > flingThreshold) && currentHasPrevious -> {
                                            hSwipeAnim.animateTo(
                                                screenWidthPx,
                                                SpringSpec(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                                            )
                                            currentOnPreviousTrack?.invoke()
                                            hSwipeAnim.snapTo(0f)
                                        }
                                        else -> {
                                            hSwipeAnim.animateTo(
                                                0f,
                                                SpringSpec(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                                            )
                                        }
                                    }
                                }
                            },
                            onDragCancel = {
                                scope.launch {
                                    hSwipeAnim.animateTo(0f, SpringSpec(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                velocityTracker.addPointerInputChange(change)
                                val newOffset = hSwipeAnim.value + dragAmount
                                val clamped = when {
                                    newOffset > 0 && !currentHasPrevious -> (newOffset * 0.15f).coerceAtMost(20f)
                                    newOffset < 0 && nextTrackInfo == null -> (newOffset * 0.15f).coerceAtLeast(-20f)
                                    else -> newOffset
                                }
                                scope.launch { hSwipeAnim.snapTo(clamped) }
                            }
                        )
                    }
            ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play button with progress ring
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 4.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    drawCircle(
                        color = Color(0xFF1F1F1F),
                        radius = radius,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        center = center
                    )
                    if (progress > 0f) {
                        drawArc(
                            color = Color(0xFFFF5C6C),
                            startAngle = -90f,
                            sweepAngle = progress * 360f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2)
                        )
                    }
                }
                
                // Иконка зависит только от isPlaying
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = Color.White,
                            shape = CircleShape
                        )
                        .clickable { onPlayPauseClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(if (isPlaying) 18.dp else 20.dp)
                    )
                }
            }

            // Track info (center)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = currentTrack.name,
                    color = LocalAppColors.current.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentTrack.artist,
                    color = LocalAppColors.current.secondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Action buttons (right)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Add button — переход на профиль артиста
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Artist profile",
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            onArtistClick?.invoke(
                                currentTrack.artistId,
                                currentTrack.artist,
                                currentTrack.artistImageUrl
                            )
                        }
                )
                
                // Score badge — оценка или звёздочка
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            color = if (avgScore != null) GradientStart.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .border(
                            1.dp,
                            if (avgScore != null) GradientStart.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.12f),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (avgScore != null) {
                        Text(
                            text = String.format("%.1f", avgScore),
                            color = GradientStart,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = LocalAppColors.current.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        } // закрываем Box горизонтального свайпа
        } // закрываем внешний Box
    }
}

@Composable
private fun MiniTrackGhost(
    track: com.example.lumisound.data.model.Track,
    offsetX: Float
) {
    Box(
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .offset { IntOffset(x = offsetX.toInt(), y = 0) }
            .background(
                color = androidx.compose.ui.graphics.Color(0xFF1A1A1A),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Обложка
            Box(
                modifier = androidx.compose.ui.Modifier
                    .size(48.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                    .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.08f))
            ) {
                if (!track.imageUrl.isNullOrEmpty()) {
                    coil.compose.AsyncImage(
                        model = track.imageUrl,
                        contentDescription = null,
                        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
            }
            // Название и артист
            Column(
                modifier = androidx.compose.ui.Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = track.name,
                    color = LocalAppColors.current.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    color = LocalAppColors.current.secondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
