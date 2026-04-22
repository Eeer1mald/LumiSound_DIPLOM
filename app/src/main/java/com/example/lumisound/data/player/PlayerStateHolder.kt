package com.example.lumisound.data.player

import com.example.lumisound.data.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerStateHolder @Inject constructor() {
    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _reviewTrack = MutableStateFlow<Track?>(null)
    val reviewTrack: StateFlow<Track?> = _reviewTrack.asStateFlow()

    fun setReviewTrack(track: Track) {
        _reviewTrack.value = track
    }

    var autoplayEnabled: Boolean = true
    var showFloatingComments: Boolean = true

    private val _playlist = MutableStateFlow<List<Track>>(emptyList())
    val playlist: StateFlow<List<Track>> = _playlist.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    /** true если есть предыдущий трек в плейлисте */
    val hasPrevious: Boolean get() = _currentIndex.value > 0 && _playlist.value.isNotEmpty()

    /** true если есть следующий трек в плейлисте */
    val hasNext: Boolean get() = _currentIndex.value < _playlist.value.size - 1

    fun setPlaylist(tracks: List<Track>, startIndex: Int = 0) {
        _playlist.value = tracks
        _currentIndex.value = startIndex.coerceIn(0, tracks.size - 1)
        if (tracks.isNotEmpty()) {
            _currentTrack.value = tracks[_currentIndex.value]
        }
    }

    fun setCurrentTrack(track: Track) {
        _currentTrack.value = track
    }

    fun getNextTrack(): Track? {
        val current = _currentIndex.value
        val playlist = _playlist.value
        if (current < playlist.size - 1) {
            _currentIndex.value = current + 1
            _currentTrack.value = playlist[_currentIndex.value]
            return _currentTrack.value
        }
        return null
    }

    fun getPreviousTrack(): Track? {
        val current = _currentIndex.value
        val playlist = _playlist.value
        if (current > 0) {
            _currentIndex.value = current - 1
            _currentTrack.value = playlist[_currentIndex.value]
            return _currentTrack.value
        }
        return null
    }
}
