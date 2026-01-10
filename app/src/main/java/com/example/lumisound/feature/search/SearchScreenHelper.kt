package com.example.lumisound.feature.search

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.lumisound.data.player.PlayerStateHolder
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PlayerStateHolderEntryPoint {
    fun playerStateHolder(): PlayerStateHolder
}

@Composable
fun getPlayerStateHolder(): PlayerStateHolder {
    val context = LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(
        context.applicationContext,
        PlayerStateHolderEntryPoint::class.java
    )
    return entryPoint.playerStateHolder()
}
