package uz.vazifa.app.presentation.components

import android.media.MediaPlayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import dagger.hilt.android.EntryPointAccessors
import uz.vazifa.app.di.ApiClientEntryPoint
import uz.vazifa.app.domain.model.TaskAttachment
import uz.vazifa.app.presentation.theme.GlassCard
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.presentation.theme.VazifaColors

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
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(context.applicationContext, ApiClientEntryPoint::class.java)
    }
    val token = entryPoint.tokenStore().accessToken
    var isPlaying by remember(attachment.id) { mutableStateOf(false) }
    var mediaPlayer by remember(attachment.id) { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(attachment.id) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    GlassCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Default.Mic, null, tint = LiquidGlass.Blue, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f)) {
                Text(localized("task_voice_message"), color = LiquidTheme.text, fontSize = 14.sp)
                Text(attachment.fileName, color = LiquidTheme.textMuted, fontSize = 11.sp, maxLines = 1)
            }
            IconButton(
                onClick = {
                    if (isPlaying) {
                        mediaPlayer?.pause()
                        isPlaying = false
                        return@IconButton
                    }
                    val player = mediaPlayer
                    if (player != null) {
                        player.start()
                        isPlaying = true
                        return@IconButton
                    }
                    val headers = token?.let { mapOf("Authorization" to "Bearer $it") } ?: emptyMap()
                    val mp = MediaPlayer()
                    mp.setDataSource(context, android.net.Uri.parse(attachment.url), headers)
                    mp.setOnCompletionListener {
                        isPlaying = false
                    }
                    mp.prepareAsync()
                    mp.setOnPreparedListener {
                        it.start()
                        isPlaying = true
                    }
                    mediaPlayer = mp
                },
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = localized("task_voice_play"),
                    tint = LiquidGlass.Blue,
                )
            }
        }
    }
}

// Keep legacy API for image-only lists
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
