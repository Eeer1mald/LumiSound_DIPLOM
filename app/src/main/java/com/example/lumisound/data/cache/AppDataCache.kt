package com.example.lumisound.data.cache

import com.example.lumisound.data.model.Track
import com.example.lumisound.data.remote.SupabaseService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Синглтон-кэш данных приложения.
 * Заполняется при старте в AppPreloadViewModel и используется всеми ViewModels
 * чтобы избежать повторных сетевых запросов при переключении вкладок.
 */
@Singleton
class AppDataCache @Inject constructor() {

    // ── Профиль ──────────────────────────────────────────────────────────────
    private val _profile = MutableStateFlow<SupabaseService.ProfileResponse?>(null)
    val profile: StateFlow<SupabaseService.ProfileResponse?> = _profile.asStateFlow()

    // ── Плейлисты ─────────────────────────────────────────────────────────────
    private val _myPlaylists = MutableStateFlow<List<SupabaseService.PlaylistResponse>>(emptyList())
    val myPlaylists: StateFlow<List<SupabaseService.PlaylistResponse>> = _myPlaylists.asStateFlow()

    private val _topPlaylists = MutableStateFlow<List<SupabaseService.PlaylistResponse>>(emptyList())
    val topPlaylists: StateFlow<List<SupabaseService.PlaylistResponse>> = _topPlaylists.asStateFlow()

    private val _recommendedPlaylists = MutableStateFlow<List<SupabaseService.PlaylistResponse>>(emptyList())
    val recommendedPlaylists: StateFlow<List<SupabaseService.PlaylistResponse>> = _recommendedPlaylists.asStateFlow()

    private val _likedPlaylistIds = MutableStateFlow<Set<String>>(emptySet())
    val likedPlaylistIds: StateFlow<Set<String>> = _likedPlaylistIds.asStateFlow()

    // ── Треки ─────────────────────────────────────────────────────────────────
    private val _recentTracks = MutableStateFlow<List<SupabaseService.FavoriteTrackResponse>>(emptyList())
    val recentTracks: StateFlow<List<SupabaseService.FavoriteTrackResponse>> = _recentTracks.asStateFlow()

    private val _topTracks = MutableStateFlow<List<SupabaseService.FavoriteTrackResponse>>(emptyList())
    val topTracks: StateFlow<List<SupabaseService.FavoriteTrackResponse>> = _topTracks.asStateFlow()

    // ── Рейтинги и комментарии ────────────────────────────────────────────────
    private val _myRatings = MutableStateFlow<List<SupabaseService.TrackRatingResponse>>(emptyList())
    val myRatings: StateFlow<List<SupabaseService.TrackRatingResponse>> = _myRatings.asStateFlow()

    private val _myComments = MutableStateFlow<List<SupabaseService.TrackCommentResponse>>(emptyList())
    val myComments: StateFlow<List<SupabaseService.TrackCommentResponse>> = _myComments.asStateFlow()

    private val _bestReviews = MutableStateFlow<List<SupabaseService.TrackRatingResponse>>(emptyList())
    val bestReviews: StateFlow<List<SupabaseService.TrackRatingResponse>> = _bestReviews.asStateFlow()

    // ── Избранное ─────────────────────────────────────────────────────────────
    private val _favoriteTracks = MutableStateFlow<List<SupabaseService.FavoriteTrackResponse>>(emptyList())
    val favoriteTracks: StateFlow<List<SupabaseService.FavoriteTrackResponse>> = _favoriteTracks.asStateFlow()

    private val _favoriteArtists = MutableStateFlow<List<SupabaseService.FavoriteArtistResponse>>(emptyList())
    val favoriteArtists: StateFlow<List<SupabaseService.FavoriteArtistResponse>> = _favoriteArtists.asStateFlow()

    // ── Фид ───────────────────────────────────────────────────────────────────
    private val _discoverFeed = MutableStateFlow<List<Track>>(emptyList())
    val discoverFeed: StateFlow<List<Track>> = _discoverFeed.asStateFlow()

    private val _followingFeed = MutableStateFlow<List<Track>>(emptyList())
    val followingFeed: StateFlow<List<Track>> = _followingFeed.asStateFlow()

    // ── Статус загрузки ───────────────────────────────────────────────────────
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    // ── Setters ───────────────────────────────────────────────────────────────
    fun setProfile(value: SupabaseService.ProfileResponse?) { _profile.value = value }
    fun setMyPlaylists(value: List<SupabaseService.PlaylistResponse>) { _myPlaylists.value = value }
    fun setTopPlaylists(value: List<SupabaseService.PlaylistResponse>) { _topPlaylists.value = value }
    fun setRecommendedPlaylists(value: List<SupabaseService.PlaylistResponse>) { _recommendedPlaylists.value = value }
    fun setLikedPlaylistIds(value: Set<String>) { _likedPlaylistIds.value = value }
    fun setRecentTracks(value: List<SupabaseService.FavoriteTrackResponse>) { _recentTracks.value = value }
    fun setTopTracks(value: List<SupabaseService.FavoriteTrackResponse>) { _topTracks.value = value }
    fun setMyRatings(value: List<SupabaseService.TrackRatingResponse>) { _myRatings.value = value }
    fun setMyComments(value: List<SupabaseService.TrackCommentResponse>) { _myComments.value = value }
    fun setBestReviews(value: List<SupabaseService.TrackRatingResponse>) { _bestReviews.value = value }
    fun setFavoriteTracks(value: List<SupabaseService.FavoriteTrackResponse>) { _favoriteTracks.value = value }
    fun setFavoriteArtists(value: List<SupabaseService.FavoriteArtistResponse>) { _favoriteArtists.value = value }
    fun setDiscoverFeed(value: List<Track>) { _discoverFeed.value = value }
    fun setFollowingFeed(value: List<Track>) { _followingFeed.value = value }
    fun markReady() { _isReady.value = true }

    fun invalidate() {
        _isReady.value = false
        _myPlaylists.value = emptyList()
        _topPlaylists.value = emptyList()
        _recommendedPlaylists.value = emptyList()
        _myRatings.value = emptyList()
        _myComments.value = emptyList()
        _bestReviews.value = emptyList()
    }
}
