package com.example.lumisound.feature.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.lumisound.ui.theme.*
import com.example.lumisound.ui.theme.LocalAppColors
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showPasswordSection by remember { mutableStateOf(false) }
    var showPasswordResetDialog by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var sleepTimerRemainingText by remember { mutableStateOf("") }
    var showAppealDialog by remember { mutableStateOf(false) }
    var appealText by remember { mutableStateOf("") }
    var appealSent by remember { mutableStateOf(false) }
    var showAboutScreen by remember { mutableStateOf(false) }

    // Открываем экран эквалайзера
    if (showEqualizer) {
        EqualizerScreen(onBack = { showEqualizer = false }, viewModel = viewModel)
        return
    }

    // Открываем экран "О приложении"
    if (showAboutScreen) {
        BackHandler { showAboutScreen = false }
        AboutScreen(onBack = { showAboutScreen = false })
        return
    }

    LaunchedEffect(state.successMessage) {
        if (state.successMessage != null) {
            kotlinx.coroutines.delay(2500)
            viewModel.clearMessages()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(LocalAppColors.current.background).statusBarsPadding()) {

        // ── Фиксированный хедер ────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LocalAppColors.current.background)
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.08f), CircleShape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = LocalAppColors.current.onBackground, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text("Настройки", color = LocalAppColors.current.onBackground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 68.dp) // высота хедера
                .verticalScroll(rememberScrollState())
        ) {

            // ══════════════════════════════════════════════════════
            // БАН — показываем если пользователь забанен
            // ══════════════════════════════════════════════════════
            if (state.isBanned) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(Color(0xFFFF4D6D).copy(alpha = 0.12f), androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                        .border(1.dp, Color(0xFFFF4D6D).copy(alpha = 0.4f), androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Block, null, tint = Color(0xFFFF4D6D), modifier = Modifier.size(20.dp))
                        Text("Ваш аккаунт заблокирован", color = Color(0xFFFF4D6D), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    if (!state.banReason.isNullOrBlank()) {
                        Text("Причина: ${state.banReason}", color = Color(0xFFFF4D6D).copy(alpha = 0.8f), fontSize = 13.sp)
                    }
                    Text(
                        "Если вы считаете это ошибкой, вы можете подать апелляцию.",
                        color = LocalAppColors.current.secondary,
                        fontSize = 12.sp
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                androidx.compose.ui.graphics.Brush.linearGradient(listOf(GradientStart, GradientEnd)),
                                androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                            )
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                showAppealDialog = true
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Подать апелляцию", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Диалог апелляции
            if (showAppealDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showAppealDialog = false; appealSent = false },
                    title = { Text(if (appealSent) "Апелляция отправлена" else "Подать апелляцию", color = LocalAppColors.current.onBackground) },
                    text = {
                        if (appealSent) {
                            Text("Ваша апелляция принята. Мы рассмотрим её в ближайшее время.", color = LocalAppColors.current.secondary)
                        } else {
                            androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Опишите, почему вы считаете бан ошибочным:", color = LocalAppColors.current.secondary, fontSize = 13.sp)
                                androidx.compose.material3.OutlinedTextField(
                                    value = appealText,
                                    onValueChange = { appealText = it },
                                    placeholder = { Text("Ваше сообщение...", color = LocalAppColors.current.secondary) },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 3,
                                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GradientStart,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                        focusedTextColor = LocalAppColors.current.onBackground,
                                        unfocusedTextColor = LocalAppColors.current.onBackground,
                                        cursorColor = GradientStart
                                    )
                                )
                            }
                        }
                    },
                    confirmButton = {
                        if (appealSent) {
                            androidx.compose.material3.TextButton(onClick = { showAppealDialog = false; appealSent = false }) {
                                Text("Закрыть", color = GradientStart)
                            }
                        } else {
                            androidx.compose.material3.TextButton(
                                onClick = {
                                    if (appealText.isNotBlank()) {
                                        viewModel.submitAppeal(appealText)
                                        appealSent = true
                                    }
                                },
                                enabled = appealText.isNotBlank()
                            ) {
                                Text("Отправить", color = GradientStart)
                            }
                        }
                    },
                    dismissButton = {
                        if (!appealSent) {
                            androidx.compose.material3.TextButton(onClick = { showAppealDialog = false }) {
                                Text("Отмена", color = LocalAppColors.current.secondary)
                            }
                        }
                    },
                    containerColor = LocalAppColors.current.surface
                )
            }

            // ══════════════════════════════════════════════════════
            // АККАУНТ
            // ══════════════════════════════════════════════════════
            SettingsSection(title = "Аккаунт") {                // Email — только отображение
                SettingsInfoCard(
                    icon = Icons.Default.Email,
                    title = "Email",
                    value = state.userEmail.ifBlank { "—" }
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Смена пароля через email
                SettingsActionCard(
                    icon = Icons.Default.Lock,
                    title = "Сменить пароль",
                    subtitle = "Ссылка для смены будет отправлена на email",
                    isLoading = state.isChangingPassword,
                    onClick = { showPasswordResetDialog = true }
                )
                if (state.passwordError != null) {
                    Text(
                        state.passwordError!!,
                        color = Color(0xFFFF5C6C),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }
            }

            // ══════════════════════════════════════════════════════
            // ВНЕШНИЙ ВИД
            // ══════════════════════════════════════════════════════
            SettingsSection(title = "Внешний вид") {
                // Тема — три варианта
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(LocalAppColors.current.surface)
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).background(GradientStart.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Palette, null, tint = GradientStart, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Тема оформления", color = LocalAppColors.current.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(
                                when (state.themeMode) {
                                    "light" -> "Светлая"
                                    "system" -> "Системная"
                                    else -> "Тёмная"
                                },
                                color = LocalAppColors.current.secondary, fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("dark" to "Тёмная", "light" to "Светлая", "system" to "Системная").forEach { (mode, label) ->
                            val isSelected = state.themeMode == mode
                            Box(
                                modifier = Modifier.weight(1f)
                                    .background(
                                        if (isSelected) GradientStart.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) GradientStart.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.08f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { viewModel.setThemeMode(mode) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, color = if (isSelected) GradientStart else LocalAppColors.current.secondary, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Иконка приложения
                AppIconSection(
                    selectedIcon = state.appIcon,
                    onIconSelected = { viewModel.setAppIcon(it) }
                )
            }

            // ══════════════════════════════════════════════════════
            // ВОСПРОИЗВЕДЕНИЕ
            // ══════════════════════════════════════════════════════
            SettingsSection(title = "Воспроизведение") {
                SettingsToggleCard(
                    icon = Icons.Default.PlayArrow,
                    title = "Автовоспроизведение",
                    subtitle = "Автоматически играть следующий трек",
                    checked = state.autoplayEnabled,
                    onCheckedChange = { viewModel.setAutoplay(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggleCard(
                    icon = Icons.Default.VolumeUp,
                    title = "Нормализация громкости",
                    subtitle = "Выравнивать уровень громкости треков",
                    checked = state.normalizeVolume,
                    onCheckedChange = { viewModel.setNormalizeVolume(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Spacer(modifier = Modifier.height(8.dp))
                // Скорость воспроизведения
                SpeedCard(
                    speed = state.playbackSpeed,
                    onSpeedChange = { viewModel.setPlaybackSpeed(it) }
                )
            }

            // ══════════════════════════════════════════════════════
            // ЗВУК
            // ══════════════════════════════════════════════════════
            SettingsSection(title = "Звук") {
                // Эквалайзер — кнопка-навигатор
                SettingsActionCard(
                    icon = Icons.Default.Equalizer,
                    title = "Эквалайзер",
                    subtitle = if (state.equalizerEnabled) "Включён" else "Выключен",
                    onClick = { showEqualizer = true }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggleCard(
                    icon = Icons.Default.SurroundSound,
                    title = "Виртуализатор",
                    subtitle = "Эффект объёмного звука",
                    checked = state.virtualizerEnabled,
                    onCheckedChange = { viewModel.setVirtualizer(it) }
                )
            }

            // ══════════════════════════════════════════════════════
            // ТАЙМЕР СНА
            // ══════════════════════════════════════════════════════
            SettingsSection(title = "Таймер сна") {
                SleepTimerCard(
                    isActive = state.sleepTimerActive,
                    selectedMinutes = state.sleepTimerMinutes,
                    remainingMs = state.sleepTimerRemainingMs,
                    onMinutesChange = { viewModel.setSleepTimerMinutes(it) },
                    onStart = { viewModel.startSleepTimer() },
                    onCancel = { viewModel.cancelSleepTimer() },
                    getRemainingMs = { viewModel.getSleepTimerRemainingMs() },
                    onTimerClick = {
                        val remaining = viewModel.getSleepTimerRemainingMs()
                        if (remaining > 0L) {
                            val totalSec = remaining / 1000
                            val min = totalSec / 60
                            val sec = totalSec % 60
                            sleepTimerRemainingText = "${min}:${sec.toString().padStart(2, '0')}"
                            showSleepTimerDialog = true
                        }
                    }
                )
            }

            // ══════════════════════════════════════════════════════
            // ИНТЕРФЕЙС ПЛЕЕРА
            // ══════════════════════════════════════════════════════
            SettingsSection(title = "Интерфейс плеера") {
                SettingsToggleCard(
                    icon = Icons.Default.ChatBubbleOutline,
                    title = "Плавающие комментарии",
                    subtitle = "Показывать комментарии поверх обложки",
                    checked = state.showFloatingComments,
                    onCheckedChange = { viewModel.setShowFloatingComments(it) }
                )
            }

            // ══════════════════════════════════════════════════════
            // КОНФИДЕНЦИАЛЬНОСТЬ
            // ══════════════════════════════════════════════════════
            SettingsSection(title = "Конфиденциальность") {
                SettingsToggleCard(
                    icon = if (state.isProfilePublic) Icons.Default.Public else Icons.Default.Lock,
                    title = if (state.isProfilePublic) "Публичный профиль" else "Закрытый профиль",
                    subtitle = "Разрешить другим видеть ваш профиль",
                    checked = state.isProfilePublic,
                    onCheckedChange = { viewModel.setProfilePublic(it) },
                    isLoading = state.isLoading
                )
            }

            // ══════════════════════════════════════════════════════
            // ДАННЫЕ
            // ══════════════════════════════════════════════════════
            SettingsSection(title = "Данные") {
                SettingsActionCard(
                    icon = Icons.Default.Storage,
                    title = "Очистить кэш",
                    subtitle = "Удалить кэшированные изображения",
                    isLoading = state.isClearingCache,
                    onClick = { viewModel.clearCache() }
                )
            }

            // ══════════════════════════════════════════════════════
            // О ПРИЛОЖЕНИИ
            // ══════════════════════════════════════════════════════
            SettingsSection(title = "О приложении") {
                SettingsActionCard(
                    icon = Icons.Default.Info,
                    title = "О приложении",
                    subtitle = "Версия, политики, авторские права",
                    onClick = { showAboutScreen = true }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ══════════════════════════════════════════════════════
            // ВЫХОД
            // ══════════════════════════════════════════════════════
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFFF5C6C).copy(alpha = 0.08f))
                        .border(1.dp, Color(0xFFFF5C6C).copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showLogoutDialog = true }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).background(Color(0xFFFF5C6C).copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color(0xFFFF5C6C), modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Выйти из аккаунта", color = Color(0xFFFF5C6C), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Вы будете перенаправлены на экран входа", color = Color(0xFFFF5C6C).copy(alpha = 0.6f), fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
            // Дополнительный отступ для мини-плеера
            Spacer(modifier = Modifier.height(80.dp))
        } // закрываем внутренний Column (скролл)
    } // закрываем Box

    // Диалог подтверждения смены пароля
    if (showPasswordResetDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordResetDialog = false },
            title = { Text("Сменить пароль?", color = LocalAppColors.current.onBackground) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Ссылка для смены пароля будет отправлена на:",
                        color = LocalAppColors.current.secondary,
                        fontSize = 14.sp
                    )
                    Text(
                        state.userEmail.ifBlank { "ваш email" },
                        color = GradientStart,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Перейдите по ссылке в письме и введите новый пароль.",
                        color = LocalAppColors.current.secondary,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPasswordResetDialog = false
                        viewModel.sendPasswordResetEmail()
                    }
                ) {
                    Text("Отправить письмо", color = GradientStart, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordResetDialog = false }) {
                    Text("Отмена", color = LocalAppColors.current.secondary)
                }
            },
            containerColor = LocalAppColors.current.surface,
            titleContentColor = LocalAppColors.current.onBackground,
            textContentColor = LocalAppColors.current.secondary
        )
    }

    // Диалог успеха — письмо отправлено
    if (state.successMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearMessages() },
            title = { Text("Письмо отправлено", color = LocalAppColors.current.onBackground) },
            text = {
                Text(
                    state.successMessage!!,
                    color = LocalAppColors.current.secondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearMessages() }) {
                    Text("OK", color = GradientStart, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = LocalAppColors.current.surface,
            titleContentColor = LocalAppColors.current.onBackground
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Выйти из аккаунта?", color = LocalAppColors.current.onBackground) },
            text = { Text("Вы уверены, что хотите выйти?", color = LocalAppColors.current.secondary) },
            confirmButton = {
                TextButton(onClick = { showLogoutDialog = false; viewModel.logout(); onLogout() }) {
                    Text("Выйти", color = Color(0xFFFF5C6C), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Отмена", color = LocalAppColors.current.secondary) }
            },
            containerColor = LocalAppColors.current.surface, titleContentColor = LocalAppColors.current.onBackground, textContentColor = LocalAppColors.current.secondary
        )
    }

    if (showSleepTimerDialog) {
        // Живой отсчёт в диалоге
        var dialogRemainingText by remember { mutableStateOf(sleepTimerRemainingText) }
        LaunchedEffect(showSleepTimerDialog) {
            while (showSleepTimerDialog) {
                val ms = viewModel.getSleepTimerRemainingMs()
                if (ms <= 0L) { showSleepTimerDialog = false; break }
                val totalSec = ms / 1000
                val min = totalSec / 60
                val sec = totalSec % 60
                dialogRemainingText = "${min}:${sec.toString().padStart(2, '0')}"
                kotlinx.coroutines.delay(1000L)
            }
        }
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text("Таймер сна", color = LocalAppColors.current.onBackground) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Музыка выключится через $dialogRemainingText",
                        color = LocalAppColors.current.secondary,
                        fontSize = 14.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) {
                    Text("OK", color = GradientStart)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSleepTimerDialog = false
                    viewModel.cancelSleepTimer()
                }) {
                    Text("Отменить таймер", color = Color(0xFFFF5C6C))
                }
            },
            containerColor = LocalAppColors.current.surface
        )
    }
}

// ── Компоненты ────────────────────────────────────────────────────────────────

@Composable
private fun AppIconSection(selectedIcon: String, onIconSelected: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val icons = listOf(
        "black" to "Чёрная",
        "white" to "Белая",
        "blue"  to "Синяя",
        "pink"  to "Розовая"
    )
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(LocalAppColors.current.surface)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            icons.forEach { (iconKey, label) ->
                val isSelected = selectedIcon == iconKey
                val resId = context.resources.getIdentifier(
                    "icon_$iconKey", "drawable", context.packageName
                )
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                2.dp,
                                if (isSelected) GradientStart else Color.White.copy(alpha = 0.1f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onIconSelected(iconKey) }
                    ) {
                        if (resId != 0) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = resId),
                                contentDescription = label,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp)
                                    .size(22.dp)
                                    .background(GradientStart, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                    Text(
                        label,
                        color = if (isSelected) GradientStart else LocalAppColors.current.secondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
            // Пустые веса чтобы иконки занимали только левую часть
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(title, color = LocalAppColors.current.secondary, fontSize = 12.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp))
        content()
    }
}

@Composable
private fun SettingsToggleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isLoading: Boolean = false
) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(LocalAppColors.current.surface)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(GradientStart.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = GradientStart, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = LocalAppColors.current.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(3.dp))
                Text(subtitle, color = LocalAppColors.current.secondary, fontSize = 12.sp)
            }
            if (isLoading) {
                CircularProgressIndicator(color = GradientStart, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White, checkedTrackColor = GradientStart,
                        uncheckedThumbColor = LocalAppColors.current.secondary, uncheckedTrackColor = LocalAppColors.current.surface
                    )
                )
            }
        }
    }
}

