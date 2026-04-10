package com.example.lumisound

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.example.lumisound.feature.auth.navigation.AuthNavGraph
import com.example.lumisound.ui.theme.LumiSoundTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Стартует сразу при создании Activity — параллельно грузит все данные
    private val preloadViewModel: AppPreloadViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Запрашиваем максимальный refresh rate для плавности на 120Hz экранах
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes = window.attributes.also { attrs ->
                attrs.preferredDisplayModeId = display
                    ?.supportedModes
                    ?.maxByOrNull { it.refreshRate }
                    ?.modeId ?: 0
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            window.attributes = window.attributes.also { attrs ->
                attrs.preferredRefreshRate = display?.refreshRate ?: 60f
            }
        }
        
        setContent {
            LumiSoundTheme {
                AuthNavGraph()
            }
        }
    }
}