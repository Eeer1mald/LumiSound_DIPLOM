package com.example.lumisound

import android.os.Build
import android.os.Bundle
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

    private val preloadViewModel: AppPreloadViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes = window.attributes.also { attrs ->
                attrs.preferredDisplayModeId = display?.supportedModes?.maxByOrNull { it.refreshRate }?.modeId ?: 0
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            window.attributes = window.attributes.also { attrs ->
                attrs.preferredRefreshRate = display?.refreshRate ?: 60f
            }
        }

        // Проверяем deep link синтеза — поддерживаем оба формата
        val synthesisCode = intent?.data?.let { uri ->
            when {
                // lumisound://synthesis/{code}
                uri.scheme == "lumisound" && uri.host == "synthesis" ->
                    uri.pathSegments.firstOrNull()
                // https://...supabase.co/synthesis?code={code}
                uri.scheme == "https" && uri.path?.startsWith("/synthesis") == true ->
                    uri.getQueryParameter("code")
                else -> null
            }
        }

        setContent {
            LumiSoundTheme {
                AuthNavGraph(synthesisInviteCode = synthesisCode)
            }
        }
    }
}