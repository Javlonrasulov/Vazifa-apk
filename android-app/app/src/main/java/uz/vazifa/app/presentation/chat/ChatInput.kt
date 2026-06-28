package uz.vazifa.app.presentation.chat

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import uz.vazifa.app.domain.model.ChatMessage
import uz.vazifa.app.domain.model.ChatMessageType
import uz.vazifa.app.presentation.components.localized
import uz.vazifa.app.presentation.theme.GlassCard
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.presentation.theme.liquidGlassThemed
import uz.vazifa.app.util.VoiceRecorder
import java.io.File

private val emojiPalette = listOf(
    "😀", "😁", "😂", "🤣", "😊", "😍", "😘", "😎", "🤩", "🥳",
    "🤔", "😐", "😴", "😢", "😭", "😤", "😡", "👍", "👎", "👏",
    "🙏", "🔥", "❤️", "💯", "🎉", "✅", "❌", "⚡", "💪", "👀",
)

@Composable
fun ChatInputArea(
    state: ChatUiState,
    onInputChange: (String) -> Unit,
    onSendText: () -> Unit,
    onCancelReplyEdit: () -> Unit,
    onPickMedia: (Uri, ChatMessageType) -> Unit,
    onRecordingChange: (Boolean) -> Unit = {},
    onSendVoice: (File, Int, List<Int>) -> Unit,
    onSendLocation: (Double, Double) -> Unit,
    onSendContact: (String, String) -> Unit,
) {
    val context = LocalContext.current
    var showAttach by remember { mutableStateOf(false) }
    var showEmoji by remember { mutableStateOf(false) }
    val recorder = remember { VoiceRecorder(context) }
    var recording by remember { mutableStateOf(false) }
    var recordSeconds by remember { mutableStateOf(0) }
    var cancelHint by remember { mutableStateOf(false) }
    val amplitudes = remember { mutableStateListOf<Int>() }
    var currentFile by remember { mutableStateOf<File?>(null) }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onPickMedia(it, ChatMessageType.IMAGE) }
    }
    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onPickMedia(it, ChatMessageType.VIDEO) }
    }
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onPickMedia(it, ChatMessageType.FILE) }
    }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) cameraUri?.let { onPickMedia(it, ChatMessageType.IMAGE) }
    }
    val contactLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val data = res.data?.data ?: return@rememberLauncherForActivityResult
        readContact(context, data)?.let { (name, phone) -> onSendContact(name, phone) }
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) lastLocation(context)?.let { (lat, lng) -> onSendLocation(lat, lng) }
    }

    LaunchedEffect(recording) {
        if (recording) {
            amplitudes.clear()
            recordSeconds = 0
            var ticks = 0
            while (recording && isActive) {
                amplitudes.add(recorder.amplitude())
                ticks++
                if (ticks % 10 == 0) recordSeconds++
                delay(100)
            }
        }
    }

    Column(Modifier.fillMaxWidth().background(LiquidTheme.bgMid.copy(alpha = 0.92f)).navigationBarsPadding().imePadding()) {
        AnimatedVisibility(visible = state.replyTo != null || state.editing != null) {
            ReplyEditBar(state, onCancelReplyEdit)
        }

        AnimatedVisibility(
            visible = showAttach && !recording,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            AttachmentRow(
                onPhoto = { showAttach = false; imageLauncher.launch("image/*") },
                onCamera = {
                    showAttach = false
                    val file = File(context.cacheDir, "cam_${System.currentTimeMillis()}.jpg")
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    cameraUri = uri
                    cameraLauncher.launch(uri)
                },
                onVideo = { showAttach = false; videoLauncher.launch("video/*") },
                onFile = { showAttach = false; fileLauncher.launch("*/*") },
                onLocation = {
                    showAttach = false
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        lastLocation(context)?.let { (lat, lng) -> onSendLocation(lat, lng) }
                    } else locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                },
                onContact = {
                    showAttach = false
                    contactLauncher.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI))
                },
            )
        }

        AnimatedVisibility(visible = showEmoji && !recording) {
            EmojiPanel { onInputChange(state.input + it) }
        }

        if (recording) {
            RecordingBar(seconds = recordSeconds, cancelHint = cancelHint, amplitudes = amplitudes)
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            if (!recording) {
                InputIcon(Icons.Default.Add) { showAttach = !showAttach; showEmoji = false }
            } else {
                Spacer(Modifier.width(48.dp))
            }

            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp, max = 120.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .liquidGlassThemed(radius = 22.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        if (state.input.isEmpty() && !recording) {
                            Text(localized("chat_message_hint"), color = LiquidTheme.textMuted, fontSize = 15.sp)
                        }
                        if (!recording) {
                            BasicTextField(
                                value = state.input,
                                onValueChange = onInputChange,
                                textStyle = TextStyle(color = LiquidTheme.text, fontSize = 15.sp),
                                cursorBrush = SolidColor(LiquidGlass.Blue),
                                maxLines = 5,
                            )
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Default.Mood,
                        null,
                        tint = if (showEmoji) LiquidGlass.Blue else LiquidTheme.textMuted,
                        modifier = Modifier.size(22.dp).clickable { showEmoji = !showEmoji; showAttach = false },
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            if (state.input.isNotBlank()) {
                SendButton(onClick = onSendText)
            } else {
                VoiceButton(
                    recording = recording,
                    onStart = {
                        runCatching {
                            currentFile = recorder.start()
                            recording = true
                            cancelHint = false
                            onRecordingChange(true)
                        }
                    },
                    onStop = {
                        recording = false
                        onRecordingChange(false)
                        val file = recorder.stop()
                        if (file != null && recordSeconds >= 1) {
                            onSendVoice(file, recordSeconds, downsample(amplitudes.toList()))
                        } else file?.delete()
                    },
                    onCancel = { recording = false; cancelHint = false; onRecordingChange(false); recorder.cancel() },
                    onDragCancel = { cancelHint = it },
                )
            }
        }
    }
}

