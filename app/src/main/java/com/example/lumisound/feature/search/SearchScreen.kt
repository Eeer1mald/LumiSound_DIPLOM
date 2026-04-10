package com.example.lumisound.feature.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.lumisound.feature.home.TrackPreview
import com.example.lumisound.feature.home.components.SearchField
import com.example.lumisound.feature.nowplaying.PlayerViewModel
import com.example.lumisound.feature.ratings.ReviewViewModel
import com.example.lumisound.ui.theme.ColorBackground
import com.example.lumisound.ui.theme.ColorOnBackground
import com.example.lumisound.ui.theme.ColorSecondary
import com.example.lumisound.ui.theme.ColorSurface
import com.example.lumisound.ui.theme.GradientEnd
import com.example.lumisound.ui.theme.GradientStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class GenreItem(val name: String, val gradient: Pair<Color, Color>)

@Composable
fun SearchScreen(
    navController: NavHostController,
    trendingTracks: List<TrackPreview> = emptyList(),
    onTrackClick: (String) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    // Отдельный флаг фокуса — не зависит от текста, не вызывает смену экрана при вводе
    var isSearchFocused by remember { mutableStateOf(false) }
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val discoverFeed by viewModel.discoverFeed.collectAsState()
    val followingFeed by viewModel.followingFeed.collectAsState()
    val feedLoading by viewModel.feedLoading.collectAsState()
    val isFeedMuted by viewModel.isFeedMuted.collectAsState()
    val playerStateHolder = getPlayerStateHolder()
    val mainPlayerIsPlaying by playerViewModel.isPlaying.collectAsState()
    val mainPlayerTrack by playerViewModel.currentTrack.collectAsState()
    val hasMainTrack = mainPlayerTrack != null

    // Когда основной плеер включается — глушим фид
    LaunchedEffect(mainPlayerIsPlaying) {
        if (mainPlayerIsPlaying) viewModel.muteFeed()
    }

    // Останавливаем фид при уходе со страницы
    DisposableEffect(Unit) {
        onDispose { viewModel.stopFeed() }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            delay(400)
            viewModel.searchTracks(searchQuery)
        } else {
            viewModel.searchTracks("")
        }
    }

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    Box(modifier = Modifier.fillMaxSize().background(ColorBackground)) {
        // Фид всегда рендерится — не пересоздаётся при фокусе поиска
        FeedPager(
            discoverFeed = discoverFeed,
            followingFeed = followingFeed,
            isLoading = feedLoading,
            viewModel = viewModel,
            navController = navController,
            isFeedMuted = isFeedMuted,
            mainPlayerIsPlaying = mainPlayerIsPlaying,
            hasMainTrack = hasMainTrack,
            onTapScreen = {
                if (isFeedMuted && mainPlayerIsPlaying) playerViewModel.togglePlayPause()
                viewModel.toggleMute()
            },
            onRefresh = { viewModel.loadFeeds() }
        )

        // Затемнение + результаты поиска — поверх фида при фокусе
        if (isSearchFocused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorBackground)
                    .statusBarsPadding()
                    .padding(top = 72.dp)
            ) {
                if (searchQuery.isNotEmpty()) {
                    SearchResultsList(
                        results = searchResults,
                        isLoading = isLoading,
                        error = error,
                        playerStateHolder = playerStateHolder,
                        playerViewModel = playerViewModel
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Text("Начните вводить запрос", color = ColorSecondary, fontSize = 14.sp)
                    }
                }
            }
        }

        // Строка поиска — всегда поверх всего
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .then(
                    if (isSearchFocused)
                        Modifier.background(ColorBackground)
                    else
                        Modifier.background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)))
                )
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SearchField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Поиск музыки, исполнителей...",
                    modifier = Modifier.fillMaxWidth(),
                    onFocusChanged = { if (it) isSearchFocused = true }
                )
            }
            if (isSearchFocused || searchQuery.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            searchQuery = ""
                            isSearchFocused = false
                            focusManager.clearFocus()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun FeedPager(
    discoverFeed: List<Track>,
    followingFeed: List<Track>,
    isLoading: Boolean,
    viewModel: SearchViewModel,
    navController: NavHostController,
    isFeedMuted: Boolean,
    mainPlayerIsPlaying: Boolean,
    hasMainTrack: Boolean,
    onTapScreen: () -> Unit,
    onRefresh: () -> Unit
) {
    val tabs = listOf("Для вас", "Любимое")
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GradientStart, modifier = Modifier.size(36.dp))
            }
        } else {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val feed = if (page == 0) discoverFeed else followingFeed
                val emptyText = if (page == 0)
                    "Добавьте треки и артистов в избранное\nдля персональных рекомендаций"
                else "Добавьте артистов в избранное\nчтобы видеть их треки здесь"

                if (feed.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().background(ColorBackground), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("🎵", fontSize = 40.sp)
                            Text(emptyText, color = ColorSecondary, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                            Box(
                                modifier = Modifier.background(ColorSurface, RoundedCornerShape(12.dp))
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onRefresh() }
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Refresh, null, tint = GradientStart, modifier = Modifier.size(16.dp))
                                    Text("Обновить", color = GradientStart, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                } else {
                    TikTokFeed(tracks = feed, viewModel = viewModel, navController = navController, isFeedMuted = isFeedMuted, onTapScreen = onTapScreen, hasMainTrack = hasMainTrack)
                }
            }
        }

        // Только табы поверх картинки (строка поиска теперь в SearchScreen)
        Column(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)) {
            // Отступ под строкой поиска (~72dp с statusBar)
            Spacer(modifier = Modifier.statusBarsPadding().height(64.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp), horizontalArrangement = Arrangement.Center) {
                tabs.forEachIndexed { index, title ->
                    val active = pagerState.currentPage == index
                    Column(
                        modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(title, color = if (active) Color.White else Color.White.copy(alpha = 0.5f), fontSize = 16.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                        if (active) Box(modifier = Modifier.padding(top = 3.dp).width(32.dp).height(2.dp).background(Color.White, RoundedCornerShape(1.dp)))
                        else Spacer(modifier = Modifier.height(5.dp))
                    }
                    if (index == 0) Spacer(modifier = Modifier.width(32.dp))
                }
            }
        }
    }
}

