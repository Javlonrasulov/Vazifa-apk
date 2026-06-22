package uz.vazifa.app.presentation.components

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.presentation.theme.VazifaColors
import uz.vazifa.app.util.TaskDeadlineCountdown
import java.time.Instant

private val finishedStatuses = setOf("completed", "cancelled")

@Composable
fun TaskCountdownText(
    deadlineAt: String,
    status: String?,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
) {
    val isFinished = status in finishedStatuses
    val deadline = remember(deadlineAt) { TaskDeadlineCountdown.parseDeadline(deadlineAt) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    if (!isFinished && deadline != null) {
        LaunchedEffect(deadlineAt) {
            while (isActive) {
                nowMillis = System.currentTimeMillis()
                delay(60_000L)
            }
        }
    }

    val (text, color) = when {
        isFinished || deadline == null -> {
            deadlineAt.take(16).replace('T', ' ') to LiquidTheme.textMuted
        }
        else -> {
            val remaining = TaskDeadlineCountdown.remaining(deadline, Instant.ofEpochMilli(nowMillis))
            val label = formatCountdown(remaining.hours, remaining.minutes, remaining.isOverdue)
            val tint = when {
                remaining.isOverdue -> VazifaColors.Danger
                remaining.hours < 2 -> VazifaColors.Warning
                else -> LiquidTheme.textMuted
            }
            label to tint
        }
    }

    Text(text, modifier = modifier, color = color, fontSize = fontSize)
}

@Composable
private fun formatCountdown(hours: Long, minutes: Long, overdue: Boolean): String {
    val prefix = localized(if (overdue) "task_time_overdue_prefix" else "task_time_left_prefix")
    val h = localized("task_time_hour_short")
    val m = localized("task_time_min_short")
    return "$prefix: $hours $h $minutes $m"
}
