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
import uz.vazifa.app.data.repository.ContactAliasRepository
import uz.vazifa.app.data.repository.NotificationInboxRepository
import uz.vazifa.app.notifications.PushDedup
import uz.vazifa.app.notifications.VazifaNotificationHelper
import javax.inject.Inject

@AndroidEntryPoint
class FcmService : FirebaseMessagingService() {
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var notificationInbox: NotificationInboxRepository
    @Inject lateinit var chatUnread: ChatUnreadRepository
    @Inject lateinit var contactAliases: ContactAliasRepository

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { authRepository.syncFcmTokenIfPossible(token) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val outboxId = data["outboxId"].orEmpty()
        if (outboxId.isNotBlank() && PushDedup.isDuplicate(applicationContext, outboxId)) {
            return
        }
        val serverTitle = data["title"] ?: message.notification?.title ?: "Vazifa"
        val body = data["body"] ?: message.notification?.body ?: ""
        val taskId = data["taskId"]
        val type = data["type"]
        val announcementId = data["announcementId"]
        val chatUserId = data["chatUserId"]
        val roomId = data["roomId"]
        val isChat = type == "chat" || type == "room"
        val isAnnouncement = type == "announcement" || type == "announcement_reminder"

        CoroutineScope(Dispatchers.IO).launch {
            val uid = authRepository.cachedUserId()
            val title = if (!chatUserId.isNullOrBlank()) {
                contactAliases.resolveDisplayName(chatUserId, serverTitle, uid)
            } else serverTitle

            when {
                isChat -> {
                    if (!AppForegroundState.isInForeground) {
                        chatUnread.increment()
                    }
                    notificationInbox.add(
                        title = title,
                        body = body,
                        type = type,
                        chatUserId = chatUserId,
                        roomId = roomId,
                    )
                }
                else -> {
                    notificationInbox.add(
                        taskId = taskId,
                        title = title,
                        body = body,
                        type = type,
                        announcementId = announcementId,
                    )
                }
            }

            if (isChat || isAnnouncement || !AppForegroundState.isInForeground) {
                VazifaNotificationHelper.show(
                    context = applicationContext,
                    title = title,
                    body = body,
                    taskId = taskId,
                    type = type,
                    chatUserId = chatUserId,
                    roomId = roomId,
                    announcementId = announcementId,
                )
            }
            if (outboxId.isNotBlank()) {
                PushDedup.remember(applicationContext, outboxId)
            }
        }

        if (isChat && AppForegroundState.isInForeground && !chatUserId.isNullOrBlank()) {
            if (chatUserId == uz.vazifa.app.chat.ActiveChatTracker.peerId) {
                uz.vazifa.app.chat.ActiveChatTracker.requestRefresh(chatUserId)
            }
        }
    }
}
