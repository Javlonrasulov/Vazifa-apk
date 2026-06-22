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
    showPrefix: Boolean = true,
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
            TaskDeadlineCountdown.formatDisplay(deadlineAt) to LiquidTheme.textMuted
        }
        else -> {
            val remaining = TaskDeadlineCountdown.remaining(deadline, Instant.ofEpochMilli(nowMillis))
            val label = formatCountdown(
                remaining.days,
                remaining.hours,
                remaining.minutes,
                remaining.isOverdue,
                showPrefix,
            )
            val tint = when {
                remaining.isOverdue -> VazifaColors.Danger
                remaining.days == 0L && remaining.hours < 2 -> VazifaColors.Warning
                else -> LiquidTheme.textMuted
            }
            label to tint
        }
    }

    Text(text, modifier = modifier, color = color, fontSize = fontSize)
}

@Composable
private fun formatCountdown(
    days: Long,
    hours: Long,
    minutes: Long,
    overdue: Boolean,
    showPrefix: Boolean = true,
): String {
    val prefix = localized(if (overdue) "task_time_overdue_prefix" else "task_time_left_prefix")
    val dayShort = localized("task_time_day_short")
    val hourShort = localized("task_time_hour_short")
    val minShort = localized("task_time_min_short")
    val parts = buildList {
        if (days > 0) add("$days $dayShort")
        if (hours > 0 || days > 0) add("$hours $hourShort")
        add("$minutes $minShort")
    }
    val timeText = parts.joinToString(" ")
    return if (showPrefix) "$prefix $timeText" else timeText
}

@Composable
fun formatDurationParts(days: Long, hours: Long, minutes: Long): String {
    val dayShort = localized("task_time_day_short")
    val hourShort = localized("task_time_hour_short")
    val minShort = localized("task_time_min_short")
    val parts = buildList {
        if (days > 0) add("$days $dayShort")
        if (hours > 0 || days > 0) add("$hours $hourShort")
        add("$minutes $minShort")
    }
    return parts.joinToString(" ")
}
