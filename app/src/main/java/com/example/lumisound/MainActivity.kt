package com.example.lumisound

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import com.example.lumisound.data.local.SessionManager
import com.example.lumisound.feature.auth.navigation.AuthNavGraph
import com.example.lumisound.ui.theme.LumiSoundTheme
import com.example.lumisound.ui.theme.ThemeManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val preloadViewModel: AppPreloadViewModel by viewModels()

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var themeManager: ThemeManager

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

        // Инициализируем тему из сохранённых настроек
        themeManager.setTheme(sessionManager.getThemeMode())

        val synthesisCode = intent?.data?.let { uri ->
            when {
                uri.scheme == "lumisound" && uri.host == "synthesis" ->
                    uri.pathSegments.firstOrNull()
                uri.scheme == "https" && uri.path?.startsWith("/synthesis") == true ->
                    uri.getQueryParameter("code")
                else -> null
            }
        }

        setContent {
            // Подписываемся на ThemeManager — тема меняется реактивно без перезапуска Activity
            val themeMode by themeManager.themeMode.collectAsState()

            LumiSoundTheme(themeMode = themeMode) {
                AuthNavGraph(synthesisInviteCode = synthesisCode)
            }
        }
    }
}
