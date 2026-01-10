package com.example.lumisound.feature.home

/**
 * UI state for Home screen.
 */
data class HomeUiState(
    val userName: String = "",
    val recommendations: List<TrackPreview> = emptyList()
)

/** Simple preview model for a track card. */
data class TrackPreview(
    val id: String,
    val title: String,
    val artist: String,
    val coverUrl: String? = null
)


