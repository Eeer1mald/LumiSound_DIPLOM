package com.example.lumisound.feature.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lumisound.data.model.Track
import com.example.lumisound.data.player.AudioPlayerService
import com.example.lumisound.data.repository.AuthRepository
import com.example.lumisound.data.local.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val audioPlayerService: AudioPlayerService,
    private val playerStateHolder: com.example.lumisound.data.player.PlayerStateHolder,
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
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
    private var trackPlayTrackingJob: Job? = null
    private var lastTrackedTrackId: String? = null
    private var hasTrackedCurrentTrack = false
    
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
                        // Отслеживаем прослушивание когда трек закончился
                        val currentTrack = _currentTrack.value
                        val currentPosition = _currentPosition.value
                        val currentDuration = _duration.value
                        if (currentTrack != null && currentPosition > 0) {
                            trackPlayIfNeeded(currentTrack, currentPosition, currentDuration)
                        }
                    }
                }
            }
        })
    }
    
    fun playTrack(track: Track) {
        // Сбрасываем отслеживание для нового трека
        hasTrackedCurrentTrack = false
        lastTrackedTrackId = null
        stopTrackPlayTracking()
        
        _currentTrack.value = track
        playerStateHolder.setCurrentTrack(track)
        track.previewUrl?.let { url ->
            audioPlayerService.play(url)
            _isPlaying.value = true
            startPositionUpdates()
            startTrackPlayTracking(track)
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
    
    private fun startTrackPlayTracking(track: Track) {
        stopTrackPlayTracking()
        hasTrackedCurrentTrack = false
        lastTrackedTrackId = null
        
        trackPlayTrackingJob = viewModelScope.launch {
            // Отслеживаем прослушивание в реальном времени
            while (_isPlaying.value && _currentTrack.value?.id == track.id) {
                delay(5000) // Проверяем каждые 5 секунд
                
                val duration = _duration.value
                val position = _currentPosition.value
                
                // Проверяем, что трек проигрался достаточно (30 секунд или 50% трека)
                val shouldTrack = if (duration > 0 && duration > 1000) {
                    // Если проиграно больше 50% трека или больше 30 секунд
                    position >= duration * 0.5 || position >= 30000
                } else {
                    // Если длительность неизвестна или очень короткая, отслеживаем через 30 секунд
                    position >= 30000
                }
                
                if (shouldTrack && !hasTrackedCurrentTrack && lastTrackedTrackId != track.id) {
                    trackPlayIfNeeded(track, position, duration)
                    break // Останавливаем отслеживание после первого засчитывания
                }
            }
        }
    }
    
    private fun stopTrackPlayTracking() {
        trackPlayTrackingJob?.cancel()
        trackPlayTrackingJob = null
    }
    
    private fun trackPlayIfNeeded(track: Track, position: Long, duration: Long) {
        val accessToken = sessionManager.getAccessToken() ?: return
        
        if (hasTrackedCurrentTrack || lastTrackedTrackId == track.id) {
            return // Уже отследили этот трек
        }
        
        hasTrackedCurrentTrack = true
        lastTrackedTrackId = track.id
        
        viewModelScope.launch {
            try {
                Log.d("PlayerViewModel", "Отслеживаем прослушивание: ${track.name} (${track.artist}), позиция: ${position}ms, длительность: ${duration}ms")
                
                // Увеличиваем счетчик прослушиваний трека (это автоматически добавит трек в favorite_tracks)
                authRepository.incrementTrackPlayCount(
                    accessToken = accessToken,
                    trackId = track.id,
                    trackTitle = track.name,
                    trackArtist = track.artist,
                    trackCoverUrl = track.imageUrl,
                    trackPreviewUrl = track.previewUrl
                ).onSuccess {
                    Log.d("PlayerViewModel", "✅ Счетчик прослушиваний трека увеличен: ${track.name}")
                }.onFailure { exception ->
                    Log.e("PlayerViewModel", "❌ Ошибка увеличения счетчика трека: ${exception.message}", exception)
                }
                
                    // Увеличиваем счетчик прослушиваний артиста (это автоматически добавит артиста в favorite_artists)
                    authRepository.incrementArtistPlayCount(
                        accessToken = accessToken,
                        artistId = track.artistId ?: track.artist, // Используем artistId если есть, иначе имя
                        artistName = track.artist,
                        artistImageUrl = track.artistImageUrl // Используем фотографию артиста
                    ).onSuccess {
                    Log.d("PlayerViewModel", "✅ Счетчик прослушиваний артиста увеличен: ${track.artist}")
                }.onFailure { exception ->
                    Log.e("PlayerViewModel", "❌ Ошибка увеличения счетчика артиста: ${exception.message}", exception)
                }
                
                // Добавляем в историю прослушиваний
                authRepository.addTrackHistory(
                    accessToken = accessToken,
                    track = com.example.lumisound.data.remote.SupabaseService.TrackHistoryInsert(
                        trackId = track.id,
                        trackTitle = track.name,
                        trackArtist = track.artist,
                        trackArtistId = track.artist
                    )
                )
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "❌ Ошибка отслеживания прослушивания: ${e.message}", e)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopPositionUpdates()
        stopTrackPlayTracking()
    }
}
