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

data class ReviewUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val savedSuccess: Boolean = false,
    val error: String? = null,
    val rhymeScore: Int? = null,
    val imageryScore: Int? = null,
    val structureScore: Int? = null,
    val charismaScore: Int? = null,
    val atmosphereScore: Int? = null,
    val review: String = "",
    val commentText: String = "",
    val comments: List<SupabaseService.TrackCommentResponse> = emptyList(),
    val existingRating: SupabaseService.TrackRatingResponse? = null,
    val userAvatarUrl: String? = null  // фото профиля для поля ввода
) {
    val overallScore: Double?
        get() {
            val scores = listOfNotNull(rhymeScore, imageryScore, structureScore, charismaScore, atmosphereScore)
            return if (scores.size == 5) scores.sum().toDouble() / 5.0 else null
        }
    val isRatingComplete: Boolean
        get() = rhymeScore != null && imageryScore != null && structureScore != null &&
                charismaScore != null && atmosphereScore != null
}

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(ReviewUiState())
    val state: StateFlow<ReviewUiState> = _state.asStateFlow()

    fun loadForTrack(audiusTrackId: String) {
        val token = sessionManager.getAccessToken() ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val existing = authRepository.getMyTrackRating(token, audiusTrackId)
            val comments = authRepository.getTrackComments(token, audiusTrackId)
            // Загружаем аватар профиля
            val profile = authRepository.getProfile(token).getOrNull()
            _state.value = _state.value.copy(
                isLoading = false,
                existingRating = existing,
                rhymeScore = existing?.rhymeScore,
                imageryScore = existing?.imageryScore,
                structureScore = existing?.structureScore,
                charismaScore = existing?.charismaScore,
                atmosphereScore = existing?.atmosphereScore,
                review = existing?.review ?: "",
                comments = comments,
                userAvatarUrl = profile?.avatarUrl
            )
        }
    }

    fun setScore(criterion: ScoreCriterion, value: Int) {
        _state.value = when (criterion) {
            ScoreCriterion.RHYME -> _state.value.copy(rhymeScore = value)
            ScoreCriterion.IMAGERY -> _state.value.copy(imageryScore = value)
            ScoreCriterion.STRUCTURE -> _state.value.copy(structureScore = value)
            ScoreCriterion.CHARISMA -> _state.value.copy(charismaScore = value)
            ScoreCriterion.ATMOSPHERE -> _state.value.copy(atmosphereScore = value)
        }
    }

    fun setReview(text: String) {
        _state.value = _state.value.copy(review = text)
    }

    fun setCommentText(text: String) {
        _state.value = _state.value.copy(commentText = text)
    }

    fun saveRating(
        audiusTrackId: String,
        trackTitle: String,
        trackArtist: String,
        trackCoverUrl: String?
    ) {
        val token = sessionManager.getAccessToken() ?: return
        val s = _state.value
        viewModelScope.launch {
            _state.value = s.copy(isSaving = true, error = null)
            authRepository.upsertTrackRating(
                token,
                SupabaseService.TrackRatingInsert(
                    audiusTrackId = audiusTrackId,
                    trackTitle = trackTitle,
                    trackArtist = trackArtist,
                    trackCoverUrl = trackCoverUrl,
                    rhymeScore = s.rhymeScore,
                    imageryScore = s.imageryScore,
                    structureScore = s.structureScore,
                    charismaScore = s.charismaScore,
                    atmosphereScore = s.atmosphereScore,
                    review = s.review.takeIf { it.isNotBlank() }
                )
            ).onSuccess { saved ->
                _state.value = _state.value.copy(isSaving = false, savedSuccess = true, existingRating = saved)
            }.onFailure { e ->
                _state.value = _state.value.copy(isSaving = false, error = e.message)
            }
        }
    }

    fun submitComment(
        audiusTrackId: String,
        trackTitle: String,
        trackArtist: String,
        trackCoverUrl: String?
    ) {
        val token = sessionManager.getAccessToken() ?: return
        val text = _state.value.commentText.trim()
        if (text.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            authRepository.addTrackComment(
                token,
                SupabaseService.TrackCommentInsert(
                    audiusTrackId = audiusTrackId,
                    trackTitle = trackTitle,
                    trackArtist = trackArtist,
                    trackCoverUrl = trackCoverUrl,
                    comment = text
                )
            ).onSuccess { newComment ->
                _state.value = _state.value.copy(
                    isSaving = false,
                    commentText = "",
                    comments = listOf(newComment) + _state.value.comments
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(isSaving = false, error = e.message)
            }
        }
    }

    fun deleteComment(commentId: String) {
        val token = sessionManager.getAccessToken() ?: return
        viewModelScope.launch {
            authRepository.deleteTrackComment(token, commentId).onSuccess {
                _state.value = _state.value.copy(
                    comments = _state.value.comments.filter { it.id != commentId }
                )
            }
        }
    }

    fun clearSuccess() {
        _state.value = _state.value.copy(savedSuccess = false)
    }
}

enum class ScoreCriterion(val label: String) {
    RHYME("Рифмы / Образы"),
    IMAGERY("Структура / Ритмика"),
    STRUCTURE("Реализация стиля"),
    CHARISMA("Индивидуальность / Харизма"),
    ATMOSPHERE("Атмосфера / Вайб")
}
