package com.example.lumisound.feature.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.lumisound.data.remote.SupabaseService
import com.example.lumisound.ui.theme.*
import com.example.lumisound.ui.theme.LocalAppColors

/**
 * Экран синтеза — показывает свой постоянный код и поле ввода чужого кода.
 *
 * Логика:
 * - Один хост = одна постоянная сессия (без привязки к дате)
 * - При вводе чужого кода: если синтез с этим человеком уже есть — открываем его
 * - Нельзя создать два синтеза с одним и тем же человеком
 * - Чтобы создать новый — нужно сначала покинуть старый
 */
@Composable
fun SynthesisCreateScreen(
    creatorUsername: String?,
    creatorAvatarUrl: String? = null,
    onClose: () -> Unit,
    onPlaylistCreated: (SupabaseService.PlaylistResponse?) -> Unit = {},
    viewModel: SynthesisViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var joinCodeInput by remember { mutableStateOf("") }
    var showJoinScreen by remember { mutableStateOf(false) }
    var pendingJoinCode by remember { mutableStateOf("") }

    // Создаём/загружаем постоянную сессию при первом открытии
    LaunchedEffect(Unit) {
        if (state.session == null) viewModel.createSession(creatorUsername, creatorAvatarUrl)
    }

    Box(modifier = Modifier.fillMaxSize().background(LocalAppColors.current.background).statusBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.08f), CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = LocalAppColors.current.onBackground, modifier = Modifier.size(18.dp))
                }
                Text(
                    "Синтез",
                    color = LocalAppColors.current.onBackground, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                )
            }

            when {
                state.isCreating -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(color = GradientStart, modifier = Modifier.size(36.dp))
                        Text("Создаём синтез...", color = LocalAppColors.current.secondary, fontSize = 14.sp)
                    }
                }
                state.error != null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("⚠️", fontSize = 32.sp)
                        Text(state.error ?: "", color = LocalAppColors.current.secondary, fontSize = 14.sp,
                            textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                        TextButton(onClick = { viewModel.clearError(); viewModel.createSession(creatorUsername, creatorAvatarUrl) }) {
                            Text("Попробовать снова", color = GradientStart)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Иконка
                        item {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier.size(100.dp).clip(RoundedCornerShape(20.dp))
                                        .background(Brush.linearGradient(listOf(Color(0xFF6C3FD9), Color(0xFFD93F6C), Color(0xFF3F9FD9)))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(40.dp))
                                }
                            }
                        }

                        // Описание
                        item {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text("Синтез", color = LocalAppColors.current.onBackground, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Поделитесь своим кодом с другом. Когда он введёт его — создастся плейлист из треков обоих.\nС одним человеком можно иметь только один синтез.",
                                    color = LocalAppColors.current.secondary, fontSize = 13.sp, textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }

                        // Свой код
                        state.inviteCode?.let { code ->
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Твой код синтеза", color = LocalAppColors.current.secondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Box(
                                        modifier = Modifier.fillMaxWidth()
                                            .background(GradientStart.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                                            .border(1.dp, GradientStart.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Synthesis Code", code))
                                            }
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(code, color = GradientStart, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 4.sp)
                                            Text("Нажми чтобы скопировать", color = LocalAppColors.current.secondary, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Ввод чужого кода
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                                    .background(LocalAppColors.current.surface, RoundedCornerShape(14.dp))
                                    .border(1.dp, Color(0xFF9B59B6).copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Войти в синтез друга", color = LocalAppColors.current.secondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier.weight(1f).height(44.dp)
                                            .background(LocalAppColors.current.background, RoundedCornerShape(10.dp))
                                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        androidx.compose.foundation.text.BasicTextField(
                                            value = joinCodeInput,
                                            onValueChange = { joinCodeInput = it.take(10) },
                                            textStyle = androidx.compose.ui.text.TextStyle(
                                                color = LocalAppColors.current.onBackground, fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold, letterSpacing = 3.sp
                                            ),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                                            decorationBox = { inner ->
                                                Box(contentAlignment = Alignment.CenterStart) {
                                                    if (joinCodeInput.isEmpty()) Text("Введи код синтеза", color = LocalAppColors.current.secondary, fontSize = 13.sp)
                                                    inner()
                                                }
                                            }
                                        )
                                    }
                                    Box(
                                        modifier = Modifier.size(44.dp)
                                            .background(
                                                if (joinCodeInput.isNotBlank()) Brush.linearGradient(listOf(Color(0xFF6C3FD9), Color(0xFFD93F6C)))
                                                else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.06f))),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                                if (joinCodeInput.isNotBlank()) {
                                                    pendingJoinCode = joinCodeInput.trim()
                                                    showJoinScreen = true
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }

                        // Участники своей сессии
                        if (state.participants.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Участники (${state.participants.size})",
                                        color = LocalAppColors.current.secondary, fontSize = 12.sp, fontWeight = FontWeight.Medium
                                    )
                                    Box(
                                        modifier = Modifier.size(28.dp).background(Color.White.copy(alpha = 0.06f), CircleShape)
                                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { viewModel.refreshParticipants() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Refresh, null, tint = LocalAppColors.current.secondary, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                            items(state.participants, key = { it.id }) { participant ->
                                ParticipantRow(participant = participant)
                            }
                        }

                        // Отступ для мини-плеера
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }

        // Оверлей — присоединение к синтезу друга
        androidx.compose.animation.AnimatedVisibility(
            visible = showJoinScreen && pendingJoinCode.isNotBlank(),
            enter = androidx.compose.animation.slideInVertically { it },
            exit = androidx.compose.animation.slideOutVertically { it },
            modifier = Modifier.fillMaxSize()
        ) {
            SynthesisJoinScreen(
                inviteCode = pendingJoinCode,
                currentUsername = creatorUsername,
                currentAvatarUrl = creatorAvatarUrl,
                onClose = {
                    showJoinScreen = false
                    joinCodeInput = ""
                    pendingJoinCode = ""
                },
                onPlaylistCreated = { playlist ->
                    showJoinScreen = false
                    joinCodeInput = ""
                    pendingJoinCode = ""
                    onPlaylistCreated(playlist)
                },
                playlistViewModel = playlistViewModel
            )
        }
    }
}

/**
 * Экран присоединения к чужому синтезу.
 *
 * Логика:
 * - Загружает сессию по коду
 * - Проверяет: уже есть синтез с хостом? → открывает существующий
 * - Иначе → присоединяется и создаёт плейлист
 */
@Composable
fun SynthesisJoinScreen(
    inviteCode: String,
    currentUsername: String?,
    currentAvatarUrl: String?,
    onClose: () -> Unit,
    onPlaylistCreated: (SupabaseService.PlaylistResponse?) -> Unit = {},
    playlistViewModel: PlaylistViewModel = hiltViewModel()
) {
    val viewModel: SynthesisViewModel = hiltViewModel(key = "join_$inviteCode")
    val state by viewModel.state.collectAsState()

    LaunchedEffect(inviteCode) {
        if (inviteCode.isNotBlank()) {
            viewModel.reset()
            viewModel.loadSessionByCode(inviteCode)
        }
    }

    // Когда плейлист готов — обновляем список и открываем
    LaunchedEffect(state.openPlaylistId) {
        val playlistId = state.openPlaylistId ?: return@LaunchedEffect
        playlistViewModel.loadPlaylists()
        kotlinx.coroutines.delay(800)
        val playlist = playlistViewModel.state.value.myPlaylists.firstOrNull { it.id == playlistId }
        onPlaylistCreated(playlist)
        viewModel.clearOpenPlaylist()
    }

    Box(modifier = Modifier.fillMaxSize().background(LocalAppColors.current.background).statusBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.08f), CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = LocalAppColors.current.onBackground, modifier = Modifier.size(18.dp))
                }
                Text(
                    "Приглашение в синтез",
                    color = LocalAppColors.current.onBackground, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                )
            }

            when {
                state.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(color = GradientStart, modifier = Modifier.size(36.dp))
                        Text("Загружаем синтез...", color = LocalAppColors.current.secondary, fontSize = 14.sp)
                    }
                }
                state.isBuildingPlaylist -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(color = GradientStart, modifier = Modifier.size(36.dp))
                        Text("Собираем плейлист...", color = LocalAppColors.current.secondary, fontSize = 14.sp)
                        Text("Смешиваем треки участников", color = LocalAppColors.current.secondary.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                }
                state.error != null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Text("⚠️", fontSize = 32.sp)
                        Text(
                            state.error ?: "",
                            color = LocalAppColors.current.secondary, fontSize = 14.sp, textAlign = TextAlign.Center
                        )
                        TextButton(onClick = { onClose() }) { Text("Назад", color = GradientStart) }
                    }
                }
                state.session != null -> {
                    val session = state.session!!
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier.size(100.dp).clip(RoundedCornerShape(18.dp))
                                        .background(Brush.linearGradient(listOf(Color(0xFF6C3FD9), Color(0xFFD93F6C), Color(0xFF3F9FD9)))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(40.dp))
                                }
                            }
                        }

                        item {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(session.name, color = LocalAppColors.current.onBackground, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                                Text(
                                    "${session.creatorUsername ?: "Пользователь"} приглашает тебя в синтез",
                                    color = LocalAppColors.current.secondary, fontSize = 14.sp, textAlign = TextAlign.Center
                                )
                            }
                        }

                        if (state.participants.isNotEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                        .background(LocalAppColors.current.surface, RoundedCornerShape(14.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("Уже участвуют (${state.participants.size})", color = LocalAppColors.current.secondary, fontSize = 12.sp)
                                    state.participants.forEach { p -> ParticipantRow(participant = p) }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth().height(52.dp)
                                    .background(
                                        Brush.linearGradient(listOf(GradientStart, Color(0xFFD93F6C))),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                        viewModel.joinAndBuildPlaylist(currentUsername, currentAvatarUrl)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Text("Присоединиться и создать плейлист", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParticipantRow(participant: SupabaseService.SynthesisParticipant) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(LocalAppColors.current.surface),
            contentAlignment = Alignment.Center
        ) {
            if (!participant.avatarUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(participant.avatarUrl).crossfade(false).build(),
                    contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Person, null, tint = LocalAppColors.current.secondary, modifier = Modifier.size(18.dp))
            }
        }
        Text(
            participant.username ?: "Пользователь",
            color = LocalAppColors.current.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.CheckCircle, null, tint = GradientStart, modifier = Modifier.size(16.dp))
    }
}
