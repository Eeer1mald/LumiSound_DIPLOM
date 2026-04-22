package com.example.lumisound.feature.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.example.lumisound.ui.theme.GradientEnd
import com.example.lumisound.ui.theme.GradientStart
import com.example.lumisound.ui.theme.LocalAppColors
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
    var isSearchFocused by rememberSaveable { mutableStateOf(false) }
    val searchResults by viewModel.searchResults.collectAsState()
    val artistResults by viewModel.artistResults.collectAsState()
    val playlistResults by viewModel.playlistResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Выбранный плейлист для открытия
    var selectedPlaylist by remember { mutableStateOf<com.example.lumisound.data.remote.SupabaseService.PlaylistResponse?>(null) }
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

    Box(modifier = Modifier.fillMaxSize().background(LocalAppColors.current.background)) {
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
                    .background(LocalAppColors.current.background)
                    .statusBarsPadding()
                    .padding(top = 72.dp)
            ) {
                if (searchQuery.isNotEmpty()) {
                    SearchResultsList(
                        results = searchResults,
                        artistResults = artistResults,
                        playlistResults = playlistResults,
                        isLoading = isLoading,
                        error = error,
                        playerViewModel = playerViewModel,
                        navController = navController,
                        onPlaylistClick = { selectedPlaylist = it }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Text("Начните вводить запрос", color = LocalAppColors.current.secondary, fontSize = 14.sp)
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
                        Modifier.background(LocalAppColors.current.background)
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

        // Overlay для открытия плейлиста
        selectedPlaylist?.let { pl ->
            androidx.compose.animation.AnimatedVisibility(
                visible = true,
                enter = androidx.compose.animation.slideInVertically { it },
                modifier = Modifier.fillMaxSize()
            ) {
                com.example.lumisound.feature.home.PlaylistDetailScreen(
                    playlist = pl,
                    onClose = { selectedPlaylist = null },
                    onDeleted = { selectedPlaylist = null },
                    navController = navController
                )
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
    val tabs = listOf("Рекомендации", "Для вас")
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

                if (feed.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().background(LocalAppColors.current.background), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GradientStart, modifier = Modifier.size(36.dp))
                    }
                } else {
                    TikTokFeed(tracks = feed, viewModel = viewModel, navController = navController, isFeedMuted = isFeedMuted, onTapScreen = onTapScreen, hasMainTrack = hasMainTrack, isDiscoverTab = page == 0)
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
    hasMainTrack: Boolean,
    isDiscoverTab: Boolean = true
) {
    val pagerState = rememberPagerState(pageCount = { tracks.size })
    val isFeedPlaying by viewModel.isFeedPlaying.collectAsState()

    val context = LocalContext.current
    val imageLoader = remember(context) { coil.ImageLoader(context) }

    // При смене страницы — автоплей + предзагрузка (mute НЕ сбрасываем — сохраняем состояние)
    LaunchedEffect(pagerState.currentPage) {
        val track = tracks.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        viewModel.playFeedTrack(track)
        viewModel.preloadAround(tracks, pagerState.currentPage)

        // Предзагрузка картинок соседних треков через Coil
        val range = ((pagerState.currentPage - 2).coerceAtLeast(0))..(
            (pagerState.currentPage + 3).coerceAtMost(tracks.size - 1))
        for (i in range) {
            val t = tracks.getOrNull(i) ?: continue
            val url = t.hdImageUrl ?: t.imageUrl ?: continue
            val req = coil.request.ImageRequest.Builder(context).data(url).build()
            imageLoader.enqueue(req)
        }

        // Подгружаем следующую страницу когда осталось 5 треков
        if (tracks.size - pagerState.currentPage <= 5) {
            if (isDiscoverTab) viewModel.loadMoreDiscover()
            else viewModel.loadMoreFollowing()
        }
    }

    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 1
    ) { page ->
        val track = tracks[page]
        val isThisPagePlaying = pagerState.currentPage == page && isFeedPlaying
        // key по trackId — Hilt переиспользует ViewModel для одного трека
        androidx.compose.runtime.key(track.id) {
            TikTokTrackCard(
                track = track,
                isPlaying = isThisPagePlaying,
                isMuted = isFeedMuted,
                onTapScreen = onTapScreen,
                navController = navController,
                hasMainTrack = hasMainTrack,
                isCurrentPage = pagerState.currentPage == page
            )
        }
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
    isCurrentPage: Boolean = false,
    reviewViewModel: ReviewViewModel = hiltViewModel()
) {
    var showStatsSheet by remember { mutableStateOf(false) }
    var showCommentsSheet by remember { mutableStateOf(false) }
    var showReviewsSheet by remember { mutableStateOf(false) }
    var showAddToPlaylist by remember { mutableStateOf(false) }

    val reviewState by reviewViewModel.state.collectAsState()
    // Загружаем данные только когда карточка активна
    LaunchedEffect(track.id, isCurrentPage) {
        if (isCurrentPage) reviewViewModel.loadForTrack(track.id)
    }

    val avgScore = reviewState.averageRating?.avgOverall
    val commentCount = reviewState.comments.size
    val reviewCount = reviewState.reviews.filter { !it.review.isNullOrBlank() }.size

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Обложка
        if (!track.hdImageUrl.isNullOrEmpty() || !track.imageUrl.isNullOrEmpty()) {
            val imageUrl = track.hdImageUrl ?: track.imageUrl
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(false)
                    .memoryCacheKey(imageUrl)
                    .diskCacheKey(imageUrl)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(LocalAppColors.current.surface), contentAlignment = Alignment.Center) {
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
        // BottomNavigationBar (~56dp) + MiniPlayer (72dp если есть) + запас
        val miniPlayerHeight = if (hasMainTrack) 72.dp else 0.dp
        // Чуть ниже — уменьшаем запас с 24 до 8dp
        val bottomOffset = navBarHeight + 56.dp + miniPlayerHeight + 8.dp

        // Всплывающий комментарий слева от кнопки комментариев
        var floatingComment by remember { mutableStateOf<String?>(null) }
        var showFloating by remember { mutableStateOf(false) }
        val comments = reviewState.comments
        LaunchedEffect(comments) {
            if (comments.isNotEmpty()) {
                while (true) {
                    delay(5000)
                    floatingComment = comments.random().comment.take(40)
                    showFloating = true
                    delay(3000)
                    showFloating = false
                    delay(1000)
                }
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp)
                .padding(bottom = bottomOffset),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Статистика — квадрат, показывает среднюю оценку если есть
            FeedSquareButton(onClick = { showStatsSheet = true }) {
                if (avgScore != null) {
                    Text(
                        String.format("%.1f", avgScore),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                } else {
                    Icon(Icons.Default.BarChart, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            // Комментарии — вертикальный прямоугольник
            FeedVerticalRect(onClick = { showCommentsSheet = true }, label = "$commentCount") {
                Icon(Icons.Default.ChatBubbleOutline, null, tint = Color.White, modifier = Modifier.size(17.dp))
            }

            // Рецензии — вертикальный прямоугольник
            FeedVerticalRect(onClick = { showReviewsSheet = true }, label = "$reviewCount") {
                Icon(Icons.Default.Star, null, tint = Color.White, modifier = Modifier.size(17.dp))
            }

            // Добавить — квадрат
            FeedSquareButton(onClick = { showAddToPlaylist = true }) {
                Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }

        // ── Нижний текст — ровно над мини-плеером или над навбаром ────
        val playerViewModel: PlayerViewModel = hiltViewModel()
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 76.dp)
                .padding(bottom = bottomOffset + 4.dp)
                // Поглощаем все тапы — не пропускаем до фонового Box с mute
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) { awaitPointerEvent() }
                    }
                }
        ) {
            track.genre?.takeIf { it.isNotBlank() }?.let { genre ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                    Text("★", color = GradientStart, fontSize = 11.sp)
                    Text("Жанр: $genre", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                }
            }
            // Клик на название — запускает трек в плеере
            Text(
                track.name,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { playerViewModel.playTrack(track) }
            )
            Spacer(modifier = Modifier.height(5.dp))
            // Клик на артиста — открывает профиль
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    navController.navigate(
                        com.example.lumisound.navigation.MainDestination.Artist().createRoute(
                            track.artistId,
                            track.artist,
                            track.artistImageUrl
                        )
                    )
                }
            ) {
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
        // Всплывающий комментарий — вылетает справа, позиционируется над кнопкой комментариев
        // Снизу: bottomOffset + "+" (44dp) + gap(8dp) + рецензии(~60dp) + gap(8dp) + половина комментариев(~30dp)
        val floatingCommentBottom = bottomOffset + 44.dp + 8.dp + 60.dp + 8.dp + 30.dp
        AnimatedVisibility(
            visible = showFloating && floatingComment != null,
            enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = androidx.compose.animation.core.tween(400))
                    + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)),
            exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = androidx.compose.animation.core.tween(300))
                   + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(200)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 72.dp)
                .padding(bottom = floatingCommentBottom)
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 200.dp)
                    .background(Color.Black.copy(alpha = 0.78f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Text(
                    floatingComment ?: "",
                    color = Color.White,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (showStatsSheet || showCommentsSheet || showReviewsSheet) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        showStatsSheet = false; showCommentsSheet = false; showReviewsSheet = false
                    }
            )
        }

        AnimatedVisibility(visible = showStatsSheet, enter = slideInVertically { it }, exit = slideOutVertically { it }, modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            TrackStatsSheet(state = reviewState, bottomBarPadding = bottomOffset, onDismiss = { showStatsSheet = false })
        }
        AnimatedVisibility(visible = showCommentsSheet, enter = slideInVertically { it }, exit = slideOutVertically { it }, modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            CommentsPreviewSheet(state = reviewState, track = track, navController = navController, bottomBarPadding = bottomOffset, onDismiss = { showCommentsSheet = false })
        }
        AnimatedVisibility(visible = showReviewsSheet, enter = slideInVertically { it }, exit = slideOutVertically { it }, modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            ReviewsPreviewSheet(state = reviewState, track = track, navController = navController, bottomBarPadding = bottomOffset, onDismiss = { showReviewsSheet = false })
        }

        // Шторка добавления в плейлист
        if (showAddToPlaylist) {
            com.example.lumisound.feature.playlist.AddToPlaylistOverlay(
                track = track,
                bottomBarPadding = bottomOffset,
                onDismiss = { showAddToPlaylist = false }
            )
        }
    }
}

