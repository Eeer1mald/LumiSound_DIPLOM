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
}
