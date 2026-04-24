package com.example.lumisound.data.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.lumisound.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    var onSleepTimerFinished: (() -> Unit)? = null

    private var mediaSession: MediaSessionCompat? = null
    private var notificationManager: NotificationManager? = null
    private var currentTrackTitle: String = ""
    private var currentTrackArtist: String = ""
    private var currentArtwork: Bitmap? = null

    companion object {
        const val CHANNEL_ID = "lumisound_playback"
        const val NOTIFICATION_ID = 1001
    }

    init {
        exoPlayer = ExoPlayer.Builder(context).build()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        initMediaSession()
        exoPlayer?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlaybackState(isPlaying)
                if (currentTrackTitle.isNotBlank()) updateNotification()
            }
        })
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "LumiSound — воспроизведение",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
                description = "Управление воспроизведением"
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun initMediaSession() {
        try {
            mediaSession = MediaSessionCompat(context, "LumiSoundSession").apply {
                isActive = true
                setCallback(object : MediaSessionCompat.Callback() {
                    override fun onPlay() { exoPlayer?.play() }
                    override fun onPause() { exoPlayer?.pause() }
                    override fun onSkipToNext() { seekToNext() }
                    override fun onSkipToPrevious() { seekToPrevious() }
                    override fun onStop() { exoPlayer?.stop() }
                })
            }
        } catch (e: Exception) {
            android.util.Log.w("AudioPlayerService", "MediaSession init failed: ${e.message}")
        }
    }

    private fun updatePlaybackState(isPlaying: Boolean) {
        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val position = exoPlayer?.currentPosition ?: 0L
        mediaSession?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(state, position, 1f)
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_STOP
                )
                .build()
        )
    }

    /** Обновляет метаданные трека и показывает уведомление */
    fun updateMediaMetadata(title: String, artist: String, artworkUrl: String? = null) {
        currentTrackTitle = title
        currentTrackArtist = artist
        scope.launch {
            // Загружаем обложку асинхронно
            if (!artworkUrl.isNullOrBlank()) {
                currentArtwork = withContext(Dispatchers.IO) {
                    try {
                        val url = java.net.URL(artworkUrl)
                        val conn = url.openConnection().apply {
                            connectTimeout = 3000; readTimeout = 3000
                        }
                        BitmapFactory.decodeStream(conn.getInputStream())
                    } catch (e: Exception) { null }
                }
            } else {
                currentArtwork = null
            }

            // Обновляем метаданные MediaSession
            val metadataBuilder = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
            currentArtwork?.let {
                metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it)
            }
            mediaSession?.setMetadata(metadataBuilder.build())

            updatePlaybackState(exoPlayer?.isPlaying == true)
            updateNotification()
        }
    }

    private fun updateNotification() {
        val session = mediaSession ?: return
        val title = currentTrackTitle.ifBlank { return }
        val isPlaying = exoPlayer?.isPlaying == true

        try {
            val openIntent = PendingIntent.getActivity(
                context, 0,
                Intent(context, Class.forName("com.example.lumisound.MainActivity")).apply {
                    action = "OPEN_PLAYER"
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val prevIntent = PendingIntent.getBroadcast(
                context, 1,
                Intent("com.example.lumisound.PREV"),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val playPauseIntent = PendingIntent.getBroadcast(
                context, 2,
                Intent(if (isPlaying) "com.example.lumisound.PAUSE" else "com.example.lumisound.PLAY"),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val nextIntent = PendingIntent.getBroadcast(
                context, 3,
                Intent("com.example.lumisound.NEXT"),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notif_music)
                .setContentTitle(title)
                .setContentText(currentTrackArtist)
                .setContentIntent(openIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOnlyAlertOnce(true)
                .setOngoing(isPlaying)
                .addAction(R.drawable.ic_notif_prev, "Назад", prevIntent)
                .addAction(
                    if (isPlaying) R.drawable.ic_notif_pause else R.drawable.ic_notif_play,
                    if (isPlaying) "Пауза" else "Играть",
                    playPauseIntent
                )
                .addAction(R.drawable.ic_notif_next, "Вперёд", nextIntent)
                .setStyle(
                    MediaStyle()
                        .setMediaSession(session.sessionToken)
                        .setShowActionsInCompactView(0, 1, 2)
                )

            currentArtwork?.let { builder.setLargeIcon(it) }

            notificationManager?.notify(NOTIFICATION_ID, builder.build())
        } catch (e: Exception) {
            android.util.Log.w("AudioPlayerService", "Notification update failed: ${e.message}")
        }
    }

    fun dismissNotification() {
        notificationManager?.cancel(NOTIFICATION_ID)
    }

    fun startMediaService() {
        // Уведомление управляется напрямую через updateMediaMetadata
    }

    fun getPlayer(): ExoPlayer? = exoPlayer

    fun play(url: String) {
        exoPlayer?.let { player ->
            val mediaItem = MediaItem.fromUri(url)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        }
    }

    fun setQueue(urls: List<String>, startIndex: Int = 0) {
        exoPlayer?.let { player ->
            val items = urls.map { MediaItem.fromUri(it) }
            player.setMediaItems(items, startIndex, 0L)
            player.prepare()
            player.play()
        }
    }

    fun seekToNext(): Boolean {
        val player = exoPlayer ?: return false
        return if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
            true
        } else false
    }

    fun seekToPrevious(): Boolean {
        val player = exoPlayer ?: return false
        return if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
            true
        } else false
    }

    fun getCurrentMediaIndex(): Int = exoPlayer?.currentMediaItemIndex ?: 0
    fun getQueueSize(): Int = exoPlayer?.mediaItemCount ?: 0
    fun pause() { exoPlayer?.pause() }
    fun resume() { exoPlayer?.play() }

    fun stop() {
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        dismissNotification()
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

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.playbackParameters = PlaybackParameters(speed.coerceIn(0.25f, 3.0f))
    }
    fun getPlaybackSpeed(): Float = exoPlayer?.playbackParameters?.speed ?: 1f

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

    fun initEqualizer(): Boolean {
        val audioSessionId = exoPlayer?.audioSessionId ?: return false
        if (audioSessionId == 0) return false
        return try {
            if (equalizer == null) equalizer = Equalizer(0, audioSessionId)
            equalizer?.enabled = true
            true
        } catch (e: Exception) { false }
    }

    fun getEqualizerBands(): List<EqualizerBand> {
        val eq = equalizer ?: return emptyList()
        return try {
            val numBands = eq.numberOfBands.toInt()
            (0 until numBands).map { i ->
                val band = i.toShort()
                EqualizerBand(
                    index = i,
                    centerFreqHz = eq.getCenterFreq(band) / 1000,
                    levelMilliBel = eq.getBandLevel(band).toInt(),
                    minLevel = eq.bandLevelRange[0].toInt(),
                    maxLevel = eq.bandLevelRange[1].toInt()
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    fun setEqualizerBandLevel(bandIndex: Int, levelMilliBel: Int) {
        try { equalizer?.setBandLevel(bandIndex.toShort(), levelMilliBel.toShort()) } catch (e: Exception) {}
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        try {
            if (enabled && equalizer == null) initEqualizer()
            equalizer?.enabled = enabled
        } catch (e: Exception) {}
    }

    fun getEqualizerPresets(): List<String> {
        val eq = equalizer ?: return emptyList()
        return try { (0 until eq.numberOfPresets).map { eq.getPresetName(it.toShort()) } } catch (e: Exception) { emptyList() }
    }

    fun applyEqualizerPreset(presetIndex: Int) {
        try { equalizer?.usePreset(presetIndex.toShort()) } catch (e: Exception) {}
    }

    fun setVirtualizerEnabled(enabled: Boolean) {
        val audioSessionId = exoPlayer?.audioSessionId ?: return
        if (audioSessionId == 0) return
        try {
            if (enabled) {
                if (virtualizer == null) virtualizer = Virtualizer(0, audioSessionId)
                virtualizer?.setStrength(1000)
                virtualizer?.enabled = true
            } else {
                virtualizer?.enabled = false
            }
        } catch (e: Exception) {}
    }

    // ── Таймер сна ────────────────────────────────────────────────────────

    private var sleepTimerEndMs: Long = 0L

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        sleepTimerEndMs = System.currentTimeMillis() + minutes * 60 * 1000L
        sleepTimerJob = scope.launch {
            val endTime = sleepTimerEndMs
            while (System.currentTimeMillis() < endTime) { delay(1000L) }
            exoPlayer?.pause()
            sleepTimerEndMs = 0L
            onSleepTimerFinished?.invoke()
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        sleepTimerEndMs = 0L
    }

    fun isSleepTimerActive(): Boolean = sleepTimerJob?.isActive == true

    fun getSleepTimerRemainingMs(): Long {
        if (!isSleepTimerActive() || sleepTimerEndMs == 0L) return 0L
        return (sleepTimerEndMs - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    fun release() {
        sleepTimerJob?.cancel()
        mediaSession?.release()
        mediaSession = null
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
