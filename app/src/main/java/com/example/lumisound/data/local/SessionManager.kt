package com.example.lumisound.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("session", Context.MODE_PRIVATE)
    
    fun saveAccessToken(token: String) {
        prefs.edit().putString("access_token", token).apply()
    }
    
    fun getAccessToken(): String? = prefs.getString("access_token", null)

    fun saveRefreshToken(token: String) {
        prefs.edit().putString("refresh_token", token).apply()
    }

    fun getRefreshToken(): String? = prefs.getString("refresh_token", null)

    fun saveTokenExpiry(expiresAt: Long) {
        prefs.edit().putLong("token_expiry", expiresAt).apply()
    }

    fun getTokenExpiry(): Long = prefs.getLong("token_expiry", 0L)

    /** Токен истекает менее чем через 5 минут или уже истёк */
    fun isTokenExpiredOrExpiringSoon(): Boolean {
        val expiry = getTokenExpiry()
        if (expiry == 0L) return false // нет данных — считаем валидным
        return System.currentTimeMillis() >= expiry - 5 * 60 * 1000L
    }
    
    fun saveEmail(email: String) {
        prefs.edit().putString("email", email).apply()
    }
    
    fun getEmail(): String? = prefs.getString("email", null)
    
    fun saveUserId(userId: String) {
        prefs.edit().putString("user_id", userId).apply()
    }
    
    fun getUserId(): String? = prefs.getString("user_id", null)
    
    fun clear() {
        prefs.edit().clear().apply()
    }

    // ── Настройки приложения ──────────────────────────────────────────────

    /** Тема: "dark" | "light" | "system" */
    fun saveThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
    }
    fun getThemeMode(): String = prefs.getString("theme_mode", "dark") ?: "dark"

    fun saveShowFloatingComments(enabled: Boolean) {
        prefs.edit().putBoolean("floating_comments", enabled).apply()
    }
    fun getShowFloatingComments(): Boolean = prefs.getBoolean("floating_comments", true)

    fun saveNormalizeVolume(enabled: Boolean) {
        prefs.edit().putBoolean("normalize_volume", enabled).apply()
    }
    fun getNormalizeVolume(): Boolean = prefs.getBoolean("normalize_volume", false)

    fun saveCrossfade(enabled: Boolean) {
        prefs.edit().putBoolean("crossfade", enabled).apply()
    }
    fun getCrossfade(): Boolean = prefs.getBoolean("crossfade", false)

    // Универсальные методы для произвольных настроек
    fun saveBoolean(key: String, value: Boolean) { prefs.edit().putBoolean(key, value).apply() }
    fun getBoolean(key: String, default: Boolean = false): Boolean = prefs.getBoolean(key, default)
    fun saveFloat(key: String, value: Float) { prefs.edit().putFloat(key, value).apply() }
    fun getFloat(key: String, default: Float = 0f): Float = prefs.getFloat(key, default)
}
