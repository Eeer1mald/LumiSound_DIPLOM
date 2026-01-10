package com.example.lumisound.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.lumisound.feature.home.components.BottomNavigationBar
import com.example.lumisound.feature.home.components.MiniPlayer
import com.example.lumisound.feature.nowplaying.PlayerViewModel
import com.example.lumisound.feature.search.PlayerStateHolderEntryPoint
import dagger.hilt.android.EntryPointAccessors

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
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            PlayerStateHolderEntryPoint::class.java
        )
        entryPoint.playerStateHolder()
    }
    val routes = listOf("home", "search", "ratings", "profile")
    val currentIndex = routes.indexOf(currentRoute).coerceIn(0, routes.size - 1)
    
    // Pager state - синхронизируется с currentRoute  
    val pagerState = rememberPagerState(initialPage = currentIndex)
    
    // Синхронизация pagerState с currentRoute
    LaunchedEffect(currentRoute) {
        val newIndex = routes.indexOf(currentRoute).coerceIn(0, routes.size - 1)
        if (pagerState.currentPage != newIndex) {
            pagerState.animateScrollToPage(newIndex)
        }
    }
    
    // Синхронизация навигации при свайпе в pager
    LaunchedEffect(pagerState.currentPage) {
        val newRoute = routes[pagerState.currentPage]
        if (newRoute != currentRoute) {
            onNavigate(newRoute)
        }
    }
    
    // Player state
    val currentTrack by playerStateHolder.currentTrack.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val currentPosition by playerViewModel.currentPosition.collectAsState()
    val duration by playerViewModel.duration.collectAsState()
    
    val progress = if (duration > 0 && currentTrack != null) {
        (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    } else 0f
    
    var isLiked by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Карусель из экранов - как в галерее
        HorizontalPager(
            count = routes.size,
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true
        ) { page ->
            ScreenContent(
                route = routes[page],
                navController = navController,
                userName = userName
            )
        }
        
        // Mini Player - показываем только если есть текущий трек, над нижней панелью
        val track = currentTrack
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            if (track != null) {
                MiniPlayer(
                    currentTrack = track,
                    isPlaying = isPlaying,
                    progress = progress,
                    onPlayPauseClick = { playerViewModel.togglePlayPause() },
                    onTrackClick = {
                        navController.navigate("now_playing/${track.id}")
                    },
                    onAddClick = { /* TODO */ },
                    onLikeClick = { isLiked = !isLiked },
                    isLiked = isLiked,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Bottom Navigation Bar - всегда внизу
            BottomNavigationBar(
                currentRoute = currentRoute,
                onNavigate = onNavigate,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
