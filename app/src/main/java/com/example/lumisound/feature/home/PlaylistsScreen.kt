package com.example.lumisound.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.lumisound.ui.theme.*
import com.example.lumisound.ui.theme.LocalAppColors

@Composable
fun PlaylistsScreen(
    onClose: () -> Unit,
    viewModel: PlaylistViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedPlaylist by remember { mutableStateOf<SupabaseService.PlaylistResponse?>(null) }

    LaunchedEffect(state.savedSuccess) {
        if (state.savedSuccess) viewModel.clearSuccess()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(LocalAppColors.current.background).statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.08f), CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = LocalAppColors.current.onBackground, modifier = Modifier.size(18.dp))
                }
                Text("Мои плейлисты", color = LocalAppColors.current.onBackground, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier.size(36.dp).background(GradientStart.copy(alpha = 0.15f), CircleShape)
                        .border(1.dp, GradientStart.copy(alpha = 0.4f), CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showCreateDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, "Create", tint = GradientStart, modifier = Modifier.size(18.dp))
                }
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.06f)))

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GradientStart, modifier = Modifier.size(32.dp))
                }
            } else if (state.myPlaylists.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("🎵", fontSize = 40.sp)
                        Text("Нет плейлистов", color = LocalAppColors.current.onBackground.copy(alpha = 0.5f), fontSize = 16.sp)
                        Text("Нажмите + чтобы создать первый", color = LocalAppColors.current.secondary, fontSize = 13.sp)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.myPlaylists, key = { it.id }) { playlist ->
                        PlaylistGridCard(
                            playlist = playlist,
                            onDelete = { viewModel.deletePlaylist(playlist.id) },
                            onToggleVisibility = { viewModel.toggleVisibility(playlist.id, playlist.isPublic) },
                            onClick = { selectedPlaylist = playlist }
                        )
                    }
                }
            }
        }

        // Детальный экран плейлиста
        selectedPlaylist?.let { pl ->
            androidx.compose.animation.AnimatedVisibility(
                visible = true,
                enter = slideInVertically { it },
                modifier = Modifier.fillMaxSize()
            ) {
                PlaylistDetailScreen(
                    playlist = pl,
                    onClose = { selectedPlaylist = null },
                    onDeleted = { selectedPlaylist = null }
                )
            }
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            isSaving = state.isSaving,
            onConfirm = { name, desc, isPublic ->
                viewModel.createPlaylist(name, desc, isPublic)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }
}

@Composable
fun PlaylistGridCard(
    playlist: SupabaseService.PlaylistResponse,
    onDelete: () -> Unit = {},
    onToggleVisibility: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().background(LocalAppColors.current.surface, RoundedCornerShape(14.dp))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            if (!playlist.coverUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(playlist.coverUrl).crossfade(false).build(),
                    contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        androidx.compose.ui.graphics.Brush.linearGradient(listOf(GradientStart.copy(alpha = 0.3f), Color.Black.copy(alpha = 0.5f)))
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(36.dp))
                }
            }
            // Visibility + Delete
            Row(
                modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier.size(24.dp).background(Color.Black.copy(alpha = 0.65f), CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onToggleVisibility() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (playlist.isPublic) Icons.Default.Public else Icons.Default.Lock,
                        null,
                        tint = if (playlist.isPublic) GradientStart else LocalAppColors.current.secondary,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Box(
                    modifier = Modifier.size(24.dp).background(Color.Black.copy(alpha = 0.65f), CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showDeleteConfirm = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Delete, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                }
            }
        }
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(playlist.name, color = LocalAppColors.current.onBackground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${playlist.trackCount} ${trackWord(playlist.trackCount)}", color = LocalAppColors.current.secondary, fontSize = 11.sp)
                if (playlist.isPublic) {
                    Text("· ♥ ${playlist.likesCount}", color = LocalAppColors.current.secondary, fontSize = 11.sp)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Удалить плейлист?") },
            text = { Text("«${playlist.name}» будет удалён навсегда.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text("Удалить", color = Color(0xFFFF5C6C))
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Отмена") } },
            containerColor = LocalAppColors.current.surface,
            titleContentColor = LocalAppColors.current.onBackground,
            textContentColor = LocalAppColors.current.secondary
        )
    }
}

@Composable
fun CreatePlaylistDialog(
    isSaving: Boolean,
    onConfirm: (String, String?, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый плейлист", color = LocalAppColors.current.onBackground) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 50) name = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GradientStart,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = LocalAppColors.current.onBackground,
                        unfocusedTextColor = LocalAppColors.current.onBackground,
                        cursorColor = GradientStart,
                        focusedLabelColor = GradientStart,
                        unfocusedLabelColor = LocalAppColors.current.secondary
                    )
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 200) description = it },
                    label = { Text("Описание (необязательно)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GradientStart,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = LocalAppColors.current.onBackground,
                        unfocusedTextColor = LocalAppColors.current.onBackground,
                        cursorColor = GradientStart,
                        focusedLabelColor = GradientStart,
                        unfocusedLabelColor = LocalAppColors.current.secondary
                    )
                )
                // Переключатель публичности
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LocalAppColors.current.surface, RoundedCornerShape(10.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { isPublic = !isPublic }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            if (isPublic) Icons.Default.Public else Icons.Default.Lock,
                            null,
                            tint = if (isPublic) GradientStart else LocalAppColors.current.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(if (isPublic) "Открытый" else "Приватный", color = LocalAppColors.current.onBackground, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(
                                if (isPublic) "Виден всем, можно лайкать" else "Только для вас",
                                color = LocalAppColors.current.secondary, fontSize = 11.sp
                            )
                        }
                    }
                    Switch(
                        checked = isPublic,
                        onCheckedChange = { isPublic = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = GradientStart,
                            uncheckedThumbColor = LocalAppColors.current.secondary,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name, description.takeIf { it.isNotBlank() }, isPublic) },
                enabled = name.isNotBlank() && !isSaving
            ) {
                if (isSaving) CircularProgressIndicator(color = GradientStart, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("Создать", color = GradientStart, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = LocalAppColors.current.secondary) }
        },
        containerColor = LocalAppColors.current.surface,
        titleContentColor = LocalAppColors.current.onBackground,
        textContentColor = LocalAppColors.current.secondary
    )
}

private fun trackWord(count: Int): String = when {
    count % 100 in 11..19 -> "треков"
    count % 10 == 1 -> "трек"
    count % 10 in 2..4 -> "трека"
    else -> "треков"
}
