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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import uz.vazifa.app.domain.model.ChatMessageStatus
import uz.vazifa.app.domain.model.Conversation
import uz.vazifa.app.presentation.components.VazifaHeaderActions
import uz.vazifa.app.presentation.components.VazifaScreenBox
import uz.vazifa.app.presentation.components.VazifaTabScaffold
import uz.vazifa.app.presentation.components.localized
import uz.vazifa.app.presentation.theme.GlassCard
import uz.vazifa.app.presentation.theme.LiquidBackground
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.presentation.theme.liquidGlassThemed

@Composable
fun ChatListScreen(
    currentUserId: String,
    onOpenChat: (peerId: String, peerName: String) -> Unit,
    onNewChat: () -> Unit,
    viewModel: ChatListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotBlank()) {
            viewModel.start(currentUserId)
            viewModel.clearTabBadge()
        }
    }

    VazifaTabScaffold(
        title = localized("chat_title"),
        actions = {
            VazifaHeaderActions(
                extra = {
                    Box(
                        Modifier
                            .size(36.dp)
                            .liquidGlassThemed(radius = LiquidGlass.RadiusChip)
                            .clickable { onNewChat() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.EditNote,
                            contentDescription = localized("chat_new"),
                            tint = LiquidGlass.Blue,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
            )
        },
    ) { padding ->
        LiquidBackground(Modifier.fillMaxSize()) {
            VazifaScreenBox(padding) {
                Column(Modifier.fillMaxSize()) {
                    ChatSearchField(
                        query = state.query,
                        onChange = viewModel::onQueryChange,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )

                    if (state.query.isNotBlank()) {
                        SearchResults(state, currentUserId, onOpenChat)
                    } else if (state.conversations.isEmpty() && !state.loading) {
                        EmptyChats(Modifier.fillMaxSize(), onNewChat = onNewChat)
                    } else {
                        LazyColumn(
                            Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(state.conversations, key = { it.peer.id }) { conv ->
                                ConversationRow(
                                    conv = conv,
                                    online = conv.peer.isOnline || conv.peer.id in state.onlineIds,
                                    isMineLast = conv.lastMessage?.senderId == currentUserId,
                                    onClick = { onOpenChat(conv.peer.id, conv.peer.fullName) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatSearchField(query: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .liquidGlassThemed(radius = LiquidGlass.RadiusChip),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Search, null, tint = LiquidTheme.textMuted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(localized("chat_search"), color = LiquidTheme.textMuted, fontSize = 15.sp)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onChange,
                    singleLine = true,
                    textStyle = TextStyle(color = LiquidTheme.text, fontSize = 15.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(LiquidGlass.Blue),
                )
            }
        }
    }
}

@Composable
private fun SearchResults(
    state: ChatListUiState,
    currentUserId: String,
    onOpenChat: (String, String) -> Unit,
) {
    if (state.resultPeers.isEmpty() && state.resultMessages.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(localized("chat_search_no_results"), color = LiquidTheme.textMuted)
        }
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (state.resultPeers.isNotEmpty()) {
            item { SectionLabel(localized("chat_contacts")) }
            items(state.resultPeers, key = { "p_" + it.id }) { peer ->
                PeerRow(peer.fullName, peer.position, peer.isOnline || peer.id in state.onlineIds) {
                    onOpenChat(peer.id, peer.fullName)
                }
            }
        }
        if (state.resultMessages.isNotEmpty()) {
            item { SectionLabel(localized("chat_recent")) }
            items(state.resultMessages, key = { "m_" + it.id }) { msg ->
                val peerId = if (msg.senderId == currentUserId) msg.receiverId else msg.senderId
                val resolve = rememberStringResolver()
                PeerRow(msg.body ?: messagePreview(msg, resolve), null, false) {
                    onOpenChat(peerId, "")
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = LiquidTheme.textMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun PeerRow(title: String, subtitle: String?, online: Boolean, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), radius = 18.dp) {
        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChatAvatar(title.ifBlank { "?" }, online, size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = LiquidTheme.text, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                subtitle?.let { Text(it, color = LiquidTheme.textMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
    }
}

@Composable
private fun ConversationRow(
    conv: Conversation,
    online: Boolean,
    isMineLast: Boolean,
    onClick: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), radius = 20.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val displayName = conv.peer.fullName.ifBlank { "…" }
            ChatAvatar(displayName, online, size = 52.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        displayName,
                        color = LiquidTheme.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        ChatFormat.listTime(conv.lastMessage?.createdAt).let {
                            if (it == "—") localized("chat_yesterday") else it
                        },
                        color = LiquidTheme.textMuted,
                        fontSize = 12.sp,
                    )
                }
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isMineLast && conv.lastMessage != null && !conv.lastMessage.isDeleted) {
                        Box(Modifier.padding(end = 4.dp)) {
                            TicksTinted(conv.lastMessage.status)
                        }
                    }
                    Text(
                        messagePreview(conv.lastMessage, rememberStringResolver()),
                        color = LiquidTheme.textMuted,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (conv.unreadCount > 0) {
                        Spacer(Modifier.width(8.dp))
                        UnreadBadge(conv.unreadCount)
                    }
                }
            }
        }
    }
}

@Composable
private fun TicksTinted(status: ChatMessageStatus) {
    val tint = if (status == ChatMessageStatus.READ) Color(0xFF38BDF8) else LiquidTheme.textMuted
    val icon = when (status) {
        ChatMessageStatus.SENDING -> Icons.Default.Schedule
        ChatMessageStatus.SENT -> Icons.Default.Done
        else -> Icons.Default.DoneAll
    }
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
}

@Composable
private fun UnreadBadge(count: Int) {
    Box(
        Modifier
            .height(20.dp)
            .clip(CircleShape)
            .background(LiquidGlass.Blue)
            .padding(horizontal = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (count > 99) "99+" else count.toString(),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun EmptyChats(modifier: Modifier, onNewChat: () -> Unit) {
    Column(
        modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(96.dp).clip(CircleShape).liquidGlassThemed(radius = LiquidGlass.RadiusChip),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.Chat, null, tint = LiquidGlass.Blue, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(localized("chat_empty"), color = LiquidTheme.text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            localized("chat_empty_desc"),
            color = LiquidTheme.textMuted,
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(22.dp))
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(Brush.linearGradient(listOf(LiquidGlass.Blue, LiquidGlass.Cyan)))
                .clickable(onClick = onNewChat)
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.EditNote, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(localized("chat_new"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}
