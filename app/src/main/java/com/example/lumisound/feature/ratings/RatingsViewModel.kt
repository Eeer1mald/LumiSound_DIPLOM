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
    val activeTab: RatingsTab = RatingsTab.RATINGS,
    val filterScore: Int? = null,
    val error: String? = null
) {
    val filteredRatings: List<SupabaseService.TrackRatingResponse>
        get() = if (filterScore == null) ratings
        else ratings.filter { (it.overallScore ?: 0.0) >= filterScore }

    val averageScore: Double?
        get() = ratings.mapNotNull { it.overallScore }.takeIf { it.isNotEmpty() }?.average()

    val topRatedCount: Int
        get() = ratings.count { (it.overallScore ?: 0.0) >= 8.0 }
}

enum class RatingsTab(val label: String) {
    RATINGS("Оценки"),
    COMMENTS("Комментарии")
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
            val ratings = authRepository.getMyRatings(token)
            val comments = authRepository.getMyComments(token)
            _state.value = _state.value.copy(isLoading = false, ratings = ratings, comments = comments)
        }
    }

    fun setTab(tab: RatingsTab) {
        _state.value = _state.value.copy(activeTab = tab)
    }

    fun setFilter(score: Int?) {
        _state.value = _state.value.copy(filterScore = score)
    }
}
