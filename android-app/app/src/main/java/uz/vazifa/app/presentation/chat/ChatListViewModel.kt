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
import uz.vazifa.app.data.repository.toDomain
import uz.vazifa.app.domain.model.ChatMessage
import uz.vazifa.app.domain.model.ChatPeer
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
)

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val repo: ChatRepository,
    private val chatUnread: ChatUnreadRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ChatListUiState())
    val state = _state.asStateFlow()

    private var currentUserId: String = ""

    fun start(userId: String) {
        if (currentUserId == userId) return
        currentUserId = userId
        repo.connect()
        observeSocket()
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            runCatching { repo.getConversations() }
                .onSuccess { list -> _state.update { it.copy(conversations = list, loading = false) } }
                .onFailure { _state.update { it.copy(loading = false) } }
        }
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
                    is ChatEvent.Read -> Unit
                    else -> Unit
                }
            }
        }
    }

    private fun onNewMessage(msg: ChatMessage) {
        val peerId = if (msg.senderId == currentUserId) msg.receiverId else msg.senderId
        val incoming = msg.senderId != currentUserId
        var peerUnknown = false
        _state.update { st ->
            val existing = st.conversations.firstOrNull { it.peer.id == peerId }
            val rest = st.conversations.filter { it.peer.id != peerId }
            val updated = if (existing != null) {
                existing.copy(
                    lastMessage = msg,
                    unreadCount = if (incoming) existing.unreadCount + 1 else existing.unreadCount,
                )
            } else {
                peerUnknown = true
                Conversation(
                    peer = ChatPeer(id = peerId, fullName = "", isOnline = peerId in st.onlineIds),
                    lastMessage = msg,
                    unreadCount = if (incoming) 1 else 0,
                )
            }
            st.copy(conversations = listOf(updated) + rest)
        }
        if (peerUnknown) load()
    }

    fun clearTabBadge() {
        viewModelScope.launch { chatUnread.clear() }
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

    fun markPeerRead(peerId: String) {
        _state.update { st ->
            st.copy(conversations = st.conversations.map {
                if (it.peer.id == peerId) it.copy(unreadCount = 0) else it
            })
        }
    }
}
