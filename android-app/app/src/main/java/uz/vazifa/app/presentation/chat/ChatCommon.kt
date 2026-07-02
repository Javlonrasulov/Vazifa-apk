package uz.vazifa.app.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.SubcomposeAsyncImage
import dagger.hilt.android.EntryPointAccessors
import uz.vazifa.app.di.ImageLoaderEntryPoint
import uz.vazifa.app.util.MediaUrl
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.vazifa.app.domain.model.ChatMessage
import uz.vazifa.app.domain.model.ChatMessageStatus
import uz.vazifa.app.domain.model.ChatMessageType
import uz.vazifa.app.localization.AppStrings
import uz.vazifa.app.localization.LocalAppLanguage
import uz.vazifa.app.presentation.theme.LiquidGlass
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val zone: ZoneId = ZoneId.of("Asia/Tashkent")
private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
private val dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")

data class LastSeenLabels(
    val prefix: String = "",
    val justNow: String = "",
    val minutesAgo: String = "",
    val todayAt: String = "",
    val yesterdayAt: String = "",
)

object ChatFormat {
    fun parse(raw: String?): Instant? =
        raw?.let { runCatching { Instant.parse(it) }.getOrNull() }

    fun time(raw: String?): String = parse(raw)?.atZone(zone)?.format(timeFmt) ?: ""

    fun listTime(raw: String?): String {
        val inst = parse(raw) ?: return ""
        val date = inst.atZone(zone).toLocalDate()
        val today = LocalDate.now(zone)
        return when {
            date == today -> inst.atZone(zone).format(timeFmt)
            date == today.minusDays(1) -> "—"
            else -> inst.atZone(zone).format(dateFmt)
        }
    }

    fun dateHeaderKeyOrDate(raw: String?): String? {
        val inst = parse(raw) ?: return null
        val date = inst.atZone(zone).toLocalDate()
        val today = LocalDate.now(zone)
        return when (date) {
            today -> "chat_today"
            today.minusDays(1) -> "chat_yesterday"
            else -> inst.atZone(zone).format(dateFmt)
        }
    }

    fun durationLabel(seconds: Int?): String {
        val s = (seconds ?: 0).coerceAtLeast(0)
        return "%d:%02d".format(s / 60, s % 60)
    }

    fun videoNoteDurationLabel(seconds: Int?): String {
        val s = (seconds ?: 0).coerceAtLeast(0)
        return "%02d:%02d".format(s / 60, s % 60)
    }

    fun lastSeen(raw: String?, prefix: String, online: String, justNow: String): String =
        lastSeen(raw, LastSeenLabels(prefix = prefix, justNow = justNow))

    fun lastSeen(raw: String?, labels: LastSeenLabels): String {
        val inst = parse(raw) ?: return ""
        val zoned = inst.atZone(zone)
        val now = java.time.ZonedDateTime.now(zone)
        val minutes = Duration.between(zoned, now).toMinutes()
        val date = zoned.toLocalDate()
        val today = LocalDate.now(zone)
        val time = zoned.format(timeFmt)
        val p = labels.prefix
        return when {
            minutes < 1 -> "$p ${labels.justNow}".trim()
            minutes < 60 -> "$p $minutes ${labels.minutesAgo}".trim()
            date == today -> "$p ${labels.todayAt} $time".trim()
            date == today.minusDays(1) -> "$p ${labels.yesterdayAt} $time".trim()
            else -> "$p ${zoned.format(dateFmt)}".trim()
        }
    }
}

private val avatarPalettes = listOf(
    listOf(Color(0xFF2563EB), Color(0xFF22D3EE)),
    listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)),
    listOf(Color(0xFF10B981), Color(0xFF34D399)),
    listOf(Color(0xFFF59E0B), Color(0xFFFB7185)),
    listOf(Color(0xFF6366F1), Color(0xFF818CF8)),
    listOf(Color(0xFF06B6D4), Color(0xFF3B82F6)),
)

fun avatarBrush(name: String): Brush {
    val idx = (name.hashCode().ushr(1)) % avatarPalettes.size
    return Brush.linearGradient(avatarPalettes[idx])
}

fun initials(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}

@Composable
fun ChatAvatar(
    name: String,
    online: Boolean,
    size: Dp = 50.dp,
    showPresence: Boolean = true,
    avatarUrl: String? = null,
) {
    val context = LocalContext.current
    val imageLoader = remember {
        EntryPointAccessors.fromApplication(context.applicationContext, ImageLoaderEntryPoint::class.java)
            .imageLoader()
    }
    val resolvedUrl = remember(avatarUrl) { avatarUrl?.takeIf { it.isNotBlank() }?.let { MediaUrl.resolve(it) } }

    val dotSize = size * 0.28f
    val presenceExtra = if (showPresence && online) dotSize * 0.35f else 0.dp

    Box(Modifier.size(size + presenceExtra), contentAlignment = Alignment.Center) {
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        if (!resolvedUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = resolvedUrl,
                imageLoader = imageLoader,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size).clip(CircleShape).background(avatarBrush(name)),
                loading = {
                    Box(
                        Modifier.size(size).clip(CircleShape).background(avatarBrush(name)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            initials(name),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = (size.value * 0.36f).sp,
                        )
                    }
                },
                error = {
                    Box(
                        Modifier.size(size).clip(CircleShape).background(avatarBrush(name)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            initials(name),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = (size.value * 0.36f).sp,
                        )
                    }
                },
            )
        } else {
            Box(
                Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(avatarBrush(name)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    initials(name),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.36f).sp,
                )
            }
        }
        }
        if (showPresence && online) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(Color(0xFF34D399))
                    .border((size.value * 0.04f).dp, Color.White, CircleShape),
            )
        }
    }
}

