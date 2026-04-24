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
    val isBuildingPlaylist: Boolean = false,
    val error: String? = null,
    val inviteCode: String? = null,
    val openPlaylistId: String? = null  // сигнал открыть плейлист
)

@HiltViewModel
class SynthesisViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(SynthesisUiState())
    val state: StateFlow<SynthesisUiState> = _state.asStateFlow()

    /**
     * Создаёт/получает постоянную сессию синтеза для текущего пользователя.
     * Один хост = одна сессия (без привязки к дате).
     */
    fun createSession(creatorUsername: String?, creatorAvatarUrl: String? = null) {
        val token = sessionManager.getAccessToken() ?: run {
            _state.value = _state.value.copy(error = "Необходимо войти в аккаунт")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isCreating = true, error = null)

            val profile = authRepository.getProfile(token).getOrNull()
            val realUsername = profile?.username?.takeIf { it.isNotBlank() } ?: creatorUsername
            val realAvatarUrl = profile?.avatarUrl ?: creatorAvatarUrl

            authRepository.createSynthesisSession(token, realUsername)
                .onSuccess { session ->
                    _state.value = _state.value.copy(
                        session = session,
                        inviteCode = session.inviteCode,
                        isCreating = false
                    )
                    // Добавляем себя как участника если ещё нет
                    authRepository.joinSynthesisSession(token, session.id, realUsername, realAvatarUrl)
                    loadParticipants(session.id)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(isCreating = false, error = "Ошибка создания синтеза: ${e.message}")
                }
        }
    }

    /** Загружает сессию по коду (для экрана присоединения) */
    fun loadSessionByCode(inviteCode: String) {
        val token = sessionManager.getAccessToken() ?: run {
            _state.value = _state.value.copy(error = "Необходимо войти в аккаунт")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            authRepository.getSynthesisSession(token, inviteCode)
                .onSuccess { session ->
                    _state.value = _state.value.copy(session = session, inviteCode = session.inviteCode, isLoading = false)
                    loadParticipants(session.id)
                }
                .onFailure {
                    _state.value = _state.value.copy(isLoading = false, error = "Синтез не найден. Проверьте код.")
                }
        }
    }

    /**
     * Присоединяется к сессии и создаёт/обновляет плейлист синтеза.
     *
     * Логика:
     * 1. Нельзя присоединиться к своей же сессии
     * 2. Если уже есть активный синтез с хостом — открываем его (нельзя создать дубль)
     * 3. Если уже участник этой сессии и плейлист есть — просто открываем
     * 4. Иначе — присоединяемся и создаём/обновляем плейлист через атомарную функцию
     */
    fun joinAndBuildPlaylist(username: String?, avatarUrl: String?) {
        // Защита от двойного вызова
        if (_state.value.isBuildingPlaylist) return
        val token = sessionManager.getAccessToken() ?: return
        val session = _state.value.session ?: return
        val myUserId = sessionManager.getUserId()

        if (session.creatorId == myUserId) {
            _state.value = _state.value.copy(error = "Это ваш синтез — поделитесь кодом с друзьями")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isBuildingPlaylist = true, error = null)
            try {
                val profile = authRepository.getProfile(token).getOrNull()
                val realUsername = profile?.username?.takeIf { it.isNotBlank() } ?: username
                val realAvatarUrl = profile?.avatarUrl ?: avatarUrl

                // Проверяем: уже есть синтез с хостом этой сессии?
                val existingSynthesisPlaylistId = authRepository.findExistingSynthesis(token, session.creatorId)
                if (existingSynthesisPlaylistId != null) {
                    android.util.Log.d("SynthesisVM", "Уже есть синтез с хостом: $existingSynthesisPlaylistId")
                    _state.value = _state.value.copy(isBuildingPlaylist = false, openPlaylistId = existingSynthesisPlaylistId)
                    return@launch
                }

                // Присоединяемся (ignore-duplicates — безопасно вызывать повторно)
                authRepository.joinSynthesisSession(token, session.id, realUsername, realAvatarUrl)
                kotlinx.coroutines.delay(300)

                val participants = authRepository.getSynthesisParticipants(token, session.id)
                _state.value = _state.value.copy(participants = participants)
                android.util.Log.d("SynthesisVM", "Участников: ${participants.size} — ${participants.map { it.username }}")

                val allTracks = collectTracksFromParticipants(token, participants)
                if (allTracks.isEmpty()) {
                    _state.value = _state.value.copy(isBuildingPlaylist = false, error = "У участников нет треков. Послушайте музыку сначала!")
                    return@launch
                }

                val names = participants.mapNotNull { it.username }.joinToString(" × ")
                val playlistName = if (names.isNotBlank()) "Синтез: $names" else "Синтез"
                val playlistDesc = "Синтез ${participants.size} участников"

                // Атомарно получаем или создаём плейлист — исключает дубли
                val playlistId = authRepository.getOrCreateSynthesisPlaylist(
                    token = token,
                    sessionId = session.id,
                    hostUserId = session.creatorId,
                    name = playlistName,
                    description = playlistDesc,
                    synthesisCode = session.inviteCode
                ).getOrElse { e ->
                    _state.value = _state.value.copy(isBuildingPlaylist = false, error = "Ошибка создания плейлиста: ${e.message}")
                    return@launch
                }

                // Обновляем название если плейлист уже существовал
                authRepository.updatePlaylistName(token, playlistId, playlistName, playlistDesc)

                // Пересобираем треки
                authRepository.clearPlaylistTracks(token, playlistId)
                insertTracksToPlaylist(token, playlistId, allTracks)

                _state.value = _state.value.copy(
                    session = session.copy(playlistId = playlistId),
                    isBuildingPlaylist = false,
                    openPlaylistId = playlistId
                )
            } catch (e: Exception) {
                android.util.Log.e("SynthesisVM", "joinAndBuildPlaylist error: ${e.message}", e)
                _state.value = _state.value.copy(isBuildingPlaylist = false, error = "Ошибка: ${e.message}")
            }
        }
    }

    private suspend fun collectTracksFromParticipants(
        token: String,
        participants: List<SupabaseService.SynthesisParticipant>
    ): List<Triple<SupabaseService.FavoriteTrackResponse, String?, String?>> {
        val trackContributors = mutableMapOf<String, MutableList<Pair<String?, String?>>>()
        val trackData = mutableMapOf<String, SupabaseService.FavoriteTrackResponse>()

        for (p in participants) {
            val userId = p.userId ?: continue
            val tracks = authRepository.getFavoriteTracksForUser(token, userId, limit = 25).getOrDefault(emptyList())
            for (t in tracks) {
                trackContributors.getOrPut(t.trackId) { mutableListOf() }.add(Pair(p.username, p.avatarUrl))
                if (!trackData.containsKey(t.trackId)) trackData[t.trackId] = t
            }
        }

        return trackData.values.shuffled().map { track ->
            val contributors = trackContributors[track.trackId] ?: emptyList()
            val usernames = contributors.mapNotNull { it.first }.joinToString(",")
            val avatars = contributors.mapNotNull { it.second }.joinToString(",")
            Triple(track, usernames.ifBlank { null }, avatars.ifBlank { null })
        }
    }

    private suspend fun insertTracksToPlaylist(
        token: String,
        playlistId: String,
        tracks: List<Triple<SupabaseService.FavoriteTrackResponse, String?, String?>>
    ) {
        tracks.take(60).forEachIndexed { index, (track, username, avatar) ->
            authRepository.addTrackToPlaylist(
                token,
                SupabaseService.PlaylistTrackInsert(
                    playlistId = playlistId,
                    trackId = track.trackId,
                    trackTitle = track.trackTitle,
                    trackArtist = track.trackArtist,
                    trackCoverUrl = track.trackCoverUrl,
                    trackPreviewUrl = track.trackPreviewUrl,
                    position = index,
                    addedByUsername = username,
                    addedByAvatar = avatar
                )
            )
        }
    }

    fun refreshParticipants() {
        val sessionId = _state.value.session?.id ?: return
        loadParticipants(sessionId)
    }

    /**
     * Покидает синтез.
     * - Если остаётся < 2 участников — плейлист удаляется
     * - Иначе — пересобирает плейлист без вышедшего
     * - Также удаляет сессию хоста если хост выходит
     */
    fun leaveSynthesis(synthesisCode: String, playlistId: String?, playlistViewModel: PlaylistViewModel) {
        val token = sessionManager.getAccessToken() ?: return
        val myUserId = sessionManager.getUserId()
        viewModelScope.launch {
            _state.value = _state.value.copy(isBuildingPlaylist = true, error = null)
            try {
                val session = _state.value.session
                    ?: authRepository.getSynthesisSession(token, synthesisCode).getOrNull()
                    ?: run {
                        _state.value = _state.value.copy(isBuildingPlaylist = false, error = "Сессия не найдена")
                        return@launch
                    }

                // Выходим из сессии
                authRepository.leaveSynthesisSession(token, session.id)

                val remaining = authRepository.getSynthesisParticipants(token, session.id)

                if (remaining.size < 2) {
                    // Удаляем плейлист
                    if (!playlistId.isNullOrBlank()) {
                        authRepository.deletePlaylist(token, playlistId)
                    }
                    // Если хост выходит — удаляем и сессию (чтобы можно было создать новую)
                    if (session.creatorId == myUserId) {
                        authRepository.deleteSynthesisSession(token, session.id)
                    }
                    playlistViewModel.loadPlaylists()
                    _state.value = _state.value.copy(isBuildingPlaylist = false, openPlaylistId = "DELETED")
                } else if (!playlistId.isNullOrBlank()) {
                    // Пересобираем плейлист без вышедшего
                    val allTracks = collectTracksFromParticipants(token, remaining)
                    val names = remaining.mapNotNull { it.username }.joinToString(" × ")
                    val playlistName = if (names.isNotBlank()) "Синтез: $names" else "Синтез"
                    authRepository.clearPlaylistTracks(token, playlistId)
                    authRepository.updatePlaylistName(token, playlistId, playlistName, "Синтез ${remaining.size} участников")
                    insertTracksToPlaylist(token, playlistId, allTracks)
                    playlistViewModel.loadPlaylists()
                    _state.value = _state.value.copy(isBuildingPlaylist = false, openPlaylistId = "LEFT")
                } else {
                    playlistViewModel.loadPlaylists()
                    _state.value = _state.value.copy(isBuildingPlaylist = false, openPlaylistId = "LEFT")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isBuildingPlaylist = false, error = "Ошибка выхода: ${e.message}")
            }
        }
    }


    private fun loadParticipants(sessionId: String) {
        val token = sessionManager.getAccessToken() ?: return
        viewModelScope.launch {
            val participants = authRepository.getSynthesisParticipants(token, sessionId)
            _state.value = _state.value.copy(participants = participants)
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }
    fun clearOpenPlaylist() { _state.value = _state.value.copy(openPlaylistId = null) }
    fun reset() { _state.value = SynthesisUiState() }
}
