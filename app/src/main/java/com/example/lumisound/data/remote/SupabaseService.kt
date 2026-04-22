package com.example.lumisound.data.remote

import com.example.lumisound.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.patch
import io.ktor.client.request.put
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
    
    /** Обновляет access_token используя refresh_token */
    suspend fun refreshToken(refreshToken: String): SupabaseTokenResponse? {
        return try {
            val response = http.post {
                url("$baseUrl/auth/v1/token?grant_type=refresh_token")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $anonKey")
                contentType(ContentType.Application.Json)
                setBody("""{"refresh_token":"$refreshToken"}""")
            }
            if (response.status.isSuccess()) {
                val json = Json { ignoreUnknownKeys = true; explicitNulls = false; isLenient = true }
                json.decodeFromString(SupabaseTokenResponse.serializer(), response.bodyAsText())
            } else {
                Log.w("SupabaseService", "refreshToken failed: ${response.status}")
                null
            }
        } catch (e: Exception) {
            Log.e("SupabaseService", "refreshToken error: ${e.message}")
            null
        }
    }

    suspend fun getUser(accessToken: String): SupabaseUser? {        return try {
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

    /** Смена пароля через Supabase Auth API */
    suspend fun changePassword(accessToken: String, newPassword: String): Result<Unit> {
        return runCatching {
            val response = http.put {
                url("$baseUrl/auth/v1/user")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody("""{"password":"$newPassword"}""")
            }
            if (!response.status.isSuccess()) {
                val text = response.bodyAsText()
                throw IllegalStateException("Ошибка смены пароля: ${response.status}. $text")
            }
        }
    }

    /** Обновить поле is_public в профиле */
    suspend fun updateProfileVisibility(accessToken: String, isPublic: Boolean): Result<Unit> {
        return runCatching {
            val userId = getUser(accessToken)?.id ?: throw IllegalStateException("Не авторизован")
            val response = http.patch {
                url("$baseUrl/rest/v1/profiles?id=eq.$userId")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody("""{"is_public":$isPublic}""")
            }
            if (!response.status.isSuccess()) {
                val text = response.bodyAsText()
                throw IllegalStateException("Ошибка обновления видимости: ${response.status}. $text")
            }
        }
    }

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

    /** Получить профиль любого пользователя по его UUID */
    suspend fun getProfileById(userId: String): ProfileResponse? {
        return try {
            val json = Json { ignoreUnknownKeys = true; explicitNulls = false; isLenient = true }
            val response = http.get {
                url("$baseUrl/rest/v1/profiles")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $anonKey")
                parameter("id", "eq.$userId")
                parameter("select", "id,username,email,bio,favorite_genre,avatar_url,created_at,updated_at")
                parameter("limit", "1")
            }
            if (response.status.isSuccess()) {
                json.decodeFromString<List<ProfileResponse>>(response.bodyAsText()).firstOrNull()
            } else null
        } catch (e: Exception) {
            Log.w("SupabaseService", "getProfileById error: ${e.message}")
            null
        }
    }

    /** Батч-загрузка профилей по списку UUID — один запрос вместо N */
    suspend fun getProfilesByIds(userIds: List<String>): Map<String, ProfileResponse> {
        if (userIds.isEmpty()) return emptyMap()
        return try {
            val json = Json { ignoreUnknownKeys = true; explicitNulls = false; isLenient = true }
            val ids = userIds.distinct().joinToString(",")
            val response = http.get {
                url("$baseUrl/rest/v1/profiles")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $anonKey")
                parameter("id", "in.($ids)")
                parameter("select", "id,username,avatar_url")
            }
            if (response.status.isSuccess()) {
                @Serializable data class MinProfile(
                    val id: String,
                    val username: String,
                    @SerialName("avatar_url") val avatarUrl: String? = null
                )
                val profiles = json.decodeFromString<List<MinProfile>>(response.bodyAsText())
                profiles.associate { p ->
                    p.id to ProfileResponse(
                        id = p.id, username = p.username, email = "",
                        avatarUrl = p.avatarUrl
                    )
                }
            } else emptyMap()
        } catch (e: Exception) {
            Log.w("SupabaseService", "getProfilesByIds error: ${e.message}")
            emptyMap()
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
    
    /** Получить избранные треки любого пользователя по его userId (используется для синтеза) */
    suspend fun getFavoriteTracksForUser(accessToken: String, userId: String, limit: Int = 30): List<FavoriteTrackResponse> {
        return try {
            val response = http.get {
                url("$baseUrl/rest/v1/favorite_tracks")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                header("select", "*")
                parameter("user_id", "eq.$userId")
                parameter("order", "play_count.desc,added_at.desc")
                parameter("limit", limit.toString())
            }
            if (response.status.isSuccess()) {
                val json = Json { ignoreUnknownKeys = true; explicitNulls = false; isLenient = true }
                json.decodeFromString<List<FavoriteTrackResponse>>(response.bodyAsText())
            } else {
                Log.w("SupabaseService", "getFavoriteTracksForUser error: ${response.status}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("SupabaseService", "getFavoriteTracksForUser error: ${e.message}")
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
    
    @Serializable
    data class TrackHistoryResponse(
        val id: String,
        @SerialName("track_id") val trackId: String,
        @SerialName("track_title") val trackTitle: String,
        @SerialName("track_artist") val trackArtist: String,
        @SerialName("played_at") val playedAt: String? = null
    )

    suspend fun getTrackHistory(accessToken: String, limit: Int = 200): List<TrackHistoryResponse> {
        return try {
            val userId = getUser(accessToken)?.id ?: return emptyList()
            val json = Json { ignoreUnknownKeys = true; explicitNulls = false; isLenient = true }
            val response = http.get {
                url("$baseUrl/rest/v1/track_history")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("user_id", "eq.$userId")
                parameter("order", "played_at.desc")
                parameter("limit", limit.toString())
            }
            if (response.status.isSuccess()) json.decodeFromString(response.bodyAsText()) else emptyList()
        } catch (e: Exception) { emptyList() }
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
                rating.username?.let { append("\"username\":\"${it.replace("\"", "\\\"")}\",") }
                rating.userAvatarUrl?.let { append("\"user_avatar_url\":\"${it.replace("\"", "\\\"")}\",") }
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
            // Читаем из VIEW track_ratings_avg — видит все оценки всех пользователей
            val response = http.get {
                url("$baseUrl/rest/v1/track_ratings_avg")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $anonKey")
                parameter("audius_track_id", "eq.$audiusTrackId")
                parameter("limit", "1")
            }
            if (!response.status.isSuccess()) return null
            val text = response.bodyAsText()
            Log.d("SupabaseService", "track_ratings_avg: $text")
            if (text.isBlank() || text == "[]") return null
            @Serializable data class ViewRow(
                @SerialName("avg_rhyme") val avgRhyme: Double? = null,
                @SerialName("avg_imagery") val avgImagery: Double? = null,
                @SerialName("avg_structure") val avgStructure: Double? = null,
                @SerialName("avg_charisma") val avgCharisma: Double? = null,
                @SerialName("avg_atmosphere") val avgAtmosphere: Double? = null,
                @SerialName("avg_overall") val avgOverall: Double? = null,
                @SerialName("rating_count") val ratingCount: Int = 0
            )
            val rows = json.decodeFromString<List<ViewRow>>(text)
            val row = rows.firstOrNull() ?: return null
            if (row.ratingCount == 0) return null
            TrackAverageRating(
                avgRhyme = row.avgRhyme,
                avgImagery = row.avgImagery,
                avgStructure = row.avgStructure,
                avgCharisma = row.avgCharisma,
                avgAtmosphere = row.avgAtmosphere,
                avgOverall = row.avgOverall,
                ratingCount = row.ratingCount
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
                header(HttpHeaders.Authorization, "Bearer $anonKey")
                parameter("audius_track_id", "eq.$audiusTrackId")
                parameter("review", "not.is.null")
                parameter("order", "reputation.desc,created_at.desc")
            }
            if (response.status.isSuccess()) json.decodeFromString(response.bodyAsText()) else emptyList()
        } catch (e: Exception) { emptyList() }
    }

    // Лучшие рецензии по любимым трекам (приоритет) и артистам
    suspend fun getBestReviewsForFavorites(
        accessToken: String,
        favoriteTrackIds: List<String>,
        favoriteArtistNames: List<String>,
        limit: Int = 30
    ): List<TrackRatingResponse> {
        return try {
            val json = Json { ignoreUnknownKeys = true; explicitNulls = false; isLenient = true }
            val all = mutableListOf<TrackRatingResponse>()

            // Рецензии по любимым трекам
            if (favoriteTrackIds.isNotEmpty()) {
                val ids = favoriteTrackIds.joinToString(",")
                val r = http.get {
                    url("$baseUrl/rest/v1/track_ratings")
                    header("apikey", anonKey)
                    header(HttpHeaders.Authorization, "Bearer $anonKey")
                    parameter("audius_track_id", "in.($ids)")
                    parameter("review", "not.is.null")
                    parameter("order", "reputation.desc,overall_score.desc")
                    parameter("limit", limit.toString())
                }
                if (r.status.isSuccess()) all += json.decodeFromString<List<TrackRatingResponse>>(r.bodyAsText())
            }

            // Рецензии по любимым артистам (меньший приоритет)
            if (favoriteArtistNames.isNotEmpty()) {
                favoriteArtistNames.take(5).forEach { artistName ->
                    val r = http.get {
                        url("$baseUrl/rest/v1/track_ratings")
                        header("apikey", anonKey)
                        header(HttpHeaders.Authorization, "Bearer $anonKey")
                        parameter("track_artist", "ilike.*${artistName.take(20)}*")
                        parameter("review", "not.is.null")
                        parameter("order", "reputation.desc,overall_score.desc")
                        parameter("limit", "10")
                    }
                    if (r.status.isSuccess()) {
                        val items = json.decodeFromString<List<TrackRatingResponse>>(r.bodyAsText())
                        // Не дублируем треки из любимых
                        all += items.filter { item -> favoriteTrackIds.none { item.audiusTrackId == it } }
                    }
                }
            }

            // Сортируем: любимые треки выше, потом по репутации
            all.distinctBy { it.id }
                .sortedWith(compareByDescending<TrackRatingResponse> {
                    if (favoriteTrackIds.contains(it.audiusTrackId)) 1 else 0
                }.thenByDescending { it.reputation }.thenByDescending { it.overallScore ?: 0.0 })
                .take(limit)
        } catch (e: Exception) {
            Log.e("SupabaseService", "getBestReviewsForFavorites error: ${e.message}")
            emptyList()
        }
    }

    suspend fun deleteVoteReview(accessToken: String, ratingId: String): Result<Unit> {
        return runCatching {
            val userId = getUser(accessToken)?.id
                ?: throw IllegalStateException("Не удалось получить user ID")
            val response = http.delete {
                url("$baseUrl/rest/v1/review_votes?user_id=eq.$userId&rating_id=eq.$ratingId")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }
            // Пересчитываем репутацию
            if (response.status.isSuccess()) {
                val json = Json { ignoreUnknownKeys = true; explicitNulls = false; isLenient = true }
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

    // ========== PLAYLISTS ==========

    @Serializable
    data class PlaylistInsert(
        val name: String,
        val description: String? = null,
        @SerialName("cover_url") val coverUrl: String? = null
    )

    @Serializable
    data class PlaylistResponse(
        val id: String,
        @SerialName("user_id") val userId: String,
        val name: String,
        val description: String? = null,
        @SerialName("cover_url") val coverUrl: String? = null,
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("track_count") val trackCount: Int = 0,
        @SerialName("is_public") val isPublic: Boolean = false,
        @SerialName("likes_count") val likesCount: Int = 0,
        val username: String? = null,
        @SerialName("user_avatar_url") val userAvatarUrl: String? = null
    )

    @Serializable
    data class PlaylistTrackInsert(
        @SerialName("playlist_id") val playlistId: String,
        @SerialName("track_id") val trackId: String,
        @SerialName("track_title") val trackTitle: String,
        @SerialName("track_artist") val trackArtist: String,
        @SerialName("track_cover_url") val trackCoverUrl: String? = null,
        @SerialName("track_preview_url") val trackPreviewUrl: String? = null,
        val position: Int = 0,
        @SerialName("added_by_username") val addedByUsername: String? = null,
        @SerialName("added_by_avatar") val addedByAvatar: String? = null
    )

    @Serializable
    data class PlaylistTrackResponse(
        val id: String,
        @SerialName("playlist_id") val playlistId: String,
        @SerialName("track_id") val trackId: String,
        @SerialName("track_title") val trackTitle: String,
        @SerialName("track_artist") val trackArtist: String,
        @SerialName("track_cover_url") val trackCoverUrl: String? = null,
        @SerialName("track_preview_url") val trackPreviewUrl: String? = null,
        val position: Int = 0,
        @SerialName("added_at") val addedAt: String? = null,
        @SerialName("added_by_username") val addedByUsername: String? = null,
        @SerialName("added_by_avatar") val addedByAvatar: String? = null
    )

    suspend fun createPlaylist(accessToken: String, name: String, description: String? = null, isPublic: Boolean = false): Result<PlaylistResponse> {
        return runCatching {
            val userId = getUser(accessToken)?.id ?: throw IllegalStateException("Не авторизован")
            val profile = getProfile(accessToken)
            val json = Json { ignoreUnknownKeys = true; explicitNulls = false; isLenient = true }
            val body = buildString {
                append("{")
                append("\"user_id\":\"$userId\",")
                append("\"name\":\"${name.replace("\"", "\\\"")}\",")
                description?.let { append("\"description\":\"${it.replace("\"", "\\\"")}\",") }
                profile?.username?.let { append("\"username\":\"${it.replace("\"", "\\\"")}\",") }
                profile?.avatarUrl?.let { append("\"user_avatar_url\":\"${it.replace("\"", "\\\"")}\",") }
                append("\"is_public\":$isPublic,")
                append("\"track_count\":0,")
                append("\"likes_count\":0")
                append("}")
            }
            val response = http.post {
                url("$baseUrl/rest/v1/playlists")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                header("Prefer", "return=representation")
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            if (!response.status.isSuccess()) throw IllegalStateException("Ошибка создания плейлиста: ${response.status}")
            json.decodeFromString<List<PlaylistResponse>>(response.bodyAsText()).first()
        }
    }

    suspend fun updatePlaylistTrackCount(accessToken: String, playlistId: String, count: Int): Result<Unit> {
        return runCatching {
            val userId = getUser(accessToken)?.id ?: throw IllegalStateException("Не авторизован")
            http.patch {
                url("$baseUrl/rest/v1/playlists?id=eq.$playlistId&user_id=eq.$userId")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody("{\"track_count\":$count}")
            }
        }
    }

    suspend fun updatePlaylistVisibility(accessToken: String, playlistId: String, isPublic: Boolean): Result<Unit> {
        return runCatching {
            val userId = getUser(accessToken)?.id ?: throw IllegalStateException("Не авторизован")
            val response = http.patch {
                url("$baseUrl/rest/v1/playlists?id=eq.$playlistId&user_id=eq.$userId")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody("{\"is_public\":$isPublic}")
            }
            if (!response.status.isSuccess()) throw IllegalStateException("Ошибка обновления: ${response.status}")
        }
    }

    suspend fun uploadPlaylistCover(accessToken: String, playlistId: String, fileBytes: ByteArray): Result<String> {
        return runCatching {
            val fileName = "playlist_${playlistId}_${System.currentTimeMillis()}.jpg"
            // Пробуем playlist-covers, fallback на avatars
            val buckets = listOf("playlist-covers", "avatars")
            var lastError: Exception? = null
            for (bucket in buckets) {
                try {
                    val response = http.post {
                        url("$baseUrl/storage/v1/object/$bucket/$fileName")
                        header("apikey", anonKey)
                        header(HttpHeaders.Authorization, "Bearer $accessToken")
                        header("x-upsert", "true")
                        header("Content-Type", "image/jpeg")
                        setBody(fileBytes)
                    }
                    if (response.status.isSuccess()) {
                        return@runCatching "$baseUrl/storage/v1/object/public/$bucket/$fileName"
                    }
                    lastError = IllegalStateException("Bucket $bucket: ${response.status}")
                } catch (e: Exception) {
                    lastError = e
                }
            }
            throw lastError ?: IllegalStateException("Не удалось загрузить обложку")
        }
    }

    suspend fun updatePlaylistCover(accessToken: String, playlistId: String, coverUrl: String): Result<Unit> {
        return runCatching {
            val userId = getUser(accessToken)?.id ?: throw IllegalStateException("Не авторизован")
            val response = http.patch {
                url("$baseUrl/rest/v1/playlists?id=eq.$playlistId&user_id=eq.$userId")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody("{\"cover_url\":\"${coverUrl.replace("\"", "\\\"")}\"}")
            }
            if (!response.status.isSuccess()) throw IllegalStateException("Ошибка обновления обложки: ${response.status}")
        }
    }

    suspend fun updatePlaylistName(accessToken: String, playlistId: String, name: String, description: String?): Result<Unit> {
        return runCatching {
            val userId = getUser(accessToken)?.id ?: throw IllegalStateException("Не авторизован")
            val body = buildString {
                append("{\"name\":\"${name.replace("\"", "\\\"")}\",")
                if (description != null) append("\"description\":\"${description.replace("\"", "\\\"")}\"")
                else append("\"description\":null")
                append("}")
            }
            val response = http.patch {
                url("$baseUrl/rest/v1/playlists?id=eq.$playlistId&user_id=eq.$userId")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            if (!response.status.isSuccess()) throw IllegalStateException("Ошибка обновления: ${response.status}")
        }
    }

    suspend fun getPublicPlaylists(accessToken: String, limit: Int = 20): List<PlaylistResponse> {
        return try {
            val json = Json { ignoreUnknownKeys = true; explicitNulls = false; isLenient = true }
            val response = http.get {
                url("$baseUrl/rest/v1/playlists")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("is_public", "eq.true")
                parameter("order", "likes_count.desc,created_at.desc")
                parameter("limit", limit.toString())
            }
            if (response.status.isSuccess()) json.decodeFromString(response.bodyAsText()) else emptyList()
        } catch (e: Exception) { emptyList() }
    }

    /**
     * Поиск публичных плейлистов по запросу.
     * Алгоритм: ищем по name (ilike) — нечувствительно к регистру, частичное совпадение.
     * Сортировка: сначала точные совпадения (по likes_count), потом частичные.
     */
    suspend fun searchPublicPlaylists(query: String, limit: Int = 8): List<PlaylistResponse> {
        if (query.isBlank()) return emptyList()
        return try {
            val json = Json { ignoreUnknownKeys = true; explicitNulls = false; isLenient = true }
            val response = http.get {
                url("$baseUrl/rest/v1/playlists")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $anonKey")
                parameter("is_public", "eq.true")
                parameter("name", "ilike.*${query.trim()}*")
                parameter("order", "likes_count.desc,created_at.desc")
                parameter("limit", limit.toString())
            }
            if (response.status.isSuccess()) json.decodeFromString(response.bodyAsText()) else emptyList()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun likePlaylist(accessToken: String, playlistId: String): Result<Unit> {
        return runCatching {
            val userId = getUser(accessToken)?.id ?: throw IllegalStateException("Не авторизован")
            // Insert like — ON CONFLICT DO NOTHING через resolution=ignore-duplicates
            val likeResponse = http.post {
                url("$baseUrl/rest/v1/playlist_likes")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                header("Prefer", "resolution=ignore-duplicates,return=minimal")
                contentType(ContentType.Application.Json)
                setBody("{\"user_id\":\"$userId\",\"playlist_id\":\"$playlistId\"}")
            }
            // Обновляем счётчик только если лайк был реально добавлен (201 Created)
            if (likeResponse.status.value == 201) {
                http.patch {
                    url("$baseUrl/rest/v1/playlists?id=eq.$playlistId")
                    header("apikey", anonKey)
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                    contentType(ContentType.Application.Json)
                    setBody("{\"likes_count\":\"likes_count + 1\"}")
                }
                // Fallback через RPC если PATCH не поддерживает выражения
                runCatching {
                    http.post {
                        url("$baseUrl/rest/v1/rpc/increment_playlist_likes")
                        header("apikey", anonKey)
                        header(HttpHeaders.Authorization, "Bearer $accessToken")
                        contentType(ContentType.Application.Json)
                        setBody("{\"p_playlist_id\":\"$playlistId\"}")
                    }
                }
            }
        }
    }

    suspend fun unlikePlaylist(accessToken: String, playlistId: String): Result<Unit> {
        return runCatching {
            val userId = getUser(accessToken)?.id ?: throw IllegalStateException("Не авторизован")
            val deleteResponse = http.delete {
                url("$baseUrl/rest/v1/playlist_likes?user_id=eq.$userId&playlist_id=eq.$playlistId")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }
            if (deleteResponse.status.isSuccess()) {
                runCatching {
                    http.post {
                        url("$baseUrl/rest/v1/rpc/decrement_playlist_likes")
                        header("apikey", anonKey)
                        header(HttpHeaders.Authorization, "Bearer $accessToken")
                        contentType(ContentType.Application.Json)
                        setBody("{\"p_playlist_id\":\"$playlistId\"}")
                    }
                }
            }
        }
    }

    suspend fun getMyLikedPlaylistIds(accessToken: String): Set<String> {
        return try {
            val userId = getUser(accessToken)?.id ?: return emptySet()
            val json = Json { ignoreUnknownKeys = true; explicitNulls = false; isLenient = true }
            @Serializable data class LikeRow(@SerialName("playlist_id") val playlistId: String)
            val response = http.get {
                url("$baseUrl/rest/v1/playlist_likes")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("user_id", "eq.$userId")
                parameter("select", "playlist_id")
            }
            if (response.status.isSuccess()) {
                json.decodeFromString<List<LikeRow>>(response.bodyAsText()).map { it.playlistId }.toSet()
            } else emptySet()
        } catch (e: Exception) {
            android.util.Log.e("SupabaseService", "getMyLikedPlaylistIds error: ${e.message}")
            emptySet()
        }
    }

    suspend fun isPlaylistLiked(accessToken: String, playlistId: String): Boolean {
        return try {
            val userId = getUser(accessToken)?.id ?: return false
            val json = Json { ignoreUnknownKeys = true; explicitNulls = false; isLenient = true }
            @Serializable data class LikeRow(val id: String)
            val response = http.get {
                url("$baseUrl/rest/v1/playlist_likes")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("user_id", "eq.$userId")
                parameter("playlist_id", "eq.$playlistId")
                parameter("select", "id")
                parameter("limit", "1")
            }
            if (response.status.isSuccess()) {
                val text = response.bodyAsText()
                text.contains("\"id\"") // Проверяем что есть хотя бы одна запись
            } else false
        } catch (e: Exception) { false }
    }

    // Рекомендованные плейлисты — публичные от пользователей с похожим вкусом
    // (те, кто слушает тех же артистов)
    suspend fun getRecommendedPlaylists(accessToken: String, favoriteArtistNames: List<String>, limit: Int = 20): List<PlaylistResponse> {
        return try {
            // Берём просто публичные плейлисты, исключая свои
            val userId = getUser(accessToken)?.id ?: return emptyList()
            val json = Json { ignoreUnknownKeys = true; explicitNulls = false; isLenient = true }
            val response = http.get {
                url("$baseUrl/rest/v1/playlists")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("is_public", "eq.true")
                parameter("user_id", "neq.$userId")
                parameter("order", "likes_count.desc,created_at.desc")
                parameter("limit", limit.toString())
            }
            if (response.status.isSuccess()) json.decodeFromString(response.bodyAsText()) else emptyList()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getMyPlaylists(accessToken: String): List<PlaylistResponse> {
        return try {
            val userId = getUser(accessToken)?.id ?: return emptyList()
            val json = Json { ignoreUnknownKeys = true; explicitNulls = false; isLenient = true }
            val response = http.get {
                url("$baseUrl/rest/v1/playlists")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("user_id", "eq.$userId")
                parameter("order", "created_at.desc")
            }
            if (response.status.isSuccess()) json.decodeFromString(response.bodyAsText()) else emptyList()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun deletePlaylist(accessToken: String, playlistId: String): Result<Unit> {
        return runCatching {
            val userId = getUser(accessToken)?.id ?: throw IllegalStateException("Не авторизован")
            val response = http.delete {
                url("$baseUrl/rest/v1/playlists?id=eq.$playlistId&user_id=eq.$userId")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }
            if (!response.status.isSuccess()) throw IllegalStateException("Ошибка удаления: ${response.status}")
        }
    }

    suspend fun addTrackToPlaylist(accessToken: String, track: PlaylistTrackInsert): Result<Unit> {
        return runCatching {
            val response = http.post {
                url("$baseUrl/rest/v1/playlist_tracks")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                header("Prefer", "resolution=ignore-duplicates")
                contentType(ContentType.Application.Json)
                setBody(listOf(track))
            }
            if (!response.status.isSuccess()) throw IllegalStateException("Ошибка добавления трека: ${response.status}")
        }
    }

    suspend fun getPlaylistTracks(accessToken: String, playlistId: String): List<PlaylistTrackResponse> {
        return try {
            val json = Json { ignoreUnknownKeys = true; explicitNulls = false; isLenient = true }
            val response = http.get {
                url("$baseUrl/rest/v1/playlist_tracks")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("playlist_id", "eq.$playlistId")
                parameter("order", "position.asc,added_at.asc")
            }
            if (response.status.isSuccess()) json.decodeFromString(response.bodyAsText()) else emptyList()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun removeTrackFromPlaylist(accessToken: String, playlistId: String, trackId: String): Result<Unit> {
        return runCatching {
            val response = http.delete {
                url("$baseUrl/rest/v1/playlist_tracks?playlist_id=eq.$playlistId&track_id=eq.$trackId")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }
            if (!response.status.isSuccess()) throw IllegalStateException("Ошибка удаления трека: ${response.status}")
        }
    }

    // ========== SYNTHESIS ==========

    @Serializable
    data class SynthesisSession(
        val id: String,
        @SerialName("creator_id") val creatorId: String,
        @SerialName("creator_username") val creatorUsername: String? = null,
        @SerialName("invite_code") val inviteCode: String,
        val name: String = "Синтез",
        val status: String = "pending",
        @SerialName("created_at") val createdAt: String? = null
    )

    @Serializable
    data class SynthesisParticipant(
        val id: String,
        @SerialName("session_id") val sessionId: String,
        @SerialName("user_id") val userId: String,
        val username: String? = null,
        @SerialName("avatar_url") val avatarUrl: String? = null,
        @SerialName("joined_at") val joinedAt: String? = null
    )

    suspend fun createSynthesisSession(accessToken: String, creatorUsername: String?): Result<SynthesisSession> {
        return runCatching {
            val userId = getUser(accessToken)?.id ?: throw IllegalStateException("Не авторизован")
            val json = Json { ignoreUnknownKeys = true; explicitNulls = false; isLenient = true }
            val body = buildString {
                append("{\"creator_id\":\"$userId\"")
                creatorUsername?.let { append(",\"creator_username\":\"${it.replace("\"", "\\\"")}\"") }
                append("}")
            }
            val response = http.post {
                url("$baseUrl/rest/v1/synthesis_sessions")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                header("Prefer", "return=representation")
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            if (!response.status.isSuccess()) throw IllegalStateException("Ошибка создания синтеза: ${response.status}\n${response.bodyAsText()}")
            json.decodeFromString<List<SynthesisSession>>(response.bodyAsText()).first()
        }
    }

    suspend fun getSynthesisSession(accessToken: String, inviteCode: String): Result<SynthesisSession> {
        return runCatching {
            val json = Json { ignoreUnknownKeys = true; explicitNulls = false; isLenient = true }
            val response = http.get {
                url("$baseUrl/rest/v1/synthesis_sessions")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("invite_code", "eq.$inviteCode")
                parameter("limit", "1")
            }
            if (!response.status.isSuccess()) throw IllegalStateException("Сессия не найдена")
            json.decodeFromString<List<SynthesisSession>>(response.bodyAsText()).firstOrNull()
                ?: throw IllegalStateException("Сессия не найдена")
        }
    }

    suspend fun getMySynthesisSessions(accessToken: String): List<SynthesisSession> {
        return try {
            val userId = getUser(accessToken)?.id ?: return emptyList()
            val json = Json { ignoreUnknownKeys = true; explicitNulls = false; isLenient = true }
            val response = http.get {
                url("$baseUrl/rest/v1/synthesis_sessions")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("creator_id", "eq.$userId")
                parameter("order", "created_at.desc")
            }
            if (response.status.isSuccess()) json.decodeFromString(response.bodyAsText()) else emptyList()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun joinSynthesisSession(accessToken: String, sessionId: String, username: String?, avatarUrl: String?): Result<Unit> {
        return runCatching {
            val userId = getUser(accessToken)?.id ?: throw IllegalStateException("Не авторизован")
            val body = buildString {
                append("{\"session_id\":\"$sessionId\",\"user_id\":\"$userId\"")
                username?.let { append(",\"username\":\"${it.replace("\"", "\\\"")}\"") }
                avatarUrl?.let { append(",\"avatar_url\":\"${it.replace("\"", "\\\"")}\"") }
                append("}")
            }
            val response = http.post {
                url("$baseUrl/rest/v1/synthesis_participants")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                header("Prefer", "resolution=ignore-duplicates")
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            if (!response.status.isSuccess()) throw IllegalStateException("Ошибка присоединения: ${response.status}")
        }
    }

    suspend fun getSynthesisParticipants(accessToken: String, sessionId: String): List<SynthesisParticipant> {
        return try {
            val json = Json { ignoreUnknownKeys = true; explicitNulls = false; isLenient = true }
            val response = http.get {
                url("$baseUrl/rest/v1/synthesis_participants")
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("session_id", "eq.$sessionId")
                parameter("order", "joined_at.asc")
            }
            if (response.status.isSuccess()) json.decodeFromString(response.bodyAsText()) else emptyList()
        } catch (e: Exception) { emptyList() }
    }
}
