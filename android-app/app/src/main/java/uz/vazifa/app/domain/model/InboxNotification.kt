package uz.vazifa.app.domain.model

data class InboxNotification(
    val id: String,
    val taskId: String? = null,
    val chatUserId: String? = null,
    val roomId: String? = null,
    val title: String,
    val body: String,
    val type: String? = null,
    val receivedAt: Long,
)
