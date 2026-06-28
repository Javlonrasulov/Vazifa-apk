package uz.vazifa.app.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.vazifa.app.data.repository.ChatRepository
import uz.vazifa.app.domain.model.ChatPeer
import uz.vazifa.app.presentation.components.VazifaStackScaffold
import uz.vazifa.app.presentation.components.localized
import uz.vazifa.app.presentation.theme.GlassCard
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.presentation.theme.liquidGlassThemed
import javax.inject.Inject

@HiltViewModel
class ChatContactsViewModel @Inject constructor(
    private val repo: ChatRepository,
) : ViewModel() {
    private val _contacts = MutableStateFlow<List<ChatPeer>>(emptyList())
    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _state = MutableStateFlow<List<ChatPeer>>(emptyList())
    val state = _state.asStateFlow()

    private var excludeId: String = ""
    private var loaded = false
    private var onlineIds: Set<String> = emptySet()

    fun start(currentUserId: String) {
        excludeId = currentUserId
        if (loaded) {
            recompute()
            return
        }
        loaded = true
        repo.connect()
        observePresence()
        viewModelScope.launch {
            runCatching { repo.loadAliases(currentUserId) }
            runCatching { repo.getContacts() }.onSuccess {
                _contacts.value = it
                recompute()
            }
        }
    }

    private fun observePresence() {
        viewModelScope.launch {
            repo.events.collect { ev ->
                when (ev) {
                    is uz.vazifa.app.data.remote.ChatEvent.PresenceList -> {
                        onlineIds = ev.online.toSet()
                        recompute()
                    }
                    is uz.vazifa.app.data.remote.ChatEvent.Presence -> {
                        onlineIds = if (ev.online) onlineIds + ev.userId else onlineIds - ev.userId
                        recompute()
                    }
                    else -> Unit
                }
            }
        }
    }

    fun onQuery(q: String) {
        _query.value = q
        recompute()
    }

    private fun recompute() {
        val q = _query.value
        _state.value = _contacts.value
            .filter { p ->
                p.id != excludeId && (
                    q.isBlank() ||
                        p.displayName.contains(q, ignoreCase = true) ||
                        p.fullName.contains(q, ignoreCase = true) ||
                        p.position?.contains(q, true) == true
                    )
            }
            .map { if (it.id in onlineIds) it.copy(isOnline = true) else it }
            .sortedWith(compareByDescending<ChatPeer> { it.isOnline }.thenBy { it.displayName.lowercase() })
    }
}

@Composable
fun NewChatScreen(
    currentUserId: String,
    onBack: () -> Unit,
    onPick: (peerId: String, peerName: String) -> Unit,
    titleKey: String = "chat_new",
    viewModel: ChatContactsViewModel = hiltViewModel(),
) {
    val contacts by viewModel.state.collectAsState()
    val query by viewModel.query.collectAsState()
    val lastSeenLabels = rememberLastSeenLabels()

    LaunchedEffect(currentUserId) { viewModel.start(currentUserId) }

    VazifaStackScaffold(title = localized(titleKey), onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(44.dp)
                    .liquidGlassThemed(radius = LiquidGlass.RadiusChip),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, null, tint = LiquidTheme.textMuted, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) Text(localized("chat_select_contact"), color = LiquidTheme.textMuted, fontSize = 15.sp)
                        BasicTextField(
                            value = query,
                            onValueChange = viewModel::onQuery,
                            singleLine = true,
                            textStyle = TextStyle(color = LiquidTheme.text, fontSize = 15.sp),
                            cursorBrush = SolidColor(LiquidGlass.Blue),
                        )
                    }
                }
            }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(contacts, key = { it.id }) { peer ->
                    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onPick(peer.id, peer.fullName) }, radius = 18.dp) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            ChatAvatar(peer.displayName, peer.isOnline, size = 46.dp, avatarUrl = peer.avatarUrl)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(peer.displayName, color = LiquidTheme.text, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                val subtitle = when {
                                    peer.isOnline -> localized("chat_online")
                                    peer.lastSeenAt != null -> ChatFormat.lastSeen(peer.lastSeenAt, lastSeenLabels)
                                    else -> peer.position ?: peer.department ?: ""
                                }
                                Text(
                                    subtitle,
                                    color = if (peer.isOnline) LiquidGlass.Blue else LiquidTheme.textMuted,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
