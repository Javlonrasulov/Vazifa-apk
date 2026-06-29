package uz.vazifa.app.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.vazifa.app.data.remote.ChatEvent
import uz.vazifa.app.data.remote.ChatUploadDto
import uz.vazifa.app.chat.ActiveChatTracker
import uz.vazifa.app.data.remote.ChatMessageDto
import uz.vazifa.app.data.repository.ChatRepository
import uz.vazifa.app.data.repository.safeToDomain
import uz.vazifa.app.domain.model.ChatMessage
import uz.vazifa.app.domain.model.ChatMessageMeta
import uz.vazifa.app.domain.model.ChatMessageStatus
import uz.vazifa.app.domain.model.ChatMessageType
import uz.vazifa.app.domain.model.ChatPeer
import uz.vazifa.app.domain.model.PeerProfile
import uz.vazifa.app.domain.model.toPeerProfile
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
    /** null | "typing" | "recording" | "uploading" */
    val peerActivity: String? = null,
    val sending: Boolean = false,
    val loadFailed: Boolean = false,
    val peerProfile: PeerProfile? = null,
    val peerProfileLoading: Boolean = false,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repo: ChatRepository,
    private val auth: uz.vazifa.app.data.repository.AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ChatUiState())
    val state = _state.asStateFlow()

    private var peerId: String = ""
    private var currentUserId: String = ""
    private var stopTypingJob: Job? = null
    private var historyJob: Job? = null
    private val historyMutex = Mutex()
    private var socketJob: Job? = null
    private var refreshJob: Job? = null
    private val hiddenIds = mutableSetOf<String>()

    private fun applyHidden(list: List<ChatMessage>): List<ChatMessage> {
        val filtered = list.filter { !it.isDeleted }
        if (hiddenIds.isEmpty()) return filtered
        return filtered.filter {
            it.id !in hiddenIds && (it.clientId == null || it.clientId !in hiddenIds)
        }
    }

    fun isMine(msg: ChatMessage): Boolean = msg.senderId == currentUserId

    fun start(peerId: String, initialPeer: ChatPeer?) {
        if (peerId.isBlank()) return

        val peerChanged = this.peerId != peerId
        this.peerId = peerId
        initialPeer?.let {
            repo.rememberPeer(it)
            _state.update { st -> st.copy(peer = it) }
        }
        viewModelScope.launch {
            val userId = auth.ensureSessionForApi()
            if (userId.isNullOrBlank()) {
                _state.update { it.copy(loading = false, loadFailed = true) }
                return@launch
            }
            currentUserId = userId
            repo.reconnect()
            repo.ping()
            ensureSocketObserver()
            hiddenIds.clear()
            if (peerChanged || _state.value.messages.isEmpty()) {
                val cached = runCatching { repo.getCachedHistory(peerId) }.getOrDefault(emptyList())
                _state.update {
                    ChatUiState(
                        loading = true,
                        peer = initialPeer?.takeIf { it.fullName.isNotBlank() } ?: _state.value.peer,
                        messages = applyHidden(cached),
                    )
                }
            } else if (initialPeer?.fullName?.isNotBlank() == true) {
                repo.rememberPeer(initialPeer)
                _state.update { st -> st.copy(peer = initialPeer) }
            }
            runCatching { repo.loadAliases(userId) }
            resolvePeerInfo(initialPeer)
            loadHistory()
        }
    }

    fun reload() {
        if (peerId.isBlank()) return
        repo.reconnect()
        repo.ping()
        viewModelScope.launch {
            val userId = auth.ensureSessionForApi() ?: return@launch
            currentUserId = userId
            loadHistory()
        }
    }

    fun loadPeerProfile() {
        if (peerId.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(peerProfileLoading = true) }
            val profile = runCatching { repo.getContactProfile(peerId) }.getOrNull()
                ?: _state.value.peer?.toPeerProfile()
            _state.update { st ->
                val peer = st.peer
                st.copy(
                    peerProfile = profile,
                    peerProfileLoading = false,
                    peer = peer?.copy(
                        avatarUrl = profile?.avatarUrl ?: peer.avatarUrl,
                        position = profile?.position ?: peer.position,
                        department = profile?.department ?: peer.department,
                        isOnline = profile?.isOnline ?: peer.isOnline,
                        lastSeenAt = profile?.lastSeenAt ?: peer.lastSeenAt,
                    ),
                )
            }
        }
    }

    private suspend fun resolvePeerInfo(initialPeer: ChatPeer?) {
        val alias = repo.aliasFor(peerId)
        var peer = repo.knownPeer(peerId)
            ?: initialPeer?.takeIf { it.fullName.isNotBlank() }
            ?: ChatPeer(id = peerId, fullName = initialPeer?.fullName.orEmpty())
        if (peer.fullName.isBlank()) {
            peer = repo.resolvePeer(peerId) ?: peer
        }
        runCatching { repo.getConversations() }.getOrNull()
            ?.find { it.peer.id == peerId }
            ?.peer
            ?.let { conv ->
                peer = peer.copy(
                    fullName = peer.fullName.ifBlank { conv.fullName },
                    avatarUrl = peer.avatarUrl ?: conv.avatarUrl,
                    isOnline = conv.isOnline,
                    lastSeenAt = conv.lastSeenAt ?: peer.lastSeenAt,
                    position = peer.position ?: conv.position,
                )
            }
        if (peer.avatarUrl.isNullOrBlank()) {
            runCatching { repo.getContacts() }.getOrNull()
                ?.find { it.id == peerId }
                ?.let { contact ->
                    peer = peer.copy(
                        avatarUrl = contact.avatarUrl,
                        position = peer.position ?: contact.position,
                        department = peer.department ?: contact.department,
                    )
                }
        }
        if (alias != null) peer = peer.copy(alias = alias)
        repo.rememberPeer(peer)
        _state.update { it.copy(peer = peer) }
    }

    fun renameContact(alias: String?) {
        _state.update { st -> st.copy(peer = st.peer?.copy(alias = alias?.trim()?.takeIf { it.isNotBlank() })) }
        viewModelScope.launch { runCatching { repo.setAlias(peerId, alias) } }
    }

    private fun loadHistory() {
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            historyMutex.withLock {
                val cached = runCatching { repo.getCachedHistory(peerId) }.getOrDefault(emptyList())
                if (cached.isNotEmpty()) {
                    _state.update { st ->
                        st.copy(messages = applyHidden(cached), loading = true, loadFailed = false)
                    }
                } else {
                    _state.update { it.copy(loading = true, loadFailed = false) }
                }
                try {
                    var list = repo.getHistory(peerId, before = null)
                    if (list.isEmpty()) {
                        auth.currentUser()
                        list = repo.getHistory(peerId, before = null)
                    }
                    if (list.isEmpty()) {
                        list = fallbackFromConversation()
                    }
                    _state.update { st ->
                        val merged = mergeChatHistory(applyHidden(list), st.messages)
                        st.copy(
                            messages = merged,
                            loading = false,
                            loadFailed = merged.isEmpty(),
                            hasMore = list.size >= 40,
                        )
                    }
                    if (list.isNotEmpty()) markIncomingRead()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    val fallback = fallbackFromConversation()
                    _state.update { st ->
                        if (fallback.isNotEmpty()) {
                            st.copy(messages = applyHidden(fallback), loading = false, loadFailed = false)
                        } else {
                            st.copy(loading = false, loadFailed = st.messages.isEmpty())
                        }
                    }
                }
            }
        }
    }

    private suspend fun fallbackFromConversation(): List<ChatMessage> {
        val fromConv = runCatching { repo.getConversations() }.getOrNull()
            ?.find { it.peer.id == peerId }
            ?.lastMessage
            ?.let { listOf(it) }
            .orEmpty()
        if (fromConv.isNotEmpty()) return fromConv
        return runCatching { repo.getCachedHistory(peerId) }.getOrDefault(emptyList())
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

    private fun ensureSocketObserver() {
        if (socketJob?.isActive != true) {
            socketJob = viewModelScope.launch {
                repo.events.collect { ev ->
                    runCatching { handleSocketEvent(ev) }
                }
            }
        }
        if (refreshJob?.isActive != true) {
            refreshJob = viewModelScope.launch {
                ActiveChatTracker.refreshRequests.collect { id ->
                    if (id == peerId) refreshLatestMessages()
                }
            }
        }
    }

    private fun handleSocketEvent(ev: ChatEvent) {
        when (ev) {
            is ChatEvent.NewMessage -> handleIncomingDto(ev.message)
            is ChatEvent.Updated -> safeToDomain(ev.message)?.let { m ->
                if (involvesPeer(m)) upsert(m)
            }
            is ChatEvent.Deleted -> {
                val target = _state.value.messages.find { it.id == ev.id } ?: return
                if (!involvesPeer(target)) return
                _state.update { st -> st.copy(messages = st.messages.filter { it.id != ev.id }) }
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
                _state.update { it.copy(peerActivity = if (ev.typing) ev.action else null) }
            }
            is ChatEvent.Presence -> if (ev.userId == peerId) _state.update {
                it.copy(peer = it.peer?.copy(isOnline = ev.online, lastSeenAt = ev.lastSeenAt ?: it.peer.lastSeenAt))
            }
            is ChatEvent.PresenceList -> _state.update {
                it.copy(peer = it.peer?.copy(isOnline = peerId in ev.online))
            }
            else -> Unit
        }
    }

    private fun handleIncomingDto(dto: ChatMessageDto) {
        val m = safeToDomain(dto) ?: return
        if (!involvesPeer(m)) return
        upsert(m)
        if (m.senderId == peerId) markIncomingRead()
    }

    private fun refreshLatestMessages() {
        if (peerId.isBlank()) return
        viewModelScope.launch {
            runCatching { repo.getHistory(peerId, before = null) }
                .onSuccess { list ->
                    list.forEach { upsert(it) }
                }
        }
    }

    private fun involvesPeer(m: ChatMessage): Boolean {
        if (peerId.isBlank()) return false
        return m.senderId == peerId || m.receiverId == peerId
    }

    private fun upsert(msg: ChatMessage) {
        if (msg.id in hiddenIds || (msg.clientId != null && msg.clientId in hiddenIds)) return
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
            repo.sendTyping(peerId, true, "typing")
            stopTypingJob?.cancel()
            stopTypingJob = viewModelScope.launch {
                delay(2_000)
                repo.sendTyping(peerId, false, "typing")
            }
        } else {
            repo.sendTyping(peerId, false, "typing")
        }
    }

    fun setRecording(active: Boolean) {
        if (peerId.isNotBlank()) repo.sendTyping(peerId, active, "recording")
    }

    fun setUploading(active: Boolean) {
        if (peerId.isNotBlank()) repo.sendTyping(peerId, active, "uploading")
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
        val fileUrl = upload?.fileUrl ?: upload?.filePath?.let { uz.vazifa.app.util.MediaUrl.fromFilePath(it) }
        val optimistic = ChatMessage(
            id = clientId,
            senderId = currentUserId,
            receiverId = peerId,
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
                    receiverId = peerId,
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

    fun react(msg: ChatMessage, emoji: String) {
        val current = msg.reactions[currentUserId]
        val next = if (current == emoji) null else emoji
        viewModelScope.launch { runCatching { repo.react(msg.id, next) }.onSuccess { upsert(it) } }
    }

    fun pin(msg: ChatMessage) {
        viewModelScope.launch { runCatching { repo.pin(msg.id) }.onSuccess { upsert(it) } }
    }

    fun delete(msg: ChatMessage) {
        _state.update { st -> st.copy(messages = st.messages.filter { it.id != msg.id }) }
        viewModelScope.launch { runCatching { repo.delete(msg.id) } }
    }

    fun hideForMe(msg: ChatMessage) {
        hiddenIds.add(msg.id)
        msg.clientId?.let { hiddenIds.add(it) }
        _state.update { st -> st.copy(messages = st.messages.filter { it.id != msg.id && it.clientId != msg.id }) }
    }

    suspend fun loadForwardTargets(): List<ChatPeer> =
        runCatching { repo.getContacts() }
            .getOrDefault(emptyList())
            .filter { it.id != peerId && it.id != currentUserId }

    fun forward(msg: ChatMessage, toPeerId: String) {
        viewModelScope.launch {
            runCatching {
                val upload = buildForwardUpload(msg)
                repo.send(
                    receiverId = toPeerId,
                    type = msg.type,
                    body = msg.body,
                    upload = upload,
                    meta = msg.meta,
                    forwardedFrom = msg.id,
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

    override fun onCleared() {
        super.onCleared()
        repo.sendTyping(peerId, false)
    }
}
