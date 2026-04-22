package com.example.lumisound.data.player

import android.content.Context
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioPlayerService @Inject constructor(
    private val context: Context
) {
    private var exoPlayer: ExoPlayer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var equalizer: Equalizer? = null
    private var virtualizer: Virtualizer? = null

    private var sleepTimerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    // Callback когда таймер сна срабатывает
    var onSleepTimerFinished: (() -> Unit)? = null

    init {
        exoPlayer = ExoPlayer.Builder(context).build()
    }

    fun getPlayer(): ExoPlayer? = exoPlayer

    /**
     * Играет один трек. Для мгновенного переключения используй setQueue().
     */
    fun play(url: String) {
        exoPlayer?.let { player ->
            val mediaItem = MediaItem.fromUri(url)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        }
    }

    /**
     * Устанавливает очередь треков в ExoPlayer — он предзагружает следующий трек автоматически.
     * @param urls список URL треков
     * @param startIndex индекс с которого начать воспроизведение
     */
    fun setQueue(urls: List<String>, startIndex: Int = 0) {
        exoPlayer?.let { player ->
            val items = urls.map { MediaItem.fromUri(it) }
            player.setMediaItems(items, startIndex, 0L)
            player.prepare()
            player.play()
        }
    }

    /**
     * Переключает на следующий трек в очереди ExoPlayer — мгновенно, без буферизации.
     * @return true если переключение произошло
     */
    fun seekToNext(): Boolean {
        val player = exoPlayer ?: return false
        return if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
            true
        } else false
    }

    /**
     * Переключает на предыдущий трек в очереди ExoPlayer.
     * @return true если переключение произошло
     */
    fun seekToPrevious(): Boolean {
        val player = exoPlayer ?: return false
        return if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
            true
        } else false
    }

    /** Текущий индекс в очереди ExoPlayer */
    fun getCurrentMediaIndex(): Int = exoPlayer?.currentMediaItemIndex ?: 0

    /** Размер очереди ExoPlayer */
    fun getQueueSize(): Int = exoPlayer?.mediaItemCount ?: 0

    fun pause() { exoPlayer?.pause() }
    fun resume() { exoPlayer?.play() }

    fun stop() {
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
    }

    fun seekTo(positionMs: Long) { exoPlayer?.seekTo(positionMs) }
    fun setVolume(volume: Float) { exoPlayer?.volume = volume.coerceIn(0f, 1f) }
    fun getCurrentPosition(): Long = exoPlayer?.currentPosition ?: 0L

    fun getDuration(): Long {
        val dur = exoPlayer?.duration ?: 0L
        return if (dur == Long.MAX_VALUE || dur < 0) 0L else dur
    }

    fun isPlaying(): Boolean = exoPlayer?.isPlaying == true

    fun addListener(listener: Player.Listener) { exoPlayer?.addListener(listener) }
    fun removeListener(listener: Player.Listener) { exoPlayer?.removeListener(listener) }

    // ── Скорость воспроизведения ──────────────────────────────────────────

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 3.0f)
        exoPlayer?.playbackParameters = PlaybackParameters(clamped)
    }

    fun getPlaybackSpeed(): Float = exoPlayer?.playbackParameters?.speed ?: 1f

    // ── Нормализация громкости ────────────────────────────────────────────

    fun setNormalizeVolume(enabled: Boolean) {
        val audioSessionId = exoPlayer?.audioSessionId ?: return
        if (audioSessionId == 0) return
        try {
            if (enabled) {
                if (loudnessEnhancer == null) loudnessEnhancer = LoudnessEnhancer(audioSessionId)
                loudnessEnhancer?.setTargetGain(500)
                loudnessEnhancer?.enabled = true
            } else {
                loudnessEnhancer?.enabled = false
            }
        } catch (e: Exception) {
            android.util.Log.w("AudioPlayerService", "LoudnessEnhancer error: ${e.message}")
        }
    }

    // ── Эквалайзер ────────────────────────────────────────────────────────

    fun initEqualizer(): Boolean {
        val audioSessionId = exoPlayer?.audioSessionId ?: return false
        if (audioSessionId == 0) return false
        return try {
            if (equalizer == null) equalizer = Equalizer(0, audioSessionId)
            equalizer?.enabled = true
            true
        } catch (e: Exception) {
            android.util.Log.w("AudioPlayerService", "Equalizer init error: ${e.message}")
            false
        }
    }

    fun getEqualizerBands(): List<EqualizerBand> {
        val eq = equalizer ?: return emptyList()
        return try {
            val numBands = eq.numberOfBands.toInt()
            (0 until numBands).map { i ->
                val band = i.toShort()
                EqualizerBand(
                    index = i,
                    centerFreqHz = eq.getCenterFreq(band) / 1000, // milliHz → Hz
                    levelMilliBel = eq.getBandLevel(band).toInt(),
                    minLevel = eq.bandLevelRange[0].toInt(),
                    maxLevel = eq.bandLevelRange[1].toInt()
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    fun setEqualizerBandLevel(bandIndex: Int, levelMilliBel: Int) {
        try {
            equalizer?.setBandLevel(bandIndex.toShort(), levelMilliBel.toShort())
        } catch (e: Exception) {
            android.util.Log.w("AudioPlayerService", "EQ setBandLevel error: ${e.message}")
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        try {
            if (enabled && equalizer == null) initEqualizer()
            equalizer?.enabled = enabled
        } catch (e: Exception) {
            android.util.Log.w("AudioPlayerService", "EQ enabled error: ${e.message}")
        }
    }

    fun getEqualizerPresets(): List<String> {
        val eq = equalizer ?: return emptyList()
        return try {
            (0 until eq.numberOfPresets).map { eq.getPresetName(it.toShort()) }
        } catch (e: Exception) { emptyList() }
    }

    fun applyEqualizerPreset(presetIndex: Int) {
        try {
            equalizer?.usePreset(presetIndex.toShort())
        } catch (e: Exception) {
            android.util.Log.w("AudioPlayerService", "EQ preset error: ${e.message}")
        }
    }

    // ── Виртуализатор ─────────────────────────────────────────────────────

    fun setVirtualizerEnabled(enabled: Boolean) {
        val audioSessionId = exoPlayer?.audioSessionId ?: return
        if (audioSessionId == 0) return
        try {
            if (enabled) {
                if (virtualizer == null) virtualizer = Virtualizer(0, audioSessionId)
                virtualizer?.setStrength(1000) // максимальная сила (0-1000)
                virtualizer?.enabled = true
            } else {
                virtualizer?.enabled = false
            }
        } catch (e: Exception) {
            android.util.Log.w("AudioPlayerService", "Virtualizer error: ${e.message}")
        }
    }

    // ── Таймер сна ────────────────────────────────────────────────────────

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        sleepTimerJob = scope.launch {
            delay(minutes * 60 * 1000L)
            exoPlayer?.pause()
            onSleepTimerFinished?.invoke()
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
    }

    fun isSleepTimerActive(): Boolean = sleepTimerJob?.isActive == true

    fun release() {
        sleepTimerJob?.cancel()
        loudnessEnhancer?.release()
        equalizer?.release()
        virtualizer?.release()
        loudnessEnhancer = null
        equalizer = null
        virtualizer = null
        exoPlayer?.release()
        exoPlayer = null
    }
}

data class EqualizerBand(
    val index: Int,
    val centerFreqHz: Int,
    val levelMilliBel: Int,
    val minLevel: Int,
    val maxLevel: Int
)
