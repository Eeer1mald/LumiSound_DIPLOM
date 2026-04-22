package com.example.lumisound.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.lumisound.feature.home.TrackPreview
import com.example.lumisound.ui.theme.GradientStart
import com.example.lumisound.ui.theme.LocalAppColors

@Composable
fun TrackCard(
    track: TrackPreview,
    modifier: Modifier = Modifier,
    onClick: (TrackPreview) -> Unit,
    testTag: String
) {
    val cardColor = LocalAppColors.current.surface

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
                .background(color = cardColor)
                .border(
                    width = 1.dp,
                    color = Color(0xFF1F1F1F).copy(alpha = 0.3f),
                    shape = RoundedCornerShape(18.dp)
                )
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = { onClick(track) }
                )
        ) {
            if (track.coverUrl != null && track.coverUrl.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(track.coverUrl)
                        .crossfade(false)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
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

        Text(
            text = track.title,
            style = MaterialTheme.typography.bodyMedium.copy(color = LocalAppColors.current.onBackground),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (track.artist.isNotEmpty()) {
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodySmall.copy(color = LocalAppColors.current.secondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