@Composable
private fun TikTokFeed(
    tracks: List<Track>,
    viewModel: SearchViewModel,
    navController: NavHostController,
    isFeedMuted: Boolean,
    onTapScreen: () -> Unit,
    hasMainTrack: Boolean
) {
    val pagerState = rememberPagerState(pageCount = { tracks.size })
    val isFeedPlaying by viewModel.isFeedPlaying.collectAsState()

    val context = LocalContext.current

    // При смене страницы — автоплей + предзагрузка (mute НЕ сбрасываем — сохраняем состояние)
    LaunchedEffect(pagerState.currentPage) {
        val track = tracks.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        viewModel.playFeedTrack(track)
        viewModel.preloadAround(tracks, pagerState.currentPage)

        // Предзагрузка картинок соседних треков через Coil
        val imageLoader = coil.ImageLoader(context)
        val range = ((pagerState.currentPage - 2).coerceAtLeast(0))..(
            (pagerState.currentPage + 3).coerceAtMost(tracks.size - 1))
        for (i in range) {
            val t = tracks.getOrNull(i) ?: continue
            val url = t.hdImageUrl ?: t.imageUrl ?: continue
            val req = coil.request.ImageRequest.Builder(context).data(url).build()
            imageLoader.enqueue(req)
        }
    }

    VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
        val track = tracks[page]
        val isThisPagePlaying = pagerState.currentPage == page && isFeedPlaying
        TikTokTrackCard(
            track = track,
            isPlaying = isThisPagePlaying,
            isMuted = isFeedMuted,
            onTapScreen = onTapScreen,
            navController = navController,
            hasMainTrack = hasMainTrack
        )
    }
}

