package com.example.lumisound

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.lumisound.data.cache.AppDataCache
import com.example.lumisound.data.cache.DiskCache
import com.example.lumisound.data.local.SessionManager
import com.example.lumisound.data.repository.AuthRepository
import com.example.lumisound.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Запускается при старте приложения.
 *
 * Стратегия загрузки:
 * 1. Мгновенно: читаем DiskCache (SharedPreferences) → заполняем AppDataCache → UI показывает данные
 * 2. Фоново: делаем сетевые запросы → обновляем AppDataCache + DiskCache
 *
 * TTL контролируется в DiskCache — устаревшие данные не читаются с диска.
 */
@HiltViewModel
class AppPreloadViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val musicRepository: MusicRepository,
    private val sessionManager: SessionManager,
    private val playerStateHolder: com.example.lumisound.data.player.PlayerStateHolder,
    private val cache: AppDataCache,
    private val diskCache: DiskCache,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isPreloaded = MutableStateFlow(false)
    val isPreloaded: StateFlow<Boolean> = _isPreloaded.asStateFlow()

    init {
        preloadAll()
    }

    fun triggerPreload() {
        if (playerStateHolder.currentTrack.value != null && cache.isReady.value) return
        preloadAll()
    }

    private fun preloadAll() {
        viewModelScope.launch {
            val refreshed = runCatching { authRepository.refreshTokenIfNeeded() }.getOrNull()
            val validToken = refreshed ?: sessionManager.getAccessToken() ?: return@launch

            // Инвалидируем кэш если сменился пользователь
            val userId = sessionManager.getUserId()
            if (userId != null) diskCache.validateUser(userId)

            // ── Шаг 1: МГНОВЕННО загружаем с диска ───────────────────────────
            loadFromDisk()

            // ── Шаг 2: ФОНОВО обновляем из сети ──────────────────────────────
            refreshFromNetwork(validToken)
        }
    }

    /**
     * Читает все данные с диска и заполняет AppDataCache.
     * Выполняется синхронно — данные доступны сразу после вызова.
     */
    private fun loadFromDisk() {
        var anyLoaded = false

        // Последний играющий трек — восстанавливаем мгновенно без сети
        if (playerStateHolder.currentTrack.value == null) {
            val userId = sessionManager.getUserId()
            if (userId != null) {
                diskCache.loadLastTrack(userId)?.let {
                    playerStateHolder.setCurrentTrack(it)
                    anyLoaded = true
                }
            }
        }

        diskCache.loadProfile()?.let { cache.setProfile(it); anyLoaded = true }
        diskCache.loadMyPlaylists()?.let { cache.setMyPlaylists(it); anyLoaded = true }
        diskCache.loadTopPlaylists()?.let { cache.setTopPlaylists(it); anyLoaded = true }
        diskCache.loadRecommendedPlaylists()?.let { cache.setRecommendedPlaylists(it); anyLoaded = true }
        diskCache.loadLikedPlaylistIds()?.let { cache.setLikedPlaylistIds(it); anyLoaded = true }
        diskCache.loadRecentTracks()?.let { cache.setRecentTracks(it); anyLoaded = true }
        diskCache.loadTopTracks()?.let { cache.setTopTracks(it); anyLoaded = true }
        diskCache.loadFavoriteTracks()?.let { cache.setFavoriteTracks(it); anyLoaded = true }
        diskCache.loadFavoriteArtists()?.let { cache.setFavoriteArtists(it); anyLoaded = true }
        diskCache.loadMyRatings()?.let { cache.setMyRatings(it); anyLoaded = true }
        diskCache.loadMyComments()?.let { cache.setMyComments(it); anyLoaded = true }
        diskCache.loadBestReviews()?.let { cache.setBestReviews(it); anyLoaded = true }
        diskCache.loadDiscoverFeed()?.let { cache.setDiscoverFeed(it); anyLoaded = true }
        diskCache.loadFollowingFeed()?.let { cache.setFollowingFeed(it); anyLoaded = true }

        if (anyLoaded) {
            // Кэш с диска готов — UI может показывать данные немедленно
            cache.markReady()
            _isPreloaded.value = true
        }
    }

    /**
     * Обновляет данные из сети и сохраняет на диск.
     * Запускается в фоне, не блокирует UI.
     */
    private fun refreshFromNetwork(validToken: String) {
        viewModelScope.launch {
            coroutineScope {
                // ── Волна 1: критичные данные ─────────────────────────────────
                val historyJob    = async { runCatching { authRepository.getTrackHistory(validToken, limit = 1) }.getOrNull() }
                val favTracksJob  = async { authRepository.getFavoriteTracks(validToken, limit = 20, orderByPlayCount = true).getOrNull() }
                val quickJob      = async { musicRepository.searchTracks("popular", limit = 5).getOrNull() }
                val discoverJob   = async { musicRepository.getDiscoverFeed(page = 0, pageSize = 20).getOrNull() }
                val followingJob  = async { musicRepository.getFollowingFeed(page = 0, pageSize = 20).getOrNull() }

                // ── Волна 2: данные для всех вкладок ─────────────────────────
                val profileJob      = async { authRepository.getProfile(validToken).getOrNull() }
                val ratingsJob      = async { runCatching { authRepository.getMyRatings(validToken, limit = 200) }.getOrNull() }
                val commentsJob     = async { runCatching { authRepository.getMyComments(validToken, limit = 200) }.getOrNull() }
                val favArtistsJob   = async { authRepository.getFavoriteArtists(validToken, limit = 20).getOrNull() }
                val recentJob       = async { authRepository.getFavoriteTracks(validToken, limit = 10, orderByPlayCount = false).getOrNull() }
                val topTracksJob    = async { authRepository.getFavoriteTracks(validToken, limit = 10, orderByPlayCount = true).getOrNull() }
                val myPlaylistsJob  = async { runCatching { authRepository.getMyPlaylists(validToken) }.getOrNull() }
                val topPlaylistsJob = async { runCatching { authRepository.getPublicPlaylists(validToken, limit = 20) }.getOrNull() }
                val likedIdsJob     = async { runCatching { authRepository.getMyLikedPlaylistIds(validToken) }.getOrNull() }

                // ── Обрабатываем волну 1 ──────────────────────────────────────
                val history     = historyJob.await()
                val favTracks   = favTracksJob.await()
                val quickTracks = quickJob.await()
                val discover    = discoverJob.await()
                val following   = followingJob.await()

                // Начальный трек для мини-плеера — только если не восстановлен с диска
                if (playerStateHolder.currentTrack.value == null) {
                    val lastPlayed = history?.getOrNull()?.firstOrNull()
                    when {
                        lastPlayed != null -> {
                            // Обогащаем трек из истории данными Audius (обложка, artistId и т.д.)
                            val enriched = musicRepository.getTrackById(lastPlayed.trackId)
                            val track = enriched ?: com.example.lumisound.data.model.Track(
                                id = lastPlayed.trackId,
                                name = lastPlayed.trackTitle,
                                artist = lastPlayed.trackArtist,
                                previewUrl = musicRepository.getStreamUrl(lastPlayed.trackId)
                            )
                            playerStateHolder.setCurrentTrack(track)
                            // Сохраняем обогащённый трек на диск — будет доступен при следующем запуске
                            val userId = sessionManager.getUserId()
                            if (userId != null) diskCache.saveLastTrack(track, userId)
                        }
                        else -> {
                            val fromFav = favTracks?.firstOrNull { !it.trackPreviewUrl.isNullOrBlank() }
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
                                quickTracks?.firstOrNull { !it.previewUrl.isNullOrBlank() }
                                    ?.let { playerStateHolder.setCurrentTrack(it) }
                            }
                        }
                    }
                }

                // Обновляем кэш + сохраняем на диск
                discover?.let { cache.setDiscoverFeed(it); diskCache.saveDiscoverFeed(it) }
                following?.let { cache.setFollowingFeed(it); diskCache.saveFollowingFeed(it) }
                favTracks?.let { cache.setFavoriteTracks(it); diskCache.saveFavoriteTracks(it) }

                // ── Обрабатываем волну 2 ──────────────────────────────────────
                val profile      = profileJob.await()
                val ratings      = ratingsJob.await()
                val comments     = commentsJob.await()
                val favArtists   = favArtistsJob.await()
                val recentTracks = recentJob.await()
                val topTracks    = topTracksJob.await()
                val myPlaylists  = myPlaylistsJob.await()
                val topPlaylists = topPlaylistsJob.await()
                val likedIds     = likedIdsJob.await()

                profile?.let      { cache.setProfile(it);           diskCache.saveProfile(it) }
                ratings?.let      { cache.setMyRatings(it);         diskCache.saveMyRatings(it) }
                comments?.let     { cache.setMyComments(it);        diskCache.saveMyComments(it) }
                favArtists?.let   { cache.setFavoriteArtists(it);   diskCache.saveFavoriteArtists(it) }
                recentTracks?.let { cache.setRecentTracks(it);      diskCache.saveRecentTracks(it) }
                topTracks?.let    { cache.setTopTracks(it);         diskCache.saveTopTracks(it) }
                myPlaylists?.let  { cache.setMyPlaylists(it);       diskCache.saveMyPlaylists(it) }
                topPlaylists?.let { cache.setTopPlaylists(it);      diskCache.saveTopPlaylists(it) }
                likedIds?.let     { cache.setLikedPlaylistIds(it);  diskCache.saveLikedPlaylistIds(it) }

                // Рекомендованные плейлисты
                val artistNames = favArtists?.map { it.artistName } ?: emptyList()
                val recommended = runCatching {
                    authRepository.getRecommendedPlaylists(validToken, artistNames, limit = 20)
                }.getOrDefault(emptyList())
                cache.setRecommendedPlaylists(recommended)
                diskCache.saveRecommendedPlaylists(recommended)

                // Лучшие рецензии
                val favTrackIds = favTracks?.map { it.trackId } ?: emptyList()
                val bestReviews = runCatching {
                    authRepository.getBestReviewsForFavorites(validToken, favTrackIds, artistNames, limit = 30)
                }.getOrDefault(emptyList())
                cache.setBestReviews(bestReviews)
                diskCache.saveBestReviews(bestReviews)

                cache.markReady()
            }

            // Предзагружаем изображения в фоне
            preloadImages()
            _isPreloaded.value = true
        }
    }

    private fun preloadImages() {
        viewModelScope.launch {
            val imageLoader = ImageLoader.Builder(context).build()
            val urls = mutableListOf<String>()

            cache.profile.value?.avatarUrl?.let { urls.add(it) }
            cache.favoriteTracks.value.mapNotNull { it.trackCoverUrl }.forEach { urls.add(it) }
            cache.favoriteArtists.value.mapNotNull { it.artistImageUrl }.forEach { urls.add(it) }
            cache.discoverFeed.value.take(15).mapNotNull { it.imageUrl ?: it.hdImageUrl }.forEach { urls.add(it) }
            cache.myPlaylists.value.mapNotNull { it.coverUrl }.forEach { urls.add(it) }
            cache.topPlaylists.value.take(10).mapNotNull { it.coverUrl }.forEach { urls.add(it) }

            urls.distinct().filter { it.isNotBlank() }.forEach { url ->
                imageLoader.enqueue(
                    ImageRequest.Builder(context)
                        .data(url)
                        .memoryCacheKey(url)
                        .diskCacheKey(url)
                        .build()
                )
            }
        }
    }
}
