package uz.vazifa.app.presentation.announcements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import uz.vazifa.app.di.ApiClientEntryPoint
import uz.vazifa.app.domain.model.AnnouncementAttachment
import uz.vazifa.app.domain.model.AnnouncementRecipient
import uz.vazifa.app.util.ChatAudioPlayer
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
import uz.vazifa.app.presentation.theme.VazifaColors
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
fun AnnouncementActionChip(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    background: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(background)
            .border(1.dp, tint.copy(alpha = 0.18f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun AnnouncementRowActions(
    onEditClick: (() -> Unit)?,
    onDeleteClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    if (onEditClick == null && onDeleteClick == null) return
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        onEditClick?.let { action ->
            AnnouncementActionChip(
                onClick = action,
                icon = Icons.Outlined.Edit,
                contentDescription = localized("announcement_edit"),
                tint = AnnouncementAccent.Primary,
                background = AnnouncementAccent.Soft.copy(alpha = if (LiquidTheme.isDark) 0.22f else 0.85f),
            )
        }
        onDeleteClick?.let { action ->
            AnnouncementActionChip(
                onClick = action,
                icon = Icons.Outlined.DeleteOutline,
                contentDescription = localized("announcement_delete"),
                tint = VazifaColors.Danger,
                background = VazifaColors.Danger.copy(alpha = if (LiquidTheme.isDark) 0.18f else 0.1f),
            )
        }
    }
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
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
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
        if (onEditClick != null || onDeleteClick != null) {
            AnnouncementRowActions(
                onEditClick = onEditClick,
                onDeleteClick = onDeleteClick,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
    }
}

@Composable
fun AnnouncementReceivedListRow(
    title: String,
    fromText: String,
    statusText: String,
    acknowledged: Boolean,
    onClick: () -> Unit,
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
            AnnouncementTypeBadge()
            Text(title, color = LiquidTheme.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(fromText, color = LiquidTheme.textMuted, fontSize = 12.sp)
            Text(
                statusText,
                color = if (acknowledged) AnnouncementAccent.Primary else LiquidTheme.textMuted,
                fontSize = 12.sp,
                fontWeight = if (acknowledged) FontWeight.Medium else FontWeight.Normal,
            )
        }
    }
}

enum class AnnouncementRecipientTrackStatus {
    VIEWED,
    NOT_VIEWED,
    ACKNOWLEDGED,
    NOT_ACKNOWLEDGED,
}

@Composable
fun AnnouncementRecipientStatusRow(recipient: AnnouncementRecipient, status: AnnouncementRecipientTrackStatus) {
    val positive = status == AnnouncementRecipientTrackStatus.VIEWED ||
        status == AnnouncementRecipientTrackStatus.ACKNOWLEDGED
    val icon = when (status) {
        AnnouncementRecipientTrackStatus.VIEWED -> Icons.Default.Visibility
        AnnouncementRecipientTrackStatus.NOT_VIEWED -> Icons.Default.VisibilityOff
        AnnouncementRecipientTrackStatus.ACKNOWLEDGED -> Icons.Default.CheckCircle
        AnnouncementRecipientTrackStatus.NOT_ACKNOWLEDGED -> Icons.Default.RadioButtonUnchecked
    }
    GlassCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (positive) AnnouncementAccent.Primary else LiquidTheme.textMuted,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    recipient.recipient?.fullName ?: recipient.recipientId,
                    color = LiquidTheme.text,
                    fontWeight = FontWeight.Medium,
                )
                recipient.recipient?.department?.let { dept ->
                    Text(dept, color = LiquidTheme.textMuted, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun AnnouncementRecipientsStatusSection(recipients: List<AnnouncementRecipient>) {
    val viewed = recipients.filter { it.viewedAt != null }
    val notViewed = recipients.filter { it.viewedAt == null }
    val acknowledged = recipients.filter { it.acknowledgedAt != null }
    val pending = recipients.filter { it.acknowledgedAt == null }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(localized("announcement_viewed_list"), color = LiquidTheme.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        if (viewed.isEmpty()) {
            Text(localized("announcement_none_viewed"), color = LiquidTheme.textMuted, fontSize = 13.sp)
        } else {
            viewed.forEach { r ->
                AnnouncementRecipientStatusRow(r, AnnouncementRecipientTrackStatus.VIEWED)
            }
        }

        Text(localized("announcement_not_viewed_list"), color = LiquidTheme.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        if (notViewed.isEmpty()) {
            Text(localized("announcement_all_viewed"), color = AnnouncementAccent.Primary, fontSize = 13.sp)
        } else {
            notViewed.forEach { r ->
                AnnouncementRecipientStatusRow(r, AnnouncementRecipientTrackStatus.NOT_VIEWED)
            }
        }

        Text(localized("announcement_acknowledged_list"), color = LiquidTheme.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        if (acknowledged.isEmpty()) {
            Text(localized("announcement_none_acknowledged"), color = LiquidTheme.textMuted, fontSize = 13.sp)
        } else {
            acknowledged.forEach { r ->
                AnnouncementRecipientStatusRow(r, AnnouncementRecipientTrackStatus.ACKNOWLEDGED)
            }
        }

        Text(localized("announcement_pending_list"), color = LiquidTheme.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        if (pending.isEmpty()) {
            Text(localized("announcement_all_acknowledged"), color = AnnouncementAccent.Primary, fontSize = 13.sp)
        } else {
            pending.forEach { r ->
                AnnouncementRecipientStatusRow(r, AnnouncementRecipientTrackStatus.NOT_ACKNOWLEDGED)
            }
        }
    }
}

@Composable
fun AnnouncementAttachmentsList(attachments: List<AnnouncementAttachment>) {
    if (attachments.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(localized("task_attachments"), color = LiquidTheme.textMuted, fontSize = 13.sp)
        attachments.forEach { attachment ->
            if (attachment.mimeType.startsWith("audio/")) {
                AnnouncementVoiceAttachmentPlayer(attachment)
            }
        }
    }
}

@Composable
private fun AnnouncementVoiceAttachmentPlayer(attachment: AnnouncementAttachment) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(context.applicationContext, ApiClientEntryPoint::class.java)
    }
    val player = remember { ChatAudioPlayer() }
    var playing by remember(attachment.id) { mutableStateOf(false) }
    var loading by remember(attachment.id) { mutableStateOf(false) }
    val playFailedText = localized("chat_voice_play_failed")

    DisposableEffect(attachment.id) {
        onDispose { player.release() }
    }

    GlassCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(
                onClick = {
                    if (playing) {
                        player.pause()
                        playing = false
                        return@IconButton
                    }
                    loading = true
                    scope.launch {
                        val started = player.toggleRemote(
                            context = context,
                            remoteUrl = attachment.url,
                            speed = 1f,
                            authToken = entryPoint.tokenStore().accessToken,
                            okHttpClient = entryPoint.apiClient().httpClient,
                            onProgress = {},
                            onComplete = { playing = false },
                            onError = {
                                playing = false
                                android.widget.Toast.makeText(context, playFailedText, android.widget.Toast.LENGTH_SHORT).show()
                            },
                        )
                        playing = started
                        loading = false
                    }
                },
                modifier = Modifier.size(40.dp),
            ) {
                if (loading) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = AnnouncementAccent.Primary)
                } else {
                    Icon(
                        if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = localized("task_voice_play"),
                        tint = AnnouncementAccent.Primary,
                    )
                }
            }
            Icon(Icons.Default.Mic, contentDescription = null, tint = AnnouncementAccent.Primary, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f)) {
                Text(localized("task_voice_message"), color = LiquidTheme.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(attachment.fileName, color = LiquidTheme.textMuted, fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}
