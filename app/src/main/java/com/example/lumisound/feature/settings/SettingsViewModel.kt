package com.example.lumisound.feature.settings

import androidx.lifecycle.ViewModel
import com.example.lumisound.data.local.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {

    fun logout() {
        sessionManager.clear()
    }
}
