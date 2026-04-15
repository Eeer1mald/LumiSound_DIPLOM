package com.example.lumisound.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lumisound.data.local.SessionManager
import com.example.lumisound.data.remote.SupabaseService
import com.example.lumisound.data.repository.AuthRepository
import com.example.lumisound.feature.playlist.PlaylistEvent
import com.example.lumisound.feature.playlist.PlaylistEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PlaylistTab { MY, RECOMMENDED, TOP }

data class HomeStats(
    val ratingsThisWeek: Int = 0,
    val totalRatings: Int = 0,
    val totalComments: Int = 0
)

data class PlaylistUiState(
    val myPlaylists: List<SupabaseService.PlaylistResponse> = emptyList(),
    val recommendedPlaylists: List<SupabaseService.PlaylistResponse> = emptyList(),
    val topPlaylists: List<SupabaseService.PlaylistResponse> = emptyList(),
    val recentTracks: List<SupabaseService.FavoriteTrackResponse> = emptyList(),
    val topTracks: List<SupabaseService.FavoriteTrackResponse> = emptyList(),
    val likedPlaylistIds: Set<String> = emptySet(),
    val stats: HomeStats = HomeStats(),
    val selectedTab: PlaylistTab = PlaylistTab.MY,
    val currentUserId: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val savedSuccess: Boolean = false
) {
    val playlists: List<SupabaseService.PlaylistResponse> get() = when (selectedTab) {
        PlaylistTab.MY -> myPlaylists
        PlaylistTab.RECOMMENDED -> recommendedPlaylists
        PlaylistTab.TOP -> topPlaylists
    }
}

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    private val playlistEventBus: PlaylistEventBus
) : ViewModel() {

    private val _state = MutableStateFlow(PlaylistUiState())
    val state: StateFlow<PlaylistUiState> = _state.asStateFlow()

    init {
        loadAll()
        // Слушаем события создания плейлиста из других экранов
        viewModelScope.launch {
            playlistEventBus.events.collect { event ->
                when (event) {
                    is PlaylistEvent.PlaylistCreated -> refreshMyPlaylists()
                }
            }
        }
    }

    fun loadAll() {
        val token = sessionManager.getAccessToken() ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            val myJob = async { authRepository.getMyPlaylists(token) }
            val topJob = async { authRepository.getPublicPlaylists(token, limit = 20) }
            val favArtistsJob = async { authRepository.getFavoriteArtists(token, limit = 20).getOrDefault(emptyList()) }
            val recentJob = async { authRepository.getFavoriteTracks(token, limit = 10, orderByPlayCount = false).getOrDefault(emptyList()) }
            val topTracksJob = async { authRepository.getFavoriteTracks(token, limit = 10, orderByPlayCount = true).getOrDefault(emptyList()) }
            val ratingsJob = async { authRepository.getMyRatings(token, limit = 200) }
            val commentsJob = async { authRepository.getMyComments(token, limit = 200) }
            // Загружаем все лайки пользователя сразу
            val likedIdsJob = async { authRepository.getMyLikedPlaylistIds(token) }

            val myPlaylists = myJob.await()
            val topPlaylists = topJob.await()
            val favArtists = favArtistsJob.await()
            val recentTracks = recentJob.await()
            val topTracks = topTracksJob.await()
            val ratings = ratingsJob.await()
            val comments = commentsJob.await()
            val likedIds = likedIdsJob.await()

            val artistNames = favArtists.map { it.artistName }
            val recommended = authRepository.getRecommendedPlaylists(token, artistNames, limit = 20)

            // Обогащаем плейлисты без обложки — берём аватарку первого трека
            val enrichedMy = enrichWithTrackCovers(token, myPlaylists)

            val weekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
            val ratingsThisWeek = ratings.count { r ->
                try {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                    val date = sdf.parse(r.createdAt?.take(19) ?: "")
                    (date?.time ?: 0L) > weekAgo
                } catch (e: Exception) { false }
            }

            _state.value = _state.value.copy(
                myPlaylists = enrichedMy,
                recommendedPlaylists = recommended,
                topPlaylists = topPlaylists,
                recentTracks = recentTracks,
                topTracks = topTracks,
                currentUserId = sessionManager.getUserId(),
                likedPlaylistIds = likedIds,
                stats = HomeStats(
                    ratingsThisWeek = ratingsThisWeek,
                    totalRatings = ratings.size,
                    totalComments = comments.size
                ),
                isLoading = false
            )
        }
    }

    /** Обогащает плейлисты без coverUrl обложкой первого трека */
    private suspend fun enrichWithTrackCovers(
        token: String,
        playlists: List<SupabaseService.PlaylistResponse>
    ): List<SupabaseService.PlaylistResponse> {
        return playlists.map { playlist ->
            if (playlist.coverUrl.isNullOrEmpty() && playlist.trackCount > 0) {
                val tracks = authRepository.getPlaylistTracks(token, playlist.id)
                val firstCover = tracks.firstOrNull { !it.trackCoverUrl.isNullOrEmpty() }?.trackCoverUrl
                if (firstCover != null) playlist.copy(coverUrl = firstCover) else playlist
            } else {
                playlist
            }
        }
    }

    fun loadPlaylists() = loadAll()

    /** Быстрое обновление только списка моих плейлистов с обогащением обложками */
    private fun refreshMyPlaylists() {
        val token = sessionManager.getAccessToken() ?: return
        viewModelScope.launch {
            val playlists = authRepository.getMyPlaylists(token)
            val enriched = enrichWithTrackCovers(token, playlists)
            _state.value = _state.value.copy(myPlaylists = enriched)
        }
    }

    fun selectTab(tab: PlaylistTab) {
        _state.value = _state.value.copy(selectedTab = tab)
    }

    fun createPlaylist(name: String = "", description: String? = null, isPublic: Boolean = false) {
        val token = sessionManager.getAccessToken() ?: return
        val autoName = name.ifBlank {
            val count = _state.value.myPlaylists.size + 1
            "Плейлист $count"
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            authRepository.createPlaylist(token, autoName, description?.takeIf { it.isNotBlank() }, isPublic)
                .onSuccess { newPlaylist ->
                    _state.value = _state.value.copy(
                        myPlaylists = listOf(newPlaylist) + _state.value.myPlaylists,
                        isSaving = false,
                        savedSuccess = true
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(isSaving = false, error = e.message)
                }
        }
    }

    fun toggleVisibility(playlistId: String, currentIsPublic: Boolean) {
        val token = sessionManager.getAccessToken() ?: return
        val newIsPublic = !currentIsPublic
        viewModelScope.launch {
            authRepository.updatePlaylistVisibility(token, playlistId, newIsPublic).onSuccess {
                _state.value = _state.value.copy(
                    myPlaylists = _state.value.myPlaylists.map {
                        if (it.id == playlistId) it.copy(isPublic = newIsPublic) else it
                    }
                )
            }
        }
    }

    fun toggleLike(playlistId: String) {
        val token = sessionManager.getAccessToken() ?: return
        val myUserId = sessionManager.getUserId()
        val playlist = (_state.value.topPlaylists + _state.value.recommendedPlaylists).firstOrNull { it.id == playlistId }
        if (playlist?.userId == myUserId) return

        val isLiked = _state.value.likedPlaylistIds.contains(playlistId)
        val delta = if (isLiked) -1 else 1

        // Optimistic update
        _state.value = _state.value.copy(
            likedPlaylistIds = if (isLiked) _state.value.likedPlaylistIds - playlistId
                               else _state.value.likedPlaylistIds + playlistId,
            topPlaylists = _state.value.topPlaylists.map {
                if (it.id == playlistId) it.copy(likesCount = (it.likesCount + delta).coerceAtLeast(0)) else it
            },
            recommendedPlaylists = _state.value.recommendedPlaylists.map {
                if (it.id == playlistId) it.copy(likesCount = (it.likesCount + delta).coerceAtLeast(0)) else it
            }
        )
        viewModelScope.launch {
            val result = if (isLiked) authRepository.unlikePlaylist(token, playlistId)
                         else authRepository.likePlaylist(token, playlistId)
            result.onFailure {
                // Rollback при ошибке
                _state.value = _state.value.copy(
                    likedPlaylistIds = if (isLiked) _state.value.likedPlaylistIds + playlistId
                                       else _state.value.likedPlaylistIds - playlistId,
                    topPlaylists = _state.value.topPlaylists.map {
                        if (it.id == playlistId) it.copy(likesCount = (it.likesCount - delta).coerceAtLeast(0)) else it
                    },
                    recommendedPlaylists = _state.value.recommendedPlaylists.map {
                        if (it.id == playlistId) it.copy(likesCount = (it.likesCount - delta).coerceAtLeast(0)) else it
                    }
                )
            }
        }
    }

    // Загружаем состояние лайка для конкретного плейлиста (при открытии)
    fun loadLikeStatus(playlistId: String) {
        val token = sessionManager.getAccessToken() ?: return
        viewModelScope.launch {
            val isLiked = authRepository.isPlaylistLiked(token, playlistId)
            _state.value = _state.value.copy(
                likedPlaylistIds = if (isLiked) _state.value.likedPlaylistIds + playlistId
                                   else _state.value.likedPlaylistIds - playlistId
            )
        }
    }

    // Обновляем название/описание плейлиста в списке
    fun updatePlaylistNameLocally(playlistId: String, name: String, description: String?, trackCount: Int? = null) {
        _state.value = _state.value.copy(
            myPlaylists = _state.value.myPlaylists.map {
                if (it.id == playlistId) it.copy(
                    name = name,
                    description = description,
                    trackCount = trackCount ?: it.trackCount
                ) else it
            }
        )
    }

    fun deletePlaylist(playlistId: String) {
        val token = sessionManager.getAccessToken() ?: return
        viewModelScope.launch {
            authRepository.deletePlaylist(token, playlistId).onSuccess {
                _state.value = _state.value.copy(
                    myPlaylists = _state.value.myPlaylists.filter { it.id != playlistId }
                )
            }
        }
    }

    fun updatePlaylistCoverLocally(playlistId: String, coverUrl: String) {
        _state.value = _state.value.copy(
            myPlaylists = _state.value.myPlaylists.map {
                if (it.id == playlistId) it.copy(coverUrl = coverUrl) else it
            }
        )
    }

    fun getRandomTrackFromPlaylists(): SupabaseService.FavoriteTrackResponse? {
        return _state.value.topTracks.randomOrNull()
    }

    fun clearSuccess() {
        _state.value = _state.value.copy(savedSuccess = false)
    }
}
