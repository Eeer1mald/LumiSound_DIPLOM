package com.example.lumisound.data.remote

import com.example.lumisound.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.delete
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.HttpHeaders
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class SupabaseTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Int? = null,
    val user: SupabaseUser? = null
)

@Serializable
data class SupabaseUser(
    val id: String? = null,
    val email: String? = null,
    @SerialName("email_confirmed_at") val emailConfirmedAt: String? = null
)

@Serializable
data class SupabaseSignInRequest(
    val email: String,
    val password: String
)

@Serializable
data class SupabaseSignUpRequest(
    val email: String,
    val password: String,
    val options: SupabaseSignUpOptions? = null
)

@Serializable
data class SupabaseSignUpOptions(
    @SerialName("email_redirect_to") val emailRedirectTo: String
)

@Serializable
data class SupabaseAuthError(
    val error: String? = null,
    @SerialName("error_description") val description: String? = null
)

@Singleton
class SupabaseService @Inject constructor(
    private val http: HttpClient
) {
    private val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val anonKey = BuildConfig.SUPABASE_ANON_KEY

    suspend fun signIn(email: String, password: String): SupabaseTokenResponse {
        val response = http.post {
            url("$baseUrl/auth/v1/token?grant_type=password")
            header("apikey", anonKey)
            header(HttpHeaders.Authorization, "Bearer $anonKey")
            contentType(ContentType.Application.Json)
            setBody(SupabaseSignInRequest(email, password))
        }
        val text = response.bodyAsText()
        if (response.status.isSuccess()) {
            return try {
                // Создаем Json с теми же настройками, что и в NetworkModule
                val json = Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                    isLenient = true
                }
                val tokenResponse = json.decodeFromString(SupabaseTokenResponse.serializer(), text)
                
                // Если user ID нет в ответе, получаем его через /auth/v1/user
                if (tokenResponse.user?.id == null && tokenResponse.accessToken.isNotBlank()) {
                    try {
                        val userResponse = http.get {
                            url("$baseUrl/auth/v1/user")
                            header("apikey", anonKey)
                            header(HttpHeaders.Authorization, "Bearer ${tokenResponse.accessToken}")
                        }
                        if (userResponse.status.isSuccess()) {
                            val userText = userResponse.bodyAsText()
                            val user = json.decodeFromString(SupabaseUser.serializer(), userText)
                            // Обновляем токен ответ с информацией о пользователе
                            return tokenResponse.copy(user = user)
                        }
                    } catch (e: Exception) {
                        Log.w("SupabaseService", "Не удалось получить информацию о пользователе: ${e.message}")
                        // Продолжаем с тем, что есть
                    }
                }
                
                tokenResponse
            } catch (e: SerializationException) {
                Log.e("SupabaseService", "Ошибка парсинга ответа: ${e.message}")
                Log.e("SupabaseService", "Ответ от сервера: $text")
                throw IllegalStateException("Ошибка обработки ответа от сервера. Попробуйте еще раз.")
            } catch (e: Exception) {
                Log.e("SupabaseService", "Неожиданная ошибка при входе: ${e.message}")
                Log.e("SupabaseService", "Ответ от сервера: $text")
                throw IllegalStateException("Не удалось войти. Попробуйте еще раз.")
            }
        } else {
            // Try to parse structured error
            val authError = runCatching { Json.decodeFromString(SupabaseAuthError.serializer(), text) }.getOrNull()
            val message = when (authError?.error) {
                "email_not_confirmed" -> "Подтвердите email — письмо отправлено на вашу почту."
                "invalid_grant" -> "Неверный email или пароль."
                else -> authError?.description ?: "Не удалось войти (${response.status})."
            }
            throw IllegalStateException(message)
        }
    }
    
    suspend fun getUser(accessToken: String): SupabaseUser? {
        return try {
            val response = http.get {
                url("$baseUrl/auth/v1/user")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }
            if (response.status.isSuccess()) {
                val json = Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                    isLenient = true
                }
                json.decodeFromString(SupabaseUser.serializer(), response.bodyAsText())
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w("SupabaseService", "Ошибка получения пользователя: ${e.message}")
            null
        }
    }

    suspend fun signUp(email: String, password: String, redirectUrl: String? = null): Unit {
        val request = SupabaseSignUpRequest(
            email = email,
            password = password,
            options = redirectUrl?.let { SupabaseSignUpOptions(emailRedirectTo = it) }
        )

        val response = http.post {
            url("$baseUrl/auth/v1/signup")
            header("apikey", anonKey)
            header(HttpHeaders.Authorization, "Bearer $anonKey")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (!response.status.isSuccess()) {
            val text = response.bodyAsText()
            val authError = runCatching { Json.decodeFromString(SupabaseAuthError.serializer(), text) }.getOrNull()
            val message = authError?.description ?: "Регистрация не удалась (${response.status})."
            throw IllegalStateException(message)
        }
    }

    @Serializable
    data class ProfileInsert(
        val id: String? = null, // optional if you use auth.uid() default
        val username: String,
        val email: String,
        val bio: String? = null,
        @SerialName("favorite_genre") val favoriteGenre: String? = null,
        @SerialName("avatar_url") val avatarUrl: String? = null
    )

    suspend fun upsertProfile(accessToken: String, userId: String?, username: String, email: String, bio: String? = null, favoriteGenre: String? = null, avatarUrl: String? = null) {
        // Получаем user ID из токена, если он не передан
        var finalUserId = userId
        if (finalUserId == null) {
            finalUserId = getUser(accessToken)?.id
            Log.d("SupabaseService", "Получен user ID из токена: $finalUserId")
        }
        
        // Используем POST с upsert - PostgREST будет сопоставлять по PRIMARY KEY (id), если указан
        // Или по UNIQUE constraint (email), если id не указан
        val response = http.post {
            url("$baseUrl/rest/v1/profiles")
            header("apikey", anonKey)
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            header("Prefer", "resolution=merge-duplicates,return=minimal")
            contentType(ContentType.Application.Json)
            setBody(listOf(ProfileInsert(
                id = finalUserId,
                username = username,
                email = email,
                bio = bio,
                favoriteGenre = favoriteGenre,
                avatarUrl = avatarUrl
            )))
        }
        
        if (!response.status.isSuccess()) {
            val text = response.bodyAsText()
            Log.e("SupabaseService", "Ошибка сохранения профиля: $text")
            Log.e("SupabaseService", "Status: ${response.status}, userId: $finalUserId, email: $email, username: $username")
            
            // Если получили 404 и у нас нет userId, возможно таблица использует другой подход
            // Попробуем без id (будет использован email для сопоставления, если email уникален)
            if (response.status.value == 404 && finalUserId != null) {
                Log.w("SupabaseService", "Попытка сохранить профиль без id, используя только email")
                val retryResponse = http.post {
                    url("$baseUrl/rest/v1/profiles")
                    header("apikey", anonKey)
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                    header("Prefer", "resolution=merge-duplicates,return=minimal")
                    contentType(ContentType.Application.Json)
                    setBody(listOf(ProfileInsert(
                        username = username,
                        email = email,
                        bio = bio,
                        favoriteGenre = favoriteGenre,
                        avatarUrl = avatarUrl
                    )))
                }
                if (!retryResponse.status.isSuccess()) {
                    val retryText = retryResponse.bodyAsText()
                    Log.e("SupabaseService", "Повторная попытка также не удалась: $retryText")
                    throw IllegalStateException("Не удалось сохранить профиль: ${retryResponse.status}. Ответ: $retryText")
                }
            } else {
                throw IllegalStateException("Не удалось сохранить профиль: ${response.status}. Ответ: $text")
            }
        }
    }
    
    suspend fun uploadAvatar(accessToken: String, userId: String, fileBytes: ByteArray, fileName: String): String {
        // Upload to Supabase Storage bucket "avatars"
        // Используем правильный endpoint для загрузки файла
        val response = http.post {
            url("$baseUrl/storage/v1/object/avatars/$fileName")
            header("apikey", anonKey)
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            header("x-upsert", "true") // Overwrite if exists
            header("Content-Type", "image/jpeg")
            setBody(fileBytes)
        }
        
        if (!response.status.isSuccess()) {
            val text = response.bodyAsText()
            Log.e("SupabaseService", "Ошибка загрузки аватара: $text")
            Log.e("SupabaseService", "Status code: ${response.status.value}")
            throw IllegalStateException("Не удалось загрузить аватар: ${response.status}. Ответ: $text")
        }
        
        // Return public URL для доступа к файлу
        // Формат: {baseUrl}/storage/v1/object/public/{bucket}/{path}
        val publicUrl = "$baseUrl/storage/v1/object/public/avatars/$fileName"
        Log.d("SupabaseService", "Аватар успешно загружен, публичный URL: $publicUrl")
        return publicUrl
    }

    // Удаляет файл аватара из Storage по его URL
    suspend fun deleteAvatarByUrl(accessToken: String, avatarUrl: String): Result<Unit> {
        return runCatching {
            // Извлекаем имя файла из URL
            // URL формат: .../storage/v1/object/public/avatars/filename.jpg
            val fileName = avatarUrl.substringAfterLast("/avatars/")
            if (fileName.isBlank() || fileName == avatarUrl) {
                Log.w("SupabaseService", "Не удалось извлечь имя файла из URL: $avatarUrl")
                return@runCatching
            }
            Log.d("SupabaseService", "Удаляем старый аватар: $fileName")
            val response = http.post {
                url("$baseUrl/storage/v1/object/avatars")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                // Supabase Storage delete API принимает список файлов
                setBody("""{"prefixes":["$fileName"]}""")
            }
            // Используем DELETE endpoint
            val deleteResponse = http.delete {
                url("$baseUrl/storage/v1/object/avatars/$fileName")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }
            if (deleteResponse.status.isSuccess()) {
                Log.d("SupabaseService", "Старый аватар удалён: $fileName")
            } else {
                Log.w("SupabaseService", "Не удалось удалить аватар: ${deleteResponse.status}")
            }
        }
    }
    
    @Serializable
    data class ProfileResponse(
        val id: String,
        val username: String,
        val email: String,
        val bio: String? = null,
        @SerialName("favorite_genre") val favoriteGenre: String? = null,
        @SerialName("avatar_url") val avatarUrl: String? = null,
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("updated_at") val updatedAt: String? = null
    )
    
    suspend fun getProfile(accessToken: String): ProfileResponse? {
        return try {
            val userId = getUser(accessToken)?.id
            if (userId == null) {
                Log.w("SupabaseService", "Не удалось получить user ID")
                return null
            }
            
            val response = http.get {
                url("$baseUrl/rest/v1/profiles")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                header("select", "id,username,email,bio,favorite_genre,avatar_url,created_at,updated_at")
                // Фильтруем по id текущего пользователя
                parameter("id", "eq.$userId")
            }
            
            if (response.status.isSuccess()) {
                val json = Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                    isLenient = true
                }
                val profiles = json.decodeFromString<List<ProfileResponse>>(response.bodyAsText())
                profiles.firstOrNull()
            } else {
                Log.w("SupabaseService", "Ошибка получения профиля: ${response.status}")
                null
            }
        } catch (e: Exception) {
            Log.e("SupabaseService", "Ошибка получения профиля: ${e.message}")
            null
        }
    }
    
    // ========== FAVORITE TRACKS ==========
    @Serializable
    data class FavoriteTrackResponse(
        val id: String,
        @SerialName("user_id") val userId: String,
        @SerialName("track_id") val trackId: String,
        @SerialName("track_title") val trackTitle: String,
        @SerialName("track_artist") val trackArtist: String,
        @SerialName("track_cover_url") val trackCoverUrl: String? = null,
        @SerialName("track_preview_url") val trackPreviewUrl: String? = null,
        @SerialName("play_count") val playCount: Int = 0,
        @SerialName("added_at") val addedAt: String
    )
    
    @Serializable
    data class FavoriteTrackInsert(
        @SerialName("track_id") val trackId: String,
        @SerialName("track_title") val trackTitle: String,
        @SerialName("track_artist") val trackArtist: String,
        @SerialName("track_cover_url") val trackCoverUrl: String? = null,
        @SerialName("track_preview_url") val trackPreviewUrl: String? = null
    )
    
    suspend fun getFavoriteTracks(accessToken: String, limit: Int = 20, orderByPlayCount: Boolean = false): List<FavoriteTrackResponse> {
        return try {
            val userId = getUser(accessToken)?.id ?: return emptyList()
            
            val orderBy = if (orderByPlayCount) {
                "play_count.desc,added_at.desc"
            } else {
                "added_at.desc"
            }
            
            val response = http.get {
                url("$baseUrl/rest/v1/favorite_tracks")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                header("select", "*")
                parameter("user_id", "eq.$userId")
                parameter("order", orderBy)
                parameter("limit", limit.toString())
            }
            
            if (response.status.isSuccess()) {
                val json = Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                    isLenient = true
                }
                json.decodeFromString<List<FavoriteTrackResponse>>(response.bodyAsText())
            } else {
                Log.w("SupabaseService", "Ошибка получения избранных треков: ${response.status}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("SupabaseService", "Ошибка получения избранных треков: ${e.message}")
            emptyList()
        }
    }
    
    suspend fun addFavoriteTrack(accessToken: String, track: FavoriteTrackInsert): Result<Unit> {
        return runCatching {
            val response = http.post {
                url("$baseUrl/rest/v1/favorite_tracks")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                header("Prefer", "resolution=merge-duplicates")
                contentType(ContentType.Application.Json)
                setBody(listOf(track))
            }
            
            if (!response.status.isSuccess()) {
                val text = response.bodyAsText()
                Log.e("SupabaseService", "Ошибка добавления избранного трека: $text")
                throw IllegalStateException("Не удалось добавить трек: ${response.status}")
            }
        }
    }
    
    suspend fun removeFavoriteTrack(accessToken: String, trackId: String): Result<Unit> {
        return runCatching {
            val userId = getUser(accessToken)?.id
                ?: throw IllegalStateException("Не удалось получить user ID")
            
            val response = http.delete {
                url("$baseUrl/rest/v1/favorite_tracks?user_id=eq.$userId&track_id=eq.$trackId")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }
            
            if (!response.status.isSuccess()) {
                val text = response.bodyAsText()
                Log.e("SupabaseService", "Ошибка удаления избранного трека: $text")
                throw IllegalStateException("Не удалось удалить трек: ${response.status}")
            }
        }
    }
    
    // ========== FAVORITE ARTISTS ==========
    @Serializable
    data class FavoriteArtistResponse(
        val id: String,
        @SerialName("user_id") val userId: String,
        @SerialName("artist_id") val artistId: String,
        @SerialName("artist_name") val artistName: String,
        @SerialName("artist_image_url") val artistImageUrl: String? = null,
        @SerialName("play_count") val playCount: Int = 0,
        @SerialName("added_at") val addedAt: String
    )
    
    @Serializable
    data class FavoriteArtistInsert(
        @SerialName("artist_id") val artistId: String,
        @SerialName("artist_name") val artistName: String,
        @SerialName("artist_image_url") val artistImageUrl: String? = null
    )
    
    suspend fun getFavoriteArtists(accessToken: String, limit: Int = 20): List<FavoriteArtistResponse> {
        return try {
            val userId = getUser(accessToken)?.id ?: return emptyList()
            
            val response = http.get {
                url("$baseUrl/rest/v1/favorite_artists")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                header("select", "*")
                parameter("user_id", "eq.$userId")
                parameter("order", "play_count.desc,added_at.desc")
                parameter("limit", limit.toString())
            }
            
            if (response.status.isSuccess()) {
                val json = Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                    isLenient = true
                }
                json.decodeFromString<List<FavoriteArtistResponse>>(response.bodyAsText())
            } else {
                Log.w("SupabaseService", "Ошибка получения любимых артистов: ${response.status}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("SupabaseService", "Ошибка получения любимых артистов: ${e.message}")
            emptyList()
        }
    }
    
    suspend fun addFavoriteArtist(accessToken: String, artist: FavoriteArtistInsert): Result<Unit> {
        return runCatching {
            val response = http.post {
                url("$baseUrl/rest/v1/favorite_artists")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                header("Prefer", "resolution=merge-duplicates")
                contentType(ContentType.Application.Json)
                setBody(listOf(artist))
            }
            
            if (!response.status.isSuccess()) {
                val text = response.bodyAsText()
                Log.e("SupabaseService", "Ошибка добавления любимого артиста: $text")
                throw IllegalStateException("Не удалось добавить артиста: ${response.status}")
            }
        }
    }
    
    // ========== TRACK HISTORY ==========
    @Serializable
    data class TrackHistoryInsert(
        @SerialName("track_id") val trackId: String,
        @SerialName("track_title") val trackTitle: String,
        @SerialName("track_artist") val trackArtist: String,
        @SerialName("track_artist_id") val trackArtistId: String? = null
    )
    
    suspend fun addTrackHistory(accessToken: String, track: TrackHistoryInsert): Result<Unit> {
        return runCatching {
            val response = http.post {
                url("$baseUrl/rest/v1/track_history")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(listOf(track))
            }
            
            if (!response.status.isSuccess()) {
                val text = response.bodyAsText()
                Log.w("SupabaseService", "Ошибка добавления в историю: $text")
                // Не бросаем исключение, это не критично
            }
        }
    }
    
    // ========== TRACK PLAY COUNT TRACKING ==========
    suspend fun incrementTrackPlayCount(
        accessToken: String,
        trackId: String,
        trackTitle: String,
        trackArtist: String,
        trackCoverUrl: String? = null,
        trackPreviewUrl: String? = null
    ): Result<Unit> {
        return runCatching {
            val userId = getUser(accessToken)?.id
                ?: throw IllegalStateException("Не удалось получить user ID")
            
            Log.d("SupabaseService", "Вызываем increment_track_play_count для трека: $trackTitle ($trackId), userId: $userId")
            
            // Вызываем функцию increment_track_play_count через RPC
            val requestBody = mapOf(
                "p_user_id" to userId,
                "p_track_id" to trackId,
                "p_track_title" to trackTitle,
                "p_track_artist" to trackArtist,
                "p_track_cover_url" to (trackCoverUrl ?: ""),
                "p_track_preview_url" to (trackPreviewUrl ?: "")
            )
            
            Log.d("SupabaseService", "Тело запроса: $requestBody")
            
            val response = http.post {
                url("$baseUrl/rest/v1/rpc/increment_track_play_count")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                header("Content-Type", "application/json")
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            
            val responseText = response.bodyAsText()
            Log.d("SupabaseService", "Ответ от RPC: статус=${response.status.value}, тело=$responseText")
            
            if (!response.status.isSuccess()) {
                Log.e("SupabaseService", "Ошибка увеличения счетчика трека: статус=${response.status.value}, ответ=$responseText")
                throw IllegalStateException("Не удалось увеличить счетчик трека: ${response.status}. Ответ: $responseText")
            } else {
                Log.d("SupabaseService", "✅ Счетчик трека успешно увеличен")
            }
        }
    }
    
    suspend fun incrementArtistPlayCount(
        accessToken: String,
        artistId: String,
        artistName: String,
        artistImageUrl: String? = null
    ): Result<Unit> {
        return runCatching {
            val userId = getUser(accessToken)?.id
                ?: throw IllegalStateException("Не удалось получить user ID")
            
            Log.d("SupabaseService", "Вызываем increment_artist_play_count для артиста: $artistName ($artistId), userId: $userId")
            
            // Вызываем функцию increment_artist_play_count через RPC
            val requestBody = mapOf(
                "p_user_id" to userId,
                "p_artist_id" to artistId,
                "p_artist_name" to artistName,
                "p_artist_image_url" to (artistImageUrl ?: "")
            )
            
            Log.d("SupabaseService", "Тело запроса: $requestBody")
            
            val response = http.post {
                url("$baseUrl/rest/v1/rpc/increment_artist_play_count")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                header("Content-Type", "application/json")
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            
            val responseText = response.bodyAsText()
            Log.d("SupabaseService", "Ответ от RPC: статус=${response.status.value}, тело=$responseText")
            
            if (!response.status.isSuccess()) {
                Log.e("SupabaseService", "Ошибка увеличения счетчика артиста: статус=${response.status.value}, ответ=$responseText")
                throw IllegalStateException("Не удалось увеличить счетчик артиста: ${response.status}. Ответ: $responseText")
            } else {
                Log.d("SupabaseService", "✅ Счетчик артиста успешно увеличен")
            }
        }
    }

    // ========== TRACK RATINGS ==========

    @Serializable
    data class TrackRatingInsert(
        @SerialName("audius_track_id") val audiusTrackId: String,
        @SerialName("track_title") val trackTitle: String,
        @SerialName("track_artist") val trackArtist: String,
        @SerialName("track_cover_url") val trackCoverUrl: String? = null,
        @SerialName("rhyme_score") val rhymeScore: Int? = null,
        @SerialName("imagery_score") val imageryScore: Int? = null,
        @SerialName("structure_score") val structureScore: Int? = null,
        @SerialName("charisma_score") val charismaScore: Int? = null,
        @SerialName("atmosphere_score") val atmosphereScore: Int? = null,
        val review: String? = null,
        val username: String? = null,
        @SerialName("user_avatar_url") val userAvatarUrl: String? = null
    )

    @Serializable
    data class TrackRatingResponse(
        val id: String,
        @SerialName("user_id") val userId: String,
        @SerialName("audius_track_id") val audiusTrackId: String,
        @SerialName("track_title") val trackTitle: String,
        @SerialName("track_artist") val trackArtist: String,
        @SerialName("track_cover_url") val trackCoverUrl: String? = null,
        @SerialName("rhyme_score") val rhymeScore: Int? = null,
        @SerialName("imagery_score") val imageryScore: Int? = null,
        @SerialName("structure_score") val structureScore: Int? = null,
        @SerialName("charisma_score") val charismaScore: Int? = null,
        @SerialName("atmosphere_score") val atmosphereScore: Int? = null,
        @SerialName("overall_score") val overallScore: Double? = null,
        val review: String? = null,
        val username: String? = null,
        @SerialName("user_avatar_url") val userAvatarUrl: String? = null,
        val reputation: Int = 0,
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("updated_at") val updatedAt: String? = null
    )

    suspend fun upsertTrackRating(accessToken: String, rating: TrackRatingInsert): Result<TrackRatingResponse> {
        return runCatching {
            val userId = getUser(accessToken)?.id
                ?: throw IllegalStateException("Не удалось получить user ID")
            val json = Json { ignoreUnknownKeys = true; explicitNulls = false; isLenient = true }

            val body = buildString {
                append("{")
                append("\"user_id\":\"$userId\",")
                append("\"audius_track_id\":\"${rating.audiusTrackId}\",")
                append("\"track_title\":\"${rating.trackTitle.replace("\"", "\\\"")}\",")
                append("\"track_artist\":\"${rating.trackArtist.replace("\"", "\\\"")}\",")
                rating.trackCoverUrl?.let { append("\"track_cover_url\":\"$it\",") }
                rating.rhymeScore?.let { append("\"rhyme_score\":$it,") }
                rating.imageryScore?.let { append("\"imagery_score\":$it,") }
                rating.structureScore?.let { append("\"structure_score\":$it,") }
                rating.charismaScore?.let { append("\"charisma_score\":$it,") }
                rating.atmosphereScore?.let { append("\"atmosphere_score\":$it,") }
                rating.review?.let { append("\"review\":\"${it.replace("\"", "\\\"")}\",") }
                append("\"updated_at\":\"${java.time.Instant.now()}\"")
                append("}")
            }

            val response = http.post {
                url("$baseUrl/rest/v1/track_ratings")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                // on_conflict указывает колонки для определения конфликта
                header("Prefer", "resolution=merge-duplicates,return=representation")
                parameter("on_conflict", "user_id,audius_track_id")
                contentType(ContentType.Application.Json)
                setBody("[$body]")
            }
            val text = response.bodyAsText()
            if (!response.status.isSuccess()) throw IllegalStateException("Ошибка сохранения оценки: ${response.status}. $text")
            json.decodeFromString<List<TrackRatingResponse>>(text).first()
        }
    }

    suspend fun getMyTrackRating(accessToken: String, audiusTrackId: String): TrackRatingResponse? {
        return try {
            val userId = getUser(accessToken)?.id ?: return null
            val json = Json { ignoreUnknownKeys = true; explicitNulls = false; isLenient = true }
            val response = http.get {
                url("$baseUrl/rest/v1/track_ratings")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("user_id", "eq.$userId")
                parameter("audius_track_id", "eq.$audiusTrackId")
                parameter("limit", "1")
            }
            if (response.status.isSuccess()) {
                json.decodeFromString<List<TrackRatingResponse>>(response.bodyAsText()).firstOrNull()
            } else null
        } catch (e: Exception) { null }
    }

    suspend fun getMyRatings(accessToken: String, limit: Int = 50): List<TrackRatingResponse> {
        return try {
            val userId = getUser(accessToken)?.id ?: return emptyList()
            val json = Json { ignoreUnknownKeys = true; explicitNulls = false; isLenient = true }
            val response = http.get {
                url("$baseUrl/rest/v1/track_ratings")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("user_id", "eq.$userId")
                parameter("order", "updated_at.desc")
                parameter("limit", limit.toString())
            }
            if (response.status.isSuccess()) json.decodeFromString(response.bodyAsText()) else emptyList()
        } catch (e: Exception) { emptyList() }
    }

    // Средние оценки всех пользователей по треку
    @Serializable
    data class TrackAverageRating(
        @SerialName("avg_rhyme") val avgRhyme: Double? = null,
        @SerialName("avg_imagery") val avgImagery: Double? = null,
        @SerialName("avg_structure") val avgStructure: Double? = null,
        @SerialName("avg_charisma") val avgCharisma: Double? = null,
        @SerialName("avg_atmosphere") val avgAtmosphere: Double? = null,
        @SerialName("avg_overall") val avgOverall: Double? = null,
        @SerialName("rating_count") val ratingCount: Int = 0
    )

    suspend fun getTrackAverageRating(accessToken: String, audiusTrackId: String): TrackAverageRating? {
        return try {
            val json = Json { ignoreUnknownKeys = true; explicitNulls = false; isLenient = true }
            // Получаем все оценки по треку и считаем средние
            val response = http.get {
                url("$baseUrl/rest/v1/track_ratings")
                header("apikey", anonKey)
                // Используем anon key — политика read_all USING (true) работает для всех
                header(HttpHeaders.Authorization, "Bearer $anonKey")
                parameter("audius_track_id", "eq.$audiusTrackId")
                parameter("select", "rhyme_score,imagery_score,structure_score,charisma_score,atmosphere_score,overall_score")
            }
            if (!response.status.isSuccess()) return null
            val ratings = json.decodeFromString<List<TrackRatingResponse>>(response.bodyAsText())
            Log.d("SupabaseService", "Получено ${ratings.size} оценок для трека $audiusTrackId")
            if (ratings.isEmpty()) return null
            // Фильтруем только оценки с хотя бы одним заполненным критерием
            val validRatings = ratings.filter { r ->
                r.rhymeScore != null || r.imageryScore != null || r.structureScore != null ||
                r.charismaScore != null || r.atmosphereScore != null
            }
            if (validRatings.isEmpty()) return null
            TrackAverageRating(
                avgRhyme = validRatings.mapNotNull { it.rhymeScore?.toDouble() }.takeIf { it.isNotEmpty() }?.average(),
                avgImagery = validRatings.mapNotNull { it.imageryScore?.toDouble() }.takeIf { it.isNotEmpty() }?.average(),
                avgStructure = validRatings.mapNotNull { it.structureScore?.toDouble() }.takeIf { it.isNotEmpty() }?.average(),
                avgCharisma = validRatings.mapNotNull { it.charismaScore?.toDouble() }.takeIf { it.isNotEmpty() }?.average(),
                avgAtmosphere = validRatings.mapNotNull { it.atmosphereScore?.toDouble() }.takeIf { it.isNotEmpty() }?.average(),
                avgOverall = validRatings.mapNotNull { r ->
                    val scores = listOfNotNull(
                        r.rhymeScore?.toDouble(),
                        r.imageryScore?.toDouble(),
                        r.structureScore?.toDouble(),
                        r.charismaScore?.toDouble(),
                        r.atmosphereScore?.toDouble()
                    )
                    if (scores.isNotEmpty()) scores.average() else null
                }.takeIf { it.isNotEmpty() }?.average(),
                ratingCount = validRatings.size
            )
        } catch (e: Exception) {
            Log.e("SupabaseService", "Ошибка получения средних оценок: ${e.message}")
            null
        }
    }

    // ========== TRACK COMMENTS ==========

    @Serializable
    data class TrackCommentInsert(
        @SerialName("audius_track_id") val audiusTrackId: String,
        @SerialName("track_title") val trackTitle: String,
        @SerialName("track_artist") val trackArtist: String,
        @SerialName("track_cover_url") val trackCoverUrl: String? = null,
        val comment: String,
        val username: String? = null,
        @SerialName("user_avatar_url") val userAvatarUrl: String? = null
    )

    @Serializable
    data class TrackCommentResponse(
        val id: String,
        @SerialName("user_id") val userId: String,
        @SerialName("audius_track_id") val audiusTrackId: String,
        @SerialName("track_title") val trackTitle: String,
        @SerialName("track_artist") val trackArtist: String,
        @SerialName("track_cover_url") val trackCoverUrl: String? = null,
        val comment: String,
        val username: String? = null,
        @SerialName("user_avatar_url") val userAvatarUrl: String? = null,
        @SerialName("created_at") val createdAt: String? = null
    )

    suspend fun addTrackComment(accessToken: String, comment: TrackCommentInsert): Result<TrackCommentResponse> {
        return runCatching {
            val userId = getUser(accessToken)?.id
                ?: throw IllegalStateException("Не удалось получить user ID")
            val json = Json { ignoreUnknownKeys = true; explicitNulls = false; isLenient = true }

            val body = buildString {
                append("{")
                append("\"user_id\":\"$userId\",")
                append("\"audius_track_id\":\"${comment.audiusTrackId}\",")
                append("\"track_title\":\"${comment.trackTitle.replace("\"", "\\\"")}\",")
                append("\"track_artist\":\"${comment.trackArtist.replace("\"", "\\\"")}\",")
                comment.trackCoverUrl?.let { append("\"track_cover_url\":\"$it\",") }
                comment.username?.let { append("\"username\":\"${it.replace("\"", "\\\"")}\",") }
                comment.userAvatarUrl?.let { append("\"user_avatar_url\":\"$it\",") }
                append("\"comment\":\"${comment.comment.replace("\"", "\\\"")}\"")
                append("}")
            }

            val response = http.post {
                url("$baseUrl/rest/v1/track_comments")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                header("Prefer", "return=representation")
                contentType(ContentType.Application.Json)
                setBody("[$body]")
            }
            val text = response.bodyAsText()
            if (!response.status.isSuccess()) throw IllegalStateException("Ошибка добавления комментария: ${response.status}. $text")
            json.decodeFromString<List<TrackCommentResponse>>(text).first()
        }
    }

    suspend fun getTrackComments(accessToken: String, audiusTrackId: String): List<TrackCommentResponse> {
        return try {
            val json = Json { ignoreUnknownKeys = true; explicitNulls = false; isLenient = true }
            val response = http.get {
                url("$baseUrl/rest/v1/track_comments")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("audius_track_id", "eq.$audiusTrackId")
                parameter("order", "created_at.desc")
                parameter("limit", "100")
            }
            if (response.status.isSuccess()) json.decodeFromString(response.bodyAsText()) else emptyList()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getMyComments(accessToken: String, limit: Int = 50): List<TrackCommentResponse> {
        return try {
            val userId = getUser(accessToken)?.id ?: return emptyList()
            val json = Json { ignoreUnknownKeys = true; explicitNulls = false; isLenient = true }
            val response = http.get {
                url("$baseUrl/rest/v1/track_comments")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("user_id", "eq.$userId")
                parameter("order", "created_at.desc")
                parameter("limit", limit.toString())
            }
            if (response.status.isSuccess()) json.decodeFromString(response.bodyAsText()) else emptyList()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun deleteTrackComment(accessToken: String, commentId: String): Result<Unit> {
        return runCatching {
            val response = http.delete {
                url("$baseUrl/rest/v1/track_comments?id=eq.$commentId")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }
            if (!response.status.isSuccess()) throw IllegalStateException("Ошибка удаления комментария: ${response.status}")
        }
    }

    // ========== REVIEW VOTES (репутация рецензий) ==========

    suspend fun getTrackReviews(accessToken: String, audiusTrackId: String): List<TrackRatingResponse> {
        return try {
            val json = Json { ignoreUnknownKeys = true; explicitNulls = false; isLenient = true }
            val response = http.get {
                url("$baseUrl/rest/v1/track_ratings")
                header("apikey", anonKey)
                // anon key для чтения всех рецензий (read_all policy)
                header(HttpHeaders.Authorization, "Bearer $anonKey")
                parameter("audius_track_id", "eq.$audiusTrackId")
                parameter("review", "not.is.null")
                parameter("order", "reputation.desc,created_at.desc")
            }
            if (response.status.isSuccess()) json.decodeFromString(response.bodyAsText()) else emptyList()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun voteReview(accessToken: String, ratingId: String, vote: Int): Result<Unit> {
        return runCatching {
            val userId = getUser(accessToken)?.id
                ?: throw IllegalStateException("Не удалось получить user ID")
            val json = Json { ignoreUnknownKeys = true; explicitNulls = false; isLenient = true }

            // Upsert голос
            val voteResponse = http.post {
                url("$baseUrl/rest/v1/review_votes")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                header("Prefer", "resolution=merge-duplicates,return=minimal")
                parameter("on_conflict", "user_id,rating_id")
                contentType(ContentType.Application.Json)
                setBody("""[{"user_id":"$userId","rating_id":"$ratingId","vote":$vote}]""")
            }
            if (!voteResponse.status.isSuccess()) {
                throw IllegalStateException("Ошибка голосования: ${voteResponse.status}")
            }

            // Пересчитываем репутацию
            val countResponse = http.get {
                url("$baseUrl/rest/v1/review_votes")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("rating_id", "eq.$ratingId")
                parameter("select", "vote")
            }
            if (countResponse.status.isSuccess()) {
                @Serializable data class VoteRow(val vote: Int)
                val votes = json.decodeFromString<List<VoteRow>>(countResponse.bodyAsText())
                val reputation = votes.sumOf { it.vote }
                http.post {
                    url("$baseUrl/rest/v1/track_ratings")
                    header("apikey", anonKey)
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                    header("Prefer", "resolution=merge-duplicates,return=minimal")
                    parameter("on_conflict", "id")
                    contentType(ContentType.Application.Json)
                    setBody("""[{"id":"$ratingId","reputation":$reputation}]""")
                }
            }
        }
    }

    suspend fun getMyVoteForReview(accessToken: String, ratingId: String): Int? {
        return try {
            val userId = getUser(accessToken)?.id ?: return null
            val json = Json { ignoreUnknownKeys = true; explicitNulls = false; isLenient = true }
            @Serializable data class VoteRow(val vote: Int)
            val response = http.get {
                url("$baseUrl/rest/v1/review_votes")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("user_id", "eq.$userId")
                parameter("rating_id", "eq.$ratingId")
                parameter("select", "vote")
                parameter("limit", "1")
            }
            if (response.status.isSuccess()) {
                json.decodeFromString<List<VoteRow>>(response.bodyAsText()).firstOrNull()?.vote
            } else null
        } catch (e: Exception) { null }
    }
}
