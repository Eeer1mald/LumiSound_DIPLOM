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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
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
import com.example.lumisound.ui.theme.ColorBackground
import com.example.lumisound.ui.theme.ColorOnBackground
import com.example.lumisound.ui.theme.ColorSecondary
import com.example.lumisound.ui.theme.ColorSurface
import com.example.lumisound.ui.theme.GradientEnd
import com.example.lumisound.ui.theme.GradientStart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReviewsScreen(
    track: Track,
    onClose: () -> Unit,
    viewModel: ReviewViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current
    var showAddReview by remember { mutableStateOf(false) }

    LaunchedEffect(track.id) { viewModel.loadForTrack(track.id) }
    LaunchedEffect(state.savedSuccess) { if (state.savedSuccess) viewModel.clearSuccess() }

    Box(modifier = Modifier.fillMaxSize().background(ColorBackground)) {
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
                    Icon(Icons.Default.Close, "Close", tint = ColorOnBackground, modifier = Modifier.size(18.dp))
                }

                Text(
                    "Рецензии",
                    color = ColorOnBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )

                // Кнопка добавить рецензию
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(36.dp)
                        .background(
                            if (showAddReview) GradientStart else ColorSurface,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            showAddReview = !showAddReview
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, "Add review", tint = if (showAddReview) Color.White else GradientStart, modifier = Modifier.size(18.dp))
                }
            }

            // ── Инфо о треке ───────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(ColorSurface)) {
                    if (!track.imageUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(track.imageUrl).crossfade(false).build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.MusicNote, null, tint = ColorSecondary, modifier = Modifier.size(22.dp).align(Alignment.Center))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(track.name, color = ColorOnBackground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist ?: "", color = ColorSecondary, fontSize = 12.sp, maxLines = 1)
                }
                // Общая оценка
                state.existingRating?.overallScore?.let { score ->
                    Box(
                        modifier = Modifier
                            .background(brush = Brush.linearGradient(listOf(GradientStart, GradientEnd)), shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(String.format("%.1f", score) + "/10", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.06f)))

            // ── Форма добавления рецензии ──────────────────────────────
            if (showAddReview) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF111111))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text("Оценки (обязательно)", color = ColorSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))

                    // 5 критериев — обязательны для рецензии
                    ScoreCriterion.entries.forEach { criterion ->
                        val score = when (criterion) {
                            ScoreCriterion.RHYME -> state.rhymeScore
                            ScoreCriterion.IMAGERY -> state.imageryScore
                            ScoreCriterion.STRUCTURE -> state.structureScore
                            ScoreCriterion.CHARISMA -> state.charismaScore
                            ScoreCriterion.ATMOSPHERE -> state.atmosphereScore
                        }
                        Column(modifier = Modifier.padding(bottom = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(criterion.label, color = ColorOnBackground, fontSize = 12.sp)
                                score?.let { Text("$it", color = GradientStart, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                    ?: Text("—", color = ColorSecondary, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                (1..10).forEach { v ->
                                    val isSelected = score != null && v <= score
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(22.dp)
                                            .background(
                                                if (isSelected) GradientStart else Color.White.copy(alpha = 0.07f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                                viewModel.setScore(criterion, v)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("$v", color = if (isSelected) Color.White else ColorSecondary, fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Итоговая оценка
                    state.overallScore?.let { score ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Итого", color = ColorSecondary, fontSize = 13.sp)
                            Box(
                                modifier = Modifier
                                    .background(brush = Brush.linearGradient(listOf(GradientStart, GradientEnd)), shape = RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(String.format("%.1f", score) + " / 10", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Рецензия (необязательно)", color = ColorSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
                    OutlinedTextField(
                        value = state.review,
                        onValueChange = { viewModel.setReview(it) },
                        placeholder = { Text("Поделитесь впечатлениями о треке...", color = ColorSecondary, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GradientStart,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedTextColor = ColorOnBackground,
                            unfocusedTextColor = ColorOnBackground,
                            cursorColor = GradientStart,
                            focusedContainerColor = Color.White.copy(alpha = 0.04f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.04f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        maxLines = 5,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(
                                brush = if (state.isRatingComplete)
                                    Brush.linearGradient(listOf(GradientStart, GradientEnd))
                                else Brush.linearGradient(listOf(ColorSurface, ColorSurface)),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = !state.isSaving && state.isRatingComplete
                            ) {
                                focusManager.clearFocus()
                                viewModel.saveRating(track.id, track.name, track.artist ?: "", track.imageUrl)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(
                                if (!state.isRatingComplete) "Оцените все критерии" else "Опубликовать рецензию",
                                color = if (state.isRatingComplete) Color.White else ColorSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (state.savedSuccess) {
                        Text("Рецензия сохранена", color = GradientStart, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), textAlign = TextAlign.Center)
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.06f)))
            }

            // ── Список рецензий ────────────────────────────────────────
            if (state.isLoading) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
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
                            Text("Нет рецензий", color = ColorOnBackground.copy(alpha = 0.5f), fontSize = 15.sp, textAlign = TextAlign.Center)
                            Text("Нажмите карандаш, чтобы написать", color = ColorSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
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
                                onVote = { vote -> viewModel.voteReview(track.id, rating.id, vote) }
                            )
                        }
                    }
                }
            }

            // ── Поле добавления комментария снизу ─────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111111))
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).background(ColorSurface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MusicNote, null, tint = ColorSecondary, modifier = Modifier.size(16.dp))
                    }
                    OutlinedTextField(
                        value = state.commentText,
                        onValueChange = { viewModel.setCommentText(it) },
                        placeholder = { Text("Комментарий к рецензии...", color = ColorSecondary, fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = ColorOnBackground,
                            unfocusedTextColor = ColorOnBackground,
                            cursorColor = GradientStart,
                            focusedContainerColor = Color.White.copy(alpha = 0.06f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.06f)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        maxLines = 2,
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
                }
            }
        }
    }
}

@Composable
private fun ReviewCard(
    rating: com.example.lumisound.data.remote.SupabaseService.TrackRatingResponse,
    myVote: Int? = null,
    currentUserId: String? = null,
    onVote: (Int) -> Unit = {}
) {
    val dateStr = remember(rating.createdAt) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = sdf.parse(rating.createdAt?.take(19) ?: "") ?: Date()
            SimpleDateFormat("d MMM yyyy", Locale("ru")).format(date)
        } catch (e: Exception) { "" }
    }

    val isOwn = currentUserId != null && rating.userId == currentUserId
    val displayName = rating.username?.takeIf { it.isNotBlank() } ?: if (isOwn) "Вы" else "Пользователь"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorSurface.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
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
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(ColorSurface),
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
                        androidx.compose.material3.Icon(
                            Icons.Default.MusicNote, null,
                            tint = ColorSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Column {
                    Text(displayName, color = ColorOnBackground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(dateStr, color = ColorSecondary, fontSize = 11.sp)
                }
            }
            rating.overallScore?.let { score ->
                Box(
                    modifier = Modifier
                        .background(brush = Brush.linearGradient(listOf(GradientStart, GradientEnd)), shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(String.format("%.1f", score), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        // Мини-шкалы критериев
        val criteria = listOf(
            "Рифмы / Образы" to rating.rhymeScore,
            "Структура / Ритмика" to rating.imageryScore,
            "Реализация стиля" to rating.structureScore,
            "Индивидуальность" to rating.charismaScore,
            "Атмосфера / Вайб" to rating.atmosphereScore
        )
        val filledCriteria = criteria.filter { it.second != null }
        if (filledCriteria.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            filledCriteria.forEach { (label, score) ->
                if (score != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(label, color = ColorSecondary, fontSize = 11.sp, modifier = Modifier.width(120.dp))
                        Box(
                            modifier = Modifier.weight(1f).height(4.dp)
                                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(2.dp))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth(score / 10f).height(4.dp)
                                    .background(GradientStart, RoundedCornerShape(2.dp))
                            )
                        }
                        Text("$score", color = GradientStart, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Текст рецензии
        rating.review?.takeIf { it.isNotBlank() }?.let { review ->
            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.06f)))
            Spacer(modifier = Modifier.height(10.dp))
            Text(review, color = ColorOnBackground, fontSize = 14.sp, lineHeight = 22.sp)
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
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        onVote(1)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("↑", color = if (myVote == 1) Color(0xFF2ECC71) else ColorSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Счётчик репутации
            val repColor = when {
                rating.reputation > 0 -> Color(0xFF2ECC71)
                rating.reputation < 0 -> Color(0xFFE74C3C)
                else -> ColorSecondary
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
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        onVote(-1)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("↓", color = if (myVote == -1) Color(0xFFE74C3C) else ColorSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
