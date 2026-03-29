package com.example.lumisound.data.remote

import com.example.lumisound.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
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
        // Upload to Supabase Storage: /storage/v1/object/avatars/{fileName}
        val response = http.post {
            url("$baseUrl/storage/v1/object/avatars/$fileName")
            header("apikey", anonKey)
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            header("x-upsert", "true") // Overwrite if exists
            contentType(ContentType.Image.JPEG)
            setBody(fileBytes)
        }
        
        if (!response.status.isSuccess()) {
            val text = response.bodyAsText()
            Log.e("SupabaseService", "Ошибка загрузки аватара: $text")
            throw IllegalStateException("Не удалось загрузить аватар: ${response.status}")
        }
        
        // Return public URL
        return "$baseUrl/storage/v1/object/public/avatars/$fileName"
    }
}
