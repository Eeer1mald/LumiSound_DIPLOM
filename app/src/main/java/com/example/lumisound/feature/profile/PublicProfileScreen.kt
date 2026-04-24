package com.example.lumisound.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.lumisound.data.remote.SupabaseService
import com.example.lumisound.ui.theme.GradientEnd
import com.example.lumisound.ui.theme.GradientStart
import com.example.lumisound.ui.theme.LocalAppColors

@Composable
fun PublicProfileScreen(
    userId: String,
    username: String,
    avatarUrl: String?,
    onClose: () -> Unit,
    onArtistClick: ((artistId: String, artistName: String, artistImageUrl: String?) -> Unit)? = null,
    viewModel: PublicProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(userId) {
        viewModel.load(userId, username, avatarUrl)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalAppColors.current.background)
            .statusBarsPadding()
    ) {
        // Хедер — фиксированный
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LocalAppColors.current.background)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.KeyboardArrowDown, "Back", tint = LocalAppColors.current.onBackground, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text("Профиль", color = LocalAppColors.current.secondary, fontSize = 14.sp)
        }

        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GradientStart, modifier = Modifier.size(32.dp))
                }
            }
            state.isPrivate -> {
                // Скрытый профиль
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 62.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(LocalAppColors.current.surface, RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lock, null, tint = LocalAppColors.current.secondary, modifier = Modifier.size(36.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(state.username, color = LocalAppColors.current.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Профиль скрыт", color = LocalAppColors.current.secondary, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Этот пользователь скрыл свой профиль", color = LocalAppColors.current.secondary.copy(alpha = 0.6f), fontSize = 13.sp)
                }
            }
            else -> {
                // Публичный профиль
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 62.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 100.dp)
                ) {
                    // Аватар + имя
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(GradientStart),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!state.avatarUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(state.avatarUrl).crossfade(false).build(),
                                    contentDescription = state.username,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    state.username.take(1).uppercase(),
                                    color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(state.username, color = LocalAppColors.current.onBackground, fontSize = 22.sp, fontWeight = FontWeight.Bold)

                        if (!state.bio.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                state.bio!!,
                                color = LocalAppColors.current.secondary,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }

                        // Статистика
                        if (state.ratingsCount > 0 || state.reviewsCount > 0) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatChip(
                                    icon = Icons.Default.Star,
                                    value = state.ratingsCount.toString(),
                                    label = "оценок",
                                    modifier = Modifier.weight(1f)
                                )
                                StatChip(
                                    icon = Icons.Default.RateReview,
                                    value = state.reviewsCount.toString(),
                                    label = "рецензий",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.06f)))

                    // Любимые треки
                    if (state.favoriteTracks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            "Любимые треки",
                            color = LocalAppColors.current.onBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.favoriteTracks, key = { it.id }) { track ->
                                PublicTrackCard(track = track)
                            }
                        }
                    }

                    // Любимые артисты
                    if (state.favoriteArtists.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "Любимые артисты",
                            color = LocalAppColors.current.onBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.favoriteArtists, key = { it.id }) { artist ->
                                PublicArtistCard(
                                    artist = artist,
                                    onClick = {
                                        onArtistClick?.invoke(artist.artistId, artist.artistName, artist.artistImageUrl)
                                    }
                                )
                            }
                        }
                    }

                    // Лучшие рецензии
                    if (state.topReviews.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "Лучшие рецензии",
                            color = LocalAppColors.current.onBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            state.topReviews.forEach { review ->
                                PublicReviewCard(review = review)
                            }
                        }
                    }

                    // Если нет данных
                    if (state.favoriteTracks.isEmpty() && state.favoriteArtists.isEmpty() && state.topReviews.isEmpty()) {
                        Spacer(modifier = Modifier.height(40.dp))
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.MusicNote, null, tint = GradientStart.copy(alpha = 0.4f), modifier = Modifier.size(40.dp))
                                Text("Пользователь ещё ничего не слушал", color = LocalAppColors.current.secondary, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(LocalAppColors.current.surface, RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(28.dp).background(GradientStart.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = GradientStart, modifier = Modifier.size(14.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(value, color = LocalAppColors.current.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(label, color = LocalAppColors.current.secondary, fontSize = 10.sp)
    }
}

@Composable
private fun PublicTrackCard(track: SupabaseService.FavoriteTrackResponse) {
    Column(
        modifier = Modifier.width(90.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier.size(90.dp).clip(RoundedCornerShape(12.dp))
                .background(LocalAppColors.current.surface),
            contentAlignment = Alignment.Center
        ) {
            if (!track.trackCoverUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(track.trackCoverUrl).crossfade(false).build(),
                    contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.MusicNote, null, tint = GradientStart.copy(alpha = 0.4f), modifier = Modifier.size(32.dp))
            }
        }
        Text(track.trackTitle, color = LocalAppColors.current.onBackground, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(track.trackArtist, color = LocalAppColors.current.secondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PublicArtistCard(
    artist: SupabaseService.FavoriteArtistResponse,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.width(80.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier.size(80.dp).clip(CircleShape).background(LocalAppColors.current.surface),
            contentAlignment = Alignment.Center
        ) {
            if (!artist.artistImageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(artist.artistImageUrl).crossfade(false).build(),
                    contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Person, null, tint = GradientStart.copy(alpha = 0.4f), modifier = Modifier.size(32.dp))
            }
        }
        Text(artist.artistName, color = LocalAppColors.current.onBackground, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PublicReviewCard(review: SupabaseService.TrackRatingResponse) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LocalAppColors.current.surface, RoundedCornerShape(14.dp))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Трек
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(LocalAppColors.current.background),
                contentAlignment = Alignment.Center
            ) {
                if (!review.trackCoverUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(review.trackCoverUrl).crossfade(false).build(),
                        contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.MusicNote, null, tint = GradientStart.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(review.trackTitle, color = LocalAppColors.current.onBackground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(review.trackArtist, color = LocalAppColors.current.secondary, fontSize = 11.sp, maxLines = 1)
            }
            // Оценка
            if (review.overallScore != null) {
                Box(
                    modifier = Modifier
                        .background(GradientStart.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .border(1.dp, GradientStart.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        String.format("%.1f", review.overallScore),
                        color = GradientStart, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // Текст рецензии
        if (!review.review.isNullOrBlank()) {
            Text(
                review.review!!,
                color = LocalAppColors.current.onBackground.copy(alpha = 0.85f),
                fontSize = 12.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Репутация
        if (review.reputation != 0) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    if (review.reputation > 0) Icons.Default.ThumbUp else Icons.Default.ThumbDown,
                    null,
                    tint = if (review.reputation > 0) GradientStart else Color(0xFFFF5C6C),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    "${if (review.reputation > 0) "+" else ""}${review.reputation}",
                    color = if (review.reputation > 0) GradientStart else Color(0xFFFF5C6C),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
