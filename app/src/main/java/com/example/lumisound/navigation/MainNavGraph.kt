package com.example.lumisound.navigation

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
import com.example.lumisound.feature.search.PlayerStateHolderEntryPoint
import dagger.hilt.android.EntryPointAccessors
import com.example.lumisound.feature.profile.ProfileScreen
import com.example.lumisound.feature.ratings.RatedTrack
import com.example.lumisound.feature.ratings.RatingsScreen
import com.example.lumisound.feature.search.SearchScreen

sealed class MainDestination(val route: String) {
    data object Home : MainDestination("home")
    data object Search : MainDestination("search")
    data object Ratings : MainDestination("ratings")
    data object Profile : MainDestination("profile")
    data class NowPlaying(val trackId: String = "{trackId}") : MainDestination("now_playing/{trackId}") {
        fun createRoute(trackId: String) = "now_playing/$trackId"
    }
}

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


    // Порядок вкладок для определения направления анимации
    val tabOrder = listOf("home", "search", "ratings", "profile")
    
    fun getTabIndex(route: String?): Int {
        return (tabOrder.indexOf(route ?: "")).coerceAtLeast(0)
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Основные вкладки с swipe navigation и mini player
        composable(
            route = MainDestination.Home.route,
            enterTransition = {
                val fromIndex = getTabIndex(initialState.destination.route)
                val toIndex = getTabIndex(targetState.destination.route)
                val isSlidingRight = toIndex > fromIndex
                slideInHorizontally(
                    initialOffsetX = { if (isSlidingRight) it else -it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(animationSpec = tween(250))
            },
            exitTransition = {
                val fromIndex = getTabIndex(initialState.destination.route)
                val toIndex = getTabIndex(targetState.destination.route)
                val isSlidingRight = toIndex > fromIndex
                slideOutHorizontally(
                    targetOffsetX = { if (isSlidingRight) -it else it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeOut(animationSpec = tween(250))
            }
        ) {
            SwipeableNavHost(
                navController = navController,
                currentRoute = "home",
                userName = userName,
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        composable(
            route = MainDestination.Search.route,
            enterTransition = {
                val fromIndex = getTabIndex(initialState.destination.route)
                val toIndex = getTabIndex(targetState.destination.route)
                val isSlidingRight = toIndex > fromIndex
                slideInHorizontally(
                    initialOffsetX = { if (isSlidingRight) it else -it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(animationSpec = tween(250))
            },
            exitTransition = {
                val fromIndex = getTabIndex(initialState.destination.route)
                val toIndex = getTabIndex(targetState.destination.route)
                val isSlidingRight = toIndex > fromIndex
                slideOutHorizontally(
                    targetOffsetX = { if (isSlidingRight) -it else it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeOut(animationSpec = tween(250))
            }
        ) {
            SwipeableNavHost(
                navController = navController,
                currentRoute = "search",
                userName = userName,
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        composable(
            route = MainDestination.Ratings.route,
            enterTransition = {
                val fromIndex = getTabIndex(initialState.destination.route)
                val toIndex = getTabIndex(targetState.destination.route)
                val isSlidingRight = toIndex > fromIndex
                slideInHorizontally(
                    initialOffsetX = { if (isSlidingRight) it else -it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(animationSpec = tween(250))
            },
            exitTransition = {
                val fromIndex = getTabIndex(initialState.destination.route)
                val toIndex = getTabIndex(targetState.destination.route)
                val isSlidingRight = toIndex > fromIndex
                slideOutHorizontally(
                    targetOffsetX = { if (isSlidingRight) -it else it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeOut(animationSpec = tween(250))
            }
        ) {
            SwipeableNavHost(
                navController = navController,
                currentRoute = "ratings",
                userName = userName,
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        composable(
            route = MainDestination.Profile.route,
            enterTransition = {
                val fromIndex = getTabIndex(initialState.destination.route)
                val toIndex = getTabIndex(targetState.destination.route)
                val isSlidingRight = toIndex > fromIndex
                slideInHorizontally(
                    initialOffsetX = { if (isSlidingRight) it else -it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(animationSpec = tween(250))
            },
            exitTransition = {
                val fromIndex = getTabIndex(initialState.destination.route)
                val toIndex = getTabIndex(targetState.destination.route)
                val isSlidingRight = toIndex > fromIndex
                slideOutHorizontally(
                    targetOffsetX = { if (isSlidingRight) -it else it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeOut(animationSpec = tween(250))
            }
        ) {
            SwipeableNavHost(
                navController = navController,
                currentRoute = "profile",
                userName = userName,
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        composable(
            route = MainDestination.NowPlaying().route,
            arguments = listOf(navArgument("trackId") { type = NavType.StringType }),
            enterTransition = { fadeIn(animationSpec = tween(150)) },
            exitTransition = { fadeOut(animationSpec = tween(150)) }
        ) { backStackEntry ->
            // Получаем трек из PlayerStateHolder
            val currentTrack by playerStateHolder.currentTrack.collectAsState()
            val track = currentTrack
            if (track != null) {
                NowPlayingScreen(
                    track = track,
                    onClose = { navController.popBackStack() },
                    onNavigate = { route -> navController.navigate(route) }
                )
            } else {
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
                    onNavigate = { route -> navController.navigate(route) }
                )
            }
        }

    }
}



