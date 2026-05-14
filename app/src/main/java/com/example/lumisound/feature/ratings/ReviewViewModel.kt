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
    val reviews: List<SupabaseService.TrackRatingResponse> = emptyList(),
    val myVotes: Map<String, Int> = emptyMap(), // ratingId -> vote (+1 или -1)
    val votingIds: Set<String> = emptySet(),    // ratingId-ы, по которым идёт запрос
    val existingRating: SupabaseService.TrackRatingResponse? = null,
    val averageRating: SupabaseService.TrackAverageRating? = null,
    val userAvatarUrl: String? = null,
    val currentUserId: String? = null
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

    // Кэш: trackId для которого уже загружены данные
    private var loadedTrackId: String? = null
    // Флаг: идёт ли загрузка прямо сейчас (защита от параллельных вызовов)
    private var isLoadingNow = false

    fun loadForTrack(audiusTrackId: String) {
        // Если данные уже загружены для этого трека — не перезагружаем
        if (loadedTrackId == audiusTrackId && !_state.value.isLoading && _state.value.error == null) {
            return
        }
        // Защита от параллельных вызовов
        if (isLoadingNow && loadedTrackId == audiusTrackId) return

        val token = sessionManager.getAccessToken() ?: return
        isLoadingNow = true
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val existing = authRepository.getMyTrackRating(token, audiusTrackId)
            val comments = authRepository.getTrackComments(token, audiusTrackId)
            val rawReviews = authRepository.getTrackReviews(token, audiusTrackId)
            val profile = authRepository.getProfile(token).getOrNull()
            val averageRating = authRepository.getTrackAverageRating(token, audiusTrackId)

            // Обогащаем рецензии реальными данными профиля если username/avatar отсутствуют
            // Один батч-запрос вместо N отдельных
            val missingProfileIds = rawReviews
                .filter { it.username.isNullOrBlank() || it.userAvatarUrl.isNullOrBlank() }
                .mapNotNull { it.userId }
                .distinct()
            val profilesMap = if (missingProfileIds.isNotEmpty())
                authRepository.getProfilesByIds(missingProfileIds)
            else emptyMap()

            val reviews = rawReviews.map { review ->
                val needsEnrich = review.username.isNullOrBlank() || review.userAvatarUrl.isNullOrBlank()
                if (needsEnrich && review.userId != null) {
                    val p = profilesMap[review.userId]
                    if (p != null) {
                        review.copy(
                            username = review.username?.takeIf { it.isNotBlank() } ?: p.username,
                            userAvatarUrl = review.userAvatarUrl?.takeIf { it.isNotBlank() } ?: p.avatarUrl
                        )
                    } else review
                } else review
            }

            _state.value = _state.value.copy(
                isLoading = false,
                existingRating = existing,
                averageRating = averageRating,
                reviews = reviews,
                rhymeScore = existing?.rhymeScore,
                imageryScore = existing?.imageryScore,
                structureScore = existing?.structureScore,
                charismaScore = existing?.charismaScore,
                atmosphereScore = existing?.atmosphereScore,
                review = "", // поле ввода всегда пустое при открытии
                comments = comments,
                userAvatarUrl = profile?.avatarUrl,
                currentUserId = sessionManager.getUserId()
            )
            // Запоминаем для какого трека загружены данные
            loadedTrackId = audiusTrackId
            isLoadingNow = false
        }
    }

    /** Принудительно перезагружает данные (например после отправки комментария) */
    fun invalidateAndReload(audiusTrackId: String) {
        loadedTrackId = null
        isLoadingNow = false
        loadForTrack(audiusTrackId)
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
                    review = s.review.takeIf { it.isNotBlank() },
                    username = authRepository.getProfile(token).getOrNull()?.username,
                    userAvatarUrl = s.userAvatarUrl
                )
            ).onSuccess { saved ->
                val newAverage = authRepository.getTrackAverageRating(token, audiusTrackId)
                // Оптимистично обновляем список рецензий — добавляем/заменяем свою
                val updatedReviews = _state.value.reviews.toMutableList()
                val existingIdx = updatedReviews.indexOfFirst { it.userId == sessionManager.getUserId() }
                if (existingIdx >= 0) {
                    updatedReviews[existingIdx] = saved
                } else {
                    updatedReviews.add(0, saved)
                }
                _state.value = _state.value.copy(
                    isSaving = false,
                    savedSuccess = true,
                    existingRating = saved,
                    averageRating = newAverage,
                    reviews = updatedReviews.sortedByDescending { it.reputation },
                    review = "" // очищаем поле после сохранения
                )
                // Инвалидируем кэш чтобы при следующем открытии данные обновились с сервера
                loadedTrackId = null
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
                    comment = text,
                    username = authRepository.getProfile(token).getOrNull()?.username,
                    userAvatarUrl = _state.value.userAvatarUrl
                )
            ).onSuccess { newComment ->
                _state.value = _state.value.copy(
                    isSaving = false,
                    commentText = "",
                    comments = listOf(newComment) + _state.value.comments
                )
                // Инвалидируем кэш чтобы при следующем открытии данные обновились
                loadedTrackId = null
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

    fun voteReview(audiusTrackId: String, ratingId: String, vote: Int) {
        val token = sessionManager.getAccessToken() ?: return

        val currentVote = _state.value.myVotes[ratingId]

        // Повторное нажатие на ту же стрелку — ничего не делаем
        if (currentVote == vote) return

        // Блокируем повторные нажатия пока идёт запрос
        if (_state.value.votingIds.contains(ratingId)) return

        val delta = vote - (currentVote ?: 0)

        // 1. Оптимистичное обновление UI — мгновенно
        val optimisticVotes = _state.value.myVotes.toMutableMap()
        optimisticVotes[ratingId] = vote
        val optimisticReviews = _state.value.reviews.map { r ->
            if (r.id == ratingId) r.copy(reputation = r.reputation + delta) else r
        }.sortedByDescending { it.reputation }

        _state.value = _state.value.copy(
            myVotes = optimisticVotes,
            reviews = optimisticReviews,
            votingIds = _state.value.votingIds + ratingId
        )

        // 2. Сетевой запрос
        viewModelScope.launch {
            val result = authRepository.voteReview(token, ratingId, vote)

            result.onFailure {
                // Откат при ошибке
                val rollbackVotes = _state.value.myVotes.toMutableMap()
                if (currentVote == null) rollbackVotes.remove(ratingId) else rollbackVotes[ratingId] = currentVote
                val rollbackReviews = _state.value.reviews.map { r ->
                    if (r.id == ratingId) r.copy(reputation = r.reputation - delta) else r
                }.sortedByDescending { it.reputation }
                _state.value = _state.value.copy(myVotes = rollbackVotes, reviews = rollbackReviews)
            }

            // Снимаем блокировку
            _state.value = _state.value.copy(votingIds = _state.value.votingIds - ratingId)
        }
    }

    fun clearSuccess() {
        _state.value = _state.value.copy(savedSuccess = false)
    }

    fun submitReport(
        targetType: String,
        targetId: String,
        reason: String,
        targetContent: String? = null,
        targetUserId: String? = null,
        targetUsername: String? = null,
        trackTitle: String? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val token = sessionManager.getAccessToken() ?: return
        val reporterId = sessionManager.getUserId() ?: return
        viewModelScope.launch {
            authRepository.submitReport(
                token,
                com.example.lumisound.data.remote.SupabaseService.ReportInsert(
                    reporterId = reporterId,
                    targetType = targetType,
                    targetId = targetId,
                    reason = reason,
                    targetContent = targetContent,
                    targetUserId = targetUserId,
                    targetUsername = targetUsername,
                    trackTitle = trackTitle
                )
            ).onSuccess { onSuccess() }
             .onFailure { onError(it.message ?: "Ошибка") }
        }
    }
}

enum class ScoreCriterion(val label: String) {
    RHYME("Рифмы / Образы"),
    IMAGERY("Структура / Ритмика"),
    STRUCTURE("Реализация стиля"),
    CHARISMA("Индивидуальность / Харизма"),
    ATMOSPHERE("Атмосфера / Вайб")
}
