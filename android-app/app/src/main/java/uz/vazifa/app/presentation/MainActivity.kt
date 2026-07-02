package uz.vazifa.app.presentation

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
    var pendingChatUserId by mutableStateOf<String?>(null)
        private set
    var pendingChatPeerName by mutableStateOf<String?>(null)
        private set
    var pendingRoomId by mutableStateOf<String?>(null)
        private set
    var pendingAnnouncementId by mutableStateOf<String?>(null)
        private set

    private val notifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        CoroutineScope(Dispatchers.IO).launch {
            if (granted) {
                runCatching { authRepository.registerPushToken() }
            }
        }
    }

    private var keepSystemSplash = true

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { keepSystemSplash }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingTaskId = intent.getStringExtra(VazifaNotificationHelper.EXTRA_TASK_ID)
        pendingChatUserId = intent.getStringExtra(VazifaNotificationHelper.EXTRA_CHAT_USER_ID)
        pendingChatPeerName = intent.getStringExtra(VazifaNotificationHelper.EXTRA_CHAT_PEER_NAME)
        pendingRoomId = intent.getStringExtra(VazifaNotificationHelper.EXTRA_ROOM_ID)
        pendingAnnouncementId = intent.getStringExtra(VazifaNotificationHelper.EXTRA_ANNOUNCEMENT_ID)

        CoroutineScope(Dispatchers.IO).launch {
            runCatching { authRepository.ensureSessionForApi() }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { authRepository.registerPushToken() }
            }
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
                        pendingChatUserId = pendingChatUserId,
                        pendingChatPeerName = pendingChatPeerName,
                        onPendingChatConsumed = { pendingChatUserId = null; pendingChatPeerName = null },
                        pendingRoomId = pendingRoomId,
                        onPendingRoomConsumed = { pendingRoomId = null },
                        pendingAnnouncementId = pendingAnnouncementId,
                        onPendingAnnouncementConsumed = { pendingAnnouncementId = null },
                        onSplashActiveChange = { active -> keepSystemSplash = active },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingTaskId = intent.getStringExtra(VazifaNotificationHelper.EXTRA_TASK_ID)
        pendingChatUserId = intent.getStringExtra(VazifaNotificationHelper.EXTRA_CHAT_USER_ID)
        pendingChatPeerName = intent.getStringExtra(VazifaNotificationHelper.EXTRA_CHAT_PEER_NAME)
        pendingRoomId = intent.getStringExtra(VazifaNotificationHelper.EXTRA_ROOM_ID)
        pendingAnnouncementId = intent.getStringExtra(VazifaNotificationHelper.EXTRA_ANNOUNCEMENT_ID)
    }
}
