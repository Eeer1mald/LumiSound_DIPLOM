package com.example.lumisound.feature.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lumisound.data.model.Track
import com.example.lumisound.data.player.AudioPlayerService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val audioPlayerService: AudioPlayerService,
    private val playerStateHolder: com.example.lumisound.data.player.PlayerStateHolder
) : ViewModel() {
    
    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()
    
    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()
    
    private var positionUpdateJob: Job? = null
    
    init {
        val player = audioPlayerService.getPlayer()
        player?.addListener(object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startPositionUpdates()
                } else {
                    stopPositionUpdates()
                }
            }
            
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    androidx.media3.common.Player.STATE_READY -> {
                        _duration.value = audioPlayerService.getDuration()
                    }
                    androidx.media3.common.Player.STATE_ENDED -> {
                        _isPlaying.value = false
                        stopPositionUpdates()
                    }
                }
            }
        })
    }
    
    fun playTrack(track: Track) {
        _currentTrack.value = track
        playerStateHolder.setCurrentTrack(track)
        track.previewUrl?.let { url ->
            audioPlayerService.play(url)
            _isPlaying.value = true
            startPositionUpdates()
        }
    }
    
    fun playPlaylist(tracks: List<Track>, startIndex: Int = 0) {
        playerStateHolder.setPlaylist(tracks, startIndex)
        val track = tracks.getOrNull(startIndex)
        track?.let { playTrack(it) }
    }
    
    fun nextTrack() {
        val nextTrack = playerStateHolder.getNextTrack()
        nextTrack?.let { playTrack(it) }
    }
    
    fun previousTrack() {
        val prevTrack = playerStateHolder.getPreviousTrack()
        prevTrack?.let { playTrack(it) }
    }
    
    fun togglePlayPause() {
        if (_isPlaying.value) {
            audioPlayerService.pause()
            _isPlaying.value = false
            stopPositionUpdates()
        } else {
            audioPlayerService.resume()
            _isPlaying.value = true
            startPositionUpdates()
        }
    }
    
    fun seekTo(positionMs: Long) {
        audioPlayerService.seekTo(positionMs)
        _currentPosition.value = positionMs
    }
    
    fun setVolume(volume: Float) {
        audioPlayerService.setVolume(volume)
    }
    
    fun stop() {
        audioPlayerService.stop()
        _isPlaying.value = false
        stopPositionUpdates()
        _currentPosition.value = 0L
    }
    
    private fun startPositionUpdates() {
        stopPositionUpdates()
        positionUpdateJob = viewModelScope.launch {
            while (_isPlaying.value) {
                _currentPosition.value = audioPlayerService.getCurrentPosition()
                _duration.value = audioPlayerService.getDuration()
                delay(100)
            }
        }
    }
    
    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }
    
    override fun onCleared() {
        super.onCleared()
        stopPositionUpdates()
    }
}
