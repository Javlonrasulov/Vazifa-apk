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
        if (socket?.connected() == true) return
        if (socket != null) {
            socket?.connect()
            return
        }
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

    private inline fun emitMessage(args: Array<Any>, build: (ChatMessageDto) -> ChatEvent) {
        val o = obj(args) ?: return
        runCatching { gson.fromJson(o.toString(), ChatMessageDto::class.java) }
            .getOrNull()?.let { _events.tryEmit(build(it)) }
    }

    private inline fun emitRoomMessage(args: Array<Any>, build: (RoomMessageDto) -> ChatEvent) {
        val o = obj(args) ?: return
        runCatching { gson.fromJson(o.toString(), RoomMessageDto::class.java) }
            .getOrNull()?.let { _events.tryEmit(build(it)) }
    }

    private fun obj(args: Array<Any>): JSONObject? = args.firstOrNull() as? JSONObject

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

    @Synchronized
    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
        _connected.value = false
    }
}
