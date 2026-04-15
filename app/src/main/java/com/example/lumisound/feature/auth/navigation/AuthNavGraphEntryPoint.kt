package com.example.lumisound.feature.auth.navigation

import com.example.lumisound.data.local.SessionManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AuthNavGraphEntryPoint {
    fun sessionManager(): SessionManager
}
