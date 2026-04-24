package com.example.lumisound.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.lumisound.AppPreloadViewModel
import com.example.lumisound.feature.home.components.BottomNavigationBar
import com.example.lumisound.feature.home.components.MiniPlayer
import com.example.lumisound.feature.nowplaying.PlayerViewModel
import com.example.lumisound.feature.search.PlayerStateHolderEntryPoint
import com.example.lumisound.ui.theme.LocalAppColors
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
    synthesisInviteCode: String? = null,
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

    // Профиль — подписываемся через snapshotFlow чтобы поймать момент загрузки
    val profileViewModel: com.example.lumisound.feature.profile.ProfileViewModel = hiltViewModel()
    val realUsername = remember { mutableStateOf(userName) }
    val realAvatarUrl = remember { mutableStateOf<String?>(null) }
    LaunchedEffect(profileViewModel) {
        // Ждём пока профиль загрузится (username не пустой)
        snapshotFlow { profileViewModel.uiState.value.username }
            .distinctUntilChanged()
            .collect { username ->
                if (username.isNotBlank()) {
                    realUsername.value = username
                    realAvatarUrl.value = profileViewModel.avatarUri.value?.toString()
                        ?: profileViewModel.uiState.value.avatarUrl
                }
            }
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

    val appPreloadViewModel: AppPreloadViewModel = hiltViewModel()
    LaunchedEffect(Unit) {
        appPreloadViewModel.triggerPreload()
    }

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
        modifier = Modifier.fillMaxSize().background(LocalAppColors.current.background)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().clipToBounds(),
            userScrollEnabled = true,
            pageSize = PageSize.Fill,
            beyondViewportPageCount = 1,
            key = { page -> routes[page] }
        ) { page ->
            val route = remember(page) { routes[page] }
            Box(modifier = Modifier.fillMaxSize().background(LocalAppColors.current.background)) {
                ScreenContent(
                    route = route,
                    navController = navController,
                    userName = realUsername.value,
                    synthesisInviteCode = if (route == "home") synthesisInviteCode else null,
                    realAvatarUrl = if (route == "home") realAvatarUrl.value else null
                )
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
            getRawProgress = { rawAnimationProgress },
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
    getRawProgress: () -> Float,
    isDraggingMiniPlayer: Boolean,
    onRawProgressChange: (Float) -> Unit,
    onDraggingChange: (Boolean) -> Unit,
    onVelocityChange: (Float) -> Unit,
    onTargetProgressChange: (Float) -> Unit,
    getLastVelocity: () -> Float,
    modifier: Modifier = Modifier
) {
    var isLiked by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        if (currentTrack != null) {
            // MiniPlayer изолирован — его рекомпозиция не затрагивает BottomNavigationBar
            MiniPlayerWrapper(
                currentTrack = currentTrack,
                playerViewModel = playerViewModel,
                navController = navController,
                localCurrentRoute = localCurrentRoute,
                animationProgress = animationProgress,
                getRawProgress = getRawProgress,
                isDraggingMiniPlayer = isDraggingMiniPlayer,
                isLiked = isLiked,
                onLikedChange = { isLiked = it },
                onRawProgressChange = onRawProgressChange,
                onDraggingChange = onDraggingChange,
                onVelocityChange = onVelocityChange,
                onTargetProgressChange = onTargetProgressChange,
                getLastVelocity = getLastVelocity,
                scope = scope
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

// Изолированный wrapper для MiniPlayer — рекомпозиция прогресса не затрагивает BottomNavigationBar
@Composable
private fun MiniPlayerWrapper(
    currentTrack: com.example.lumisound.data.model.Track,
    playerViewModel: PlayerViewModel,
    navController: NavHostController,
    localCurrentRoute: String,
    animationProgress: Float,
    getRawProgress: () -> Float,
    isDraggingMiniPlayer: Boolean,
    isLiked: Boolean,
    onLikedChange: (Boolean) -> Unit,
    onRawProgressChange: (Float) -> Unit,
    onDraggingChange: (Boolean) -> Unit,
    onVelocityChange: (Float) -> Unit,
    onTargetProgressChange: (Float) -> Unit,
    getLastVelocity: () -> Float,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val currentPosition by playerViewModel.currentPosition.collectAsState()
    val duration by playerViewModel.duration.collectAsState()
    val avgScore by playerViewModel.avgScore.collectAsState()
    val miniProgress = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
    val isSleepTimerActive = playerViewModel.playerStateHolder.sleepTimerActive
    var showSleepTimerDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var sleepTimerRemainingText by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }

    // Обновляем оставшееся время каждую секунду когда диалог открыт
    androidx.compose.runtime.LaunchedEffect(showSleepTimerDialog) {
        if (showSleepTimerDialog) {
            while (showSleepTimerDialog) {
                val remainingMs = playerViewModel.audioPlayerService.getSleepTimerRemainingMs()
                if (remainingMs <= 0L) {
                    showSleepTimerDialog = false
                    break
                }
                val totalSec = remainingMs / 1000
                val min = totalSec / 60
                val sec = totalSec % 60
                sleepTimerRemainingText = "${min}:${sec.toString().padStart(2, '0')}"
                delay(1000L)
            }
        }
    }

    if (showSleepTimerDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { androidx.compose.material3.Text("Таймер сна", color = com.example.lumisound.ui.theme.LocalAppColors.current.onBackground) },
            text = {
                androidx.compose.foundation.layout.Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.Text(
                        "Музыка выключится через $sleepTimerRemainingText",
                        color = com.example.lumisound.ui.theme.LocalAppColors.current.secondary,
                        fontSize = 14.sp
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showSleepTimerDialog = false }) {
                    androidx.compose.material3.Text("OK", color = com.example.lumisound.ui.theme.GradientStart)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showSleepTimerDialog = false
                    playerViewModel.audioPlayerService.cancelSleepTimer()
                    playerViewModel.playerStateHolder.sleepTimerActive = false
                }) {
                    androidx.compose.material3.Text("Отменить таймер", color = androidx.compose.ui.graphics.Color(0xFFFF5C6C))
                }
            },
            containerColor = com.example.lumisound.ui.theme.LocalAppColors.current.surface
        )
    }

    // Соседние треки для отображения при свайпе
    val playlist by playerViewModel.playerStateHolder.playlist.collectAsState()
    val currentIdx by playerViewModel.playerStateHolder.currentIndex.collectAsState()
    val miniPrevTrack = playlist.getOrNull(currentIdx - 1)
    val miniNextTrack = playlist.getOrNull(currentIdx + 1)

    MiniPlayer(
        currentTrack = currentTrack,
        isPlaying = isPlaying,
        progress = miniProgress,
        onPlayPauseClick = { playerViewModel.togglePlayPause() },
        onTrackClick = {
            if (animationProgress < 0.05f && !isDraggingMiniPlayer) {
                navController.currentBackStackEntry?.savedStateHandle?.set("playerSourceRoute", localCurrentRoute)
                navController.navigate("now_playing/${currentTrack.id}") { launchSingleTop = true }
            }
        },
        onAddClick = {},
        onLikeClick = { onLikedChange(!isLiked) },
        onArtistClick = { artistId, artistName, artistImageUrl ->
            navController.navigate(MainDestination.Artist().createRoute(artistId, artistName, artistImageUrl))
        },
        onNextTrack = { playerViewModel.nextTrack() },
        onPreviousTrack = { playerViewModel.previousTrack() },
        hasPrevious = playerViewModel.hasPrevious,
        nextTrackInfo = miniNextTrack,
        prevTrackInfo = miniPrevTrack,
        isLiked = isLiked,
        avgScore = avgScore,
        isSleepTimerActive = isSleepTimerActive,
        onSleepTimerClick = {
            val remainingMs = playerViewModel.audioPlayerService.getSleepTimerRemainingMs()
            if (remainingMs > 0L) {
                val totalSec = remainingMs / 1000
                val min = totalSec / 60
                val sec = totalSec % 60
                sleepTimerRemainingText = "${min}:${sec.toString().padStart(2, '0')}"
                showSleepTimerDialog = true
            }
        },
        animationProgress = animationProgress,
        onAnimationProgressChange = { onRawProgressChange(it.coerceIn(0f, 1f)) },
        onDragStart = { onDraggingChange(true); onVelocityChange(0f) },
        onDragVelocityChange = { onVelocityChange(it) },
        onDragEnd = {
            val currentProgress = getRawProgress()
            val velocity = getLastVelocity()
            onDraggingChange(false)
            scope.launch {
                val shouldOpen = currentProgress >= 0.3f || velocity > 8f
                if (shouldOpen) {
                    // Сразу сбрасываем прогресс и открываем плеер — без двойной анимации
                    onRawProgressChange(0f)
                    onTargetProgressChange(0f)
                    navController.currentBackStackEntry?.savedStateHandle?.set("playerSourceRoute", localCurrentRoute)
                    navController.navigate("now_playing/${currentTrack.id}") { launchSingleTop = true }
                } else {
                    onTargetProgressChange(0f)
                    delay(300)
                    onRawProgressChange(0f)
                }
            }
        },
        modifier = Modifier.fillMaxWidth()
    )

}