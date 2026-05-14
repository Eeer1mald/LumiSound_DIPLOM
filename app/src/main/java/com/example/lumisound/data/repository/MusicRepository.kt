package com.example.lumisound.data.repository

import com.example.lumisound.data.local.SessionManager
import com.example.lumisound.data.model.Track
import com.example.lumisound.data.remote.AudiusApiService
import com.example.lumisound.data.remote.AudiusArtistFull
import com.example.lumisound.data.remote.AudiusTrack
import com.example.lumisound.data.remote.SupabaseService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
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

    suspend fun searchPlaylists(query: String, limit: Int = 6): Result<List<com.example.lumisound.data.remote.AudiusPlaylist>> {
        return audiusApi.searchPlaylists(query, limit)
    }

    /** Строит stream URL для трека — не требует сетевого запроса */
    fun getStreamUrl(trackId: String): String = audiusApi.getStreamUrl(trackId)

    /** Получает полные данные трека по ID с обложкой и метаданными */
    suspend fun getTrackById(trackId: String): Track? {
        return audiusApi.getTrackById(trackId).getOrNull()?.toTrack()
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

    private suspend fun fetchDiscoverTracks(token: String?, needed: Int): List<Track> = coroutineScope {
        val result = mutableListOf<Track>()
        val seenIds = discoverPool.map { it.id }.toMutableSet()

        // Получаем историю один раз
        val historyIds = if (token != null) {
            cachedHistoryIds ?: withTimeoutOrNull(5_000L) {
                supabase.getTrackHistory(token, limit = 300).map { it.trackId }.toSet()
            }.also { cachedHistoryIds = it } ?: emptySet()
        } else emptySet()

        if (token != null) {
            // 1. Треки из плейлистов пользователя — параллельно
            val playlistTrackIds = cachedPlaylistTrackIds ?: run {
                val playlists = withTimeoutOrNull(5_000L) { supabase.getMyPlaylists(token) } ?: emptyList()
                val ids = mutableListOf<String>()
                // Загружаем треки плейлистов параллельно
                val playlistJobs = playlists.take(3).map { pl ->
                    async {
                        withTimeoutOrNull(4_000L) { supabase.getPlaylistTracks(token, pl.id) }
                            ?.map { it.trackId } ?: emptyList()
                    }
                }
                playlistJobs.forEach { ids.addAll(it.await()) }
                ids.also { cachedPlaylistTrackIds = it }
            }

            // Загружаем треки плейлистов параллельно
            val playlistTrackJobs = playlistTrackIds
                .filter { it !in seenIds && it !in historyIds }
                .take(6)
                .map { trackId ->
                    async {
                        withTimeoutOrNull(4_000L) {
                            audiusApi.getTrackById(trackId).getOrNull()?.toTrack()
                        }
                    }
                }
            playlistTrackJobs.forEach { job ->
                job.await()?.let { track -> if (seenIds.add(track.id)) result.add(track) }
            }

            // 2. Треки любимых артистов — параллельно
            val favArtistIds = cachedFavArtistIds ?: withTimeoutOrNull(5_000L) {
                supabase.getFavoriteArtists(token, limit = 20).map { it.artistId }
            }.also { cachedFavArtistIds = it } ?: emptyList()

            if (favArtistIds.isNotEmpty()) {
                val artistsToFetch = favArtistIds
                    .drop(discoverArtistOffset % favArtistIds.size.coerceAtLeast(1))
                    .take(4)
                discoverArtistOffset = (discoverArtistOffset + 4) % favArtistIds.size.coerceAtLeast(1)

                val artistJobs = artistsToFetch.map { artistId ->
                    async {
                        withTimeoutOrNull(5_000L) {
                            audiusApi.getArtistTracks(artistId, limit = 8).getOrNull()
                                ?.map { it.toTrack() }
                                ?.filter { it.id !in seenIds && it.id !in historyIds }
                                ?.shuffled()?.take(3)
                        } ?: emptyList()
                    }
                }
                artistJobs.forEach { job ->
                    job.await().forEach { track -> if (seenIds.add(track.id)) result.add(track) }
                }
            }

            // 3. Поиск по жанрам — параллельно, только если ещё нужны треки
            if (result.size < needed) {
                val topGenres = cachedTopGenres ?: run {
                    val ratings = withTimeoutOrNull(4_000L) { supabase.getMyRatings(token, limit = 50) } ?: emptyList()
                    val genreQueries = ratings
                        .mapNotNull { it.trackArtist.takeIf { a -> a.isNotBlank() } }
                        .groupingBy { it }.eachCount()
                        .entries.sortedByDescending { it.value }
                        .take(4).map { it.key }
                    genreQueries.also { cachedTopGenres = it }
                }

                val genresToSearch = topGenres
                    .drop(discoverGenreOffset % topGenres.size.coerceAtLeast(1))
                    .take(2)
                discoverGenreOffset = (discoverGenreOffset + 2) % topGenres.size.coerceAtLeast(1)

                val genreJobs = genresToSearch.map { query ->
                    async {
                        withTimeoutOrNull(5_000L) {
                            audiusApi.searchTracks(query, limit = 8).getOrNull()
                                ?.map { it.toTrack() }
                                ?.filter { it.id !in seenIds && it.id !in historyIds }
                                ?.shuffled()?.take(3)
                        } ?: emptyList()
                    }
                }
                genreJobs.forEach { job ->
                    job.await().forEach { track -> if (seenIds.add(track.id)) result.add(track) }
                }
            }
        }

        // 4. Fallback — trending (параллельно с underground)
        if (result.size < needed) {
            val trendingGenres = listOf("Hip-Hop/Rap", "Electronic", "Pop", "R&B/Soul", null)
            val genreIdx = (discoverPool.size / 5) % trendingGenres.size

            val trendingJob = async {
                withTimeoutOrNull(6_000L) {
                    audiusApi.getTrendingTracks(limit = 20, genre = trendingGenres[genreIdx]).getOrNull()
                        ?.map { it.toTrack() }?.filter { it.id !in seenIds }?.shuffled()
                } ?: emptyList()
            }
            val undergroundJob = async {
                withTimeoutOrNull(6_000L) {
                    audiusApi.getUndergroundTrendingTracks(limit = 20).getOrNull()
                        ?.map { it.toTrack() }?.filter { it.id !in seenIds }?.shuffled()
                } ?: emptyList()
            }

            trendingJob.await().forEach { if (seenIds.add(it.id)) result.add(it) }
            undergroundJob.await().forEach { if (seenIds.add(it.id)) result.add(it) }
        }

        result.shuffled()
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

    private suspend fun fetchFollowingTracks(token: String?, needed: Int): List<Track> = coroutineScope {
        val result = mutableListOf<Track>()
        val seenIds = followingPool.map { it.id }.toMutableSet()

        if (token != null) {
            // 1. Избранные треки — параллельно
            val favTracks = withTimeoutOrNull(5_000L) {
                supabase.getFavoriteTracks(token, limit = 30, orderByPlayCount = true)
            } ?: emptyList()

            val favTrackJobs = favTracks
                .filter { it.trackId !in seenIds }
                .take(10)
                .map { ft ->
                    async {
                        withTimeoutOrNull(4_000L) {
                            audiusApi.getTrackById(ft.trackId).getOrNull()?.toTrack()
                        }
                    }
                }
            favTrackJobs.forEach { job ->
                job.await()?.let { track -> if (seenIds.add(track.id)) result.add(track) }
            }

            // 2. Треки из плейлистов — параллельно
            val playlists = withTimeoutOrNull(4_000L) { supabase.getMyPlaylists(token) } ?: emptyList()
            val playlistJobs = playlists.take(3).map { pl ->
                async {
                    val tracks = withTimeoutOrNull(4_000L) { supabase.getPlaylistTracks(token, pl.id) } ?: emptyList()
                    tracks.filter { it.trackId !in seenIds }.take(4).mapNotNull { pt ->
                        withTimeoutOrNull(4_000L) {
                            audiusApi.getTrackById(pt.trackId).getOrNull()?.toTrack()
                        }
                    }
                }
            }
            playlistJobs.forEach { job ->
                job.await().forEach { track -> if (seenIds.add(track.id)) result.add(track) }
            }

            // 3. Треки любимых артистов — параллельно
            val favArtists = withTimeoutOrNull(5_000L) {
                supabase.getFavoriteArtists(token, limit = 15)
            } ?: emptyList()
            val sorted = favArtists.sortedByDescending { it.playCount }

            val artistsToFetch = sorted
                .drop(followingArtistOffset % sorted.size.coerceAtLeast(1))
                .take(4)
            followingArtistOffset = (followingArtistOffset + 4) % sorted.size.coerceAtLeast(1)

            val artistJobs = artistsToFetch.map { artist ->
                async {
                    withTimeoutOrNull(5_000L) {
                        audiusApi.getArtistTracks(artist.artistId, limit = 6).getOrNull()
                            ?.map { it.toTrack() }
                            ?.filter { it.id !in seenIds }
                            ?.take(3)
                    } ?: emptyList()
                }
            }
            artistJobs.forEach { job ->
                job.await().forEach { track -> if (seenIds.add(track.id)) result.add(track) }
            }
        }

        // Fallback — trending
        if (result.size < needed) {
            withTimeoutOrNull(6_000L) {
                audiusApi.getTrendingTracks(limit = 30).getOrNull()
                    ?.map { it.toTrack() }
                    ?.filter { it.id !in seenIds }
                    ?.shuffled()
            }?.forEach { if (seenIds.add(it.id)) result.add(it) }
        }

        result
    }

    private fun AudiusTrack.toTrack(): Track {
        val artworkUrl = audiusApi.getArtworkUrl(this.artwork, "480x480")
        val previewImageUrl = audiusApi.getArtworkUrl(this.artwork, "150x150")
        // Для HD берём 1000x1000, потом 480x480 — но не 150x150 (это LQ)
        val hdImageUrl = audiusApi.getArtworkUrlExact(this.artwork, "1000x1000")
            ?: audiusApi.getArtworkUrlExact(this.artwork, "480x480")
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
            genre = this.genre,
            playCount = this.playCount,
            duration = this.duration
        )
    }
}
