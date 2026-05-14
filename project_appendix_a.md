# ПРИЛОЖЕНИЕ А
## (справочное)
## Фрагменты текста программы

// Авторизация и регистрация пользователя
```kotlin
package com.example.lumisound.data.repository

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val supabase: SupabaseService,
    private val pendingUsernameStore: PendingUsernameStore,
    private val sessionManager: SessionManager
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<SupabaseTokenResponse> {
        return runCatching {
            val tokenResponse = supabase.signIn(email, password)
            sessionManager.saveAccessToken(tokenResponse.accessToken)
            sessionManager.saveEmail(email)
            tokenResponse.user?.id?.let { userId -> sessionManager.saveUserId(userId) }
            tokenResponse.refreshToken?.let { sessionManager.saveRefreshToken(it) }
            tokenResponse.expiresIn?.let { expiresIn ->
                sessionManager.saveTokenExpiry(System.currentTimeMillis() + expiresIn * 1000L)
            }
            invalidateCache()
            tokenResponse
        }
    }

    override suspend fun signUpLoginCreateProfile(
        username: String,
        email: String,
        password: String
    ): Result<SupabaseTokenResponse> {
        return runCatching {
            supabase.signUp(
                email, password,
                redirectUrl = "https://eeer1mald.github.io/LumiSound_DIPLOM/email-confirmed.html"
            )
            savePendingUsername(email, username)
            val tokenResponse = supabase.signIn(email, password)
            sessionManager.saveAccessToken(tokenResponse.accessToken)
            sessionManager.saveEmail(email)
            tokenResponse.user?.id?.let { userId -> sessionManager.saveUserId(userId) }
            tokenResponse.refreshToken?.let { sessionManager.saveRefreshToken(it) }
            tokenResponse.expiresIn?.let { expiresIn ->
                sessionManager.saveTokenExpiry(System.currentTimeMillis() + expiresIn * 1000L)
            }
            tokenResponse
        }
    }

    override suspend fun syncProfileIfNeeded(accessToken: String, email: String) {
        val pending = pendingUsernameStore.get(email)
        if (!pending.isNullOrBlank()) {
            runCatching {
                val userId = sessionManager.getUserId()
                supabase.upsertProfile(
                    accessToken = accessToken,
                    userId = userId,
                    username = pending,
                    email = email
                )
                pendingUsernameStore.clear(email)
            }
        }
    }

    private fun invalidateCache() {
        cachedProfile = null
        cachedMyRatings = null
        cachedMyComments = null
        cachedFavTracks = null
        cachedFavArtists = null
    }
}
```
