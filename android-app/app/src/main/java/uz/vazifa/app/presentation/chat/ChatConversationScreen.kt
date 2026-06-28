package uz.vazifa.app.presentation.chat

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import uz.vazifa.app.domain.model.ChatMessage
import uz.vazifa.app.domain.model.ChatMessageMeta
import uz.vazifa.app.domain.model.ChatMessageType
import uz.vazifa.app.domain.model.ChatPeer
import uz.vazifa.app.presentation.components.localized
import uz.vazifa.app.presentation.theme.GlassCard
import uz.vazifa.app.presentation.theme.LiquidBackground
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidGlassDropdownItem
import uz.vazifa.app.presentation.theme.LiquidGlassDropdownMenu
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.presentation.theme.liquidGlassThemed
import uz.vazifa.app.util.ChatAudioPlayer
import uz.vazifa.app.util.ChatFiles
import uz.vazifa.app.util.ImageCompress
import uz.vazifa.app.util.MediaUrl
import uz.vazifa.app.util.VoiceRecorder
import kotlin.math.abs

@Composable
fun ChatConversationScreen(
    peerId: String,
    peerName: String,
    currentUserId: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(key = peerId),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sendFailedText = localized("chat_send_failed")

    LaunchedEffect(peerId, currentUserId) {
        if (peerId.isNotBlank() && currentUserId.isNotBlank()) {
            viewModel.start(peerId, currentUserId, ChatPeer(id = peerId, fullName = peerName))
        }
    }

    var actionTarget by remember { mutableStateOf<ChatMessage?>(null) }
    var deleteTarget by remember { mutableStateOf<ChatMessage?>(null) }
    var forwardTarget by remember { mutableStateOf<ChatMessage?>(null) }
    var forwardPeers by remember { mutableStateOf<List<ChatPeer>>(emptyList()) }
    var forwardLoading by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    val strings = rememberStringResolver()
    val copiedText = localized("chat_copied")
    val forwardText = localized("chat_forward")

    LiquidBackground(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding(),
        ) {
            ChatHeader(
                peer = state.peer ?: ChatPeer(id = peerId, fullName = peerName),
                activity = state.peerActivity,
                onBack = onBack,
                onRename = { showRename = true },
            )

            state.messages.lastOrNull { it.isPinned && !it.isDeleted }?.let { pinned ->
                PinnedMessageBanner(
                    preview = messagePreview(pinned, strings),
                    onUnpin = { viewModel.pin(pinned) },
                )
            }

            MessageList(
                state = state,
                isMine = viewModel::isMine,
                onLoadMore = viewModel::loadMore,
                onLongPress = { actionTarget = it },
                modifier = Modifier.weight(1f),
            )

            ChatInputArea(
                state = state,
                onInputChange = viewModel::onInputChange,
                onSendText = viewModel::sendText,
                onCancelReplyEdit = {
                    viewModel.setReplyTo(null)
                    viewModel.cancelEdit()
                },
                onPickMedia = { uri, type ->
                    scope.launch {
                        viewModel.setUploading(true)
                        runCatching { sendPicked(context, viewModel, uri, type) }
                            .onFailure {
                                android.widget.Toast.makeText(context, sendFailedText, android.widget.Toast.LENGTH_SHORT).show()
                            }
                        viewModel.setUploading(false)
                    }
                },
                onRecordingChange = { active -> viewModel.setRecording(active) },
                onSendVoice = { file, dur, wave ->
                    scope.launch {
                        viewModel.setRecording(false)
                        viewModel.setUploading(true)
                        runCatching {
                            val upload = viewModel.upload(file, "audio/mp4")
                            viewModel.sendUpload(
                                ChatMessageType.VOICE,
                                upload,
                                ChatMessageMeta(durationSec = dur, waveform = wave),
                            )
                        }.onFailure {
                            android.widget.Toast.makeText(context, sendFailedText, android.widget.Toast.LENGTH_SHORT).show()
                        }
                        viewModel.setUploading(false)
                    }
                },
                onSendLocation = { lat, lng -> viewModel.sendLocation(lat, lng) },
                onSendContact = { name, phone -> viewModel.sendContact(name, phone) },
            )
        }
    }

    if (showRename) {
        val peer = state.peer ?: ChatPeer(id = peerId, fullName = peerName)
        RenameContactDialog(
            originalName = peer.fullName.ifBlank { peerName },
            currentAlias = peer.alias,
            onDismiss = { showRename = false },
            onSave = { newAlias ->
                viewModel.renameContact(newAlias)
                showRename = false
            },
        )
    }

    actionTarget?.let { target ->
        val copyText = copyableMessageText(target, strings)
        MessageActionSheet(
            message = target,
            isMine = viewModel.isMine(target),
            canCopy = copyText.isNotBlank(),
            onDismiss = { actionTarget = null },
            onReact = { emoji -> viewModel.react(target, emoji); actionTarget = null },
            onReply = { viewModel.setReplyTo(target); actionTarget = null },
            onCopy = {
                copyToClipboard(context, copyText)
                android.widget.Toast.makeText(context, copiedText, android.widget.Toast.LENGTH_SHORT).show()
                actionTarget = null
            },
            onForward = {
                forwardTarget = target
                forwardLoading = true
                forwardPeers = emptyList()
                actionTarget = null
                scope.launch {
                    forwardPeers = viewModel.loadForwardTargets()
                    forwardLoading = false
                }
            },
            onEdit = { viewModel.startEdit(target); actionTarget = null },
            onDelete = { deleteTarget = target; actionTarget = null },
            onPin = { viewModel.pin(target); actionTarget = null },
        )
    }

    deleteTarget?.let { msg ->
        DeleteMessageDialog(
            peerName = state.peer?.displayName ?: peerName,
            isMine = viewModel.isMine(msg),
            onDismiss = { deleteTarget = null },
            onDeleteForMe = { viewModel.hideForMe(msg); deleteTarget = null },
            onDeleteForEveryone = { viewModel.delete(msg); deleteTarget = null },
        )
    }

    forwardTarget?.let { msg ->
        ForwardPickerDialog(
            peers = forwardPeers,
            loading = forwardLoading,
            onDismiss = { forwardTarget = null },
            onPick = { peer ->
                viewModel.forward(msg, peer.id)
                forwardTarget = null
                android.widget.Toast.makeText(context, forwardText, android.widget.Toast.LENGTH_SHORT).show()
            },
        )
    }
}

