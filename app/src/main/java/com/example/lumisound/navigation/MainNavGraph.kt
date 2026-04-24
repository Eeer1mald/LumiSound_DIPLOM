package com.example.lumisound.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lumisound.ui.theme.LocalAppColors
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavOptionsBuilder
import com.example.lumisound.data.model.Track
import com.example.lumisound.data.player.PlayerStateHolder
import com.example.lumisound.feature.home.HomeScreen
import com.example.lumisound.feature.nowplaying.NowPlayingScreen
import com.example.lumisound.feature.nowplaying.PlayerScreen
import com.example.lumisound.feature.search.PlayerStateHolderEntryPoint
import dagger.hilt.android.EntryPointAccessors
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.lumisound.feature.profile.ProfileScreen
import com.example.lumisound.feature.ratings.RatedTrack
import com.example.lumisound.feature.ratings.RatingsScreen
import com.example.lumisound.feature.search.SearchScreen
import com.example.lumisound.feature.artist.ArtistCardScreen
import com.example.lumisound.feature.settings.SettingsScreen
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * Навигационные маршруты основного приложения.
 */
sealed class MainDestination(val route: String) {
    data object Home : MainDestination("home")
    data object Search : MainDestination("search")
    data object Ratings : MainDestination("ratings")
    data object Profile : MainDestination("profile")
    data object Settings : MainDestination("settings")
    data class PublicProfile(
        val userId: String = "{userId}",
        val username: String = "{username}",
        val avatarUrl: String = "{avatarUrl}"
    ) : MainDestination("public_profile/{userId}/{username}/{avatarUrl}") {
        fun createRoute(userId: String, username: String, avatarUrl: String?): String {
            val encodedUser = java.net.URLEncoder.encode(username, "UTF-8")
            val encodedAvatar = java.net.URLEncoder.encode(avatarUrl ?: "", "UTF-8")
            return "public_profile/$userId/$encodedUser/$encodedAvatar"
        }
    }
    data class Review(val trackId: String = "{trackId}") : MainDestination("review/{trackId}/{trackTitle}/{trackArtist}") {
        fun createRoute(trackId: String, trackTitle: String = "", trackArtist: String = ""): String {
            val encodedTitle = URLEncoder.encode(trackTitle.ifBlank { "—" }, StandardCharsets.UTF_8.toString())
            val encodedArtist = URLEncoder.encode(trackArtist.ifBlank { "—" }, StandardCharsets.UTF_8.toString())
            return "review/$trackId/$encodedTitle/$encodedArtist"
        }
    }
    data class Reviews(val trackId: String = "{trackId}") : MainDestination("reviews/{trackId}/{trackTitle}/{trackArtist}") {
        fun createRoute(trackId: String, trackTitle: String = "", trackArtist: String = ""): String {
            val encodedTitle = URLEncoder.encode(trackTitle.ifBlank { "—" }, StandardCharsets.UTF_8.toString())
            val encodedArtist = URLEncoder.encode(trackArtist.ifBlank { "—" }, StandardCharsets.UTF_8.toString())
            return "reviews/$trackId/$encodedTitle/$encodedArtist"
        }
    }
    data class NowPlaying(val trackId: String = "{trackId}") : MainDestination("now_playing/{trackId}") {
        fun createRoute(trackId: String) = "now_playing/$trackId"
    }
    data class Artist(val artistId: String = "{artistId}", val artistName: String = "{artistName}", val artistImageUrl: String = "{artistImageUrl}") : MainDestination("artist/{artistId}/{artistName}/{artistImageUrl}") {
        fun createRoute(artistId: String?, artistName: String, artistImageUrl: String?): String {
            val encodedId = URLEncoder.encode(artistId ?: "", StandardCharsets.UTF_8.toString())
            val encodedName = URLEncoder.encode(artistName, StandardCharsets.UTF_8.toString())
            val encodedImageUrl = URLEncoder.encode(artistImageUrl ?: "", StandardCharsets.UTF_8.toString())
            return "artist/$encodedId/$encodedName/$encodedImageUrl"
        }
    }
}

