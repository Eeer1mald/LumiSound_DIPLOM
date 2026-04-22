package com.example.lumisound.feature.ratings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.lumisound.data.model.Track
import com.example.lumisound.data.remote.SupabaseService
import com.example.lumisound.ui.theme.GradientEnd
import com.example.lumisound.ui.theme.GradientStart
import com.example.lumisound.ui.theme.LocalAppColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReviewScreen(
    track: Track,
    onClose: () -> Unit,
    onOpenReviews: () -> Unit = {},
    navController: androidx.navigation.NavHostController? = null,
    viewModel: ReviewViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current
    var showRatingSheet by remember { mutableStateOf(false) }
    var showStatsSheet by remember { mutableStateOf(false) }

    LaunchedEffect(track.id) { viewModel.loadForTrack(track.id) }
    LaunchedEffect(state.savedSuccess) { if (state.savedSuccess) viewModel.clearSuccess() }

    Box(modifier = Modifier.fillMaxSize().background(LocalAppColors.current.background)) {
        // Scaffold обеспечивает правильное поведение с клавиатурой
        Scaffold(
            containerColor = LocalAppColors.current.background,
            contentColor = LocalAppColors.current.onBackground,
            topBar = {
                Column(modifier = Modifier.statusBarsPadding()) {
                    // ── Хедер ──────────────────────────────────────────
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart).size(36.dp)
                                .background(Color.White.copy(alpha = 0.08f), CircleShape)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClose() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, "Close", tint = LocalAppColors.current.onBackground, modifier = Modifier.size(18.dp))
                        }

                        // Счётчик комментариев — слева, со склонением
                        val count = state.comments.size
                        val word = when {
                            count % 100 in 11..19 -> "комментариев"
                            count % 10 == 1 -> "комментарий"
                            count % 10 in 2..4 -> "комментария"
                            else -> "комментариев"
                        }
                        Text(
                            "$count $word",
                            color = LocalAppColors.current.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.CenterStart).padding(start = 52.dp)
                        )

                        // Кнопка рецензий — заметная с текстом, справа
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .background(
                                    brush = Brush.linearGradient(listOf(GradientStart.copy(alpha = 0.15f), GradientEnd.copy(alpha = 0.15f))),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .border(1.dp, GradientStart.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onOpenReviews() }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(Icons.Default.Star, "Reviews", tint = GradientStart, modifier = Modifier.size(12.dp))
                                Text("Рецензии", color = GradientStart, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // ── Инфо о треке + кружок оценки ───────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(LocalAppColors.current.surface)) {
                            if (!track.imageUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current).data(track.imageUrl).crossfade(false).build(),
                                    contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.MusicNote, null, tint = LocalAppColors.current.secondary, modifier = Modifier.size(22.dp).align(Alignment.Center))
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(track.name, color = LocalAppColors.current.onBackground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(track.artist ?: "", color = LocalAppColors.current.secondary, fontSize = 12.sp, maxLines = 1)
                        }
                        // Кружок с оценкой + карандаш
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Карандаш — редактировать свою оценку
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(LocalAppColors.current.surface, CircleShape)
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                        showRatingSheet = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    androidx.compose.material.icons.Icons.Default.Edit,
                                    "Edit rating",
                                    tint = GradientStart,
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            // Кружок — просмотр средних показателей
                            // Приоритет: средняя всех → своя оценка → null
                            val displayScore = state.averageRating?.avgOverall
                                ?: state.existingRating?.let { r ->
                                    listOfNotNull(
                                        r.rhymeScore?.toDouble(),
                                        r.imageryScore?.toDouble(),
                                        r.structureScore?.toDouble(),
                                        r.charismaScore?.toDouble(),
                                        r.atmosphereScore?.toDouble()
                                    ).takeIf { it.isNotEmpty() }?.average()
                                }

                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(
                                        brush = if (displayScore != null)
                                            Brush.linearGradient(listOf(GradientStart, GradientEnd))
                                        else Brush.linearGradient(listOf(LocalAppColors.current.surface, LocalAppColors.current.surface)),
                                        shape = CircleShape
                                    )
                                    .border(
                                        width = if (displayScore == null) 1.dp else 0.dp,
                                        color = Color.White.copy(alpha = 0.2f), shape = CircleShape
                                    )
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                        showStatsSheet = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (displayScore != null) {
                                    Text(
                                        String.format("%.1f", displayScore),
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-0.5).sp
                                    )
                                } else {
                                    Icon(Icons.Default.Star, "Rate", tint = LocalAppColors.current.secondary, modifier = Modifier.size(22.dp))
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.06f)))
                }
            },
            bottomBar = {
                // ── Поле ввода — прилипает к клавиатуре через Scaffold ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0E0E0E))
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Аватар профиля
                    Box(
                        modifier = Modifier.size(34.dp).clip(CircleShape).background(LocalAppColors.current.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!state.userAvatarUrl.isNullOrEmpty()) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(state.userAvatarUrl).crossfade(false).build(),
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                loading = {
                                    Icon(Icons.Default.Person, null, tint = LocalAppColors.current.secondary, modifier = Modifier.size(18.dp))
                                },
                                error = {
                                    Icon(Icons.Default.Person, null, tint = LocalAppColors.current.secondary, modifier = Modifier.size(18.dp))
                                },
                                success = { SubcomposeAsyncImageContent() }
                            )
                        } else {
                            Icon(Icons.Default.Person, null, tint = LocalAppColors.current.secondary, modifier = Modifier.size(18.dp))
                        }
                    }

                    OutlinedTextField(
                        value = state.commentText,
                        onValueChange = { if (it.length <= 100) viewModel.setCommentText(it) },
                        placeholder = { Text("Комментарий...", color = LocalAppColors.current.secondary, fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = LocalAppColors.current.onBackground, unfocusedTextColor = LocalAppColors.current.onBackground,
                            cursorColor = GradientStart,
                            focusedContainerColor = Color.White.copy(alpha = 0.07f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.07f)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            focusManager.clearFocus()
                            viewModel.submitComment(track.id, track.name, track.artist ?: "", track.imageUrl)
                        })
                    )

                    if (state.commentText.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(brush = Brush.linearGradient(listOf(GradientStart, GradientEnd)), shape = CircleShape)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    focusManager.clearFocus()
                                    viewModel.submitComment(track.id, track.name, track.artist ?: "", track.imageUrl)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                    } // закрываем Row
            }
        ) { innerPadding ->
            // ── Список комментариев ────────────────────────────────────
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GradientStart, modifier = Modifier.size(32.dp))
                }
            } else if (state.comments.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Нет комментариев", color = LocalAppColors.current.onBackground.copy(alpha = 0.5f), fontSize = 15.sp, textAlign = TextAlign.Center)
                        Text("Будьте первым!", color = LocalAppColors.current.secondary, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(state.comments, key = { it.id }) { comment ->
                        CommentRow(
                            comment = comment,
                            avatarUrl = state.userAvatarUrl,
                            currentUserId = state.currentUserId,
                            onDelete = { viewModel.deleteComment(comment.id) },
                            onAvatarClick = {
                                // Не переходим на свой профиль
                                if (comment.userId != state.currentUserId && comment.userId.isNotBlank()) {
                                    navController?.navigate(
                                        com.example.lumisound.navigation.MainDestination.PublicProfile()
                                            .createRoute(comment.userId, comment.username ?: "Пользователь", comment.userAvatarUrl)
                                    )
                                }
                            }
                        )
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).padding(start = 52.dp).background(Color.White.copy(alpha = 0.04f)))
                    }
                }
            }
        }

        // Затемнение — рендерится ДО sheet, чтобы sheet был поверх
        androidx.compose.animation.AnimatedVisibility(
            visible = showRatingSheet || showStatsSheet,
            enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(200)),
            exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        showRatingSheet = false
                        showStatsSheet = false
                    }
            )
        }

        // ── Bottom sheet с оценками (редактирование) ──────────────────
        AnimatedVisibility(
            visible = showRatingSheet,
            enter = slideInVertically(
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                )
            ) { it },
            exit = slideOutVertically(
                animationSpec = androidx.compose.animation.core.tween(200)
            ) { it },
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
        ) {
            RatingBottomSheet(
                state = state, track = track, viewModel = viewModel,
                onDismiss = { showRatingSheet = false }
            )
        }

        // ── Bottom sheet со статистикой (просмотр) ─────────────────────
        AnimatedVisibility(
            visible = showStatsSheet,
            enter = slideInVertically(
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                )
            ) { it },
            exit = slideOutVertically(
                animationSpec = androidx.compose.animation.core.tween(200)
            ) { it },
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
        ) {
            StatsBottomSheet(
                state = state,
                onEditClick = {
                    showStatsSheet = false
                    showRatingSheet = true
                }
            )
        }
    }
}

