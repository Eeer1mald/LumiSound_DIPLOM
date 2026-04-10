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
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _isPreloaded = MutableStateFlow(false)
    val isPreloaded: StateFlow<Boolean> = _isPreloaded.asStateFlow()

    init {
        preloadAll()
    }

    private fun preloadAll() {
        val token = sessionManager.getAccessToken() ?: return

        viewModelScope.launch {
            // Всё параллельно через async
            val profileJob = async {
                runCatching { authRepository.getProfile(token) }
            }
            val ratingsJob = async {
                runCatching { authRepository.getMyRatings(token, limit = 50) }
            }
            val commentsJob = async {
                runCatching { authRepository.getMyComments(token, limit = 50) }
            }
            val favTracksJob = async {
                runCatching { authRepository.getFavoriteTracks(token, limit = 20, orderByPlayCount = true) }
            }
            val favArtistsJob = async {
                runCatching { authRepository.getFavoriteArtists(token, limit = 20) }
            }
            val discoverJob = async {
                runCatching { musicRepository.getDiscoverFeed(30) }
            }
            val followingJob = async {
                runCatching { musicRepository.getFollowingFeed(30) }
            }
            val homeRecsJob = async {
                runCatching { musicRepository.searchTracks("", limit = 10) }
            }

            // Ждём завершения всех — результаты уже закэшированы в репозиториях/ViewModels
            profileJob.await()
            ratingsJob.await()
            commentsJob.await()
            favTracksJob.await()
            favArtistsJob.await()
            discoverJob.await()
            followingJob.await()
            homeRecsJob.await()

            _isPreloaded.value = true
        }
    }
}
