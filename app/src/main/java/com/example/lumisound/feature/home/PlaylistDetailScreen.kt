package com.example.lumisound.feature.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.lumisound.data.model.Track
import com.example.lumisound.data.remote.SupabaseService
import com.example.lumisound.feature.nowplaying.PlayerViewModel
import com.example.lumisound.ui.theme.*
import com.example.lumisound.ui.theme.LocalAppColors

@Composable
fun PlaylistDetailScreen(
    playlist: SupabaseService.PlaylistResponse,
    onClose: () -> Unit,
    onDeleted: () -> Unit = onClose,
    navController: androidx.navigation.NavHostController? = null,
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current
    var showSearch by remember { mutableStateOf(false) }
    var showDeletePlaylistDialog by remember { mutableStateOf(false) }
    var showLeaveSynthesisDialog by remember { mutableStateOf(false) }
    val isOwner = state.currentUserId != null && state.playlist?.userId == state.currentUserId
    val isSynthesis = playlist.isSynthesis || state.playlist?.isSynthesis == true
    val synthesisViewModel: SynthesisViewModel = hiltViewModel()
    // Актуальный счётчик лайков из playlistViewModel — обновляется оптимистично при toggleLike
    val playlistVmState by playlistViewModel.state.collectAsState()
    val isLiked = playlistVmState.likedPlaylistIds.contains(playlist.id)
    val currentLikesCount = (playlistVmState.topPlaylists + playlistVmState.recommendedPlaylists)
        .firstOrNull { it.id == playlist.id }?.likesCount
        ?: playlist.likesCount

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.setCoverUri(it) }
    }

    LaunchedEffect(playlist.id) {
        viewModel.loadPlaylist(playlist)
        viewModel.setOnCoverUpdated { id, url -> playlistViewModel.updatePlaylistCoverLocally(id, url) }
        viewModel.setOnNameUpdated { id, name, desc, count -> playlistViewModel.updatePlaylistNameLocally(id, name, desc, count) }
        playlistViewModel.loadLikeStatus(playlist.id)
        // Для синтез-плейлиста загружаем сессию
        if (isSynthesis && !playlist.synthesisCode.isNullOrBlank()) {
            synthesisViewModel.loadSessionByCode(playlist.synthesisCode)
        }
    }

    // Обработка выхода из синтеза
    val synthesisState by synthesisViewModel.state.collectAsState()
    var leaveTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(synthesisState.openPlaylistId, leaveTriggered) {
        val result = synthesisState.openPlaylistId
        if (leaveTriggered && (result == "DELETED" || result == "LEFT")) {
            synthesisViewModel.clearOpenPlaylist()
            playlistViewModel.loadPlaylists()
            onDeleted()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(LocalAppColors.current.background).statusBarsPadding()) {
        val listState = rememberLazyListState()

        LazyColumn(modifier = Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 80.dp)) {

            // Header
            item(key = "header") {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.08f), CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClose() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = LocalAppColors.current.onBackground, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        when {
                            isSynthesis -> "Синтез"
                            isOwner -> "Мой плейлист"
                            else -> "Плейлист"
                        },
                        color = LocalAppColors.current.onBackground, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                    )
                    if (isOwner && !isSynthesis) {
                        Box(modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.08f), CircleShape)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { viewModel.toggleVisibility() }, contentAlignment = Alignment.Center) {
                            Icon(if (state.playlist?.isPublic == true) Icons.Default.Public else Icons.Default.Lock, null,
                                tint = if (state.playlist?.isPublic == true) GradientStart else LocalAppColors.current.secondary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.08f), CircleShape)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showDeletePlaylistDialog = true }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Delete, null, tint = Color(0xFFFF5C6C).copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                        }
                    }
                    // Кнопка выхода из синтеза
                    if (isSynthesis) {
                        Box(modifier = Modifier.size(36.dp).background(Color(0xFFFF5C6C).copy(alpha = 0.1f), CircleShape)
                            .border(1.dp, Color(0xFFFF5C6C).copy(alpha = 0.3f), CircleShape)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showLeaveSynthesisDialog = true },
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ExitToApp, null, tint = Color(0xFFFF5C6C), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Обложка + инфо
            item(key = "info") {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Box(modifier = Modifier.size(110.dp).clip(RoundedCornerShape(14.dp)).background(LocalAppColors.current.surface)
                            .then(if (isOwner) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { imagePicker.launch("image/*") } else Modifier),
                            contentAlignment = Alignment.Center) {
                            val cover = state.coverUri?.toString() ?: state.playlist?.coverUrl
                            if (!cover.isNullOrEmpty()) {
                                AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(cover).crossfade(false).memoryCacheKey(cover).build(),
                                    contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else {
                                Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(GradientStart.copy(alpha = 0.4f), Color.Black.copy(alpha = 0.7f)))), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
                                }
                            }
                            if (isOwner) {
                                Box(modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).size(24.dp).background(Color.Black.copy(alpha = 0.7f), CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(13.dp))
                                }
                            }
                        }
                        // Лайк прямо под обложкой — только для чужих и не синтез
                        if (!isOwner && !isSynthesis) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(36.dp)
                                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { playlistViewModel.toggleLike(playlist.id) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        null,
                                        tint = if (isLiked) GradientStart else LocalAppColors.current.secondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                if (currentLikesCount > 0) {
                                    Text("$currentLikesCount", color = LocalAppColors.current.secondary, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (isOwner && state.isEditingName) {
                            OutlinedTextField(value = state.editName, onValueChange = { viewModel.setEditName(it) }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { viewModel.saveName(); focusManager.clearFocus() }),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GradientStart, unfocusedBorderColor = Color.White.copy(alpha = 0.2f), focusedTextColor = LocalAppColors.current.onBackground, unfocusedTextColor = LocalAppColors.current.onBackground, cursorColor = GradientStart),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold))
                        } else {
                            Text(state.playlist?.name ?: playlist.name, color = LocalAppColors.current.onBackground, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth().then(if (isOwner) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { viewModel.toggleEditName() } else Modifier))
                        }
                        if (isOwner) {
                            if (state.isEditingName) {
                                OutlinedTextField(value = state.editDescription, onValueChange = { viewModel.setEditDescription(it) }, modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Описание...", color = LocalAppColors.current.secondary, fontSize = 12.sp) }, maxLines = 3,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { viewModel.saveName(); focusManager.clearFocus() }),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GradientStart, unfocusedBorderColor = Color.White.copy(alpha = 0.2f), focusedTextColor = LocalAppColors.current.onBackground, unfocusedTextColor = LocalAppColors.current.onBackground, cursorColor = GradientStart),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { viewModel.saveName(); focusManager.clearFocus() }) { Text("Сохранить", color = GradientStart, fontSize = 12.sp) }
                                    TextButton(onClick = { viewModel.toggleEditName() }) { Text("Отмена", color = LocalAppColors.current.secondary, fontSize = 12.sp) }
                                }
                            } else {
                                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { viewModel.toggleEditName() }.padding(10.dp)) {
                                    Text(state.playlist?.description?.takeIf { it.isNotBlank() } ?: "Описание...",
                                        color = if (state.playlist?.description.isNullOrBlank()) LocalAppColors.current.secondary else LocalAppColors.current.onBackground, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        } else {
                            if (!state.playlist?.description.isNullOrBlank()) {
                                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp)).padding(10.dp)) {
                                    Text(state.playlist!!.description!!, color = LocalAppColors.current.onBackground, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            if (!state.playlist?.username.isNullOrBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                        val userId = state.playlist?.userId
                                        val username = state.playlist?.username ?: "Пользователь"
                                        val avatarUrl = state.playlist?.userAvatarUrl
                                        if (!userId.isNullOrBlank()) {
                                            navController?.navigate(
                                                com.example.lumisound.navigation.MainDestination.PublicProfile()
                                                    .createRoute(userId, username, avatarUrl)
                                            )
                                        }
                                    }
                                ) {
                                    Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(GradientStart.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                                        val avatarUrl = state.playlist?.userAvatarUrl
                                        if (!avatarUrl.isNullOrEmpty()) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current).data(avatarUrl).crossfade(false).memoryCacheKey(avatarUrl).build(),
                                                contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Text(state.playlist!!.username!!.take(1).uppercase(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Text(state.playlist!!.username!!, color = LocalAppColors.current.onBackground, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                        // Количество треков
                        Text("${state.tracks.size} ${trackWord(state.tracks.size)}", color = LocalAppColors.current.secondary, fontSize = 12.sp)
                    }
                }
            }

            // Кнопки действий — только для владельца и не синтез-плейлиста
            if (isOwner && !isSynthesis) {
                item(key = "actions") {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Box(modifier = Modifier.background(LocalAppColors.current.surface, RoundedCornerShape(22.dp)).border(1.dp, GradientStart.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showSearch = true }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Add, null, tint = GradientStart, modifier = Modifier.size(16.dp))
                                Text("Добавить трек", color = GradientStart, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.06f)))
                }
            } else {
                item(key = "divider") {
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.06f)))
                }
            }

            // Загрузка
            if (state.isLoading) {
                item(key = "loading") {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GradientStart, modifier = Modifier.size(28.dp))
                    }
                }
            }

            // Пустой плейлист
            if (state.tracks.isEmpty() && !state.isLoading) {
                item(key = "empty") {
                    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("🎵", fontSize = 40.sp)
                            Text("Плейлист пуст", color = LocalAppColors.current.onBackground.copy(alpha = 0.5f), fontSize = 16.sp)
                            if (isOwner) {
                                Box(modifier = Modifier.background(GradientStart.copy(alpha = 0.15f), RoundedCornerShape(22.dp)).border(1.dp, GradientStart.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showSearch = true }.padding(horizontal = 20.dp, vertical = 10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.Add, null, tint = GradientStart, modifier = Modifier.size(16.dp))
                                        Text("Найти и добавить треки", color = GradientStart, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Треки
            items(state.tracks.take(200), key = { it.id }) { track ->
                PlaylistTrackRow(track = track, isOwner = isOwner && !isSynthesis,
                    onPlay = {
                        // Устанавливаем весь плейлист для навигации между треками
                        val allTracks = state.tracks.map { t ->
                            Track(id = t.trackId, name = t.trackTitle, artist = t.trackArtist,
                                imageUrl = t.trackCoverUrl, previewUrl = t.trackPreviewUrl, hdImageUrl = t.trackCoverUrl)
                        }
                        val idx = state.tracks.indexOf(track)
                        playerViewModel.playPlaylist(allTracks, idx.coerceAtLeast(0))
                    },
                    onRemove = { viewModel.removeTrack(track.trackId) })
            }

            // Кнопка добавить ещё — только не для синтеза
            if (isOwner && !isSynthesis && state.tracks.isNotEmpty()) {
                item(key = "add_more") {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.background(GradientStart.copy(alpha = 0.12f), RoundedCornerShape(22.dp)).border(1.dp, GradientStart.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showSearch = true }.padding(horizontal = 24.dp, vertical = 10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Add, null, tint = GradientStart, modifier = Modifier.size(16.dp))
                                Text("Добавить ещё треки", color = GradientStart, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }

        // Поиск треков
        if (isOwner) {
            AnimatedVisibility(visible = showSearch, enter = slideInVertically { it }, exit = slideOutVertically { it }, modifier = Modifier.fillMaxSize()) {
                TrackSearchSheet(state = state, onQueryChange = { viewModel.setSearchQuery(it) }, onAddTrack = { viewModel.addTrack(it) }, onClose = { showSearch = false; viewModel.setSearchQuery("") })
            }
        }
    }

    if (showDeletePlaylistDialog) {
        AlertDialog(onDismissRequest = { showDeletePlaylistDialog = false },
            title = { Text("Удалить плейлист?", color = LocalAppColors.current.onBackground) },
            text = { Text("«${state.playlist?.name ?: playlist.name}» будет удалён навсегда.", color = LocalAppColors.current.secondary) },
            confirmButton = { TextButton(onClick = { showDeletePlaylistDialog = false; playlistViewModel.deletePlaylist(playlist.id); onDeleted() }) { Text("Удалить", color = Color(0xFFFF5C6C), fontWeight = FontWeight.SemiBold) } },
            dismissButton = { TextButton(onClick = { showDeletePlaylistDialog = false }) { Text("Отмена", color = LocalAppColors.current.secondary) } },
            containerColor = LocalAppColors.current.surface, titleContentColor = LocalAppColors.current.onBackground, textContentColor = LocalAppColors.current.secondary)
    }

    if (showLeaveSynthesisDialog) {
        val synthesisStateLocal by synthesisViewModel.state.collectAsState()
        AlertDialog(
            onDismissRequest = { showLeaveSynthesisDialog = false },
            title = { Text("Покинуть синтез?", color = LocalAppColors.current.onBackground) },
            text = {
                if (synthesisStateLocal.isBuildingPlaylist) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(color = GradientStart, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("Обновляем плейлист...", color = LocalAppColors.current.secondary)
                    }
                } else {
                    Text("Вы покинете синтез. Если останется меньше 2 участников — плейлист будет удалён.", color = LocalAppColors.current.secondary)
                }
            },
            confirmButton = {
                if (!synthesisStateLocal.isBuildingPlaylist) {
                    TextButton(onClick = {
                        val code = state.playlist?.synthesisCode ?: playlist.synthesisCode ?: ""
                        val pid = playlist.id
                        leaveTriggered = true
                        synthesisViewModel.leaveSynthesis(code, pid, playlistViewModel)
                    }) { Text("Покинуть", color = Color(0xFFFF5C6C), fontWeight = FontWeight.SemiBold) }
                }
            },
            dismissButton = {
                if (!synthesisStateLocal.isBuildingPlaylist) {
                    TextButton(onClick = { showLeaveSynthesisDialog = false }) { Text("Отмена", color = LocalAppColors.current.secondary) }
                }
            },
            containerColor = LocalAppColors.current.surface
        )
    }
}

@Composable
private fun PlaylistTrackRow(track: SupabaseService.PlaylistTrackResponse, isOwner: Boolean = true, onPlay: () -> Unit, onRemove: () -> Unit) {
    var showRemoveDialog by remember { mutableStateOf(false) }
    var showContributorsDialog by remember { mutableStateOf(false) }

    // Парсим несколько участников (хранятся через запятую)
    val usernames = track.addedByUsername?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    val avatars = track.addedByAvatar?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    val hasMultiple = usernames.size > 1

    Row(modifier = Modifier.fillMaxWidth().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onPlay() }.padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(LocalAppColors.current.surface), contentAlignment = Alignment.Center) {
            if (!track.trackCoverUrl.isNullOrEmpty()) {
                AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(track.trackCoverUrl).crossfade(false).memoryCacheKey(track.trackCoverUrl).build(),
                    contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else { Icon(Icons.Default.MusicNote, null, tint = LocalAppColors.current.secondary, modifier = Modifier.size(22.dp)) }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(track.trackTitle, color = LocalAppColors.current.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.trackArtist, color = LocalAppColors.current.secondary, fontSize = 12.sp, maxLines = 1)
        }
        // Аватарки участников — стопкой если несколько
        if (usernames.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .width(if (hasMultiple) 36.dp else 24.dp)
                    .height(24.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        if (hasMultiple) showContributorsDialog = true
                    }
            ) {
                // Вторая аватарка (сзади) — только если несколько
                if (hasMultiple && avatars.size >= 2) {
                    Box(
                        modifier = Modifier.size(24.dp).offset(x = 12.dp).clip(CircleShape)
                            .background(GradientStart.copy(alpha = 0.3f)).border(1.dp, LocalAppColors.current.background, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(model = avatars.getOrNull(1), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                }
                // Первая аватарка (спереди)
                Box(
                    modifier = Modifier.size(24.dp).clip(CircleShape).background(GradientStart.copy(alpha = 0.2f))
                        .border(1.dp, LocalAppColors.current.background, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val firstAvatar = avatars.firstOrNull()
                    if (!firstAvatar.isNullOrEmpty()) {
                        AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(firstAvatar).crossfade(false).build(),
                            contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Text(usernames.first().take(1).uppercase(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (isOwner) {
            Box(modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = 0.06f), CircleShape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showRemoveDialog = true }, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Remove, null, tint = LocalAppColors.current.secondary, modifier = Modifier.size(16.dp))
            }
        }
    }
    if (showRemoveDialog) {
        AlertDialog(onDismissRequest = { showRemoveDialog = false },
            title = { Text("Удалить трек?", color = LocalAppColors.current.onBackground) },
            text = { Text("«${track.trackTitle}» будет удалён из плейлиста.", color = LocalAppColors.current.secondary) },
            confirmButton = { TextButton(onClick = { showRemoveDialog = false; onRemove() }) { Text("Удалить", color = Color(0xFFFF5C6C), fontWeight = FontWeight.SemiBold) } },
            dismissButton = { TextButton(onClick = { showRemoveDialog = false }) { Text("Отмена", color = LocalAppColors.current.secondary) } },
            containerColor = LocalAppColors.current.surface, titleContentColor = LocalAppColors.current.onBackground, textContentColor = LocalAppColors.current.secondary)
    }
    // Диалог с участниками трека
    if (showContributorsDialog) {
        AlertDialog(
            onDismissRequest = { showContributorsDialog = false },
            title = { Text("Трек у участников", color = LocalAppColors.current.onBackground, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    usernames.forEachIndexed { idx, name ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(GradientStart.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                                val av = avatars.getOrNull(idx)
                                if (!av.isNullOrEmpty()) {
                                    AsyncImage(model = av, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                } else {
                                    Text(name.take(1).uppercase(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(name, color = LocalAppColors.current.onBackground, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showContributorsDialog = false }) { Text("OK", color = GradientStart) }
            },
            containerColor = LocalAppColors.current.surface
        )
    }
}

@Composable
private fun TrackSearchSheet(state: PlaylistDetailState, onQueryChange: (String) -> Unit, onAddTrack: (Track) -> Unit, onClose: () -> Unit) {
    val focusManager = LocalFocusManager.current
    Column(modifier = Modifier.fillMaxSize().background(LocalAppColors.current.background).statusBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.08f), CircleShape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClose() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Close, null, tint = LocalAppColors.current.onBackground, modifier = Modifier.size(18.dp))
            }
            Box(modifier = Modifier.weight(1f).height(44.dp).background(LocalAppColors.current.surface, RoundedCornerShape(12.dp)).border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.CenterStart) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Search, null, tint = LocalAppColors.current.secondary, modifier = Modifier.size(18.dp))
                    androidx.compose.foundation.text.BasicTextField(value = state.searchQuery, onValueChange = onQueryChange,
                        textStyle = androidx.compose.ui.text.TextStyle(color = LocalAppColors.current.onBackground, fontSize = 14.sp), singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner -> Box(contentAlignment = Alignment.CenterStart) { if (state.searchQuery.isEmpty()) Text("Поиск треков...", color = LocalAppColors.current.secondary, fontSize = 14.sp); inner() } })
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.06f)))
        when {
            state.isSearching -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = GradientStart, modifier = Modifier.size(28.dp)) }
            state.searchQuery.isBlank() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Введите название трека или исполнителя", color = LocalAppColors.current.secondary, fontSize = 14.sp) }
            state.searchResults.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Ничего не найдено", color = LocalAppColors.current.secondary, fontSize = 14.sp) }
            else -> {
                val searchListState = rememberLazyListState()
                LazyColumn(modifier = Modifier.fillMaxSize(), state = searchListState, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(state.searchResults, key = { it.id }) { track ->
                        val isAdded = state.tracks.any { it.trackId == track.id }
                        SearchTrackRow(track = track, isAdded = isAdded, onAdd = { if (!isAdded) onAddTrack(track) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchTrackRow(track: Track, isAdded: Boolean, onAdd: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(LocalAppColors.current.surface), contentAlignment = Alignment.Center) {
            if (!track.imageUrl.isNullOrEmpty()) {
                AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(track.imageUrl).crossfade(false).memoryCacheKey(track.imageUrl).build(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else { Icon(Icons.Default.MusicNote, null, tint = LocalAppColors.current.secondary, modifier = Modifier.size(22.dp)) }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(track.name, color = LocalAppColors.current.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = LocalAppColors.current.secondary, fontSize = 12.sp, maxLines = 1)
        }
        Box(modifier = Modifier.size(32.dp).background(if (isAdded) GradientStart.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.06f), CircleShape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onAdd() }, contentAlignment = Alignment.Center) {
            Icon(if (isAdded) Icons.Default.Check else Icons.Default.Add, null, tint = if (isAdded) GradientStart else LocalAppColors.current.secondary, modifier = Modifier.size(16.dp))
        }
    }
}

private fun trackWord(count: Int): String = when {
    count % 100 in 11..19 -> "треков"
    count % 10 == 1 -> "трек"
    count % 10 in 2..4 -> "трека"
    else -> "треков"
}

private fun likeWord(count: Int): String = when {
    count % 100 in 11..19 -> "сохранений"
    count % 10 == 1 -> "сохранение"
    count % 10 in 2..4 -> "сохранения"
    else -> "сохранений"
}
