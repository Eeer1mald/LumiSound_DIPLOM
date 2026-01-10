package com.example.lumisound.data.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioPlayerService @Inject constructor(
    private val context: Context
) {
    private var exoPlayer: ExoPlayer? = null
    
    init {
        exoPlayer = ExoPlayer.Builder(context).build()
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
    
    fun pause() {
        exoPlayer?.pause()
    }
    
    fun resume() {
        exoPlayer?.play()
    }
    
    fun stop() {
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
    }
    
    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }
    
    fun setVolume(volume: Float) {
        exoPlayer?.volume = volume.coerceIn(0f, 1f)
    }
    
    fun getCurrentPosition(): Long {
        return exoPlayer?.currentPosition ?: 0L
    }
    
    fun getDuration(): Long {
        return exoPlayer?.duration ?: 0L
    }
    
    fun isPlaying(): Boolean {
        return exoPlayer?.isPlaying == true
    }
    
    fun addListener(listener: Player.Listener) {
        exoPlayer?.addListener(listener)
    }
    
    fun removeListener(listener: Player.Listener) {
        exoPlayer?.removeListener(listener)
    }
    
    fun release() {
        exoPlayer?.release()
        exoPlayer = null
    }
}
