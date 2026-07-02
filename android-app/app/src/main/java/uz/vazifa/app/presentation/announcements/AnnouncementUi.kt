package uz.vazifa.app.presentation.announcements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.vazifa.app.presentation.components.TaskCountdownText
import uz.vazifa.app.presentation.components.localized
import uz.vazifa.app.presentation.theme.GlassCard
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.presentation.theme.liquidGlassThemed
import uz.vazifa.app.util.TaskDeadlineCountdown

object AnnouncementAccent {
    val Primary = Color(0xFFEA580C)
    val Light = Color(0xFFF97316)
    val Soft = Color(0xFFFFEDD5)
    val SoftDark = Color(0x33EA580C)
    val Gradient = listOf(Light, Primary)
}

@Composable
fun AnnouncementTypeBadge(modifier: Modifier = Modifier) {
    Text(
        localized("announcement_type_label"),
        color = AnnouncementAccent.Primary,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 0.8.sp,
        modifier = modifier
            .background(AnnouncementAccent.SoftDark, RoundedCornerShape(8.dp))
            .border(1.dp, AnnouncementAccent.Primary.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
fun AnnouncementIconCircle(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 44.dp,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(AnnouncementAccent.Gradient)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.Campaign,
            contentDescription = localized("announcement_type_label"),
            tint = Color.White,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
fun AnnouncementScreenBanner(modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .liquidGlassThemed()
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(AnnouncementAccent.Gradient),
                shape = RoundedCornerShape(LiquidGlass.RadiusCard),
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnnouncementIconCircle(size = 40.dp, iconSize = 22.dp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            AnnouncementTypeBadge()
            Text(
                localized("announcement_type_desc"),
                color = LiquidTheme.textMuted,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
fun AnnouncementContentCard(
    title: String,
    description: String?,
    creatorName: String?,
    deadlineAt: String,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(Brush.verticalGradient(AnnouncementAccent.Gradient)),
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AnnouncementIconCircle(size = 36.dp, iconSize = 20.dp)
                    AnnouncementTypeBadge()
                }
                Text(title, color = LiquidTheme.text, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                description?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = LiquidTheme.textMuted, fontSize = 14.sp, lineHeight = 20.sp)
                }
                creatorName?.let { name ->
                    Text(
                        "${localized("announcement_from")}: $name",
                        color = LiquidTheme.textMuted,
                        fontSize = 13.sp,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "${localized("announcement_deadline")}: ${TaskDeadlineCountdown.formatDisplay(deadlineAt)}",
                        color = LiquidTheme.textMuted,
                        fontSize = 13.sp,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${localized("announcement_time_remaining")}:",
                            color = LiquidTheme.textMuted,
                            fontSize = 13.sp,
                        )
                        TaskCountdownText(
                            deadlineAt = deadlineAt,
                            status = null,
                            fontSize = 13.sp,
                            showPrefix = false,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnnouncementPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = AnnouncementAccent.Primary,
            contentColor = Color.White,
            disabledContainerColor = AnnouncementAccent.Primary.copy(alpha = 0.45f),
            disabledContentColor = Color.White.copy(alpha = 0.8f),
        ),
        shape = RoundedCornerShape(LiquidGlass.RadiusInput),
        content = content,
    )
}

@Composable
fun AnnouncementListRow(
    title: String,
    viewedText: String,
    acknowledgedText: String,
    onClick: () -> Unit,
    onTrackingClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .liquidGlassThemed()
            .border(
                width = 1.dp,
                color = AnnouncementAccent.Primary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(LiquidGlass.RadiusCard),
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnnouncementIconCircle(size = 40.dp, iconSize = 20.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AnnouncementTypeBadge()
            }
            Text(title, color = LiquidTheme.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(viewedText, color = LiquidTheme.textMuted, fontSize = 12.sp)
            Text(acknowledgedText, color = LiquidTheme.textMuted, fontSize = 12.sp)
            onTrackingClick?.let { action ->
                TextButton(onClick = action, contentPadding = PaddingValues(0.dp)) {
                    Text(localized("announcement_tracking"), color = AnnouncementAccent.Primary)
                }
            }
        }
    }
}
