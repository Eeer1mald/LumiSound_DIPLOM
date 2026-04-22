package com.example.lumisound.feature.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.lumisound.data.player.EqualizerBand
import com.example.lumisound.ui.theme.*

@Composable
fun EqualizerScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Загружаем полосы при открытии
    LaunchedEffect(state.equalizerEnabled) {
        if (state.equalizerEnabled) viewModel.loadEqualizerBands()
    }

    val appColors = LocalAppColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.background)
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            // ── Хедер ──────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.08f), CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Назад",
                            tint = appColors.onBackground,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Эквалайзер",
                        color = appColors.onBackground,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // Переключатель вкл/выкл
                    Switch(
                        checked = state.equalizerEnabled,
                        onCheckedChange = {
                            viewModel.setEqualizerEnabled(it)
                            if (it) viewModel.loadEqualizerBands()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = GradientStart,
                            uncheckedThumbColor = appColors.secondary,
                            uncheckedTrackColor = appColors.surface
                        )
                    )
                }
            }

            // ── График EQ ───────────────────────────────────────────
            item {
                if (state.equalizerEnabled && state.equalizerBands.isNotEmpty()) {
                    EqGraph(
                        bands = state.equalizerBands,
                        onBandChange = { idx, level -> viewModel.setEqualizerBandLevel(idx, level) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                } else if (!state.equalizerEnabled) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .padding(horizontal = 16.dp)
                            .background(appColors.surface, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Включите эквалайзер чтобы настроить",
                            color = appColors.secondary,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // ── Разделитель ─────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.06f))
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── Пресеты ─────────────────────────────────────────────
            if (state.equalizerEnabled && state.equalizerPresets.isNotEmpty()) {
                itemsIndexed(state.equalizerPresets.filter { !it.lowercase().contains("flat") }) { idx, preset ->
                    val realIdx = state.equalizerPresets.indexOf(preset)
                    PresetRow(
                        name = preset,
                        isSelected = state.selectedPreset == realIdx,
                        onClick = { viewModel.applyEqualizerPreset(realIdx) }
                    )
                }
            } else if (!state.equalizerEnabled) {
                item {
                    // Показываем пресеты серыми когда EQ выключен
                    val dummyPresets = listOf(
                        "Bass Boost", "Classical", "Dance",
                        "Deep", "Electronic", "Hip-Hop", "Jazz",
                        "Latin", "Loudness", "Lounge", "Piano",
                        "Pop", "R&B", "Rock", "Small Speakers",
                        "Spoken Word", "Treble Boost", "Treble Reducer", "Vocal Booster"
                    )
                    dummyPresets.forEach { preset ->
                        PresetRow(
                            name = preset,
                            isSelected = false,
                            onClick = {},
                            dimmed = true
                        )
                    }
                }
            }
        }
    }
}

// ── График EQ с перетаскиваемыми точками ─────────────────────────────────────
// ── График EQ с перетаскиваемыми точками ─────────────────────────────────────
@Composable
private fun EqGraph(
    bands: List<EqualizerBand>,
    onBandChange: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val appColors = LocalAppColors.current

    val minLevel = bands.firstOrNull()?.minLevel ?: -1500
    val maxLevel = bands.firstOrNull()?.maxLevel ?: 1500
    val range = (maxLevel - minLevel).toFloat()

    // Локальные уровни — обновляются мгновенно при drag, без вызова ViewModel
    val localLevels = remember(bands.size) {
        bands.map { it.levelMilliBel.toFloat() }.toMutableStateList()
    }

    // Синхронизируем только когда bands меняются снаружи (пресет применён)
    // Не синхронизируем во время drag чтобы не было петли
    var isDragging by remember { mutableStateOf(false) }
    LaunchedEffect(bands) {
        if (!isDragging) {
            bands.forEachIndexed { i, b ->
                if (i < localLevels.size) localLevels[i] = b.levelMilliBel.toFloat()
            }
        }
    }

    var draggedBandIndex by remember { mutableStateOf<Int?>(null) }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(bands.size, minLevel, maxLevel) {
                    awaitPointerEventScope {
                        while (true) {
                            // Ждём первого касания
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()

                            val startX = down.position.x
                            val w = size.width.toFloat()
                            val bandCount = bands.size
                            val step = w / (bandCount - 1).coerceAtLeast(1)

                            // Фиксируем ближайшую полосу по X при касании
                            val idx = ((startX / step + 0.5f).toInt()).coerceIn(0, bandCount - 1)
                            draggedBandIndex = idx
                            isDragging = true

                            // Обрабатываем движение
                            var lastAppliedLevel = localLevels[idx].toInt()

                            do {
                                val event = awaitPointerEvent()
                                val pointer = event.changes.firstOrNull() ?: break
                                if (!pointer.pressed) break
                                pointer.consume()

                                val y = pointer.position.y
                                val h = size.height.toFloat()
                                val graphH = h - 32.dp.toPx()

                                val fraction = 1f - (y / graphH).coerceIn(0f, 1f)
                                val newLevelF = minLevel + fraction * range
                                val newLevel = newLevelF.toInt().coerceIn(minLevel, maxLevel)

                                // Обновляем UI мгновенно
                                localLevels[idx] = newLevel.toFloat()

                                // Применяем к EQ только если значение изменилось
                                if (newLevel != lastAppliedLevel) {
                                    lastAppliedLevel = newLevel
                                    onBandChange(idx, newLevel)
                                }
                            } while (true)

                            // Drag завершён
                            draggedBandIndex = null
                            isDragging = false
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val freqLabelH = 32.dp.toPx()
            val graphH = h - freqLabelH
            val bandCount = bands.size
            if (bandCount < 2) return@Canvas

            val step = w / (bandCount - 1).toFloat()

            // Вычисляем точки кривой из localLevels
            val points = localLevels.mapIndexed { i, level ->
                val x = i * step
                val fraction = (level - minLevel) / range
                val y = graphH * (1f - fraction)
                Offset(x, y)
            }

            // Заливка под кривой
            val fillPath = Path().apply {
                moveTo(points.first().x, graphH)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, graphH)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        GradientStart.copy(alpha = 0.45f),
                        GradientStart.copy(alpha = 0.04f)
                    ),
                    startY = 0f,
                    endY = graphH
                )
            )

            // Линия кривой (сглаженная)
            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 0 until points.size - 1) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val cx = (p0.x + p1.x) / 2f
                    cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                }
            }
            drawPath(
                path = linePath,
                color = GradientStart,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Вертикальные пунктирные линии сетки
            points.forEach { pt ->
                drawLine(
                    color = Color.White.copy(alpha = 0.06f),
                    start = Offset(pt.x, 0f),
                    end = Offset(pt.x, graphH),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                )
            }

            // Горизонтальная нулевая линия
            val zeroFraction = (-minLevel) / range
            val zeroY = graphH * (1f - zeroFraction)
            drawLine(
                color = Color.White.copy(alpha = 0.15f),
                start = Offset(0f, zeroY),
                end = Offset(w, zeroY),
                strokeWidth = 1.dp.toPx()
            )

            // Точки на кривой
            points.forEachIndexed { i, pt ->
                val active = draggedBandIndex == i
                drawCircle(
                    color = Color.White,
                    radius = if (active) 11.dp.toPx() else 8.dp.toPx(),
                    center = pt
                )
                drawCircle(
                    color = if (active) GradientEnd else GradientStart,
                    radius = if (active) 7.dp.toPx() else 5.dp.toPx(),
                    center = pt
                )
            }
        }

        // Подписи частот снизу
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            bands.forEach { band ->
                Text(
                    text = formatFreqLabel(band.centerFreqHz),
                    color = appColors.secondary,
                    fontSize = 10.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

private fun formatFreqLabel(hz: Int): String = when {
    hz >= 1000 -> "${hz / 1000} kHz"
    else -> "$hz Hz"
}

// ── Строка пресета ────────────────────────────────────────────────────────────
@Composable
private fun PresetRow(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    dimmed: Boolean = false
) {
    val appColors = LocalAppColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = !dimmed
            ) { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            color = if (dimmed) appColors.secondary.copy(alpha = 0.4f)
                    else if (isSelected) GradientStart
                    else appColors.onBackground,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                null,
                tint = GradientStart,
                modifier = Modifier.size(18.dp)
            )
        }
    }

    // Разделитель
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(horizontal = 20.dp)
            .background(Color.White.copy(alpha = 0.04f))
    )
}