// Квадратная кнопка с скруглением (верхняя/нижняя)
@Composable
private fun FeedSquareButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(Color.Black.copy(alpha = 0.55f), shape)
            .border(1.dp, Color.White.copy(alpha = 0.18f), shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

// Вертикальный прямоугольник (комментарии / рецензии) — иконка сверху, число снизу
@Composable
private fun FeedVerticalRect(
    onClick: () -> Unit,
    label: String,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .width(44.dp)
            .background(Color.Black.copy(alpha = 0.55f), shape)
            .border(1.dp, Color.White.copy(alpha = 0.18f), shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        content()
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TrackStatsSheet(
    state: com.example.lumisound.feature.ratings.ReviewUiState,
    bottomBarPadding: androidx.compose.ui.unit.Dp = 0.dp,
    onDismiss: () -> Unit
) {
    var dragOffset by remember { mutableStateOf(0f) }
    Column(
        modifier = Modifier.fillMaxWidth()
            .graphicsLayer { translationY = dragOffset.coerceAtLeast(0f) }
            .background(Color(0xFF1C1C1C), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp)
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
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
    ) {
        Box(modifier = Modifier.width(36.dp).height(4.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp)).align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Средние показатели", color = LocalAppColors.current.onBackground, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                val ratingCount = state.averageRating?.ratingCount ?: 0
                if (ratingCount > 0) {
                    Text(
                        "$ratingCount ${when {
                            ratingCount % 100 in 11..19 -> "оценок"
                            ratingCount % 10 == 1 -> "оценка"
                            ratingCount % 10 in 2..4 -> "оценки"
                            else -> "оценок"
                        }}",
                        color = LocalAppColors.current.secondary,
                        fontSize = 12.sp
                    )
                }
            }
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
                Text(label, color = LocalAppColors.current.secondary, fontSize = 12.sp, modifier = Modifier.width(130.dp))
                Box(modifier = Modifier.weight(1f).height(4.dp).background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(2.dp))) {
                    if (score != null) Box(modifier = Modifier.fillMaxWidth((score / 10.0).toFloat().coerceIn(0f, 1f)).height(4.dp).background(GradientStart, RoundedCornerShape(2.dp)))
                }
                Text(score?.let { String.format("%.1f", it) } ?: "—", color = if (score != null) GradientStart else LocalAppColors.current.secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun CommentsPreviewSheet(
    state: com.example.lumisound.feature.ratings.ReviewUiState,
    track: Track,
    navController: NavHostController,
    bottomBarPadding: androidx.compose.ui.unit.Dp = 0.dp,
    onDismiss: () -> Unit
) {
    val playerStateHolder = getPlayerStateHolder()
    var dragOffset by remember { mutableStateOf(0f) }
    Column(
        modifier = Modifier.fillMaxWidth()
            .graphicsLayer { translationY = dragOffset.coerceAtLeast(0f) }
            .background(Color(0xFF1C1C1C), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp)
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
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
    ) {
        Box(modifier = Modifier.width(36.dp).height(4.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp)).align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Комментарии (${state.comments.size})", color = LocalAppColors.current.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Все →", color = GradientStart, fontSize = 13.sp, modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                onDismiss()
                playerStateHolder.setReviewTrack(track)
                navController.navigate(com.example.lumisound.navigation.MainDestination.Review().createRoute(track.id, track.name, track.artist))
            })
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (state.comments.isEmpty()) {
            Text("Нет комментариев", color = LocalAppColors.current.secondary, fontSize = 14.sp, modifier = Modifier.padding(vertical = 8.dp))
        } else {
            state.comments.take(3).forEach { comment ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(LocalAppColors.current.surface), contentAlignment = Alignment.Center) {
                        if (!comment.userAvatarUrl.isNullOrEmpty()) {
                            AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(comment.userAvatarUrl).crossfade(false).build(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Icon(Icons.Default.MusicNote, null, tint = LocalAppColors.current.secondary, modifier = Modifier.size(14.dp))
                        }
                    }
                    Column {
                        Text(comment.username ?: "Пользователь", color = LocalAppColors.current.secondary, fontSize = 11.sp)
                        Text(comment.comment, color = LocalAppColors.current.onBackground, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
    bottomBarPadding: androidx.compose.ui.unit.Dp = 0.dp,
    onDismiss: () -> Unit
) {
    val reviews = state.reviews.filter { !it.review.isNullOrBlank() }
    val playerStateHolder = getPlayerStateHolder()
    var dragOffset by remember { mutableStateOf(0f) }
    Column(
        modifier = Modifier.fillMaxWidth()
            .graphicsLayer { translationY = dragOffset.coerceAtLeast(0f) }
            .background(Color(0xFF1C1C1C), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp)
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
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
    ) {
        Box(modifier = Modifier.width(36.dp).height(4.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp)).align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Рецензии (${reviews.size})", color = LocalAppColors.current.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Все →", color = GradientStart, fontSize = 13.sp, modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                onDismiss()
                playerStateHolder.setReviewTrack(track)
                navController.navigate(com.example.lumisound.navigation.MainDestination.Reviews().createRoute(track.id, track.name, track.artist))
            })
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (reviews.isEmpty()) {
            Text("Нет рецензий", color = LocalAppColors.current.secondary, fontSize = 14.sp, modifier = Modifier.padding(vertical = 8.dp))
        } else {
            reviews.take(2).forEach { rating ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Аватар
                    Box(
                        modifier = Modifier.size(30.dp).clip(CircleShape).background(LocalAppColors.current.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!rating.userAvatarUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current).data(rating.userAvatarUrl).crossfade(false).build(),
                                contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Person, null, tint = LocalAppColors.current.secondary, modifier = Modifier.size(15.dp))
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                rating.username?.takeIf { it.isNotBlank() } ?: "Пользователь",
                                color = LocalAppColors.current.secondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                            )
                            rating.overallScore?.let { score ->
                                Box(
                                    modifier = Modifier
                                        .background(GradientStart.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(String.format("%.1f", score), color = GradientStart, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text(rating.review ?: "", color = LocalAppColors.current.onBackground, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultsList(
    results: List<Track>,
    artistResults: List<com.example.lumisound.data.remote.AudiusArtistFull>,
    playlistResults: List<com.example.lumisound.data.remote.SupabaseService.PlaylistResponse> = emptyList(),
    isLoading: Boolean,
    error: String?,
    playerViewModel: PlayerViewModel,
    navController: NavHostController,
    onPlaylistClick: (com.example.lumisound.data.remote.SupabaseService.PlaylistResponse) -> Unit = {}
) {
    when {
        isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = GradientStart) }
        error != null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(error, color = LocalAppColors.current.secondary, fontSize = 14.sp) }
        results.isEmpty() && artistResults.isEmpty() && playlistResults.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Ничего не найдено", color = LocalAppColors.current.secondary, fontSize = 14.sp) }
        else -> LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Артист
            val topArtist = artistResults.firstOrNull()
            if (topArtist != null) {
                item(key = "artist_${topArtist.id}") {
                    ArtistSearchCard(artist = topArtist, onArtistClick = {
                        navController.navigate(
                            com.example.lumisound.navigation.MainDestination.Artist().createRoute(
                                topArtist.id,
                                topArtist.name,
                                com.example.lumisound.data.remote.AudiusApiServiceHelper.getProfilePictureUrl(topArtist.profilePicture)
                            )
                        )
                    })
                }
            }
            // Плейлисты — горизонтальный ряд квадратных карточек
            if (playlistResults.isNotEmpty()) {
                item(key = "playlists_row") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(playlistResults, key = { it.id }) { playlist ->
                            PlaylistSearchCard(
                                playlist = playlist,
                                onClick = { onPlaylistClick(playlist) }
                            )
                        }
                    }
                }
            }
            // Треки
            itemsIndexed(results, key = { _, t -> t.id }) { index, track ->
                SearchResultItem(track = track, onClick = {
                    playerViewModel.playPlaylist(results, index)
                })
            }
        }
    }
}

@Composable
private fun ArtistSearchCard(
    artist: com.example.lumisound.data.remote.AudiusArtistFull,
    onArtistClick: () -> Unit
) {
    val avatarUrl = com.example.lumisound.data.remote.AudiusApiServiceHelper.getProfilePictureUrl(artist.profilePicture)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LocalAppColors.current.surface, RoundedCornerShape(14.dp))
            .border(1.dp, GradientStart.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onArtistClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Аватар — круглый, чуть крупнее трека
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
        ) {
            if (!avatarUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(avatarUrl).crossfade(false).build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, tint = LocalAppColors.current.secondary, modifier = Modifier.size(26.dp))
                }
            }
        }

        // Инфо
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    artist.name,
                    color = LocalAppColors.current.onBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (artist.isVerified == true) {
                    Icon(Icons.Default.CheckCircle, null, tint = GradientStart, modifier = Modifier.size(13.dp))
                }
            }
            val sub = buildString {
                if (!artist.location.isNullOrBlank()) append(artist.location)
                if (!artist.location.isNullOrBlank() && (artist.followerCount ?: 0) > 0) append(" · ")
                if ((artist.followerCount ?: 0) > 0) append(formatFollowers(artist.followerCount!!))
            }
            if (sub.isNotBlank()) Text(sub, color = LocalAppColors.current.secondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        // Иконка артиста справа
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(GradientStart.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, null, tint = GradientStart, modifier = Modifier.size(16.dp))
        }
    }
}

