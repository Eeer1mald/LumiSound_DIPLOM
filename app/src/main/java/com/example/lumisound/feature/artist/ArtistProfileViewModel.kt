package com.example.lumisound.feature.artist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lumisound.data.model.Track
import com.example.lumisound.data.remote.AudiusApiService
import com.example.lumisound.data.remote.AudiusArtistFull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArtistProfileState(
    val isLoading: Boolean = false,
    val artist: AudiusArtistFull? = null,
    val tracks: List<Track> = emptyList(),
    val avatarUrl: String? = null,
    val coverUrl: String? = null,
    val error: String? = null
)

@HiltViewModel
class ArtistProfileViewModel @Inject constructor(
    private val audiusApi: AudiusApiService
) : ViewModel() {

    private val _state = MutableStateFlow(ArtistProfileState())
    val state: StateFlow<ArtistProfileState> = _state

    fun load(artistId: String, fallbackName: String, fallbackImageUrl: String?) {
        viewModelScope.launch {
            _state.value = ArtistProfileState(isLoading = true)

            // Загружаем данные артиста
            val artistResult = audiusApi.getArtist(artistId)
            val artist = artistResult.getOrNull()

            val avatarUrl = artist?.let {
                audiusApi.getProfilePictureUrl(it.profilePicture, "480x480")
            } ?: fallbackImageUrl

            val coverUrl = artist?.let {
                audiusApi.getCoverPhotoUrl(it.coverPhoto)
            }

            // Загружаем треки артиста
            val tracksResult = audiusApi.getArtistTracks(artistId, limit = 10)
            val tracks = tracksResult.getOrNull()?.map { t ->
                val artworkUrl = audiusApi.getArtworkUrl(t.artwork, "480x480")
                val hdUrl = audiusApi.getArtworkUrl(t.artwork, "1000x1000")
                Track(
                    id = t.id,
                    name = t.title,
                    artist = t.artist.name,
                    artistId = t.artist.id,
                    artistImageUrl = avatarUrl,
                    imageUrl = artworkUrl,
                    hdImageUrl = hdUrl ?: artworkUrl,
                    previewUrl = audiusApi.getStreamUrl(t.id),
                    genre = t.genre
                )
            } ?: emptyList()

            _state.value = ArtistProfileState(
                isLoading = false,
                artist = artist,
                tracks = tracks,
                avatarUrl = avatarUrl,
                coverUrl = coverUrl,
                error = if (artist == null) "Не удалось загрузить профиль" else null
            )
        }
    }
}
