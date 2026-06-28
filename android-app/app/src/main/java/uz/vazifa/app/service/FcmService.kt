package uz.vazifa.app.service

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uz.vazifa.app.AppForegroundState
import uz.vazifa.app.data.repository.AuthRepository
import uz.vazifa.app.data.repository.ChatUnreadRepository
import uz.vazifa.app.data.repository.NotificationInboxRepository
import uz.vazifa.app.notifications.VazifaNotificationHelper
import javax.inject.Inject

@AndroidEntryPoint
class FcmService : FirebaseMessagingService() {
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var notificationInbox: NotificationInboxRepository
    @Inject lateinit var chatUnread: ChatUnreadRepository

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { authRepository.syncFcmTokenIfPossible(token) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val title = data["title"] ?: message.notification?.title ?: "Vazifa"
        val body = data["body"] ?: message.notification?.body ?: ""
        val taskId = data["taskId"]
        val type = data["type"]
        val chatUserId = data["chatUserId"]
        val roomId = data["roomId"]

        CoroutineScope(Dispatchers.IO).launch {
            when (type) {
                "chat", "room" -> {
                    if (!AppForegroundState.isInForeground) {
                        chatUnread.increment()
                    }
                    if (!AppForegroundState.isInForeground) {
                        notificationInbox.add(
                            title = title,
                            body = body,
                            type = type,
                            chatUserId = chatUserId,
                            roomId = roomId,
                        )
                    }
                }
                else -> {
                    if (!AppForegroundState.isInForeground) {
                        notificationInbox.add(taskId, title, body, type)
                    }
                }
            }
        }

        if (!AppForegroundState.isInForeground) {
            VazifaNotificationHelper.show(
                context = applicationContext,
                title = title,
                body = body,
                taskId = taskId,
                type = type,
                chatUserId = chatUserId,
                roomId = roomId,
            )
        }
    }
}
