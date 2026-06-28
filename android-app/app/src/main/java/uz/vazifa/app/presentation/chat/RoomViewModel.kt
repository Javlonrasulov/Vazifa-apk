package uz.vazifa.app.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.vazifa.app.data.remote.ChatEvent
import uz.vazifa.app.data.remote.ChatUploadDto
import uz.vazifa.app.data.repository.ChatRepository
import uz.vazifa.app.data.repository.RoomRepository
import uz.vazifa.app.data.repository.toDomain
import uz.vazifa.app.domain.model.ChatMessage
import uz.vazifa.app.domain.model.ChatMessageMeta
import uz.vazifa.app.domain.model.ChatPeer
import uz.vazifa.app.domain.model.ChatMessageStatus
import uz.vazifa.app.domain.model.ChatMessageType
import uz.vazifa.app.domain.model.ChatRoom
import uz.vazifa.app.domain.model.RoomMember
import uz.vazifa.app.domain.model.RoomMessage
import java.io.File
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

data class RoomUiState(
    val room: ChatRoom? = null,
    val members: List<RoomMember> = emptyList(),
    val messages: List<RoomMessage> = emptyList(),
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val loadFailed: Boolean = false,
    val input: String = "",
    val replyTo: RoomMessage? = null,
    val editing: RoomMessage? = null,
    /** "<fullName> action" yoki null */
    val typingLabel: String? = null,
)

