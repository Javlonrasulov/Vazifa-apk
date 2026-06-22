package uz.vazifa.app.presentation

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.AndroidEntryPoint
import uz.vazifa.app.data.repository.AppSettingsRepository
import uz.vazifa.app.data.repository.AuthRepository
import uz.vazifa.app.data.repository.ThemeMode
import uz.vazifa.app.localization.AppLanguage
import uz.vazifa.app.localization.LocalAppLanguage
import uz.vazifa.app.notifications.VazifaNotificationHelper
import uz.vazifa.app.presentation.navigation.VazifaNavHost
import uz.vazifa.app.presentation.theme.VazifaTheme
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var appSettingsRepository: AppSettingsRepository
    @Inject lateinit var authRepository: AuthRepository

    var pendingTaskId by mutableStateOf<String?>(null)
        private set

    private val notifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { authRepository.syncFcmTokenIfPossible() }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingTaskId = intent.getStringExtra(VazifaNotificationHelper.EXTRA_TASK_ID)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val themeMode by appSettingsRepository.themeMode.collectAsState(initial = ThemeMode.LIGHT)
            val language by appSettingsRepository.language.collectAsState(initial = AppLanguage.DEFAULT)
            val isSystemDark = isSystemInDarkTheme()
            val isDark = appSettingsRepository.resolvedDark(themeMode, isSystemDark)

            VazifaTheme(darkTheme = isDark) {
                CompositionLocalProvider(LocalAppLanguage provides language) {
                    VazifaNavHost(
                        pendingTaskId = pendingTaskId,
                        onPendingTaskConsumed = { pendingTaskId = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingTaskId = intent.getStringExtra(VazifaNotificationHelper.EXTRA_TASK_ID)
    }
}