@Composable
private fun ReplyEditBar(state: ChatUiState, onCancel: () -> Unit) {
    val target = state.editing ?: state.replyTo ?: return
    val label = if (state.editing != null) localized("chat_edit") else localized("chat_replying_to")
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(3.dp).height(34.dp).clip(RoundedCornerShape(2.dp)).background(LiquidGlass.Blue))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = LiquidGlass.Blue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(messagePreview(target, rememberStringResolver()), color = LiquidTheme.textMuted, fontSize = 13.sp, maxLines = 1)
        }
        Icon(Icons.Default.Close, null, tint = LiquidTheme.textMuted, modifier = Modifier.size(20.dp).clickable(onClick = onCancel))
    }
}

@Composable
private fun AttachmentRow(
    onPhoto: () -> Unit,
    onCamera: () -> Unit,
    onVideo: () -> Unit,
    onFile: () -> Unit,
    onLocation: () -> Unit,
    onContact: () -> Unit,
) {
    LazyRow(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { AttachItem(Icons.Default.Image, localized("chat_attach_photo"), LiquidGlass.Violet, onPhoto) }
        item { AttachItem(Icons.Default.PhotoCamera, localized("chat_attach_camera"), LiquidGlass.Blue, onCamera) }
        item { AttachItem(Icons.Default.Videocam, localized("chat_attach_video"), LiquidGlass.Rose, onVideo) }
        item { AttachItem(Icons.Default.Folder, localized("chat_attach_file"), LiquidGlass.Cyan, onFile) }
        item { AttachItem(Icons.Default.LocationOn, localized("chat_attach_location"), LiquidGlass.Emerald, onLocation) }
        item { AttachItem(Icons.Default.Person, localized("chat_attach_contact"), LiquidGlass.Amber, onContact) }
    }
}

@Composable
private fun AttachItem(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(
            Modifier.size(54.dp).clip(CircleShape).background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.7f)))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = LiquidTheme.textMuted, fontSize = 11.sp)
    }
}

@Composable
private fun EmojiPanel(onEmoji: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(8),
        modifier = Modifier.fillMaxWidth().height(180.dp).padding(horizontal = 8.dp),
    ) {
        items(emojiPalette.size) { i ->
            Box(
                Modifier.padding(4.dp).clickable { onEmoji(emojiPalette[i]) },
                contentAlignment = Alignment.Center,
            ) {
                Text(emojiPalette[i], fontSize = 24.sp)
            }
        }
    }
}

