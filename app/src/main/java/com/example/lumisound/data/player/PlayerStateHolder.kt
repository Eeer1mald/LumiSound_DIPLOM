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
    
    private val _playlist = MutableStateFlow<List<Track>>(emptyList())
    val playlist: StateFlow<List<Track>> = _playlist.asStateFlow()
    
    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()
    
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