private fun formatFollowers(count: Int): String = when {
    count >= 1_000_000 -> "${count / 1_000_000}M подписчиков"
    count >= 1_000 -> "${String.format("%.1f", count / 1000.0).trimEnd('0').trimEnd('.')}K подписчиков"
    else -> "$count подписчиков"
}

@Composable
private fun SearchResultItem(track: Track, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(LocalAppColors.current.surface, RoundedCornerShape(14.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Обложка — чуть крупнее чтобы влезла третья строка
        Box(modifier = Modifier.size(62.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.06f))) {
            if (!track.imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(track.imageUrl).crossfade(false).build(),
                    contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.MusicNote, null, tint = LocalAppColors.current.secondary.copy(alpha = 0.5f), modifier = Modifier.size(28.dp).align(Alignment.Center))
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(track.name, color = LocalAppColors.current.onBackground, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = LocalAppColors.current.secondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            // Третья строка: прослушивания и длина — только если данные есть
            val hasStats = (track.playCount ?: 0) > 0 || (track.duration ?: 0) > 0
            if (hasStats) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if ((track.playCount ?: 0) > 0) {
                        Icon(
                            Icons.Default.PlayArrow, null,
                            tint = LocalAppColors.current.secondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            formatPlayCount(track.playCount!!),
                            color = LocalAppColors.current.secondary,
                            fontSize = 11.sp
                        )
                    }
                    if ((track.playCount ?: 0) > 0 && (track.duration ?: 0) > 0) {
                        Text("·", color = LocalAppColors.current.secondary, fontSize = 11.sp)
                    }
                    if ((track.duration ?: 0) > 0) {
                        Text(
                            formatDuration(track.duration!!),
                            color = LocalAppColors.current.secondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

private fun formatPlayCount(count: Int): String = when {
    count >= 1_000_000 -> "${String.format("%.1f", count / 1_000_000.0).trimEnd('0').trimEnd('.')}M"
    count >= 1_000 -> "${String.format("%.1f", count / 1_000.0).trimEnd('0').trimEnd('.')}K"
    else -> count.toString()
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "$m:${s.toString().padStart(2, '0')}"
}

@Composable
private fun PlaylistSearchCard(
    playlist: com.example.lumisound.data.remote.SupabaseService.PlaylistResponse,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(LocalAppColors.current.surface),
            contentAlignment = Alignment.Center
        ) {
            if (!playlist.coverUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(playlist.coverUrl)
                        .crossfade(false)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.MusicNote, null,
                    tint = LocalAppColors.current.secondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Text(
            text = playlist.name,
            color = LocalAppColors.current.onBackground,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!playlist.username.isNullOrEmpty()) {
            Text(
                text = playlist.username,
                color = LocalAppColors.current.secondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
