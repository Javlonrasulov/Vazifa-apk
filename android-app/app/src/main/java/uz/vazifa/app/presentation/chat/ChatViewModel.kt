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
import uz.vazifa.app.data.repository.toDomain
import uz.vazifa.app.domain.model.ChatMessage
import uz.vazifa.app.domain.model.ChatMessageMeta
import uz.vazifa.app.domain.model.ChatMessageStatus
import uz.vazifa.app.domain.model.ChatMessageType
import uz.vazifa.app.domain.model.ChatPeer
import java.io.File
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val peer: ChatPeer? = null,
    val messages: List<ChatMessage> = emptyList(),
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val input: String = "",
    val replyTo: ChatMessage? = null,
    val editing: ChatMessage? = null,
    val peerTyping: Boolean = false,
    val sending: Boolean = false,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repo: ChatRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ChatUiState())
    val state = _state.asStateFlow()

    private var peerId: String = ""
    private var currentUserId: String = ""
    private var typingJob: Job? = null
    private var stopTypingJob: Job? = null
    private var started = false

    fun isMine(msg: ChatMessage): Boolean = msg.senderId == currentUserId

    fun start(peerId: String, currentUserId: String, initialPeer: ChatPeer?) {
        if (started) return
        started = true
        this.peerId = peerId
        this.currentUserId = currentUserId
        if (initialPeer != null) _state.update { it.copy(peer = initialPeer) }
        repo.connect()
        observeSocket()
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            runCatching { repo.getHistory(peerId, before = null) }
                .onSuccess { list ->
                    _state.update { it.copy(messages = list, loading = false, hasMore = list.size >= 40) }
                    markIncomingRead()
                }
                .onFailure { _state.update { it.copy(loading = false) } }
        }
    }

    fun loadMore() {
        val st = _state.value
        if (st.loadingMore || !st.hasMore || st.messages.isEmpty()) return
        val oldest = st.messages.first().createdAt
        viewModelScope.launch {
            _state.update { it.copy(loadingMore = true) }
            runCatching { repo.getHistory(peerId, before = oldest) }
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
                    is ChatEvent.NewMessage -> {
                        val m = ev.message.toDomain()
                        if (involvesPeer(m)) {
                            upsert(m)
                            if (m.senderId == peerId) markIncomingRead()
                        }
                    }
                    is ChatEvent.Updated -> {
                        val m = ev.message.toDomain()
                        if (involvesPeer(m)) upsert(m)
                    }
                    is ChatEvent.Deleted -> _state.update { st ->
                        st.copy(messages = st.messages.map {
                            if (it.id == ev.id) it.copy(isDeleted = true, body = null, meta = null) else it
                        })
                    }
                    is ChatEvent.Status -> _state.update { st ->
                        st.copy(messages = st.messages.map {
                            if (it.id == ev.id) it.copy(status = ChatMessageStatus.from(ev.status)) else it
                        })
                    }
                    is ChatEvent.Read -> if (ev.by == peerId) _state.update { st ->
                        st.copy(messages = st.messages.map {
                            if (isMine(it)) it.copy(status = ChatMessageStatus.READ, isRead = true) else it
                        })
                    }
                    is ChatEvent.Typing -> if (ev.userId == peerId) {
                        _state.update { it.copy(peerTyping = ev.typing) }
                    }
                    is ChatEvent.Presence -> if (ev.userId == peerId) _state.update {
                        it.copy(peer = it.peer?.copy(isOnline = ev.online, lastSeenAt = ev.lastSeenAt ?: it.peer.lastSeenAt))
                    }
                    is ChatEvent.PresenceList -> _state.update {
                        it.copy(peer = it.peer?.copy(isOnline = peerId in ev.online))
                    }
                }
            }
        }
    }

    private fun involvesPeer(m: ChatMessage): Boolean =
        (m.senderId == peerId && m.receiverId == currentUserId) ||
            (m.senderId == currentUserId && m.receiverId == peerId)

    private fun upsert(msg: ChatMessage) {
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

    private fun markIncomingRead() {
        val unread = _state.value.messages.filter { it.senderId == peerId && !it.isRead }
        if (unread.isEmpty()) return
        _state.update { st ->
            st.copy(messages = st.messages.map {
                if (it.senderId == peerId) it.copy(isRead = true, status = ChatMessageStatus.READ) else it
            })
        }
        viewModelScope.launch { runCatching { repo.markRead(peerId) } }
    }

    fun onInputChange(text: String) {
        _state.update { it.copy(input = text) }
        if (text.isNotBlank()) {
            repo.sendTyping(peerId, true)
            stopTypingJob?.cancel()
            stopTypingJob = viewModelScope.launch {
                delay(2_000)
                repo.sendTyping(peerId, false)
            }
        } else {
            repo.sendTyping(peerId, false)
        }
    }

    fun setReplyTo(msg: ChatMessage?) = _state.update { it.copy(replyTo = msg, editing = null) }
    fun startEdit(msg: ChatMessage) = _state.update { it.copy(editing = msg, input = msg.body.orEmpty(), replyTo = null) }
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
        repo.sendTyping(peerId, false)
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
        val optimistic = ChatMessage(
            id = clientId,
            senderId = currentUserId,
            receiverId = peerId,
            type = type,
            body = body,
            fileName = upload?.fileName,
            mimeType = upload?.mimeType,
            meta = (meta ?: ChatMessageMeta()).copy(fileUrl = upload?.fileUrl, fileSize = upload?.fileSize),
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
                    receiverId = peerId,
                    type = type,
                    body = body,
                    upload = upload,
                    meta = meta,
                    replyToId = replyToId,
                    clientId = clientId,
                )
            }.onSuccess { sent -> upsert(sent.copy(clientId = clientId)) }
                .onFailure {
                    _state.update { st ->
                        st.copy(messages = st.messages.map {
                            if (it.id == clientId) it.copy(status = ChatMessageStatus.FAILED) else it
                        })
                    }
                }
        }
    }

    suspend fun upload(file: File, mime: String): ChatUploadDto = repo.uploadFile(file, mime)

    fun react(msg: ChatMessage, emoji: String) {
        val current = msg.reactions[currentUserId]
        val next = if (current == emoji) null else emoji
        viewModelScope.launch { runCatching { repo.react(msg.id, next) }.onSuccess { upsert(it) } }
    }

    fun pin(msg: ChatMessage) {
        viewModelScope.launch { runCatching { repo.pin(msg.id) }.onSuccess { upsert(it) } }
    }

    fun delete(msg: ChatMessage) {
        _state.update { st -> st.copy(messages = st.messages.map { if (it.id == msg.id) it.copy(isDeleted = true, body = null, meta = null) else it }) }
        viewModelScope.launch { runCatching { repo.delete(msg.id) } }
    }

    override fun onCleared() {
        super.onCleared()
        repo.sendTyping(peerId, false)
    }
}
