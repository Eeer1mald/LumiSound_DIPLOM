package com.example.lumisound.feature.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.lumisound.ui.theme.GradientEnd
import com.example.lumisound.ui.theme.GradientStart
import kotlin.math.abs

@Composable
fun MiniPlayer(
    currentTrack: Track?,
    isPlaying: Boolean,
    progress: Float, // от 0 до 1
    onPlayPauseClick: () -> Unit,
    onTrackClick: () -> Unit,
    onAddClick: () -> Unit = {},
    onLikeClick: () -> Unit = {},
    isLiked: Boolean = false,
    animationProgress: Float = 0f, // Прогресс анимации от 0 до 1
    onAnimationProgressChange: (Float) -> Unit = {},
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (currentTrack == null) {
        return
    }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    // Вычисляем масштаб и позицию на основе прогресса анимации
    // Используем нелинейную функцию для более плавной анимации
    val easedProgress = remember(animationProgress) {
        // Ease-out функция для более естественной анимации
        val t = animationProgress.coerceIn(0f, 1f)
        1f - (1f - t) * (1f - t)
    }
    
    val scale = remember(easedProgress) {
        // Масштаб от 1.0 до ~2.5 (мини-плеер увеличивается)
        1f + (easedProgress * 1.5f).coerceIn(0f, 1.5f)
    }
    
    val alpha = remember(animationProgress) {
        // Прозрачность быстро уменьшается при анимации (мини-плеер исчезает)
        // Начинаем скрывать после 20% прогресса
        if (animationProgress < 0.2f) {
            1f
        } else {
            (1f - ((animationProgress - 0.2f) / 0.8f)).coerceIn(0f, 1f)
        }
    }
    
    // Вычисляем смещение вверх на основе прогресса
    val offsetY = remember(animationProgress, screenHeightPx) {
        // Смещаем мини-плеер вверх при анимации
        -easedProgress * screenHeightPx * 0.25f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .scale(scale)
            .alpha(alpha)
            .offset(y = with(density) { offsetY.toDp() })
            .background(
                color = ColorSurface, // Тёмно-серый вместо 0xFF343639
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = Color(0xFF1F1F1F).copy(alpha = 0.5f), // Тёмно-серый вместо 0xFF2A2D3E
                shape = RoundedCornerShape(20.dp)
            )
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color.Black.copy(alpha = 0.3f)
            )
            .pointerInput(Unit) {
                var totalDrag = 0f
                var startY = 0f
                
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        startY = offset.y
                        onDragStart()
                    },
                    onDragEnd = {
                        onDragEnd()
                        totalDrag = 0f
                    },
                    onVerticalDrag = { change, dragAmount ->
                        if (dragAmount < 0) { // Свайп вверх (отрицательное значение)
                            totalDrag += abs(dragAmount)
                            // Вычисляем прогресс на основе свайпа (максимум 300px для полного открытия)
                            val newProgress = (totalDrag / 300f).coerceIn(0f, 1f)
                            onAnimationProgressChange(newProgress)
                            change.consume()
                        }
                    }
                )
            }
            .clickable { 
                if (animationProgress == 0f) {
                    onTrackClick()
                }
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
                // Progress ring (orange)
                val animatedProgress by animateFloatAsState(
                    targetValue = progress,
                    label = "progress"
                )
                
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val strokeWidth = 4.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    
                    // Background ring (grey)
                    drawCircle(
                        color = Color(0xFF1F1F1F), // Тёмно-серый вместо 0xFF2A2D3E
                        radius = radius,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        center = center
                    )
                    
                    // Progress ring (orange)
                    if (animatedProgress > 0f) {
                        drawArc(
                            color = Color(0xFFFF5C6C),
                            startAngle = -90f,
                            sweepAngle = animatedProgress * 360f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                            topLeft = androidx.compose.ui.geometry.Offset(
                                center.x - radius,
                                center.y - radius
                            ),
                            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                        )
                    }
                }
                
                // Play button (white circle with black triangle/pause)
                // Показываем паузу с прогрессом, если трек уже играл (progress > 0) или сейчас играет
                val shouldShowPause = isPlaying || progress > 0f
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
                        imageVector = if (shouldShowPause) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (shouldShowPause) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(if (shouldShowPause) 18.dp else 20.dp)
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
                // Add button
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Add",
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onAddClick() }
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
