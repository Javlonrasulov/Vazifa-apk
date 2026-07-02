package uz.vazifa.app.presentation.chat

import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import uz.vazifa.app.presentation.components.localized
import uz.vazifa.app.presentation.navigation.BottomNavHeight
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.liquidGlassThemed
import uz.vazifa.app.util.VideoNoteRecorder

@Composable
fun VideoNoteRecordingOverlay(
    seconds: Int,
    locked: Boolean,
    lockHint: Boolean,
    cancelHint: Boolean,
    recorder: VideoNoteRecorder,
    onFlipCamera: () -> Unit,
    onCancel: () -> Unit,
    onSend: () -> Unit,
    onCameraReady: () -> Unit = {},
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var previewReady by remember { mutableStateOf(false) }
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomControlsPadding = maxOf(navBottom, 48.dp) + BottomNavHeight + 12.dp

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.88f))
                .padding(WindowInsets.statusBars.asPaddingValues()),
        ) {
            val lockAlpha by animateFloatAsState(if (lockHint) 1f else 0.35f, label = "lockAlpha")

            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp)
                    .alpha(lockAlpha),
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }

            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(280.dp)
                    .clip(CircleShape)
                    .border(3.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { view ->
                        if (!previewReady) {
                            previewReady = true
                            scope.launch {
                                runCatching {
                                    recorder.bindPreview(lifecycleOwner, view)
                                    onCameraReady()
                                }
                            }
                        }
                    },
                )
            }

            Row(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 20.dp, end = 20.dp),
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .liquidGlassThemed(radius = 50.dp)
                        .clickable(onClick = onFlipCamera)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Icon(Icons.Default.Cameraswitch, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }

            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = bottomControlsPadding),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(LiquidGlass.Rose))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            ChatFormat.durationLabel(seconds),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                    }
                    Text(
                        when {
                            locked -> localized("chat_voice_locked")
                            cancelHint -> localized("chat_voice_cancel")
                            lockHint -> localized("chat_video_note_slide_lock")
                            else -> localized("chat_voice_slide_cancel")
                        },
                        color = when {
                            cancelHint -> LiquidGlass.Rose
                            lockHint -> Color.White
                            else -> Color.White.copy(alpha = 0.7f)
                        },
                        fontSize = 13.sp,
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        localized("com_cancel"),
                        color = LiquidGlass.Blue,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onCancel)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    )
                    if (locked) {
                        Box(
                            Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(LiquidGlass.Blue)
                                .clickable(onClick = onSend),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                    } else {
                        Spacer(Modifier.size(52.dp))
                    }
                }
            }
        }
    }
}
