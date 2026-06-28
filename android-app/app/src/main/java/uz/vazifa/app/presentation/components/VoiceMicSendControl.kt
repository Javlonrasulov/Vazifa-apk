package uz.vazifa.app.presentation.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.VazifaColors
import uz.vazifa.app.util.VoiceRecorder
import java.io.File

private const val MIN_VOICE_MS = 1_000L
private const val LOCK_MS = 2_000L

@Stable
class VoiceRecordState(
    val isRecording: Boolean,
    val isLocked: Boolean,
    val elapsedSec: Int,
)

class VoiceRecordController(
    val startHold: () -> Unit,
    val finishRecording: () -> Unit,
    val cancelRecording: () -> Unit,
    val lock: () -> Unit,
    val locked: () -> Boolean,
)

@Composable
fun rememberVoiceRecorder(
    onVoiceRecorded: (File) -> Unit,
    onTooShort: () -> Unit = {},
): Pair<VoiceRecordState, VoiceRecordController> {
    val context = LocalContext.current
    val recorder = remember { VoiceRecorder(context) }
    var isRecording by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    var elapsedSec by remember { mutableIntStateOf(0) }
    var recordingStartedAt by remember { mutableLongStateOf(0L) }
    var pendingStart by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) pendingStart = true
    }

    LaunchedEffect(pendingStart) {
        if (pendingStart) {
            pendingStart = false
            recorder.start()
            isRecording = true
            isLocked = false
            elapsedSec = 0
            recordingStartedAt = System.currentTimeMillis()
        }
    }

    LaunchedEffect(isRecording) {
        if (!isRecording) return@LaunchedEffect
        while (isActive && isRecording) {
            delay(1_000L)
            elapsedSec++
        }
    }

    DisposableEffect(Unit) {
        onDispose { recorder.cancel() }
    }

    fun beginRecording() {
        when (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)) {
            PackageManager.PERMISSION_GRANTED -> {
                recorder.start()
                isRecording = true
                isLocked = false
                elapsedSec = 0
                recordingStartedAt = System.currentTimeMillis()
            }
            else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun finishRecording() {
        if (!isRecording) return
        isRecording = false
        isLocked = false
        val file = recorder.stop()
        if (file != null && System.currentTimeMillis() - recordingStartedAt >= MIN_VOICE_MS) {
            onVoiceRecorded(file)
        } else {
            file?.delete()
            onTooShort()
        }
    }

    fun cancelRecording() {
        isRecording = false
        isLocked = false
        elapsedSec = 0
        recorder.cancel()
    }

    val controller = VoiceRecordController(
        startHold = ::beginRecording,
        finishRecording = ::finishRecording,
        cancelRecording = ::cancelRecording,
        lock = { isLocked = true },
        locked = { isLocked },
    )
    return VoiceRecordState(isRecording, isLocked, elapsedSec) to controller
}

@Composable
fun VoiceMicSendButton(
    state: VoiceRecordState,
    controller: VoiceRecordController,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    iconSize: Dp = 24.dp,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                if (state.isRecording) {
                    Brush.linearGradient(listOf(LiquidGlass.Rose, LiquidGlass.Rose.copy(alpha = 0.7f)))
                } else {
                    Brush.linearGradient(listOf(LiquidGlass.Blue, LiquidGlass.Cyan))
                },
            )
            .then(
                if (state.isRecording) {
                    Modifier.clickable(enabled = enabled) { controller.finishRecording() }
                } else {
                    Modifier.pointerInput(enabled) {
                        if (!enabled) return@pointerInput
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            controller.startHold()
                            val downAt = System.currentTimeMillis()
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (!controller.locked() && System.currentTimeMillis() - downAt >= LOCK_MS) {
                                    controller.lock()
                                }
                                if (change.changedToUp()) break
                            }
                            if (!controller.locked()) {
                                controller.finishRecording()
                            }
                        }
                    }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (state.isRecording) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = localized("task_voice_release_send"),
                tint = Color.White,
                modifier = Modifier.size(iconSize),
            )
        } else {
            Icon(
                Icons.Default.Mic,
                contentDescription = localized("task_voice_hold"),
                tint = Color.White,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}
