package uz.vazifa.app.data.repository

import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Request
import uz.vazifa.app.BuildConfig
import uz.vazifa.app.data.remote.ApiClient
import uz.vazifa.app.data.remote.ChatEvent
import uz.vazifa.app.data.remote.ChatMessageDto
import uz.vazifa.app.data.remote.ChatMetaDto
import uz.vazifa.app.data.remote.ChatPeerDto
import uz.vazifa.app.data.remote.ChatSocket
import uz.vazifa.app.data.remote.ChatUploadDto
import uz.vazifa.app.data.remote.EditMessageBody
import uz.vazifa.app.data.remote.MarkReadBody
import uz.vazifa.app.data.remote.ReactBody
import uz.vazifa.app.data.remote.SendMessageBody
import uz.vazifa.app.data.remote.UserDto
import uz.vazifa.app.domain.model.ChatMessage
import uz.vazifa.app.domain.model.ChatMessageMeta
import uz.vazifa.app.domain.model.ChatMessageStatus
import uz.vazifa.app.domain.model.ChatMessageType
import uz.vazifa.app.domain.model.ChatPeer
import uz.vazifa.app.domain.model.Conversation
import uz.vazifa.app.domain.model.PeerProfile
import uz.vazifa.app.domain.model.toPeerProfile
import java.io.File
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val client: ApiClient,
    private val socket: ChatSocket,
    private val aliases: ContactAliasRepository,
    private val messageCache: ChatMessageCache,
) {
    private val api get() = client.api

    suspend fun loadAliases(userId: String) = aliases.load(userId)
    fun aliasFor(peerId: String): String? = aliases.aliasFor(peerId)
    suspend fun setAlias(userId: String, peerId: String, alias: String?) =
        aliases.setAlias(userId, peerId, alias)

    val events: SharedFlow<ChatEvent> get() = socket.events
    val connected: StateFlow<Boolean> get() = socket.connected

    private val peerCache = mutableMapOf<String, ChatPeer>()

    fun rememberPeer(peer: ChatPeer) {
        if (peer.id.isBlank() || peer.fullName.isBlank()) return
        val cached = peerCache[peer.id]
        peerCache[peer.id] = if (cached == null) peer else peer.copy(
            position = peer.position ?: cached.position,
            department = peer.department ?: cached.department,
            isOnline = peer.isOnline || cached.isOnline,
            lastSeenAt = peer.lastSeenAt ?: cached.lastSeenAt,
        )
    }

    fun knownPeer(id: String): ChatPeer? = peerCache[id]

    private fun enrichPeer(peer: ChatPeer): ChatPeer {
        val known = peerCache[peer.id]
        val enriched = if (known == null) {
            peer
        } else {
            peer.copy(
                fullName = peer.fullName.ifBlank { known.fullName },
                position = peer.position ?: known.position,
                department = peer.department ?: known.department,
                isOnline = peer.isOnline || known.isOnline,
                lastSeenAt = peer.lastSeenAt ?: known.lastSeenAt,
            )
        }
        if (enriched.fullName.isNotBlank()) rememberPeer(enriched)
        return enriched.copy(alias = aliases.aliasFor(enriched.id))
    }

    fun connect() = socket.connect()
    fun disconnect() = socket.disconnect()
    fun sendTyping(receiverId: String, typing: Boolean, action: String = "typing") =
        socket.sendTyping(receiverId, typing, action)
    fun sendRoomTyping(roomId: String, typing: Boolean, action: String = "typing") =
        socket.sendRoomTyping(roomId, typing, action)
    fun ping() = socket.ping()

    suspend fun getConversations(): List<Conversation> =
        api.getConversations().map { dto ->
            Conversation(
                peer = enrichPeer(dto.peer.toDomain()),
                lastMessage = dto.lastMessage?.toDomain(),
                unreadCount = dto.unreadCount,
            )
        }

    suspend fun getHistory(peerId: String, before: String? = null, limit: Int = 40): List<ChatMessage> {
        val dtos = try {
            api.getChatHistory(peerId, before, limit)
        } catch (_: Exception) {
            fetchHistoryLenient(peerId, before, limit) ?: emptyList()
        }
        val messages = dtos.mapNotNull { safeToDomain(it) }
        if (before == null && dtos.isNotEmpty()) {
            messageCache.saveDm(peerId, dtos)
        }
        return messages
    }

    private suspend fun fetchHistoryLenient(
        peerId: String,
        before: String?,
        limit: Int,
    ): List<ChatMessageDto>? = withContext(Dispatchers.IO) {
        val base = BuildConfig.API_BASE_URL.removeSuffix("/")
        val urlBuilder = "$base/chat/$peerId".toHttpUrlOrNull()?.newBuilder() ?: return@withContext null
        urlBuilder.addQueryParameter("limit", limit.toString())
        before?.takeIf { it.isNotBlank() }?.let { urlBuilder.addQueryParameter("before", it) }
        val response = client.httpClient.newCall(
            Request.Builder().url(urlBuilder.build()).get().build(),
        ).execute()
        response.use { resp ->
            if (!resp.isSuccessful) return@withContext null
            val body = resp.body?.string()?.trim().orEmpty()
            if (body.isEmpty() || body == "[]") return@withContext emptyList()
            val gson = Gson()
            val arr = runCatching { JsonParser.parseString(body).asJsonArray }.getOrNull()
                ?: return@withContext null
            arr.mapNotNull { el ->
                runCatching { gson.fromJson(el, ChatMessageDto::class.java) }.getOrNull()
            }
        }
    }

    suspend fun getCachedHistory(peerId: String): List<ChatMessage> =
        messageCache.loadDm(peerId)

    suspend fun unreadCount(): Int = api.getChatUnread().count

    suspend fun search(q: String): Pair<List<ChatMessage>, List<ChatPeer>> {
        val res = api.searchChat(q)
        return res.messages.map { it.toDomain() } to res.peers.map { enrichPeer(it.toDomain()) }
    }

    suspend fun getContacts(): List<ChatPeer> = api.getContacts().map { enrichPeer(it.toPeer()) }

    suspend fun resolvePeer(peerId: String): ChatPeer? {
        knownPeer(peerId)?.takeIf { it.fullName.isNotBlank() }?.let { return it }
        getContacts().find { it.id == peerId }?.let { return enrichPeer(it) }
        getConversations().find { it.peer.id == peerId }?.peer?.let { return enrichPeer(it) }
        return null
    }

    suspend fun getContactProfile(peerId: String): PeerProfile? {
        val peer = knownPeer(peerId) ?: resolvePeer(peerId)
        val user = api.getContacts().find { it.id == peerId }
        return if (user != null) {
            enrichPeer(user.toPeer()).toPeerProfile(
                login = user.login,
                phone = user.phone,
                role = user.role,
            )
        } else {
            peer?.toPeerProfile()
        }
    }

    suspend fun send(
        receiverId: String,
        type: ChatMessageType,
        body: String? = null,
        upload: ChatUploadDto? = null,
        meta: ChatMessageMeta? = null,
        replyToId: String? = null,
        forwardedFrom: String? = null,
        clientId: String,
    ): ChatMessage {
        val mergedMeta = (meta ?: ChatMessageMeta()).copy(
            fileUrl = upload?.fileUrl ?: upload?.filePath?.let { uz.vazifa.app.util.MediaUrl.fromFilePath(it) } ?: meta?.fileUrl,
            fileSize = upload?.fileSize ?: meta?.fileSize,
        )
        val dto = api.sendChatMessage(
            SendMessageBody(
                receiverId = receiverId,
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
        )
        return dto.toDomain()
    }

    suspend fun uploadFile(file: File, mime: String): ChatUploadDto {
        val body = file.asRequestBody(mime.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", file.name, body)
        return api.uploadChatFile(part)
    }

    suspend fun markRead(peerId: String, messageIds: List<String>? = null) {
        socket.emitRead(peerId, messageIds)
        runCatching { api.markChatRead(MarkReadBody(peerId, messageIds)) }
    }

    suspend fun edit(id: String, body: String): ChatMessage = api.editChatMessage(id, EditMessageBody(body)).toDomain()
    suspend fun delete(id: String) { api.deleteChatMessage(id) }
    suspend fun react(id: String, emoji: String?): ChatMessage = api.reactChatMessage(id, ReactBody(emoji)).toDomain()
    suspend fun pin(id: String): ChatMessage = api.pinChatMessage(id).toDomain()
}

fun ChatMessageDto.toDomain(): ChatMessage {
    val resolvedMeta = meta.toDomainMessageMeta(filePath)
    return ChatMessage(
        id = id,
        senderId = senderId,
        receiverId = receiverId,
        type = ChatMessageType.from(type),
        body = body,
        filePath = filePath,
        fileName = fileName,
        mimeType = mimeType,
        meta = resolvedMeta,
        replyToId = replyToId,
        replyTo = replyTo?.copy(replyTo = null)?.let { safeToDomain(it) },
        forwardedFrom = forwardedFrom,
        reactions = reactions ?: emptyMap(),
        status = ChatMessageStatus.from(status),
        isRead = isRead,
        isEdited = isEdited,
        isDeleted = isDeleted,
        isPinned = isPinned,
        clientId = clientId,
        createdAt = createdAt?.takeIf { it.isNotBlank() } ?: Instant.now().toString(),
    )
}

internal fun ChatMetaDto?.toDomainMessageMeta(filePath: String?): ChatMessageMeta? {
    val base = this?.toDomain() ?: if (filePath.isNullOrBlank()) null else ChatMessageMeta()
    if (base == null) return null
    if (!base.fileUrl.isNullOrBlank() || filePath.isNullOrBlank()) return base
    return base.copy(fileUrl = uz.vazifa.app.util.MediaUrl.fromFilePath(filePath))
}

fun ChatMetaDto.toDomain(): ChatMessageMeta = ChatMessageMeta(
    fileUrl = fileUrl,
    fileSize = fileSize?.toLong(),
    durationSec = durationSec?.toInt(),
    waveform = waveform?.map { it.toInt() },
    width = width?.toInt(),
    height = height?.toInt(),
    thumbUrl = thumbUrl,
    latitude = latitude,
    longitude = longitude,
    contactName = contactName,
    contactPhone = contactPhone,
)

fun ChatMessageMeta.toDto(): ChatMetaDto = ChatMetaDto(
    fileUrl = fileUrl,
    fileSize = fileSize?.toDouble(),
    durationSec = durationSec?.toDouble(),
    waveform = waveform?.map { it.toDouble() },
    width = width?.toDouble(),
    height = height?.toDouble(),
    thumbUrl = thumbUrl,
    latitude = latitude,
    longitude = longitude,
    contactName = contactName,
    contactPhone = contactPhone,
)

private fun ChatPeerDto.toDomain(): ChatPeer = ChatPeer(
    id = id,
    fullName = fullName,
    avatarUrl = avatarUrl,
    position = position,
    department = department,
    isOnline = isOnline,
    lastSeenAt = lastSeenAt,
)

private fun UserDto.toPeer(): ChatPeer = ChatPeer(
    id = id,
    fullName = fullName,
    avatarUrl = avatarUrl,
    position = position,
    department = department,
    isOnline = isOnline,
    lastSeenAt = lastSeenAt,
)
