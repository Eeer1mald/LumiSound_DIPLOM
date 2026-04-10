package com.example.lumisound.feature.search

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.example.lumisound.data.model.Track
import com.example.lumisound.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
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

    // Кэш предзагруженных MediaSource по URL
    private val preloadedSources = mutableMapOf<String, ProgressiveMediaSource>()
    private var currentTrackUrl: String? = null

    init {
        loadFeeds()
        feedPlayer.addListener(object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isFeedPlaying.value = isPlaying
            }
        })
    }

    fun loadFeeds() {
        viewModelScope.launch {
            _feedLoading.value = true
            val discover = musicRepository.getDiscoverFeed(30)
            val following = musicRepository.getFollowingFeed(30)
            _discoverFeed.value = discover.getOrDefault(emptyList())
            _followingFeed.value = following.getOrDefault(emptyList())
            _feedLoading.value = false
            // Предзагружаем первые 3 трека
            preloadTracks(_discoverFeed.value.take(3))
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
        // Seek уже сделан при загрузке трека — никакой задержки
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
            _error.value = null
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            musicRepository.searchTracks(query, limit = 20)
                .onSuccess { tracks ->
                    _searchResults.value = tracks
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