@Composable
private fun ChatHeader(peer: ChatPeer, activity: String?, onBack: () -> Unit, onRename: () -> Unit) {
    val active = activity != null
    var showMenu by remember { mutableStateOf(false) }
    val dark = LiquidTheme.isDark
    Row(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (dark) 0.10f else 0.45f),
                        Color.White.copy(alpha = if (dark) 0.03f else 0.22f),
                    ),
                ),
            )
            .drawBehind {
                drawLine(
                    color = Color.White.copy(alpha = if (dark) 0.12f else 0.30f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1f,
                )
            }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, localized("com_back"), tint = LiquidGlass.Blue)
        }
        ChatAvatar(peer.displayName, peer.isOnline, size = 42.dp, avatarUrl = peer.avatarUrl)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(peer.displayName, color = LiquidTheme.text, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val sub = when (activity) {
                "recording" -> localized("chat_recording")
                "uploading" -> localized("chat_sending_file")
                "typing" -> localized("chat_typing")
                else -> if (peer.isOnline) {
                    localized("chat_online")
                } else {
                    ChatFormat.lastSeen(peer.lastSeenAt, rememberLastSeenLabels())
                }
            }
            Text(
                sub,
                color = if (active || peer.isOnline) LiquidGlass.Blue else LiquidTheme.textMuted,
                fontSize = 12.sp,
            )
        }
        Box {
            HeaderIcon(Icons.Default.MoreVert) { showMenu = true }
            LiquidGlassDropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                LiquidGlassDropdownItem(
                    text = localized("chat_rename_contact"),
                    selected = false,
                    onClick = { showMenu = false; onRename() },
                )
            }
        }
    }
}

