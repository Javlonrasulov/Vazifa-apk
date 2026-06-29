package uz.vazifa.app.data.remote

import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import uz.vazifa.app.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ChatEvent {
    data class NewMessage(val message: ChatMessageDto) : ChatEvent
    data class Status(val id: String, val status: String) : ChatEvent
    data class Read(val by: String, val peerId: String, val messageIds: List<String>?) : ChatEvent
    data class Updated(val message: ChatMessageDto) : ChatEvent
    data class Deleted(val id: String) : ChatEvent
    data class Typing(val userId: String, val typing: Boolean, val action: String = "typing") : ChatEvent
    data class Presence(val userId: String, val online: Boolean, val lastSeenAt: String?) : ChatEvent
    data class PresenceList(val online: List<String>) : ChatEvent

    data class RoomCreated(val room: RoomDto) : ChatEvent
    data class RoomNewMessage(val message: RoomMessageDto) : ChatEvent
    data class RoomUpdated(val message: RoomMessageDto) : ChatEvent
    data class RoomDeleted(val roomId: String, val id: String) : ChatEvent
    data class RoomTyping(
        val roomId: String,
        val userId: String,
        val fullName: String,
        val typing: Boolean,
        val action: String = "typing",
    ) : ChatEvent
}

@Singleton
class ChatSocket @Inject constructor(
    private val tokenStore: TokenStore,
) {
    private val gson = Gson()
    private var socket: Socket? = null
    private var lastAuthToken: String? = null

    private val _events = MutableSharedFlow<ChatEvent>(extraBufferCapacity = 128)
    val events: SharedFlow<ChatEvent> = _events

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected

    private fun socketUrl(): String = BuildConfig.API_BASE_URL
        .removeSuffix("/api/v1/")
        .removeSuffix("/api/v1")
        .removeSuffix("/") + "/chat"

    @Synchronized
    fun connect() {
        val token = tokenStore.accessToken ?: return
        if (socket != null && lastAuthToken != null && lastAuthToken != token) {
            tearDownSocket()
        }
        if (socket?.connected() == true) return
        if (socket != null) {
            lastAuthToken = token
            socket?.connect()
            return
        }
        lastAuthToken = token
        val opts = IO.Options().apply {
            forceNew = true
            reconnection = true
            reconnectionDelay = 1_000
            reconnectionDelayMax = 5_000
            transports = arrayOf("websocket")
            auth = mapOf("token" to token)
        }
        val s = IO.socket(socketUrl(), opts)
        s.on(Socket.EVENT_CONNECT) { _connected.value = true }
        s.on(Socket.EVENT_DISCONNECT) { _connected.value = false }
        s.on(Socket.EVENT_CONNECT_ERROR) { _connected.value = false }

        s.on("message:new") { args -> emitMessage(args) { ChatEvent.NewMessage(it) } }
        s.on("message:sent") { args -> emitMessage(args) { ChatEvent.NewMessage(it) } }
        s.on("message:updated") { args -> emitMessage(args) { ChatEvent.Updated(it) } }
        s.on("message:status") { args ->
            obj(args)?.let {
                _events.tryEmit(ChatEvent.Status(it.optString("id"), it.optString("status")))
            }
        }
        s.on("message:read") { args ->
            obj(args)?.let { o ->
                val ids = o.optJSONArray("messageIds")?.let { arr ->
                    List(arr.length()) { arr.optString(it) }
                }
                _events.tryEmit(ChatEvent.Read(o.optString("by"), o.optString("peerId"), ids))
            }
        }
        s.on("message:deleted") { args ->
            obj(args)?.let { _events.tryEmit(ChatEvent.Deleted(it.optString("id"))) }
        }
        s.on("message:typing") { args ->
            obj(args)?.let {
                _events.tryEmit(
                    ChatEvent.Typing(
                        it.optString("userId"),
                        it.optBoolean("typing"),
                        it.optString("action", "typing").ifBlank { "typing" },
                    ),
                )
            }
        }
        s.on("presence:update") { args ->
            obj(args)?.let {
                _events.tryEmit(
                    ChatEvent.Presence(
                        it.optString("userId"),
                        it.optBoolean("online"),
                        if (it.isNull("lastSeenAt")) null else it.optString("lastSeenAt"),
                    ),
                )
            }
        }
        s.on("presence:list") { args ->
            obj(args)?.optJSONArray("online")?.let { arr ->
                _events.tryEmit(ChatEvent.PresenceList(List(arr.length()) { arr.optString(it) }))
            }
        }

        s.on("room:created") { args ->
            obj(args)?.let { o ->
                runCatching { gson.fromJson(o.toString(), RoomDto::class.java) }
                    .getOrNull()?.let { _events.tryEmit(ChatEvent.RoomCreated(it)) }
            }
        }
        s.on("room:message:new") { args -> emitRoomMessage(args) { ChatEvent.RoomNewMessage(it) } }
        s.on("room:message:updated") { args -> emitRoomMessage(args) { ChatEvent.RoomUpdated(it) } }
        s.on("room:message:deleted") { args ->
            obj(args)?.let {
                _events.tryEmit(ChatEvent.RoomDeleted(it.optString("roomId"), it.optString("id")))
            }
        }
        s.on("room:typing") { args ->
            obj(args)?.let {
                _events.tryEmit(
                    ChatEvent.RoomTyping(
                        it.optString("roomId"),
                        it.optString("userId"),
                        it.optString("fullName"),
                        it.optBoolean("typing"),
                        it.optString("action", "typing").ifBlank { "typing" },
                    ),
                )
            }
        }

        socket = s
        s.connect()
    }

    private fun parseMessageDto(o: JSONObject?): ChatMessageDto? {
        if (o == null) return null
        return runCatching { gson.fromJson(o.toString(), ChatMessageDto::class.java) }.getOrNull()
            ?: parseMessageDtoLoose(o)
    }

    private fun parseMessageDtoLoose(o: JSONObject): ChatMessageDto? {
        return runCatching {
            fun str(key: String): String? = o.optString(key).trim().takeIf { it.isNotBlank() && it != "null" }
            val id = str("id") ?: return@runCatching null
            val senderId = str("senderId") ?: return@runCatching null
            val receiverId = str("receiverId") ?: return@runCatching null
            val metaObj = o.optJSONObject("meta")
            val meta = metaObj?.let { m ->
                ChatMetaDto(
                    fileUrl = m.optString("fileUrl").ifBlank { null },
                    fileSize = m.optDouble("fileSize").takeIf { !it.isNaN() },
                    durationSec = m.optDouble("durationSec").takeIf { !it.isNaN() },
                    width = m.optDouble("width").takeIf { !it.isNaN() },
                    height = m.optDouble("height").takeIf { !it.isNaN() },
                    thumbUrl = m.optString("thumbUrl").ifBlank { null },
                )
            }
            ChatMessageDto(
                id = id,
                senderId = senderId,
                receiverId = receiverId,
                type = str("type") ?: "text",
                body = str("body"),
                filePath = str("filePath"),
                fileName = str("fileName"),
                mimeType = str("mimeType"),
                meta = meta,
                replyToId = str("replyToId"),
                status = str("status") ?: "sent",
                isRead = o.optBoolean("isRead"),
                isEdited = o.optBoolean("isEdited"),
                isDeleted = o.optBoolean("isDeleted"),
                isPinned = o.optBoolean("isPinned"),
                clientId = str("clientId"),
                createdAt = str("createdAt"),
            )
        }.getOrNull()
    }

    private inline fun emitMessage(args: Array<Any>, build: (ChatMessageDto) -> ChatEvent) {
        parseMessageDto(parsePayload(args))?.let { _events.tryEmit(build(it)) }
    }

    private fun parsePayload(args: Array<Any>): JSONObject? {
        val raw = args.firstOrNull() ?: return null
        return when (raw) {
            is JSONObject -> raw
            is String -> runCatching { JSONObject(raw) }.getOrNull()
            else -> runCatching { JSONObject(gson.toJson(raw)) }.getOrNull()
        }
    }

    private fun obj(args: Array<Any>): JSONObject? = parsePayload(args)

    private inline fun emitRoomMessage(args: Array<Any>, build: (RoomMessageDto) -> ChatEvent) {
        val o = parsePayload(args) ?: return
        runCatching { gson.fromJson(o.toString(), RoomMessageDto::class.java) }
            .getOrNull()?.let { _events.tryEmit(build(it)) }
    }

    fun sendTyping(receiverId: String, typing: Boolean, action: String = "typing") {
        socket?.emit(
            "message:typing",
            JSONObject().put("receiverId", receiverId).put("typing", typing).put("action", action),
        )
    }

    fun emitRead(peerId: String, messageIds: List<String>? = null) {
        val payload = JSONObject().put("peerId", peerId)
        if (messageIds != null) payload.put("messageIds", org.json.JSONArray(messageIds))
        socket?.emit("message:read", payload)
    }

    fun sendRoomTyping(roomId: String, typing: Boolean, action: String = "typing") {
        socket?.emit(
            "room:typing",
            JSONObject().put("roomId", roomId).put("typing", typing).put("action", action),
        )
    }

    fun ping() {
        socket?.emit("presence:ping")
    }

    /** JWT yangilanganda yoki ilova qayta ochilganda — eski token bilan qayta ulanish */
    @Synchronized
    fun reconnect() {
        tearDownSocket()
        connect()
    }

    @Synchronized
    fun disconnect() {
        tearDownSocket()
    }

    private fun tearDownSocket() {
        socket?.disconnect()
        socket?.off()
        socket = null
        lastAuthToken = null
        _connected.value = false
    }
}
