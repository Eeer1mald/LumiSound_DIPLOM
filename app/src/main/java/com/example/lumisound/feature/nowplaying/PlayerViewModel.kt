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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    val audiusApi: com.example.lumisound.data.remote.AudiusApiService,
    private val diskCache: com.example.lumisound.data.cache.DiskCache
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

    /** Есть ли предыдущий трек (в плейлисте или в истории) */
    val hasPrevious: Boolean get() = playerStateHolder.hasPrevious || playerStateHolder.history.value.isNotEmpty()
    
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
                    loadAvgScore(track.id)
                    // Сохраняем последний трек на диск с привязкой к userId
                    val userId = sessionManager.getUserId()
                    if (userId != null) diskCache.saveLastTrack(track, userId)
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
                // Игнорируем переходы вызванные изменением плейлиста (appendToQueue и т.п.)
                // Реагируем только на реальные переключения треков пользователем или авто
                if (reason == androidx.media3.common.Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                    return
                }

                // Если автопереход (не ручной) и autoplay выключен — останавливаем
                if (reason == androidx.media3.common.Player.MEDIA_ITEM_TRANSITION_REASON_AUTO
                    && !playerStateHolder.autoplayEnabled) {
                    audioPlayerService.pause()
                    return
                }

                val exoUrl = mediaItem?.localConfiguration?.uri?.toString()
                val newIdx = audioPlayerService.getCurrentMediaIndex()
                val playlist = playerStateHolder.playlist.value

                // Определяем реальный трек по URL аудио — источник истины
                val trackByUrl = if (exoUrl != null) {
                    playlist.firstOrNull { it.previewUrl == exoUrl }
                } else null

                val trackByIdx = playlist.getOrNull(newIdx)

                // Приоритет: трек найденный по URL аудио (аудио = источник истины)
                // Если по URL не нашли — берём по индексу
                val actualTrack = trackByUrl ?: trackByIdx ?: return

                // Если нашли по URL, но индекс не совпадает — синхронизируем плейлист
                val actualIdx = if (trackByUrl != null) {
                    playlist.indexOf(trackByUrl).takeIf { it >= 0 } ?: newIdx
                } else newIdx

                if (_currentTrack.value?.id != actualTrack.id) {
                    playerStateHolder.setPlaylist(playlist, actualIdx)
                    _currentTrack.value = actualTrack
                    playerStateHolder.setCurrentTrack(actualTrack)
                    _currentPosition.value = 0L
                    _duration.value = 0L
                    loadAvgScore(actualTrack.id)
                    fetchAndUpdateHdCover(actualTrack)
                    audioPlayerService.updateMediaMetadata(
                        actualTrack.name, actualTrack.artist,
                        actualTrack.imageUrl ?: actualTrack.hdImageUrl
                    )
                    hasTrackedCurrentTrack = false
                    lastTrackedTrackId = null
                    stopTrackPlayTracking()
                    startTrackPlayTracking(actualTrack)
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
        // Сохраняем текущий трек в историю перед переключением
        _currentTrack.value?.let { playerStateHolder.pushHistory(it) }

        hasTrackedCurrentTrack = false
        lastTrackedTrackId = null
        stopTrackPlayTracking()

        _currentTrack.value = track
        _currentPosition.value = 0L
        _duration.value = 0L

        // Запрашиваем HD обложку в фоне если её нет или она совпадает с LQ
        fetchAndUpdateHdCover(track)
        playerStateHolder.setCurrentTrack(track)
        // Обновляем плейлист — трек становится единственным элементом,
        // чтобы ExoPlayer и PlayerStateHolder были синхронизированы.
        // autoExtendQueueIfNeeded добавит похожие треки в фоне.
        playerStateHolder.setPlaylist(listOf(track), 0)
        loadAvgScore(track.id)
        preloadTrackReviewData(track.id)
        audioPlayerService.startMediaService()
        audioPlayerService.updateMediaMetadata(track.name, track.artist, track.imageUrl ?: track.hdImageUrl)
        track.previewUrl?.let { url ->
            // Всегда играем напрямую через URL — не ищем в старом плейлисте ExoPlayer,
            // чтобы гарантировать что аудио соответствует отображаемому треку.
            audioPlayerService.play(url)
            _isPlaying.value = true
            startPositionUpdates()
            startTrackPlayTracking(track)
            autoExtendQueueIfNeeded()
        }
    }

    fun playPlaylist(tracks: List<Track>, startIndex: Int = 0) {
        // Сохраняем текущий трек в историю
        _currentTrack.value?.let { playerStateHolder.pushHistory(it) }

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
        // Предзагружаем данные для экрана рецензий в фоне
        preloadTrackReviewData(track.id)
        audioPlayerService.startMediaService()
        audioPlayerService.updateMediaMetadata(track.name, track.artist, track.imageUrl ?: track.hdImageUrl)

        val urls = tracks.mapNotNull { it.previewUrl }
        if (urls.size == tracks.size) {
            audioPlayerService.setQueue(urls, startIndex)
        } else {
            track.previewUrl?.let { audioPlayerService.play(it) }
        }
        _isPlaying.value = true
        startPositionUpdates()
        startTrackPlayTracking(track)
        // Автодополняем очередь если треков мало
        autoExtendQueueIfNeeded()
    }
    
    fun nextTrack() {
        doNextTrack()
    }

    private fun doNextTrack() {
        // Сохраняем текущий трек в историю
        _currentTrack.value?.let { playerStateHolder.pushHistory(it) }

        // Сначала пробуем нативный ExoPlayer seek — мгновенно
        if (audioPlayerService.seekToNext()) {
            // Обновляем только индекс — currentTrack обновит onMediaItemTransition
            // когда ExoPlayer реально переключится и сообщит новый URL
            val currentIdx = playerStateHolder.currentIndex.value
            playerStateHolder.setIndexOnly(currentIdx + 1)
            autoExtendQueueIfNeeded()
            return
        }

        // Fallback — ищем похожие треки по артисту
        val current = _currentTrack.value ?: return
        viewModelScope.launch {
            val similar = musicRepository.searchTracks(current.artist, limit = 10).getOrNull()
                ?.filter { it.id != current.id && !it.previewUrl.isNullOrBlank() }
                ?.shuffled()
            if (!similar.isNullOrEmpty()) {
                // Добавляем в конец текущего плейлиста вместо замены
                val currentPlaylist = playerStateHolder.playlist.value.toMutableList()
                val newTracks = similar.filter { n -> currentPlaylist.none { it.id == n.id } }
                if (newTracks.isNotEmpty()) {
                    currentPlaylist.addAll(newTracks)
                    val newIdx = playerStateHolder.currentIndex.value + 1
                    if (currentPlaylist.isEmpty()) return@launch
                    playerStateHolder.setPlaylist(currentPlaylist, newIdx.coerceIn(0, currentPlaylist.size - 1))
                    val nextTrack = currentPlaylist.getOrNull(newIdx)
                    if (nextTrack != null) {
                        _currentTrack.value = nextTrack
                        playerStateHolder.setCurrentTrack(nextTrack)
                        nextTrack.previewUrl?.let { audioPlayerService.play(it) }
                        _isPlaying.value = true
                        startPositionUpdates()
                        loadAvgScore(nextTrack.id)
                        hasTrackedCurrentTrack = false
                        lastTrackedTrackId = null
                        stopTrackPlayTracking()
                        startTrackPlayTracking(nextTrack)
                    }
                }
            } else {
                val trending = musicRepository.getDiscoverFeed(page = 0, pageSize = 10).getOrNull()
                    ?.filter { it.id != current.id && !it.previewUrl.isNullOrBlank() }
                    ?.shuffled()
                if (!trending.isNullOrEmpty()) {
                    val currentPlaylist = playerStateHolder.playlist.value.toMutableList()
                    val newTracks = trending.filter { n -> currentPlaylist.none { it.id == n.id } }
                    if (newTracks.isNotEmpty()) {
                        currentPlaylist.addAll(newTracks)
                        val newIdx = playerStateHolder.currentIndex.value + 1
                        if (currentPlaylist.isEmpty()) return@launch
                        playerStateHolder.setPlaylist(currentPlaylist, newIdx.coerceIn(0, currentPlaylist.size - 1))
                        val nextTrack = currentPlaylist.getOrNull(newIdx)
                        if (nextTrack != null) {
                            _currentTrack.value = nextTrack
                            playerStateHolder.setCurrentTrack(nextTrack)
                            nextTrack.previewUrl?.let { audioPlayerService.play(it) }
                            _isPlaying.value = true
                            startPositionUpdates()
                            loadAvgScore(nextTrack.id)
                            hasTrackedCurrentTrack = false
                            lastTrackedTrackId = null
                            stopTrackPlayTracking()
                            startTrackPlayTracking(nextTrack)
                        }
                    }
                }
            }
        }
    }

    fun previousTrack() {
        val playlist = playerStateHolder.playlist.value
        val currentIdx = playerStateHolder.currentIndex.value

        // Если есть предыдущий в плейлисте — используем его
        if (currentIdx > 0) {
            if (audioPlayerService.seekToPrevious()) {
                // Обновляем только индекс — currentTrack обновит onMediaItemTransition
                playerStateHolder.setIndexOnly(currentIdx - 1)
                return
            }
        }

        // Fallback — берём из истории воспроизведения
        val historyTrack = playerStateHolder.popHistory()
        if (historyTrack != null && !historyTrack.previewUrl.isNullOrBlank()) {
            hasTrackedCurrentTrack = false
            lastTrackedTrackId = null
            stopTrackPlayTracking()
            _currentTrack.value = historyTrack
            _currentPosition.value = 0L
            _duration.value = 0L
            playerStateHolder.setCurrentTrack(historyTrack)
            loadAvgScore(historyTrack.id)
            audioPlayerService.startMediaService()
            audioPlayerService.updateMediaMetadata(historyTrack.name, historyTrack.artist, historyTrack.imageUrl ?: historyTrack.hdImageUrl)
            val previewUrl = historyTrack.previewUrl ?: return
            audioPlayerService.play(previewUrl)
            _isPlaying.value = true
            startPositionUpdates()
            startTrackPlayTracking(historyTrack)
        }
    }

    /**
     * Автоматически дополняет очередь похожими треками когда осталось мало.
     * Вызывается после каждого переключения трека.
     */
    private var autoExtendJob: Job? = null
    private fun autoExtendQueueIfNeeded() {
        val playlist = playerStateHolder.playlist.value
        val currentIdx = playerStateHolder.currentIndex.value
        val remaining = playlist.size - currentIdx - 1
        // Дополняем если осталось меньше 3 треков впереди
        if (remaining >= 3) return
        autoExtendJob?.cancel()
        autoExtendJob = viewModelScope.launch {
            val current = _currentTrack.value ?: return@launch
            val similar = musicRepository.searchTracks(current.artist, limit = 10).getOrNull()
                ?.filter { t -> !t.previewUrl.isNullOrBlank() && playlist.none { it.id == t.id } }
                ?.shuffled()
                ?.take(5)
            if (!similar.isNullOrEmpty()) {
                // appendToPlaylist НЕ меняет текущий трек и индекс
                playerStateHolder.appendToPlaylist(similar)
                // Добавляем URL в ExoPlayer очередь
                val newUrls = similar.mapNotNull { it.previewUrl }
                audioPlayerService.appendToQueue(newUrls)
                Log.d("PlayerViewModel", "Автодополнение: добавлено ${similar.size} треков")
            }
        }
    }
    
    fun togglePlayPause() {
        if (_isPlaying.value) {
            audioPlayerService.pause()
            _isPlaying.value = false
        } else {
            // Если ExoPlayer не готов (нет медиа-элементов) — нужно загрузить трек
            val player = audioPlayerService.getPlayer()
            val hasMedia = (player?.mediaItemCount ?: 0) > 0
            if (!hasMedia) {
                // Трек есть в PlayerStateHolder но не загружен в ExoPlayer — загружаем
                val track = _currentTrack.value ?: playerStateHolder.currentTrack.value
                if (track != null && !track.previewUrl.isNullOrBlank()) {
                    playTrack(track)
                    return
                }
            }
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

    /** Загружает HD обложку через Audius API и обновляет currentTrack если нашёл лучше */
    private fun fetchAndUpdateHdCover(track: Track) {
        // Если уже есть нормальный HD (не совпадает с LQ) — ничего не делаем
        if (!track.hdImageUrl.isNullOrEmpty() && track.hdImageUrl != track.imageUrl) return
        viewModelScope.launch {
            try {
                val audiusTrack = audiusApi.getTrackById(track.id).getOrNull() ?: return@launch
                val hd1000 = audiusApi.getArtworkUrlExact(audiusTrack.artwork, "1000x1000")
                val hd480  = audiusApi.getArtworkUrlExact(audiusTrack.artwork, "480x480")
                val newHd  = hd1000 ?: hd480 ?: return@launch
                // Обновляем только если нашли что-то лучше текущего
                if (newHd == track.imageUrl) return@launch
                val updatedTrack = track.copy(hdImageUrl = newHd)
                // Обновляем в PlayerStateHolder — PlayerScreen подхватит через collectAsState
                playerStateHolder.setCurrentTrack(updatedTrack)
                _currentTrack.value = updatedTrack
                // Обновляем в плейлисте тоже
                val playlist = playerStateHolder.playlist.value.toMutableList()
                val idx = playlist.indexOfFirst { it.id == track.id }
                if (idx >= 0) {
                    playlist[idx] = updatedTrack
                    playerStateHolder.setPlaylist(playlist, playerStateHolder.currentIndex.value)
                }
            } catch (_: Exception) {}
        }
    }

    private fun loadAvgScore(trackId: String) {        val token = sessionManager.getAccessToken() ?: run { _avgScore.value = null; return }
        viewModelScope.launch {
            val avg = authRepository.getTrackAverageRating(token, trackId)
            _avgScore.value = avg?.avgOverall?.toFloat()
        }
    }

    /** Предзагружает данные рецензий/комментариев трека в фоне пока пользователь слушает */
    private var preloadReviewJob: kotlinx.coroutines.Job? = null
    fun preloadTrackReviewData(trackId: String) {
        preloadReviewJob?.cancel()
        val token = sessionManager.getAccessToken() ?: return
        preloadReviewJob = viewModelScope.launch {
            // Небольшая задержка — не мешаем основному воспроизведению
            kotlinx.coroutines.delay(2000)
            // Предзагружаем параллельно — данные попадут в кэш AuthRepository
            kotlinx.coroutines.coroutineScope {
                val commentsJob = async { authRepository.getTrackComments(token, trackId) }
                val reviewsJob = async { authRepository.getTrackReviews(token, trackId) }
                val avgJob = async { authRepository.getTrackAverageRating(token, trackId) }
                commentsJob.await()
                reviewsJob.await()
                avgJob.await()
            }
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
        autoExtendJob?.cancel()
    }
}
