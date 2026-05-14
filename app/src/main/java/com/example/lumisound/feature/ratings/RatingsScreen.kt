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
import com.example.lumisound.data.model.Track
import com.example.lumisound.data.remote.SupabaseService
import com.example.lumisound.ui.theme.GradientEnd
import com.example.lumisound.ui.theme.GradientStart
import com.example.lumisound.ui.theme.LocalAppColors
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
    viewModel: RatingsViewModel = hiltViewModel(),
    playerViewModel: com.example.lumisound.feature.nowplaying.PlayerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalAppColors.current.background)
            .statusBarsPadding()
    ) {
        val gradientBrush = remember {
            androidx.compose.ui.graphics.Brush.linearGradient(
                listOf(GradientStart.copy(alpha = 0.9f), GradientEnd.copy(alpha = 0.7f), Color(0xFF1A0A2E))
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // ── Средняя оценка — красивый блок сверху ──────────────────
            item(key = "header") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(brush = gradientBrush, shape = RoundedCornerShape(24.dp))
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

            // ── Табы в стиле плейлистов ────────────────────────────────
            item(key = "tabs") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RatingsTab.entries.forEach { tab ->
                        val active = state.activeTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .background(
                                    if (active) GradientStart.copy(alpha = 0.18f)
                                    else LocalAppColors.current.surface,
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    1.dp,
                                    if (active) GradientStart.copy(alpha = 0.6f)
                                    else Color.White.copy(alpha = 0.07f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    viewModel.setTab(tab)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                tab.label,
                                color = if (active) GradientStart else LocalAppColors.current.secondary,
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
                        item { EmptyState("Нет рецензий", "Пока никто не написал рецензий") }
                    } else {
                        items(state.bestReviews, key = { "best_${it.id}" }) { rating ->
                            ReviewCard(rating = rating, navController = navController, playerViewModel = playerViewModel, getStreamUrl = { viewModel.getStreamUrl(it) })
                        }
                    }
                }
                RatingsTab.MINE -> {
                    if (state.myReviews.isEmpty()) {
                        item { EmptyState("Нет ваших рецензий", "Нажмите 💬 в плеере, чтобы написать рецензию") }
                    } else {
                        items(state.myReviews, key = { "mine_${it.id}" }) { rating ->
                            ReviewCard(rating = rating, navController = navController, playerViewModel = playerViewModel, getStreamUrl = { viewModel.getStreamUrl(it) })
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
    navController: NavHostController? = null,
    playerViewModel: com.example.lumisound.feature.nowplaying.PlayerViewModel? = null,
    getStreamUrl: ((String) -> String)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val scoreBrush = remember { androidx.compose.ui.graphics.Brush.linearGradient(listOf(GradientStart, GradientEnd)) }
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
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .background(LocalAppColors.current.surface, RoundedCornerShape(18.dp))
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(18.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { expanded = !expanded }
            .padding(14.dp)
    ) {
        // Трек + оценка в одной строке
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Обложка — кликабельная для запуска трека
            Box(modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(LocalAppColors.current.background)
                .then(
                    if (playerViewModel != null)
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            val isCustom = rating.audiusTrackId.startsWith("custom_")
                            val streamUrl = if (isCustom) null
                                else getStreamUrl?.invoke(rating.audiusTrackId)
                            playerViewModel.playTrack(
                                Track(
                                    id = rating.audiusTrackId,
                                    name = rating.trackTitle,
                                    artist = rating.trackArtist,
                                    imageUrl = rating.trackCoverUrl,
                                    hdImageUrl = rating.trackCoverUrl,
                                    previewUrl = streamUrl
                                )
                            )
                        }
                    else Modifier
                )
            ) {
                if (!rating.trackCoverUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(rating.trackCoverUrl).crossfade(false).build(),
                        contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(
                        Brush.linearGradient(listOf(GradientStart.copy(alpha = 0.3f), Color.Black.copy(alpha = 0.5f)))
                    ), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(22.dp))
                    }
                }
            }

            // Название + артист
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(rating.trackTitle, color = LocalAppColors.current.onBackground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(rating.trackArtist, color = LocalAppColors.current.secondary, fontSize = 12.sp, maxLines = 1)
            }

            // Оценка — компактный бейдж
            rating.overallScore?.let { score ->
                Box(
                    modifier = Modifier
                        .background(brush = scoreBrush, shape = RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        String.format("%.1f", score),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                }
            }

            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                null, tint = LocalAppColors.current.secondary.copy(alpha = 0.6f), modifier = Modifier.size(18.dp)
            )
        }

        // Автор + дата
        val displayName = rating.username?.takeIf { it.isNotBlank() } ?: "Пользователь"
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier.size(20.dp).clip(CircleShape).background(LocalAppColors.current.background)
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
                    Icon(Icons.Default.Person, null, tint = LocalAppColors.current.secondary, modifier = Modifier.size(11.dp))
                }
            }
            Text(displayName, color = LocalAppColors.current.secondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            if (dateStr.isNotBlank()) {
                Text("·", color = LocalAppColors.current.secondary.copy(alpha = 0.4f), fontSize = 11.sp)
                Text(dateStr, color = LocalAppColors.current.secondary.copy(alpha = 0.6f), fontSize = 11.sp)
            }
            // Репутация
            if (rating.reputation != 0) {
                Spacer(modifier = Modifier.weight(1f))
                val repColor = if (rating.reputation > 0) Color(0xFF2ECC71) else Color(0xFFE74C3C)
                Box(
                    modifier = Modifier
                        .background(repColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "${if (rating.reputation > 0) "+" else ""}${rating.reputation}",
                        color = repColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Текст рецензии
        rating.review?.takeIf { it.isNotBlank() }?.let { review ->
            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.06f)))
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                review,
                color = LocalAppColors.current.onBackground.copy(alpha = 0.85f),
                fontSize = 13.sp,
                lineHeight = 20.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = if (expanded) TextOverflow.Visible else TextOverflow.Ellipsis
            )
        }

        // Раскрытые критерии
        AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
            Column {
                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.06f)))
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
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(label, color = LocalAppColors.current.secondary, fontSize = 11.sp, modifier = Modifier.width(130.dp))
                            Box(modifier = Modifier.weight(1f).height(4.dp).background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(2.dp))) {
                                Box(modifier = Modifier.fillMaxWidth(score / 10f).height(4.dp)
                                    .background(Brush.horizontalGradient(listOf(GradientStart, GradientEnd)), RoundedCornerShape(2.dp)))
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
        Text(title, color = LocalAppColors.current.onBackground.copy(alpha = 0.7f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(subtitle, color = LocalAppColors.current.secondary, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}
