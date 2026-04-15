package com.example.lumisound.data.repository

import com.example.lumisound.data.local.SessionManager
import com.example.lumisound.data.model.Track
import com.example.lumisound.data.remote.AudiusApiService
import com.example.lumisound.data.remote.AudiusArtistFull
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
    // Пул треков для бесконечного скролла — пополняется по мере листания
    private val discoverPool = mutableListOf<Track>()
    private val followingPool = mutableListOf<Track>()

    // Смещения для следующей страницы
    private var discoverArtistOffset = 0
    private var discoverGenreOffset = 0
    private var followingArtistOffset = 0

    // Кэш данных пользователя — не запрашиваем повторно в рамках сессии
    private var cachedFavArtistIds: List<String>? = null
    private var cachedFavTrackIds: List<String>? = null
    private var cachedTopGenres: List<String>? = null
    private var cachedHistoryIds: Set<String>? = null
    private var cachedPlaylistTrackIds: List<String>? = null

    // Сбрасываем всё при явном refresh
    fun invalidateFeeds() {
        discoverPool.clear()
        followingPool.clear()
        discoverArtistOffset = 0
        discoverGenreOffset = 0
        followingArtistOffset = 0
        cachedFavArtistIds = null
        cachedFavTrackIds = null
        cachedTopGenres = null
        cachedHistoryIds = null
        cachedPlaylistTrackIds = null
    }

    suspend fun searchArtists(query: String, limit: Int = 3): Result<List<AudiusArtistFull>> {
        return audiusApi.searchArtists(query, limit)
    }

    suspend fun searchTracks(query: String, limit: Int = 20): Result<List<Track>> {
        return audiusApi.searchTracks(query, limit).map { audiusTracks ->
            audiusTracks.map { it.toTrack() }
        }
    }

    // ── "Рекомендации" — всегда новое, разнообразное, никогда не пустое ──────────
    // Первый вызов: page=0, последующие: page>0 (бесконечный скролл)
    suspend fun getDiscoverFeed(page: Int = 0, pageSize: Int = 20): Result<List<Track>> {
        return runCatching {
            val token = sessionManager.getAccessToken()

            // Если в пуле достаточно треков — отдаём из него
            val startIdx = page * pageSize
            if (discoverPool.size >= startIdx + pageSize) {
                return@runCatching discoverPool.subList(startIdx, startIdx + pageSize).toList()
            }

            // Нужно подгрузить ещё
            val needed = (startIdx + pageSize) - discoverPool.size
            val newTracks = fetchDiscoverTracks(token, needed * 2) // берём с запасом
            discoverPool.addAll(newTracks)

            val end = minOf(startIdx + pageSize, discoverPool.size)
            if (startIdx >= discoverPool.size) emptyList()
            else discoverPool.subList(startIdx, end).toList()
        }
    }

    private suspend fun fetchDiscoverTracks(token: String?, needed: Int): List<Track> {
        val result = mutableListOf<Track>()
        val seenIds = discoverPool.map { it.id }.toMutableSet()

        // Получаем историю один раз
        val historyIds = if (token != null) {
            cachedHistoryIds ?: supabase.getTrackHistory(token, limit = 300)
                .map { it.trackId }.toSet().also { cachedHistoryIds = it }
        } else emptySet()

        if (token != null) {
            // 1. Треки из плейлистов пользователя (новый источник!)
            val playlistTrackIds = cachedPlaylistTrackIds ?: run {
                val playlists = supabase.getMyPlaylists(token)
                val ids = mutableListOf<String>()
                for (pl in playlists.take(5)) {
                    val tracks = supabase.getPlaylistTracks(token, pl.id)
                    ids.addAll(tracks.map { it.trackId })
                }
                ids.also { cachedPlaylistTrackIds = it }
            }

            // Для плейлистных треков ищем их через Audius по ID
            for (trackId in playlistTrackIds.filter { it !in seenIds && it !in historyIds }.take(10)) {
                // Ищем трек через поиск по ID (Audius поддерживает /v1/tracks/{id})
                audiusApi.getTrackById(trackId).getOrNull()?.let { t ->
                    val track = t.toTrack()
                    if (seenIds.add(track.id)) result.add(track)
                }
            }

            // 2. Треки любимых артистов (ротация — каждый раз разные артисты)
            val favArtistIds = cachedFavArtistIds ?: supabase.getFavoriteArtists(token, limit = 20)
                .map { it.artistId }.also { cachedFavArtistIds = it }

            if (favArtistIds.isNotEmpty()) {
                // Берём артистов со смещением для разнообразия
                val artistsToFetch = favArtistIds
                    .drop(discoverArtistOffset % favArtistIds.size.coerceAtLeast(1))
                    .take(5)
                discoverArtistOffset = (discoverArtistOffset + 5) % favArtistIds.size.coerceAtLeast(1)

                for (artistId in artistsToFetch) {
                    audiusApi.getArtistTracks(artistId, limit = 10).getOrNull()
                        ?.map { it.toTrack() }
                        ?.filter { it.id !in seenIds && it.id !in historyIds }
                        ?.shuffled()
                        ?.take(4)
                        ?.forEach { if (seenIds.add(it.id)) result.add(it) }
                }
            }

            // 3. Поиск по жанрам из оценок (используем реальный genre, не trackArtist)
            val topGenres = cachedTopGenres ?: run {
                val ratings = supabase.getMyRatings(token, limit = 100)
                // Берём жанры из избранных треков
                val favTracks = supabase.getFavoriteTracks(token, limit = 50)
                val genresFromFav = favTracks.mapNotNull { it.trackArtist.takeIf { a -> a.isNotBlank() } }
                // Комбинируем: жанры из оценок + артисты из избранного как поисковые запросы
                val genreQueries = ratings
                    .mapNotNull { it.trackArtist.takeIf { a -> a.isNotBlank() } }
                    .groupingBy { it }.eachCount()
                    .entries.sortedByDescending { it.value }
                    .take(5).map { it.key }
                (genreQueries + genresFromFav.take(3)).distinct().take(5)
                    .also { cachedTopGenres = it }
            }

            val genresToSearch = topGenres
                .drop(discoverGenreOffset % topGenres.size.coerceAtLeast(1))
                .take(3)
            discoverGenreOffset = (discoverGenreOffset + 3) % topGenres.size.coerceAtLeast(1)

            for (query in genresToSearch) {
                audiusApi.searchTracks(query, limit = 10).getOrNull()
                    ?.map { it.toTrack() }
                    ?.filter { it.id !in seenIds && it.id !in historyIds }
                    ?.shuffled()
                    ?.take(4)
                    ?.forEach { if (seenIds.add(it.id)) result.add(it) }
            }
        }

        // 4. Fallback / дополнение — trending + underground (всегда, чтобы фид не был пустым)
        if (result.size < needed) {
            val trendingGenres = listOf("Hip-Hop/Rap", "Electronic", "Pop", "R&B/Soul", null)
            val genreIdx = (discoverPool.size / 5) % trendingGenres.size
            audiusApi.getTrendingTracks(limit = 20, genre = trendingGenres[genreIdx]).getOrNull()
                ?.map { it.toTrack() }
                ?.filter { it.id !in seenIds }
                ?.shuffled()
                ?.forEach { if (seenIds.add(it.id)) result.add(it) }
        }

        if (result.size < needed) {
            audiusApi.getUndergroundTrendingTracks(limit = 20).getOrNull()
                ?.map { it.toTrack() }
                ?.filter { it.id !in seenIds }
                ?.shuffled()
                ?.forEach { if (seenIds.add(it.id)) result.add(it) }
        }

        return result.shuffled()
    }

    // ── "Для вас" — любимое: избранные треки + плейлисты + топ артисты ──────────
    suspend fun getFollowingFeed(page: Int = 0, pageSize: Int = 20): Result<List<Track>> {
        return runCatching {
            val token = sessionManager.getAccessToken()

            val startIdx = page * pageSize
            if (followingPool.size >= startIdx + pageSize) {
                return@runCatching followingPool.subList(startIdx, startIdx + pageSize).toList()
            }

            val needed = (startIdx + pageSize) - followingPool.size
            val newTracks = fetchFollowingTracks(token, needed * 2)
            followingPool.addAll(newTracks)

            val end = minOf(startIdx + pageSize, followingPool.size)
            if (startIdx >= followingPool.size) emptyList()
            else followingPool.subList(startIdx, end).toList()
        }
    }

    private suspend fun fetchFollowingTracks(token: String?, needed: Int): List<Track> {
        val result = mutableListOf<Track>()
        val seenIds = followingPool.map { it.id }.toMutableSet()

        if (token != null) {
            // 1. Избранные треки пользователя — самое "любимое"
            val favTracks = supabase.getFavoriteTracks(token, limit = 50, orderByPlayCount = true)
            for (ft in favTracks.filter { it.trackId !in seenIds }.take(15)) {
                audiusApi.getTrackById(ft.trackId).getOrNull()?.let { t ->
                    val track = t.toTrack()
                    if (seenIds.add(track.id)) result.add(track)
                }
            }

            // 2. Треки из плейлистов пользователя
            val playlists = supabase.getMyPlaylists(token)
            for (pl in playlists.take(5)) {
                val tracks = supabase.getPlaylistTracks(token, pl.id)
                for (pt in tracks.filter { it.trackId !in seenIds }.take(6)) {
                    audiusApi.getTrackById(pt.trackId).getOrNull()?.let { t ->
                        val track = t.toTrack()
                        if (seenIds.add(track.id)) result.add(track)
                    }
                }
            }

            // 3. Треки любимых артистов (по play_count — самые слушаемые первыми)
            val favArtists = supabase.getFavoriteArtists(token, limit = 20)
            val sorted = favArtists.sortedByDescending { it.playCount }

            val artistsToFetch = sorted
                .drop(followingArtistOffset % sorted.size.coerceAtLeast(1))
                .take(6)
            followingArtistOffset = (followingArtistOffset + 6) % sorted.size.coerceAtLeast(1)

            for (artist in artistsToFetch) {
                audiusApi.getArtistTracks(artist.artistId, limit = 8).getOrNull()
                    ?.map { it.toTrack() }
                    ?.filter { it.id !in seenIds }
                    ?.take(4)
                    ?.forEach { if (seenIds.add(it.id)) result.add(it) }
            }
        }

        // Fallback — trending если нет данных
        if (result.size < needed) {
            audiusApi.getTrendingTracks(limit = 30).getOrNull()
                ?.map { it.toTrack() }
                ?.filter { it.id !in seenIds }
                ?.shuffled()
                ?.forEach { if (seenIds.add(it.id)) result.add(it) }
        }

        return result
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
