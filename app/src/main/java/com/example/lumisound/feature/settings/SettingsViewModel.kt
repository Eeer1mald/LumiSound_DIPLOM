package com.example.lumisound.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lumisound.data.local.SessionManager
import com.example.lumisound.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isLoading: Boolean = false,
    val isProfilePublic: Boolean = false,
    val autoplayEnabled: Boolean = true,
    val showFloatingComments: Boolean = true,
    val normalizeVolume: Boolean = false,
    val themeMode: String = "dark",
    val userEmail: String = "",
    // Эквалайзер
    val equalizerEnabled: Boolean = false,
    val equalizerBands: List<com.example.lumisound.data.player.EqualizerBand> = emptyList(),
    val equalizerPresets: List<String> = emptyList(),
    val selectedPreset: Int = -1,
    // Виртуализатор
    val virtualizerEnabled: Boolean = false,
    // Скорость воспроизведения
    val playbackSpeed: Float = 1f,
    // Таймер сна
    val sleepTimerActive: Boolean = false,
    val sleepTimerMinutes: Int = 30,
    val successMessage: String? = null,
    val error: String? = null,
    // Смена пароля
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isChangingPassword: Boolean = false,
    val passwordError: String? = null,
    // Кэш
    val isClearingCache: Boolean = false,
    val cacheCleared: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    private val playerStateHolder: com.example.lumisound.data.player.PlayerStateHolder,
    private val audioPlayerService: com.example.lumisound.data.player.AudioPlayerService,
    private val themeManager: com.example.lumisound.ui.theme.ThemeManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    // SharedPreferences для локальных настроек
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val autoplay = prefs.getBoolean("autoplay", true)
        val speed = sessionManager.getFloat("playback_speed", 1f)
        _state.value = _state.value.copy(
            autoplayEnabled = autoplay,
            isProfilePublic = prefs.getBoolean("profile_public", false),
            showFloatingComments = sessionManager.getShowFloatingComments(),
            normalizeVolume = sessionManager.getNormalizeVolume(),
            themeMode = sessionManager.getThemeMode(),
            userEmail = sessionManager.getEmail() ?: "",
            equalizerEnabled = sessionManager.getBoolean("eq_enabled", false),
            virtualizerEnabled = sessionManager.getBoolean("virtualizer_enabled", false),
            playbackSpeed = speed
        )
        playerStateHolder.autoplayEnabled = autoplay
        playerStateHolder.showFloatingComments = sessionManager.getShowFloatingComments()
        audioPlayerService.setNormalizeVolume(sessionManager.getNormalizeVolume())
        audioPlayerService.setPlaybackSpeed(speed)
        if (sessionManager.getBoolean("eq_enabled", false)) {
            audioPlayerService.setEqualizerEnabled(true)
        }
        if (sessionManager.getBoolean("virtualizer_enabled", false)) {
            audioPlayerService.setVirtualizerEnabled(true)
        }
    }

    fun setAutoplay(enabled: Boolean) {
        prefs.edit().putBoolean("autoplay", enabled).apply()
        playerStateHolder.autoplayEnabled = enabled
        _state.value = _state.value.copy(autoplayEnabled = enabled)
    }

    fun setShowFloatingComments(enabled: Boolean) {
        sessionManager.saveShowFloatingComments(enabled)
        playerStateHolder.showFloatingComments = enabled
        _state.value = _state.value.copy(showFloatingComments = enabled)
    }

    fun setNormalizeVolume(enabled: Boolean) {
        sessionManager.saveNormalizeVolume(enabled)
        audioPlayerService.setNormalizeVolume(enabled)
        _state.value = _state.value.copy(normalizeVolume = enabled)
    }

    // ── Эквалайзер ────────────────────────────────────────────────────────

    fun setEqualizerEnabled(enabled: Boolean) {
        sessionManager.saveBoolean("eq_enabled", enabled)
        audioPlayerService.setEqualizerEnabled(enabled)
        val bands = if (enabled) audioPlayerService.getEqualizerBands() else _state.value.equalizerBands
        val presets = audioPlayerService.getEqualizerPresets()
        _state.value = _state.value.copy(
            equalizerEnabled = enabled,
            equalizerBands = bands,
            equalizerPresets = presets
        )
    }

    fun loadEqualizerBands() {
        if (!_state.value.equalizerEnabled) return
        val bands = audioPlayerService.getEqualizerBands()
        val presets = audioPlayerService.getEqualizerPresets()
        _state.value = _state.value.copy(equalizerBands = bands, equalizerPresets = presets)
    }

    fun setEqualizerBandLevel(bandIndex: Int, levelMilliBel: Int) {
        audioPlayerService.setEqualizerBandLevel(bandIndex, levelMilliBel)
        val updatedBands = _state.value.equalizerBands.map {
            if (it.index == bandIndex) it.copy(levelMilliBel = levelMilliBel) else it
        }
        _state.value = _state.value.copy(equalizerBands = updatedBands, selectedPreset = -1)
    }

    fun applyEqualizerPreset(presetIndex: Int) {
        val presetName = _state.value.equalizerPresets.getOrNull(presetIndex)?.lowercase() ?: ""
        // Flat/Normal — сбрасываем все полосы в 0 вручную
        if (presetName.contains("flat") || presetName.contains("normal")) {
            val bands = _state.value.equalizerBands
            bands.forEach { band ->
                audioPlayerService.setEqualizerBandLevel(band.index, 0)
            }
            val zeroBands = bands.map { it.copy(levelMilliBel = 0) }
            _state.value = _state.value.copy(equalizerBands = zeroBands, selectedPreset = presetIndex)
        } else {
            audioPlayerService.applyEqualizerPreset(presetIndex)
            val bands = audioPlayerService.getEqualizerBands()
            _state.value = _state.value.copy(equalizerBands = bands, selectedPreset = presetIndex)
        }
    }

    // ── Виртуализатор ─────────────────────────────────────────────────────

    fun setVirtualizer(enabled: Boolean) {
        sessionManager.saveBoolean("virtualizer_enabled", enabled)
        audioPlayerService.setVirtualizerEnabled(enabled)
        _state.value = _state.value.copy(virtualizerEnabled = enabled)
    }

    // ── Скорость воспроизведения ──────────────────────────────────────────

    fun setPlaybackSpeed(speed: Float) {
        sessionManager.saveFloat("playback_speed", speed)
        audioPlayerService.setPlaybackSpeed(speed)
        _state.value = _state.value.copy(playbackSpeed = speed)
    }

    // ── Таймер сна ────────────────────────────────────────────────────────

    fun setSleepTimerMinutes(minutes: Int) {
        _state.value = _state.value.copy(sleepTimerMinutes = minutes)
    }

    fun startSleepTimer() {
        val minutes = _state.value.sleepTimerMinutes
        audioPlayerService.startSleepTimer(minutes)
        audioPlayerService.onSleepTimerFinished = {
            _state.value = _state.value.copy(sleepTimerActive = false)
        }
        _state.value = _state.value.copy(sleepTimerActive = true, successMessage = "Таймер сна: $minutes мин")
    }

    fun cancelSleepTimer() {
        audioPlayerService.cancelSleepTimer()
        _state.value = _state.value.copy(sleepTimerActive = false)
    }

    fun setThemeMode(mode: String) {
        sessionManager.saveThemeMode(mode)
        themeManager.setTheme(mode)
        _state.value = _state.value.copy(themeMode = mode)
    }

    fun setProfilePublic(isPublic: Boolean) {
        val token = sessionManager.getAccessToken() ?: return
        _state.value = _state.value.copy(isLoading = true)
        viewModelScope.launch {
            authRepository.updateProfileVisibility(token, isPublic)
                .onSuccess {
                    prefs.edit().putBoolean("profile_public", isPublic).apply()
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isProfilePublic = isPublic,
                        successMessage = if (isPublic) "Профиль теперь публичный" else "Профиль теперь приватный"
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "Ошибка обновления"
                    )
                }
        }
    }

    fun setNewPassword(value: String) {
        _state.value = _state.value.copy(newPassword = value, passwordError = null)
    }

    fun setConfirmPassword(value: String) {
        _state.value = _state.value.copy(confirmPassword = value, passwordError = null)
    }

    fun changePassword() {
        val s = _state.value
        if (s.newPassword.length < 8) {
            _state.value = s.copy(passwordError = "Минимум 8 символов")
            return
        }
        if (s.newPassword != s.confirmPassword) {
            _state.value = s.copy(passwordError = "Пароли не совпадают")
            return
        }
        val token = sessionManager.getAccessToken() ?: return
        _state.value = s.copy(isChangingPassword = true, passwordError = null)
        viewModelScope.launch {
            authRepository.changePassword(token, s.newPassword)
                .onSuccess {
                    _state.value = _state.value.copy(
                        isChangingPassword = false,
                        newPassword = "",
                        confirmPassword = "",
                        successMessage = "Пароль успешно изменён"
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isChangingPassword = false,
                        passwordError = e.message ?: "Ошибка смены пароля"
                    )
                }
        }
    }

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun clearCache() {
        _state.value = _state.value.copy(isClearingCache = true)
        viewModelScope.launch {
            // Очищаем Coil image cache
            try {
                val imageLoader = coil.ImageLoader(context)
                imageLoader.memoryCache?.clear()
                imageLoader.diskCache?.clear()
            } catch (e: Exception) {
                android.util.Log.w("SettingsVM", "Coil cache clear: ${e.message}")
            }
            _state.value = _state.value.copy(
                isClearingCache = false,
                cacheCleared = true,
                successMessage = "Кэш очищен"
            )
        }
    }

    fun logout() {
        sessionManager.clear()
    }

    fun clearMessages() {
        _state.value = _state.value.copy(successMessage = null, error = null, cacheCleared = false)
    }
}