@Composable
private fun RecordingBar(seconds: Int, cancelHint: Boolean, amplitudes: List<Int>) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(LiquidGlass.Rose))
        Spacer(Modifier.width(8.dp))
        Text(ChatFormat.durationLabel(seconds), color = LiquidTheme.text, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(12.dp))
        Text(
            if (cancelHint) localized("chat_voice_cancel") else localized("chat_voice_slide_cancel"),
            color = if (cancelHint) LiquidGlass.Rose else LiquidTheme.textMuted,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun InputIcon(icon: ImageVector, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = LiquidGlass.Blue, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun SendButton(onClick: () -> Unit) {
    Box(
        Modifier.size(46.dp).clip(CircleShape).background(Brush.linearGradient(listOf(LiquidGlass.Blue, LiquidGlass.Cyan))).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun VoiceButton(
    recording: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onCancel: () -> Unit,
    onDragCancel: (Boolean) -> Unit,
) {
    Box(
        Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(
                if (recording) Brush.linearGradient(listOf(LiquidGlass.Rose, LiquidGlass.Rose.copy(alpha = 0.7f)))
                else Brush.linearGradient(listOf(LiquidGlass.Blue, LiquidGlass.Cyan)),
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitPointerEvent().changes.firstOrNull() ?: return@awaitEachGesture
                    onStart()
                    var cancelled = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        val dx = change.position.x - down.position.x
                        cancelled = dx < -120f
                        onDragCancel(cancelled)
                        if (change.changedToUp()) break
                    }
                    if (cancelled) onCancel() else onStop()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.Mic, null, tint = Color.White, modifier = Modifier.size(24.dp))
    }
}

@Composable
fun MessageActionSheet(
    message: ChatMessage,
    isMine: Boolean,
    onDismiss: () -> Unit,
    onReact: (String) -> Unit,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPin: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            GlassCard(radius = 28.dp) {
                LazyRow(
                    Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(ReactionEmojis.size) { i ->
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).clickable { onReact(ReactionEmojis[i]) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(ReactionEmojis[i], fontSize = 24.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            GlassCard(radius = 20.dp, modifier = Modifier.width(220.dp)) {
                Column(Modifier.padding(vertical = 6.dp)) {
                    ActionItem(Icons.AutoMirrored.Filled.Reply, localized("chat_reply"), onReply)
                    if (message.type == ChatMessageType.TEXT && !message.isDeleted) {
                        ActionItem(Icons.Default.ContentCopy, localized("chat_copy"), onCopy)
                    }
                    if (isMine && message.type == ChatMessageType.TEXT && !message.isDeleted) {
                        ActionItem(Icons.Default.Edit, localized("chat_edit"), onEdit)
                    }
                    ActionItem(Icons.Default.PushPin, localized(if (message.isPinned) "chat_unpin" else "chat_pin"), onPin)
                    if (isMine) {
                        ActionItem(Icons.Default.Delete, localized("chat_delete"), onDelete, danger = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionItem(icon: ImageVector, label: String, onClick: () -> Unit, danger: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (danger) LiquidGlass.Rose else LiquidGlass.Blue, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, color = if (danger) LiquidGlass.Rose else LiquidTheme.text, fontSize = 15.sp)
    }
}

private fun downsample(values: List<Int>, target: Int = 28): List<Int> {
    if (values.isEmpty()) return List(target) { 8 }
    val max = (values.maxOrNull() ?: 1).coerceAtLeast(1)
    val step = (values.size.toFloat() / target).coerceAtLeast(1f)
    return (0 until target).map { i ->
        val v = values.getOrElse((i * step).toInt()) { 0 }
        ((v.toFloat() / max) * 28).toInt().coerceIn(3, 30)
    }
}

private fun lastLocation(context: Context): Pair<Double, Double>? {
    return runCatching {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = lm.getProviders(true)
        var best: android.location.Location? = null
        for (p in providers) {
            val l = lm.getLastKnownLocation(p) ?: continue
            if (best == null || l.accuracy < best!!.accuracy) best = l
        }
        best?.let { it.latitude to it.longitude }
    }.getOrNull()
}

private fun readContact(context: Context, uri: Uri): Pair<String, String>? = runCatching {
    context.contentResolver.query(uri, null, null, null, null)?.use { c ->
        if (!c.moveToFirst()) return null
        val nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val name = if (nameIdx >= 0) c.getString(nameIdx) else ""
        val num = if (numIdx >= 0) c.getString(numIdx) else ""
        if (num.isNullOrBlank()) null else (name ?: "") to num
    }
}.getOrNull()
