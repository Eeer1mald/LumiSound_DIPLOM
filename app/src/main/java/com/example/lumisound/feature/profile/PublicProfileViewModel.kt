package com.example.lumisound.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lumisound.data.remote.SupabaseService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PublicProfileUiState(
    val isLoading: Boolean = true,
    val isPrivate: Boolean = false,
    val username: String = "",
    val bio: String? = null,
    val avatarUrl: String? = null,
    val favoriteTracks: List<SupabaseService.FavoriteTrackResponse> = emptyList(),
    val favoriteArtists: List<SupabaseService.FavoriteArtistResponse> = emptyList(),
    val topReviews: List<SupabaseService.TrackRatingResponse> = emptyList(),
    val ratingsCount: Int = 0,
    val reviewsCount: Int = 0
)

@HiltViewModel
class PublicProfileViewModel @Inject constructor(
    private val supabase: SupabaseService
) : ViewModel() {

    private val _state = MutableStateFlow(PublicProfileUiState())
    val state: StateFlow<PublicProfileUiState> = _state.asStateFlow()

    fun load(userId: String, fallbackUsername: String, fallbackAvatarUrl: String?) {
        viewModelScope.launch {
            _state.value = PublicProfileUiState(isLoading = true)

            // Загружаем профиль и данные параллельно
            val profileJob = async { supabase.getProfileById(userId) }
            // Для публичных данных используем anon-доступ (пустой токен — SupabaseService использует anonKey)
            val tracksJob = async { supabase.getFavoriteTracksForUser("anon", userId, limit = 10) }
            val artistsJob = async { supabase.getFavoriteArtistsForUser(userId, limit = 10) }
            val reviewsJob = async { supabase.getRatingsByUserId(userId, limit = 5) }

            val profile = profileJob.await()
            val tracks = tracksJob.await()
            val artists = artistsJob.await()
            val reviews = reviewsJob.await()

            // Проверяем is_public — null считаем публичным (старые профили без поля)
            val isPrivate = profile?.isPublic == false

            _state.value = PublicProfileUiState(
                isLoading = false,
                isPrivate = isPrivate,
                username = profile?.username ?: fallbackUsername,
                bio = profile?.bio,
                avatarUrl = profile?.avatarUrl ?: fallbackAvatarUrl,
                favoriteTracks = if (isPrivate) emptyList() else tracks,
                favoriteArtists = if (isPrivate) emptyList() else artists,
                topReviews = if (isPrivate) emptyList() else reviews,
                ratingsCount = if (isPrivate) 0 else reviews.size,
                reviewsCount = if (isPrivate) 0 else reviews.count { !it.review.isNullOrBlank() }
            )
        }
    }
}
