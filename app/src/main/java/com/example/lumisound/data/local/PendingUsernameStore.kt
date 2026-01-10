package com.example.lumisound.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PendingUsernameStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs by lazy {
        context.getSharedPreferences("pending_profile", Context.MODE_PRIVATE)
    }

    fun save(email: String, username: String) {
        prefs.edit().putString(key(email), username).apply()
    }

    fun get(email: String): String? = prefs.getString(key(email), null)

    fun clear(email: String) {
        prefs.edit().remove(key(email)).apply()
    }

    private fun key(email: String) = "username_$email"
}


