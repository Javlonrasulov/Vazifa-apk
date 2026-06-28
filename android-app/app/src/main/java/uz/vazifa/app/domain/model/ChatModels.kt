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
    val avatarUrl: String? = null,
    val position: String? = null,
    val department: String? = null,
    val isOnline: Boolean = false,
    val lastSeenAt: String? = null,
    /** Lokal o'zgartirilgan nom (faqat shu akkauntda ko'rinadi) */
    val alias: String? = null,
) {
    /** Ko'rsatish uchun nom: alias bo'lsa o'shani, aks holda asl ismni qaytaradi */
    val displayName: String get() = alias?.takeIf { it.isNotBlank() } ?: fullName
}

/** Chat ichidagi foydalanuvchi profili (kontakt ma'lumotlari) */
data class PeerProfile(
    val id: String,
    val fullName: String,
    val avatarUrl: String? = null,
    val login: String? = null,
    val phone: String? = null,
    val position: String? = null,
    val department: String? = null,
    val role: String? = null,
    val isOnline: Boolean = false,
    val lastSeenAt: String? = null,
    val alias: String? = null,
) {
    val displayName: String get() = alias?.takeIf { it.isNotBlank() } ?: fullName
}

fun ChatPeer.toPeerProfile(
    login: String? = null,
    phone: String? = null,
    role: String? = null,
) = PeerProfile(
    id = id,
    fullName = fullName,
    avatarUrl = avatarUrl,
    login = login,
    phone = phone,
    position = position,
    department = department,
    role = role,
    isOnline = isOnline,
    lastSeenAt = lastSeenAt,
    alias = alias,
)

data class Conversation(
    val peer: ChatPeer,
    val lastMessage: ChatMessage?,
    val unreadCount: Int,
)

enum class ChatRoomType(val key: String) {
    GROUP("group"),
    CHANNEL("channel");

    companion object {
        fun from(key: String?): ChatRoomType =
            entries.firstOrNull { it.key == key } ?: GROUP
    }
}

enum class ChatRoomRole(val key: String) {
    OWNER("owner"),
    ADMIN("admin"),
    MEMBER("member");

    companion object {
        fun from(key: String?): ChatRoomRole =
            entries.firstOrNull { it.key == key } ?: MEMBER
    }
}

data class RoomMessage(
    val id: String,
    val roomId: String,
    val senderId: String,
    val senderName: String? = null,
    val senderAvatarUrl: String? = null,
    val type: ChatMessageType = ChatMessageType.TEXT,
    val body: String? = null,
    val filePath: String? = null,
    val fileName: String? = null,
    val mimeType: String? = null,
    val meta: ChatMessageMeta? = null,
    val replyToId: String? = null,
    val replyTo: RoomMessage? = null,
    val forwardedFrom: String? = null,
    val reactions: Map<String, String> = emptyMap(),
    val status: ChatMessageStatus = ChatMessageStatus.SENT,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
    val isPinned: Boolean = false,
    val clientId: String? = null,
    val createdAt: String,
)

data class ChatRoom(
    val id: String,
    val type: ChatRoomType,
    val title: String,
    val description: String? = null,
    val avatarUrl: String? = null,
    val isVerified: Boolean = false,
    val ownerId: String,
    val myRole: ChatRoomRole = ChatRoomRole.MEMBER,
    val memberCount: Int = 0,
    val muted: Boolean = false,
    val canPost: Boolean = true,
    val lastMessage: RoomMessage? = null,
    val unreadCount: Int = 0,
)

data class RoomMember(
    val id: String,
    val fullName: String,
    val avatarUrl: String? = null,
    val position: String? = null,
    val role: ChatRoomRole = ChatRoomRole.MEMBER,
)
