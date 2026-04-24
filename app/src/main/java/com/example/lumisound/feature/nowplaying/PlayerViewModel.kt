package com.example.lumisound.feature.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lumisound.data.model.Track
import com.example.lumisound.data.player.AudioPlayerService
import com.example.lumisound.data.repository.AuthRepository
import com.example.lumisound.data.repository.MusicRepository
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
    val audioPlayerService: AudioPlayerService,
    val playerStateHolder: com.example.lumisound.data.player.PlayerStateHolder,
    private val authRepository: AuthRepository,
    private val musicRepository: MusicRepository,
    private val sessionManager: SessionManager,
    val audiusApi: com.example.lumisound.data.remote.AudiusApiService
) : ViewModel() {
    
    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()
    
    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _avgScore = MutableStateFlow<Float?>(null)
    val avgScore: StateFlow<Float?> = _avgScore.asStateFlow()

    /** Показывать плавающие комментарии — читается из PlayerStateHolder */
    val showFloatingComments: Boolean get() = playerStateHolder.showFloatingComments

    /** Есть ли предыдущий трек */
    val hasPrevious: Boolean get() = playerStateHolder.hasPrevious
    
    private var positionUpdateJob: Job? = null
    private var trackPlayTrackingJob: Job? = null
    private var lastTrackedTrackId: String? = null
    private var hasTrackedCurrentTrack = false
    
    init {
        // Синхронизируем _currentTrack с playerStateHolder — чтобы трек установленный
        // из AppPreloadViewModel сразу отображался в мини-плеере
        viewModelScope.launch {
            playerStateHolder.currentTrack.collect { track ->
                if (track != null && _currentTrack.value?.id != track.id) {
                    _currentTrack.value = track
                    // Загружаем среднюю оценку для нового трека
                    loadAvgScore(track.id)
                }
            }
        }

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

            override fun onMediaItemTransition(
                mediaItem: androidx.media3.common.MediaItem?,
                reason: Int
            ) {
                // Если автопереход (не ручной) и autoplay выключен — останавливаем
                if (reason == androidx.media3.common.Player.MEDIA_ITEM_TRANSITION_REASON_AUTO
                    && !playerStateHolder.autoplayEnabled) {
                    audioPlayerService.pause()
                    return
                }
                // ExoPlayer перешёл к следующему треку (авто или вручную)
                val newIdx = audioPlayerService.getCurrentMediaIndex()
                val playlist = playerStateHolder.playlist.value
                val newTrack = playlist.getOrNull(newIdx) ?: return
                if (_currentTrack.value?.id != newTrack.id) {
                    playerStateHolder.setPlaylist(playlist, newIdx)
                    _currentTrack.value = newTrack
                    playerStateHolder.setCurrentTrack(newTrack)
                    _currentPosition.value = 0L
                    _duration.value = 0L
                    loadAvgScore(newTrack.id)
                    audioPlayerService.updateMediaMetadata(newTrack.name, newTrack.artist, newTrack.imageUrl ?: newTrack.hdImageUrl)
                    hasTrackedCurrentTrack = false
                    lastTrackedTrackId = null
                    stopTrackPlayTracking()
                    startTrackPlayTracking(newTrack)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    androidx.media3.common.Player.STATE_READY -> {
                        val dur = audioPlayerService.getDuration()
                        if (dur > 0) _duration.value = dur
                    }
                    androidx.media3.common.Player.STATE_ENDED -> {
                        _isPlaying.value = false
                        stopPositionUpdates()
                        val currentTrack = _currentTrack.value
                        val currentPosition = _currentPosition.value
                        val currentDuration = _duration.value
                        if (currentTrack != null && currentPosition > 0) {
                            trackPlayIfNeeded(currentTrack, currentPosition, currentDuration)
                        }
                        // Автовоспроизведение следующего трека
                        if (playerStateHolder.autoplayEnabled) {
                            nextTrack()
                        }
                    }
                }
            }
        })
    }
    
    fun playTrack(track: Track) {
        hasTrackedCurrentTrack = false
        lastTrackedTrackId = null
        stopTrackPlayTracking()

        _currentTrack.value = track
        _currentPosition.value = 0L
        _duration.value = 0L
        playerStateHolder.setCurrentTrack(track)
        loadAvgScore(track.id)
        // Обновляем метаданные для уведомления и запускаем сервис
        audioPlayerService.startMediaService()
        audioPlayerService.updateMediaMetadata(track.name, track.artist, track.imageUrl ?: track.hdImageUrl)
        track.previewUrl?.let { url ->
            // Если трек уже в очереди ExoPlayer — просто переходим к нему
            val playlist = playerStateHolder.playlist.value
            val idx = playlist.indexOfFirst { it.id == track.id }
            if (idx >= 0 && audioPlayerService.getQueueSize() == playlist.size) {
                // Очередь уже установлена — seekTo нужному индексу
                val player = audioPlayerService.getPlayer()
                if (player != null && player.currentMediaItemIndex != idx) {
                    player.seekTo(idx, 0L)
                    player.play()
                } else {
                    audioPlayerService.resume()
                }
            } else {
                // Одиночный трек — играем напрямую
                audioPlayerService.play(url)
            }
            _isPlaying.value = true
            startPositionUpdates()
            startTrackPlayTracking(track)
        }
    }

    fun playPlaylist(tracks: List<Track>, startIndex: Int = 0) {
        playerStateHolder.setPlaylist(tracks, startIndex)
        val track = tracks.getOrNull(startIndex) ?: return

        hasTrackedCurrentTrack = false
        lastTrackedTrackId = null
        stopTrackPlayTracking()

        _currentTrack.value = track
        _currentPosition.value = 0L
        _duration.value = 0L
        playerStateHolder.setCurrentTrack(track)
        loadAvgScore(track.id)
        // Обновляем метаданные для уведомления и запускаем сервис
        audioPlayerService.startMediaService()
        audioPlayerService.updateMediaMetadata(track.name, track.artist, track.imageUrl ?: track.hdImageUrl)

        // Устанавливаем всю очередь в ExoPlayer — он предзагрузит следующие треки
        val urls = tracks.mapNotNull { it.previewUrl }
        if (urls.size == tracks.size) {
            // Все треки имеют URL — используем нативный плейлист
            audioPlayerService.setQueue(urls, startIndex)
        } else {
            // Некоторые треки без URL — играем только текущий
            track.previewUrl?.let { audioPlayerService.play(it) }
        }
        _isPlaying.value = true
        startPositionUpdates()
        startTrackPlayTracking(track)
    }
    
    fun nextTrack() {
        doNextTrack()
    }

    private fun doNextTrack() {
        // Сначала пробуем нативный ExoPlayer seek — мгновенно
        if (audioPlayerService.seekToNext()) {
            val newIdx = audioPlayerService.getCurrentMediaIndex()
            val playlist = playerStateHolder.playlist.value
            val nextTrack = playlist.getOrNull(newIdx)
            if (nextTrack != null) {
                playerStateHolder.setPlaylist(playlist, newIdx)
                _currentTrack.value = nextTrack
                playerStateHolder.setCurrentTrack(nextTrack)
                loadAvgScore(nextTrack.id)
                hasTrackedCurrentTrack = false
                lastTrackedTrackId = null
                stopTrackPlayTracking()
                startTrackPlayTracking(nextTrack)
            }
            return
        }

        // Fallback — ищем похожие треки по артисту
        val current = _currentTrack.value ?: return
        viewModelScope.launch {
            val similar = musicRepository.searchTracks(current.artist, limit = 10).getOrNull()
                ?.filter { it.id != current.id && !it.previewUrl.isNullOrBlank() }
                ?.shuffled()
            if (!similar.isNullOrEmpty()) {
                playPlaylist(similar, 0)
            } else {
                val trending = musicRepository.getDiscoverFeed(page = 0, pageSize = 10).getOrNull()
                    ?.filter { it.id != current.id && !it.previewUrl.isNullOrBlank() }
                    ?.shuffled()
                if (!trending.isNullOrEmpty()) {
                    playPlaylist(trending, 0)
                }
            }
        }
    }

    fun previousTrack() {
        // Нативный ExoPlayer seek — мгновенно
        if (audioPlayerService.seekToPrevious()) {
            val newIdx = audioPlayerService.getCurrentMediaIndex()
            val playlist = playerStateHolder.playlist.value
            val prevTrack = playlist.getOrNull(newIdx)
            if (prevTrack != null) {
                playerStateHolder.setPlaylist(playlist, newIdx)
                _currentTrack.value = prevTrack
                playerStateHolder.setCurrentTrack(prevTrack)
                loadAvgScore(prevTrack.id)
                hasTrackedCurrentTrack = false
                lastTrackedTrackId = null
                stopTrackPlayTracking()
                startTrackPlayTracking(prevTrack)
            }
        }
        // Если нет предыдущего — ничего не делаем
    }
    
    fun togglePlayPause() {
        if (_isPlaying.value) {
            audioPlayerService.pause()
            _isPlaying.value = false
            // НЕ останавливаем positionUpdateJob — пусть продолжает читать позицию
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

    private fun loadAvgScore(trackId: String) {
        val token = sessionManager.getAccessToken() ?: run { _avgScore.value = null; return }
        viewModelScope.launch {
            val avg = authRepository.getTrackAverageRating(token, trackId)
            _avgScore.value = avg?.avgOverall?.toFloat()
        }
    }

    // Синхронизирует состояние ViewModel с реальным состоянием плеера
    fun syncPlayerState() {
        val player = audioPlayerService.getPlayer()
        if (player != null) {
            _isPlaying.value = player.isPlaying
            _currentPosition.value = audioPlayerService.getCurrentPosition()
            val dur = audioPlayerService.getDuration()
            if (dur > 0) _duration.value = dur
            startPositionUpdates()
        }
    }
    
    private fun startPositionUpdates() {
        stopPositionUpdates()
        positionUpdateJob = viewModelScope.launch {
            while (true) {
                val player = audioPlayerService.getPlayer()
                if (player != null) {
                    _currentPosition.value = audioPlayerService.getCurrentPosition()
                    val dur = audioPlayerService.getDuration()
                    // ExoPlayer возвращает Long.MAX_VALUE пока трек не загружен — фильтруем
                    if (dur > 0 && dur != Long.MAX_VALUE) {
                        _duration.value = dur
                    }
                }
                delay(250)
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
