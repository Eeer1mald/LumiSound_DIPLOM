package com.example.lumisound.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.url
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class AudiusTrack(
    val id: String,
    val title: String,
    @SerialName("user") val artist: AudiusArtist,
    @SerialName("artwork") val artwork: JsonObject? = null,
    @SerialName("genre") val genre: String? = null,
    @SerialName("mood") val mood: String? = null,
    @SerialName("tags") val tags: String? = null,
    @SerialName("permalink") val permalink: String? = null,
    @SerialName("duration") val duration: Int? = null,
    @SerialName("play_count") val playCount: Int? = null,
    @SerialName("repost_count") val repostCount: Int? = null,
    @SerialName("favorite_count") val favoriteCount: Int? = null,
    @SerialName("comment_count") val commentCount: Int? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("is_downloadable") val isDownloadable: Boolean? = null,
    @SerialName("description") val description: String? = null
)

@Serializable
data class AudiusArtist(
    val id: String? = null,
    val name: String,
    @SerialName("profile_picture") val profilePicture: JsonObject? = null
)

@Serializable
data class AudiusArtistFull(
    val id: String,
    val name: String,
    @SerialName("profile_picture") val profilePicture: JsonObject? = null,
    @SerialName("cover_photo") val coverPhoto: JsonObject? = null,
    @SerialName("bio") val bio: String? = null,
    @SerialName("location") val location: String? = null,
    @SerialName("follower_count") val followerCount: Int? = null,
    @SerialName("following_count") val followingCount: Int? = null,
    @SerialName("track_count") val trackCount: Int? = null,
    @SerialName("playlist_count") val playlistCount: Int? = null,
    @SerialName("is_verified") val isVerified: Boolean? = null,
    @SerialName("twitter_handle") val twitterHandle: String? = null,
    @SerialName("instagram_handle") val instagramHandle: String? = null
)

@Serializable
data class AudiusArtistResponse(
    val data: AudiusArtistFull? = null
)

@Serializable
data class AudiusTracksResponse(
    val data: List<AudiusTrack>? = null
)

@Serializable
data class AudiusArtistSearchResponse(
    val data: List<AudiusArtistFull>? = null
)

@Serializable
data class AudiusSearchResponse(
    val data: List<AudiusTrack>? = null
)

@Serializable
data class AudiusPlaylist(
    val id: String,
    @SerialName("playlist_name") val playlistName: String? = null,
    @SerialName("artwork") val artwork: JsonObject? = null,
    @SerialName("track_count") val trackCount: Int? = null,
    @SerialName("user") val user: AudiusArtist? = null
)

@Serializable
data class AudiusPlaylistSearchResponse(
    val data: List<AudiusPlaylist>? = null
)

