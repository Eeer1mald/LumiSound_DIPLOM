package com.example.lumisound.feature.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lumisound.data.local.SessionManager
import com.example.lumisound.data.model.Track
import com.example.lumisound.data.remote.SupabaseService
import com.example.lumisound.data.repository.AuthRepository
import com.example.lumisound.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddToPlaylistState(
    val playlists: List<SupabaseService.PlaylistResponse> = emptyList(),
    val isLoading: Boolean = false,
    val isAdding: Boolean = false,
    val isCreating: Boolean = false,
    val successMessage: String? = null,
    val error: String? = null
)

@HiltViewModel
class AddToPlaylistViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val musicRepository: MusicRepository,
    private val sessionManager: SessionManager,
    private val playlistEventBus: PlaylistEventBus
) : ViewModel() {

    private val _state = MutableStateFlow(AddToPlaylistState())
    val state: StateFlow<AddToPlaylistState> = _state.asStateFlow()

    fun loadPlaylists() {
        val token = sessionManager.getAccessToken() ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val playlists = authRepository.getMyPlaylists(token)

            // Для плейлистов без обложки — берём аватарку первого трека
            val enriched = playlists.map { playlist ->
                if (playlist.coverUrl.isNullOrEmpty() && playlist.trackCount > 0) {
                    val tracks = authRepository.getPlaylistTracks(token, playlist.id)
                    val firstCover = tracks.firstOrNull { !it.trackCoverUrl.isNullOrEmpty() }?.trackCoverUrl
                    if (firstCover != null) playlist.copy(coverUrl = firstCover) else playlist
                } else {
                    playlist
                }
            }

            _state.value = _state.value.copy(playlists = enriched, isLoading = false)
        }
    }

    /** Добавить трек в существующий плейлист */
    fun addTrackToPlaylist(
        track: Track,
        playlistId: String,
        playlistName: String,
        onDone: () -> Unit
    ) {
        val token = sessionManager.getAccessToken() ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isAdding = true, error = null)
            val insert = SupabaseService.PlaylistTrackInsert(
                playlistId = playlistId,
                trackId = track.id,
                trackTitle = track.name,
                trackArtist = track.artist,
                trackCoverUrl = track.imageUrl,
                trackPreviewUrl = track.previewUrl,
                position = 0
            )
            authRepository.addTrackToPlaylist(token, insert)
                .onSuccess {
                    // Обновляем счётчик треков
                    val playlist = _state.value.playlists.find { it.id == playlistId }
                    if (playlist != null) {
                        authRepository.updatePlaylistTrackCount(token, playlistId, playlist.trackCount + 1)
                    }
                    _state.value = _state.value.copy(
                        isAdding = false,
                        successMessage = "Добавлено в «$playlistName»"
                    )
                    onDone()
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isAdding = false,
                        error = e.message ?: "Ошибка добавления"
                    )
                }
        }
    }

    /**
     * Создать новый плейлист и добавить трек.
     * Если autoFill=true — дополнительно ищем похожие треки и добавляем их.
     */
    fun createPlaylistAndAdd(
        track: Track,
        playlistName: String,
        autoFill: Boolean,
        onDone: () -> Unit
    ) {
        val token = sessionManager.getAccessToken() ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isCreating = true, error = null)
            authRepository.createPlaylist(token, playlistName)
                .onSuccess { playlist ->
                    // Добавляем исходный трек
                    val insert = SupabaseService.PlaylistTrackInsert(
                        playlistId = playlist.id,
                        trackId = track.id,
                        trackTitle = track.name,
                        trackArtist = track.artist,
                        trackCoverUrl = track.imageUrl,
                        trackPreviewUrl = track.previewUrl,
                        position = 0
                    )
                    authRepository.addTrackToPlaylist(token, insert)

                    var addedCount = 1

                    if (autoFill) {
                        // Ищем похожие треки по артисту и жанру
                        val similarTracks = mutableListOf<Track>()

                        // По артисту
                        musicRepository.searchTracks(track.artist, limit = 10).getOrNull()
                            ?.filter { it.id != track.id }
                            ?.take(5)
                            ?.let { similarTracks.addAll(it) }

                        // По жанру если есть
                        if (similarTracks.size < 8 && !track.genre.isNullOrBlank()) {
                            musicRepository.searchTracks(track.genre, limit = 10).getOrNull()
                                ?.filter { t -> t.id != track.id && similarTracks.none { it.id == t.id } }
                                ?.take(8 - similarTracks.size)
                                ?.let { similarTracks.addAll(it) }
                        }

                        similarTracks.forEachIndexed { idx, t ->
                            val sim = SupabaseService.PlaylistTrackInsert(
                                playlistId = playlist.id,
                                trackId = t.id,
                                trackTitle = t.name,
                                trackArtist = t.artist,
                                trackCoverUrl = t.imageUrl,
                                trackPreviewUrl = t.previewUrl,
                                position = idx + 1
                            )
                            authRepository.addTrackToPlaylist(token, sim)
                            addedCount++
                        }
                    }

                    authRepository.updatePlaylistTrackCount(token, playlist.id, addedCount)

                    val msg = if (autoFill)
                        "Плейлист «$playlistName» создан ($addedCount треков)"
                    else
                        "Плейлист «$playlistName» создан"

                    _state.value = _state.value.copy(isCreating = false, successMessage = msg)
                    // Уведомляем PlaylistViewModel об обновлении
                    playlistEventBus.emit(PlaylistEvent.PlaylistCreated)
                    // Обновляем список плейлистов в шторке
                    val updated = authRepository.getMyPlaylists(token)
                    _state.value = _state.value.copy(playlists = updated)
                    onDone()
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isCreating = false,
                        error = e.message ?: "Ошибка создания плейлиста"
                    )
                }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(successMessage = null, error = null)
    }
}
