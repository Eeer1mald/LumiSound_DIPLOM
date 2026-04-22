package com.example.lumisound.feature.playlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.lumisound.data.model.Track
import com.example.lumisound.ui.theme.GradientEnd
import com.example.lumisound.ui.theme.GradientStart
import com.example.lumisound.ui.theme.LocalAppColors
import kotlinx.coroutines.delay

/**
 * Оверлей с шторкой "Добавить в плейлист".
 * Рендерится поверх текущего экрана через Box.
 */
@Composable
fun AddToPlaylistOverlay(
    track: Track,
    bottomBarPadding: androidx.compose.ui.unit.Dp = 0.dp,
    onDismiss: () -> Unit,
    viewModel: AddToPlaylistViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Загружаем плейлисты при открытии
    LaunchedEffect(Unit) { viewModel.loadPlaylists() }

    // Показываем success и закрываем
    LaunchedEffect(state.successMessage) {
        if (state.successMessage != null) {
            delay(1500)
            viewModel.clearMessages()
            onDismiss()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Затемнение фона
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
        )

        // Шторка снизу
        AddToPlaylistSheet(
            track = track,
            state = state,
            modifier = Modifier.align(Alignment.BottomCenter),
            bottomBarPadding = bottomBarPadding,
            onDismiss = onDismiss,
            onAddToPlaylist = { playlistId, playlistName ->
                viewModel.addTrackToPlaylist(track, playlistId, playlistName) {}
            },
            onCreatePlaylist = { name, autoFill ->
                viewModel.createPlaylistAndAdd(track, name, autoFill) {}
            }
        )
    }
}

@Composable
private fun AddToPlaylistSheet(
    track: Track,
    state: AddToPlaylistState,
    modifier: Modifier = Modifier,
    bottomBarPadding: androidx.compose.ui.unit.Dp = 0.dp,
    onDismiss: () -> Unit = {},
    onAddToPlaylist: (playlistId: String, playlistName: String) -> Unit,
    onCreatePlaylist: (name: String, autoFill: Boolean) -> Unit
) {
    var showCreateForm by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var autoFill by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    var dragOffset by remember { mutableStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = dragOffset.coerceAtLeast(0f) }
            .background(Color(0xFF1A1A1A), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(bottom = bottomBarPadding)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragOffset > 120f) onDismiss() else dragOffset = 0f
                    },
                    onDragCancel = { dragOffset = 0f },
                    onVerticalDrag = { change, delta ->
                        change.consume()
                        dragOffset = (dragOffset + delta).coerceAtLeast(0f)
                    }
                )
            }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
    ) {
        // Ручка
        Box(
            modifier = Modifier
                .padding(top = 12.dp)
                .width(36.dp)
                .height(4.dp)
                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Заголовок с инфо о треке
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Обложка трека
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(LocalAppColors.current.surface)
            ) {
                if (!track.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = track.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.MusicNote, null,
                        tint = LocalAppColors.current.secondary,
                        modifier = Modifier.size(22.dp).align(Alignment.Center)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Добавить в плейлист",
                    color = LocalAppColors.current.secondary,
                    fontSize = 12.sp
                )
                Text(
                    track.name,
                    color = LocalAppColors.current.onBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    track.artist,
                    color = LocalAppColors.current.secondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Success / Error сообщение
        if (state.successMessage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(GradientStart.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Check, null, tint = GradientStart, modifier = Modifier.size(18.dp))
                Text(state.successMessage, color = GradientStart, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (state.error != null) {
            Text(
                state.error,
                color = Color(0xFFFF5C6C),
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Форма создания нового плейлиста
        AnimatedVisibility(visible = showCreateForm) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(LocalAppColors.current.surface, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    "Новый плейлист",
                    color = LocalAppColors.current.onBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    placeholder = { Text("Название плейлиста", color = LocalAppColors.current.secondary, fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GradientStart,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedTextColor = LocalAppColors.current.onBackground,
                        unfocusedTextColor = LocalAppColors.current.onBackground,
                        cursorColor = GradientStart
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Переключатель автодобавления похожих треков
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            null,
                            tint = if (autoFill) GradientStart else LocalAppColors.current.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                "Добавить похожие треки",
                                color = LocalAppColors.current.onBackground,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Автоматически найдём треки по артисту и жанру",
                                color = LocalAppColors.current.secondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Switch(
                        checked = autoFill,
                        onCheckedChange = { autoFill = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = GradientStart,
                            uncheckedThumbColor = LocalAppColors.current.secondary,
                            uncheckedTrackColor = LocalAppColors.current.surface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Кнопка создать
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(listOf(GradientStart, GradientEnd)),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (newPlaylistName.isNotBlank() && !state.isCreating) {
                                focusManager.clearFocus()
                                onCreatePlaylist(newPlaylistName.trim(), autoFill)
                            }
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isCreating) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            if (autoFill) "Создать и заполнить" else "Создать плейлист",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Кнопка "Создать новый" (переключает форму)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .background(
                    if (showCreateForm) GradientStart.copy(alpha = 0.1f) else LocalAppColors.current.surface,
                    RoundedCornerShape(14.dp)
                )
                .border(
                    1.dp,
                    if (showCreateForm) GradientStart.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f),
                    RoundedCornerShape(14.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { showCreateForm = !showCreateForm }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (showCreateForm) GradientStart.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.06f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add, null,
                    tint = if (showCreateForm) GradientStart else LocalAppColors.current.onBackground,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                "Создать новый плейлист",
                color = if (showCreateForm) GradientStart else LocalAppColors.current.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Список существующих плейлистов
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GradientStart, modifier = Modifier.size(28.dp))
            }
        } else if (state.playlists.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "У вас пока нет плейлистов",
                    color = LocalAppColors.current.secondary,
                    fontSize = 14.sp
                )
            }
        } else {
            Text(
                "Мои плейлисты",
                color = LocalAppColors.current.secondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(minOf(state.playlists.size * 64, 256).dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(state.playlists, key = { it.id }) { playlist ->
                    PlaylistPickerItem(
                        playlist = playlist,
                        isAdding = state.isAdding,
                        onClick = { onAddToPlaylist(playlist.id, playlist.name) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun PlaylistPickerItem(
    playlist: com.example.lumisound.data.remote.SupabaseService.PlaylistResponse,
    isAdding: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .background(LocalAppColors.current.surface, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = !isAdding
            ) { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Обложка плейлиста
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            if (!playlist.coverUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = playlist.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.PlaylistAdd, null,
                    tint = LocalAppColors.current.secondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                playlist.name,
                color = LocalAppColors.current.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${playlist.trackCount} ${
                    when {
                        playlist.trackCount % 100 in 11..19 -> "треков"
                        playlist.trackCount % 10 == 1 -> "трек"
                        playlist.trackCount % 10 in 2..4 -> "трека"
                        else -> "треков"
                    }
                }",
                color = LocalAppColors.current.secondary,
                fontSize = 12.sp
            )
        }

        if (isAdding) {
            CircularProgressIndicator(
                color = GradientStart,
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                Icons.Default.Add, null,
                tint = GradientStart,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
