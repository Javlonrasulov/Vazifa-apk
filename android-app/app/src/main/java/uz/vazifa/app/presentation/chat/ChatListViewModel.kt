package uz.vazifa.app.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.vazifa.app.data.remote.ChatEvent
import uz.vazifa.app.data.repository.ChatRepository
import uz.vazifa.app.data.repository.ChatUnreadRepository
import uz.vazifa.app.data.repository.RoomRepository
import uz.vazifa.app.data.repository.toDomain
import uz.vazifa.app.domain.model.ChatMessage
import uz.vazifa.app.domain.model.ChatPeer
import uz.vazifa.app.domain.model.ChatRoom
import uz.vazifa.app.domain.model.ChatRoomType
import uz.vazifa.app.domain.model.Conversation
import javax.inject.Inject

data class ChatListUiState(
    val conversations: List<Conversation> = emptyList(),
    val loading: Boolean = true,
    val query: String = "",
    val searching: Boolean = false,
    val resultPeers: List<ChatPeer> = emptyList(),
    val resultMessages: List<ChatMessage> = emptyList(),
    val onlineIds: Set<String> = emptySet(),
    val connected: Boolean = false,
    /** peerId -> activity ("typing"|"recording"|"uploading") */
    val activityPeers: Map<String, String> = emptyMap(),
    val rooms: List<ChatRoom> = emptyList(),
) {
    val groups: List<ChatRoom> get() = rooms.filter { it.type == ChatRoomType.GROUP }
    val channels: List<ChatRoom> get() = rooms.filter { it.type == ChatRoomType.CHANNEL }
}

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val repo: ChatRepository,
    private val rooms: RoomRepository,
    private val chatUnread: ChatUnreadRepository,
    private val auth: uz.vazifa.app.data.repository.AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ChatListUiState())
    val state = _state.asStateFlow()

    private var currentUserId: String = ""

    private var socketObserving = false

    fun start(userId: String) {
        if (userId.isBlank()) return
        currentUserId = userId
        viewModelScope.launch {
            auth.ensureSessionForApi()
            repo.connect()
            repo.ping()
            if (!socketObserving) {
                socketObserving = true
                observeSocket()
            }
            runCatching { repo.loadAliases(userId) }
            load()
            loadRooms()
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            auth.ensureSessionForApi()
            runCatching { repo.getConversations() }
                .onSuccess { list ->
                    _state.update { st ->
                        st.copy(conversations = mergeConversations(st.conversations, list), loading = false)
                    }
                }
                .onFailure { _state.update { it.copy(loading = false) } }
        }
    }

    fun loadRooms() {
        viewModelScope.launch {
            runCatching { rooms.list() }.onSuccess { list ->
                _state.update { it.copy(rooms = list) }
            }
        }
    }

    fun rememberPeer(peerId: String, fullName: String) {
        if (peerId.isBlank() || fullName.isBlank()) return
        repo.rememberPeer(ChatPeer(id = peerId, fullName = fullName))
        _state.update { st ->
            st.copy(conversations = st.conversations.map { conv ->
                if (conv.peer.id == peerId && conv.peer.fullName.isBlank()) {
                    conv.copy(peer = conv.peer.copy(fullName = fullName))
                } else conv
            })
        }
    }

    fun refresh() {
        if (currentUserId.isBlank()) return
        viewModelScope.launch {
            runCatching { chatUnread.setCount(repo.unreadCount()) }
            load()
            loadRooms()
        }
    }

    fun reconnect() {
        repo.reconnect()
        repo.ping()
    }

    private fun mergeConversations(
        local: List<Conversation>,
        server: List<Conversation>,
    ): List<Conversation> {
        val serverIds = server.map { it.peer.id }.toSet()
        val localOnly = local.filter { it.peer.id !in serverIds }
        val localMap = local.associateBy { it.peer.id }
        val merged = server.map { serverConv ->
            val localConv = localMap[serverConv.peer.id] ?: return@map serverConv
            val name = serverConv.peer.fullName.takeIf { it.isNotBlank() }
                ?: localConv.peer.fullName.takeIf { it.isNotBlank() }
                ?: repo.knownPeer(serverConv.peer.id)?.fullName.orEmpty()
            val localTime = localConv.lastMessage?.createdAt.orEmpty()
            val serverTime = serverConv.lastMessage?.createdAt.orEmpty()
            serverConv.copy(
                peer = serverConv.peer.copy(
                    fullName = name,
                    position = serverConv.peer.position ?: localConv.peer.position,
                ),
                lastMessage = if (localTime > serverTime) localConv.lastMessage else serverConv.lastMessage,
            )
        }
        return (localOnly.map { enrichLocalConversation(it) } + merged)
            .sortedByDescending { it.lastMessage?.createdAt.orEmpty() }
    }

    private fun enrichLocalConversation(conv: Conversation): Conversation {
        val known = repo.knownPeer(conv.peer.id) ?: return conv
        if (conv.peer.fullName.isNotBlank()) return conv
        return conv.copy(peer = conv.peer.copy(
            fullName = known.fullName,
            position = conv.peer.position ?: known.position,
        ))
    }

    private fun peerForMessage(peerId: String, onlineIds: Set<String>): ChatPeer {
        val known = repo.knownPeer(peerId)
        return known?.copy(isOnline = peerId in onlineIds)
            ?: ChatPeer(id = peerId, fullName = "", isOnline = peerId in onlineIds)
    }

    private fun observeSocket() {
        viewModelScope.launch {
            repo.connected.collect { c -> _state.update { it.copy(connected = c) } }
        }
        viewModelScope.launch {
            repo.events.collect { ev ->
                when (ev) {
                    is ChatEvent.NewMessage -> onNewMessage(ev.message.toDomain())
                    is ChatEvent.Presence -> _state.update {
                        val set = it.onlineIds.toMutableSet()
                        if (ev.online) set.add(ev.userId) else set.remove(ev.userId)
                        it.copy(onlineIds = set)
                    }
                    is ChatEvent.PresenceList -> _state.update { it.copy(onlineIds = ev.online.toSet()) }
                    is ChatEvent.Typing -> onPeerActivity(ev.userId, if (ev.typing) ev.action else null)
                    is ChatEvent.RoomCreated -> loadRooms()
                    is ChatEvent.RoomNewMessage -> onRoomMessage(ev.message.roomId)
                    is ChatEvent.Read -> Unit
                    else -> Unit
                }
            }
        }
    }

    private val activityTimers = mutableMapOf<String, kotlinx.coroutines.Job>()

    private fun onPeerActivity(peerId: String, activity: String?) {
        activityTimers.remove(peerId)?.cancel()
        _state.update { st ->
            val map = st.activityPeers.toMutableMap()
            if (activity == null) map.remove(peerId) else map[peerId] = activity
            st.copy(activityPeers = map)
        }
        if (activity != null) {
            activityTimers[peerId] = viewModelScope.launch {
                kotlinx.coroutines.delay(5_000)
                _state.update { st ->
                    val map = st.activityPeers.toMutableMap()
                    map.remove(peerId)
                    st.copy(activityPeers = map)
                }
            }
        }
    }

    private fun onNewMessage(msg: ChatMessage) {
        val peerId = if (msg.senderId == currentUserId) msg.receiverId else msg.senderId
        val incoming = msg.senderId != currentUserId
        if (incoming) onPeerActivity(peerId, null)
        var peerUnknown = false
        _state.update { st ->
            val existing = st.conversations.firstOrNull { it.peer.id == peerId }
            val rest = st.conversations.filter { it.peer.id != peerId }
            val updated = if (existing != null) {
                val peer = existing.peer.let { p ->
                    if (p.fullName.isBlank()) peerForMessage(peerId, st.onlineIds) else p
                }
                existing.copy(
                    peer = peer,
                    lastMessage = msg,
                    unreadCount = if (incoming) existing.unreadCount + 1 else existing.unreadCount,
                )
            } else {
                peerUnknown = true
                Conversation(
                    peer = peerForMessage(peerId, st.onlineIds),
                    lastMessage = msg,
                    unreadCount = if (incoming) 1 else 0,
                )
            }
            st.copy(conversations = listOf(updated) + rest)
        }
        if (peerUnknown && repo.knownPeer(peerId) == null) load()
    }

    private fun onRoomMessage(roomId: String) {
        loadRooms()
    }

    fun markPeerRead(peerId: String) {
        _state.update { st ->
            st.copy(conversations = st.conversations.map {
                if (it.peer.id == peerId) it.copy(unreadCount = 0) else it
            })
        }
        viewModelScope.launch { runCatching { repo.markRead(peerId) } }
    }

    fun markRoomRead(roomId: String) {
        _state.update { st ->
            st.copy(rooms = st.rooms.map { if (it.id == roomId) it.copy(unreadCount = 0) else it })
        }
        viewModelScope.launch { runCatching { rooms.markRead(roomId) } }
    }

    fun uploadAvatar(file: java.io.File, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val url = runCatching { auth.uploadAvatar(file).avatarUrl }.getOrNull()
            onResult(url)
        }
    }

    fun deleteAvatar(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val url = runCatching { auth.deleteAvatar().avatarUrl }.getOrNull()
            onResult(url)
        }
    }

    fun onQueryChange(q: String) {
        _state.update { it.copy(query = q) }
        if (q.isBlank()) {
            _state.update { it.copy(searching = false, resultPeers = emptyList(), resultMessages = emptyList()) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(searching = true) }
            runCatching { repo.search(q) }.onSuccess { (messages, peers) ->
                _state.update { it.copy(resultMessages = messages, resultPeers = peers) }
            }
        }
    }

    fun clearTabBadge() {
        viewModelScope.launch { chatUnread.clear() }
    }
}
