package uz.vazifa.app.domain.model

data class InboxNotification(
    val id: String,
    val taskId: String?,
    val title: String,
    val body: String,
    val type: String?,
    val receivedAt: Long,
)
