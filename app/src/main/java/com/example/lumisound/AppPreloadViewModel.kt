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
        val token = sessionManager.getAccessToken() ?: return

        viewModelScope.launch {
            val profileJob = async { runCatching { authRepository.getProfile(token) } }
            val ratingsJob = async { runCatching { authRepository.getMyRatings(token, limit = 50) } }
            val commentsJob = async { runCatching { authRepository.getMyComments(token, limit = 50) } }
            val favTracksJob = async {
                runCatching { authRepository.getFavoriteTracks(token, limit = 20, orderByPlayCount = true) }
            }
            val favArtistsJob = async { runCatching { authRepository.getFavoriteArtists(token, limit = 20) } }
            // Быстрый поиск для fallback мини-плеера
            val quickSearchJob = async { runCatching { musicRepository.searchTracks("popular", limit = 5) } }
            val discoverJob = async { runCatching { musicRepository.getDiscoverFeed(30) } }
            val followingJob = async { runCatching { musicRepository.getFollowingFeed(30) } }

            // Ждём favTracks и быстрый поиск параллельно
            val favTracks = favTracksJob.await()
            val quickTracks = quickSearchJob.await()

            // Устанавливаем трек: приоритет — избранное, потом быстрый поиск
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
