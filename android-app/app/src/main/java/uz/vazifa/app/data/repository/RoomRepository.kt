package uz.vazifa.app.data.repository

import uz.vazifa.app.data.remote.AddMembersBody
import uz.vazifa.app.data.remote.ApiClient
import uz.vazifa.app.data.remote.CreateRoomBody
import uz.vazifa.app.data.remote.EditMessageBody
import uz.vazifa.app.data.remote.ReactBody
import uz.vazifa.app.data.remote.RoomDto
import uz.vazifa.app.data.remote.RoomMemberDto
import uz.vazifa.app.data.remote.RoomMessageDto
import uz.vazifa.app.data.remote.SendRoomMessageBody
import uz.vazifa.app.data.remote.UpdateRoomBody
import uz.vazifa.app.domain.model.ChatMessageMeta
import uz.vazifa.app.domain.model.ChatMessageStatus
import uz.vazifa.app.domain.model.ChatMessageType
import uz.vazifa.app.domain.model.ChatRoom
import uz.vazifa.app.domain.model.ChatRoomRole
import uz.vazifa.app.domain.model.ChatRoomType
import uz.vazifa.app.domain.model.RoomMember
import uz.vazifa.app.domain.model.RoomMessage
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRepository @Inject constructor(
    private val client: ApiClient,
    private val chat: ChatRepository,
) {
    private val api get() = client.api

    val events get() = chat.events
    val connected get() = chat.connected
    fun connect() = chat.connect()
    fun sendTyping(roomId: String, typing: Boolean, action: String = "typing") =
        chat.sendRoomTyping(roomId, typing, action)

    suspend fun list(): List<ChatRoom> = api.getRooms().map { it.toDomain() }

    suspend fun get(id: String): ChatRoom = api.getRoom(id).toDomain()

    suspend fun create(
        type: ChatRoomType,
        title: String,
        description: String?,
        memberIds: List<String>,
        avatarUrl: String? = null,
    ): ChatRoom = api.createRoom(
        CreateRoomBody(
            type = type.key,
            title = title,
            description = description,
            avatarUrl = avatarUrl,
            memberIds = memberIds,
        ),
    ).toDomain()

    suspend fun update(id: String, title: String?, description: String?): ChatRoom =
        api.updateRoom(id, UpdateRoomBody(title = title, description = description)).toDomain()

    suspend fun delete(id: String) { api.deleteRoom(id) }

    suspend fun members(id: String): List<RoomMember> = api.getRoomMembers(id).map { it.toDomain() }

    suspend fun addMembers(id: String, memberIds: List<String>) {
        api.addRoomMembers(id, AddMembersBody(memberIds))
    }

    suspend fun removeMember(id: String, userId: String) { api.removeRoomMember(id, userId) }

    suspend fun history(id: String, before: String? = null, limit: Int = 40): List<RoomMessage> =
        api.getRoomHistory(id, before, limit).map { it.toDomain() }

    suspend fun send(
        roomId: String,
        type: ChatMessageType,
        body: String? = null,
        upload: uz.vazifa.app.data.remote.ChatUploadDto? = null,
        meta: ChatMessageMeta? = null,
        replyToId: String? = null,
        forwardedFrom: String? = null,
        clientId: String,
    ): RoomMessage {
        val mergedMeta = (meta ?: ChatMessageMeta()).copy(
            fileUrl = upload?.fileUrl ?: upload?.filePath?.let { uz.vazifa.app.util.MediaUrl.fromFilePath(it) } ?: meta?.fileUrl,
            fileSize = upload?.fileSize ?: meta?.fileSize,
        )
        return api.sendRoomMessage(
            roomId,
            SendRoomMessageBody(
                type = type.key,
                body = body,
                filePath = upload?.filePath,
                fileName = upload?.fileName,
                mimeType = upload?.mimeType,
                meta = mergedMeta.toDto(),
                replyToId = replyToId,
                forwardedFrom = forwardedFrom,
                clientId = clientId,
            ),
        ).toDomain()
    }

    suspend fun uploadFile(file: File, mime: String) = chat.uploadFile(file, mime)

    suspend fun markRead(id: String) { runCatching { api.markRoomRead(id) } }

    suspend fun edit(msgId: String, body: String): RoomMessage =
        api.editRoomMessage(msgId, EditMessageBody(body)).toDomain()

    suspend fun delete(msgId: String, room: Boolean) { api.deleteRoomMessage(msgId) }

    suspend fun react(msgId: String, emoji: String?): RoomMessage =
        api.reactRoomMessage(msgId, ReactBody(emoji)).toDomain()

    suspend fun pin(msgId: String): RoomMessage = api.pinRoomMessage(msgId).toDomain()
}

fun RoomDto.toDomain(): ChatRoom = ChatRoom(
    id = id,
    type = ChatRoomType.from(type),
    title = title,
    description = description,
    avatarUrl = avatarUrl,
    isVerified = isVerified,
    ownerId = ownerId,
    myRole = ChatRoomRole.from(myRole),
    memberCount = memberCount,
    muted = muted,
    canPost = canPost,
    lastMessage = lastMessage?.toDomain(),
    unreadCount = unreadCount,
)

fun RoomMessageDto.toDomain(): RoomMessage {
    val resolvedMeta = meta.toDomainMessageMeta(filePath)
    return RoomMessage(
        id = id,
        roomId = roomId,
        senderId = senderId,
        senderName = sender?.fullName,
        senderAvatarUrl = sender?.avatarUrl,
        type = ChatMessageType.from(type),
        body = body,
        filePath = filePath,
        fileName = fileName,
        mimeType = mimeType,
        meta = resolvedMeta,
        replyToId = replyToId,
        replyTo = replyTo?.toDomain(),
        forwardedFrom = forwardedFrom,
        reactions = reactions ?: emptyMap(),
        status = ChatMessageStatus.SENT,
        isEdited = isEdited,
        isDeleted = isDeleted,
        isPinned = isPinned,
        clientId = clientId,
        createdAt = createdAt,
    )
}

private fun RoomMemberDto.toDomain(): RoomMember = RoomMember(
    id = id,
    fullName = fullName,
    avatarUrl = avatarUrl,
    position = position,
    role = ChatRoomRole.from(role),
)
