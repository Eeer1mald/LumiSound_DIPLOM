package com.example.lumisound.feature.ratings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.lumisound.data.remote.SupabaseService
import com.example.lumisound.ui.theme.ColorBackground
import com.example.lumisound.ui.theme.ColorOnBackground
import com.example.lumisound.ui.theme.ColorSecondary
import com.example.lumisound.ui.theme.ColorSurface
import com.example.lumisound.ui.theme.GradientEnd
import com.example.lumisound.ui.theme.GradientStart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RatedTrack(
    val id: String,
    val title: String,
    val artist: String,
    val coverUrl: String? = null,
    val rating: Int,
    val ratedAt: String
)

@Composable
fun RatingsScreen(
    navController: NavHostController,
    ratedTracks: List<RatedTrack> = emptyList(),
    onTrackClick: (String) -> Unit = {},
    viewModel: RatingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // ── Средняя оценка — красивый блок сверху ──────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    // Фоновый градиентный блок
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = GradientStart.copy(alpha = 0.4f))
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(
                                        GradientStart.copy(alpha = 0.9f),
                                        GradientEnd.copy(alpha = 0.7f),
                                        Color(0xFF1A0A2E)
                                    )
                                ),
                                shape = RoundedCornerShape(24.dp)
                            )
                    )
                    // Контент поверх
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Левая колонка — оценок
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                state.ratings.size.toString(),
                                color = Color.White,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text("оценок", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        }

                        // Центр — большая средняя оценка
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    state.averageScore?.let { String.format("%.1f", it) } ?: "—",
                                    color = Color.White,
                                    fontSize = 52.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-1).sp
                                )
                                Text(
                                    "/10",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                repeat(5) { i ->
                                    val filled = (state.averageScore ?: 0.0) / 2.0 > i
                                    Icon(
                                        Icons.Default.Star,
                                        null,
                                        tint = if (filled) Color.White else Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        // Правая колонка — рецензий
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                state.myReviews.size.toString(),
                                color = Color.White,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text("рецензий", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }
                }
            }

            // ── Табы ───────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RatingsTab.entries.forEach { tab ->
                        val active = state.activeTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .background(
                                    if (active) Brush.linearGradient(listOf(GradientStart, GradientEnd))
                                    else Brush.linearGradient(listOf(ColorSurface.copy(alpha = 0.4f), ColorSurface.copy(alpha = 0.4f))),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    viewModel.setTab(tab)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                tab.label,
                                color = if (active) Color.White else ColorSecondary,
                                fontSize = 13.sp,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (state.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GradientStart, modifier = Modifier.size(36.dp))
                    }
                }
            } else when (state.activeTab) {
                RatingsTab.BEST -> {
                    if (state.bestReviews.isEmpty()) {
                        item { EmptyState("Нет рецензий", "Добавьте треки и артистов в избранное, чтобы видеть лучшие рецензии") }
                    } else {
                        items(state.bestReviews, key = { "best_${it.id}" }) { rating ->
                            ReviewCard(rating = rating, navController = navController)
                        }
                    }
                }
                RatingsTab.MINE -> {
                    if (state.myReviews.isEmpty()) {
                        item { EmptyState("Нет ваших рецензий", "Нажмите 💬 в плеере, чтобы написать рецензию") }
                    } else {
                        items(state.myReviews, key = { "mine_${it.id}" }) { rating ->
                            ReviewCard(rating = rating, navController = navController)
                        }
                    }
                }
            }
        }
    }
}

// ── Карточка рецензии ────────────────────────────────────────────────────────
@Composable
private fun ReviewCard(
    rating: SupabaseService.TrackRatingResponse,
    navController: NavHostController? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val dateStr = remember(rating.createdAt) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = sdf.parse(rating.createdAt?.take(19) ?: "") ?: Date()
            SimpleDateFormat("d MMMM", Locale("ru")).format(date)
        } catch (e: Exception) { "" }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .background(ColorSurface.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { expanded = !expanded }
            .padding(16.dp)
    ) {
        // Аватар + имя автора
        val displayName = rating.username?.takeIf { it.isNotBlank() } ?: "Пользователь"
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(ColorSurface)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        if (rating.userId.isNotBlank()) {
                            navController?.navigate(
                                com.example.lumisound.navigation.MainDestination.PublicProfile()
                                    .createRoute(rating.userId, displayName, rating.userAvatarUrl)
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (!rating.userAvatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(rating.userAvatarUrl).crossfade(false).build(),
                        contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, null, tint = ColorSecondary, modifier = Modifier.size(14.dp))
                }
            }
            Text(displayName, color = ColorSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.weight(1f))
            Text(dateStr, color = ColorSecondary, fontSize = 11.sp)
        }

        // Трек + оценка
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(ColorSurface)) {
                if (!rating.trackCoverUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(rating.trackCoverUrl).crossfade(false).build(),
                        contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.MusicNote, null, tint = ColorSecondary, modifier = Modifier.size(24.dp).align(Alignment.Center))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(rating.trackTitle, color = ColorOnBackground, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(rating.trackArtist, color = ColorSecondary, fontSize = 12.sp, maxLines = 1)
                Spacer(modifier = Modifier.height(6.dp))
                rating.overallScore?.let { score ->
                    Row(
                        modifier = Modifier
                            .background(brush = Brush.linearGradient(listOf(GradientStart, GradientEnd)), shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Star, null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Text(String.format("%.1f", score), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                        Text("/10", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                    }
                }
            }
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                null, tint = ColorSecondary, modifier = Modifier.size(20.dp)
            )
        }

        // Текст рецензии
        rating.review?.takeIf { it.isNotBlank() }?.let { review ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                review,
                color = ColorOnBackground.copy(alpha = 0.9f),
                fontSize = 14.sp,
                lineHeight = 21.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = if (expanded) TextOverflow.Visible else TextOverflow.Ellipsis
            )
        }

        // Репутация
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val repColor = when {
                rating.reputation > 0 -> Color(0xFF2ECC71)
                rating.reputation < 0 -> Color(0xFFE74C3C)
                else -> ColorSecondary
            }
            if (rating.reputation != 0) {
                Text(
                    "${if (rating.reputation > 0) "+" else ""}${rating.reputation}",
                    color = repColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Раскрытые критерии
        AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
            Column {
                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.07f)))
                Spacer(modifier = Modifier.height(10.dp))
                val criteria = listOf(
                    "Рифмы / Образы" to rating.rhymeScore,
                    "Структура / Ритмика" to rating.imageryScore,
                    "Реализация стиля" to rating.structureScore,
                    "Индивидуальность" to rating.charismaScore,
                    "Атмосфера / Вайб" to rating.atmosphereScore
                )
                criteria.filter { it.second != null }.forEach { (label, score) ->
                    if (score != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(label, color = ColorSecondary, fontSize = 11.sp, modifier = Modifier.width(130.dp))
                            Box(modifier = Modifier.weight(1f).height(4.dp).background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(2.dp))) {
                                Box(modifier = Modifier.fillMaxWidth(score / 10f).height(4.dp).background(GradientStart, RoundedCornerShape(2.dp)))
                            }
                            Text("$score", color = GradientStart, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp), textAlign = TextAlign.End)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("📝", fontSize = 36.sp)
        Text(title, color = ColorOnBackground.copy(alpha = 0.7f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(subtitle, color = ColorSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}
