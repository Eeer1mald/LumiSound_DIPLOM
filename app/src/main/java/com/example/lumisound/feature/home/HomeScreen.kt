package com.example.lumisound.feature.home

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.QueueMusic
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.lumisound.data.remote.SupabaseService
import com.example.lumisound.feature.nowplaying.PlayerViewModel
import com.example.lumisound.ui.theme.*

@Composable
fun HomeScreen(
    navController: NavHostController,
    userName: String = "Пользователь",
    viewModel: HomeViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel(),
    synthesisInviteCode: String? = null,
    creatorAvatarUrl: String? = null
) {
    val state by playlistViewModel.state.collectAsState()
    var showCreateMenu by remember { mutableStateOf(false) }
    var showAllPlaylists by remember { mutableStateOf(false) }
    var selectedPlaylist by remember { mutableStateOf<SupabaseService.PlaylistResponse?>(null) }
    var showSynthesis by remember { mutableStateOf(false) }
    var showSynthesisJoin by remember { mutableStateOf(synthesisInviteCode != null) }
    val joinCode by remember { mutableStateOf(synthesisInviteCode) }
    var manualJoinCode by remember { mutableStateOf("") }
    var showManualJoin by remember { mutableStateOf(false) }

    // Для рандомного трека — меняется при каждом нажатии
    var randomSeed by remember { mutableStateOf(0) }
    val randomTrack = remember(randomSeed, state.recentTracks) {
        if (state.recentTracks.isEmpty()) null
        else state.recentTracks.shuffled()[randomSeed % state.recentTracks.size.coerceAtLeast(1)]
    }

    // После создания — сразу открываем детальный экран
    LaunchedEffect(state.savedSuccess) {
        if (state.savedSuccess) {
            state.myPlaylists.firstOrNull()?.let { selectedPlaylist = it }
            playlistViewModel.clearSuccess()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(ColorBackground)) {
        val listState = rememberLazyListState()
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 140.dp),
            state = listState
        ) {
            // ── Header ──────────────────────────────────────────────
            item(key = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("LumiSound", color = ColorOnBackground, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Рандомный трек
                        Box(
                            modifier = Modifier.size(38.dp)
                                .background(Color.White.copy(alpha = 0.08f), CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    randomSeed++
                                    randomTrack?.let { t ->
                                        playerViewModel.playTrack(
                                            com.example.lumisound.data.model.Track(
                                                id = t.trackId, name = t.trackTitle, artist = t.trackArtist,
                                                imageUrl = t.trackCoverUrl, previewUrl = t.trackPreviewUrl
                                            )
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Shuffle, "Случайный трек", tint = ColorSecondary, modifier = Modifier.size(18.dp))
                        }
                        // Создать
                        Box(
                            modifier = Modifier.size(38.dp)
                                .background(GradientStart.copy(alpha = 0.15f), CircleShape)
                                .border(1.dp, GradientStart.copy(alpha = 0.4f), CircleShape)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showCreateMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, "Создать", tint = GradientStart, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // ── Активность — первой ──────────────────────────────────
            if (state.stats.totalRatings > 0 || state.stats.totalComments > 0) {
                item(key = "stats") {
                    StatsSection(stats = state.stats)
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // ── Три вкладки ──────────────────────────────────────────
            item(key = "tabs") {
                PlaylistTabSelector(selected = state.selectedTab, onSelect = { playlistViewModel.selectTab(it) })
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Контент вкладки ──────────────────────────────────────
            item(key = "playlist_content") {
                PlaylistTabContent(
                    state = state,
                    onSeeAll = { showAllPlaylists = true },
                    onCreateClick = { playlistViewModel.createPlaylist() },
                    onToggleLike = { playlistViewModel.toggleLike(it) },
                    onToggleVisibility = { id, pub -> playlistViewModel.toggleVisibility(id, pub) },
                    onPlaylistClick = { selectedPlaylist = it }
                )
            }

            // ── История прослушиваний ────────────────────────────────
            if (state.recentTracks.isNotEmpty()) {
                item(key = "recent_header") {
                    Spacer(modifier = Modifier.height(24.dp))
                    SectionHeader("Недавно слушал")
                    Spacer(modifier = Modifier.height(12.dp))
                }
                item(key = "recent_tracks") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.recentTracks, key = { it.id }) { track ->
                            SmallTrackCard(track = track, onClick = {
                                playerViewModel.playTrack(
                                    com.example.lumisound.data.model.Track(
                                        id = track.trackId, name = track.trackTitle, artist = track.trackArtist,
                                        imageUrl = track.trackCoverUrl, previewUrl = track.trackPreviewUrl
                                    )
                                )
                            })
                        }
                    }
                }
            }
        }

        // Полная страница плейлистов
        AnimatedVisibility(
            visible = showAllPlaylists,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.fillMaxSize()
        ) {
            PlaylistsScreen(onClose = { showAllPlaylists = false }, viewModel = playlistViewModel)
        }

        // Детальный экран плейлиста
        selectedPlaylist?.let { pl ->
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically { it },
                modifier = Modifier.fillMaxSize()
            ) {
                PlaylistDetailScreen(
                    playlist = pl,
                    onClose = { selectedPlaylist = null },
                    onDeleted = { selectedPlaylist = null },
                    navController = navController,
                    playlistViewModel = playlistViewModel
                )
            }
        }

        // Экран синтеза
        AnimatedVisibility(
            visible = showSynthesis,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.fillMaxSize()
        ) {
            SynthesisCreateScreen(
                creatorUsername = userName,
                creatorAvatarUrl = creatorAvatarUrl,
                onClose = { showSynthesis = false },
                onPlaylistCreated = { playlist ->
                    showSynthesis = false
                    playlist?.let { selectedPlaylist = it }
                },
                playlistViewModel = playlistViewModel
            )
        }

        // Экран присоединения к синтезу (по deep link)
        if (showSynthesisJoin && joinCode != null) {
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically { it },
                modifier = Modifier.fillMaxSize()
            ) {
                SynthesisJoinScreen(
                    inviteCode = joinCode!!,
                    currentUsername = userName,
                    currentAvatarUrl = creatorAvatarUrl,
                    onClose = { showSynthesisJoin = false },
                    onJoined = { showSynthesisJoin = false }
                )
            }
        }

        // Экран присоединения по ручному вводу кода
        if (showManualJoin && manualJoinCode.isNotBlank()) {
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically { it },
                modifier = Modifier.fillMaxSize()
            ) {
                SynthesisJoinScreen(
                    inviteCode = manualJoinCode,
                    currentUsername = userName,
                    currentAvatarUrl = creatorAvatarUrl,
                    onClose = { showManualJoin = false; manualJoinCode = "" },
                    onJoined = { showManualJoin = false; manualJoinCode = "" }
                )
            }
        }
    }

    if (showCreateMenu) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCreateMenu = false },
            title = { Text("Создать", color = ColorOnBackground, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Плейлист
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .background(ColorSurface, RoundedCornerShape(12.dp))
                            .border(1.dp, GradientStart.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                showCreateMenu = false
                                playlistViewModel.createPlaylist()
                            }
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(36.dp).background(GradientStart.copy(alpha = 0.15f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.QueueMusic, null, tint = GradientStart, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text("Плейлист", color = ColorOnBackground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Собери треки в коллекцию", color = ColorSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                    // Синтез (активный)
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .background(ColorSurface, RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF9B59B6).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                showCreateMenu = false
                                showSynthesis = true
                            }
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(36.dp)
                                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF6C3FD9), Color(0xFFD93F6C))), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text("Создать синтез", color = ColorOnBackground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Смешай треки с друзьями", color = ColorSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                    // Войти в синтез по коду
                    var codeInput by remember { mutableStateOf("") }
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(ColorSurface, RoundedCornerShape(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = codeInput,
                            onValueChange = { codeInput = it.uppercase().take(10) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = ColorOnBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            decorationBox = { inner ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (codeInput.isEmpty()) Text("Введи код синтеза", color = ColorSecondary, fontSize = 13.sp)
                                    inner()
                                }
                            }
                        )
                        Box(
                            modifier = Modifier.size(36.dp)
                                .background(if (codeInput.isNotBlank()) GradientStart else Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    if (codeInput.isNotBlank()) {
                                        manualJoinCode = codeInput
                                        showCreateMenu = false
                                        showManualJoin = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ArrowForward, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showCreateMenu = false }) {
                    Text("Отмена", color = ColorSecondary)
                }
            },
            containerColor = com.example.lumisound.ui.theme.ColorBackground,
            titleContentColor = ColorOnBackground
        )
    }
}

// ── Три квадратика-вкладки ───────────────────────────────────────────────────
@Composable
fun PlaylistTabSelector(selected: PlaylistTab, onSelect: (PlaylistTab) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PlaylistTab.entries.forEach { tab ->
            val isActive = selected == tab
            Box(
                modifier = Modifier.weight(1f).height(44.dp)
                    .background(if (isActive) GradientStart.copy(alpha = 0.18f) else ColorSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, if (isActive) GradientStart.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.07f), RoundedCornerShape(12.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(tab) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (tab) {
                        PlaylistTab.MY -> "Мои"
                        PlaylistTab.RECOMMENDED -> "Для вас"
                        PlaylistTab.TOP -> "Топ"
                    },
                    color = if (isActive) GradientStart else ColorSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

// ── Контент вкладки ──────────────────────────────────────────────────────────
@Composable
private fun PlaylistTabContent(
    state: PlaylistUiState,
    onSeeAll: () -> Unit,
    onCreateClick: () -> Unit,
    onToggleLike: (String) -> Unit,
    onToggleVisibility: (String, Boolean) -> Unit,
    onPlaylistClick: (SupabaseService.PlaylistResponse) -> Unit = {}
) {
    val playlists = state.playlists
    val isMyTab = state.selectedTab == PlaylistTab.MY

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxWidth().height(130.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = GradientStart, modifier = Modifier.size(28.dp))
        }
        return
    }

    if (playlists.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(110.dp)
                .background(ColorSurface, RoundedCornerShape(16.dp))
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                .then(if (isMyTab) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onCreateClick() } else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Text(if (isMyTab) "Создать первый плейлист" else "Пока пусто", color = ColorSecondary, fontSize = 13.sp)
        }
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                when (state.selectedTab) {
                    PlaylistTab.MY -> "Мои плейлисты"
                    PlaylistTab.RECOMMENDED -> "Рекомендации"
                    PlaylistTab.TOP -> "Топ плейлисты"
                },
                color = ColorOnBackground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold
            )
            if (playlists.size > 4 && isMyTab) {
                Row(
                    modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSeeAll() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Все", color = GradientStart, fontSize = 13.sp)
                    Icon(Icons.Default.KeyboardArrowRight, null, tint = GradientStart, modifier = Modifier.size(16.dp))
                }
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(playlists.take(10), key = { it.id }) { playlist ->
                PlaylistCard(
                    playlist = playlist,
                    isLiked = state.likedPlaylistIds.contains(playlist.id),
                    showLike = !isMyTab && playlist.userId != state.currentUserId,
                    showVisibilityToggle = isMyTab,
                    onToggleLike = { onToggleLike(playlist.id) },
                    onToggleVisibility = { onToggleVisibility(playlist.id, playlist.isPublic) },
                    onClick = { onPlaylistClick(playlist) }
                )
            }
        }
    }
}

@Composable
private fun PlaylistCard(
    playlist: SupabaseService.PlaylistResponse,
    isLiked: Boolean = false,
    showLike: Boolean = false,
    showVisibilityToggle: Boolean = false,
    onToggleLike: () -> Unit = {},
    onToggleVisibility: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier.width(110.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(modifier = Modifier.size(110.dp).clip(RoundedCornerShape(14.dp)).background(ColorSurface), contentAlignment = Alignment.Center) {
            if (!playlist.coverUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(playlist.coverUrl).crossfade(false).build(),
                    contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(GradientStart.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.6f)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
                }
            }
            // Visibility badge (без мусорки)
            if (showVisibilityToggle) {
                Box(
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                        .size(22.dp).background(Color.Black.copy(alpha = 0.65f), CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onToggleVisibility() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (playlist.isPublic) Icons.Default.Public else Icons.Default.Lock,
                        null, tint = if (playlist.isPublic) GradientStart else ColorSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            // Like button
            if (showLike) {
                Box(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
                        .size(26.dp).background(Color.Black.copy(alpha = 0.65f), CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onToggleLike() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        null, tint = if (isLiked) GradientStart else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
        Text(playlist.name, color = ColorOnBackground, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${playlist.trackCount} ${trackWord(playlist.trackCount)}", color = ColorSecondary, fontSize = 10.sp)
            if (playlist.likesCount > 0) {
                Text("·", color = ColorSecondary, fontSize = 10.sp)
                Icon(Icons.Default.Favorite, null, tint = GradientStart, modifier = Modifier.size(10.dp))
                Text("${playlist.likesCount}", color = ColorSecondary, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun SmallTrackCard(track: SupabaseService.FavoriteTrackResponse, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(80.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() },
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(10.dp)).background(ColorSurface), contentAlignment = Alignment.Center) {
            if (!track.trackCoverUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(track.trackCoverUrl).crossfade(false).build(),
                    contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.MusicNote, null, tint = ColorSecondary.copy(alpha = 0.5f), modifier = Modifier.size(24.dp))
            }
        }
        Text(track.trackTitle, color = ColorOnBackground, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(track.trackArtist, color = ColorSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, color = ColorOnBackground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 20.dp))
}

@Composable
private fun StatsSection(stats: HomeStats) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            .background(ColorSurface, RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("За неделю", color = ColorOnBackground, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatChip(icon = Icons.Default.Star, value = stats.ratingsThisWeek.toString(), label = "оценок", modifier = Modifier.weight(1f))
            StatChip(icon = Icons.Default.BarChart, value = stats.totalRatings.toString(), label = "всего оценок", modifier = Modifier.weight(1f))
            StatChip(icon = Icons.Default.ChatBubbleOutline, value = stats.totalComments.toString(), label = "комментариев", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatChip(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.background(ColorSurface, RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, tint = GradientStart, modifier = Modifier.size(16.dp))
        Text(value, color = ColorOnBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(label, color = ColorSecondary, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun trackWord(count: Int): String = when {
    count % 100 in 11..19 -> "треков"
    count % 10 == 1 -> "трек"
    count % 10 in 2..4 -> "трека"
    else -> "треков"
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenPreview() {
    HomeScreen(navController = rememberNavController())
}
