package uz.vazifa.app.presentation.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import uz.vazifa.app.domain.model.ChatMessageStatus
import uz.vazifa.app.domain.model.ChatRoom
import uz.vazifa.app.domain.model.Conversation
import uz.vazifa.app.presentation.components.VazifaHeaderActions
import uz.vazifa.app.presentation.components.CountBadgeLabel
import uz.vazifa.app.presentation.components.formatBadgeCount
import uz.vazifa.app.presentation.components.localized
import uz.vazifa.app.presentation.theme.GlassCard
import uz.vazifa.app.presentation.theme.LiquidBackground
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.presentation.theme.liquidGlassThemed

private enum class ChatFolder(val labelKey: String) {
    ALL("chat_tab_all"),
    PRIVATE("chat_tab_private"),
    GROUPS("chat_tab_groups"),
    CHANNELS("chat_tab_channels"),
}

@Composable
fun ChatListScreen(
    currentUserId: String,
    currentUserName: String,
    currentUserAvatar: String?,
    onOpenChat: (peerId: String, peerName: String) -> Unit,
    onNewChat: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenProfile: () -> Unit,
    onNewGroup: () -> Unit,
    onNewChannel: () -> Unit,
    onOpenRoom: (roomId: String) -> Unit,
    onComingSoon: () -> Unit,
    viewModel: ChatListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var drawerOpen by remember { mutableStateOf(false) }
    var appActive by remember { mutableStateOf(true) }
    val context = LocalContext.current
    var avatarOverride by remember { mutableStateOf<String?>(null) }
    var uploadingAvatar by remember { mutableStateOf(false) }
    var showAvatarViewer by remember { mutableStateOf(false) }
    val effectiveAvatar = avatarOverride ?: currentUserAvatar
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            uploadingAvatar = true
            uz.vazifa.app.util.ChatFiles.copyToCache(context, it)?.let { file ->
                viewModel.uploadAvatar(file) { url ->
                    uploadingAvatar = false
                    if (url != null) avatarOverride = url
                }
            } ?: run { uploadingAvatar = false }
        }
    }

    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotBlank()) {
            viewModel.start(currentUserId)
            viewModel.clearTabBadge()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, currentUserId) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    appActive = true
                    if (currentUserId.isNotBlank()) {
                        viewModel.reconnect()
                        viewModel.refresh()
                    }
                }
                Lifecycle.Event.ON_PAUSE -> appActive = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val drawerOnline = state.connected || appActive

    Box(Modifier.fillMaxSize()) {
        ChatListContent(
            state = state,
            currentUserId = currentUserId,
            onMenu = { drawerOpen = true },
            onNewChat = onNewChat,
            onQueryChange = viewModel::onQueryChange,
            onRememberPeer = viewModel::rememberPeer,
            onOpenChat = { peerId, name ->
                onOpenChat(peerId, name)
            },
            onOpenRoom = { roomId ->
                viewModel.markRoomRead(roomId)
                onOpenRoom(roomId)
            },
            onNewGroup = onNewGroup,
            onNewChannel = onNewChannel,
        )
    }

    if (drawerOpen) {
        Dialog(
            onDismissRequest = { drawerOpen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Transparent,
            ) {
                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.32f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { drawerOpen = false },
                            ),
                    )
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInHorizontally(tween(320)) { -it },
                        exit = slideOutHorizontally(tween(280)) { -it },
                        modifier = Modifier.align(Alignment.CenterStart),
                    ) {
                        ChatDrawerContent(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(312.dp),
                            userName = currentUserName,
                            userSubtitle = if (drawerOnline) localized("chat_online") else localized("chat_offline"),
                            userAvatarUrl = effectiveAvatar,
                            isOnline = drawerOnline,
                            isOpen = drawerOpen,
                            uploadingAvatar = uploadingAvatar,
                            onAvatarClick = {
                                if (!effectiveAvatar.isNullOrBlank()) showAvatarViewer = true
                                else avatarPicker.launch("image/*")
                            },
                            onAction = { action ->
                                drawerOpen = false
                                when (action) {
                                    ChatDrawerAction.PROFILE, ChatDrawerAction.SETTINGS -> onOpenProfile()
                                    ChatDrawerAction.CONTACTS -> onOpenContacts()
                                    ChatDrawerAction.NEW_GROUP -> onNewGroup()
                                    ChatDrawerAction.NEW_CHANNEL -> onNewChannel()
                                    ChatDrawerAction.SAVED -> onComingSoon()
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    if (showAvatarViewer && !effectiveAvatar.isNullOrBlank()) {
        uz.vazifa.app.presentation.components.AvatarViewerDialog(
            avatarUrl = effectiveAvatar,
            name = currentUserName,
            onDismiss = { showAvatarViewer = false },
            onChangePhoto = { avatarPicker.launch("image/*") },
            onDeletePhoto = {
                uploadingAvatar = true
                viewModel.deleteAvatar { url ->
                    uploadingAvatar = false
                    avatarOverride = url
                }
            },
        )
    }
}

@Composable
private fun ChatListContent(
    state: ChatListUiState,
    currentUserId: String,
    onMenu: () -> Unit,
    onNewChat: () -> Unit,
    onQueryChange: (String) -> Unit,
    onRememberPeer: (String, String) -> Unit,
    onOpenChat: (String, String) -> Unit,
    onOpenRoom: (String) -> Unit,
    onNewGroup: () -> Unit,
    onNewChannel: () -> Unit,
) {
    val folders = remember { ChatFolder.entries.toList() }
    val pagerState = rememberPagerState(pageCount = { folders.size })
    val scope = rememberCoroutineScope()

    val unreadTotal = state.conversations.count { it.unreadCount > 0 }
    val badges = mapOf(
        ChatFolder.ALL to unreadTotal,
        ChatFolder.PRIVATE to unreadTotal,
        ChatFolder.GROUPS to state.groups.count { it.unreadCount > 0 },
        ChatFolder.CHANNELS to state.channels.count { it.unreadCount > 0 },
    )

    LiquidBackground(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ChatTopBar(
                onMenu = onMenu,
                onNewChat = onNewChat,
            )

            ChatSearchField(
                query = state.query,
                onChange = onQueryChange,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            )

            if (state.query.isNotBlank()) {
                SearchResults(state, currentUserId, onRememberPeer, onOpenChat)
                return@Column
            }

            ChatTabsRow(
                folders = folders,
                selected = folders[pagerState.currentPage],
                badges = badges,
                onSelect = { folder ->
                    scope.launch { pagerState.animateScrollToPage(folders.indexOf(folder)) }
                },
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                when (folders[page]) {
                    ChatFolder.ALL, ChatFolder.PRIVATE -> ConversationsList(
                        conversations = state.conversations,
                        onlineIds = state.onlineIds,
                        activityPeers = state.activityPeers,
                        currentUserId = currentUserId,
                        loading = state.loading,
                        onOpenChat = onOpenChat,
                        onRememberPeer = onRememberPeer,
                        onNewChat = onNewChat,
                    )
                    ChatFolder.GROUPS -> RoomsList(
                        rooms = state.groups,
                        emptyIcon = Icons.Default.Group,
                        emptyText = localized("chat_groups_empty"),
                        actionText = localized("chat_new_group"),
                        currentUserId = currentUserId,
                        onCreate = onNewGroup,
                        onOpenRoom = onOpenRoom,
                    )
                    ChatFolder.CHANNELS -> RoomsList(
                        rooms = state.channels,
                        emptyIcon = Icons.Default.Campaign,
                        emptyText = localized("chat_channels_empty"),
                        actionText = localized("chat_new_channel"),
                        currentUserId = currentUserId,
                        onCreate = onNewChannel,
                        onOpenRoom = onOpenRoom,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatTopBar(
    onMenu: () -> Unit,
    onNewChat: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(LiquidTheme.bgMid.copy(alpha = 0.92f)),
    ) {
        Column(Modifier.statusBarsPadding()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .liquidGlassThemed(radius = LiquidGlass.RadiusChip)
                        .clickable(onClick = onMenu),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Menu, localized("chat_menu"), tint = LiquidGlass.Blue, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    localized("chat_title"),
                    color = LiquidTheme.text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    modifier = Modifier.weight(1f),
                )
                VazifaHeaderActions(
                    extra = {
                        Box(
                            Modifier
                                .size(36.dp)
                                .liquidGlassThemed(radius = LiquidGlass.RadiusChip)
                                .clickable(onClick = onNewChat),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.EditNote, localized("chat_new"), tint = LiquidGlass.Blue, modifier = Modifier.size(20.dp))
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ChatTabsRow(
    folders: List<ChatFolder>,
    selected: ChatFolder,
    badges: Map<ChatFolder, Int>,
    onSelect: (ChatFolder) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        folders.forEach { folder ->
            ChatTab(
                label = localized(folder.labelKey),
                selected = folder == selected,
                badge = badges[folder] ?: 0,
                onClick = { onSelect(folder) },
            )
        }
    }
}

@Composable
private fun ChatTab(label: String, selected: Boolean, badge: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) LiquidGlass.Blue
                else LiquidTheme.text.copy(alpha = 0.07f),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = if (selected) Color.White else LiquidTheme.textMuted,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp,
        )
        if (badge > 0) {
            Spacer(Modifier.width(7.dp))
            val badgeText = formatBadgeCount(badge)
            val wide = badgeText.length >= 2
            Box(
                Modifier
                    .defaultMinSize(minWidth = if (wide) 22.dp else 18.dp, minHeight = 18.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (selected) Color.White.copy(alpha = 0.28f)
                        else LiquidTheme.textMuted.copy(alpha = 0.35f),
                    )
                    .padding(horizontal = if (wide) 5.dp else 0.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center,
            ) {
                CountBadgeLabel(text = badgeText, fontSize = if (wide) 10.sp else 11.sp)
            }
        }
    }
}

@Composable
private fun ConversationsList(
    conversations: List<Conversation>,
    onlineIds: Set<String>,
    activityPeers: Map<String, String>,
    currentUserId: String,
    loading: Boolean,
    onOpenChat: (String, String) -> Unit,
    onRememberPeer: (String, String) -> Unit,
    onNewChat: () -> Unit,
) {
    if (conversations.isEmpty() && !loading) {
        EmptyChats(Modifier.fillMaxSize(), onNewChat = onNewChat)
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(conversations, key = { it.peer.id }) { conv ->
            ConversationRow(
                conv = conv,
                online = conv.peer.isOnline || conv.peer.id in onlineIds,
                activity = activityPeers[conv.peer.id],
                isMineLast = conv.lastMessage?.senderId == currentUserId,
                onClick = {
                    onRememberPeer(conv.peer.id, conv.peer.fullName)
                    onOpenChat(conv.peer.id, conv.peer.fullName)
                },
            )
        }
    }
}

@Composable
private fun RoomsList(
    rooms: List<ChatRoom>,
    emptyIcon: androidx.compose.ui.graphics.vector.ImageVector,
    emptyText: String,
    actionText: String,
    currentUserId: String,
    onCreate: () -> Unit,
    onOpenRoom: (String) -> Unit,
) {
    if (rooms.isEmpty()) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier.size(88.dp).clip(CircleShape).liquidGlassThemed(radius = LiquidGlass.RadiusChip),
                contentAlignment = Alignment.Center,
            ) {
                Icon(emptyIcon, null, tint = LiquidGlass.Blue, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(emptyText, color = LiquidTheme.text, fontWeight = FontWeight.Bold, fontSize = 17.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(22.dp))
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Brush.linearGradient(listOf(LiquidGlass.Blue, LiquidGlass.Cyan)))
                    .clickable(onClick = onCreate)
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.EditNote, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(actionText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(rooms, key = { it.id }) { room ->
            RoomRow(room = room, currentUserId = currentUserId, onClick = { onOpenRoom(room.id) })
        }
    }
}

@Composable
private fun RoomRow(room: ChatRoom, currentUserId: String, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), radius = 20.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChatAvatar(room.title, online = false, size = 52.dp, showPresence = false, avatarUrl = room.avatarUrl)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        room.title,
                        color = LiquidTheme.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (room.isVerified) {
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.Verified, null, tint = LiquidGlass.Cyan, modifier = Modifier.size(15.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        ChatFormat.listTime(room.lastMessage?.createdAt),
                        color = LiquidTheme.textMuted,
                        fontSize = 12.sp,
                    )
                }
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val preview = room.lastMessage?.let { lm ->
                        val prefix = lm.senderName?.takeIf { it.isNotBlank() && lm.senderId != currentUserId }
                            ?.let { "${it.substringBefore(' ')}: " } ?: ""
                        prefix + (lm.body?.takeIf { it.isNotBlank() } ?: messagePreviewType(lm.type, rememberStringResolver()))
                    } ?: room.description?.takeIf { it.isNotBlank() } ?: localized("chat_members_count").replace("%d", room.memberCount.toString())
                    Text(
                        preview,
                        color = LiquidTheme.textMuted,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (room.unreadCount > 0) {
                        Spacer(Modifier.width(8.dp))
                        UnreadBadge(room.unreadCount)
                    }
                }
            }
        }
    }
}

@Composable
private fun activityLabel(activity: String): String = when (activity) {
    "recording" -> localized("chat_recording")
    "uploading" -> localized("chat_sending_file")
    else -> localized("chat_typing")
}

@Composable
private fun ComingSoonPage(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(88.dp).clip(CircleShape).liquidGlassThemed(radius = LiquidGlass.RadiusChip),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = LiquidGlass.Blue, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(text, color = LiquidTheme.text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Spacer(Modifier.height(6.dp))
        Text(localized("chat_feature_soon"), color = LiquidTheme.textMuted, fontSize = 14.sp)
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
                    Text(localized("chat_search_chats"), color = LiquidTheme.textMuted, fontSize = 15.sp)
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
    onRememberPeer: (String, String) -> Unit,
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
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (state.resultPeers.isNotEmpty()) {
            item { SectionLabel(localized("chat_contacts")) }
            items(state.resultPeers, key = { "p_" + it.id }) { peer ->
                PeerRow(peer.displayName, peer.position, peer.isOnline || peer.id in state.onlineIds, avatarUrl = peer.avatarUrl) {
                    onRememberPeer(peer.id, peer.fullName)
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
private fun PeerRow(title: String, subtitle: String?, online: Boolean, avatarUrl: String? = null, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), radius = 18.dp) {
        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChatAvatar(title.ifBlank { "?" }, online, size = 44.dp, avatarUrl = avatarUrl)
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
    activity: String?,
    isMineLast: Boolean,
    onClick: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), radius = 20.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChatAvatar(conv.peer.displayName, online, size = 52.dp, avatarUrl = conv.peer.avatarUrl)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        conv.peer.displayName,
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
                    if (activity == null && isMineLast && conv.lastMessage != null && !conv.lastMessage.isDeleted) {
                        Box(Modifier.padding(end = 4.dp)) {
                            TicksTinted(conv.lastMessage.status)
                        }
                    }
                    if (activity != null) {
                        Text(
                            activityLabel(activity),
                            color = LiquidGlass.Blue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Text(
                            messagePreview(conv.lastMessage, rememberStringResolver()),
                            color = LiquidTheme.textMuted,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
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
    ChatUnreadBadge(count = count)
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
            textAlign = TextAlign.Center,
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