@Singleton
class AudiusApiService @Inject constructor(
    private val httpClient: HttpClient
) {
    companion object {
        private const val TAG = "AudiusApiService"
        private val HOSTS = listOf(
            "https://discoveryprovider.audius.co",
            "https://discovery-us-01.audius.openplayer.org",
            "https://discovery-us-02.audius.openplayer.org",
            "https://discoveryprovider3.audius.co"
        )
        private const val APP_NAME = "LumiSound"
    }

    private var currentHostIndex = 0

    private val currentHost: String
        get() = HOSTS[currentHostIndex]

    private fun rotateHost() {
        currentHostIndex = (currentHostIndex + 1) % HOSTS.size
        Log.d(TAG, "Switching to host: $currentHost")
    }

    private suspend fun <T> makeRequestWithFallback(
        endpoint: String,
        block: suspend (String) -> T
    ): T {
        var lastException: Exception? = null
        for (i in HOSTS.indices) {
            try {
                val url = "$currentHost$endpoint"
                Log.d(TAG, "Attempting request to: $url")
                return block(url)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to $currentHost: ${e.message}")
                lastException = e
                rotateHost()
                if (i == HOSTS.size - 1) break
            }
        }
        throw lastException ?: Exception("All Audius API hosts failed")
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    suspend fun searchArtists(query: String, limit: Int = 3): Result<List<AudiusArtistFull>> {
        return runCatching {
            makeRequestWithFallback("/v1/users/search") { baseUrl ->
                val response = httpClient.get {
                    url(baseUrl)
                    parameter("query", query)
                    parameter("limit", limit)
                    parameter("app_name", APP_NAME)
                }.body<String>()
                val searchResponse = json.decodeFromString(AudiusArtistSearchResponse.serializer(), response)
                searchResponse.data ?: emptyList()
            }
        }
    }

    suspend fun searchTracks(query: String, limit: Int = 10): Result<List<AudiusTrack>> {
        return runCatching {
            makeRequestWithFallback("/v1/tracks/search") { baseUrl ->
                val response = httpClient.get {
                    url(baseUrl)
                    parameter("query", query)
                    parameter("limit", limit)
                    parameter("app_name", APP_NAME)
                }.body<String>()
                val searchResponse = json.decodeFromString(AudiusSearchResponse.serializer(), response)
                searchResponse.data ?: emptyList()
            }
        }
    }

    suspend fun getArtist(artistId: String): Result<AudiusArtistFull> {
        return runCatching {
            makeRequestWithFallback("/v1/users/$artistId") { baseUrl ->
                val response = httpClient.get {
                    url(baseUrl)
                    parameter("app_name", APP_NAME)
                }.body<String>()
                Log.d(TAG, "Artist response: $response")
                val artistResponse = json.decodeFromString(AudiusArtistResponse.serializer(), response)
                artistResponse.data ?: throw Exception("Artist not found")
            }
        }
    }

    suspend fun getArtistTracks(artistId: String, limit: Int = 10): Result<List<AudiusTrack>> {
        return runCatching {
            makeRequestWithFallback("/v1/users/$artistId/tracks") { baseUrl ->
                val response = httpClient.get {
                    url(baseUrl)
                    parameter("limit", limit)
                    parameter("app_name", APP_NAME)
                }.body<String>()
                Log.d(TAG, "Artist tracks response: $response")
                val tracksResponse = json.decodeFromString(AudiusTracksResponse.serializer(), response)
                tracksResponse.data ?: emptyList()
            }
        }
    }

    fun getArtworkUrl(artwork: JsonObject?, size: String = "480x480"): String? {
        return artwork?.get(size)?.jsonPrimitive?.content
            ?: artwork?.get("150x150")?.jsonPrimitive?.content
            ?: artwork?.get("1000x1000")?.jsonPrimitive?.content
    }

    fun getProfilePictureUrl(profilePicture: JsonObject?, size: String = "480x480"): String? {
        return profilePicture?.get(size)?.jsonPrimitive?.content
            ?: profilePicture?.get("150x150")?.jsonPrimitive?.content
            ?: profilePicture?.get("1000x1000")?.jsonPrimitive?.content
            ?: profilePicture?.get("200x200")?.jsonPrimitive?.content
    }

    fun getCoverPhotoUrl(coverPhoto: JsonObject?, size: String = "2000x"): String? {
        return coverPhoto?.get(size)?.jsonPrimitive?.content
            ?: coverPhoto?.get("640x")?.jsonPrimitive?.content
    }

    fun getStreamUrl(trackId: String): String {
        return "$currentHost/v1/tracks/$trackId/stream"
    }

    suspend fun searchPlaylists(query: String, limit: Int = 6): Result<List<AudiusPlaylist>> {
        return runCatching {
            makeRequestWithFallback("/v1/playlists/search") { baseUrl ->
                val response = httpClient.get {
                    url(baseUrl)
                    parameter("query", query)
                    parameter("limit", limit)
                    parameter("app_name", APP_NAME)
                }.body<String>()
                val parsed = json.decodeFromString(AudiusPlaylistSearchResponse.serializer(), response)
                parsed.data ?: emptyList()
            }
        }
    }

    // Получить трек по ID
    suspend fun getTrackById(trackId: String): Result<AudiusTrack> {        return runCatching {
            makeRequestWithFallback("/v1/tracks/$trackId") { baseUrl ->
                val response = httpClient.get {
                    url(baseUrl)
                    parameter("app_name", APP_NAME)
                }.body<String>()
                @Serializable data class SingleTrackResponse(val data: AudiusTrack? = null)
                val parsed = json.decodeFromString(SingleTrackResponse.serializer(), response)
                parsed.data ?: throw Exception("Track $trackId not found")
            }
        }
    }

    // Trending треки — используется как fallback когда нет избранного
    suspend fun getTrendingTracks(limit: Int = 20, genre: String? = null): Result<List<AudiusTrack>> {
        return runCatching {
            makeRequestWithFallback("/v1/tracks/trending") { baseUrl ->
                val response = httpClient.get {
                    url(baseUrl)
                    parameter("limit", limit)
                    parameter("app_name", APP_NAME)
                    genre?.let { parameter("genre", it) }
                }.body<String>()
                val tracksResponse = json.decodeFromString(AudiusTracksResponse.serializer(), response)
                tracksResponse.data ?: emptyList()
            }
        }
    }

    // Underground trending — разнообразие для рекомендаций
    suspend fun getUndergroundTrendingTracks(limit: Int = 20): Result<List<AudiusTrack>> {
        return runCatching {
            makeRequestWithFallback("/v1/tracks/trending/underground") { baseUrl ->
                val response = httpClient.get {
                    url(baseUrl)
                    parameter("limit", limit)
                    parameter("app_name", APP_NAME)
                }.body<String>()
                val tracksResponse = json.decodeFromString(AudiusTracksResponse.serializer(), response)
                tracksResponse.data ?: emptyList()
            }
        }
    }
}

// Статический хелпер для использования в Composable без инжекции
object AudiusApiServiceHelper {
    fun getProfilePictureUrl(profilePicture: kotlinx.serialization.json.JsonObject?): String? {
        return profilePicture?.get("480x480")?.jsonPrimitive?.content
            ?: profilePicture?.get("150x150")?.jsonPrimitive?.content
            ?: profilePicture?.get("1000x1000")?.jsonPrimitive?.content
            ?: profilePicture?.get("200x200")?.jsonPrimitive?.content
    }
}
