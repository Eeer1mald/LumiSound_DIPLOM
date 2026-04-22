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
    val isLoadingAllTracks: Boolean = false,
    val artist: AudiusArtistFull? = null,
    val tracks: List<Track> = emptyList(),       // топ-5 для главного экрана
    val allTracks: List<Track> = emptyList(),     // все треки для отдельного экрана
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

    fun load(artistId: String?, fallbackName: String, fallbackImageUrl: String?) {
        viewModelScope.launch {
            _state.value = ArtistProfileState(isLoading = true)

            // Если artistId пустой — ищем по имени
            val resolvedId: String? = if (!artistId.isNullOrBlank()) {
                artistId
            } else if (fallbackName.isNotBlank()) {
                audiusApi.searchArtists(fallbackName, limit = 3).getOrNull()
                    ?.firstOrNull { it.name.equals(fallbackName, ignoreCase = true) }?.id
                    ?: audiusApi.searchArtists(fallbackName, limit = 1).getOrNull()?.firstOrNull()?.id
            } else {
                null
            }

            if (resolvedId.isNullOrBlank()) {
                _state.value = ArtistProfileState(
                    isLoading = false,
                    avatarUrl = fallbackImageUrl,
                    error = "Артист не найден"
                )
                return@launch
            }

            // Загружаем данные артиста
            val artistResult = audiusApi.getArtist(resolvedId)
            val artist = artistResult.getOrNull()

            val avatarUrl = artist?.let {
                audiusApi.getProfilePictureUrl(it.profilePicture, "480x480")
            } ?: fallbackImageUrl

            val coverUrl = artist?.let {
                audiusApi.getCoverPhotoUrl(it.coverPhoto)
            }

            // Загружаем треки артиста — сразу 50 для "Все треки"
            val tracksResult = audiusApi.getArtistTracks(resolvedId, limit = 50)
            val allTracks = tracksResult.getOrNull()?.map { t ->
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
                tracks = allTracks.take(5),   // топ-5 для главного экрана
                allTracks = allTracks,
                avatarUrl = avatarUrl,
                coverUrl = coverUrl,
                error = if (artist == null) "Не удалось загрузить профиль" else null
            )
        }
    }
}