@HiltViewModel
class RoomViewModel @Inject constructor(
    private val repo: RoomRepository,
    private val chatRepo: ChatRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(RoomUiState())
    val state = _state.asStateFlow()

    private var roomId: String = ""
    private var currentUserId: String = ""
    private var socketObserving = false
    private var historyJob: Job? = null
    private val hiddenIds = mutableSetOf<String>()
    private var stopTypingJob: Job? = null
    private val typingUsers = mutableMapOf<String, Pair<String, String>>() // userId -> (name, action)
    private val typingTimers = mutableMapOf<String, Job>()

    fun isMine(senderId: String): Boolean = senderId == currentUserId

    fun start(roomId: String, currentUserId: String) {
        if (roomId.isBlank() || currentUserId.isBlank()) return
        val roomChanged = this.roomId != roomId
        this.roomId = roomId
        this.currentUserId = currentUserId
        if (roomChanged) {
            hiddenIds.clear()
            _state.value = RoomUiState(loading = true)
        }
        repo.connect()
        if (!socketObserving) {
            socketObserving = true
            observeSocket()
        }
        viewModelScope.launch {
            runCatching { repo.get(roomId) }.onSuccess { r -> _state.update { it.copy(room = r) } }
            runCatching { repo.members(roomId) }.onSuccess { m -> _state.update { it.copy(members = m) } }
            if (roomChanged || _state.value.messages.isEmpty()) {
                loadHistory()
            }
        }
    }

    private fun loadHistory() {
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            val cached = runCatching { repo.getCachedHistory(roomId) }.getOrDefault(emptyList())
            if (cached.isNotEmpty()) {
                _state.update { st ->
                    val visible = cached.filter { !it.isDeleted && it.id !in hiddenIds }
                    st.copy(messages = visible, loading = true, loadFailed = false)
                }
            } else {
                _state.update { it.copy(loading = true, loadFailed = false) }
            }
            runCatching { repo.history(roomId, before = null) }
                .onSuccess { list ->
                    _state.update { st ->
                        val visible = list.filter { !it.isDeleted && it.id !in hiddenIds && (it.clientId == null || it.clientId !in hiddenIds) }
                        val merged = mergeRoomHistory(visible, st.messages)
                        st.copy(messages = merged, loading = false, loadFailed = false, hasMore = list.size >= 40)
                    }
                    markRead()
                }
                .onFailure {
                    _state.update { st ->
                        st.copy(loading = false, loadFailed = st.messages.isEmpty())
                    }
                }
        }
    }

    fun reload() {
        if (roomId.isBlank()) return
        loadHistory()
    }

    private fun mergeRoomHistory(server: List<RoomMessage>, local: List<RoomMessage>): List<RoomMessage> {
        if (local.isEmpty()) return server
        val serverIds = server.map { it.id }.toSet()
        val serverClientIds = server.mapNotNull { it.clientId }.toSet()
        val merged = server.toMutableList()
        for (msg in local) {
            if (msg.id in serverIds) continue
            if (msg.clientId != null && msg.clientId in serverClientIds) continue
            val pending = msg.status == ChatMessageStatus.SENDING ||
                msg.status == ChatMessageStatus.FAILED ||
                (msg.clientId != null && msg.id == msg.clientId)
            if (pending) merged.add(msg)
        }
        return merged.sortedBy {
            runCatching { Instant.parse(it.createdAt) }.getOrNull() ?: Instant.EPOCH
        }
    }

    fun loadMore() {
        val st = _state.value
        if (st.loadingMore || !st.hasMore || st.messages.isEmpty()) return
        val oldest = st.messages.first().createdAt
        viewModelScope.launch {
            _state.update { it.copy(loadingMore = true) }
            runCatching { repo.history(roomId, before = oldest) }
                .onSuccess { older ->
                    _state.update {
                        it.copy(
                            messages = older + it.messages,
                            loadingMore = false,
                            hasMore = older.size >= 40,
                        )
                    }
                }
                .onFailure { _state.update { it.copy(loadingMore = false) } }
        }
    }

    private fun observeSocket() {
        viewModelScope.launch {
            repo.events.collect { ev ->
                when (ev) {
                    is ChatEvent.RoomNewMessage -> if (ev.message.roomId == roomId) {
                        clearTyping(ev.message.senderId)
                        upsert(ev.message.toDomain())
                        markRead()
                    }
                    is ChatEvent.RoomUpdated -> if (ev.message.roomId == roomId) upsert(ev.message.toDomain())
                    is ChatEvent.RoomDeleted -> if (ev.roomId == roomId) _state.update { st ->
                        st.copy(messages = st.messages.filter { it.id != ev.id })
                    }
                    is ChatEvent.RoomTyping -> if (ev.roomId == roomId && ev.userId != currentUserId) {
                        if (ev.typing) onTyping(ev.userId, ev.fullName, ev.action) else clearTyping(ev.userId)
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun onTyping(userId: String, name: String, action: String) {
        typingTimers.remove(userId)?.cancel()
        typingUsers[userId] = name to action
        recomputeTyping()
        typingTimers[userId] = viewModelScope.launch {
            delay(5_000)
            clearTyping(userId)
        }
    }

    private fun clearTyping(userId: String) {
        typingTimers.remove(userId)?.cancel()
        if (typingUsers.remove(userId) != null) recomputeTyping()
    }

    private fun recomputeTyping() {
        val first = typingUsers.values.firstOrNull()
        _state.update { it.copy(typingLabel = first?.let { (name, action) -> "${name.substringBefore(' ')}|$action" }) }
    }

    private fun upsert(msg: RoomMessage) {
        _state.update { st ->
            val list = st.messages.toMutableList()
            val byId = list.indexOfFirst { it.id == msg.id }
            val byClient = if (msg.clientId != null) {
                list.indexOfFirst { it.id == msg.clientId || (it.clientId != null && it.clientId == msg.clientId) }
            } else -1
            when {
                byId >= 0 -> list[byId] = msg
                byClient >= 0 -> list[byClient] = msg
                else -> list.add(msg)
            }
            list.sortBy { runCatching { Instant.parse(it.createdAt) }.getOrNull() ?: Instant.now() }
            st.copy(messages = list)
        }
    }

    private fun markRead() {
        viewModelScope.launch { runCatching { repo.markRead(roomId) } }
    }

    fun onInputChange(text: String) {
        _state.update { it.copy(input = text) }
        if (text.isNotBlank()) {
            repo.sendTyping(roomId, true, "typing")
            stopTypingJob?.cancel()
            stopTypingJob = viewModelScope.launch {
                delay(2_000)
                repo.sendTyping(roomId, false, "typing")
            }
        } else {
            repo.sendTyping(roomId, false, "typing")
        }
    }

    fun setRecording(active: Boolean) = repo.sendTyping(roomId, active, "recording")
    fun setUploading(active: Boolean) = repo.sendTyping(roomId, active, "uploading")

    fun setReplyTo(msg: RoomMessage?) = _state.update { it.copy(replyTo = msg, editing = null) }
    fun startEdit(msg: RoomMessage) = _state.update { it.copy(editing = msg, input = msg.body.orEmpty(), replyTo = null) }
    fun cancelEdit() = _state.update { it.copy(editing = null, input = "") }

    fun sendText() {
        val st = _state.value
        val text = st.input.trim()
        if (text.isEmpty()) return
        if (st.editing != null) {
            val editing = st.editing
            _state.update { it.copy(input = "", editing = null) }
            viewModelScope.launch { runCatching { repo.edit(editing.id, text) }.onSuccess { upsert(it) } }
            return
        }
        val replyId = st.replyTo?.id
        _state.update { it.copy(input = "", replyTo = null) }
        repo.sendTyping(roomId, false)
        dispatchSend(ChatMessageType.TEXT, body = text, replyToId = replyId)
    }

    fun sendUpload(type: ChatMessageType, upload: ChatUploadDto, meta: ChatMessageMeta? = null, caption: String? = null) {
        dispatchSend(type, body = caption, upload = upload, meta = meta)
    }

    fun sendLocation(lat: Double, lng: Double) {
        dispatchSend(ChatMessageType.LOCATION, meta = ChatMessageMeta(latitude = lat, longitude = lng))
    }

    fun sendContact(name: String, phone: String) {
        dispatchSend(ChatMessageType.CONTACT, body = name, meta = ChatMessageMeta(contactName = name, contactPhone = phone))
    }

    private fun dispatchSend(
        type: ChatMessageType,
        body: String? = null,
        upload: ChatUploadDto? = null,
        meta: ChatMessageMeta? = null,
        replyToId: String? = null,
    ) {
        val clientId = UUID.randomUUID().toString()
        val fileUrl = upload?.fileUrl ?: upload?.filePath?.let { uz.vazifa.app.util.MediaUrl.fromFilePath(it) }
        val optimistic = RoomMessage(
            id = clientId,
            roomId = roomId,
            senderId = currentUserId,
            type = type,
            body = body,
            filePath = upload?.filePath,
            fileName = upload?.fileName,
            mimeType = upload?.mimeType,
            meta = (meta ?: ChatMessageMeta()).copy(fileUrl = fileUrl, fileSize = upload?.fileSize),
            replyToId = replyToId,
            replyTo = _state.value.messages.firstOrNull { it.id == replyToId },
            status = ChatMessageStatus.SENDING,
            clientId = clientId,
            createdAt = Instant.now().toString(),
        )
        upsert(optimistic)
        viewModelScope.launch {
            runCatching {
                repo.send(
                    roomId = roomId,
                    type = type,
                    body = body,
                    upload = upload,
                    meta = meta,
                    replyToId = replyToId,
                    clientId = clientId,
                )
            }.onSuccess { sent ->
                val mergedMeta = sent.meta?.copy(
                    fileUrl = sent.meta?.fileUrl ?: fileUrl,
                    fileSize = sent.meta?.fileSize ?: upload?.fileSize,
                ) ?: optimistic.meta
                upsert(
                    sent.copy(
                        clientId = clientId,
                        meta = mergedMeta,
                        filePath = sent.filePath ?: upload?.filePath,
                    ),
                )
            }.onFailure {
                    _state.update { st ->
                        st.copy(messages = st.messages.map {
                            if (it.id == clientId) it.copy(status = ChatMessageStatus.FAILED) else it
                        })
                    }
                }
        }
    }

    suspend fun upload(file: File, mime: String): ChatUploadDto = repo.uploadFile(file, mime)

    fun react(msg: RoomMessage, emoji: String) {
        val current = msg.reactions[currentUserId]
        val next = if (current == emoji) null else emoji
        viewModelScope.launch { runCatching { repo.react(msg.id, next) }.onSuccess { upsert(it) } }
    }

    fun pin(msg: RoomMessage) {
        viewModelScope.launch { runCatching { repo.pin(msg.id) }.onSuccess { upsert(it) } }
    }

    fun delete(msg: RoomMessage) {
        _state.update { st -> st.copy(messages = st.messages.filter { it.id != msg.id }) }
        viewModelScope.launch { runCatching { repo.delete(msg.id, room = true) } }
    }

    fun hideForMe(msg: RoomMessage) {
        hiddenIds.add(msg.id)
        _state.update { st -> st.copy(messages = st.messages.filter { it.id != msg.id }) }
    }

    suspend fun loadForwardTargets(): List<ChatPeer> =
        runCatching { chatRepo.getContacts() }
            .getOrDefault(emptyList())
            .filter { it.id != currentUserId }

    fun forward(msg: RoomMessage, toPeerId: String) {
        viewModelScope.launch {
            runCatching {
                val chatMsg = msg.toChatMessageForForward()
                val upload = buildForwardUpload(chatMsg)
                chatRepo.send(
                    receiverId = toPeerId,
                    type = chatMsg.type,
                    body = chatMsg.body,
                    upload = upload,
                    meta = chatMsg.meta,
                    forwardedFrom = chatMsg.id,
                    clientId = UUID.randomUUID().toString(),
                )
            }
        }
    }

    private fun buildForwardUpload(msg: ChatMessage): ChatUploadDto? {
        if (msg.type == ChatMessageType.TEXT || msg.type == ChatMessageType.LOCATION || msg.type == ChatMessageType.CONTACT) {
            return null
        }
        val path = msg.filePath ?: msg.meta?.fileUrl ?: return null
        return ChatUploadDto(
            filePath = path,
            fileName = msg.fileName ?: "file",
            mimeType = msg.mimeType ?: "application/octet-stream",
            fileSize = msg.meta?.fileSize ?: 0L,
            fileUrl = msg.meta?.fileUrl,
        )
    }

    fun deleteRoom(onDone: () -> Unit) {
        if (roomId.isBlank()) return
        viewModelScope.launch {
            runCatching { repo.delete(roomId) }.onSuccess { onDone() }
        }
    }

    override fun onCleared() {
        super.onCleared()
        repo.sendTyping(roomId, false)
    }
}

private fun RoomMessage.toChatMessageForForward(): ChatMessage = ChatMessage(
    id = id,
    senderId = senderId,
    receiverId = roomId,
    type = type,
    body = body,
    filePath = filePath,
    fileName = fileName,
    mimeType = mimeType,
    meta = meta,
    createdAt = createdAt,
)
