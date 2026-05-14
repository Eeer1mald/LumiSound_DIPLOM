package com.example.lumisound.data.cache

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.lumisound.data.model.Track
import com.example.lumisound.data.remote.SupabaseService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Персистентный дисковый кэш на основе SharedPreferences + JSON.
 * Каждая запись хранится вместе с timestamp для TTL-валидации.
 *
 * TTL политика:
 *  - Профиль, рейтинги, комментарии: 30 минут
 *  - Плейлисты, избранное: 15 минут
 *  - Фид (discover/following): 60 минут
 *  - Лучшие рецензии: 60 минут
 */
@Singleton
class DiskCache @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_disk_cache", Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        isLenient = true
    }

    companion object {
        private const val TAG = "DiskCache"

        // TTL в миллисекундах
        private const val TTL_PROFILE       = 30 * 60 * 1000L   // 30 мин
        private const val TTL_RATINGS       = 30 * 60 * 1000L   // 30 мин
        private const val TTL_PLAYLISTS     = 15 * 60 * 1000L   // 15 мин
        private const val TTL_FEED          = 60 * 60 * 1000L   // 60 мин
        private const val TTL_BEST_REVIEWS  = 60 * 60 * 1000L   // 60 мин
        private const val TTL_FAV_TRACKS    = 15 * 60 * 1000L   // 15 мин
        private const val TTL_FAV_ARTISTS   = 15 * 60 * 1000L   // 15 мин

        // Ключи
        private const val KEY_PROFILE           = "profile"
        private const val KEY_MY_PLAYLISTS      = "my_playlists"
        private const val KEY_TOP_PLAYLISTS     = "top_playlists"
        private const val KEY_REC_PLAYLISTS     = "rec_playlists"
        private const val KEY_LIKED_IDS         = "liked_ids"
        private const val KEY_RECENT_TRACKS     = "recent_tracks"
        private const val KEY_TOP_TRACKS        = "top_tracks"
        private const val KEY_FAV_TRACKS        = "fav_tracks"
        private const val KEY_FAV_ARTISTS       = "fav_artists"
        private const val KEY_MY_RATINGS        = "my_ratings"
        private const val KEY_MY_COMMENTS       = "my_comments"
        private const val KEY_BEST_REVIEWS      = "best_reviews"
        private const val KEY_DISCOVER_FEED     = "discover_feed"
        private const val KEY_FOLLOWING_FEED    = "following_feed"
        private const val KEY_USER_ID           = "cache_user_id"
    }

    // ── Инвалидация при смене пользователя ───────────────────────────────────

    /** Сохраняет userId для которого кэш актуален. При смене — очищает всё. */
    fun validateUser(userId: String) {
        val cachedUserId = prefs.getString(KEY_USER_ID, null)
        if (cachedUserId != null && cachedUserId != userId) {
            Log.i(TAG, "User changed, clearing disk cache")
            clearAll()
        }
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    // ── Внутренние helpers ────────────────────────────────────────────────────

    private fun isValid(key: String, ttlMs: Long): Boolean {
        val ts = prefs.getLong("${key}_ts", 0L)
        return ts > 0L && (System.currentTimeMillis() - ts) < ttlMs
    }

    private fun putString(key: String, value: String) {
        prefs.edit()
            .putString(key, value)
            .putLong("${key}_ts", System.currentTimeMillis())
            .apply()
    }

    private fun getString(key: String): String? = prefs.getString(key, null)

    // ── Профиль ───────────────────────────────────────────────────────────────

    fun saveProfile(profile: SupabaseService.ProfileResponse) {
        try { putString(KEY_PROFILE, json.encodeToString(profile)) }
        catch (e: Exception) { Log.w(TAG, "saveProfile failed: ${e.message}") }
    }

    fun loadProfile(): SupabaseService.ProfileResponse? {
        if (!isValid(KEY_PROFILE, TTL_PROFILE)) return null
        return try { getString(KEY_PROFILE)?.let { json.decodeFromString(it) } }
        catch (e: Exception) { null }
    }

    // ── Плейлисты ─────────────────────────────────────────────────────────────

    fun saveMyPlaylists(list: List<SupabaseService.PlaylistResponse>) {
        try { putString(KEY_MY_PLAYLISTS, json.encodeToString(list)) }
        catch (e: Exception) { Log.w(TAG, "saveMyPlaylists failed: ${e.message}") }
    }

    fun loadMyPlaylists(): List<SupabaseService.PlaylistResponse>? {
        if (!isValid(KEY_MY_PLAYLISTS, TTL_PLAYLISTS)) return null
        return try { getString(KEY_MY_PLAYLISTS)?.let { json.decodeFromString(it) } }
        catch (e: Exception) { null }
    }

    fun saveTopPlaylists(list: List<SupabaseService.PlaylistResponse>) {
        try { putString(KEY_TOP_PLAYLISTS, json.encodeToString(list)) }
        catch (e: Exception) { Log.w(TAG, "saveTopPlaylists failed: ${e.message}") }
    }

    fun loadTopPlaylists(): List<SupabaseService.PlaylistResponse>? {
        if (!isValid(KEY_TOP_PLAYLISTS, TTL_PLAYLISTS)) return null
        return try { getString(KEY_TOP_PLAYLISTS)?.let { json.decodeFromString(it) } }
        catch (e: Exception) { null }
    }

    fun saveRecommendedPlaylists(list: List<SupabaseService.PlaylistResponse>) {
        try { putString(KEY_REC_PLAYLISTS, json.encodeToString(list)) }
        catch (e: Exception) { Log.w(TAG, "saveRecommendedPlaylists failed: ${e.message}") }
    }

    fun loadRecommendedPlaylists(): List<SupabaseService.PlaylistResponse>? {
        if (!isValid(KEY_REC_PLAYLISTS, TTL_PLAYLISTS)) return null
        return try { getString(KEY_REC_PLAYLISTS)?.let { json.decodeFromString(it) } }
        catch (e: Exception) { null }
    }

    fun saveLikedPlaylistIds(ids: Set<String>) {
        try { putString(KEY_LIKED_IDS, json.encodeToString(ids.toList())) }
        catch (e: Exception) { Log.w(TAG, "saveLikedIds failed: ${e.message}") }
    }

    fun loadLikedPlaylistIds(): Set<String>? {
        if (!isValid(KEY_LIKED_IDS, TTL_PLAYLISTS)) return null
        return try { getString(KEY_LIKED_IDS)?.let { json.decodeFromString<List<String>>(it).toSet() } }
        catch (e: Exception) { null }
    }

    // ── Треки ─────────────────────────────────────────────────────────────────

    fun saveRecentTracks(list: List<SupabaseService.FavoriteTrackResponse>) {
        try { putString(KEY_RECENT_TRACKS, json.encodeToString(list)) }
        catch (e: Exception) { Log.w(TAG, "saveRecentTracks failed: ${e.message}") }
    }

    fun loadRecentTracks(): List<SupabaseService.FavoriteTrackResponse>? {
        if (!isValid(KEY_RECENT_TRACKS, TTL_FAV_TRACKS)) return null
        return try { getString(KEY_RECENT_TRACKS)?.let { json.decodeFromString(it) } }
        catch (e: Exception) { null }
    }

    fun saveTopTracks(list: List<SupabaseService.FavoriteTrackResponse>) {
        try { putString(KEY_TOP_TRACKS, json.encodeToString(list)) }
        catch (e: Exception) { Log.w(TAG, "saveTopTracks failed: ${e.message}") }
    }

    fun loadTopTracks(): List<SupabaseService.FavoriteTrackResponse>? {
        if (!isValid(KEY_TOP_TRACKS, TTL_FAV_TRACKS)) return null
        return try { getString(KEY_TOP_TRACKS)?.let { json.decodeFromString(it) } }
        catch (e: Exception) { null }
    }

    fun saveFavoriteTracks(list: List<SupabaseService.FavoriteTrackResponse>) {
        try { putString(KEY_FAV_TRACKS, json.encodeToString(list)) }
        catch (e: Exception) { Log.w(TAG, "saveFavoriteTracks failed: ${e.message}") }
    }

    fun loadFavoriteTracks(): List<SupabaseService.FavoriteTrackResponse>? {
        if (!isValid(KEY_FAV_TRACKS, TTL_FAV_TRACKS)) return null
        return try { getString(KEY_FAV_TRACKS)?.let { json.decodeFromString(it) } }
        catch (e: Exception) { null }
    }

    // ── Артисты ───────────────────────────────────────────────────────────────

    fun saveFavoriteArtists(list: List<SupabaseService.FavoriteArtistResponse>) {
        try { putString(KEY_FAV_ARTISTS, json.encodeToString(list)) }
        catch (e: Exception) { Log.w(TAG, "saveFavoriteArtists failed: ${e.message}") }
    }

    fun loadFavoriteArtists(): List<SupabaseService.FavoriteArtistResponse>? {
        if (!isValid(KEY_FAV_ARTISTS, TTL_FAV_ARTISTS)) return null
        return try { getString(KEY_FAV_ARTISTS)?.let { json.decodeFromString(it) } }
        catch (e: Exception) { null }
    }

    // ── Рейтинги и комментарии ────────────────────────────────────────────────

    fun saveMyRatings(list: List<SupabaseService.TrackRatingResponse>) {
        try { putString(KEY_MY_RATINGS, json.encodeToString(list)) }
        catch (e: Exception) { Log.w(TAG, "saveMyRatings failed: ${e.message}") }
    }

    fun loadMyRatings(): List<SupabaseService.TrackRatingResponse>? {
        if (!isValid(KEY_MY_RATINGS, TTL_RATINGS)) return null
        return try { getString(KEY_MY_RATINGS)?.let { json.decodeFromString(it) } }
        catch (e: Exception) { null }
    }

    fun saveMyComments(list: List<SupabaseService.TrackCommentResponse>) {
        try { putString(KEY_MY_COMMENTS, json.encodeToString(list)) }
        catch (e: Exception) { Log.w(TAG, "saveMyComments failed: ${e.message}") }
    }

    fun loadMyComments(): List<SupabaseService.TrackCommentResponse>? {
        if (!isValid(KEY_MY_COMMENTS, TTL_RATINGS)) return null
        return try { getString(KEY_MY_COMMENTS)?.let { json.decodeFromString(it) } }
        catch (e: Exception) { null }
    }

    fun saveBestReviews(list: List<SupabaseService.TrackRatingResponse>) {
        try { putString(KEY_BEST_REVIEWS, json.encodeToString(list)) }
        catch (e: Exception) { Log.w(TAG, "saveBestReviews failed: ${e.message}") }
    }

    fun loadBestReviews(): List<SupabaseService.TrackRatingResponse>? {
        if (!isValid(KEY_BEST_REVIEWS, TTL_BEST_REVIEWS)) return null
        return try { getString(KEY_BEST_REVIEWS)?.let { json.decodeFromString(it) } }
        catch (e: Exception) { null }
    }

    // ── Фид ───────────────────────────────────────────────────────────────────

    fun saveDiscoverFeed(list: List<Track>) {
        try { putString(KEY_DISCOVER_FEED, json.encodeToString(list)) }
        catch (e: Exception) { Log.w(TAG, "saveDiscoverFeed failed: ${e.message}") }
    }

    fun loadDiscoverFeed(): List<Track>? {
        if (!isValid(KEY_DISCOVER_FEED, TTL_FEED)) return null
        return try { getString(KEY_DISCOVER_FEED)?.let { json.decodeFromString(it) } }
        catch (e: Exception) { null }
    }

    fun saveFollowingFeed(list: List<Track>) {
        try { putString(KEY_FOLLOWING_FEED, json.encodeToString(list)) }
        catch (e: Exception) { Log.w(TAG, "saveFollowingFeed failed: ${e.message}") }
    }

    fun loadFollowingFeed(): List<Track>? {
        if (!isValid(KEY_FOLLOWING_FEED, TTL_FEED)) return null
        return try { getString(KEY_FOLLOWING_FEED)?.let { json.decodeFromString(it) } }
        catch (e: Exception) { null }
    }

    // ── Последний играющий трек ───────────────────────────────────────────────

    fun saveLastTrack(track: com.example.lumisound.data.model.Track) {
        try { putString("last_track", json.encodeToString(track)) }
        catch (e: Exception) { Log.w(TAG, "saveLastTrack failed: ${e.message}") }
    }

    fun loadLastTrack(): com.example.lumisound.data.model.Track? {
        // Последний трек не имеет TTL — показываем всегда
        return try { getString("last_track")?.let { json.decodeFromString(it) } }
        catch (e: Exception) { null }
    }

    // ── Последний играющий трек (привязан к userId) ───────────────────────────

    /** Сохраняет последний трек для конкретного пользователя. */
    fun saveLastTrack(track: com.example.lumisound.data.model.Track, userId: String) {
        try { putString("last_track_$userId", json.encodeToString(track)) }
        catch (e: Exception) { Log.w(TAG, "saveLastTrack failed: ${e.message}") }
    }

    /**
     * Загружает последний трек для пользователя.
     * Работает после переустановки — данные хранятся бессрочно.
     * При смене аккаунта автоматически возвращает трек нужного пользователя.
     */
    fun loadLastTrack(userId: String): com.example.lumisound.data.model.Track? {
        return try { getString("last_track_$userId")?.let { json.decodeFromString(it) } }
        catch (e: Exception) { null }
    }
}
