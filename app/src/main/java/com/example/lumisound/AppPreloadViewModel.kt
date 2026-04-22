package com.example.lumisound

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lumisound.data.local.SessionManager
import com.example.lumisound.data.repository.AuthRepository
import com.example.lumisound.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Запускается при старте приложения и параллельно предзагружает
 * данные для всех экранов, чтобы при переходе всё было готово.
 */
@HiltViewModel
class AppPreloadViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val musicRepository: MusicRepository,
    private val sessionManager: SessionManager,
    private val playerStateHolder: com.example.lumisound.data.player.PlayerStateHolder
) : ViewModel() {

    private val _isPreloaded = MutableStateFlow(false)
    val isPreloaded: StateFlow<Boolean> = _isPreloaded.asStateFlow()

    init {
        preloadAll()
    }

    fun triggerPreload() {
        // Если трек уже установлен — не перезапускаем
        if (playerStateHolder.currentTrack.value != null) return
        preloadAll()
    }

    private fun preloadAll() {
        viewModelScope.launch {
            // Сначала пробуем обновить токен, потом берём актуальный
            val refreshed = runCatching { authRepository.refreshTokenIfNeeded() }.getOrNull()
            val validToken = refreshed ?: sessionManager.getAccessToken() ?: return@launch
            val profileJob = async { runCatching { authRepository.getProfile(validToken) } }
            val ratingsJob = async { runCatching { authRepository.getMyRatings(validToken, limit = 50) } }
            val commentsJob = async { runCatching { authRepository.getMyComments(validToken, limit = 50) } }
            val favTracksJob = async {
                runCatching { authRepository.getFavoriteTracks(validToken, limit = 20, orderByPlayCount = true) }
            }
            val favArtistsJob = async { runCatching { authRepository.getFavoriteArtists(validToken, limit = 20) } }
            // История прослушивания — для начального трека
            val historyJob = async { runCatching { authRepository.getTrackHistory(validToken, limit = 1) } }
            // Быстрый поиск для fallback мини-плеера
            val quickSearchJob = async { runCatching { musicRepository.searchTracks("popular", limit = 5) } }
            val discoverJob = async { runCatching { musicRepository.getDiscoverFeed(30) } }
            val followingJob = async { runCatching { musicRepository.getFollowingFeed(30) } }

            // Ждём историю и быстрый поиск параллельно
            val history = historyJob.await()
            val favTracks = favTracksJob.await()
            val quickTracks = quickSearchJob.await()

            // Приоритет: последний прослушанный → избранное → быстрый поиск
            val lastPlayed = history.getOrNull()?.getOrNull()?.firstOrNull()
            if (lastPlayed != null) {
                // previewUrl строится из Audius stream URL — не требует сетевого запроса
                val streamUrl = musicRepository.getStreamUrl(lastPlayed.trackId)
                playerStateHolder.setCurrentTrack(
                    com.example.lumisound.data.model.Track(
                        id = lastPlayed.trackId,
                        name = lastPlayed.trackTitle,
                        artist = lastPlayed.trackArtist,
                        previewUrl = streamUrl
                    )
                )
            } else {
                val fromFav = favTracks.getOrNull()?.getOrNull()?.firstOrNull { !it.trackPreviewUrl.isNullOrBlank() }
                if (fromFav != null) {
                    playerStateHolder.setCurrentTrack(
                        com.example.lumisound.data.model.Track(
                            id = fromFav.trackId,
                            name = fromFav.trackTitle,
                            artist = fromFav.trackArtist,
                            imageUrl = fromFav.trackCoverUrl,
                            previewUrl = fromFav.trackPreviewUrl
                        )
                    )
                } else {
                    // Fallback: первый трек из быстрого поиска
                    quickTracks.getOrNull()?.getOrNull()?.firstOrNull { !it.previewUrl.isNullOrBlank() }?.let { t ->
                        playerStateHolder.setCurrentTrack(t)
                    }
                }
            }

            // Ждём остальные в фоне
            profileJob.await()
            ratingsJob.await()
            commentsJob.await()
            favArtistsJob.await()
            discoverJob.await()
            followingJob.await()

            _isPreloaded.value = true
        }
    }
}
