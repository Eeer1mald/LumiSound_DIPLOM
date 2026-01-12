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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
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
    @SerialName("permalink") val permalink: String? = null,
    @SerialName("duration") val duration: Int? = null,
    @SerialName("play_count") val playCount: Int? = null
)

@Serializable
data class AudiusArtist(
    val id: String? = null,
    val name: String,
    @SerialName("profile_picture") val profilePicture: JsonObject? = null
)

@Serializable
data class AudiusSearchResponse(
    val data: List<AudiusTrack>? = null
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
    
    suspend fun searchTracks(query: String, limit: Int = 10): Result<List<AudiusTrack>> {
        return runCatching {
            makeRequestWithFallback("/v1/tracks/search") { baseUrl ->
                val response = httpClient.get {
                    url(baseUrl)
                    parameter("query", query)
                    parameter("limit", limit)
                    parameter("app_name", APP_NAME)
                }.body<String>()
                
                Log.d(TAG, "Audius API response: $response")
                
                val json = Json { 
                    ignoreUnknownKeys = true
                    isLenient = true
                    explicitNulls = false
                }
                val searchResponse = json.decodeFromString(AudiusSearchResponse.serializer(), response)
                searchResponse.data ?: emptyList()
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
    
    fun getStreamUrl(trackId: String): String {
        return "$currentHost/v1/tracks/$trackId/stream"
    }
}