@Composable
private fun TikTokTrackCard(
    track: Track,
    isPlaying: Boolean,
    isMuted: Boolean,
    onTapScreen: () -> Unit,
    navController: NavHostController,
    hasMainTrack: Boolean = true,
    reviewViewModel: ReviewViewModel = hiltViewModel()
) {
    var showStatsSheet by remember { mutableStateOf(false) }
    var showCommentsSheet by remember { mutableStateOf(false) }
    var showReviewsSheet by remember { mutableStateOf(false) }

    val reviewState by reviewViewModel.state.collectAsState()
    LaunchedEffect(track.id) { reviewViewModel.loadForTrack(track.id) }

    val avgScore = reviewState.averageRating?.avgOverall
    val commentCount = reviewState.comments.size
    val reviewCount = reviewState.reviews.filter { !it.review.isNullOrBlank() }.size

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Обложка
        if (!track.hdImageUrl.isNullOrEmpty() || !track.imageUrl.isNullOrEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(track.hdImageUrl ?: track.imageUrl).crossfade(false).build(),
                contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(ColorSurface), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.MusicNote, null, tint = GradientStart.copy(alpha = 0.3f), modifier = Modifier.size(80.dp))
            }
        }

        // Градиент снизу
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.8f)))))

        // Тап по экрану — ВСЕГДА активен (toggle mute/unmute)
        Box(modifier = Modifier.fillMaxSize().pointerInput(isMuted) {
            detectTapGestures(onTap = { onTapScreen() })
        })

        // Иконка mute — показывается только когда muted
        if (isMuted) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 60.dp)
                    .size(52.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.VolumeOff, null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
        }

        // ── Правая панель — строго в столбик ──────────────────────────
        val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        // Если есть мини-плеер: +72dp, всегда +56dp навбар
        val bottomOffset = navBarHeight + 56.dp + (if (hasMainTrack) 72.dp else 0.dp) + 8.dp

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp)
                .padding(bottom = bottomOffset),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Оценка — круглая кнопка
            FeedActionButton(onClick = { showStatsSheet = true }) {
                if (avgScore != null) {
                    Text(String.format("%.1f", avgScore), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                } else {
                    Icon(Icons.Default.BarChart, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            // Комментарии — продолговатая кнопка с числом
            FeedPillButton(
                icon = { Icon(Icons.Default.ChatBubbleOutline, null, tint = Color.White, modifier = Modifier.size(18.dp)) },
                count = commentCount,
                onClick = { showCommentsSheet = true }
            )

            // Рецензии — продолговатая кнопка с числом
            FeedPillButton(
                icon = { Icon(Icons.Default.Star, null, tint = Color.White, modifier = Modifier.size(18.dp)) },
                count = reviewCount,
                onClick = { showReviewsSheet = true }
            )

            // Добавить в плейлист
            FeedActionButton(onClick = { /* TODO */ }) {
                Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }

        // ── Нижний текст — ровно над мини-плеером или над навбаром ────
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 76.dp)
                .padding(bottom = bottomOffset + 4.dp)
        ) {
            track.genre?.takeIf { it.isNotBlank() }?.let { genre ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                    Text("★", color = GradientStart, fontSize = 11.sp)
                    Text("Жанр: $genre", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                }
            }
            Text(track.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                if (!track.artistImageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(track.artistImageUrl).crossfade(false).build(),
                        contentDescription = null, modifier = Modifier.size(24.dp).clip(CircleShape), contentScale = ContentScale.Crop
                    )
                }
                Text(track.artist, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }

        // Затемнение под sheet
        if (showStatsSheet || showCommentsSheet || showReviewsSheet) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        showStatsSheet = false; showCommentsSheet = false; showReviewsSheet = false
                    }
            )
        }

        AnimatedVisibility(visible = showStatsSheet, enter = slideInVertically { it }, exit = slideOutVertically { it }, modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            TrackStatsSheet(state = reviewState, commentCount = commentCount, reviewCount = reviewCount, onDismiss = { showStatsSheet = false })
        }
        AnimatedVisibility(visible = showCommentsSheet, enter = slideInVertically { it }, exit = slideOutVertically { it }, modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            CommentsPreviewSheet(state = reviewState, track = track, navController = navController, onDismiss = { showCommentsSheet = false })
        }
        AnimatedVisibility(visible = showReviewsSheet, enter = slideInVertically { it }, exit = slideOutVertically { it }, modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            ReviewsPreviewSheet(state = reviewState, track = track, navController = navController, onDismiss = { showReviewsSheet = false })
        }
    }
}