@Composable
fun MessageTicks(status: ChatMessageStatus, modifier: Modifier = Modifier) {
    val readColor = Color(0xFF38BDF8)
    val normalColor = Color.White.copy(alpha = 0.85f)
    when (status) {
        ChatMessageStatus.SENDING -> Icon(
            Icons.Default.Schedule,
            contentDescription = null,
            tint = normalColor,
            modifier = modifier.size(13.dp),
        )
        ChatMessageStatus.SENT -> Icon(
            Icons.Default.Done,
            contentDescription = null,
            tint = normalColor,
            modifier = modifier.size(15.dp),
        )
        ChatMessageStatus.DELIVERED -> Icon(
            Icons.Default.DoneAll,
            contentDescription = null,
            tint = normalColor,
            modifier = modifier.size(15.dp),
        )
        ChatMessageStatus.READ -> Icon(
            Icons.Default.DoneAll,
            contentDescription = null,
            tint = readColor,
            modifier = modifier.size(15.dp),
        )
        ChatMessageStatus.FAILED -> Text("!", color = LiquidGlass.Rose, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

/** Server tarixini mahalliy (socket/optimistik) xabarlar bilan birlashtiradi */
fun mergeChatHistory(server: List<ChatMessage>, local: List<ChatMessage>): List<ChatMessage> {
    if (local.isEmpty()) return server
    val serverIds = server.map { it.id }.toSet()
    val serverClientIds = server.mapNotNull { it.clientId }.toSet()
    val merged = server.toMutableList()
    for (msg in local) {
        if (msg.id in serverIds) continue
        if (msg.clientId != null && msg.clientId in serverClientIds) continue
        if (msg.clientId != null && msg.clientId in serverIds) continue
        merged.add(msg)
    }
    return merged.sortedBy {
        runCatching { Instant.parse(it.createdAt) }.getOrNull() ?: Instant.EPOCH
    }
}

fun messagePreview(msg: ChatMessage?, strings: (String) -> String): String {
    if (msg == null) return ""
    if (msg.isDeleted) return ""
    return when (msg.type) {
        ChatMessageType.TEXT -> msg.body.orEmpty()
        ChatMessageType.IMAGE -> "📷 " + strings("chat_photo")
        ChatMessageType.VIDEO -> if (msg.meta?.isRoundVideo == true) {
            "⭕ " + strings("chat_video_note_message")
        } else {
            "🎬 " + strings("chat_video")
        }
        ChatMessageType.VOICE -> "🎤 " + strings("chat_voice_message")
        ChatMessageType.AUDIO -> "🎵 " + (msg.fileName ?: "Audio")
        ChatMessageType.FILE -> "📎 " + (msg.fileName ?: strings("chat_attachment"))
        ChatMessageType.LOCATION -> "📍 " + strings("chat_attach_location")
        ChatMessageType.CONTACT -> "👤 " + (msg.meta?.contactName ?: strings("chat_attach_contact"))
        ChatMessageType.STICKER -> "Sticker"
        ChatMessageType.GIF -> "GIF"
    }
}

fun messagePreviewType(type: ChatMessageType, strings: (String) -> String): String = when (type) {
    ChatMessageType.TEXT -> ""
    ChatMessageType.IMAGE -> "📷 " + strings("chat_photo")
    ChatMessageType.VIDEO -> "🎬 " + strings("chat_video")
    ChatMessageType.VOICE -> "🎤 " + strings("chat_voice_message")
    ChatMessageType.AUDIO -> "🎵 Audio"
    ChatMessageType.FILE -> "📎 " + strings("chat_attachment")
    ChatMessageType.LOCATION -> "📍 " + strings("chat_attach_location")
    ChatMessageType.CONTACT -> "👤 " + strings("chat_attach_contact")
    ChatMessageType.STICKER -> "Sticker"
    ChatMessageType.GIF -> "GIF"
}

val ReactionEmojis = listOf("👍", "❤️", "🔥", "👏", "😁", "😢", "😡")

/** Composable kontekstda til bo'yicha matn yechimini qaytaradi (messagePreview uchun) */
@Composable
fun rememberStringResolver(): (String) -> String {
    val lang = LocalAppLanguage.current
    return { AppStrings.t(lang, it) }
}

@Composable
fun rememberLastSeenLabels(): LastSeenLabels {
    val r = rememberStringResolver()
    return LastSeenLabels(
        prefix = r("chat_last_seen_prefix"),
        justNow = r("emp_last_seen_just_now"),
        minutesAgo = r("chat_min_ago"),
        todayAt = r("chat_today_at"),
        yesterdayAt = r("chat_yesterday_at"),
    )
}
