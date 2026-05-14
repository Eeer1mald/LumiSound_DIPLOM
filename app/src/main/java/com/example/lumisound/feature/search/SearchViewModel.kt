package com.example.lumisound.feature.search

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.example.lumisound.data.model.Track
import com.example.lumisound.data.remote.SupabaseService
import com.example.lumisound.data.repository.AuthRepository
import com.example.lumisound.data.repository.MusicRepository
import com.example.lumisound.data.local.SessionManager
import com.example.lumisound.data.cache.AppDataCache
import com.example.lumisound.data.player.PlayerStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    private val playerStateHolder: PlayerStateHolder,
    private val cache: AppDataCache,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val dataSourceFactory = DefaultHttpDataSource.Factory()
        .setConnectTimeoutMs(8000)
        .setReadTimeoutMs(8000)

    // Отдельный ExoPlayer только для фида — не трогает PlayerStateHolder
    private val feedPlayer: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        volume = 0f
    }

    private val _searchResults = MutableStateFlow<List<Track>>(emptyList())
    val searchResults: StateFlow<List<Track>> = _searchResults.asStateFlow()

    private val _artistResults = MutableStateFlow<List<com.example.lumisound.data.remote.ArtistSearchResult>>(emptyList())
    val artistResults: StateFlow<List<com.example.lumisound.data.remote.ArtistSearchResult>> = _artistResults.asStateFlow()

    private val _playlistResults = MutableStateFlow<List<SupabaseService.PlaylistResponse>>(emptyList())
    val playlistResults: StateFlow<List<SupabaseService.PlaylistResponse>> = _playlistResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _discoverFeed = MutableStateFlow<List<Track>>(emptyList())
    val discoverFeed: StateFlow<List<Track>> = _discoverFeed.asStateFlow()

    private val _followingFeed = MutableStateFlow<List<Track>>(emptyList())
    val followingFeed: StateFlow<List<Track>> = _followingFeed.asStateFlow()

    private val _feedLoading = MutableStateFlow(false)
    val feedLoading: StateFlow<Boolean> = _feedLoading.asStateFlow()

    private val _isFeedMuted = MutableStateFlow(true)
    val isFeedMuted: StateFlow<Boolean> = _isFeedMuted.asStateFlow()

    private val _isFeedPlaying = MutableStateFlow(false)
    val isFeedPlaying: StateFlow<Boolean> = _isFeedPlaying.asStateFlow()

    // Пагинация
    private var discoverPage = 0
    private var followingPage = 0
    private val pageSize = 20
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    // Кэш предзагруженных MediaSource по URL
    private val preloadedSources = mutableMapOf<String, ProgressiveMediaSource>()
    private var currentTrackUrl: String? = null

    init {
        // Сразу применяем кэш если готов — фид отображается мгновенно
        val cachedDiscover = cache.discoverFeed.value
        val cachedFollowing = cache.followingFeed.value
        if (cachedDiscover.isNotEmpty()) {
            _discoverFeed.value = cachedDiscover
            _followingFeed.value = cachedFollowing
            preloadTracks(cachedDiscover.take(5))
            // Если followingFeed пустой — загружаем в фоне, не блокируя UI
            if (cachedFollowing.isEmpty()) {
                viewModelScope.launch {
                    val following = withTimeoutOrNull(12_000L) {
                        musicRepository.getFollowingFeed(page = 0, pageSize = pageSize)
                            .getOrDefault(emptyList())
                    } ?: emptyList()
                    _followingFeed.value = following
                }
            }
        } else {
            loadFeeds()
        }
        feedPlayer.addListener(object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isFeedPlaying.value = isPlaying
            }
        })
    }

    fun loadFeeds() {
        // Сбрасываем пагинацию и кэш репозитория
        discoverPage = 0
        followingPage = 0
        musicRepository.invalidateFeeds()

        viewModelScope.launch {
            _feedLoading.value = true

            // Запускаем оба запроса параллельно с таймаутом 12 секунд
            val discoverDeferred = async {
                withTimeoutOrNull(12_000L) {
                    musicRepository.getDiscoverFeed(page = 0, pageSize = pageSize)
                        .getOrDefault(emptyList())
                } ?: emptyList()
            }
            val followingDeferred = async {
                withTimeoutOrNull(12_000L) {
                    musicRepository.getFollowingFeed(page = 0, pageSize = pageSize)
                        .getOrDefault(emptyList())
                } ?: emptyList()
            }

            val discover = discoverDeferred.await()
            val following = followingDeferred.await()

            _discoverFeed.value = discover
            _followingFeed.value = following
            _feedLoading.value = false

            // Предзагружаем первые 3 трека
            if (discover.isNotEmpty()) preloadTracks(discover.take(3))
        }
    }

    // Подгрузка следующей страницы для "Рекомендации"
    fun loadMoreDiscover() {
        if (_isLoadingMore.value) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            discoverPage++
            val more = withTimeoutOrNull(12_000L) {
                musicRepository.getDiscoverFeed(page = discoverPage, pageSize = pageSize)
                    .getOrDefault(emptyList())
            } ?: emptyList()
            if (more.isNotEmpty()) {
                _discoverFeed.value = _discoverFeed.value + more
                preloadTracks(more.take(3))
            }
            _isLoadingMore.value = false
        }
    }

    // Подгрузка следующей страницы для "Для вас"
    fun loadMoreFollowing() {
        if (_isLoadingMore.value) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            followingPage++
            val more = withTimeoutOrNull(12_000L) {
                musicRepository.getFollowingFeed(page = followingPage, pageSize = pageSize)
                    .getOrDefault(emptyList())
            } ?: emptyList()
            if (more.isNotEmpty()) {
                _followingFeed.value = _followingFeed.value + more
            }
            _isLoadingMore.value = false
        }
    }

    // Предзагрузка треков — создаём MediaSource заранее
    private fun preloadTracks(tracks: List<Track>) {
        tracks.forEach { track ->
            val url = track.previewUrl ?: return@forEach
            if (!preloadedSources.containsKey(url)) {
                val source = ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(url))
                preloadedSources[url] = source
            }
        }
    }

    // Предзагружаем следующие и предыдущие треки при свайпе
    fun preloadAround(tracks: List<Track>, currentIndex: Int) {
        val start = (currentIndex - 2).coerceAtLeast(0)
        val end = (currentIndex + 3).coerceAtMost(tracks.size)
        preloadTracks(tracks.subList(start, end))
    }

    // Играть трек — использует предзагруженный source если есть
    fun playFeedTrack(track: Track) {
        val url = track.previewUrl ?: return
        // НЕ обновляем PlayerStateHolder здесь — фид играет через отдельный feedPlayer,
        // и мини-плеер должен показывать только треки из основного плеера (AudioPlayerService).
        // PlayerStateHolder обновляется только когда пользователь явно нажимает на трек.
        if (currentTrackUrl == url) {
            feedPlayer.volume = if (_isFeedMuted.value) 0f else 1f
            if (!feedPlayer.isPlaying) feedPlayer.play()
            return
        }
        currentTrackUrl = url
        feedPlayer.volume = if (_isFeedMuted.value) 0f else 1f
        val source = preloadedSources[url]
        if (source != null) {
            feedPlayer.setMediaSource(source)
        } else {
            feedPlayer.setMediaItem(MediaItem.fromUri(url))
        }
        feedPlayer.prepare()
        // Seek to 10% сразу при загрузке — чтобы при unmute не было задержки
        feedPlayer.addListener(object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == androidx.media3.common.Player.STATE_READY) {
                    val duration = feedPlayer.duration
                    if (duration > 0 && feedPlayer.currentPosition < 1000) {
                        feedPlayer.seekTo((duration * 0.10f).toLong())
                    }
                    feedPlayer.removeListener(this)
                }
            }
        })
        feedPlayer.play()
    }

    // Toggle mute/unmute — мгновенно, только volume
    fun toggleMute() {
        val newMuted = !_isFeedMuted.value
        _isFeedMuted.value = newMuted
        feedPlayer.volume = if (newMuted) 0f else 1f
    }

    fun muteFeed() {
        _isFeedMuted.value = true
        feedPlayer.volume = 0f
    }

    fun stopFeed() {
        feedPlayer.stop()
        currentTrackUrl = null
        _isFeedPlaying.value = false
        _isFeedMuted.value = true
    }

    fun searchTracks(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _artistResults.value = emptyList()
            _playlistResults.value = emptyList()
            _error.value = null
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            // Параллельный поиск треков, артистов и плейлистов
            launch {
                // Сначала ищем в Audius, параллельно в custom_artists
                val audiusArtistJob = async {
                    withTimeoutOrNull(8_000L) {
                        musicRepository.searchArtists(query, limit = 5).getOrNull() ?: emptyList()
                    } ?: emptyList()
                }
                val customArtistJob = async {
                    withTimeoutOrNull(5_000L) {
                        authRepository.searchCustomArtists(query, limit = 3)
                    } ?: emptyList()
                }

                val audiusArtists = audiusArtistJob.await()
                val customArtists = customArtistJob.await()

                val q = query.trim().lowercase()

                // Ищем совпадение в Audius
                val audiusMatch = audiusArtists.firstOrNull { artist ->
                    val name = artist.name.lowercase()
                    name == q || name.startsWith(q) || q.length >= 3 && name.contains(q)
                }

                // Ищем совпадение в custom_artists
                val customMatch = customArtists.firstOrNull { artist ->
                    val name = artist.name.lowercase()
                    name == q || name.startsWith(q) || q.length >= 3 && name.contains(q)
                }

                // Приоритет: custom artist если есть, иначе Audius
                _artistResults.value = when {
                    customMatch != null -> listOf(
                        com.example.lumisound.data.remote.ArtistSearchResult(
                            artist = com.example.lumisound.data.remote.AudiusArtistFull(
                                id = customMatch.id,
                                name = customMatch.name,
                                bio = customMatch.bio,
                                location = customMatch.location,
                                isVerified = customMatch.isVerified,
                                profilePicture = null,
                                coverPhoto = null,
                                followerCount = null,
                                trackCount = null,
                                playlistCount = null
                            ),
                            avatarUrl = customMatch.avatarUrl
                        )
                    )
                    audiusMatch != null -> listOf(
                        com.example.lumisound.data.remote.ArtistSearchResult(artist = audiusMatch)
                    )
                    else -> emptyList()
                }
            }
            launch {
                authRepository.searchPublicPlaylists(query, limit = 8)
                    .let { _playlistResults.value = it }
            }
            // Поиск custom_tracks из Supabase (треки добавленные через admin-панель)
            musicRepository.searchTracks(query, limit = 20)
                .onSuccess { audiusTracks ->
                    // Конвертируем custom_tracks в Track и добавляем в начало результатов
                    val customTracks = authRepository.searchCustomTracks(query, limit = 10)
                        .map { ct ->
                            com.example.lumisound.data.model.Track(
                                id = "custom_${ct.id}",
                                name = ct.title,
                                artist = ct.artistName ?: "Unknown",
                                artistId = ct.artistId,
                                artistImageUrl = ct.artistAvatarUrl,
                                imageUrl = ct.coverUrl,
                                hdImageUrl = ct.coverUrl,
                                previewUrl = ct.audioUrl,
                                genre = ct.genre,
                                playCount = ct.playCount,
                                duration = ct.duration
                            )
                        }
                    _searchResults.value = customTracks + audiusTracks
                    _isLoading.value = false
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Ошибка при поиске треков"
                    _searchResults.value = emptyList()
                    _isLoading.value = false
                }
        }
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        feedPlayer.release()
        preloadedSources.clear()
    }
}
