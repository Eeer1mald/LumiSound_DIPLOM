package com.example.lumisound.ui.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeManager @Inject constructor() {
    private val _themeMode = MutableStateFlow("dark")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun setTheme(mode: String) {
        _themeMode.value = mode
    }
}