@Composable
private fun SettingsActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(LocalAppColors.current.surface)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = !isLoading) { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(GradientStart.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = GradientStart, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = LocalAppColors.current.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(3.dp))
                Text(subtitle, color = LocalAppColors.current.secondary, fontSize = 12.sp)
            }
            if (isLoading) {
                CircularProgressIndicator(color = GradientStart, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.ChevronRight, null, tint = LocalAppColors.current.secondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun SettingsInfoCard(
    icon: ImageVector,
    title: String,
    value: String
) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(LocalAppColors.current.surface)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(GradientStart.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = GradientStart, modifier = Modifier.size(20.dp))
            }
            Text(title, color = LocalAppColors.current.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text(value, color = LocalAppColors.current.secondary, fontSize = 13.sp)
        }
    }
}

// ── Скорость воспроизведения ──────────────────────────────────────────────────
@Composable
private fun SpeedCard(speed: Float, onSpeedChange: (Float) -> Unit) {
    val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
    val labels = listOf("0.5x", "0.75x", "1x", "1.25x", "1.5x", "2.0x")
    val n = speeds.size // 6

    // Индекс — единственный источник истины
    var currentIdx by remember {
        mutableIntStateOf(
            speeds.indexOfFirst { kotlin.math.abs(it - speed) < 0.01f }.let { if (it < 0) 2 else it }
        )
    }
    LaunchedEffect(speed) {
        val idx = speeds.indexOfFirst { kotlin.math.abs(it - speed) < 0.01f }
        if (idx >= 0 && idx != currentIdx) currentIdx = idx
    }

    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(LocalAppColors.current.surface)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp)
                        .background(GradientStart.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Speed, null, tint = GradientStart, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Скорость воспроизведения", color = LocalAppColors.current.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(
                        if (speeds[currentIdx] == 1f) "Обычная (1x)" else "${speeds[currentIdx]}x",
                        color = GradientStart, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Slider по индексу 0..(n-1) с steps=(n-2) — thumb стоит РОВНО на целых позициях
            // valueRange = 0f..5f, steps = 4 → 6 дискретных позиций: 0,1,2,3,4,5
            Slider(
                value = currentIdx.toFloat(),
                onValueChange = { raw ->
                    val newIdx = raw.roundToInt().coerceIn(0, n - 1)
                    if (newIdx != currentIdx) {
                        currentIdx = newIdx
                        onSpeedChange(speeds[newIdx])
                    }
                },
                onValueChangeFinished = { onSpeedChange(speeds[currentIdx]) },
                valueRange = 0f..(n - 1).toFloat(),
                steps = n - 2, // 4 промежуточных = 6 позиций
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = GradientStart,
                    activeTrackColor = GradientStart,
                    inactiveTrackColor = Color.White.copy(alpha = 0.12f),
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                )
            )

            // Подписи — SpaceBetween с padding 10dp (компенсация thumb padding Slider)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                labels.forEachIndexed { idx, label ->
                    Text(
                        text = label,
                        color = if (idx == currentIdx) GradientStart else LocalAppColors.current.secondary,
                        fontSize = 10.sp,
                        fontWeight = if (idx == currentIdx) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
// ── Эквалайзер ────────────────────────────────────────────────────────────────
@Composable
private fun EqualizerCard(
    enabled: Boolean,
    bands: List<com.example.lumisound.data.player.EqualizerBand>,
    presets: List<String>,
    selectedPreset: Int,
    onToggle: (Boolean) -> Unit,
    onBandChange: (Int, Int) -> Unit,
    onPresetSelect: (Int) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(LocalAppColors.current.surface)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            // Заголовок с переключателем
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp).background(GradientStart.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Equalizer, null, tint = GradientStart, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Эквалайзер", color = LocalAppColors.current.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(if (enabled) "Включён" else "Выключен", color = LocalAppColors.current.secondary, fontSize = 12.sp)
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White, checkedTrackColor = GradientStart,
                        uncheckedThumbColor = LocalAppColors.current.secondary, uncheckedTrackColor = LocalAppColors.current.surface
                    )
                )
            }

            if (enabled && bands.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                // Пресеты
                if (presets.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(presets.size) { idx ->
                            val name = presets[idx]
                            val isSelected = selectedPreset == idx
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSelected) GradientStart.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(1.dp, if (isSelected) GradientStart.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onPresetSelect(idx) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(name, color = if (isSelected) GradientStart else LocalAppColors.current.secondary, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Полосы эквалайзера
                Row(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bands.forEach { band ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Значение в dB
                            Text(
                                "${band.levelMilliBel / 100}",
                                color = if (band.levelMilliBel != 0) GradientStart else LocalAppColors.current.secondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            // Вертикальный слайдер
                            androidx.compose.material3.Slider(
                                value = band.levelMilliBel.toFloat(),
                                onValueChange = { onBandChange(band.index, it.toInt()) },
                                valueRange = band.minLevel.toFloat()..band.maxLevel.toFloat(),
                                modifier = Modifier.weight(1f).graphicsLayer { rotationZ = -90f },
                                colors = SliderDefaults.colors(
                                    thumbColor = GradientStart,
                                    activeTrackColor = GradientStart,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.12f)
                                )
                            )
                            // Частота
                            Text(
                                formatFreq(band.centerFreqHz),
                                color = LocalAppColors.current.secondary,
                                fontSize = 8.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatFreq(hz: Int): String = when {
    hz >= 1000 -> "${hz / 1000}K"
    else -> "$hz"
}

// ── Таймер сна ────────────────────────────────────────────────────────────────
@Composable
private fun SleepTimerCard(
    isActive: Boolean,
    selectedMinutes: Int,
    remainingMs: Long = 0L,
    onMinutesChange: (Int) -> Unit,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onTimerClick: (() -> Unit)? = null,
    getRemainingMs: (() -> Long)? = null   // колбэк для живого обратного отсчёта
) {
    val options = listOf(5, 10, 15, 20, 30, 45, 60, 90)
    val labels = listOf("5м", "10м", "15м", "20м", "30м", "45м", "60м", "90м")
    val n = options.size

    var currentIdx by remember {
        mutableIntStateOf(
            options.indexOfFirst { it == selectedMinutes }.let { if (it < 0) 2 else it }
        )
    }
    LaunchedEffect(selectedMinutes) {
        val idx = options.indexOfFirst { it == selectedMinutes }
        if (idx >= 0 && idx != currentIdx) currentIdx = idx
    }

    // Живой обратный отсчёт — тикает каждую секунду пока таймер активен
    var liveRemainingMs by remember { mutableStateOf(remainingMs) }
    LaunchedEffect(isActive) {
        if (isActive && getRemainingMs != null) {
            while (true) {
                val ms = getRemainingMs()
                liveRemainingMs = ms
                if (ms <= 0L) break
                kotlinx.coroutines.delay(1000L)
            }
        } else {
            liveRemainingMs = remainingMs
        }
    }

    // Форматируем оставшееся время
    val remainingText = if (isActive && liveRemainingMs > 0L) {
        val totalSec = liveRemainingMs / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        "${min}:${sec.toString().padStart(2, '0')}"
    } else null

    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(LocalAppColors.current.surface)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp)
                        .background(
                            if (isActive) GradientStart.copy(alpha = 0.2f) else GradientStart.copy(alpha = 0.15f),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { if (isActive) onTimerClick?.invoke() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Bedtime, null, tint = GradientStart, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Таймер сна", color = LocalAppColors.current.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(
                        if (isActive && remainingText != null) "Осталось: $remainingText"
                        else if (isActive) "Активен"
                        else "${options[currentIdx]} мин",
                        color = GradientStart, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (!isActive) {
                Spacer(modifier = Modifier.height(16.dp))

                // Slider по индексу 0..(n-1) — точно как SpeedCard
                Slider(
                    value = currentIdx.toFloat(),
                    onValueChange = { raw ->
                        val newIdx = raw.roundToInt().coerceIn(0, n - 1)
                        if (newIdx != currentIdx) {
                            currentIdx = newIdx
                            onMinutesChange(options[newIdx])
                        }
                    },
                    onValueChangeFinished = { onMinutesChange(options[currentIdx]) },
                    valueRange = 0f..(n - 1).toFloat(),
                    steps = n - 2, // 6 промежуточных = 8 позиций
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = GradientStart,
                        activeTrackColor = GradientStart,
                        inactiveTrackColor = Color.White.copy(alpha = 0.12f),
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent
                    )
                )

                // Подписи — SpaceBetween с padding 10dp
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    labels.forEachIndexed { idx, label ->
                        Text(
                            text = label,
                            color = if (idx == currentIdx) GradientStart else LocalAppColors.current.secondary,
                            fontSize = 10.sp,
                            fontWeight = if (idx == currentIdx) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(brush = Brush.linearGradient(listOf(GradientStart, GradientEnd)), shape = RoundedCornerShape(10.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onStart() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Запустить таймер", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(Color(0xFFFF5C6C).copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFFF5C6C).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onCancel() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Отменить таймер", color = Color(0xFFFF5C6C), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
