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
    suspend fun deleteAvatarByUrl(accessToken: String, avatarUrl: String): Result<Unit>
    suspend fun getProfile(accessToken: String): Result<SupabaseService.ProfileResponse?>
    suspend fun getProfileById(userId: String): SupabaseService.ProfileResponse?
    suspend fun getProfilesByIds(userIds: List<String>): Map<String, SupabaseService.ProfileResponse>
    
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

    // Track Ratings
    suspend fun upsertTrackRating(accessToken: String, rating: SupabaseService.TrackRatingInsert): Result<SupabaseService.TrackRatingResponse>
    suspend fun getMyTrackRating(accessToken: String, audiusTrackId: String): SupabaseService.TrackRatingResponse?
    suspend fun getMyRatings(accessToken: String, limit: Int = 50): List<SupabaseService.TrackRatingResponse>
    suspend fun getTrackAverageRating(accessToken: String, audiusTrackId: String): SupabaseService.TrackAverageRating?

    // Track Comments
    suspend fun addTrackComment(accessToken: String, comment: SupabaseService.TrackCommentInsert): Result<SupabaseService.TrackCommentResponse>
    suspend fun getTrackComments(accessToken: String, audiusTrackId: String): List<SupabaseService.TrackCommentResponse>
    suspend fun getMyComments(accessToken: String, limit: Int = 50): List<SupabaseService.TrackCommentResponse>
    suspend fun deleteTrackComment(accessToken: String, commentId: String): Result<Unit>

    // Review votes
    suspend fun getTrackReviews(accessToken: String, audiusTrackId: String): List<SupabaseService.TrackRatingResponse>
    suspend fun getBestReviewsForFavorites(accessToken: String, favoriteTrackIds: List<String>, favoriteArtistNames: List<String>, limit: Int = 30): List<SupabaseService.TrackRatingResponse>
    suspend fun voteReview(accessToken: String, ratingId: String, vote: Int): Result<Unit>
    suspend fun deleteVoteReview(accessToken: String, ratingId: String): Result<Unit>
    suspend fun getMyVoteForReview(accessToken: String, ratingId: String): Int?

    // Playlists
    suspend fun createPlaylist(accessToken: String, name: String, description: String? = null, isPublic: Boolean = false): Result<SupabaseService.PlaylistResponse>
    suspend fun getMyPlaylists(accessToken: String): List<SupabaseService.PlaylistResponse>
    suspend fun deletePlaylist(accessToken: String, playlistId: String): Result<Unit>
    suspend fun updatePlaylistTrackCount(accessToken: String, playlistId: String, count: Int): Result<Unit>
    suspend fun updatePlaylistVisibility(accessToken: String, playlistId: String, isPublic: Boolean): Result<Unit>
    suspend fun updatePlaylistName(accessToken: String, playlistId: String, name: String, description: String?): Result<Unit>
    suspend fun uploadPlaylistCover(accessToken: String, playlistId: String, fileBytes: ByteArray): Result<String>
    suspend fun updatePlaylistCover(accessToken: String, playlistId: String, coverUrl: String): Result<Unit>
    suspend fun addTrackToPlaylist(accessToken: String, track: SupabaseService.PlaylistTrackInsert): Result<Unit>
    suspend fun getPlaylistTracks(accessToken: String, playlistId: String): List<SupabaseService.PlaylistTrackResponse>
    suspend fun removeTrackFromPlaylist(accessToken: String, playlistId: String, trackId: String): Result<Unit>
    suspend fun getPublicPlaylists(accessToken: String, limit: Int = 20): List<SupabaseService.PlaylistResponse>
    suspend fun getRecommendedPlaylists(accessToken: String, favoriteArtistNames: List<String>, limit: Int = 20): List<SupabaseService.PlaylistResponse>
    suspend fun likePlaylist(accessToken: String, playlistId: String): Result<Unit>
    suspend fun unlikePlaylist(accessToken: String, playlistId: String): Result<Unit>
    suspend fun getMyLikedPlaylistIds(accessToken: String): Set<String>
    suspend fun isPlaylistLiked(accessToken: String, playlistId: String): Boolean

    // Synthesis
    suspend fun createSynthesisSession(accessToken: String, creatorUsername: String?): Result<SupabaseService.SynthesisSession>
    suspend fun getSynthesisSession(accessToken: String, inviteCode: String): Result<SupabaseService.SynthesisSession>
    suspend fun joinSynthesisSession(accessToken: String, sessionId: String, username: String?, avatarUrl: String?): Result<Unit>
    suspend fun getSynthesisParticipants(accessToken: String, sessionId: String): List<SupabaseService.SynthesisParticipant>
}

