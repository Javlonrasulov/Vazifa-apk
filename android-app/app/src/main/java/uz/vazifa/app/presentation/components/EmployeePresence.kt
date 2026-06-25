package uz.vazifa.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.vazifa.app.domain.model.User
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.presentation.theme.VazifaColors
import uz.vazifa.app.util.LastSeenFormatter

@Composable
fun EmployeePresenceDot(
    user: User,
    modifier: Modifier = Modifier,
    dotSize: Dp = 11.dp,
) {
    Box(
        modifier
            .size(dotSize)
            .clip(CircleShape)
            .background(if (user.isOnline) VazifaColors.Success else VazifaColors.Danger)
            .border(2.dp, LiquidTheme.bg, CircleShape),
    )
}

@Composable
fun EmployeePresenceStatus(user: User) {
    if (user.isOnline) {
        Text(
            localized("emp_online"),
            color = VazifaColors.Success,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        return
    }
    val justNow = localized("emp_last_seen_just_now")
    val minShort = localized("task_time_min_short")
    val hourShort = localized("task_time_hour_short")
    val ago = localized("emp_last_seen_ago")
    val yesterday = localized("emp_last_seen_yesterday")
    val never = localized("emp_last_seen_never")
    val lastSeenText = LastSeenFormatter.formatRelative(
        raw = user.lastSeenAt,
        justNow = justNow,
        minutesAgo = { m -> "$m $minShort $ago" },
        hoursAgo = { h -> "$h $hourShort $ago" },
        yesterdayAt = { t -> "$yesterday $t" },
        never = never,
    )
    Text(
        "${localized("emp_last_seen_prefix")}: $lastSeenText",
        color = VazifaColors.Danger,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
    )
}
