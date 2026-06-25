package uz.vazifa.app.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import uz.vazifa.app.AppForegroundState
import uz.vazifa.app.data.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PresenceService @Inject constructor(
    private val auth: AuthRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var heartbeatJob: Job? = null

    fun start() {
        if (heartbeatJob?.isActive == true) return
        heartbeatJob = scope.launch {
            while (isActive) {
                if (AppForegroundState.isInForeground) {
                    runCatching { auth.sendPresenceHeartbeat() }
                }
                delay(HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    companion object {
        private const val HEARTBEAT_INTERVAL_MS = 30_000L
    }
}
