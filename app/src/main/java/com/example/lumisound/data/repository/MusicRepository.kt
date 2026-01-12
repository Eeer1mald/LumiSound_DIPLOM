package com.example.lumisound.data.repository

import com.example.lumisound.data.model.Track
import com.example.lumisound.data.remote.AudiusApiService
import com.example.lumisound.data.remote.AudiusTrack
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val audiusApi: AudiusApiService
) {
    suspend fun searchTracks(query: String, limit: Int = 20): Result<List<Track>> {
        return audiusApi.searchTracks(query, limit).map { audiusTracks ->
            audiusTracks.map { it.toTrack() }
        }
    }
    
    private fun AudiusTrack.toTrack(): Track {
        val artworkUrl = audiusApi.getArtworkUrl(this.artwork, "480x480")
        val previewImageUrl = audiusApi.getArtworkUrl(this.artwork, "150x150")
        val hdImageUrl = audiusApi.getArtworkUrl(this.artwork, "1000x1000")
        val streamUrl = audiusApi.getStreamUrl(this.id)
        
        // Извлекаем фотографию артиста
        val artistImageUrl = audiusApi.getProfilePictureUrl(this.artist.profilePicture, "480x480")
        val artistHdImageUrl = audiusApi.getProfilePictureUrl(this.artist.profilePicture, "1000x1000")
        
        return Track(
            id = this.id,
            name = this.title,
            artist = this.artist.name,
            artistId = this.artist.id,
            artistImageUrl = artistHdImageUrl ?: artistImageUrl,
            imageUrl = previewImageUrl,
            hdImageUrl = hdImageUrl ?: artworkUrl,
            previewUrl = streamUrl,
            trackUrl = this.permalink?.let { "https://audius.co$it" },
            genre = this.genre
        )
    }
}
