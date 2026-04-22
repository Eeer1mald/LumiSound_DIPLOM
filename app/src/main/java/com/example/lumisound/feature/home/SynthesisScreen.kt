package com.example.lumisound.feature.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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

// ── Экран создания синтеза ────────────────────────────────────────────────────
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

    LaunchedEffect(Unit) {
        if (state.session == null) {
            viewModel.createSession(creatorUsername, creatorAvatarUrl)
        }
    }

    LaunchedEffect(state.createdPlaylistId) {
        if (state.createdPlaylistId != null) {
            val playlist = playlistViewModel.state.value.myPlaylists.firstOrNull { it.id == state.createdPlaylistId }
            onPlaylistCreated(playlist)
            viewModel.clearCreatedPlaylist()
        }
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
                    Icon(Icons.Default.ArrowBack, null, tint = LocalAppColors.current.onBackground, modifier = Modifier.size(18.dp))
                }
                Text("Синтез", color = LocalAppColors.current.onBackground, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
            }

            if (state.isCreating) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(color = GradientStart, modifier = Modifier.size(36.dp))
                        Text("Создаём синтез...", color = LocalAppColors.current.secondary, fontSize = 14.sp)
                    }
                }
            } else if (state.error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("⚠️", fontSize = 32.sp)
                        Text(state.error ?: "", color = LocalAppColors.current.secondary, fontSize = 14.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp))
                        TextButton(onClick = { viewModel.createSession(creatorUsername, creatorAvatarUrl) }) {
                            Text("Попробовать снова", color = GradientStart)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Обложка синтеза
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier.size(120.dp).clip(RoundedCornerShape(20.dp))
                                    .background(Brush.linearGradient(listOf(Color(0xFF6C3FD9), Color(0xFFD93F6C), Color(0xFF3F9FD9)))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(48.dp))
                            }
                        }
                    }

                    // Название
                    item {
                        Text(
                            state.session?.name ?: "Синтез",
                            color = LocalAppColors.current.onBackground, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Поделитесь кодом с друзьями — они введут его и присоединятся к синтезу",
                            color = LocalAppColors.current.secondary, fontSize = 13.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        )
                    }

                    // Ссылка-инвайт
                    state.inviteLink?.let { link ->
                        item {
                            val code = state.session?.inviteCode ?: ""
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Пригласи друга в синтез", color = LocalAppColors.current.secondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)

                                // Код — большой и заметный
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
                                        Text("Код синтеза", color = LocalAppColors.current.secondary, fontSize = 11.sp)
                                        Text(code, color = GradientStart, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 4.sp)
                                        Text("Нажми чтобы скопировать", color = LocalAppColors.current.secondary, fontSize = 10.sp)
                                    }
                                }

                                // Кнопка поделиться ссылкой
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .background(LocalAppColors.current.surface, RoundedCornerShape(12.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(link, color = LocalAppColors.current.secondary, fontSize = 12.sp, modifier = Modifier.weight(1f),
                                        maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                    Box(
                                        modifier = Modifier.size(32.dp).background(GradientStart.copy(alpha = 0.15f), CircleShape)
                                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Synthesis Link", link))
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.ContentCopy, null, tint = GradientStart, modifier = Modifier.size(16.dp))
                                    }
                                    Box(
                                        modifier = Modifier.size(32.dp).background(GradientStart.copy(alpha = 0.15f), CircleShape)
                                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                                val shareText = "Присоединяйся к моему Синтезу в LumiSound!\n\nКод: $code\nСсылка: $link"
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                                }
                                                context.startActivity(Intent.createChooser(intent, "Поделиться синтезом"))
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Share, null, tint = GradientStart, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Войти в синтез друга по коду
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .background(LocalAppColors.current.surface, RoundedCornerShape(14.dp))
                                .border(1.dp, Color(0xFF9B59B6).copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Войти в синтез друга", color = LocalAppColors.current.secondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier.weight(1f).height(44.dp)
                                        .background(LocalAppColors.current.background, RoundedCornerShape(10.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    androidx.compose.foundation.text.BasicTextField(
                                        value = joinCodeInput,
                                        onValueChange = { joinCodeInput = it.uppercase().take(10) },
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            color = LocalAppColors.current.onBackground,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 3.sp
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
                                            if (joinCodeInput.isNotBlank())
                                                Brush.linearGradient(listOf(Color(0xFF6C3FD9), Color(0xFFD93F6C)))
                                            else
                                                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.06f))),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                            if (joinCodeInput.isNotBlank()) {
                                                pendingJoinCode = joinCodeInput
                                                showJoinScreen = true
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.ArrowForward, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    // Участники
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Участники (${state.participants.size})", color = LocalAppColors.current.secondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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

                    // Кнопка создать плейлист
                    if (state.participants.size >= 2) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth().height(52.dp)
                                    .background(Brush.linearGradient(listOf(GradientStart, Color(0xFFD93F6C))), RoundedCornerShape(14.dp))
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                        if (!state.isBuildingPlaylist) viewModel.buildPlaylist(playlistViewModel)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (state.isBuildingPlaylist) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Text("Создать плейлист синтеза", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth()
                                    .background(LocalAppColors.current.surface, RoundedCornerShape(12.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Ожидаем присоединения друзей...\nМинимум 2 участника для создания плейлиста",
                                    color = LocalAppColors.current.secondary, fontSize = 13.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }

        // Оверлей — присоединение к синтезу друга по коду
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
                onClose = { showJoinScreen = false; joinCodeInput = ""; pendingJoinCode = "" },
                onJoined = { showJoinScreen = false; joinCodeInput = ""; pendingJoinCode = "" }
            )
        }
    }
}

// ── Экран принятия инвайта ────────────────────────────────────────────────────
@Composable
fun SynthesisJoinScreen(
    inviteCode: String,
    currentUsername: String?,
    currentAvatarUrl: String?,
    onClose: () -> Unit,
    onJoined: () -> Unit = {},
    viewModel: SynthesisViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(inviteCode) {
        if (inviteCode.isNotBlank()) viewModel.loadSessionByCode(inviteCode)
    }

    LaunchedEffect(state.joinSuccess) {
        if (state.joinSuccess) {
            viewModel.clearJoinSuccess()
            onJoined()
        }
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
                    Icon(Icons.Default.ArrowBack, null, tint = LocalAppColors.current.onBackground, modifier = Modifier.size(18.dp))
                }
                Text("Приглашение в синтез", color = LocalAppColors.current.onBackground, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
            }

            when {
                state.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GradientStart, modifier = Modifier.size(36.dp))
                }
                state.error != null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("⚠️", fontSize = 32.sp)
                        Text(state.error ?: "", color = LocalAppColors.current.secondary, fontSize = 14.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp))
                    }
                }
                state.session != null -> {
                    val session = state.session!!
                    val myUserId = viewModel.state.value.session?.creatorId
                    val isOwn = session.creatorId == myUserId

                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Обложка
                        Box(
                            modifier = Modifier.size(100.dp).clip(RoundedCornerShape(18.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF6C3FD9), Color(0xFFD93F6C), Color(0xFF3F9FD9)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(40.dp))
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(session.name, color = LocalAppColors.current.onBackground, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                            Text(
                                "${session.creatorUsername ?: "Пользователь"} приглашает тебя в синтез",
                                color = LocalAppColors.current.secondary, fontSize = 14.sp, textAlign = TextAlign.Center
                            )
                        }

                        // Участники
                        if (state.participants.isNotEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                                    .background(LocalAppColors.current.surface, RoundedCornerShape(14.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Уже участвуют (${state.participants.size})", color = LocalAppColors.current.secondary, fontSize = 12.sp)
                                state.participants.forEach { p ->
                                    ParticipantRow(participant = p)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        if (isOwn) {
                            Text("Это ваш синтез", color = LocalAppColors.current.secondary, fontSize = 14.sp)
                        } else {
                            // Кнопка присоединиться
                            Box(
                                modifier = Modifier.fillMaxWidth().height(52.dp)
                                    .background(Brush.linearGradient(listOf(GradientStart, Color(0xFFD93F6C))), RoundedCornerShape(14.dp))
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                        if (!state.isJoining) viewModel.joinSession(currentUsername, currentAvatarUrl)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (state.isJoining) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.GroupAdd, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Text("Присоединиться к синтезу", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
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
        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(LocalAppColors.current.surface), contentAlignment = Alignment.Center) {
            if (!participant.avatarUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(participant.avatarUrl).crossfade(false).build(),
                    contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Person, null, tint = LocalAppColors.current.secondary, modifier = Modifier.size(18.dp))
            }
        }
        Text(participant.username ?: "Пользователь", color = LocalAppColors.current.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.CheckCircle, null, tint = GradientStart, modifier = Modifier.size(16.dp))
    }
}
