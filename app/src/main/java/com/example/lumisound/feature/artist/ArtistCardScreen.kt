package com.example.lumisound.feature.artist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.lumisound.data.model.Track
import com.example.lumisound.feature.nowplaying.PlayerViewModel
import com.example.lumisound.ui.theme.GradientEnd
import com.example.lumisound.ui.theme.GradientStart
import com.example.lumisound.ui.theme.LocalAppColors

private fun formatCount(count: Int?): String {
    if (count == null) return "—"
    return when {
        count >= 1_000_000 -> "${count / 1_000_000}M"
        count >= 1_000 -> "${count / 1_000}K"
        else -> count.toString()
    }
}

@Composable
fun ArtistCardScreen(
    artistId: String? = null,
    artistName: String,
    artistImageUrl: String?,
    onClose: () -> Unit,
    onTrackClick: ((Track) -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: ArtistProfileViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(artistId, artistName) {
        if (!artistId.isNullOrBlank() || artistName.isNotBlank()) {
            viewModel.load(artistId, artistName, artistImageUrl)
        }
    }

    var showAllTracks by remember { mutableStateOf(false) }

    // Экран всех треков
    if (showAllTracks) {
        ArtistAllTracksScreen(
            artistName = artistName,
            tracks = state.allTracks,
            onClose = { showAllTracks = false },
            onTrackClick = { track ->
                val idx = state.allTracks.indexOf(track)
                playerViewModel.playPlaylist(state.allTracks, idx.coerceAtLeast(0))
                onTrackClick?.invoke(track)
            }
        )
        return
    }

    val artist = state.artist
    val avatarUrl = state.avatarUrl ?: artistImageUrl
    val coverUrl = state.coverUrl

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LocalAppColors.current.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ── Hero секция ──────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                ) {
                    // Cover photo или аватар как фон
                    if (!coverUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(coverUrl)
                                .crossfade(false)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (!avatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(avatarUrl)
                                .crossfade(false)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF1A1A2E), LocalAppColors.current.background)
                                    )
                                )
                        )
                    }

                    // Градиент снизу для читаемости текста
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Transparent,
                                        LocalAppColors.current.background.copy(alpha = 0.6f),
                                        LocalAppColors.current.background
                                    )
                                )
                            )
                    )

                    // Кнопка назад
                    Box(
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(16.dp)
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onClose() }
                            .align(Alignment.TopStart),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Назад",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Имя артиста и верификация внизу hero
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = artistName,
                                color = Color.White,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            if (artist?.isVerified == true) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Verified",
                                    tint = GradientStart,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        if (!artist?.location.isNullOrEmpty()) {
                            Text(
                                text = artist!!.location!!,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            // ── Аватар + статистика ──────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Аватар
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(LocalAppColors.current.surface)
                    ) {
                        if (!avatarUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(avatarUrl)
                                    .crossfade(false)
                                    .memoryCacheKey(avatarUrl)
                                    .build(),
                                contentDescription = artistName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, null, tint = LocalAppColors.current.secondary, modifier = Modifier.size(40.dp))
                            }
                        }
                    }

                    // Статистика
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(label = "Треки", value = formatCount(artist?.trackCount))
                        StatItem(label = "Фолловеры", value = formatCount(artist?.followerCount))
                        StatItem(label = "Плейлисты", value = formatCount(artist?.playlistCount))
                    }
                }
            }

            // ── Кнопка Play all ─────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                brush = Brush.linearGradient(listOf(GradientStart, GradientEnd)),
                                shape = CircleShape
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (state.allTracks.isNotEmpty()) {
                                    playerViewModel.playPlaylist(state.allTracks, 0)
                                } else {
                                    state.tracks.firstOrNull()?.let { playerViewModel.playPlaylist(state.tracks, 0) }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // ── Биография ───────────────────────────────────────────
            if (!artist?.bio.isNullOrEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "О себе",
                            color = LocalAppColors.current.onBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = artist!!.bio!!,
                            color = LocalAppColors.current.secondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // ── Популярные треки (топ-5) ─────────────────────────────
            item {
                Text(
                    text = "Популярные треки",
                    color = LocalAppColors.current.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }

            if (state.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = GradientStart, modifier = Modifier.size(32.dp))
                    }
                }
            } else if (state.tracks.isEmpty() && !state.isLoading) {
                item {
                    Text(
                        text = if (artistId.isNullOrBlank()) "Нет данных об артисте" else "Треки не найдены",
                        color = LocalAppColors.current.secondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            } else {
                itemsIndexed(
                    items = state.tracks,
                    key = { _, t -> t.id },
                    contentType = { _, _ -> "track" }
                ) { index, track ->
                    ArtistTrackItem(
                        index = index + 1,
                        track = track,
                        onClick = {
                            playerViewModel.playPlaylist(state.tracks, index)
                            onTrackClick?.invoke(track)
                        }
                    )
                }
            }

            // ── Все треки — горизонтальный скролл карточек ───────────
            if (state.allTracks.size > 5) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 20.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Все треки",
                            color = LocalAppColors.current.onBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        // Кнопка See All
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { showAllTracks = true }
                                .padding(horizontal = 16.dp, vertical = 7.dp)
                        ) {
                            Text(
                                "See All",
                                color = LocalAppColors.current.onBackground,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.allTracks, key = { it.id }) { track ->
                            ArtistTrackCard(
                                track = track,
                                onClick = {
                                    val idx = state.allTracks.indexOf(track)
                                    playerViewModel.playPlaylist(state.allTracks, idx.coerceAtLeast(0))
                                    onTrackClick?.invoke(track)
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item { Spacer(modifier = Modifier.navigationBarsPadding()) }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = LocalAppColors.current.onBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = LocalAppColors.current.secondary,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun ArtistTrackItem(
    index: Int,
    track: Track,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Номер
        Text(
            text = index.toString(),
            color = LocalAppColors.current.secondary,
            fontSize = 14.sp,
            modifier = Modifier.width(20.dp)
        )

        // Обложка
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(LocalAppColors.current.surface)
        ) {
            if (!track.imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(track.imageUrl)
                        .crossfade(false)
                        .memoryCacheKey(track.imageUrl)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.MusicNote, null, tint = LocalAppColors.current.secondary, modifier = Modifier.size(20.dp).align(Alignment.Center))
            }
        }

        // Название и артист
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.name,
                color = LocalAppColors.current.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!track.genre.isNullOrEmpty()) {
                Text(
                    text = track.genre,
                    color = LocalAppColors.current.secondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ── Карточка трека для горизонтального скролла ───────────────────────────────
@Composable
private fun ArtistTrackCard(
    track: Track,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(LocalAppColors.current.surface),
            contentAlignment = Alignment.Center
        ) {
            if (!track.imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(track.imageUrl)
                        .crossfade(false)
                        .memoryCacheKey(track.imageUrl)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.MusicNote, null,
                    tint = LocalAppColors.current.secondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(36.dp)
                )
            }
            // Кнопка play поверх обложки
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(28.dp)
                    .background(
                        brush = Brush.linearGradient(listOf(GradientStart, GradientEnd)),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow, null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Text(
            text = track.name,
            color = LocalAppColors.current.onBackground,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ── Экран всех треков артиста ─────────────────────────────────────────────────
@Composable
fun ArtistAllTracksScreen(
    artistName: String,
    tracks: List<Track>,
    onClose: () -> Unit,
    onTrackClick: (Track) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalAppColors.current.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Хедер
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.08f), CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onClose() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown, "Назад",
                            tint = LocalAppColors.current.onBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Все треки",
                            color = LocalAppColors.current.onBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            artistName,
                            color = LocalAppColors.current.secondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Список треков
            itemsIndexed(
                items = tracks,
                key = { _, t -> t.id },
                contentType = { _, _ -> "track" }
            ) { index, track ->
                ArtistTrackItem(
                    index = index + 1,
                    track = track,
                    onClick = { onTrackClick(track) }
                )
            }

            item { Spacer(modifier = Modifier.navigationBarsPadding()) }
        }
    }
}
