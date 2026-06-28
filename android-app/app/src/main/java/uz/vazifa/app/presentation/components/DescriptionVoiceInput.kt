package uz.vazifa.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.presentation.theme.VazifaColors
import java.io.File

@Composable
fun DescriptionVoiceInput(
    value: String,
    onValueChange: (String) -> Unit,
    voiceFile: File?,
    onVoiceRecorded: (File) -> Unit,
    onVoiceRemove: () -> Unit,
    modifier: Modifier = Modifier,
    minLines: Int = 3,
) {
    val fieldColors = liquidGlassFieldColors()
    var showTooShort by remember { mutableStateOf(false) }
    val (voiceState, voiceController) = rememberVoiceRecorder(
        onVoiceRecorded = {
            showTooShort = false
            onVoiceRecorded(it)
        },
        onTooShort = { showTooShort = true },
    )

    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(localized("task_desc")) },
            modifier = Modifier.fillMaxWidth(),
            minLines = minLines,
            shape = RoundedCornerShape(LiquidGlass.RadiusInput),
            colors = fieldColors,
            trailingIcon = {
                VoiceMicSendButton(
                    state = voiceState,
                    controller = voiceController,
                    size = 40.dp,
                    iconSize = 22.dp,
                )
            },
        )
        if (voiceState.isRecording) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(VazifaColors.Danger),
                )
                Text(
                    "${localized("task_voice_recording")} ${formatVoiceTimer(voiceState.elapsedSec)}",
                    color = VazifaColors.Danger,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    if (voiceState.isLocked) {
                        localized("chat_voice_locked")
                    } else {
                        localized("task_voice_release_send")
                    },
                    color = LiquidTheme.textMuted,
                    fontSize = 12.sp,
                )
            }
        }
        if (showTooShort) {
            Text(
                localized("task_voice_too_short"),
                color = VazifaColors.Danger,
                fontSize = 12.sp,
            )
        }
        voiceFile?.let {
            LocalVoicePreviewRow(file = it, onRemove = onVoiceRemove)
        }
    }
}

private fun formatVoiceTimer(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}
