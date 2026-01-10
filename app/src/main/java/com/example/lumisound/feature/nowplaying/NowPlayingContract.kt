package com.example.lumisound.feature.nowplaying

data class NowPlayingUiState(
    val track: Track? = null,
    val isPlaying: Boolean = false,
    val currentTime: Int = 0,
    val isLiked: Boolean = false,
    val userRating: Int? = null
)

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val coverUrl: String? = null,
    val duration: Int = 0 // в секундах
)

