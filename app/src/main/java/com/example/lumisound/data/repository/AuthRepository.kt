package com.example.lumisound.data.repository

import com.example.lumisound.data.remote.SupabaseService
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
    suspend fun getProfile(accessToken: String): Result<SupabaseService.ProfileResponse?>
    
    // Favorite Tracks
    suspend fun getFavoriteTracks(accessToken: String, limit: Int = 20, orderByPlayCount: Boolean = false): Result<List<SupabaseService.FavoriteTrackResponse>>
    suspend fun addFavoriteTrack(accessToken: String, track: SupabaseService.FavoriteTrackInsert): Result<Unit>
    suspend fun removeFavoriteTrack(accessToken: String, trackId: String): Result<Unit>
    
    // Favorite Artists
    suspend fun getFavoriteArtists(accessToken: String, limit: Int = 20): Result<List<SupabaseService.FavoriteArtistResponse>>
    suspend fun addFavoriteArtist(accessToken: String, artist: SupabaseService.FavoriteArtistInsert): Result<Unit>
    
    // Track History
    suspend fun addTrackHistory(accessToken: String, track: SupabaseService.TrackHistoryInsert): Result<Unit>
    
    // Play Count Tracking
    suspend fun incrementTrackPlayCount(accessToken: String, trackId: String, trackTitle: String, trackArtist: String, trackCoverUrl: String? = null, trackPreviewUrl: String? = null): Result<Unit>
    suspend fun incrementArtistPlayCount(accessToken: String, artistId: String, artistName: String, artistImageUrl: String? = null): Result<Unit>
}

