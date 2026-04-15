package com.example.lumisound.feature.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lumisound.data.local.SessionManager
import com.example.lumisound.data.model.Track
import com.example.lumisound.data.remote.SupabaseService
import com.example.lumisound.data.repository.AuthRepository
import com.example.lumisound.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistDetailState(
    val playlist: SupabaseService.PlaylistResponse? = null,
    val tracks: List<SupabaseService.PlaylistTrackResponse> = emptyList(),
    val searchResults: List<Track> = emptyList(),
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val editName: String = "",
    val editDescription: String = "",
    val isEditingName: Boolean = false,
    val coverUri: android.net.Uri? = null,
    val currentUserId: String? = null,
    val error: String? = null
)

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val musicRepository: MusicRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(PlaylistDetailState())
    val state: StateFlow<PlaylistDetailState> = _state.asStateFlow()

    fun loadPlaylist(playlist: SupabaseService.PlaylistResponse) {
        val token = sessionManager.getAccessToken() ?: return
        _state.value = _state.value.copy(
            playlist = playlist,
            editName = playlist.name,
            editDescription = playlist.description ?: "",
            isLoading = true,
            currentUserId = sessionManager.getUserId()
        )
        viewModelScope.launch {
            val tracks = authRepository.getPlaylistTracks(token, playlist.id)
            // Если у плейлиста нет обложки — берём аватарку первого трека
            val enrichedPlaylist = if (playlist.coverUrl.isNullOrEmpty()) {
                val firstCover = tracks.firstOrNull { !it.trackCoverUrl.isNullOrEmpty() }?.trackCoverUrl
                if (firstCover != null) playlist.copy(coverUrl = firstCover) else playlist
            } else {
                playlist
            }
            _state.value = _state.value.copy(
                playlist = enrichedPlaylist,
                tracks = tracks,
                isLoading = false
            )
        }
    }

    fun setSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        if (query.isBlank()) {
            _state.value = _state.value.copy(searchResults = emptyList(), isSearching = false)
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSearching = true)
            musicRepository.searchTracks(query, limit = 20)
                .onSuccess { results ->
                    _state.value = _state.value.copy(searchResults = results, isSearching = false)
                }
                .onFailure {
                    _state.value = _state.value.copy(searchResults = emptyList(), isSearching = false)
                }
        }
    }

    fun addTrack(track: Track) {
        val token = sessionManager.getAccessToken() ?: return
        val playlistId = _state.value.playlist?.id ?: return
        if (_state.value.tracks.any { it.trackId == track.id }) return

        val newTrack = SupabaseService.PlaylistTrackResponse(
            id = java.util.UUID.randomUUID().toString(),
            playlistId = playlistId,
            trackId = track.id,
            trackTitle = track.name,
            trackArtist = track.artist,
            trackCoverUrl = track.imageUrl,
            trackPreviewUrl = track.previewUrl,
            position = _state.value.tracks.size
        )
        val newCount = _state.value.tracks.size + 1
        _state.value = _state.value.copy(
            tracks = _state.value.tracks + newTrack,
            playlist = _state.value.playlist?.copy(trackCount = newCount)
        )
        viewModelScope.launch {
            authRepository.addTrackToPlaylist(
                token,
                SupabaseService.PlaylistTrackInsert(
                    playlistId = playlistId,
                    trackId = track.id,
                    trackTitle = track.name,
                    trackArtist = track.artist,
                    trackCoverUrl = track.imageUrl,
                    trackPreviewUrl = track.previewUrl,
                    position = _state.value.tracks.size - 1
                )
            )
            // Обновляем track_count в БД
            authRepository.updatePlaylistTrackCount(token, playlistId, newCount)
            onNameUpdated?.invoke(playlistId, _state.value.playlist?.name ?: "", _state.value.playlist?.description, newCount)
        }
    }

    fun removeTrack(trackId: String) {
        val token = sessionManager.getAccessToken() ?: return
        val playlistId = _state.value.playlist?.id ?: return
        val newCount = (_state.value.tracks.size - 1).coerceAtLeast(0)
        _state.value = _state.value.copy(
            tracks = _state.value.tracks.filter { it.trackId != trackId },
            playlist = _state.value.playlist?.copy(trackCount = newCount)
        )
        viewModelScope.launch {
            authRepository.removeTrackFromPlaylist(token, playlistId, trackId)
            // Обновляем track_count в БД
            authRepository.updatePlaylistTrackCount(token, playlistId, newCount)
            onNameUpdated?.invoke(playlistId, _state.value.playlist?.name ?: "", _state.value.playlist?.description, newCount)
        }
    }

    fun setEditName(name: String) {
        _state.value = _state.value.copy(editName = name)
    }

    fun setEditDescription(desc: String) {
        _state.value = _state.value.copy(editDescription = desc)
    }

    private var onNameUpdated: ((String, String, String?, Int?) -> Unit)? = null

    fun setOnNameUpdated(callback: (playlistId: String, name: String, description: String?, trackCount: Int?) -> Unit) {
        onNameUpdated = callback
    }

    fun saveName() {
        val token = sessionManager.getAccessToken() ?: return
        val playlistId = _state.value.playlist?.id ?: return
        val name = _state.value.editName.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            val desc = _state.value.editDescription.takeIf { it.isNotBlank() }
            authRepository.updatePlaylistName(token, playlistId, name, desc)
                .onSuccess { _ ->
                    _state.value = _state.value.copy(
                        playlist = _state.value.playlist?.copy(name = name, description = desc),
                        isSaving = false,
                        isEditingName = false
                    )
                    // Уведомляем главную страницу
                    onNameUpdated?.invoke(playlistId, name, desc, _state.value.playlist?.trackCount)
                }
                .onFailure { _ -> _state.value = _state.value.copy(isSaving = false) }
        }
    }

    fun toggleEditName() {
        _state.value = _state.value.copy(isEditingName = !_state.value.isEditingName)
    }

    private var onCoverUpdated: ((String, String) -> Unit)? = null

    fun setOnCoverUpdated(callback: (playlistId: String, coverUrl: String) -> Unit) {
        onCoverUpdated = callback
    }

    fun setCoverUri(uri: android.net.Uri) {
        _state.value = _state.value.copy(coverUri = uri)
        val token = sessionManager.getAccessToken() ?: return
        val playlistId = _state.value.playlist?.id ?: return
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                val bytes = inputStream.readBytes()
                inputStream.close()
                authRepository.uploadPlaylistCover(token, playlistId, bytes)
                    .onSuccess { coverUrl ->
                        authRepository.updatePlaylistCover(token, playlistId, coverUrl)
                            .onSuccess {
                                _state.value = _state.value.copy(
                                    playlist = _state.value.playlist?.copy(coverUrl = coverUrl)
                                )
                                // Уведомляем главную страницу об обновлении
                                onCoverUpdated?.invoke(playlistId, coverUrl)
                            }
                    }
                    .onFailure { e ->
                        android.util.Log.e("PlaylistDetailVM", "Ошибка загрузки обложки: ${e.message}")
                    }
            } catch (e: Exception) {
                android.util.Log.e("PlaylistDetailVM", "Ошибка: ${e.message}")
            }
        }
    }

    fun toggleVisibility() {
        val token = sessionManager.getAccessToken() ?: return
        val playlist = _state.value.playlist ?: return
        val newIsPublic = !playlist.isPublic
        _state.value = _state.value.copy(playlist = playlist.copy(isPublic = newIsPublic))
        viewModelScope.launch {
            authRepository.updatePlaylistVisibility(token, playlist.id, newIsPublic)
        }
    }

    fun isTrackAdded(trackId: String): Boolean = _state.value.tracks.any { it.trackId == trackId }
}