@Composable
private fun RatingBottomSheet(
    state: ReviewUiState,
    track: Track,
    viewModel: ReviewViewModel,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1C1C), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
    ) {
        Box(modifier = Modifier.width(36.dp).height(4.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp)).align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Оценить трек", color = LocalAppColors.current.onBackground, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            state.overallScore?.let { score ->
                Box(
                    modifier = Modifier.background(brush = Brush.linearGradient(listOf(GradientStart, GradientEnd)), shape = RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(String.format("%.1f", score) + " / 10", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ScoreCriterion.entries.forEach { criterion ->
            val score = when (criterion) {
                ScoreCriterion.RHYME -> state.rhymeScore
                ScoreCriterion.IMAGERY -> state.imageryScore
                ScoreCriterion.STRUCTURE -> state.structureScore
                ScoreCriterion.CHARISMA -> state.charismaScore
                ScoreCriterion.ATMOSPHERE -> state.atmosphereScore
            }
            SheetCriterionRow(label = criterion.label, score = score, onScoreChange = { viewModel.setScore(criterion, it) })
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth().height(48.dp)
                .background(
                    brush = if (state.isRatingComplete) Brush.linearGradient(listOf(GradientStart, GradientEnd))
                    else Brush.linearGradient(listOf(LocalAppColors.current.surface, LocalAppColors.current.surface)),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null,
                    enabled = !state.isSaving && state.isRatingComplete
                ) {
                    viewModel.saveRating(track.id, track.name, track.artist ?: "", track.imageUrl)
                },
            contentAlignment = Alignment.Center
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    if (!state.isRatingComplete) "Оцените все критерии"
                    else if (state.existingRating != null) "Обновить оценку" else "Сохранить оценку",
                    color = if (state.isRatingComplete) Color.White else LocalAppColors.current.secondary,
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (state.savedSuccess) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Оценка сохранена", color = GradientStart, fontSize = 13.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SheetCriterionRow(label: String, score: Int?, onScoreChange: (Int) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = LocalAppColors.current.onBackground, fontSize = 13.sp)
            Box(
                modifier = Modifier
                    .background(
                        if (score != null) GradientStart.copy(alpha = 0.2f) else Color.Transparent,
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    score?.toString() ?: "—",
                    color = if (score != null) GradientStart else LocalAppColors.current.secondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

        // Слайдер вместо кнопок
        androidx.compose.material3.Slider(
            value = (score ?: 0).toFloat(),
            onValueChange = { onScoreChange(it.toInt().coerceIn(1, 10)) },
            valueRange = 1f..10f,
            steps = 8, // 9 шагов = 10 значений
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = GradientStart,
                inactiveTrackColor = Color.White.copy(alpha = 0.12f),
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            )
        )
    }
}

// ── Sheet просмотра статистики ──────────────────────────────────────────────
@Composable
private fun StatsBottomSheet(
    state: ReviewUiState,
    onEditClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1C1C), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
    ) {
        Box(
            modifier = Modifier.width(36.dp).height(4.dp)
                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                .align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Моя оценка", color = LocalAppColors.current.onBackground, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Кнопка редактировать
                Box(
                    modifier = Modifier
                        .background(LocalAppColors.current.surface, RoundedCornerShape(8.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onEditClick() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Edit, null, tint = GradientStart, modifier = Modifier.size(14.dp))
                        Text("Изменить", color = GradientStart, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                // Общая средняя оценка
                val displayScore = state.averageRating?.avgOverall ?: state.existingRating?.overallScore
                if (displayScore != null) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                brush = Brush.linearGradient(listOf(GradientStart, GradientEnd)),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            String.format("%.1f", displayScore),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val rating = state.existingRating
        val avg = state.averageRating

        // Показываем средние всех пользователей, или свою если средних нет
        if (avg != null && avg.ratingCount > 0) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Средняя оценка", color = LocalAppColors.current.secondary, fontSize = 12.sp)
                Text("${avg.ratingCount} ${if (avg.ratingCount == 1) "оценка" else "оценок"}", color = LocalAppColors.current.secondary, fontSize = 12.sp)
            }
            val avgCriteria = listOf(
                ScoreCriterion.RHYME.label to avg.avgRhyme,
                ScoreCriterion.IMAGERY.label to avg.avgImagery,
                ScoreCriterion.STRUCTURE.label to avg.avgStructure,
                ScoreCriterion.CHARISMA.label to avg.avgCharisma,
                ScoreCriterion.ATMOSPHERE.label to avg.avgAtmosphere
            )
            avgCriteria.forEach { (label, score) ->
                if (score != null) {
                    CriterionBar(label = label, value = score, isAverage = true)
                }
            }
        } else if (rating != null) {
            // Fallback — показываем свою оценку
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ваша оценка", color = LocalAppColors.current.secondary, fontSize = 12.sp)
            }
            val myCriteria = listOf(
                ScoreCriterion.RHYME.label to rating.rhymeScore?.toDouble(),
                ScoreCriterion.IMAGERY.label to rating.imageryScore?.toDouble(),
                ScoreCriterion.STRUCTURE.label to rating.structureScore?.toDouble(),
                ScoreCriterion.CHARISMA.label to rating.charismaScore?.toDouble(),
                ScoreCriterion.ATMOSPHERE.label to rating.atmosphereScore?.toDouble()
            )
            myCriteria.forEach { (label, score) ->
                if (score != null) {
                    CriterionBar(label = label, value = score, isAverage = false)
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                Text("Оценок пока нет. Будьте первым!", color = LocalAppColors.current.secondary, fontSize = 14.sp, textAlign = TextAlign.Center)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun CriterionBar(label: String, value: Double, isAverage: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(label, color = LocalAppColors.current.secondary, fontSize = 12.sp, modifier = Modifier.width(140.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(5.dp)
                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((value / 10.0).toFloat().coerceIn(0f, 1f))
                    .height(5.dp)
                    .background(
                        brush = Brush.horizontalGradient(listOf(GradientStart, GradientEnd)),
                        shape = RoundedCornerShape(3.dp)
                    )
            )
        }
        Text(
            if (isAverage) String.format("%.1f", value) else value.toInt().toString(),
            color = GradientStart,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun CommentRow(
    comment: SupabaseService.TrackCommentResponse,
    avatarUrl: String?,
    currentUserId: String?,
    onDelete: () -> Unit,
    onAvatarClick: () -> Unit = {}
) {
    val dateStr = remember(comment.createdAt) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = sdf.parse(comment.createdAt?.take(19) ?: "") ?: Date()
            SimpleDateFormat("d MMM", Locale("ru")).format(date)
        } catch (e: Exception) { "" }
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    val isOwner = currentUserId != null && comment.userId == currentUserId

    // Имя всегда из comment — оно сохраняется при отправке
    val displayName = comment.username?.takeIf { it.isNotBlank() } ?: "Пользователь"
    // Аватар всегда из comment — он сохраняется при отправке
    val displayAvatar = comment.userAvatarUrl?.takeIf { it.isNotBlank() } ?: avatarUrl

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить комментарий?") },
            text = { Text("Вы точно хотите удалить этот комментарий? Это действие нельзя отменить.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) {
                    Text("Удалить", color = Color(0xFFFF5C6C))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            },
            containerColor = LocalAppColors.current.surface,
            titleContentColor = LocalAppColors.current.onBackground,
            textContentColor = LocalAppColors.current.secondary
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(LocalAppColors.current.surface)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onAvatarClick() },
            contentAlignment = Alignment.Center
        ) {
            if (!displayAvatar.isNullOrEmpty()) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(displayAvatar).crossfade(false).build(),
                    contentDescription = "Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = { Icon(Icons.Default.Person, null, tint = LocalAppColors.current.secondary, modifier = Modifier.size(18.dp)) },
                    error = { Icon(Icons.Default.Person, null, tint = LocalAppColors.current.secondary, modifier = Modifier.size(18.dp)) },
                    success = { SubcomposeAsyncImageContent() }
                )
            } else {
                Icon(Icons.Default.Person, null, tint = LocalAppColors.current.secondary, modifier = Modifier.size(18.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(displayName, color = LocalAppColors.current.onBackground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("• $dateStr", color = LocalAppColors.current.secondary, fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(comment.comment, color = LocalAppColors.current.onBackground, fontSize = 14.sp, lineHeight = 20.sp)
        }
        if (isOwner) {
            Icon(
                Icons.Default.Delete, "Delete",
                tint = Color.White.copy(alpha = 0.6f), // белая, видимая
                modifier = Modifier
                    .size(16.dp)
                    .padding(top = 2.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        showDeleteDialog = true
                    }
            )
        }
    }
}