// Компактная круглая кнопка правой панели
@Composable
private fun FeedActionButton(
    onClick: () -> Unit,
    label: String? = null,
    content: @Composable () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() },
            contentAlignment = Alignment.Center
        ) { content() }
        if (label != null) {
            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// Продолговатая кнопка с иконкой и числом
@Composable
private fun FeedPillButton(
    icon: @Composable () -> Unit,
    count: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(36.dp)
            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        icon()
        Text("$count", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TrackStatsSheet(
    state: com.example.lumisound.feature.ratings.ReviewUiState,
    commentCount: Int,
    reviewCount: Int,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(Color(0xFF1C1C1C), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .navigationBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
    ) {
        Box(modifier = Modifier.width(36.dp).height(4.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp)).align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Средние показатели", color = ColorOnBackground, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            state.averageRating?.avgOverall?.let { score ->
                Box(modifier = Modifier.background(brush = Brush.linearGradient(listOf(GradientStart, GradientEnd)), shape = RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(String.format("%.1f", score) + " / 10", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        val criteria = listOf(
            "Рифмы / Образы" to state.averageRating?.avgRhyme,
            "Структура / Ритмика" to state.averageRating?.avgImagery,
            "Реализация стиля" to state.averageRating?.avgStructure,
            "Индивидуальность" to state.averageRating?.avgCharisma,
            "Атмосфера / Вайб" to state.averageRating?.avgAtmosphere
        )
        criteria.forEach { (label, score) ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(label, color = ColorSecondary, fontSize = 12.sp, modifier = Modifier.width(130.dp))
                Box(modifier = Modifier.weight(1f).height(4.dp).background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(2.dp))) {
                    if (score != null) Box(modifier = Modifier.fillMaxWidth((score / 10.0).toFloat().coerceIn(0f, 1f)).height(4.dp).background(GradientStart, RoundedCornerShape(2.dp)))
                }
                Text(score?.let { String.format("%.1f", it) } ?: "—", color = if (score != null) GradientStart else ColorSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f).background(ColorSurface, RoundedCornerShape(10.dp)).padding(12.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$commentCount", color = ColorOnBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("комментариев", color = ColorSecondary, fontSize = 11.sp)
                }
            }
            Box(modifier = Modifier.weight(1f).background(ColorSurface, RoundedCornerShape(10.dp)).padding(12.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$reviewCount", color = ColorOnBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("рецензий", color = ColorSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun CommentsPreviewSheet(
    state: com.example.lumisound.feature.ratings.ReviewUiState,
    track: Track,
    navController: NavHostController,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(Color(0xFF1C1C1C), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .navigationBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
    ) {
        Box(modifier = Modifier.width(36.dp).height(4.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp)).align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Комментарии (${state.comments.size})", color = ColorOnBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Все →", color = GradientStart, fontSize = 13.sp, modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                onDismiss()
                navController.navigate(com.example.lumisound.navigation.MainDestination.Review().createRoute(track.id))
            })
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (state.comments.isEmpty()) {
            Text("Нет комментариев", color = ColorSecondary, fontSize = 14.sp, modifier = Modifier.padding(vertical = 8.dp))
        } else {
            state.comments.take(3).forEach { comment ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(ColorSurface), contentAlignment = Alignment.Center) {
                        if (!comment.userAvatarUrl.isNullOrEmpty()) {
                            AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(comment.userAvatarUrl).crossfade(false).build(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Icon(Icons.Default.MusicNote, null, tint = ColorSecondary, modifier = Modifier.size(14.dp))
                        }
                    }
                    Column {
                        Text(comment.username ?: "Пользователь", color = ColorSecondary, fontSize = 11.sp)
                        Text(comment.comment, color = ColorOnBackground, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewsPreviewSheet(
    state: com.example.lumisound.feature.ratings.ReviewUiState,
    track: Track,
    navController: NavHostController,
    onDismiss: () -> Unit
) {
    val reviews = state.reviews.filter { !it.review.isNullOrBlank() }
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(Color(0xFF1C1C1C), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .navigationBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
    ) {
        Box(modifier = Modifier.width(36.dp).height(4.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp)).align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Рецензии (${reviews.size})", color = ColorOnBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Все →", color = GradientStart, fontSize = 13.sp, modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                onDismiss()
                navController.navigate(com.example.lumisound.navigation.MainDestination.Reviews().createRoute(track.id))
            })
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (reviews.isEmpty()) {
            Text("Нет рецензий", color = ColorSecondary, fontSize = 14.sp, modifier = Modifier.padding(vertical = 8.dp))
        } else {
            reviews.take(2).forEach { rating ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(rating.username ?: "Пользователь", color = ColorSecondary, fontSize = 11.sp)
                        rating.overallScore?.let { score ->
                            Box(modifier = Modifier.background(brush = Brush.linearGradient(listOf(GradientStart, GradientEnd)), shape = RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text(String.format("%.1f", score), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text(rating.review ?: "", color = ColorOnBackground, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

@Composable
private fun SearchResultsList(
    results: List<Track>,
    isLoading: Boolean,
    error: String?,
    playerStateHolder: com.example.lumisound.data.player.PlayerStateHolder,
    playerViewModel: PlayerViewModel
) {
    when {
        isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = GradientStart) }
        error != null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(error, color = ColorSecondary, fontSize = 14.sp) }
        results.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Ничего не найдено", color = ColorSecondary, fontSize = 14.sp) }
        else -> LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(results, key = { _, t -> t.id }) { index, track ->
                SearchResultItem(track = track, onClick = {
                    playerStateHolder.setPlaylist(results, index)
                    playerViewModel.playTrack(track)
                })
            }
        }
    }
}

@Composable
private fun SearchResultItem(track: Track, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(ColorSurface.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF1F1F1F).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(ColorSurface)) {
            if (!track.imageUrl.isNullOrEmpty()) {
                AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(track.imageUrl).crossfade(false).build(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Default.MusicNote, null, tint = ColorSecondary.copy(alpha = 0.5f), modifier = Modifier.size(28.dp).align(Alignment.Center))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(track.name, color = ColorOnBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = ColorSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            track.genre?.takeIf { it.isNotBlank() }?.let { Text(it, color = ColorSecondary.copy(alpha = 0.7f), fontSize = 11.sp, maxLines = 1) }
        }
    }
}
