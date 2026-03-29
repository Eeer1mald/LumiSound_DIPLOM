package com.example.lumisound.feature.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.lumisound.feature.home.TrackPreview
import com.example.lumisound.ui.theme.ColorAccentSecondary
import com.example.lumisound.ui.theme.ColorOnBackground
import com.example.lumisound.ui.theme.ColorSecondary
import com.example.lumisound.ui.theme.ColorSurface
import com.example.lumisound.ui.theme.GradientEnd
import com.example.lumisound.ui.theme.GradientStart

@Composable
fun TrackCard(
    track: TrackPreview,
    modifier: Modifier = Modifier,
    onClick: (TrackPreview) -> Unit,
    testTag: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val alpha by animateFloatAsState(
        targetValue = if (isHovered) 1f else 0f,
        animationSpec = tween(150), // Уменьшено в 2 раза: было 300, стало 150
        label = "play_button_alpha"
    )

    // Заменены градиенты на однотонные цвета
    val cardColor = remember {
        ColorSurface // Тёмно-серый вместо градиента
    }
    val playButtonColor = remember {
        GradientStart // Однотонный акцентный цвет
    }
    Column(
        modifier = modifier
            .width(140.dp)
            .testTag(testTag),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(140.dp)
                .height(140.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(color = cardColor) // Однотонный цвет вместо градиента
                .border(
                    width = 1.dp,
                    color = if (isHovered) {
                        GradientStart.copy(alpha = 0.4f)
                    } else {
                        Color(0xFF1F1F1F).copy(alpha = 0.3f) // Тёмно-серый вместо 0xFF2A2D3E
                    },
                    shape = RoundedCornerShape(18.dp)
                )
                .shadow(
                    elevation = if (isHovered) 8.dp else 4.dp,
                    shape = RoundedCornerShape(18.dp),
                    spotColor = if (isHovered) GradientStart.copy(alpha = 0.2f) else Color.Transparent
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { onClick(track) }
                )
        ) {
            // Cover image or placeholder
            Box(modifier = Modifier.fillMaxSize()) {
                if (track.coverUrl != null && track.coverUrl.isNotEmpty()) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(track.coverUrl)
                            .crossfade(false) // Отключено для лучшей производительности
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = GradientStart.copy(alpha = 0.2f),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = GradientStart.copy(alpha = 0.4f),
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        },
                        success = {
                            SubcomposeAsyncImageContent()
                        }
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = GradientStart.copy(alpha = 0.4f),
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center)
                    )
                }
            }

            // Заменён градиент на однотонный цвет с прозрачностью
            val overlayColor = remember(alpha) {
                GradientStart.copy(alpha = alpha * 0.15f) // Однотонный цвет вместо градиента
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = overlayColor)
            )

            // Play button on hover
            if (alpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = playButtonColor, // Однотонный цвет вместо градиента
                                shape = CircleShape
                            )
                            .shadow(
                                elevation = 8.dp,
                                shape = CircleShape,
                                spotColor = GradientStart.copy(alpha = 0.4f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier
                                .size(24.dp)
                                .offset(x = 2.dp)
                        )
                    }
                }
            }
        }

        Text(
            text = track.title,
            style = MaterialTheme.typography.bodyMedium.copy(color = ColorOnBackground),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (track.artist.isNotEmpty()) {
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodySmall.copy(color = ColorSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

