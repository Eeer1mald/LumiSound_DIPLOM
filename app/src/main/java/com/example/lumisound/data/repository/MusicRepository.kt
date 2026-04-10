package com.example.lumisound.data.repository

import com.example.lumisound.data.local.SessionManager
import com.example.lumisound.data.model.Track
import com.example.lumisound.data.remote.AudiusApiService
import com.example.lumisound.data.remote.AudiusTrack
import com.example.lumisound.data.remote.SupabaseService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val audiusApi: AudiusApiService,
    private val supabase: SupabaseService,
    private val sessionManager: SessionManager
) {
    // Кэш фидов — заполняется при предзагрузке, используется ViewModels
    private var cachedDiscoverFeed: List<Track>? = null
    private var cachedFollowingFeed: List<Track>? = null

    suspend fun searchTracks(query: String, limit: Int = 20): Result<List<Track>> {
        return audiusApi.searchTracks(query, limit).map { audiusTracks ->
            audiusTracks.map { it.toTrack() }
        }
    }

    // Рекомендации "Для вас" — треки любимых артистов + жанры из оценок, без уже прослушанных
    suspend fun getDiscoverFeed(limit: Int = 30): Result<List<Track>> {
        cachedDiscoverFeed?.let { return Result.success(it) }
        return runCatching {
            val token = sessionManager.getAccessToken() ?: return@runCatching emptyList()
            val result = mutableListOf<Track>()
            val seenIds = mutableSetOf<String>()

            // 1. Треки любимых артистов
            val favArtists = supabase.getFavoriteArtists(token, limit = 10)
            for (artist in favArtists.take(5)) {
                audiusApi.getArtistTracks(artist.artistId, limit = 8).getOrNull()
                    ?.map { it.toTrack() }
                    ?.filter { seenIds.add(it.id) }
                    ?.let { result.addAll(it) }
            }

            // 2. Поиск по топ-жанрам из оценок пользователя
            val myRatings = supabase.getMyRatings(token, limit = 50)
            val topGenres = myRatings
                .mapNotNull { it.trackArtist.takeIf { a -> a.isNotBlank() } }
                .groupingBy { it }.eachCount()
                .entries.sortedByDescending { it.value }
                .take(3).map { it.key }

            for (genre in topGenres) {
                audiusApi.searchTracks(genre, limit = 8).getOrNull()
                    ?.map { it.toTrack() }
                    ?.filter { seenIds.add(it.id) }
                    ?.let { result.addAll(it) }
            }

            // 3. Исключаем уже прослушанные
            val history = supabase.getTrackHistory(token, limit = 200)
            val historyIds = history.map { it.trackId }.toSet()

            result.filter { it.id !in historyIds }.shuffled().take(limit).also {
                cachedDiscoverFeed = it
            }
        }
    }

    // Фид "Подписки" — новые треки артистов из избранного, отсортированные по play_count
    suspend fun getFollowingFeed(limit: Int = 30): Result<List<Track>> {
        cachedFollowingFeed?.let { return Result.success(it) }
        return runCatching {
            val token = sessionManager.getAccessToken() ?: return@runCatching emptyList()
            val result = mutableListOf<Track>()
            val seenIds = mutableSetOf<String>()

            val favArtists = supabase.getFavoriteArtists(token, limit = 20)
            // Сортируем по play_count — самые слушаемые артисты первыми
            val sorted = favArtists.sortedByDescending { it.playCount }

            for (artist in sorted) {
                audiusApi.getArtistTracks(artist.artistId, limit = 6).getOrNull()
                    ?.map { it.toTrack() }
                    ?.filter { seenIds.add(it.id) }
                    ?.let { result.addAll(it) }
            }

            result.take(limit).also {
                cachedFollowingFeed = it
            }
        }
    }

    private fun AudiusTrack.toTrack(): Track {
        val artworkUrl = audiusApi.getArtworkUrl(this.artwork, "480x480")
        val previewImageUrl = audiusApi.getArtworkUrl(this.artwork, "150x150")
        val hdImageUrl = audiusApi.getArtworkUrl(this.artwork, "1000x1000")
        val streamUrl = audiusApi.getStreamUrl(this.id)
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
