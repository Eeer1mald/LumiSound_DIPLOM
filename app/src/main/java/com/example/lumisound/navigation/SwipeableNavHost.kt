package com.example.lumisound.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.draw.clipToBounds
import com.example.lumisound.ui.theme.ColorBackground
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
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
    // Используем remember для routes, чтобы не создавать список при каждой рекомпозиции
    val routes = remember { listOf("home", "search", "ratings", "profile") }
    val currentIndex = remember(currentRoute) { 
        routes.indexOf(currentRoute).coerceIn(0, routes.size - 1) 
    }
    
    // Pager state - синхронизируется с currentRoute для поддержки swipe navigation
    val pagerState = rememberPagerState(pageCount = { routes.size }, initialPage = currentIndex)
    
    // Локальное состояние для отображения активной вкладки в bottom nav
    // Обновляется при свайпе, но НЕ вызывает пересоздание страниц
    var localCurrentRoute by remember { mutableStateOf(currentRoute) }
    
    // Синхронизация pagerState с currentRoute при программной навигации (например, через bottom nav)
    // Когда пользователь нажимает на вкладку, pager должен переключиться на соответствующую страницу
    LaunchedEffect(currentRoute) {
        val newIndex = routes.indexOf(currentRoute).coerceIn(0, routes.size - 1)
        // Обновляем локальное состояние при изменении currentRoute извне
        localCurrentRoute = currentRoute
        if (pagerState.currentPage != newIndex) {
            // Используем animateScrollToPage для плавного переключения при нажатии на bottom nav
            pagerState.animateScrollToPage(newIndex)
        }
    }
    
    // Синхронизация локального состояния при свайпе в pager
    // Обновляем ТОЛЬКО локальное состояние для bottom nav, НЕ вызываем onNavigate
    // Это предотвращает пересоздание страниц и мерцание
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                val newRoute = routes[page]
                // Обновляем только локальное состояние для bottom nav
                // НЕ вызываем onNavigate, чтобы страницы не пересоздавались
                if (newRoute != localCurrentRoute) {
                    localCurrentRoute = newRoute
                }
            }
    }
    
    // Player state - используем remember для минимизации рекомпозиций
    val currentTrack by playerStateHolder.currentTrack.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val currentPosition by playerViewModel.currentPosition.collectAsState()
    val duration by playerViewModel.duration.collectAsState()
    
    // Используем remember для вычисления progress, чтобы минимизировать рекомпозиции
    val progress = remember(currentPosition, duration, currentTrack) {
        if (duration > 0 && currentTrack != null) {
            (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        } else 0f
    }
    
    var isLiked by remember { mutableStateOf(false) }
    
    // Фон для всего контейнера, чтобы предотвратить белое мерцание
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
    ) {
        // Карусель из экранов - как в галерее
        // clipToBounds предотвращает появление белых полосок при свайпе
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds(),
            userScrollEnabled = true,
            key = { page -> routes[page] } // Ключ для предотвращения пересоздания экранов
        ) { page ->
            // Используем remember для стабильного route, чтобы минимизировать рекомпозиции
            val route = remember(page) { routes[page] }
            // Обертываем каждый экран в Box с фоном для предотвращения мерцания
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorBackground)
            ) {
                ScreenContent(
                    route = route,
                    navController = navController,
                    userName = userName
                )
            }
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
                    // TODO: Реализовать добавление трека в плейлист
                    onAddClick = { /* TODO: Add to playlist functionality */ },
                    onLikeClick = { isLiked = !isLiked },
                    isLiked = isLiked,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Bottom Navigation Bar - всегда внизу
            // Используем localCurrentRoute для отображения активной вкладки
            // onNavigate вызывается только при нажатии на bottom nav, не при свайпе
            BottomNavigationBar(
                currentRoute = localCurrentRoute,
                onNavigate = { route ->
                    // При нажатии на bottom nav вызываем onNavigate для обновления Navigation Compose
                    onNavigate(route)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
