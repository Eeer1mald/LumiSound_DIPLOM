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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.lumisound.feature.home.components.BottomNavigationBar
import com.example.lumisound.feature.home.components.MiniPlayer
import com.example.lumisound.feature.nowplaying.PlayerScreen
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
    
    // Состояние для анимации разворачивания мини-плеера в полноэкранный плеер
    var rawAnimationProgress by remember { mutableStateOf(0f) }
    var isDraggingMiniPlayer by remember { mutableStateOf(false) }
    var lastDragVelocity by remember { mutableStateOf(0f) } // Скорость для инерции
    
    // Целевой прогресс для анимации после отпускания пальца
    var targetAnimationProgress by remember { mutableStateOf(0f) }
    
    // Плавная анимация прогресса с помощью animateFloatAsState
    // Во время свайпа используем rawAnimationProgress напрямую, после - анимируем к targetAnimationProgress
    val animatedProgress by animateFloatAsState(
        targetValue = if (isDraggingMiniPlayer) rawAnimationProgress else targetAnimationProgress,
        animationSpec = tween(
            durationMillis = if (isDraggingMiniPlayer) 0 else 300,
            easing = FastOutSlowInEasing
        ),
        label = "miniPlayerAnimation"
    )
    
    // Используем анимированный прогресс для отображения
    val animationProgress = if (isDraggingMiniPlayer) rawAnimationProgress else animatedProgress
    
    // Проверяем, находимся ли мы уже на экране плеера
    val isOnPlayerScreen = remember {
        androidx.compose.runtime.derivedStateOf {
            navController.currentBackStackEntry?.destination?.route?.startsWith("now_playing") == true
        }
    }.value
    
    // Вычисляем высоту экрана для движения плеера снизу вверх
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    // Высота мини-плеера + отступы + примерная высота нижней навигации
    // Это нужно, чтобы верхняя грань полноэкранного плеера "прилипала" к месту,
    // где находится мини-плеер, а не к пространству выше него
    val miniPlayerTotalHeightPx = remember(density) {
        with(density) {
            // 72.dp высота мини-плеера + 16.dp вертикальные отступы +
            // ~56.dp высота нижнего бара (типичное значение)
            (72.dp + 16.dp + 56.dp).toPx()
        }
    }
    
    // Начальное положение верхней границы плеера — прямо над мини-плеером
    val initialPlayerTopY = (screenHeightPx - miniPlayerTotalHeightPx).coerceAtLeast(0f)
    
    // Смещение плеера: при progress = 0 верхняя грань в initialPlayerTopY,
    // при progress = 1 — в самом верху (0). Движение выглядит "привязанным" к пальцу.
    val playerOffsetY = remember(animationProgress, initialPlayerTopY) {
        val t = animationProgress.coerceIn(0f, 1f)
        initialPlayerTopY * (1f - t)
    }
    
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
        // КРИТИЧНО: используем стабильную переменную, чтобы мини-плеер не пропадал
        // Мини-плеер должен показываться ВСЕГДА, когда трек был запущен хотя бы раз
        // (кроме экрана самого плеера - там он скрывается через animationProgress)
        val trackForMiniPlayer = currentTrack
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            // ВСЕГДА показываем мини-плеер, если есть трек в PlayerStateHolder
            // Это гарантирует, что мини-плеер не пропадет после нескольких выходов из плеера
            // Проверяем напрямую из playerStateHolder для надежности
            val shouldShowMiniPlayer = trackForMiniPlayer != null
            if (shouldShowMiniPlayer) {
                MiniPlayer(
                    currentTrack = trackForMiniPlayer, // Используем стабильную переменную
                    isPlaying = isPlaying,
                    progress = progress,
                    onPlayPauseClick = { playerViewModel.togglePlayPause() },
                    onTrackClick = {
                        // Открываем плеер при клике (если не происходит анимация)
                        if (animationProgress == 0f && !isDraggingMiniPlayer) {
                            // Сохраняем, с какого экрана открываем плеер, ДО навигации
                            // в savedStateHandle текущего entry, чтобы после навигации
                            // now_playing мог его прочитать из previousBackStackEntry
                            navController.currentBackStackEntry
                                ?.savedStateHandle
                                ?.set("playerSourceRoute", localCurrentRoute)
                            
                            navController.navigate("now_playing/${trackForMiniPlayer.id}") {
                                launchSingleTop = true
                            }
                        }
                    },
                    // TODO: Реализовать добавление трека в плейлист
                    onAddClick = { /* TODO: Add to playlist functionality */ },
                    onLikeClick = { isLiked = !isLiked },
                    isLiked = isLiked,
                    // Логика анимации разворачивания мини-плеера в полноэкранный плеер
                    // animationProgress обновляется в реальном времени при свайпе
                    animationProgress = animationProgress,
                    onAnimationProgressChange = { progress ->
                        // Во время свайпа обновляем напрямую для отзывчивости
                        rawAnimationProgress = progress.coerceIn(0f, 1f)
                    },
                    onDragStart = { 
                        isDraggingMiniPlayer = true
                        lastDragVelocity = 0f
                    },
                    onDragVelocityChange = { velocity ->
                        lastDragVelocity = velocity
                    },
                    onDragEnd = {
                        val currentProgress = rawAnimationProgress
                        val velocity = lastDragVelocity
                        isDraggingMiniPlayer = false
                        
                        scope.launch {
                            var finalProgress = currentProgress
                            
                            // Если была скорость (быстрый свайп), применяем инерцию
                            // Порог значительно снижен для легкого "заброса" мини-плеера
                            // Теперь даже небольшое быстрое движение может открыть плеер
                            if (velocity > 15f && currentProgress > 0.02f) {
                                // Физически более правильный расчет инерции
                                // Используем экспоненциальное затухание для естественного движения
                                
                                // Константы для физически правильной инерции
                                // Уменьшено трение для более легкого "заброса"
                                val baseFriction = 0.97f // Базовое трение (выше = меньше замедление, увеличено для легкости)
                                val dampingFactor = 0.92f // Демпфирование (увеличено для более плавного движения)
                                
                                // Начальная скорость в единицах прогресса за кадр (нормализуем)
                                // velocity уже в пикселях/мс, конвертируем в прогресс/мс
                                val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }
                                val normalizedVelocity = velocity / screenHeight // прогресс за миллисекунду
                                
                                // Применяем инерцию с физически правильным затуханием
                                var progress = currentProgress
                                // Увеличиваем начальную скорость для более сильной инерции
                                // Умножаем на 1.5 для более легкого "заброса"
                                var currentVelocity = normalizedVelocity * 16f * 1.5f
                                
                                // Адаптивное затухание: чем выше скорость, тем сильнее инерция
                                // Увеличиваем влияние скорости для более выраженной инерции
                                val initialSpeedFactor = (velocity / 80f).coerceIn(0.3f, 3.0f)
                                val adaptiveFriction = baseFriction * (1f + (initialSpeedFactor - 1f) * 0.15f).coerceIn(0.85f, 1.0f)
                                
                                val minVelocity = 0.0003f // Минимальная скорость для остановки (снижена для более длинной инерции)
                                val frameTime = 16L // 60 FPS
                                
                                // Физически правильное затухание: экспоненциальное с учетом трения
                                // Используем более плавное затухание для естественного движения
                                while (progress < 1f && abs(currentVelocity) > minVelocity) {
                                    // Применяем трение и демпфирование с адаптивным коэффициентом
                                    // Более плавное затухание для естественного движения
                                    currentVelocity *= (adaptiveFriction * dampingFactor)
                                    
                                    // Обновляем позицию на основе скорости
                                    val step = currentVelocity
                                    progress = (progress + step).coerceIn(0f, 1f)
                                    rawAnimationProgress = progress
                                    
                                    delay(frameTime) // ~60 FPS для плавности
                                    
                                    // Если достигли верха, останавливаемся
                                    if (progress >= 1f) {
                                        rawAnimationProgress = 1f
                                        break
                                    }
                                    
                                    // Если скорость стала слишком малой, останавливаемся
                                    if (abs(currentVelocity) < minVelocity) {
                                        break
                                    }
                                }
                                
                                // Финальный прогресс после инерции
                                finalProgress = progress.coerceIn(0f, 1f)
                                rawAnimationProgress = finalProgress
                            }
                            
                            // Решаем: открывать плеер или возвращать мини-плеер
                            if (finalProgress >= 0.5f) {
                                // Завершаем анимацию до полного открытия
                                targetAnimationProgress = 1f
                                delay(350) // Ждем завершения анимации
                                // Навигируем к плееру для фиксации состояния
                                navController.navigate("now_playing/${trackForMiniPlayer.id}") {
                                    launchSingleTop = true
                                }
                                // Сохраняем, с какого экрана открыли плеер, ПОСЛЕ навигации
                                navController.currentBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("playerSourceRoute", localCurrentRoute)
                                delay(100)
                                targetAnimationProgress = 0f
                                rawAnimationProgress = 0f
                            } else {
                                // Возвращаем мини-плеер обратно
                                targetAnimationProgress = 0f
                                delay(350)
                                rawAnimationProgress = 0f
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
        
        // Полноэкранный плеер как overlay - выплывает снизу вверх при свайпе мини-плеера
        // Показываем только если есть трек, идет анимация открытия (progress > 0) и мы НЕ на экране плеера
        if (trackForMiniPlayer != null && animationProgress > 0f && !isOnPlayerScreen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = with(density) { playerOffsetY.toDp() }) // Движется снизу вверх
                    .alpha(animationProgress) // Также появляется из прозрачности для плавности
            ) {
                PlayerScreen(
                    track = trackForMiniPlayer,
                    onClose = {
                        // При закрытии сбрасываем анимацию
                        targetAnimationProgress = 0f
                        rawAnimationProgress = 0f
                        // Возвращаемся на предыдущую страницу
                        navController.popBackStack()
                    },
                    viewModel = playerViewModel
                )
            }
        }
    }
}
