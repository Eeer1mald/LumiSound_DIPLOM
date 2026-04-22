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

    // In-memory кэш — заполняется при предзагрузке
    private var cachedProfile: SupabaseService.ProfileResponse? = null
    private var cachedMyRatings: List<SupabaseService.TrackRatingResponse>? = null
    private var cachedMyComments: List<SupabaseService.TrackCommentResponse>? = null
    private var cachedFavTracks: List<SupabaseService.FavoriteTrackResponse>? = null
    private var cachedFavArtists: List<SupabaseService.FavoriteArtistResponse>? = null

    override suspend fun login(email: String, password: String): Result<SupabaseTokenResponse> {
        return runCatching {
            val tokenResponse = supabase.signIn(email, password)
            sessionManager.saveAccessToken(tokenResponse.accessToken)
            sessionManager.saveEmail(email)
            tokenResponse.user?.id?.let { userId -> sessionManager.saveUserId(userId) }
            // Сохраняем refresh token и время истечения
            tokenResponse.refreshToken?.let { sessionManager.saveRefreshToken(it) }
            tokenResponse.expiresIn?.let { expiresIn ->
                sessionManager.saveTokenExpiry(System.currentTimeMillis() + expiresIn * 1000L)
            }
            // Инвалидируем кэш при смене аккаунта
            invalidateCache()
            tokenResponse
        }
    }

    override suspend fun refreshTokenIfNeeded(): String? {
        val currentToken = sessionManager.getAccessToken() ?: return null
        val refreshToken = sessionManager.getRefreshToken()

        // Если нет данных о времени истечения (старые пользователи) — проверяем токен реальным запросом
        if (sessionManager.getTokenExpiry() == 0L && refreshToken != null) {
            // Пробуем получить пользователя — если 401, токен истёк
            val user = supabase.getUser(currentToken)
            if (user == null) {
                // Токен невалиден — обновляем
                return performRefresh(currentToken, refreshToken)
            }
            return currentToken
        }

        // Если токен не истекает скоро — возвращаем текущий
        if (!sessionManager.isTokenExpiredOrExpiringSoon()) return currentToken

        // Пробуем обновить через refresh token
        if (refreshToken == null) return currentToken
        return performRefresh(currentToken, refreshToken)
    }

    private suspend fun performRefresh(currentToken: String, refreshToken: String): String {
        val newTokenResponse = supabase.refreshToken(refreshToken) ?: return currentToken
        sessionManager.saveAccessToken(newTokenResponse.accessToken)
        newTokenResponse.refreshToken?.let { sessionManager.saveRefreshToken(it) }
        newTokenResponse.expiresIn?.let { expiresIn ->
            sessionManager.saveTokenExpiry(System.currentTimeMillis() + expiresIn * 1000L)
        }
        invalidateCache()
        android.util.Log.d("AuthRepository", "Token refreshed successfully")
        return newTokenResponse.accessToken
    }

    override suspend fun changePassword(accessToken: String, newPassword: String): Result<Unit> {
        return supabase.changePassword(accessToken, newPassword)
    }

    override suspend fun updateProfileVisibility(accessToken: String, isPublic: Boolean): Result<Unit> {
        return supabase.updateProfileVisibility(accessToken, isPublic)
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
            // Сохраняем refresh token и время истечения
            tokenResponse.refreshToken?.let { sessionManager.saveRefreshToken(it) }
            tokenResponse.expiresIn?.let { expiresIn ->
                sessionManager.saveTokenExpiry(System.currentTimeMillis() + expiresIn * 1000L)
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
            cachedProfile = null // инвалидируем кэш профиля после обновления
            Unit
        }
    }

    private fun invalidateCache() {
        cachedProfile = null
        cachedMyRatings = null
        cachedMyComments = null
        cachedFavTracks = null
        cachedFavArtists = null
    }
    
    override fun getUserId(): String? {
        return sessionManager.getUserId()
    }
    
    override suspend fun uploadAvatar(accessToken: String, userId: String, fileBytes: ByteArray, fileName: String): Result<String> {
        return runCatching {
            supabase.uploadAvatar(accessToken = accessToken, userId = userId, fileBytes = fileBytes, fileName = fileName)
        }
    }

    override suspend fun deleteAvatarByUrl(accessToken: String, avatarUrl: String): Result<Unit> {
        return supabase.deleteAvatarByUrl(accessToken, avatarUrl)
    }
    
    override suspend fun getProfile(accessToken: String): Result<SupabaseService.ProfileResponse?> {
        cachedProfile?.let { return Result.success(it) }
        return runCatching {
            supabase.getProfile(accessToken).also { cachedProfile = it }
        }
    }

    override suspend fun getProfileById(userId: String): SupabaseService.ProfileResponse? {
        return supabase.getProfileById(userId)
    }

    override suspend fun getProfilesByIds(userIds: List<String>): Map<String, SupabaseService.ProfileResponse> {
        return supabase.getProfilesByIds(userIds)
    }
    
    override suspend fun getFavoriteTracks(accessToken: String, limit: Int, orderByPlayCount: Boolean): Result<List<SupabaseService.FavoriteTrackResponse>> {
        cachedFavTracks?.let { return Result.success(it) }
        return runCatching {
            supabase.getFavoriteTracks(accessToken, limit, orderByPlayCount).also { cachedFavTracks = it }
        }
    }

    override suspend fun getFavoriteTracksForUser(accessToken: String, userId: String, limit: Int): Result<List<SupabaseService.FavoriteTrackResponse>> {
        return runCatching {
            supabase.getFavoriteTracksForUser(accessToken, userId, limit)
        }
    }

    override suspend fun addFavoriteTrack(accessToken: String, track: SupabaseService.FavoriteTrackInsert): Result<Unit> {
        return supabase.addFavoriteTrack(accessToken, track)
    }
    
    override suspend fun removeFavoriteTrack(accessToken: String, trackId: String): Result<Unit> {
        return supabase.removeFavoriteTrack(accessToken, trackId)
    }
    
    override suspend fun getFavoriteArtists(accessToken: String, limit: Int): Result<List<SupabaseService.FavoriteArtistResponse>> {
        cachedFavArtists?.let { return Result.success(it) }
        return runCatching {
            supabase.getFavoriteArtists(accessToken, limit).also { cachedFavArtists = it }
        }
    }
    
    override suspend fun addFavoriteArtist(accessToken: String, artist: SupabaseService.FavoriteArtistInsert): Result<Unit> {
        return supabase.addFavoriteArtist(accessToken, artist)
    }
    
    override suspend fun addTrackHistory(accessToken: String, track: SupabaseService.TrackHistoryInsert): Result<Unit> {
        return supabase.addTrackHistory(accessToken, track)
    }

    override suspend fun getTrackHistory(accessToken: String, limit: Int): Result<List<SupabaseService.TrackHistoryResponse>> {
        return runCatching { supabase.getTrackHistory(accessToken, limit) }
    }
    
    override suspend fun incrementTrackPlayCount(accessToken: String, trackId: String, trackTitle: String, trackArtist: String, trackCoverUrl: String?, trackPreviewUrl: String?): Result<Unit> {
        return supabase.incrementTrackPlayCount(accessToken, trackId, trackTitle, trackArtist, trackCoverUrl, trackPreviewUrl)
    }
    
    override suspend fun incrementArtistPlayCount(accessToken: String, artistId: String, artistName: String, artistImageUrl: String?): Result<Unit> {
        return supabase.incrementArtistPlayCount(accessToken, artistId, artistName, artistImageUrl)
    }

    override suspend fun upsertTrackRating(accessToken: String, rating: SupabaseService.TrackRatingInsert): Result<SupabaseService.TrackRatingResponse> {
        return supabase.upsertTrackRating(accessToken, rating)
    }

    override suspend fun getMyTrackRating(accessToken: String, audiusTrackId: String): SupabaseService.TrackRatingResponse? {
        return supabase.getMyTrackRating(accessToken, audiusTrackId)
    }

    override suspend fun getMyRatings(accessToken: String, limit: Int): List<SupabaseService.TrackRatingResponse> {
        cachedMyRatings?.let { return it }
        return supabase.getMyRatings(accessToken, limit).also { cachedMyRatings = it }
    }

    override suspend fun getTrackAverageRating(accessToken: String, audiusTrackId: String): SupabaseService.TrackAverageRating? {
        return supabase.getTrackAverageRating(accessToken, audiusTrackId)
    }

    override suspend fun addTrackComment(accessToken: String, comment: SupabaseService.TrackCommentInsert): Result<SupabaseService.TrackCommentResponse> {
        return supabase.addTrackComment(accessToken, comment)
    }

    override suspend fun getTrackComments(accessToken: String, audiusTrackId: String): List<SupabaseService.TrackCommentResponse> {
        return supabase.getTrackComments(accessToken, audiusTrackId)
    }

    override suspend fun getMyComments(accessToken: String, limit: Int): List<SupabaseService.TrackCommentResponse> {
        cachedMyComments?.let { return it }
        return supabase.getMyComments(accessToken, limit).also { cachedMyComments = it }
    }

    override suspend fun deleteTrackComment(accessToken: String, commentId: String): Result<Unit> {
        return supabase.deleteTrackComment(accessToken, commentId)
    }

    override suspend fun getTrackReviews(accessToken: String, audiusTrackId: String): List<SupabaseService.TrackRatingResponse> {
        return supabase.getTrackReviews(accessToken, audiusTrackId)
    }

    override suspend fun getBestReviewsForFavorites(accessToken: String, favoriteTrackIds: List<String>, favoriteArtistNames: List<String>, limit: Int): List<SupabaseService.TrackRatingResponse> {
        return supabase.getBestReviewsForFavorites(accessToken, favoriteTrackIds, favoriteArtistNames, limit)
    }

    override suspend fun voteReview(accessToken: String, ratingId: String, vote: Int): Result<Unit> {
        return supabase.voteReview(accessToken, ratingId, vote)
    }

    override suspend fun deleteVoteReview(accessToken: String, ratingId: String): Result<Unit> {
        return supabase.deleteVoteReview(accessToken, ratingId)
    }

    override suspend fun getMyVoteForReview(accessToken: String, ratingId: String): Int? {
        return supabase.getMyVoteForReview(accessToken, ratingId)
    }

    override suspend fun createPlaylist(accessToken: String, name: String, description: String?, isPublic: Boolean): Result<SupabaseService.PlaylistResponse> {
        return supabase.createPlaylist(accessToken, name, description, isPublic)
    }

    override suspend fun getMyPlaylists(accessToken: String): List<SupabaseService.PlaylistResponse> {
        return supabase.getMyPlaylists(accessToken)
    }

    override suspend fun deletePlaylist(accessToken: String, playlistId: String): Result<Unit> {
        return supabase.deletePlaylist(accessToken, playlistId)
    }

    override suspend fun updatePlaylistTrackCount(accessToken: String, playlistId: String, count: Int): Result<Unit> {
        return supabase.updatePlaylistTrackCount(accessToken, playlistId, count)
    }

    override suspend fun updatePlaylistVisibility(accessToken: String, playlistId: String, isPublic: Boolean): Result<Unit> {
        return supabase.updatePlaylistVisibility(accessToken, playlistId, isPublic)
    }

    override suspend fun updatePlaylistName(accessToken: String, playlistId: String, name: String, description: String?): Result<Unit> {
        return supabase.updatePlaylistName(accessToken, playlistId, name, description)
    }

    override suspend fun uploadPlaylistCover(accessToken: String, playlistId: String, fileBytes: ByteArray): Result<String> {
        return supabase.uploadPlaylistCover(accessToken, playlistId, fileBytes)
    }

    override suspend fun updatePlaylistCover(accessToken: String, playlistId: String, coverUrl: String): Result<Unit> {
        return supabase.updatePlaylistCover(accessToken, playlistId, coverUrl)
    }

    override suspend fun addTrackToPlaylist(accessToken: String, track: SupabaseService.PlaylistTrackInsert): Result<Unit> {
        return supabase.addTrackToPlaylist(accessToken, track)
    }

    override suspend fun getPlaylistTracks(accessToken: String, playlistId: String): List<SupabaseService.PlaylistTrackResponse> {
        return supabase.getPlaylistTracks(accessToken, playlistId)
    }

    override suspend fun removeTrackFromPlaylist(accessToken: String, playlistId: String, trackId: String): Result<Unit> {
        return supabase.removeTrackFromPlaylist(accessToken, playlistId, trackId)
    }

    override suspend fun getPublicPlaylists(accessToken: String, limit: Int): List<SupabaseService.PlaylistResponse> {
        return supabase.getPublicPlaylists(accessToken, limit)
    }

    override suspend fun searchPublicPlaylists(query: String, limit: Int): List<SupabaseService.PlaylistResponse> {
        return supabase.searchPublicPlaylists(query, limit)
    }

    override suspend fun getRecommendedPlaylists(accessToken: String, favoriteArtistNames: List<String>, limit: Int): List<SupabaseService.PlaylistResponse> {
        return supabase.getRecommendedPlaylists(accessToken, favoriteArtistNames, limit)
    }

    override suspend fun likePlaylist(accessToken: String, playlistId: String): Result<Unit> {
        return supabase.likePlaylist(accessToken, playlistId)
    }

    override suspend fun unlikePlaylist(accessToken: String, playlistId: String): Result<Unit> {
        return supabase.unlikePlaylist(accessToken, playlistId)
    }

    override suspend fun getMyLikedPlaylistIds(accessToken: String): Set<String> {
        return supabase.getMyLikedPlaylistIds(accessToken)
    }

    override suspend fun isPlaylistLiked(accessToken: String, playlistId: String): Boolean {
        return supabase.isPlaylistLiked(accessToken, playlistId)
    }

    override suspend fun createSynthesisSession(accessToken: String, creatorUsername: String?): Result<SupabaseService.SynthesisSession> {
        return supabase.createSynthesisSession(accessToken, creatorUsername)
    }

    override suspend fun getSynthesisSession(accessToken: String, inviteCode: String): Result<SupabaseService.SynthesisSession> {
        return supabase.getSynthesisSession(accessToken, inviteCode)
    }

    override suspend fun joinSynthesisSession(accessToken: String, sessionId: String, username: String?, avatarUrl: String?): Result<Unit> {
        return supabase.joinSynthesisSession(accessToken, sessionId, username, avatarUrl)
    }

    override suspend fun getSynthesisParticipants(accessToken: String, sessionId: String): List<SupabaseService.SynthesisParticipant> {
        return supabase.getSynthesisParticipants(accessToken, sessionId)
    }
}
