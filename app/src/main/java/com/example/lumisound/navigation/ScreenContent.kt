package com.example.lumisound.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.example.lumisound.feature.home.HomeScreen
import com.example.lumisound.feature.home.TrackPreview
import com.example.lumisound.feature.profile.ProfileScreen
import com.example.lumisound.feature.profile.UserStats
import com.example.lumisound.feature.ratings.RatedTrack
import com.example.lumisound.feature.ratings.RatingsScreen
import com.example.lumisound.feature.search.SearchScreen

@Composable
fun ScreenContent(
    route: String,
    navController: NavHostController,
    userName: String
) {
    when (route) {
        "home" -> HomeScreen(
            navController = navController,
            userName = userName
        )
        "search" -> {
            val mockTracks = listOf(
                TrackPreview(
                    id = "1",
                    title = "Midnight Dreams",
                    artist = "Luna Eclipse",
                    coverUrl = null
                ),
                TrackPreview(
                    id = "2",
                    title = "Neon Lights",
                    artist = "Synthwave Collective",
                    coverUrl = null
                )
            )
            SearchScreen(
                navController = navController,
                trendingTracks = mockTracks,
                onTrackClick = { trackId ->
                    navController.navigate("now_playing/$trackId")
                }
            )
        }
        "ratings" -> {
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
            RatingsScreen(
                navController = navController,
                ratedTracks = mockRatedTracks,
                onTrackClick = { trackId ->
                    navController.navigate("now_playing/$trackId")
                }
            )
        }
        "profile" -> {
            val mockRatedTracks = listOf(
                RatedTrack(
                    id = "1",
                    title = "Midnight Dreams",
                    artist = "Luna Eclipse",
                    rating = 9,
                    ratedAt = "5 января"
                )
            )
            ProfileScreen(
                navController = navController,
                userName = userName,
                stats = UserStats(
                    tracksListened = 247,
                    tracksRated = mockRatedTracks.size,
                    playlistsCreated = 0,
                    likedTracks = 84
                ),
                onRatingsClick = {
                    navController.navigate("ratings")
                }
            )
        }
    }
}
