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
    var sleepTimerActive: Boolean = false

    // История воспроизведения — для кнопки "назад" когда нет предыдущего в плейлисте
    private val _history = MutableStateFlow<List<Track>>(emptyList())
    val history: StateFlow<List<Track>> = _history.asStateFlow()

    fun pushHistory(track: Track) {
        // Не сохраняем треки без URL воспроизведения — их нельзя воспроизвести
        if (track.previewUrl.isNullOrBlank()) return
        val current = _history.value.toMutableList()
        // Не дублируем последний трек
        if (current.lastOrNull()?.id != track.id) {
            current.add(track)
            if (current.size > 50) current.removeAt(0)
            _history.value = current
        }
    }

    fun popHistory(): Track? {
        val current = _history.value.toMutableList()
        if (current.isEmpty()) return null
        val track = current.removeLast()
        _history.value = current
        return track
    }

    fun peekHistory(): Track? = _history.value.lastOrNull()
    // Флаг: пользователь начал свайп вниз для закрытия плеера.
    // MainNavGraph читает его чтобы показать мини-плеер заранее.
    private val _isPlayerClosing = MutableStateFlow(false)
    val isPlayerClosing: StateFlow<Boolean> = _isPlayerClosing.asStateFlow()

    fun setPlayerClosing(closing: Boolean) { _isPlayerClosing.value = closing }

    // Прогресс открытия плеера (0f = мини-плеер, 1f = полный плеер).
    // Устанавливается из MiniPlayer во время свайпа вверх.
    private val _openProgress = MutableStateFlow(0f)
    val openProgress: StateFlow<Float> = _openProgress.asStateFlow()

    fun setOpenProgress(progress: Float) { _openProgress.value = progress.coerceIn(0f, 1f) }

    // Прогресс закрытия плеера (0f = открыт, 1f = закрыт).
    // Устанавливается из PlayerScreen во время свайпа вниз.
    private val _closeProgress = MutableStateFlow(0f)
    val closeProgress: StateFlow<Float> = _closeProgress.asStateFlow()

    fun setCloseProgress(progress: Float) { _closeProgress.value = progress.coerceIn(0f, 1f) }

    // Максимальное расстояние свайпа вниз в пикселях (для синхронизации с мини-плеером)
    private val _maxDragPx = MutableStateFlow(0f)
    val maxDragPx: StateFlow<Float> = _maxDragPx.asStateFlow()
    fun setMaxDragPx(px: Float) { _maxDragPx.value = px }

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
        if (tracks.isEmpty()) {
            _currentIndex.value = 0
            return
        }
        _currentIndex.value = startIndex.coerceIn(0, tracks.size - 1)
        _currentTrack.value = tracks[_currentIndex.value]
    }

    /** Обновляет только индекс без изменения currentTrack — используется при seekToNext/Previous,
     *  чтобы визуал не менялся до подтверждения от ExoPlayer через onMediaItemTransition */
    fun setIndexOnly(newIndex: Int) {
        val size = _playlist.value.size
        if (size == 0) return
        _currentIndex.value = newIndex.coerceIn(0, size - 1)
    }

    /** Добавляет треки в конец плейлиста без изменения текущего трека и индекса */
    fun appendToPlaylist(tracks: List<Track>) {
        val current = _playlist.value.toMutableList()
        current.addAll(tracks)
        _playlist.value = current
        // currentIndex и currentTrack НЕ меняем
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
