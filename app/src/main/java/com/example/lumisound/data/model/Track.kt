package com.example.lumisound.data.model

data class Track(
    val id: String,
    val name: String,
    val artist: String,
    val artistId: String? = null,
    val artistImageUrl: String? = null,
    val imageUrl: String? = null,
    val hdImageUrl: String? = null,
    val previewUrl: String? = null,
    val trackUrl: String? = null,
    val genre: String? = null
)