@Composable
private fun RenameContactDialog(
    originalName: String,
    currentAlias: String?,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit,
) {
    var text by remember { mutableStateOf(currentAlias.orEmpty()) }
    Dialog(onDismissRequest = onDismiss) {
        GlassCard(radius = 24.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text(localized("chat_rename_contact"), color = LiquidTheme.text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    localized("chat_rename_original") + ": " + originalName,
                    color = LiquidTheme.textMuted,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .liquidGlassThemed(radius = 14.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                        Box(Modifier.weight(1f)) {
                            if (text.isEmpty()) Text(originalName, color = LiquidTheme.textMuted, fontSize = 15.sp)
                            BasicTextField(
                                value = text,
                                onValueChange = { text = it },
                                singleLine = true,
                                textStyle = TextStyle(color = LiquidTheme.text, fontSize = 15.sp),
                                cursorBrush = SolidColor(LiquidGlass.Blue),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    if (!currentAlias.isNullOrBlank()) {
                        Text(
                            localized("chat_rename_reset"),
                            color = LiquidGlass.Rose,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onSave(null) }.padding(8.dp),
                        )
                        Spacer(Modifier.weight(1f))
                    }
                    Text(
                        localized("com_cancel"),
                        color = LiquidTheme.textMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable(onClick = onDismiss).padding(8.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        localized("com_save"),
                        color = LiquidGlass.Blue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { onSave(text) }.padding(8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PinnedMessageBanner(preview: String, onUnpin: () -> Unit) {
    val dark = LiquidTheme.isDark
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = if (dark) 0.06f else 0.32f))
            .drawBehind {
                drawLine(
                    color = Color.White.copy(alpha = if (dark) 0.10f else 0.25f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1f,
                )
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(34.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(LiquidGlass.Blue),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                localized("chat_pinned_title"),
                color = LiquidGlass.Blue,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                preview,
                color = LiquidTheme.textMuted,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onUnpin),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Close, localized("chat_unpin"), tint = LiquidTheme.textMuted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun HeaderIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.size(38.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = LiquidGlass.Blue, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun MessageList(
    state: ChatUiState,
    isMine: (ChatMessage) -> Boolean,
    onLoadMore: () -> Unit,
    onLongPress: (ChatMessage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val reversed = remember(state.messages) { state.messages.filter { !it.isDeleted }.asReversed() }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            val firstVisible = listState.firstVisibleItemIndex
            if (firstVisible <= 2) listState.animateScrollToItem(0)
        }
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val layout = listState.layoutInfo
            val last = layout.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= reversed.size - 3
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) onLoadMore() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        reverseLayout = true,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        items(reversed.size, key = { reversed[it].id }) { i ->
            val msg = reversed[i]
            val ascendingIndex = state.messages.size - 1 - i
            val prev = state.messages.getOrNull(ascendingIndex - 1)
            val showDate = prev == null ||
                ChatFormat.dateHeaderKeyOrDate(prev.createdAt) != ChatFormat.dateHeaderKeyOrDate(msg.createdAt)
            Column {
                if (showDate) DateChip(msg.createdAt)
                MessageBubble(
                    msg = msg,
                    mine = isMine(msg),
                    onLongPress = { onLongPress(msg) },
                )
            }
        }
        if (state.loadingMore) {
            item { Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) { Text("...", color = LiquidTheme.textMuted) } }
        }
    }
}

@Composable
private fun DateChip(createdAt: String) {
    val key = ChatFormat.dateHeaderKeyOrDate(createdAt) ?: return
    val label = if (key.startsWith("chat_")) localized(key) else key
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.clip(RoundedCornerShape(50)).liquidGlassThemed(radius = 50.dp).padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(label, color = LiquidTheme.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(msg: ChatMessage, mine: Boolean, onLongPress: () -> Unit) {
    val bubbleShape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (mine) 18.dp else 4.dp,
        bottomEnd = if (mine) 4.dp else 18.dp,
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .combinedClickable(onClick = {}, onLongClick = onLongPress),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
    ) {
        Column(horizontalAlignment = if (mine) Alignment.End else Alignment.Start, modifier = Modifier.widthIn(max = 300.dp)) {
            Box(
                Modifier
                    .clip(bubbleShape)
                    .then(
                        if (mine) {
                            Modifier.background(Brush.linearGradient(listOf(LiquidGlass.Blue, LiquidGlass.BlueLight)))
                        } else {
                            Modifier.liquidGlassThemed(radius = 18.dp)
                        },
                    )
                    .padding(horizontal = 4.dp, vertical = 4.dp),
            ) {
                BubbleContent(msg, mine)
            }
            if (msg.reactions.isNotEmpty()) {
                ReactionChips(msg, mine)
            }
        }
    }
}

@Composable
private fun BubbleContent(msg: ChatMessage, mine: Boolean) {
    val textColor = if (mine) Color.White else LiquidTheme.text
    val mutedColor = if (mine) Color.White.copy(alpha = 0.8f) else LiquidTheme.textMuted

    Column(Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
        msg.replyTo?.let { reply ->
            ReplyPreview(reply, mine)
            Spacer(Modifier.height(4.dp))
        }
        when (msg.type) {
                ChatMessageType.IMAGE -> ImageContent(msg)
                ChatMessageType.VIDEO -> VideoContent(msg)
                ChatMessageType.VOICE -> VoiceContent(msg, mine)
                ChatMessageType.AUDIO, ChatMessageType.FILE -> FileContent(msg, mine)
                ChatMessageType.LOCATION -> LocationContent(msg, mine)
                ChatMessageType.CONTACT -> ContactContent(msg, mine)
                else -> {}
            }
            msg.body?.takeIf { it.isNotBlank() }?.let {
                if (msg.type != ChatMessageType.TEXT) Spacer(Modifier.height(4.dp))
                Text(it, color = textColor, fontSize = 15.sp, lineHeight = 20.sp)
            }
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.End)) {
            if (msg.isEdited) {
                Text(localized("chat_edited"), color = mutedColor, fontSize = 10.sp)
                Spacer(Modifier.width(4.dp))
            }
            Text(ChatFormat.time(msg.createdAt), color = mutedColor, fontSize = 11.sp)
            if (mine) {
                Spacer(Modifier.width(4.dp))
                MessageTicks(msg.status)
            }
        }
    }
}

@Composable
private fun ReplyPreview(reply: ChatMessage, mine: Boolean) {
    val accent = if (mine) Color.White else LiquidGlass.Blue
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background((if (mine) Color.White else LiquidGlass.Blue).copy(alpha = 0.12f))
            .padding(6.dp),
    ) {
        Box(Modifier.width(3.dp).height(34.dp).clip(RoundedCornerShape(2.dp)).background(accent))
        Spacer(Modifier.width(6.dp))
        Column {
            Text(localized("chat_replying_to"), color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                messagePreview(reply, rememberStringResolver()),
                color = if (mine) Color.White.copy(alpha = 0.9f) else LiquidTheme.textMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ImageContent(msg: ChatMessage) {
    val url = msg.meta?.fileUrl?.let { MediaUrl.fromFilePath(it) } ?: msg.filePath?.let { MediaUrl.fromFilePath(it) }
    if (url.isNullOrBlank()) {
        Box(
            Modifier
                .widthIn(min = 120.dp, max = 260.dp)
                .heightIn(min = 80.dp, max = 320.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Image, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(36.dp))
        }
        return
    }
    AsyncImage(
        model = url,
        contentDescription = null,
        modifier = Modifier
            .widthIn(max = 260.dp)
            .heightIn(max = 320.dp)
            .clip(RoundedCornerShape(14.dp)),
    )
}

@Composable
private fun VideoContent(msg: ChatMessage) {
    val url = msg.meta?.fileUrl?.let { MediaUrl.fromFilePath(it) } ?: msg.filePath?.let { MediaUrl.fromFilePath(it) }
    val context = LocalContext.current
    Box(
        Modifier
            .size(220.dp, 160.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable {
                url?.let { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)).apply { setDataAndType(Uri.parse(it), "video/*") }) }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(model = url, contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)))
        Box(Modifier.size(48.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
private fun VoiceContent(msg: ChatMessage, mine: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val player = remember { ChatAudioPlayer() }
    val remoteUrl = remember(msg.id, msg.filePath, msg.meta?.fileUrl) {
        val path = msg.filePath?.takeIf { it.isNotBlank() }
        val url = msg.meta?.fileUrl?.takeIf { it.isNotBlank() }
        when {
            !url.isNullOrBlank() && url.startsWith("http") -> url
            !path.isNullOrBlank() -> MediaUrl.resolve(path, url)
            !url.isNullOrBlank() -> MediaUrl.fromFilePath(url)
            else -> null
        }
    } ?: return
    var playing by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var speed by remember { mutableStateOf(1f) }
    val tint = if (mine) Color.White else LiquidGlass.Blue
    val playFailedText = localized("chat_voice_play_failed")

    DisposableEffect(Unit) { onDispose { player.release() } }
    LaunchedEffect(playing) {
        while (playing && isActive) {
            progress = player.positionFraction()
            delay(80)
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(2.dp)) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(tint.copy(alpha = 0.18f)).clickable(enabled = !loading) {
                if (playing) {
                    player.pause()
                    playing = false
                    return@clickable
                }
                loading = true
                scope.launch {
                    val started = player.toggleRemote(
                        context = context,
                        remoteUrl = remoteUrl,
                        speed = speed,
                        onProgress = { progress = it },
                        onComplete = { playing = false; progress = 0f },
                        onError = {
                            playing = false
                            android.widget.Toast.makeText(context, playFailedText, android.widget.Toast.LENGTH_SHORT).show()
                        },
                    )
                    playing = started
                    loading = false
                }
            },
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                Text("...", color = tint, fontSize = 12.sp)
            } else {
                Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = tint, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.width(8.dp))
        Waveform(
            bars = msg.meta?.waveform ?: List(28) { (8..30).random() },
            progress = progress,
            color = tint,
            modifier = Modifier.width(140.dp).height(30.dp),
        )
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(ChatFormat.durationLabel(msg.meta?.durationSec), color = if (mine) Color.White.copy(alpha = 0.85f) else LiquidTheme.textMuted, fontSize = 11.sp)
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(tint.copy(alpha = 0.18f)).clickable {
                    speed = when (speed) { 1f -> 1.5f; 1.5f -> 2f; else -> 1f }
                    player.setSpeed(speed)
                }.padding(horizontal = 6.dp, vertical = 1.dp),
            ) {
                Text("${if (speed % 1f == 0f) speed.toInt().toString() else speed.toString()}x", color = tint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun Waveform(bars: List<Int>, progress: Float, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val n = bars.size.coerceAtLeast(1)
        val gap = 3f
        val barWidth = (size.width - gap * (n - 1)) / n
        val maxVal = (bars.maxOrNull() ?: 1).coerceAtLeast(1)
        bars.forEachIndexed { i, v ->
            val h = (v.toFloat() / maxVal) * size.height
            val x = i * (barWidth + gap)
            val played = i.toFloat() / n <= progress
            drawRoundRect(
                color = if (played) color else color.copy(alpha = 0.35f),
                topLeft = Offset(x, (size.height - h) / 2f),
                size = androidx.compose.ui.geometry.Size(barWidth, h.coerceAtLeast(2f)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f),
            )
        }
    }
}

@Composable
private fun FileContent(msg: ChatMessage, mine: Boolean) {
    val context = LocalContext.current
    val url = msg.meta?.fileUrl?.let { MediaUrl.fromFilePath(it) } ?: msg.filePath?.let { MediaUrl.fromFilePath(it) }
    val tint = if (mine) Color.White else LiquidGlass.Blue
    Row(
        Modifier.clickable { url?.let { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) } }.padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(tint.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.InsertDriveFile, null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(msg.fileName ?: localized("chat_attachment"), color = if (mine) Color.White else LiquidTheme.text, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            msg.meta?.fileSize?.let {
                Text(formatSize(it), color = if (mine) Color.White.copy(alpha = 0.8f) else LiquidTheme.textMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun LocationContent(msg: ChatMessage, mine: Boolean) {
    val context = LocalContext.current
    val lat = msg.meta?.latitude ?: return
    val lng = msg.meta?.longitude ?: return
    val tint = if (mine) Color.White else LiquidGlass.Blue
    Row(
        Modifier.clickable {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng")))
        }.padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.LocationOn, null, tint = tint, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(localized("chat_attach_location"), color = if (mine) Color.White else LiquidTheme.text, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(localized("chat_view_location"), color = tint, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ContactContent(msg: ChatMessage, mine: Boolean) {
    val tint = if (mine) Color.White else LiquidGlass.Blue
    Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
        ChatAvatar(msg.meta?.contactName ?: "?", false, size = 42.dp, showPresence = false)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(msg.meta?.contactName ?: "", color = if (mine) Color.White else LiquidTheme.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(msg.meta?.contactPhone ?: "", color = if (mine) Color.White.copy(alpha = 0.85f) else LiquidTheme.textMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ReactionChips(msg: ChatMessage, mine: Boolean) {
    val grouped = msg.reactions.values.groupingBy { it }.eachCount()
    Row(
        Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        grouped.forEach { (emoji, count) ->
            Box(
                Modifier.clip(RoundedCornerShape(50)).liquidGlassThemed(radius = 50.dp).padding(horizontal = 7.dp, vertical = 2.dp),
            ) {
                Text("$emoji $count", fontSize = 12.sp, color = LiquidTheme.text)
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    val kb = bytes / 1024.0
    return when {
        kb < 1024 -> "%.0f KB".format(kb)
        else -> "%.1f MB".format(kb / 1024.0)
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    cm.setPrimaryClip(android.content.ClipData.newPlainText("chat", text))
}

private suspend fun sendPicked(
    context: Context,
    viewModel: ChatViewModel,
    uri: Uri,
    type: ChatMessageType,
) {
    if (type == ChatMessageType.IMAGE) {
        val file = ImageCompress.compressToFile(context, uri)
        val upload = viewModel.upload(file, "image/jpeg")
        viewModel.sendUpload(ChatMessageType.IMAGE, upload)
    } else {
        val file = ChatFiles.copyToCache(context, uri) ?: error("file copy failed")
        val mime = ChatFiles.mimeType(context, uri)
        val upload = viewModel.upload(file, mime)
        viewModel.sendUpload(type, upload, ChatMessageMeta(fileSize = file.length()))
    }
}

// ====================== GURUH / KANAL (ROOM) EKRANI ======================

private fun uz.vazifa.app.domain.model.RoomMessage.toChatMessage(): ChatMessage = ChatMessage(
    id = id,
    senderId = senderId,
    receiverId = roomId,
    type = type,
    body = body,
    filePath = filePath,
    fileName = fileName,
    mimeType = mimeType,
    meta = meta,
    replyToId = replyToId,
    replyTo = replyTo?.toChatMessage(),
    forwardedFrom = forwardedFrom,
    reactions = reactions,
    status = status,
    isEdited = isEdited,
    isDeleted = isDeleted,
    isPinned = isPinned,
    clientId = clientId,
    createdAt = createdAt,
)

@Composable
fun RoomConversationScreen(
    roomId: String,
    currentUserId: String,
    onBack: () -> Unit,
    viewModel: RoomViewModel = hiltViewModel(key = roomId),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sendFailedText = localized("chat_send_failed")

    LaunchedEffect(roomId, currentUserId) {
        if (roomId.isNotBlank() && currentUserId.isNotBlank()) {
            viewModel.start(roomId, currentUserId)
        }
    }

    var actionTarget by remember { mutableStateOf<uz.vazifa.app.domain.model.RoomMessage?>(null) }
    var deleteTarget by remember { mutableStateOf<uz.vazifa.app.domain.model.RoomMessage?>(null) }
    var forwardTarget by remember { mutableStateOf<uz.vazifa.app.domain.model.RoomMessage?>(null) }
    var forwardPeers by remember { mutableStateOf<List<ChatPeer>>(emptyList()) }
    var forwardLoading by remember { mutableStateOf(false) }
    val strings = rememberStringResolver()
    val copiedText = localized("chat_copied")
    val forwardText = localized("chat_forward")

    val inputState = ChatUiState(
        input = state.input,
        replyTo = state.replyTo?.toChatMessage(),
        editing = state.editing?.toChatMessage(),
    )

    LiquidBackground(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding(),
        ) {
            RoomHeader(room = state.room, roomId = roomId, typingLabel = state.typingLabel, onBack = onBack)

            state.messages.lastOrNull { it.isPinned && !it.isDeleted }?.let { pinned ->
                PinnedMessageBanner(
                    preview = messagePreview(pinned.toChatMessage(), strings),
                    onUnpin = { viewModel.pin(pinned) },
                )
            }

            RoomMessageList(
                state = state,
                isMine = viewModel::isMine,
                onLoadMore = viewModel::loadMore,
                onLongPress = { actionTarget = it },
                modifier = Modifier.weight(1f),
            )

            val canPost = state.room?.canPost ?: true
            if (canPost) {
                ChatInputArea(
                    state = inputState,
                    onInputChange = viewModel::onInputChange,
                    onSendText = viewModel::sendText,
                    onCancelReplyEdit = {
                        viewModel.setReplyTo(null)
                        viewModel.cancelEdit()
                    },
                    onPickMedia = { uri, type ->
                        scope.launch {
                            viewModel.setUploading(true)
                            runCatching { sendPickedRoom(context, viewModel, uri, type) }
                                .onFailure {
                                    android.widget.Toast.makeText(context, sendFailedText, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            viewModel.setUploading(false)
                        }
                    },
                    onRecordingChange = { active -> viewModel.setRecording(active) },
                    onSendVoice = { file, dur, wave ->
                        scope.launch {
                            viewModel.setRecording(false)
                            viewModel.setUploading(true)
                            runCatching {
                                val upload = viewModel.upload(file, "audio/mp4")
                                viewModel.sendUpload(
                                    ChatMessageType.VOICE,
                                    upload,
                                    ChatMessageMeta(durationSec = dur, waveform = wave),
                                )
                            }.onFailure {
                                android.widget.Toast.makeText(context, sendFailedText, android.widget.Toast.LENGTH_SHORT).show()
                            }
                            viewModel.setUploading(false)
                        }
                    },
                    onSendLocation = { lat, lng -> viewModel.sendLocation(lat, lng) },
                    onSendContact = { name, phone -> viewModel.sendContact(name, phone) },
                )
            } else {
                ChannelReadOnlyBar()
            }
        }
    }

    actionTarget?.let { target ->
        val chatMsg = target.toChatMessage()
        val copyText = copyableMessageText(chatMsg, strings)
        MessageActionSheet(
            message = chatMsg,
            isMine = viewModel.isMine(target.senderId),
            canCopy = copyText.isNotBlank(),
            onDismiss = { actionTarget = null },
            onReact = { emoji -> viewModel.react(target, emoji); actionTarget = null },
            onReply = { viewModel.setReplyTo(target); actionTarget = null },
            onCopy = {
                copyToClipboard(context, copyText)
                android.widget.Toast.makeText(context, copiedText, android.widget.Toast.LENGTH_SHORT).show()
                actionTarget = null
            },
            onForward = {
                forwardTarget = target
                forwardLoading = true
                forwardPeers = emptyList()
                actionTarget = null
                scope.launch {
                    forwardPeers = viewModel.loadForwardTargets()
                    forwardLoading = false
                }
            },
            onEdit = { viewModel.startEdit(target); actionTarget = null },
            onDelete = { deleteTarget = target; actionTarget = null },
            onPin = { viewModel.pin(target); actionTarget = null },
        )
    }

    deleteTarget?.let { msg ->
        DeleteMessageDialog(
            peerName = state.room?.title ?: "",
            isMine = viewModel.isMine(msg.senderId),
            onDismiss = { deleteTarget = null },
            onDeleteForMe = { viewModel.hideForMe(msg); deleteTarget = null },
            onDeleteForEveryone = { viewModel.delete(msg); deleteTarget = null },
        )
    }

    forwardTarget?.let { msg ->
        ForwardPickerDialog(
            peers = forwardPeers,
            loading = forwardLoading,
            onDismiss = { forwardTarget = null },
            onPick = { peer ->
                viewModel.forward(msg, peer.id)
                forwardTarget = null
                android.widget.Toast.makeText(context, forwardText, android.widget.Toast.LENGTH_SHORT).show()
            },
        )
    }
}

@Composable
private fun RoomHeader(
    room: uz.vazifa.app.domain.model.ChatRoom?,
    roomId: String,
    typingLabel: String?,
    onBack: () -> Unit,
) {
    val dark = LiquidTheme.isDark
    Row(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (dark) 0.10f else 0.45f),
                        Color.White.copy(alpha = if (dark) 0.03f else 0.22f),
                    ),
                ),
            )
            .drawBehind {
                drawLine(
                    color = Color.White.copy(alpha = if (dark) 0.12f else 0.30f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1f,
                )
            }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, localized("com_back"), tint = LiquidGlass.Blue)
        }
        ChatAvatar(room?.title ?: "?", online = false, size = 42.dp, showPresence = false, avatarUrl = room?.avatarUrl)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    room?.title ?: "",
                    color = LiquidTheme.text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (room?.isVerified == true) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.Verified, null, tint = LiquidGlass.Cyan, modifier = Modifier.size(15.dp))
                }
            }
            val sub = when {
                typingLabel != null -> {
                    val (name, action) = typingLabel.split("|").let { it[0] to it.getOrElse(1) { "typing" } }
                    val verb = when (action) {
                        "recording" -> localized("chat_recording")
                        "uploading" -> localized("chat_sending_file")
                        else -> localized("chat_typing")
                    }
                    "$name $verb"
                }
                room != null -> localized("chat_members_count").replace("%d", room.memberCount.toString())
                else -> ""
            }
            Text(sub, color = if (typingLabel != null) LiquidGlass.Blue else LiquidTheme.textMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ChannelReadOnlyBar() {
    Box(
        Modifier
            .fillMaxWidth()
            .background(LiquidTheme.bgMid.copy(alpha = 0.92f))
            .navigationBarsPadding()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Campaign, null, tint = LiquidTheme.textMuted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(localized("chat_channel_readonly"), color = LiquidTheme.textMuted, fontSize = 14.sp)
        }
    }
}

@Composable
private fun RoomMessageList(
    state: RoomUiState,
    isMine: (String) -> Boolean,
    onLoadMore: () -> Unit,
    onLongPress: (uz.vazifa.app.domain.model.RoomMessage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val reversed = remember(state.messages) { state.messages.filter { !it.isDeleted }.asReversed() }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            val firstVisible = listState.firstVisibleItemIndex
            if (firstVisible <= 2) listState.animateScrollToItem(0)
        }
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val layout = listState.layoutInfo
            val last = layout.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= reversed.size - 3
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) onLoadMore() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        reverseLayout = true,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        items(reversed.size, key = { reversed[it].id }) { i ->
            val msg = reversed[i]
            val ascendingIndex = state.messages.size - 1 - i
            val prev = state.messages.getOrNull(ascendingIndex - 1)
            val showDate = prev == null ||
                ChatFormat.dateHeaderKeyOrDate(prev.createdAt) != ChatFormat.dateHeaderKeyOrDate(msg.createdAt)
            val mine = isMine(msg.senderId)
            val showSender = !mine && (prev == null || prev.senderId != msg.senderId)
            Column {
                if (showDate) DateChip(msg.createdAt)
                if (showSender && !msg.senderName.isNullOrBlank()) {
                    Text(
                        msg.senderName,
                        color = LiquidGlass.Cyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 12.dp, top = 2.dp),
                    )
                }
                MessageBubble(
                    msg = msg.toChatMessage(),
                    mine = mine,
                    onLongPress = { onLongPress(msg) },
                )
            }
        }
        if (state.loadingMore) {
            item { Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) { Text("...", color = LiquidTheme.textMuted) } }
        }
    }
}

private suspend fun sendPickedRoom(
    context: Context,
    viewModel: RoomViewModel,
    uri: Uri,
    type: ChatMessageType,
) {
    if (type == ChatMessageType.IMAGE) {
        val file = ImageCompress.compressToFile(context, uri)
        val upload = viewModel.upload(file, "image/jpeg")
        viewModel.sendUpload(ChatMessageType.IMAGE, upload)
    } else {
        val file = ChatFiles.copyToCache(context, uri) ?: error("file copy failed")
        val mime = ChatFiles.mimeType(context, uri)
        val upload = viewModel.upload(file, mime)
        viewModel.sendUpload(type, upload, ChatMessageMeta(fileSize = file.length()))
    }
}
