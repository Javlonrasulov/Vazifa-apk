package uz.vazifa.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import uz.vazifa.app.di.ApiClientEntryPoint
import uz.vazifa.app.domain.model.TaskAttachment
import uz.vazifa.app.presentation.theme.GlassCard
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.presentation.theme.VazifaColors
import uz.vazifa.app.util.ChatAudioPlayer
import java.io.File

@Composable
fun TaskAttachmentsList(
    attachments: List<TaskAttachment>,
    modifier: Modifier = Modifier,
) {
    if (attachments.isEmpty()) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        attachments.forEach { attachment ->
            if (attachment.mimeType.startsWith("audio/")) {
                TaskVoiceAttachmentPlayer(attachment)
            } else {
                TaskImageAttachment(attachment.url)
            }
        }
    }
}

@Composable
fun LocalVoicePreviewRow(
    file: File,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val player = remember { ChatAudioPlayer() }
    var playing by remember(file.absolutePath) { mutableStateOf(false) }
    var loading by remember(file.absolutePath) { mutableStateOf(false) }
    val playFailedText = localized("chat_voice_play_failed")

    DisposableEffect(file.absolutePath) {
        onDispose { player.release() }
    }

    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LiquidGlass.RadiusChip))
            .background(LiquidGlass.Blue.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(
            onClick = {
                if (playing) {
                    player.pause()
                    playing = false
                    return@IconButton
                }
                loading = true
                scope.launch {
                    val started = player.toggleLocal(
                        file = file,
                        speed = 1f,
                        onProgress = {},
                        onComplete = { playing = false },
                        onError = {
                            playing = false
                            android.widget.Toast.makeText(context, playFailedText, android.widget.Toast.LENGTH_SHORT).show()
                        },
                    )
                    playing = started
                    loading = false
                }
            },
            modifier = Modifier.size(36.dp),
        ) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = LiquidGlass.Blue)
            } else {
                Icon(
                    if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = localized("task_voice_play"),
                    tint = LiquidGlass.Blue,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Icon(Icons.Default.Mic, null, tint = LiquidGlass.Blue, modifier = Modifier.size(18.dp))
        Text(
            localized("task_voice_message"),
            color = LiquidTheme.text,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = localized("com_cancel"),
                tint = LiquidTheme.textMuted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun TaskImageAttachment(url: String) {
    SubcomposeAsyncImage(
        model = url,
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(LiquidGlass.RadiusInput)),
        contentScale = ContentScale.Crop,
        loading = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), color = LiquidGlass.Blue)
            }
        },
        error = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddPhotoAlternate, null, tint = LiquidTheme.textMuted)
                    Text(localized("task_photo_load_failed"), color = LiquidTheme.textMuted, fontSize = 12.sp)
                }
            }
        },
    )
}

@Composable
private fun TaskVoiceAttachmentPlayer(attachment: TaskAttachment) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(context.applicationContext, ApiClientEntryPoint::class.java)
    }
    val token = entryPoint.tokenStore().accessToken
    val player = remember { ChatAudioPlayer() }
    var playing by remember(attachment.id) { mutableStateOf(false) }
    var loading by remember(attachment.id) { mutableStateOf(false) }
    val playFailedText = localized("chat_voice_play_failed")

    DisposableEffect(attachment.id) {
        onDispose { player.release() }
    }

    GlassCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(
                onClick = {
                    if (playing) {
                        player.pause()
                        playing = false
                        return@IconButton
                    }
                    loading = true
                    scope.launch {
                        val started = player.toggleRemote(
                            context = context,
                            remoteUrl = attachment.url,
                            speed = 1f,
                            authToken = token,
                            onProgress = {},
                            onComplete = { playing = false },
                            onError = {
                                playing = false
                                android.widget.Toast.makeText(context, playFailedText, android.widget.Toast.LENGTH_SHORT).show()
                            },
                        )
                        playing = started
                        loading = false
                    }
                },
                modifier = Modifier.size(40.dp),
            ) {
                if (loading) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = LiquidGlass.Blue)
                } else {
                    Icon(
                        if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = localized("task_voice_play"),
                        tint = LiquidGlass.Blue,
                    )
                }
            }
            Icon(Icons.Default.Mic, null, tint = LiquidGlass.Blue, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f)) {
                Text(localized("task_voice_message"), color = LiquidTheme.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(attachment.fileName, color = LiquidTheme.textMuted, fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}

@Composable
fun TaskAttachmentGrid(
    urls: List<String>,
    modifier: Modifier = Modifier,
) {
    TaskAttachmentsList(
        attachments = urls.map { url ->
            TaskAttachment("", "", "", "image/jpeg", 0, "", url)
        },
        modifier = modifier,
    )
}
