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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import uz.vazifa.app.data.repository.RoomRepository
import uz.vazifa.app.domain.model.ChatPeer
import uz.vazifa.app.domain.model.ChatRoomType
import uz.vazifa.app.presentation.components.VazifaStackScaffold
import uz.vazifa.app.presentation.components.localized
import uz.vazifa.app.presentation.theme.GlassCard
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.presentation.theme.liquidGlassThemed
import javax.inject.Inject

data class CreateRoomUiState(
    val contacts: List<ChatPeer> = emptyList(),
    val selected: Set<String> = emptySet(),
    val title: String = "",
    val description: String = "",
    val query: String = "",
    val creating: Boolean = false,
)

@HiltViewModel
class CreateRoomViewModel @Inject constructor(
    private val chat: ChatRepository,
    private val rooms: RoomRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(CreateRoomUiState())
    val state = _state.asStateFlow()

    private var allContacts: List<ChatPeer> = emptyList()
    private var excludeId = ""
    private var loaded = false

    fun start(currentUserId: String) {
        excludeId = currentUserId
        if (loaded) return
        loaded = true
        chat.connect()
        viewModelScope.launch {
            runCatching { chat.loadAliases(currentUserId) }
            runCatching { chat.getContacts() }.onSuccess {
                allContacts = it.filter { c -> c.id != excludeId }
                recompute()
            }
        }
    }

    fun onTitle(v: String) = _state.update { it.copy(title = v) }
    fun onDescription(v: String) = _state.update { it.copy(description = v) }
    fun onQuery(v: String) { _state.update { it.copy(query = v) }; recompute() }

    fun toggle(id: String) = _state.update {
        it.copy(selected = if (id in it.selected) it.selected - id else it.selected + id)
    }

    private fun recompute() {
        val q = _state.value.query
        _state.update {
            it.copy(contacts = allContacts.filter { c ->
                q.isBlank() || c.displayName.contains(q, true) || c.fullName.contains(q, true)
            }.sortedBy { c -> c.displayName.lowercase() })
        }
    }

    fun create(type: ChatRoomType, onCreated: (String) -> Unit) {
        val st = _state.value
        if (st.title.isBlank() || st.creating) return
        _state.update { it.copy(creating = true) }
        viewModelScope.launch {
            runCatching {
                rooms.create(
                    type = type,
                    title = st.title.trim(),
                    description = st.description.trim().ifBlank { null },
                    memberIds = st.selected.toList(),
                )
            }.onSuccess { room ->
                _state.update { it.copy(creating = false) }
                onCreated(room.id)
            }.onFailure {
                _state.update { it.copy(creating = false) }
            }
        }
    }
}

@Composable
fun CreateRoomScreen(
    currentUserId: String,
    roomTypeKey: String,
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    viewModel: CreateRoomViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val type = ChatRoomType.from(roomTypeKey)
    val isChannel = type == ChatRoomType.CHANNEL

    LaunchedEffect(currentUserId) { viewModel.start(currentUserId) }

    val titleKey = if (isChannel) "chat_new_channel" else "chat_new_group"
    val canCreate = state.title.isNotBlank() && !state.creating

    VazifaStackScaffold(
        title = localized(titleKey),
        onBack = onBack,
        actions = {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (canCreate) LiquidGlass.Blue else LiquidTheme.textMuted.copy(alpha = 0.3f))
                    .clickable(enabled = canCreate) { viewModel.create(type, onCreated) },
                contentAlignment = Alignment.Center,
            ) {
                if (state.creating) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Icon(Icons.Default.Check, localized("com_save"), tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            FieldBox(
                value = state.title,
                onChange = viewModel::onTitle,
                hint = if (isChannel) localized("chat_channel_name") else localized("chat_group_name"),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            FieldBox(
                value = state.description,
                onChange = viewModel::onDescription,
                hint = localized("chat_room_description"),
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
            )

            Text(
                localized(if (isChannel) "chat_add_subscribers" else "chat_add_members"),
                color = LiquidTheme.textMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )

            FieldBox(
                value = state.query,
                onChange = viewModel::onQuery,
                hint = localized("chat_select_contact"),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(state.contacts, key = { it.id }) { peer ->
                    val checked = peer.id in state.selected
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.toggle(peer.id) },
                        radius = 18.dp,
                    ) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            ChatAvatar(peer.displayName, peer.isOnline, size = 44.dp, avatarUrl = peer.avatarUrl)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(peer.displayName, color = LiquidTheme.text, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                peer.position?.let { Text(it, color = LiquidTheme.textMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            }
                            Box(
                                Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (checked) LiquidGlass.Blue else Color.Transparent)
                                    .then(if (!checked) Modifier.liquidGlassThemed(radius = 50.dp) else Modifier),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (checked) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldBox(
    value: String,
    onChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(46.dp)
            .liquidGlassThemed(radius = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) Text(hint, color = LiquidTheme.textMuted, fontSize = 15.sp)
                BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    singleLine = true,
                    textStyle = TextStyle(color = LiquidTheme.text, fontSize = 15.sp),
                    cursorBrush = SolidColor(LiquidGlass.Blue),
                )
            }
        }
    }
}
