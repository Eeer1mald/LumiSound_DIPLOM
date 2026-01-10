package com.example.lumisound.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lumisound.feature.home.TrackPreview
import com.example.lumisound.ui.theme.ColorSurface
import com.example.lumisound.ui.theme.ColorOnSurface

@Composable
fun RecommendationCard(
    track: TrackPreview,
    modifier: Modifier = Modifier,
    onClick: (TrackPreview) -> Unit,
    testTag: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable { onClick(track) }
    ) {
        Box(
            modifier = Modifier
                .height(140.dp)
                .fillMaxWidth()
                .background(color = ColorSurface, shape = RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_media_play),
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
        }
        Text(text = track.title, style = MaterialTheme.typography.bodyMedium, color = ColorOnSurface)
        Text(text = track.artist, style = MaterialTheme.typography.bodySmall, color = ColorOnSurface.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}


