package com.example.lumisound.feature.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
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
import com.example.lumisound.ui.theme.ColorOnBackground
import com.example.lumisound.ui.theme.ColorSecondary
import com.example.lumisound.ui.theme.ColorSurface
import com.example.lumisound.ui.theme.GradientStart
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .graphicsLayer {
                translationY = miniPlayerOffsetY
                this.alpha = alpha
            }
            .background(color = ColorSurface.copy(alpha = 0.8f), shape = RoundedCornerShape(20.dp))
            .border(width = 1.dp, color = Color(0xFF1F1F1F).copy(alpha = 0.5f), shape = RoundedCornerShape(20.dp))
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.3f))
            // Свайп вверх для открытия плеера
            .pointerInput(Unit) {
                val velocityTracker = VelocityTracker()
                detectVerticalDragGestures(
                    onDragStart = {
                        totalDrag = 0f
                        velocityTracker.resetTracking()
                        onDragStart()
                    },
                    onDragEnd = {
                        val vel = velocityTracker.calculateVelocity()
                        val upVelocity = (-vel.y / 1000f).coerceAtLeast(0f)
                        onDragVelocityChange(upVelocity)
                        onDragEnd()
                        totalDrag = 0f
                    },
                    onDragCancel = {
                        onDragVelocityChange(0f)
                        onDragEnd()
                        totalDrag = 0f
                    },
                    onVerticalDrag = { change, dragAmount ->
                        velocityTracker.addPointerInputChange(change)
                        if (dragAmount < 0) {
                            totalDrag += abs(dragAmount)
                            onAnimationProgressChange((totalDrag / screenHeightPx).coerceIn(0f, 1f))
                            change.consume()
                        } else if (dragAmount > 0 && totalDrag > 0) {
                            totalDrag = (totalDrag - dragAmount).coerceAtLeast(0f)
                            onAnimationProgressChange((totalDrag / screenHeightPx).coerceIn(0f, 1f))
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
                    color = ColorOnBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentTrack.artist,
                    color = ColorSecondary,
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
                
                // Like button (orange heart when liked)
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = if (isLiked) "Unlike" else "Like",
                    tint = if (isLiked) Color(0xFFFF5C6C) else Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onLikeClick() }
                )
            }
        }
    }
}
