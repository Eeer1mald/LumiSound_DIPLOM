package com.example.lumisound.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.lumisound.feature.home.components.BottomNavigationBar
import com.example.lumisound.feature.home.components.MiniPlayer
import com.example.lumisound.feature.nowplaying.PlayerViewModel
import com.example.lumisound.feature.search.PlayerStateHolderEntryPoint
import com.example.lumisound.ui.theme.ColorBackground
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun SwipeableNavHost(
    navController: NavHostController,
    currentRoute: String,
    userName: String = "Александр",
    onNavigate: (String) -> Unit,
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val playerStateHolder = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            PlayerStateHolderEntryPoint::class.java
        ).playerStateHolder()
    }

    val routes = remember { listOf("home", "search", "ratings", "profile") }
    val currentIndex = remember(currentRoute) {
        routes.indexOf(currentRoute).coerceIn(0, routes.size - 1)
    }

    val pagerState = rememberPagerState(pageCount = { routes.size }, initialPage = currentIndex)
    val scope = rememberCoroutineScope()

    var localCurrentRoute by rememberSaveable { mutableStateOf(currentRoute) }

    LaunchedEffect(Unit) { localCurrentRoute = currentRoute }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                val newRoute = routes[page]
                if (newRoute != localCurrentRoute) localCurrentRoute = newRoute
            }
    }

    val currentTrack by playerStateHolder.currentTrack.collectAsState()

    var rawAnimationProgress by remember { mutableFloatStateOf(0f) }
    var isDraggingMiniPlayer by remember { mutableStateOf(false) }
    var lastDragVelocity by remember { mutableFloatStateOf(0f) }
    var targetAnimationProgress by remember { mutableFloatStateOf(0f) }

    val animatedProgress by animateFloatAsState(
        targetValue = targetAnimationProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "miniPlayerAnimation"
    )
    val animationProgress = if (isDraggingMiniPlayer) rawAnimationProgress else animatedProgress

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().clipToBounds(),
            userScrollEnabled = true,
            pageSize = PageSize.Fill,
            key = { page -> routes[page] }
        ) { page ->
            val route = remember(page) { routes[page] }
            Box(modifier = Modifier.fillMaxSize().background(ColorBackground)) {
                ScreenContent(route = route, navController = navController, userName = userName)
            }
        }

        BottomPlayerBar(
            navController = navController,
            playerViewModel = playerViewModel,
            playerStateHolder = playerStateHolder,
            currentTrack = currentTrack,
            localCurrentRoute = localCurrentRoute,
            routes = routes,
            pagerState = pagerState,
            scope = scope,
            animationProgress = animationProgress,
            rawAnimationProgress = rawAnimationProgress,
            isDraggingMiniPlayer = isDraggingMiniPlayer,
            onRawProgressChange = { rawAnimationProgress = it },
            onDraggingChange = { isDraggingMiniPlayer = it },
            onVelocityChange = { lastDragVelocity = it },
            onTargetProgressChange = { targetAnimationProgress = it },
            getLastVelocity = { lastDragVelocity },
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
        )
    }
}

@Composable
private fun BottomPlayerBar(
    navController: NavHostController,
    playerViewModel: PlayerViewModel,
    playerStateHolder: com.example.lumisound.data.player.PlayerStateHolder,
    currentTrack: com.example.lumisound.data.model.Track?,
    localCurrentRoute: String,
    routes: List<String>,
    pagerState: androidx.compose.foundation.pager.PagerState,
    scope: kotlinx.coroutines.CoroutineScope,
    animationProgress: Float,
    rawAnimationProgress: Float,
    isDraggingMiniPlayer: Boolean,
    onRawProgressChange: (Float) -> Unit,
    onDraggingChange: (Boolean) -> Unit,
    onVelocityChange: (Float) -> Unit,
    onTargetProgressChange: (Float) -> Unit,
    getLastVelocity: () -> Float,
    modifier: Modifier = Modifier
) {
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    var isLiked by remember { mutableStateOf(false) }

    val miniProgress by remember {
        derivedStateOf {
            val pos = playerViewModel.currentPosition.value
            val dur = playerViewModel.duration.value
            if (dur > 0) (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f
        }
    }

    Column(modifier = modifier) {
        if (currentTrack != null) {
            MiniPlayer(
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                progress = miniProgress,
                onPlayPauseClick = { playerViewModel.togglePlayPause() },
                onTrackClick = {
                    if (animationProgress < 0.05f && !isDraggingMiniPlayer) {
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("playerSourceRoute", localCurrentRoute)
                        navController.navigate("now_playing/${currentTrack.id}") {
                            launchSingleTop = true
                        }
                    }
                },
                onAddClick = { },
                onLikeClick = { isLiked = !isLiked },
                onArtistClick = { artistId, artistName, artistImageUrl ->
                    navController.navigate(
                        MainDestination.Artist().createRoute(artistId, artistName, artistImageUrl)
                    )
                },
                isLiked = isLiked,
                animationProgress = animationProgress,
                onAnimationProgressChange = { onRawProgressChange(it.coerceIn(0f, 1f)) },
                onDragStart = {
                    onDraggingChange(true)
                    onVelocityChange(0f)
                },
                onDragVelocityChange = { onVelocityChange(it) },
                onDragEnd = {
                    val currentProgress = rawAnimationProgress
                    val velocity = getLastVelocity()
                    onDraggingChange(false)

                    scope.launch {
                        var finalProgress = currentProgress

                        if (velocity > 15f && currentProgress > 0.02f) {
                            val screenHeight = 2400f
                            val normalizedVelocity = velocity / screenHeight
                            var progress = currentProgress
                            var currentVelocity = normalizedVelocity * 16f * 1.5f
                            val friction = 0.97f * 0.92f
                            val minVelocity = 0.0003f

                            while (progress < 1f && abs(currentVelocity) > minVelocity) {
                                currentVelocity *= friction
                                progress = (progress + currentVelocity).coerceIn(0f, 1f)
                                onRawProgressChange(progress)
                                delay(16L)
                                if (progress >= 1f) break
                            }
                            finalProgress = progress.coerceIn(0f, 1f)
                            onRawProgressChange(finalProgress)
                        }

                        if (finalProgress >= 0.5f) {
                            onTargetProgressChange(1f)
                            delay(350)
                            navController.navigate("now_playing/${currentTrack.id}") {
                                launchSingleTop = true
                            }
                            navController.currentBackStackEntry
                                ?.savedStateHandle
                                ?.set("playerSourceRoute", localCurrentRoute)
                            delay(100)
                            onTargetProgressChange(0f)
                            onRawProgressChange(0f)
                        } else {
                            onTargetProgressChange(0f)
                            delay(350)
                            onRawProgressChange(0f)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        BottomNavigationBar(
            currentRoute = localCurrentRoute,
            onNavigate = { route ->
                val targetIndex = routes.indexOf(route).coerceIn(0, routes.size - 1)
                if (targetIndex != pagerState.currentPage) {
                    scope.launch { pagerState.animateScrollToPage(targetIndex) }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

