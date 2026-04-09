package com.example.lumisound.feature.ratings

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

data class RatingsUiState(
    val isLoading: Boolean = false,
    val ratings: List<SupabaseService.TrackRatingResponse> = emptyList(),
    val comments: List<SupabaseService.TrackCommentResponse> = emptyList(),
    val bestReviews: List<SupabaseService.TrackRatingResponse> = emptyList(),
    val myReviews: List<SupabaseService.TrackRatingResponse> = emptyList(),
    val activeTab: RatingsTab = RatingsTab.BEST,
    val error: String? = null
) {
    val averageScore: Double?
        get() = ratings.mapNotNull { it.overallScore }.takeIf { it.isNotEmpty() }?.average()
}

enum class RatingsTab(val label: String) {
    BEST("Лучшие рецензии"),
    MINE("Ваши рецензии")
}

@HiltViewModel
class RatingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(RatingsUiState())
    val state: StateFlow<RatingsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        val token = sessionManager.getAccessToken() ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            // Мои оценки и комментарии
            val ratings = authRepository.getMyRatings(token)
            val comments = authRepository.getMyComments(token)

            // Мои рецензии (с текстом)
            val myReviews = ratings.filter { !it.review.isNullOrBlank() }
                .sortedByDescending { it.reputation }

            // Любимые треки и артисты для "Лучших рецензий"
            val favTracks = authRepository.getFavoriteTracks(token).getOrDefault(emptyList())
            val favArtists = authRepository.getFavoriteArtists(token).getOrDefault(emptyList())
            val favTrackIds = favTracks.map { it.trackId }
            val favArtistNames = favArtists.map { it.artistName }

            val bestReviews = authRepository.getBestReviewsForFavorites(token, favTrackIds, favArtistNames)

            _state.value = _state.value.copy(
                isLoading = false,
                ratings = ratings,
                comments = comments,
                myReviews = myReviews,
                bestReviews = bestReviews
            )
        }
    }

    fun setTab(tab: RatingsTab) {
        _state.value = _state.value.copy(activeTab = tab)
    }
}
