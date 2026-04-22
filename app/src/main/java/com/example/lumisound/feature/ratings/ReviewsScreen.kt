package com.example.lumisound.feature.ratings

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import coil.request.ImageRequest
import com.example.lumisound.data.model.Track
import com.example.lumisound.ui.theme.GradientEnd
import com.example.lumisound.ui.theme.GradientStart
import com.example.lumisound.ui.theme.LocalAppColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReviewsScreen(
    track: Track,
    onClose: () -> Unit,
    navController: androidx.navigation.NavHostController? = null,
    viewModel: ReviewViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current
    var showAddReview by remember { mutableStateOf(false) }
    var showRatingSheet by remember { mutableStateOf(false) }
    var showStatsSheet by remember { mutableStateOf(false) }

    LaunchedEffect(track.id) { viewModel.loadForTrack(track.id) }
    LaunchedEffect(state.savedSuccess) { if (state.savedSuccess) viewModel.clearSuccess() }

    Box(modifier = Modifier.fillMaxSize().background(LocalAppColors.current.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ── Хедер ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = LocalAppColors.current.onBackground, modifier = Modifier.size(18.dp))
                }

                Text(
                    "Рецензии",
                    color = LocalAppColors.current.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // ── Инфо о треке ───────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(LocalAppColors.current.surface)) {
                    if (!track.imageUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(track.imageUrl).crossfade(false).build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.MusicNote, null, tint = LocalAppColors.current.secondary, modifier = Modifier.size(22.dp).align(Alignment.Center))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(track.name, color = LocalAppColors.current.onBackground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist ?: "", color = LocalAppColors.current.secondary, fontSize = 12.sp, maxLines = 1)
                }
                // Карандаш + кружок оценки
                val myScore = state.existingRating?.let { r ->
                    listOfNotNull(
                        r.rhymeScore?.toDouble(), r.imageryScore?.toDouble(),
                        r.structureScore?.toDouble(), r.charismaScore?.toDouble(),
                        r.atmosphereScore?.toDouble()
                    ).takeIf { it.isNotEmpty() }?.average()
                }
                // Кружок показывает среднюю всех пользователей (или свою если средней нет)
                val displayScore = state.averageRating?.avgOverall ?: myScore
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Карандаш — редактировать оценку
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(LocalAppColors.current.surface, CircleShape)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                showRatingSheet = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Edit, "Edit rating", tint = GradientStart, modifier = Modifier.size(15.dp))
                    }
                    // Кружок — просмотр оценок
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
                                color = Color.White, fontSize = 16.sp,
                                fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp
                            )
                        } else {
                            Icon(Icons.Default.Star, null, tint = LocalAppColors.current.secondary, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.06f)))

            // ── Список рецензий ────────────────────────────────────────
            if (state.isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GradientStart, modifier = Modifier.size(32.dp))
                }
            } else {
                // Рецензии из state.reviews (все пользователи, отсортированы по репутации)
                val reviewsToShow = state.reviews.filter { !it.review.isNullOrBlank() }
                if (reviewsToShow.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Нет рецензий", color = LocalAppColors.current.onBackground.copy(alpha = 0.5f), fontSize = 15.sp, textAlign = TextAlign.Center)
                            Text("Нажмите карандаш, чтобы написать", color = LocalAppColors.current.secondary, fontSize = 13.sp, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(reviewsToShow, key = { it.id }) { rating ->
                            ReviewCard(
                                rating = rating,
                                myVote = state.myVotes[rating.id],
                                currentUserId = state.currentUserId,
                                isVoting = state.votingIds.contains(rating.id),
                                onVote = { vote -> viewModel.voteReview(track.id, rating.id, vote) },
                                onProfileClick = { userId ->
                                    navController?.navigate(
                                        com.example.lumisound.navigation.MainDestination.PublicProfile()
                                            .createRoute(userId, rating.username ?: "Пользователь", rating.userAvatarUrl)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // ── Поле написания рецензии снизу ─────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0E0E0E))
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(LocalAppColors.current.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!state.userAvatarUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(state.userAvatarUrl).crossfade(false).build(),
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Person, null, tint = LocalAppColors.current.secondary, modifier = Modifier.size(16.dp))
                        }
                    }
                    OutlinedTextField(
                        value = state.review,
                        onValueChange = { if (it.length <= 2000) viewModel.setReview(it) },
                        placeholder = { Text("Написать рецензию... (нужна оценка)", color = LocalAppColors.current.secondary, fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = LocalAppColors.current.onBackground, unfocusedTextColor = LocalAppColors.current.onBackground,
                            cursorColor = GradientStart,
                            focusedContainerColor = Color.White.copy(alpha = 0.07f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.07f)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (state.isRatingComplete && state.review.isNotBlank()) {
                                focusManager.clearFocus()
                                viewModel.saveRating(track.id, track.name, track.artist ?: "", track.imageUrl)
                            }
                        })
                    )
                    if (state.review.isNotBlank()) {
                        // Можно отправить если: оценки выставлены ИЛИ уже есть существующая оценка
                        val canSend = (state.isRatingComplete || state.existingRating != null) && !state.isSaving
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    brush = if (canSend)
                                        Brush.linearGradient(listOf(GradientStart, GradientEnd))
                                    else Brush.linearGradient(listOf(LocalAppColors.current.surface, LocalAppColors.current.surface)),
                                    shape = CircleShape
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    enabled = canSend
                                ) {
                                    if (state.isRatingComplete || state.existingRating != null) {
                                        focusManager.clearFocus()
                                        viewModel.saveRating(track.id, track.name, track.artist ?: "", track.imageUrl)
                                    } else {
                                        showRatingSheet = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.isSaving) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else if (canSend) {
                                Icon(Icons.Default.Check, "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                            } else {
                                Icon(Icons.Default.Star, "Rate first", tint = LocalAppColors.current.secondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                if (!state.isRatingComplete && state.review.isNotBlank()) {
                    Text(
                        "Сначала поставьте оценку (нажмите на кружок)",
                        color = GradientStart.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 6.dp)
                    )
                }
                if (state.savedSuccess) {
                    Text(
                        "Рецензия опубликована",
                        color = GradientStart,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 6.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // ── Затемнение под sheet ───────────────────────────────────
        if (showRatingSheet || showStatsSheet) {
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

        // ── Bottom sheet с оценками ────────────────────────────────
        androidx.compose.animation.AnimatedVisibility(
            visible = showRatingSheet,
            enter = androidx.compose.animation.slideInVertically(
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                )
            ) { it },
            exit = androidx.compose.animation.slideOutVertically(
                animationSpec = androidx.compose.animation.core.tween(200)
            ) { it },
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
        ) {
            ReviewRatingSheet(
                state = state, track = track, viewModel = viewModel,
                onDismiss = { showRatingSheet = false }
            )
        }

        // ── Bottom sheet просмотра оценок ──────────────────────────
        androidx.compose.animation.AnimatedVisibility(
            visible = showStatsSheet,
            enter = androidx.compose.animation.slideInVertically(
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                )
            ) { it },
            exit = androidx.compose.animation.slideOutVertically(
                animationSpec = androidx.compose.animation.core.tween(200)
            ) { it },
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
        ) {
            ReviewStatsSheet(
                state = state,
                onEditClick = { showStatsSheet = false; showRatingSheet = true },
                onDismiss = { showStatsSheet = false }
            )
        }
    }
}

@Composable
private fun ReviewRatingSheet(
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
                Box(modifier = Modifier.background(brush = Brush.linearGradient(listOf(GradientStart, GradientEnd)), shape = RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
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
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(criterion.label, color = LocalAppColors.current.onBackground, fontSize = 13.sp)
                    score?.let { Text("$it", color = GradientStart, fontSize = 13.sp, fontWeight = FontWeight.Bold) } ?: Text("—", color = LocalAppColors.current.secondary, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                androidx.compose.material3.Slider(
                    value = (score ?: 0).toFloat(),
                    onValueChange = { viewModel.setScore(criterion, it.toInt().coerceIn(1, 10)) },
                    valueRange = 1f..10f, steps = 8,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.SliderDefaults.colors(
                        thumbColor = Color.White, activeTrackColor = GradientStart,
                        inactiveTrackColor = Color.White.copy(alpha = 0.12f),
                        activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent
                    )
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(48.dp)
                .background(
                    brush = if (state.isRatingComplete) Brush.linearGradient(listOf(GradientStart, GradientEnd))
                    else Brush.linearGradient(listOf(LocalAppColors.current.surface, LocalAppColors.current.surface)),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = !state.isSaving && state.isRatingComplete) {
                    viewModel.saveRating(track.id, track.name, track.artist ?: "", track.imageUrl)
                    onDismiss()
                },
            contentAlignment = Alignment.Center
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    if (!state.isRatingComplete) "Оцените все критерии" else if (state.existingRating != null) "Обновить оценку" else "Сохранить оценку",
                    color = if (state.isRatingComplete) Color.White else LocalAppColors.current.secondary,
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ReviewStatsSheet(
    state: ReviewUiState,
    onEditClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val myScore = state.existingRating?.let { r ->
        listOfNotNull(
            r.rhymeScore?.toDouble(), r.imageryScore?.toDouble(),
            r.structureScore?.toDouble(), r.charismaScore?.toDouble(),
            r.atmosphereScore?.toDouble()
        ).takeIf { it.isNotEmpty() }?.average()
    }
    val avgScore = state.averageRating?.avgOverall

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
            Text("Моя оценка", color = LocalAppColors.current.onBackground, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            myScore?.let {
                Box(modifier = Modifier.background(brush = Brush.linearGradient(listOf(GradientStart, GradientEnd)), shape = RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(String.format("%.1f", it) + " / 10", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                }
            } ?: Text("Нет оценки", color = LocalAppColors.current.secondary, fontSize = 14.sp)
        }
        avgScore?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Средняя оценка всех: ${String.format("%.1f", it)}", color = LocalAppColors.current.secondary, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        val criteria = listOf(
            "Рифмы / Образы" to state.existingRating?.rhymeScore,
            "Структура / Ритмика" to state.existingRating?.imageryScore,
            "Реализация стиля" to state.existingRating?.structureScore,
            "Индивидуальность" to state.existingRating?.charismaScore,
            "Атмосфера / Вайб" to state.existingRating?.atmosphereScore
        )
        criteria.forEach { (label, score) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(label, color = LocalAppColors.current.secondary, fontSize = 12.sp, modifier = Modifier.width(130.dp))
                Box(modifier = Modifier.weight(1f).height(4.dp).background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(2.dp))) {
                    if (score != null) Box(modifier = Modifier.fillMaxWidth(score / 10f).height(4.dp).background(GradientStart, RoundedCornerShape(2.dp)))
                }
                Text(score?.toString() ?: "—", color = if (score != null) GradientStart else LocalAppColors.current.secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(44.dp)
                .background(LocalAppColors.current.surface, RoundedCornerShape(12.dp))
                .border(1.dp, GradientStart.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onEditClick() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Edit, null, tint = GradientStart, modifier = Modifier.size(16.dp))
                Text(if (myScore != null) "Изменить оценку" else "Поставить оценку", color = GradientStart, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ReviewCard(
    rating: com.example.lumisound.data.remote.SupabaseService.TrackRatingResponse,
    myVote: Int? = null,
    currentUserId: String? = null,
    isVoting: Boolean = false,
    onVote: (Int) -> Unit = {},
    onProfileClick: ((String) -> Unit)? = null
) {
    val dateStr = remember(rating.createdAt) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = sdf.parse(rating.createdAt?.take(19) ?: "") ?: Date()
            SimpleDateFormat("d MMM yyyy", Locale("ru")).format(date)
        } catch (e: Exception) { "" }
    }

    val isOwn = currentUserId != null && rating.userId == currentUserId
    val displayName = rating.username?.takeIf { it.isNotBlank() } ?: "Пользователь"
    var showCriteria by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LocalAppColors.current.surface.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        // Хедер: аватар + имя + дата + оценка
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(LocalAppColors.current.surface)
                        .then(
                            if (onProfileClick != null && rating.userId != null && !isOwn)
                                Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onProfileClick(rating.userId) }
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (!rating.userAvatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(rating.userAvatarUrl).crossfade(false).build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Person, null, tint = LocalAppColors.current.secondary, modifier = Modifier.size(16.dp))
                    }
                }
                Column {
                    Text(displayName, color = LocalAppColors.current.onBackground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(dateStr, color = LocalAppColors.current.secondary, fontSize = 11.sp)
                }
            }
            // Квадрат с оценкой — тёмный с фиолетовым акцентом
            rating.overallScore?.let { score ->
                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0xFF1E1E1E),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(1.dp, GradientStart.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            showCriteria = !showCriteria
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(String.format("%.1f", score), color = GradientStart, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        // Критерии — только при showCriteria
        val criteria = listOf(
            "Рифмы / Образы" to rating.rhymeScore,
            "Структура / Ритмика" to rating.imageryScore,
            "Реализация стиля" to rating.structureScore,
            "Индивидуальность" to rating.charismaScore,
            "Атмосфера / Вайб" to rating.atmosphereScore
        )
        val filledCriteria = criteria.filter { it.second != null }
        androidx.compose.animation.AnimatedVisibility(
            visible = showCriteria && filledCriteria.isNotEmpty(),
            enter = androidx.compose.animation.expandVertically(
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                )
            ) + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(200)),
            exit = androidx.compose.animation.shrinkVertically(
                animationSpec = androidx.compose.animation.core.tween(180)
            ) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(150))
        ) {
            Column {
                Spacer(modifier = Modifier.height(10.dp))
                filledCriteria.forEach { (label, score) ->
                    if (score != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(label, color = LocalAppColors.current.secondary, fontSize = 11.sp, modifier = Modifier.width(120.dp))
                            Box(
                                modifier = Modifier.weight(1f).height(4.dp)
                                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(2.dp))
                            ) {
                                Box(modifier = Modifier.fillMaxWidth(score / 10f).height(4.dp).background(GradientStart, RoundedCornerShape(2.dp)))
                            }
                            Text("$score", color = GradientStart, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Текст рецензии
        rating.review?.takeIf { it.isNotBlank() }?.let { review ->
            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.06f)))
            Spacer(modifier = Modifier.height(10.dp))
            Text(review, color = LocalAppColors.current.onBackground, fontSize = 14.sp, lineHeight = 22.sp)
        }

        // Голосование за репутацию
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Стрелка вверх
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        if (myVote == 1) Color(0xFF2ECC71).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.06f),
                        RoundedCornerShape(6.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = !isVoting && !isOwn
                    ) { onVote(1) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "↑",
                    color = when {
                        isOwn -> LocalAppColors.current.secondary.copy(alpha = 0.3f)
                        isVoting -> LocalAppColors.current.secondary.copy(alpha = 0.4f)
                        myVote == 1 -> Color(0xFF2ECC71)
                        else -> LocalAppColors.current.secondary
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Счётчик репутации
            val repColor = when {
                rating.reputation > 0 -> Color(0xFF2ECC71)
                rating.reputation < 0 -> Color(0xFFE74C3C)
                else -> LocalAppColors.current.secondary
            }
            Text(
                "${if (rating.reputation > 0) "+" else ""}${rating.reputation}",
                color = repColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(32.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Стрелка вниз
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        if (myVote == -1) Color(0xFFE74C3C).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.06f),
                        RoundedCornerShape(6.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = !isVoting && !isOwn
                    ) { onVote(-1) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "↓",
                    color = when {
                        isOwn -> LocalAppColors.current.secondary.copy(alpha = 0.3f)
                        isVoting -> LocalAppColors.current.secondary.copy(alpha = 0.4f)
                        myVote == -1 -> Color(0xFFE74C3C)
                        else -> LocalAppColors.current.secondary
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
