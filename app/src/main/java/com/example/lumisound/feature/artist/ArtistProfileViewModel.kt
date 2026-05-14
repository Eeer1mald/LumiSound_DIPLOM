package com.example.lumisound.feature.artist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lumisound.data.model.Track
import com.example.lumisound.data.remote.AudiusApiService
import com.example.lumisound.data.remote.AudiusArtistFull
import com.example.lumisound.data.remote.SupabaseService
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
    val error: String? = null,
    // Custom artist fields (from Supabase)
    val customArtistName: String? = null,
    val customArtistBio: String? = null,
    val customArtistGenre: String? = null,
    val customArtistLocation: String? = null,
    val isCustomArtist: Boolean = false
)

/** UUID regex — custom artist IDs are UUIDs, Audius IDs are short alphanumeric strings */
private val UUID_REGEX = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", RegexOption.IGNORE_CASE)

@HiltViewModel
class ArtistProfileViewModel @Inject constructor(
    private val audiusApi: AudiusApiService,
    private val supabaseService: SupabaseService
) : ViewModel() {

    private val _state = MutableStateFlow(ArtistProfileState())
    val state: StateFlow<ArtistProfileState> = _state

    fun load(artistId: String?, fallbackName: String, fallbackImageUrl: String?) {
        viewModelScope.launch {
            _state.value = ArtistProfileState(isLoading = true)

            // Detect custom artist: UUID format or starts with "custom_"
            val isCustom = !artistId.isNullOrBlank() &&
                (artistId.matches(UUID_REGEX) || artistId.startsWith("custom_"))

            if (isCustom) {
                loadCustomArtist(artistId!!, fallbackName, fallbackImageUrl)
                return@launch
            }

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

    private suspend fun loadCustomArtist(artistId: String, fallbackName: String, fallbackImageUrl: String?) {
        // Strip "custom_" prefix if present
        val uuid = if (artistId.startsWith("custom_")) artistId.removePrefix("custom_") else artistId

        val artist = supabaseService.getCustomArtist(uuid)
        if (artist == null) {
            _state.value = ArtistProfileState(
                isLoading = false,
                avatarUrl = fallbackImageUrl,
                customArtistName = fallbackName,
                isCustomArtist = true,
                error = "Артист не найден"
            )
            return
        }

        // Load tracks for this custom artist
        val tracks = supabaseService.getCustomTracksByArtist(uuid).map { t ->
            Track(
                id = "custom_${t.id}",
                name = t.title,
                artist = artist.name,
                artistId = uuid,
                artistImageUrl = artist.avatarUrl,
                imageUrl = t.coverUrl,
                hdImageUrl = t.coverUrl,
                previewUrl = t.audioUrl,
                genre = t.genre,
                playCount = t.playCount,
                duration = t.duration
            )
        }

        _state.value = ArtistProfileState(
            isLoading = false,
            tracks = tracks.take(5),
            allTracks = tracks,
            avatarUrl = artist.avatarUrl,
            coverUrl = artist.coverUrl,
            isCustomArtist = true,
            customArtistName = artist.name,
            customArtistBio = artist.bio,
            customArtistGenre = artist.genre,
            customArtistLocation = artist.location,
            error = null
        )
    }
}
