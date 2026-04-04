package com.example.lumisound.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import com.example.lumisound.feature.home.HomeScreen
import com.example.lumisound.feature.home.TrackPreview
import com.example.lumisound.feature.profile.ProfileScreen
import com.example.lumisound.feature.ratings.RatedTrack
import com.example.lumisound.feature.ratings.RatingsScreen
import com.example.lumisound.feature.search.SearchScreen
import com.example.lumisound.navigation.MainDestination

@Composable
fun ScreenContent(
    route: String,
    navController: NavHostController,
    userName: String
) {
    when (route) {
        "home" -> key(route) {
            HomeScreen(
                navController = navController,
                userName = userName
            )
        }
        "search" -> key(route) {
            // Используем remember для mock данных, чтобы не создавать их при каждой рекомпозиции
            val mockTracks = remember {
                listOf(
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
            }
            // Оптимизация для 120Hz: используем remember для callback, чтобы не пересоздавать lambda
            val onTrackClick = remember(navController) {
                { trackId: String ->
                    navController.navigate("now_playing/$trackId")
                }
            }
            SearchScreen(
                navController = navController,
                trendingTracks = mockTracks,
                onTrackClick = onTrackClick
            )
        }
        "ratings" -> key(route) {
            // Используем remember для mock данных
            val mockRatedTracks = remember {
                listOf(
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
            }
            // Оптимизация для 120Hz: используем remember для callback, чтобы не пересоздавать lambda
            val onTrackClick = remember(navController) {
                { trackId: String ->
                    navController.navigate("now_playing/$trackId")
                }
            }
            RatingsScreen(
                navController = navController,
                ratedTracks = mockRatedTracks,
                onTrackClick = onTrackClick
            )
        }
        "profile" -> key(route) {
            val playerViewModel: com.example.lumisound.feature.nowplaying.PlayerViewModel =
                androidx.hilt.navigation.compose.hiltViewModel()
            val onRatingsClick = remember(navController) {
                { navController.navigate("ratings") }
            }
            val onSettingsClick = remember(navController) {
                { navController.navigate("settings") }
            }
            val onArtistClick = remember(navController) {
                { artistId: String, artistName: String, artistImageUrl: String? ->
                    navController.navigate(
                        MainDestination.Artist().createRoute(artistId, artistName, artistImageUrl)
                    )
                }
            }
            val onTrackClick = remember(playerViewModel) {
                { trackId: String, title: String, artist: String, coverUrl: String?, previewUrl: String? ->
                    if (!previewUrl.isNullOrBlank()) {
                        playerViewModel.playTrack(
                            com.example.lumisound.data.model.Track(
                                id = trackId,
                                name = title,
                                artist = artist,
                                imageUrl = coverUrl,
                                previewUrl = previewUrl
                            )
                        )
                    }
                }
            }
            ProfileScreen(
                navController = navController,
                onRatingsClick = onRatingsClick,
                onSettingsClick = onSettingsClick,
                onArtistClick = onArtistClick,
                onTrackClick = onTrackClick
            )
        }
    }
}
