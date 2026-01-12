package com.example.lumisound.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lumisound.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Home screen with real data from API.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        HomeUiState(
            userName = "Пользователь",
            recommendations = emptyList()
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadRecommendations()
    }

    fun refresh() {
        loadRecommendations()
    }
    
    private fun loadRecommendations() {
        viewModelScope.launch {
            _isLoading.value = true
            // Загружаем популярные треки как рекомендации
            // TODO: Реализовать отдельный метод для получения рекомендаций в репозитории
            musicRepository.searchTracks("", limit = 10)
                .onSuccess { tracks ->
                    _uiState.value = _uiState.value.copy(
                        recommendations = tracks.map { track ->
                            TrackPreview(
                                id = track.id,
                                title = track.name,
                                artist = track.artist,
                                coverUrl = track.imageUrl ?: track.hdImageUrl
                            )
                        }
                    )
                    _isLoading.value = false
                }
                .onFailure {
                    // В случае ошибки оставляем пустой список (не показываем ошибку пользователю)
                    // TODO: Добавить обработку ошибок с возможностью показа пользователю
                    _uiState.value = _uiState.value.copy(recommendations = emptyList())
                    _isLoading.value = false
                }
        }
    }
}


