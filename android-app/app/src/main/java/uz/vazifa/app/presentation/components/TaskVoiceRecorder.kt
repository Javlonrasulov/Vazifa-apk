package uz.vazifa.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.vazifa.app.presentation.theme.GlassCard
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.presentation.theme.VazifaColors
import java.io.File

@Composable
fun TaskVoiceCompleteBar(
    enabled: Boolean,
    onVoiceRecorded: (File) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showTooShort by remember { mutableStateOf(false) }
    val (voiceState, voiceController) = rememberVoiceRecorder(
        onVoiceRecorded = {
            showTooShort = false
            onVoiceRecorded(it)
        },
        onTooShort = { showTooShort = true },
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
                        voiceState.isRecording -> "task_voice_recording"
                        else -> "task_voice_hold"
                    },
                ),
                color = LiquidTheme.textMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            if (voiceState.isRecording) {
                Text(
                    formatVoiceTimer(voiceState.elapsedSec),
                    color = LiquidTheme.text,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    if (voiceState.isLocked) {
                        localized("task_voice_locked")
                    } else {
                        localized("task_voice_release_send")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = LiquidTheme.textMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
            if (showTooShort) {
                Text(
                    localized("task_voice_too_short"),
                    color = VazifaColors.Danger,
                    fontSize = 12.sp,
                )
            }
            VoiceMicSendButton(
                state = voiceState,
                controller = voiceController,
                size = 72.dp,
                iconSize = 36.dp,
                enabled = enabled,
            )
        }
    }
}

private fun formatVoiceTimer(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}
