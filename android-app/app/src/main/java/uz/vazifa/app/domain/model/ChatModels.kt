package uz.vazifa.app.domain.model

enum class ChatMessageType(val key: String) {
    TEXT("text"),
    IMAGE("image"),
    VIDEO("video"),
    AUDIO("audio"),
    VOICE("voice"),
    FILE("file"),
    STICKER("sticker"),
    GIF("gif"),
    CONTACT("contact"),
    LOCATION("location");

    companion object {
        fun from(key: String?): ChatMessageType =
            entries.firstOrNull { it.key == key } ?: TEXT
    }
}

enum class ChatMessageStatus(val key: String) {
    SENDING("sending"),
    SENT("sent"),
    DELIVERED("delivered"),
    READ("read"),
    FAILED("failed");

    companion object {
        fun from(key: String?): ChatMessageStatus =
            entries.firstOrNull { it.key == key } ?: SENT
    }
}

data class ChatMessageMeta(
    val fileUrl: String? = null,
    val fileSize: Long? = null,
    val durationSec: Int? = null,
    val waveform: List<Int>? = null,
    val width: Int? = null,
    val height: Int? = null,
    val thumbUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val contactName: String? = null,
    val contactPhone: String? = null,
)

data class ChatMessage(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val type: ChatMessageType = ChatMessageType.TEXT,
    val body: String? = null,
    val filePath: String? = null,
    val fileName: String? = null,
    val mimeType: String? = null,
    val meta: ChatMessageMeta? = null,
    val replyToId: String? = null,
    val replyTo: ChatMessage? = null,
    val forwardedFrom: String? = null,
    val reactions: Map<String, String> = emptyMap(),
    val status: ChatMessageStatus = ChatMessageStatus.SENT,
    val isRead: Boolean = false,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
    val isPinned: Boolean = false,
    val clientId: String? = null,
    val createdAt: String,
)

data class ChatPeer(
    val id: String,
    val fullName: String,
    val position: String? = null,
    val department: String? = null,
    val isOnline: Boolean = false,
    val lastSeenAt: String? = null,
)

data class Conversation(
    val peer: ChatPeer,
    val lastMessage: ChatMessage?,
    val unreadCount: Int,
)
