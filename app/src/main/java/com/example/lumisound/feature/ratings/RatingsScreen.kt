package com.example.lumisound.feature.ratings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.lumisound.data.remote.SupabaseService
import com.example.lumisound.feature.home.components.TopAppBar
import com.example.lumisound.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

data class RatedTrack(
    val id: String,
    val title: String,
    val artist: String,
    val coverUrl: String? = null,
    val rating: Int,
    val ratedAt: String
)

@Composable
fun RatingsScreen(
    navController: NavHostController,
    ratedTracks: List<RatedTrack> = emptyList(),
    onTrackClick: (String) -> Unit = {},
    viewModel: RatingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // ── Хедер ──────────────────────────────────────────────────
            item {
                TopAppBar(userName = "", onProfileClick = { navController.navigate("profile") })
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Мои рецензии", color = ColorOnBackground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(ColorSurface, CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { viewModel.load() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = ColorSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // ── Статистика ─────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        icon = { Icon(Icons.Default.Star, null, tint = ColorAccentSecondary, modifier = Modifier.size(16.dp)) },
                        label = "Оценок",
                        value = state.ratings.size.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = GradientStart, modifier = Modifier.size(16.dp)) },
                        label = "Средняя",
                        value = state.averageScore?.let { String.format("%.1f", it) } ?: "—",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = { Icon(Icons.Default.EmojiEvents, null, tint = ColorAccentSecondary, modifier = Modifier.size(16.dp)) },
                        label = "Топ (8+)",
                        value = state.topRatedCount.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = { Icon(Icons.Default.ChatBubbleOutline, null, tint = GradientEnd, modifier = Modifier.size(16.dp)) },
                        label = "Коммент.",
                        value = state.comments.size.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Табы ───────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RatingsTab.entries.forEach { tab ->
                        val isActive = state.activeTab == tab
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isActive) GradientStart else ColorSurface,
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { viewModel.setTab(tab) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(tab.label, color = if (isActive) Color.White else ColorSecondary, fontSize = 13.sp, fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }
            }

            if (state.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GradientStart, modifier = Modifier.size(36.dp))
                    }
                }
            } else when (state.activeTab) {
                RatingsTab.RATINGS -> {
                    // Фильтры по оценке
                    item {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            item {
                                FilterChip("Все", state.filterScore == null) { viewModel.setFilter(null) }
                            }
                            items(listOf(9, 8, 7, 6)) { score ->
                                FilterChip("$score+", state.filterScore == score) { viewModel.setFilter(score) }
                            }
                        }
                    }

                    if (state.filteredRatings.isEmpty()) {
                        item { EmptyState("Нет оценённых треков", "Нажмите 💬 в плеере, чтобы оценить трек") }
                    } else {
                        items(state.filteredRatings, key = { it.id }) { rating ->
                            RatingItem(rating = rating)
                        }
                    }
                }
                RatingsTab.COMMENTS -> {
                    if (state.comments.isEmpty()) {
                        item { EmptyState("Нет комментариев", "Нажмите 💬 в плеере, чтобы оставить комментарий") }
                    } else {
                        items(state.comments, key = { it.id }) { comment ->
                            CommentListItem(comment = comment)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(ColorSurface.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        icon()
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = ColorOnBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = ColorSecondary, fontSize = 9.sp)
    }
}

@Composable
private fun FilterChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (isSelected) GradientStart else ColorSurface, RoundedCornerShape(16.dp))
            .border(1.dp, if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (isSelected) Color.White else ColorSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun RatingItem(rating: SupabaseService.TrackRatingResponse) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .background(ColorSurface.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)).background(ColorSurface)
        ) {
            if (!rating.trackCoverUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(rating.trackCoverUrl).crossfade(false).build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.MusicNote, null, tint = ColorSecondary, modifier = Modifier.size(24.dp).align(Alignment.Center))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(rating.trackTitle, color = ColorOnBackground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(rating.trackArtist, color = ColorSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            rating.review?.takeIf { it.isNotBlank() }?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text("\"$it\"", color = ColorSecondary.copy(alpha = 0.8f), fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            }
            // Мини-критерии
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    "🎤" to rating.rhymeScore,
                    "⭐" to rating.imageryScore,
                    "🎨" to rating.structureScore,
                    "✨" to rating.charismaScore,
                    "🌊" to rating.atmosphereScore
                ).forEach { (emoji, score) ->
                    if (score != null) {
                        Text("$emoji$score", color = ColorSecondary, fontSize = 10.sp)
                    }
                }
            }
        }
        rating.overallScore?.let { score ->
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        brush = Brush.linearGradient(listOf(GradientStart, GradientEnd)),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(String.format("%.1f", score), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    Text("/10", color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun CommentListItem(comment: SupabaseService.TrackCommentResponse) {
    val dateStr = remember(comment.createdAt) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = sdf.parse(comment.createdAt?.take(19) ?: "") ?: Date()
            SimpleDateFormat("d MMM, HH:mm", Locale("ru")).format(date)
        } catch (e: Exception) { "" }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .background(ColorSurface.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(ColorSurface)
                ) {
                    if (!comment.trackCoverUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(comment.trackCoverUrl).crossfade(false).build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Column {
                    Text(comment.trackTitle, color = ColorOnBackground, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(comment.trackArtist, color = ColorSecondary, fontSize = 10.sp, maxLines = 1)
                }
            }
            Text(dateStr, color = ColorSecondary, fontSize = 10.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(comment.comment, color = ColorOnBackground, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("📝", fontSize = 40.sp)
        Text(title, color = ColorOnBackground.copy(alpha = 0.8f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(subtitle, color = ColorSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}