/**
 * Порядок вкладок для определения направления анимации при переключении.
 */
private val TAB_ORDER = listOf("home", "search", "ratings", "profile")

/**
 * Получает индекс вкладки по её маршруту.
 */
private fun getTabIndex(route: String?): Int {
    return TAB_ORDER.indexOf(route ?: "").coerceAtLeast(0)
}

// Анимации переключения между вкладками убраны - используется HorizontalPager для плавного свайпа
// Старые функции createTabEnterTransition и createTabExitTransition удалены, так как они вызывали конфликт
// с HorizontalPager и приводили к тряске экрана и белым полоскам

@Composable
fun MainNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = MainDestination.Home.route,
    userName: String = "Александр",
    synthesisInviteCode: String? = null
) {
    val context = LocalContext.current
    val playerStateHolder = remember {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            PlayerStateHolderEntryPoint::class.java
        )
        entryPoint.playerStateHolder()
    }
    // Глобальный PlayerViewModel для мини-плеера поверх всех экранов
    val globalPlayerViewModel: com.example.lumisound.feature.nowplaying.PlayerViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val globalCurrentTrack by playerStateHolder.currentTrack.collectAsState()
    val globalIsPlaying by globalPlayerViewModel.isPlaying.collectAsState()
    // Прогресс НЕ читаем здесь — он изолирован внутри мини-плеера через derivedStateOf

    // Mock data
    val mockTracks = listOf(
        com.example.lumisound.feature.home.TrackPreview(
            id = "1",
            title = "Midnight Dreams",
            artist = "Luna Eclipse",
            coverUrl = null
        ),
        com.example.lumisound.feature.home.TrackPreview(
            id = "2",
            title = "Neon Lights",
            artist = "Synthwave Collective",
            coverUrl = null
        ),
        com.example.lumisound.feature.home.TrackPreview(
            id = "3",
            title = "Vinyl Memories",
            artist = "The Retro Band",
            coverUrl = null
        )
    )

    // TODO: Заменить mock данные на реальные из репозитория
    val mockRatedTracks = listOf(
        RatedTrack(
            id = "1",
            title = "Midnight Dreams",
            artist = "Luna Eclipse",
            rating = 9,
            ratedAt = "5 января"
        ),
        RatedTrack(
            id = "2",
            title = "Neon Lights",
            artist = "Synthwave Collective",
            rating = 8,
            ratedAt = "4 января"
        )
    )

    // Оборачиваем NavHost в Box с черным фоном, чтобы убрать белое мерцание
    // когда плеер становится прозрачным при закрытии
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalAppColors.current.background)
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
        // Единственная точка входа для всех вкладок — SwipeableNavHost с HorizontalPager
        // Все 4 вкладки живут внутри одного composable, навигация между ними через pager
        composable(
            route = MainDestination.Home.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            SwipeableNavHost(
                navController = navController,
                currentRoute = "home",
                userName = userName,
                synthesisInviteCode = synthesisInviteCode,
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        composable(
            route = MainDestination.NowPlaying().route,
            arguments = listOf(navArgument("trackId") { type = NavType.StringType }),
            // Убираем анимации переходов, чтобы страница не обновлялась при возврате
            // Это гарантирует, что предыдущая страница сохранит свое состояние
            enterTransition = { androidx.compose.animation.EnterTransition.None },
            exitTransition = { androidx.compose.animation.ExitTransition.None },
            popEnterTransition = { androidx.compose.animation.EnterTransition.None },
            popExitTransition = { androidx.compose.animation.ExitTransition.None }
        ) { backStackEntry ->
            // ТЕСТОВАЯ СТРАНИЦА - простая страница с фоном для проверки status bar
            val currentTrack by playerStateHolder.currentTrack.collectAsState()
            val track = currentTrack
            
            // Получаем маршрут экрана, С КОТОРОГО открыли плеер.
            // Мы сохраняем playerSourceRoute в currentBackStackEntry.savedStateHandle ДО навигации,
            // поэтому после навигации это значение будет в previousBackStackEntry.savedStateHandle
            val previousRoute = remember(backStackEntry) {
                // Сначала пробуем взять из savedStateHandle предыдущего entry (где мы были до навигации)
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.get<String>("playerSourceRoute")
                    ?.takeIf { it in listOf("home", "search", "ratings", "profile") }
                    // Если нет, пробуем из текущего entry (на случай, если сохранили после навигации)
                    ?: backStackEntry.savedStateHandle
                        .get<String>("playerSourceRoute")
                        ?.takeIf { it in listOf("home", "search", "ratings", "profile") }
                    // Фоллбек: маршрут самой предыдущей destination или home.
                    ?: navController.previousBackStackEntry?.destination?.route
                        ?.takeIf { it in listOf("home", "search", "ratings", "profile") }
                        ?: "home"
            }
            
            if (track != null) {
                PlayerScreen(
                    track = track,
                    previousRoute = previousRoute,
                    navController = navController,
                    userName = userName,
                    onClose = { 
                        // КРИТИЧНО: сразу вызываем popBackStack при закрытии
                        // Это гарантирует немедленный выход с экрана плеера
                        // Трек в PlayerStateHolder НЕ очищается - мини-плеер останется видимым
                        try {
                            val result = navController.popBackStack()
                            if (!result) {
                                // Если popBackStack вернул false, навигируем на сохранённый маршрут
                                navController.navigate(previousRoute) {
                                    popUpTo("now_playing") { inclusive = true }
                                }
                            }
                        } catch (e: Exception) {
                            // Fallback: навигируем на предыдущий маршрут напрямую
                            navController.navigate(previousRoute) {
                                popUpTo("now_playing") { inclusive = true }
                            }
                        }
                    }
                )
            } else {
                val trackId = backStackEntry.arguments?.getString("trackId") ?: "1"
                val mockTrack = mockTracks.find { it.id == trackId } ?: mockTracks[0]
                PlayerScreen(
                    track = Track(
                        id = mockTrack.id,
                        name = mockTrack.title,
                        artist = mockTrack.artist,
                        imageUrl = mockTrack.coverUrl,
                        previewUrl = null,
                        genre = null
                    ),
                    previousRoute = previousRoute,
                    navController = navController,
                    userName = userName,
                    onClose = { 
                        // КРИТИЧНО: сразу вызываем popBackStack при закрытии
                        // Это гарантирует немедленный выход с экрана плеера
                        // Трек в PlayerStateHolder НЕ очищается - мини-плеер останется видимым
                        try {
                            val result = navController.popBackStack()
                            if (!result) {
                                // Если popBackStack вернул false, навигируем на сохранённый маршрут
                                navController.navigate(previousRoute) {
                                    popUpTo("now_playing") { inclusive = true }
                                }
                            }
                        } catch (e: Exception) {
                            // Fallback: навигируем на предыдущий маршрут напрямую
                            navController.navigate(previousRoute) {
                                popUpTo("now_playing") { inclusive = true }
                            }
                        }
                    }
                )
            }
            
            // ЗАКОММЕНТИРОВАНО ДЛЯ ТЕСТИРОВАНИЯ
            // Получаем трек из PlayerStateHolder
            /*
            val currentTrack by playerStateHolder.currentTrack.collectAsState()
            val track = currentTrack
            if (track != null) {
                NowPlayingScreen(
                    track = track,
                    onClose = { navController.popBackStack() },
                    onNavigate = { route -> navController.navigate(route) },
                    onArtistClick = { artistName, artistImageUrl ->
                        navController.navigate(MainDestination.Artist().createRoute(artistName, artistImageUrl))
                    }
                )
            } else {
                // TODO: Убрать fallback на mock данные после реализации загрузки треков
                // Fallback на mock данные если трек не найден
                val trackId = backStackEntry.arguments?.getString("trackId") ?: "1"
                val track = mockTracks.find { it.id == trackId } ?: mockTracks[0]
                NowPlayingScreen(
                    track = Track(
                        id = track.id,
                        name = track.title,
                        artist = track.artist,
                        imageUrl = track.coverUrl,
                        previewUrl = null,
                        genre = null
                    ),
                    onClose = { navController.popBackStack() },
                    onNavigate = { route -> navController.navigate(route) },
                    onArtistClick = { artistName, artistImageUrl ->
                        navController.navigate(MainDestination.Artist().createRoute(artistName, artistImageUrl))
                    }
                )
            }
            */
        }
        
        composable(
            route = MainDestination.Artist().route,
            arguments = listOf(
                navArgument("artistId") { type = NavType.StringType },
                navArgument("artistName") { type = NavType.StringType },
                navArgument("artistImageUrl") { type = NavType.StringType }
            ),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) { backStackEntry ->
            val artistId = backStackEntry.arguments?.getString("artistId")?.let {
                val decoded = java.net.URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
                if (decoded.isEmpty()) null else decoded
            }
            val artistName = backStackEntry.arguments?.getString("artistName")?.let {
                java.net.URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
            } ?: ""
            val artistImageUrl = backStackEntry.arguments?.getString("artistImageUrl")?.let {
                val decoded = java.net.URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
                if (decoded.isEmpty()) null else decoded
            }

            ArtistCardScreen(
                artistId = artistId,
                artistName = artistName,
                artistImageUrl = artistImageUrl,
                onClose = { navController.popBackStack() },
                onTrackClick = { track ->
                    // Просто запускаем трек — мини-плеер обновится автоматически
                    globalPlayerViewModel.playTrack(track)
                }
            )
        }
        
        composable(
            route = MainDestination.Settings.route,
            enterTransition = { fadeIn(animationSpec = tween(200)) },
            exitTransition = { fadeOut(animationSpec = tween(200)) }
        ) {
            val context = LocalContext.current
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    // Перезапускаем Activity — самый надёжный способ сбросить всё состояние
                    val activity = context as? android.app.Activity
                    activity?.let {
                        val intent = it.intent
                        it.finish()
                        it.startActivity(intent)
                    }
                }
            )
        }

        composable(
            route = MainDestination.PublicProfile().route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("username") { type = NavType.StringType },
                navArgument("avatarUrl") { type = NavType.StringType }
            ),
            enterTransition = { fadeIn(animationSpec = tween(250)) },
            exitTransition = { fadeOut(animationSpec = tween(200)) }
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val username = backStackEntry.arguments?.getString("username")?.let {
                java.net.URLDecoder.decode(it, "UTF-8")
            } ?: ""
            val avatarUrl = backStackEntry.arguments?.getString("avatarUrl")?.let {
                val decoded = java.net.URLDecoder.decode(it, "UTF-8")
                if (decoded.isEmpty()) null else decoded
            }
            com.example.lumisound.feature.profile.PublicProfileScreen(
                userId = userId,
                username = username,
                avatarUrl = avatarUrl,
                onClose = { navController.popBackStack() },
                onArtistClick = { artistId, artistName, artistImageUrl ->
                    navController.navigate(
                        MainDestination.Artist().createRoute(artistId, artistName, artistImageUrl)
                    )
                }
            )
        }

        composable(
            route = MainDestination.Review().route,
            arguments = listOf(
                navArgument("trackId") { type = NavType.StringType },
                navArgument("trackTitle") { type = NavType.StringType; defaultValue = "" },
                navArgument("trackArtist") { type = NavType.StringType; defaultValue = "" }
            ),
            enterTransition = { fadeIn(animationSpec = tween(250)) },
            exitTransition = { fadeOut(animationSpec = tween(200)) }
        ) { backStackEntry ->
            val trackId = backStackEntry.arguments?.getString("trackId") ?: ""
            val trackTitle = backStackEntry.arguments?.getString("trackTitle")
                ?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: ""
            val trackArtist = backStackEntry.arguments?.getString("trackArtist")
                ?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: ""

            val reviewTrack by playerStateHolder.reviewTrack.collectAsState()
            val currentTrack by playerStateHolder.currentTrack.collectAsState()
            // Приоритет: reviewTrack с совпадающим ID → currentTrack с совпадающим ID → минимальный трек из параметров
            val track = reviewTrack?.takeIf { it.id == trackId }
                ?: currentTrack?.takeIf { it.id == trackId }
                ?: Track(id = trackId, name = trackTitle, artist = trackArtist)

            com.example.lumisound.feature.ratings.ReviewScreen(
                track = track,
                onClose = { navController.popBackStack() },
                onOpenReviews = {
                    navController.navigate(
                        MainDestination.Reviews().createRoute(track.id, track.name, track.artist)
                    )
                },
                navController = navController
            )
        }

        composable(
            route = MainDestination.Reviews().route,
            arguments = listOf(
                navArgument("trackId") { type = NavType.StringType },
                navArgument("trackTitle") { type = NavType.StringType; defaultValue = "" },
                navArgument("trackArtist") { type = NavType.StringType; defaultValue = "" }
            ),
            enterTransition = { fadeIn(animationSpec = tween(250)) },
            exitTransition = { fadeOut(animationSpec = tween(200)) }
        ) { backStackEntry ->
            val trackId = backStackEntry.arguments?.getString("trackId") ?: ""
            val trackTitle = backStackEntry.arguments?.getString("trackTitle")
                ?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: ""
            val trackArtist = backStackEntry.arguments?.getString("trackArtist")
                ?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: ""

            val reviewTrack by playerStateHolder.reviewTrack.collectAsState()
            val currentTrack by playerStateHolder.currentTrack.collectAsState()
            val track = reviewTrack?.takeIf { it.id == trackId }
                ?: currentTrack?.takeIf { it.id == trackId }
                ?: Track(id = trackId, name = trackTitle, artist = trackArtist)

            com.example.lumisound.feature.ratings.ReviewsScreen(
                track = track,
                onClose = { navController.popBackStack() }
            )
        }
        }

        // Мини-плеер поверх всех экранов кроме SwipeableNavHost (там свой) и полного плеера
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val isOnFullPlayer = currentRoute?.startsWith("now_playing") == true
        val isOnMainTabs = currentRoute == "home"

        // Глобальный тост "Кэш очищен" — поверх любого экрана
        val settingsVm: com.example.lumisound.feature.settings.SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
        val settingsState by settingsVm.state.collectAsState()
        var showCacheToast by remember { mutableStateOf(false) }
        LaunchedEffect(settingsState.cacheCleared) {
            if (settingsState.cacheCleared) {
                showCacheToast = true
                settingsVm.clearMessages()
                delay(2500)
                showCacheToast = false
            }
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = showCacheToast,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically { -it },
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp).statusBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .background(com.example.lumisound.ui.theme.GradientStart, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                androidx.compose.material3.Text("Кэш очищен", color = androidx.compose.ui.graphics.Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
        val isOnReviewScreen = currentRoute?.startsWith("review") == true

        // Задержка показа мини-плеера после закрытия полного плеера —
        // чтобы не мигал в момент popBackStack()
        var miniPlayerVisible by remember { mutableStateOf(false) }
        LaunchedEffect(isOnFullPlayer) {
            if (!isOnFullPlayer) {
                // Ждём 2 кадра (33мс) прежде чем показать мини-плеер
                delay(33)
                miniPlayerVisible = true
            } else {
                miniPlayerVisible = false
            }
        }

        if (globalCurrentTrack != null && miniPlayerVisible && !isOnFullPlayer && !isOnMainTabs && !isOnReviewScreen) {
            // Состояние свайпа вверх для открытия плеера
            var globalMiniRawProgress by remember { mutableFloatStateOf(0f) }
            var globalMiniIsDragging by remember { mutableStateOf(false) }
            var globalMiniLastVelocity by remember { mutableFloatStateOf(0f) }
            var globalMiniTargetProgress by remember { mutableFloatStateOf(0f) }
            val globalMiniAnimatedProgress by animateFloatAsState(
                targetValue = globalMiniTargetProgress,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
                label = "globalMiniAnim"
            )
            val globalMiniProgress = if (globalMiniIsDragging) globalMiniRawProgress else globalMiniAnimatedProgress
            val globalMiniScope = rememberCoroutineScope()

            // Соседние треки
            val globalPlaylist by playerStateHolder.playlist.collectAsState()
            val globalCurrentIdx by playerStateHolder.currentIndex.collectAsState()

            // Прогресс для кольца мини-плеера — читаем снаружи лямбды
            val globalMiniPos by globalPlayerViewModel.currentPosition.collectAsState()
            val globalMiniDur by globalPlayerViewModel.duration.collectAsState()
            val globalMiniRingProgress = if (globalMiniDur > 0)
                (globalMiniPos.toFloat() / globalMiniDur.toFloat()).coerceIn(0f, 1f) else 0f

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                com.example.lumisound.feature.home.components.MiniPlayer(
                    currentTrack = globalCurrentTrack,
                    isPlaying = globalIsPlaying,
                    progress = globalMiniRingProgress,
                    isSleepTimerActive = playerStateHolder.sleepTimerActive,
                    onPlayPauseClick = { globalPlayerViewModel.togglePlayPause() },
                    onTrackClick = {
                        if (globalMiniProgress < 0.05f && !globalMiniIsDragging) {
                            navController.navigate(
                                MainDestination.NowPlaying().createRoute(globalCurrentTrack!!.id)
                            ) { launchSingleTop = true }
                        }
                    },
                    onAddClick = {},
                    onLikeClick = {},
                    onArtistClick = { artistId, artistName, artistImageUrl ->
                        navController.navigate(
                            MainDestination.Artist().createRoute(artistId, artistName, artistImageUrl)
                        )
                    },
                    onNextTrack = { globalPlayerViewModel.nextTrack() },
                    onPreviousTrack = { globalPlayerViewModel.previousTrack() },
                    hasPrevious = globalPlayerViewModel.hasPrevious,
                    nextTrackInfo = globalPlaylist.getOrNull(globalCurrentIdx + 1),
                    prevTrackInfo = globalPlaylist.getOrNull(globalCurrentIdx - 1),
                    avgScore = globalPlayerViewModel.avgScore.collectAsState().value,
                    animationProgress = globalMiniProgress,
                    onAnimationProgressChange = { globalMiniRawProgress = it.coerceIn(0f, 1f) },
                    onDragStart = { globalMiniIsDragging = true; globalMiniLastVelocity = 0f },
                    onDragVelocityChange = { globalMiniLastVelocity = it },
                    onDragEnd = {
                        val progress = globalMiniRawProgress
                        val velocity = globalMiniLastVelocity
                        globalMiniIsDragging = false
                        val shouldOpen = progress >= 0.3f || velocity > 8f
                        if (shouldOpen) {
                            globalMiniRawProgress = 0f
                            globalMiniTargetProgress = 0f
                            navController.navigate(
                                MainDestination.NowPlaying().createRoute(globalCurrentTrack!!.id)
                            ) { launchSingleTop = true }
                        } else {
                            globalMiniTargetProgress = 0f
                            globalMiniScope.launch {
                                kotlinx.coroutines.delay(300)
                                globalMiniRawProgress = 0f
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}





