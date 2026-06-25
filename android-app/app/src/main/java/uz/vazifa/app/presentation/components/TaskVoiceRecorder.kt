package uz.vazifa.app.presentation.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import uz.vazifa.app.presentation.theme.GlassCard
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.presentation.theme.VazifaColors
import uz.vazifa.app.util.VoiceRecorder
import java.io.File

private const val MIN_VOICE_MS = 1_000L

@Composable
fun TaskVoiceCompleteBar(
    enabled: Boolean,
    onVoiceRecorded: (File) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val recorder = remember { VoiceRecorder(context) }
    var isRecording by remember { mutableStateOf(false) }
    var elapsedSec by remember { mutableIntStateOf(0) }
    var recordingStartedAt by remember { mutableLongStateOf(0L) }
    var pendingStart by remember { mutableStateOf(false) }
    var showTooShort by remember { mutableStateOf(false) }

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
        showTooShort = false
        when (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)) {
            PackageManager.PERMISSION_GRANTED -> {
                recorder.start()
                isRecording = true
                elapsedSec = 0
                recordingStartedAt = System.currentTimeMillis()
            }
            else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun finishRecording() {
        if (!isRecording) return
        isRecording = false
        val file = recorder.stop()
        if (file != null && System.currentTimeMillis() - recordingStartedAt >= MIN_VOICE_MS) {
            onVoiceRecorded(file)
        } else {
            file?.delete()
            showTooShort = true
        }
    }

    val micColor by animateColorAsState(
        if (isRecording) VazifaColors.Danger else LiquidGlass.Blue,
        label = "micColor",
    )

    GlassCard(modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                localized(
                    when {
                        isRecording -> "task_voice_recording"
                        else -> "task_voice_hold"
                    },
                ),
                color = LiquidTheme.textMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            if (isRecording) {
                Text(
                    formatVoiceTimer(elapsedSec),
                    color = LiquidTheme.text,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    localized("task_voice_release_send"),
                    color = LiquidTheme.textMuted,
                    fontSize = 12.sp,
                )
            }
            if (showTooShort) {
                Text(
                    localized("task_voice_too_short"),
                    color = VazifaColors.Danger,
                    fontSize = 12.sp,
                )
            }
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(micColor.copy(alpha = 0.15f))
                    .pointerInput(enabled) {
                        if (!enabled) return@pointerInput
                        detectTapGestures(
                            onPress = {
                                beginRecording()
                                tryAwaitRelease()
                                finishRecording()
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = localized("task_voice_hold"),
                    tint = micColor,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
    }
}

private fun formatVoiceTimer(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}
