package com.example.lumisound.data.repository

import com.example.lumisound.data.local.PendingUsernameStore
import com.example.lumisound.data.local.SessionManager
import com.example.lumisound.data.remote.SupabaseService
import com.example.lumisound.data.remote.SupabaseTokenResponse
import javax.inject.Inject
import javax.inject.Singleton

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
            // Сохраняем user ID если он есть в ответе
            tokenResponse.user?.id?.let { userId ->
                sessionManager.saveUserId(userId)
            }
            tokenResponse
        }
    }

    override suspend fun googleSignIn(idToken: String): Result<Unit> {
        return Result.failure(UnsupportedOperationException("Google OAuth через Supabase будет подключён отдельно"))
    }

    override suspend fun signUp(email: String, password: String): Result<Unit> {
        return runCatching {
            supabase.signUp(email, password, redirectUrl = "lumisound://auth.callback")
            Unit
        }
    }

    override suspend fun signUpLoginCreateProfile(username: String, email: String, password: String): Result<SupabaseTokenResponse> {
        return runCatching {
            // 1) Sign up (may require email confirmation depending on project settings)
            supabase.signUp(email, password, redirectUrl = "lumisound://auth.callback")
            savePendingUsername(email, username)
            // Try to login immediately after signup to get token
            val tokenResponse = supabase.signIn(email, password)
            sessionManager.saveAccessToken(tokenResponse.accessToken)
            sessionManager.saveEmail(email)
            // Сохраняем user ID если он есть в ответе
            tokenResponse.user?.id?.let { userId ->
                sessionManager.saveUserId(userId)
            }
            tokenResponse
        }
    }

    override suspend fun savePendingUsername(email: String, username: String) {
        pendingUsernameStore.save(email, username)
    }
    
    override fun getPendingUsername(email: String): String? {
        return pendingUsernameStore.get(email)
    }
    
    override suspend fun clearPendingUsername(email: String) {
        pendingUsernameStore.clear(email)
    }

    override suspend fun syncProfileIfNeeded(accessToken: String, email: String) {
        val pending = pendingUsernameStore.get(email)
        if (!pending.isNullOrBlank()) {
            runCatching {
                val userId = sessionManager.getUserId()
                supabase.upsertProfile(accessToken = accessToken, userId = userId, username = pending, email = email)
                pendingUsernameStore.clear(email)
            }
        }
    }
    
    override suspend fun saveProfile(accessToken: String, username: String, email: String, bio: String?, favoriteGenre: String?, avatarUrl: String?): Result<Unit> {
        return runCatching {
            val userId = sessionManager.getUserId()
            supabase.upsertProfile(accessToken = accessToken, userId = userId, username = username, email = email, bio = bio, favoriteGenre = favoriteGenre, avatarUrl = avatarUrl)
            Unit
        }
    }
    
    override fun getUserId(): String? {
        return sessionManager.getUserId()
    }
    
    override suspend fun uploadAvatar(accessToken: String, userId: String, fileBytes: ByteArray, fileName: String): Result<String> {
        return runCatching {
            supabase.uploadAvatar(accessToken = accessToken, userId = userId, fileBytes = fileBytes, fileName = fileName)
        }
    }
}
