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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.lumisound.data.model.Track
import com.example.lumisound.data.player.PlayerStateHolder
import com.example.lumisound.feature.home.HomeScreen
import com.example.lumisound.feature.nowplaying.NowPlayingScreen
import com.example.lumisound.feature.nowplaying.TestPlayerScreen
import com.example.lumisound.feature.search.PlayerStateHolderEntryPoint
import dagger.hilt.android.EntryPointAccessors
import com.example.lumisound.feature.profile.ProfileScreen
import com.example.lumisound.feature.ratings.RatedTrack
import com.example.lumisound.feature.ratings.RatingsScreen
import com.example.lumisound.feature.search.SearchScreen
import com.example.lumisound.feature.artist.ArtistCardScreen
import com.example.lumisound.feature.settings.SettingsScreen
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Навигационные маршруты основного приложения.
 */
sealed class MainDestination(val route: String) {
    data object Home : MainDestination("home")
    data object Search : MainDestination("search")
    data object Ratings : MainDestination("ratings")
    data object Profile : MainDestination("profile")
    data object Settings : MainDestination("settings")
    data class NowPlaying(val trackId: String = "{trackId}") : MainDestination("now_playing/{trackId}") {
        fun createRoute(trackId: String) = "now_playing/$trackId"
    }
    data class Artist(val artistName: String = "{artistName}", val artistImageUrl: String = "{artistImageUrl}") : MainDestination("artist/{artistName}/{artistImageUrl}") {
        fun createRoute(artistName: String, artistImageUrl: String?): String {
            val encodedName = URLEncoder.encode(artistName, StandardCharsets.UTF_8.toString())
            val encodedImageUrl = URLEncoder.encode(artistImageUrl ?: "", StandardCharsets.UTF_8.toString())
            return "artist/$encodedName/$encodedImageUrl"
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
    userName: String = "Александр"
) {
    val context = LocalContext.current
    val playerStateHolder = remember {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            PlayerStateHolderEntryPoint::class.java
        )
        entryPoint.playerStateHolder()
    }
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

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Основные вкладки с swipe navigation и mini player
        addTabDestination(
            route = MainDestination.Home.route,
            currentRoute = "home",
            navController = navController,
            userName = userName
        )
        
        addTabDestination(
            route = MainDestination.Search.route,
            currentRoute = "search",
            navController = navController,
            userName = userName
        )
        
        addTabDestination(
            route = MainDestination.Ratings.route,
            currentRoute = "ratings",
            navController = navController,
            userName = userName
        )
        
        addTabDestination(
            route = MainDestination.Profile.route,
            currentRoute = "profile",
            navController = navController,
            userName = userName
        )

        composable(
            route = MainDestination.NowPlaying().route,
            arguments = listOf(navArgument("trackId") { type = NavType.StringType }),
            // Убираем анимации переходов, чтобы страница не обновлялась и не мигала
            enterTransition = { androidx.compose.animation.EnterTransition.None },
            exitTransition = { androidx.compose.animation.ExitTransition.None }
        ) { backStackEntry ->
            // ТЕСТОВАЯ СТРАНИЦА - простая страница с фоном для проверки status bar
            val currentTrack by playerStateHolder.currentTrack.collectAsState()
            val track = currentTrack
            
            // Получаем предыдущий маршрут для правильного возврата
            // Используем previousBackStackEntry - это правильный предыдущий маршрут в Navigation Compose
            val previousRoute = remember(backStackEntry) {
                navController.previousBackStackEntry?.destination?.route
                    ?.takeIf { it in listOf("home", "search", "ratings", "profile") }
                    ?: "home"
            }
            
            if (track != null) {
                TestPlayerScreen(
                    track = track,
                    onClose = { 
                        // Используем popBackStack для правильного возврата
                        val result = navController.popBackStack()
                        if (!result) {
                            // Если popBackStack вернул false, навигируем на сохранённый маршрут
                            navController.navigate(previousRoute) {
                                popUpTo("now_playing") { inclusive = true }
                            }
                        }
                    }
                )
            } else {
                val trackId = backStackEntry.arguments?.getString("trackId") ?: "1"
                val mockTrack = mockTracks.find { it.id == trackId } ?: mockTracks[0]
                TestPlayerScreen(
                    track = Track(
                        id = mockTrack.id,
                        name = mockTrack.title,
                        artist = mockTrack.artist,
                        imageUrl = mockTrack.coverUrl,
                        previewUrl = null,
                        genre = null
                    ),
                    onClose = { 
                        // Используем popBackStack для правильного возврата
                        val result = navController.popBackStack()
                        if (!result) {
                            // Если popBackStack вернул false, навигируем на сохранённый маршрут
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
                navArgument("artistName") { type = NavType.StringType },
                navArgument("artistImageUrl") { type = NavType.StringType }
            ),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) { backStackEntry ->
            val artistName = backStackEntry.arguments?.getString("artistName")?.let {
                java.net.URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
            } ?: ""
            val artistImageUrl = backStackEntry.arguments?.getString("artistImageUrl")?.let {
                val decoded = java.net.URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
                if (decoded.isEmpty()) null else decoded
            }
            
            ArtistCardScreen(
                artistName = artistName,
                artistImageUrl = artistImageUrl,
                onClose = { navController.popBackStack() }
            )
        }
        
        composable(
            route = MainDestination.Settings.route,
            enterTransition = { fadeIn(animationSpec = tween(200)) },
            exitTransition = { fadeOut(animationSpec = tween(200)) }
        ) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

/**
 * Добавляет destination для вкладки без анимаций переключения (анимация выполняется через HorizontalPager).
 */
private fun NavGraphBuilder.addTabDestination(
    route: String,
    currentRoute: String,
    navController: NavHostController,
    userName: String
) {
    composable(
        route = route
        // Убираем анимации - HorizontalPager сам обеспечивает плавный свайп
        // Не указываем enterTransition и exitTransition, чтобы избежать конфликта с HorizontalPager
    ) {
        SwipeableNavHost(
            navController = navController,
            currentRoute = currentRoute,
            userName = userName,
            onNavigate = { route -> navController.navigate(route) }
        )
    }
}



