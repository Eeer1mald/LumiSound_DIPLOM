package com.example.lumisound.feature.nowplaying

import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.lumisound.data.model.Track
import com.example.lumisound.ui.theme.ColorBackground
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun AnimatedPlayerSheet(
    track: Track?,
    isVisible: Boolean,
    animationProgress: Float = 0f, // Прогресс анимации от 0 до 1
    onAnimationProgressChange: (Float) -> Unit = {},
    onDismiss: () -> Unit,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onArtistClick: (String, String?) -> Unit = { _, _ -> },
    viewModel: PlayerViewModel
) {
    if (track == null) {
        return
    }

    val view = LocalView.current
    val context = LocalContext.current
    
    // КРИТИЧНО: Явно устанавливаем настройки status bar при каждом показе плеера
    // Material3 автоматически меняет настройки при навигации, поэтому нужно переопределять их
    // Проблема: при навигации к now_playing Material3 может установить белый фон status bar
    // Решение: принудительно устанавливаем темный/прозрачный фон и светлые иконки
    // Используем DisposableEffect для гарантированного применения при входе
    androidx.compose.runtime.DisposableEffect(isVisible, animationProgress) {
        if (!view.isInEditMode && context is Activity && (isVisible || animationProgress > 0f)) {
            val window = context.window
            val insetsController = WindowCompat.getInsetsController(window, view)
            
            // Устанавливаем прозрачный фон для status bar (чтобы не было белого фона)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            
            // Устанавливаем светлые иконки (белые) на темном фоне
            // false = светлые иконки на темном фоне (для темной темы)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
            
            // Дополнительно: убеждаемся, что система не переопределяет наши настройки
            WindowCompat.setDecorFitsSystemWindows(window, false)
            
            onDispose {
                // Оставляем настройки при выходе, чтобы не было мерцания
            }
        } else {
            onDispose { }
        }
    }
    
    // LaunchedEffect для применения при изменении видимости
    LaunchedEffect(isVisible, animationProgress) {
        if (!view.isInEditMode && context is Activity && (isVisible || animationProgress > 0f)) {
            val window = context.window
            val insetsController = WindowCompat.getInsetsController(window, view)
            
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }
    
    // SideEffect для немедленного применения при каждой рекомпозиции (самый приоритетный)
    SideEffect {
        if (!view.isInEditMode && context is Activity && (isVisible || animationProgress > 0f)) {
            val window = context.window
            val insetsController = WindowCompat.getInsetsController(window, view)
            
            // Устанавливаем прозрачный фон для status bar
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            
            // Устанавливаем светлые иконки на темном фоне
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    
    // Состояние для отслеживания позиции скролла
    var scrollPositionState by remember { mutableStateOf(0) }
    
    // Состояние для отслеживания свайпа вниз
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var startProgress by remember { mutableFloatStateOf(1f) }
    
    // Состояние для анимации при нажатии на кнопку закрытия
    var closeButtonPressed by remember { mutableStateOf(false) }
    
    // Если анимация завершена и не видим, не показываем
    // НО показываем если animationProgress > 0 (даже если isVisible еще false)
    if (animationProgress == 0f && !isVisible && !closeButtonPressed) {
        return
    }
    
    // Используем Animatable для ручного управления анимацией закрытия
    val closeAnimation = remember { Animatable(1f) }
    
    // Запускаем анимацию при нажатии на кнопку
    LaunchedEffect(closeButtonPressed) {
        if (closeButtonPressed) {
            // Начинаем анимацию от текущего прогресса до 0
            val startValue = animationProgress.coerceIn(0.1f, 1f)
            closeAnimation.snapTo(startValue)
            closeAnimation.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 400)
            )
            // После завершения анимации закрываем
            // Небольшая задержка, чтобы анимация была полностью видна
            delay(100)
            if (closeButtonPressed) {
                closeButtonPressed = false
                onDismiss()
            }
        }
    }
    
    // Обновляем прогресс во время анимации закрытия
    LaunchedEffect(closeAnimation.value) {
        if (closeButtonPressed) {
            onAnimationProgressChange(closeAnimation.value)
        }
    }
    
    // Используем текущий прогресс (либо анимированный, либо обычный)
    val currentProgress = if (closeButtonPressed) closeAnimation.value else animationProgress
    
    // Используем нелинейную функцию для более плавной анимации
    val easedProgress = remember(currentProgress) {
        val t = currentProgress.coerceIn(0f, 1f)
        1f - (1f - t) * (1f - t)
    }
    
    // Вычисляем масштаб на основе прогресса анимации
    val scale = remember(easedProgress) {
        val minScale = 0.25f
        minScale + (easedProgress * (1f - minScale))
    }
    
    // Вычисляем позицию на основе прогресса
    val startY = screenHeightPx * 0.85f // Позиция мини-плеера (снизу)
    val endY = 0f // Позиция полноценного плеера (центр)
    val baseY = remember(easedProgress, startY, endY) {
        startY - (easedProgress * (startY - endY))
    }
    
    // Добавляем dragOffset для свайпа вниз - плеер следует за пальцем
    val currentY = baseY + dragOffset
    
    // Вычисляем альфу для фона
    val backgroundAlpha = remember(easedProgress, dragOffset, screenHeightPx) {
        val baseAlpha = easedProgress * 0.8f
        val dragAlpha = (1f - (dragOffset / screenHeightPx).coerceIn(0f, 1f)) * 0.8f
        if (isDragging && dragOffset > 0) dragAlpha else baseAlpha
    }
    
    // Вычисляем альфу для самого плеера
    val playerAlpha = remember(currentProgress, dragOffset, screenHeightPx) {
        val baseAlpha = if (currentProgress < 0.1f) {
            0f
        } else {
            ((currentProgress - 0.1f) / 0.9f).coerceIn(0f, 1f)
        }
        val dragAlpha = (1f - (dragOffset / screenHeightPx).coerceIn(0f, 1f))
        if (isDragging && dragOffset > 0) dragAlpha else baseAlpha
    }
    
    // Вычисляем смещение по X для центрирования
    val offsetX = remember(scale, screenWidthPx) {
        (screenWidthPx * (1f - scale) / 2f)
    }
    
    // Сброс при изменении isVisible
    LaunchedEffect(isVisible) {
        if (!isVisible) {
            dragOffset = 0f
            isDragging = false
            closeButtonPressed = false
            closeAnimation.snapTo(1f)
        } else {
            // При открытии сбрасываем анимацию
            closeAnimation.snapTo(1f)
        }
    }
    
    // Автоматически закрываем при достижении animationProgress = 0 (только при свайпе, не при анимации кнопки)
    LaunchedEffect(animationProgress, isVisible, closeButtonPressed, isDragging) {
        if (animationProgress == 0f && isVisible && !closeButtonPressed && !isDragging) {
            delay(200)
            if (animationProgress == 0f && !closeButtonPressed && !isDragging) {
                onDismiss()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                ColorBackground.copy(alpha = backgroundAlpha)
            )
            // Обрабатываем свайп вниз на всем экране
            .pointerInput(isVisible, scrollPositionState, currentProgress) {
                if (isVisible && currentProgress > 0f) {
                    detectVerticalDragGestures(
                        onDragStart = { 
                            // Начинаем свайп только если мы в самом верху
                            if (scrollPositionState == 0) {
                                isDragging = true
                                startProgress = currentProgress
                                dragOffset = 0f
                                onDragStart()
                            }
                        },
                        onDragEnd = {
                            if (isDragging) {
                                isDragging = false
                                onDragEnd()
                                
                                // Решаем: закрыть или вернуть обратно
                                if (currentProgress < 0.3f) {
                                    // Закрываем полностью
                                    onAnimationProgressChange(0f)
                                } else {
                                    // Возвращаем в открытое состояние
                                    onAnimationProgressChange(1f)
                                }
                                dragOffset = 0f
                            }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            // Обрабатываем только свайп вниз и только когда вверху
                            if (scrollPositionState == 0 && dragAmount > 0 && isDragging) {
                                // Плеер следует за пальцем вниз
                                dragOffset = (dragOffset + dragAmount).coerceAtLeast(0f)
                                
                                // Обновляем прогресс анимации
                                val progressDelta = dragAmount / 300f
                                val newProgress = (startProgress - progressDelta).coerceIn(0f, 1f)
                                onAnimationProgressChange(newProgress)
                                
                                change.consume()
                            }
                        }
                    )
                }
            }
    ) {
        Box(    
            modifier = Modifier
                .fillMaxSize()
                .offset {
                    IntOffset(
                        x = offsetX.roundToInt(),
                        y = currentY.roundToInt()
                    )
                }
                .scale(scale)
                .alpha(playerAlpha)
        ) {
            NowPlayingScreen(
                track = track,
                onClose = {
                    // Запускаем плавную анимацию закрытия
                    // Просто устанавливаем флаг - анимация начнется автоматически
                    closeButtonPressed = true
                },
                onNavigate = {},
                onArtistClick = onArtistClick,
                viewModel = viewModel,
                onScrollStateChange = { position ->
                    scrollPositionState = position
                },
                nestedScrollConnection = null // Не используем nestedScroll
            )
        }
    }
}
