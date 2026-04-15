package com.example.lumisound.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lumisound.data.local.SessionManager
import com.example.lumisound.data.remote.SupabaseService
import com.example.lumisound.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SynthesisUiState(
    val session: SupabaseService.SynthesisSession? = null,
    val participants: List<SupabaseService.SynthesisParticipant> = emptyList(),
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val isJoining: Boolean = false,
    val isBuildingPlaylist: Boolean = false,
    val error: String? = null,
    val inviteLink: String? = null,
    val createdPlaylistId: String? = null,
    val joinSuccess: Boolean = false
)

@HiltViewModel
class SynthesisViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(SynthesisUiState())
    val state: StateFlow<SynthesisUiState> = _state.asStateFlow()

    fun createSession(creatorUsername: String?, creatorAvatarUrl: String? = null) {
        val token = sessionManager.getAccessToken() ?: run {
            _state.value = _state.value.copy(error = "Необходимо войти в аккаунт")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isCreating = true, error = null)
            authRepository.createSynthesisSession(token, creatorUsername)
                .onSuccess { session ->
                    // Используем lumisound:// схему — она работает как deep link
                    // Для Telegram: оборачиваем в https через специальный формат
                    // intent:// URL — Android открывает приложение, Telegram показывает как ссылку
                    val code = session.inviteCode
                    val intentUrl = "intent://synthesis/$code#Intent;scheme=lumisound;package=com.example.lumisound;end"
                    // Для отображения в Telegram используем обычный lumisound:// — он кликабелен в некоторых версиях
                    // Лучший вариант — показывать код и lumisound:// ссылку
                    val link = "lumisound://synthesis/$code"
                    _state.value = _state.value.copy(
                        session = session,
                        inviteLink = link,
                        isCreating = false
                    )
                    authRepository.joinSynthesisSession(token, session.id, creatorUsername, creatorAvatarUrl)
                    loadParticipants(session.id)
                }
                .onFailure { e ->
                    android.util.Log.e("SynthesisVM", "Ошибка создания: ${e.message}", e)
                    _state.value = _state.value.copy(
                        isCreating = false,
                        error = "Ошибка создания синтеза: ${e.message}"
                    )
                }
        }
    }

    fun loadSessionByCode(inviteCode: String) {
        val token = sessionManager.getAccessToken() ?: run {
            _state.value = _state.value.copy(error = "Необходимо войти в аккаунт")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            authRepository.getSynthesisSession(token, inviteCode)
                .onSuccess { session ->
                    val link = "lumisound://synthesis/${session.inviteCode}"
                    _state.value = _state.value.copy(
                        session = session,
                        inviteLink = link,
                        isLoading = false
                    )
                    loadParticipants(session.id)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Синтез не найден. Проверьте ссылку."
                    )
                }
        }
    }

    fun joinSession(username: String?, avatarUrl: String?) {
        val token = sessionManager.getAccessToken() ?: return
        val session = _state.value.session ?: return
        val myUserId = sessionManager.getUserId()

        // Нельзя присоединиться к своему синтезу
        if (session.creatorId == myUserId) {
            _state.value = _state.value.copy(error = "Это ваш синтез — поделитесь ссылкой с друзьями")
            return
        }
        // Уже участвует
        if (_state.value.participants.any { it.userId == myUserId }) {
            _state.value = _state.value.copy(joinSuccess = true)
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isJoining = true, error = null)
            authRepository.joinSynthesisSession(token, session.id, username, avatarUrl)
                .onSuccess {
                    _state.value = _state.value.copy(isJoining = false, joinSuccess = true)
                    loadParticipants(session.id)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isJoining = false,
                        error = "Ошибка присоединения: ${e.message}"
                    )
                }
        }
    }

    fun buildPlaylist(playlistViewModel: PlaylistViewModel) {
        val token = sessionManager.getAccessToken() ?: return
        val session = _state.value.session ?: return
        val participants = _state.value.participants

        if (participants.size < 2) {
            _state.value = _state.value.copy(error = "Нужно минимум 2 участника")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isBuildingPlaylist = true, error = null)
            try {
                // Берём топ-треки текущего пользователя
                val myTracks = authRepository.getFavoriteTracks(token, limit = 20, orderByPlayCount = true)
                    .getOrDefault(emptyList())

                if (myTracks.isEmpty()) {
                    _state.value = _state.value.copy(
                        isBuildingPlaylist = false,
                        error = "У вас нет прослушанных треков для синтеза. Послушайте музыку сначала!"
                    )
                    return@launch
                }

                // Перемешиваем треки
                val shuffled = myTracks.shuffled()

                // Создаём название из участников
                val names = participants.mapNotNull { it.username }.joinToString(" x ")
                val playlistName = if (names.isNotBlank()) names else "Синтез"

                authRepository.createPlaylist(token, playlistName, "Синтез ${participants.size} участников", true)
                    .onSuccess { playlist ->
                        // Добавляем треки с указанием автора
                        val myUsername = participants.firstOrNull { it.userId == sessionManager.getUserId() }?.username
                        shuffled.take(50).forEachIndexed { index, track ->
                            authRepository.addTrackToPlaylist(
                                token,
                                SupabaseService.PlaylistTrackInsert(
                                    playlistId = playlist.id,
                                    trackId = track.trackId,
                                    trackTitle = track.trackTitle,
                                    trackArtist = track.trackArtist,
                                    trackCoverUrl = track.trackCoverUrl,
                                    trackPreviewUrl = track.trackPreviewUrl,
                                    position = index,
                                    addedByUsername = myUsername,
                                    addedByAvatar = participants.firstOrNull { it.userId == sessionManager.getUserId() }?.avatarUrl
                                )
                            )
                        }
                        playlistViewModel.loadPlaylists()
                        _state.value = _state.value.copy(
                            isBuildingPlaylist = false,
                            createdPlaylistId = playlist.id
                        )
                    }
                    .onFailure { e ->
                        _state.value = _state.value.copy(
                            isBuildingPlaylist = false,
                            error = "Ошибка создания плейлиста: ${e.message}"
                        )
                    }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isBuildingPlaylist = false,
                    error = "Ошибка: ${e.message}"
                )
            }
        }
    }

    fun refreshParticipants() {
        val sessionId = _state.value.session?.id ?: return
        loadParticipants(sessionId)
    }

    private fun loadParticipants(sessionId: String) {
        val token = sessionManager.getAccessToken() ?: return
        viewModelScope.launch {
            val participants = authRepository.getSynthesisParticipants(token, sessionId)
            _state.value = _state.value.copy(participants = participants)
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }
    fun clearJoinSuccess() { _state.value = _state.value.copy(joinSuccess = false) }
    fun clearCreatedPlaylist() { _state.value = _state.value.copy(createdPlaylistId = null) }
    fun reset() { _state.value = SynthesisUiState() }
}
