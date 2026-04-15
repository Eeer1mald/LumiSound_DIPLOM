package com.example.lumisound.feature.playlist

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Событие создания нового плейлиста — уведомляет PlaylistViewModel об обновлении */
sealed class PlaylistEvent {
    data object PlaylistCreated : PlaylistEvent()
}

@Singleton
class PlaylistEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<PlaylistEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<PlaylistEvent> = _events.asSharedFlow()

    fun emit(event: PlaylistEvent) {
        _events.tryEmit(event)
    }
}
