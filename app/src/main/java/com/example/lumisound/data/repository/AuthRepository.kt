package com.example.lumisound.data.repository

import com.example.lumisound.data.remote.SupabaseTokenResponse

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<SupabaseTokenResponse>
    suspend fun googleSignIn(idToken: String): Result<Unit>
    suspend fun signUp(email: String, password: String): Result<Unit>
    suspend fun signUpLoginCreateProfile(username: String, email: String, password: String): Result<SupabaseTokenResponse>
    suspend fun savePendingUsername(email: String, username: String)
    fun getPendingUsername(email: String): String?
    suspend fun clearPendingUsername(email: String)
    suspend fun syncProfileIfNeeded(accessToken: String, email: String)
    suspend fun saveProfile(accessToken: String, username: String, email: String, bio: String?, favoriteGenre: String?, avatarUrl: String?): Result<Unit>
    fun getUserId(): String?
    suspend fun uploadAvatar(accessToken: String, userId: String, fileBytes: ByteArray, fileName: String): Result<String>
}

