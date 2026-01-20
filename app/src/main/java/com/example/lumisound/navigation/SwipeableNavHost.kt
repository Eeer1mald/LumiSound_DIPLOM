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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.lumisound.feature.home.components.BottomNavigationBar
import com.example.lumisound.feature.home.components.MiniPlayer
import com.example.lumisound.feature.nowplaying.AnimatedPlayerSheet
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
    // Используем saveableState для сохранения состояния при пересоздании
    val pagerState = rememberPagerState(pageCount = { routes.size }, initialPage = currentIndex)
    
    // Coroutine scope для вызова suspend функций при нажатии на bottom nav
    val scope = rememberCoroutineScope()
    
    // Локальное состояние для отображения активной вкладки в bottom nav
    // Используем rememberSaveable для сохранения состояния при пересоздании
    var localCurrentRoute by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(currentRoute) }
    
    // Инициализация: синхронизируем localCurrentRoute с начальным currentRoute
    LaunchedEffect(Unit) {
        localCurrentRoute = currentRoute
    }
    
    // Оптимизированная синхронизация локального состояния при свайпе в pager
    // Используем snapshotFlow с distinctUntilChanged для минимальных обновлений
    // Обновляем ТОЛЬКО локальное состояние для bottom nav, НЕ вызываем onNavigate
    // Это предотвращает пересоздание страниц и обеспечивает плавность на 120Hz
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged() // Обновляем только при реальном изменении страницы
            .collect { page ->
                val newRoute = routes[page]
                // Обновляем только локальное состояние для bottom nav
                // НЕ вызываем onNavigate, чтобы страницы не пересоздавались
                if (newRoute != localCurrentRoute) {
                    localCurrentRoute = newRoute
                }
            }
    }
    
    // Player state - используем derivedStateOf для оптимизации производительности
    val currentTrack by playerStateHolder.currentTrack.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val currentPosition by playerViewModel.currentPosition.collectAsState()
    val duration by playerViewModel.duration.collectAsState()
    
    // Используем derivedStateOf для уменьшения рекомпозиций (вычисляется только при изменении зависимостей)
    val progress = androidx.compose.runtime.derivedStateOf {
        if (duration > 0 && currentTrack != null) {
            (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        } else 0f
    }.value
    
    var isLiked by remember { mutableStateOf(false) }
    
    // Состояние для показа анимированного плеера
    var showPlayerSheet by remember { mutableStateOf(false) }
    
    // Состояние для прогресса анимации перехода (0.0 = мини-плеер, 1.0 = полноценный плеер)
    var animationProgress by remember { mutableStateOf(0f) }
    var isDraggingPlayer by remember { mutableStateOf(false) }
    
    // Отслеживаем навигацию к плееру через navController
    LaunchedEffect(navController) {
        snapshotFlow { navController.currentBackStackEntry?.destination?.route }
            .distinctUntilChanged() // Только при реальном изменении маршрута
            .collect { route ->
                val shouldShow = route?.startsWith("now_playing") == true && currentTrack != null
                if (showPlayerSheet != shouldShow) {
                    showPlayerSheet = shouldShow
                }
                if (shouldShow && animationProgress < 1f) {
                    // Автоматически анимируем открытие при навигации
                    animationProgress = 1f
                }
                // НЕ сбрасываем animationProgress при закрытии - пусть AnimatedPlayerSheet сам управляет
                // Это предотвращает обновление экрана при закрытии
            }
    }
    
    // ОТКЛЮЧЕНО ДЛЯ TESTPLAYERSCREEN - TestPlayerScreen сам управляет закрытием через onClose()
    // Эта логика была для AnimatedPlayerSheet, который сейчас отключен
    // var wasInPlayer by remember { mutableStateOf(false) }
    // 
    // LaunchedEffect(navController.currentBackStackEntry?.destination?.route) {
    //     val currentRoute = navController.currentBackStackEntry?.destination?.route
    //     wasInPlayer = currentRoute?.startsWith("now_playing") == true
    // }
    // 
    // LaunchedEffect(animationProgress, showPlayerSheet, isDraggingPlayer, wasInPlayer) {
    //     val currentRoute = navController.currentBackStackEntry?.destination?.route
    //     val isInPlayerRoute = currentRoute?.startsWith("now_playing") == true
    //     if (animationProgress == 0f && !showPlayerSheet && !isDraggingPlayer && isInPlayerRoute && wasInPlayer) {
    //         delay(100)
    //         val stillInPlayer = navController.currentBackStackEntry?.destination?.route?.startsWith("now_playing") == true
    //         if (animationProgress == 0f && !showPlayerSheet && stillInPlayer) {
    //             wasInPlayer = false
    //             navController.popBackStack()
    //         }
    //     }
    // }
    
    // Фон для всего контейнера, чтобы предотвратить белое мерцание
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
    ) {
        // Карусель из экранов - как в галерее
        // Оптимизировано для плавной работы на 120Hz дисплеях
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds(),
            userScrollEnabled = true,
            pageSize = PageSize.Fill, // Оптимизация рендеринга для лучшей производительности
            key = { page -> routes[page] } // Ключ для предотвращения пересоздания экранов
        ) { page ->
            // Используем remember для стабильного route, чтобы минимизировать рекомпозиции
            // Важно: используем page как ключ для remember, чтобы каждому экрану соответствовал свой route
            val route = remember(page, routes) { routes[page] }
            // Обертываем каждый экран в Box с фоном для предотвращения мерцания
            // КРИТИЧЕСКАЯ ОПТИМИЗАЦИЯ для 120Hz: используем graphicsLayer для кеширования экранов
            // Это позволяет GPU кешировать содержимое страницы и не перерисовывать его при свайпе
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorBackground)
                    .graphicsLayer {
                        // Кешируем графический слой для ускорения рендеринга при свайпе
                        // compositingStrategy автоматически оптимизирует рендеринг для 120Hz
                        compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.ModulateAlpha
                    }
            ) {
                ScreenContent(
                    route = route,
                    navController = navController,
                    userName = userName
                )
            }
        }
        
        // Mini Player - показываем только если есть текущий трек, над нижней панелью
        // Важно: используем отдельную переменную для стабильности, чтобы мини-плеер не пропадал
        val trackForMiniPlayer = currentTrack
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            // Всегда показываем мини-плеер, если есть трек (проверяем явно на null для стабильности)
            if (trackForMiniPlayer != null) {
                MiniPlayer(
                    currentTrack = trackForMiniPlayer, // Используем стабильную переменную
                    isPlaying = isPlaying,
                    progress = progress,
                    onPlayPauseClick = { playerViewModel.togglePlayPause() },
                    onTrackClick = {
                        // При клике сохраняем текущий маршрут перед навигацией к плееру
                        // Это позволит правильно вернуться на ту же вкладку после закрытия плеера
                        val currentRouteForPlayer = localCurrentRoute
                        showPlayerSheet = true
                        animationProgress = 1f
                        navController.navigate("now_playing/${trackForMiniPlayer.id}") {
                            // Сохраняем текущий маршрут в savedStateHandle плеера
                            launchSingleTop = true
                        }
                        // Сохраняем маршрут в savedStateHandle для последующего использования
                        // Делаем это после навигации, когда backStackEntry будет доступен
                    },
                    // TODO: Реализовать добавление трека в плейлист
                    onAddClick = { /* TODO: Add to playlist functionality */ },
                    onLikeClick = { isLiked = !isLiked },
                    isLiked = isLiked,
                    animationProgress = animationProgress,
                    onAnimationProgressChange = { progress ->
                        animationProgress = progress
                        // Автоматически открываем плеер при достижении 100%
                        if (progress >= 1f && !showPlayerSheet) {
                            navController.navigate("now_playing/${trackForMiniPlayer.id}")
                        }
                    },
                    onDragStart = { isDraggingPlayer = true },
                    onDragEnd = { 
                        isDraggingPlayer = false
                        if (animationProgress < 0.3f) {
                            // Закрываем, если свайпнули меньше 30%
                            animationProgress = 0f
                            if (showPlayerSheet) {
                                showPlayerSheet = false
                                navController.popBackStack()
                            }
                        } else {
                            // Открываем полностью, если свайпнули больше 30%
                            animationProgress = 1f
                            if (!showPlayerSheet) {
                                navController.navigate("now_playing/${trackForMiniPlayer.id}")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Bottom Navigation Bar - всегда внизу
            // Используем localCurrentRoute для отображения активной вкладки
            // При нажатии анимируем pager вместо вызова onNavigate (предотвращает пересоздание страниц)
            BottomNavigationBar(
                currentRoute = localCurrentRoute,
                onNavigate = { route ->
                    // Анимируем переход в pager вместо вызова onNavigate
                    // Это предотвращает пересоздание страниц и обеспечивает плавную анимацию свайпа
                    val targetIndex = routes.indexOf(route).coerceIn(0, routes.size - 1)
                    if (targetIndex != pagerState.currentPage) {
                        scope.launch {
                            pagerState.animateScrollToPage(targetIndex)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Анимированный плеер поверх всего контента
        // ОТКЛЮЧЕНО ДЛЯ ТЕСТИРОВАНИЯ - используем простую тестовую страницу
        /*
        if (track != null) {
            AnimatedPlayerSheet(
                track = track,
                isVisible = showPlayerSheet || animationProgress > 0f,
                animationProgress = animationProgress,
                onAnimationProgressChange = { progress ->
                    animationProgress = progress
                },
                onDismiss = {
                    // Закрываем навигацию только после завершения анимации
                    // Сначала обновляем состояние, потом закрываем навигацию с задержкой
                    animationProgress = 0f
                    showPlayerSheet = false
                },
                onDragStart = { isDraggingPlayer = true },
                onDragEnd = {
                    isDraggingPlayer = false
                    if (animationProgress < 0.3f) {
                        // Закрываем, если свайпнули вниз меньше 30%
                        animationProgress = 0f
                    } else {
                        // Возвращаем в открытое состояние
                        animationProgress = 1f
                    }
                },
                onArtistClick = { artistName, artistImageUrl ->
                    animationProgress = 0f
                    showPlayerSheet = false
                    navController.navigate("artist/${artistName}/${artistImageUrl ?: ""}")
                },
                viewModel = playerViewModel
            )
        }
        */
    }
}
