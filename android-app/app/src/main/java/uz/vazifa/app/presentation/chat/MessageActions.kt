package uz.vazifa.app.presentation.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import uz.vazifa.app.domain.model.ChatMessage
import uz.vazifa.app.domain.model.ChatPeer
import uz.vazifa.app.localization.AppLanguage
import uz.vazifa.app.localization.LocalAppLanguage
import uz.vazifa.app.presentation.components.localized
import uz.vazifa.app.presentation.theme.GlassCard
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidTheme

fun copyableMessageText(msg: ChatMessage, strings: (String) -> String): String {
    if (msg.isDeleted) return ""
    msg.body?.takeIf { it.isNotBlank() }?.let { return it }
    return messagePreview(msg, strings)
}

/** Ism + `-da` (Javlon → Javlonda, "Test uchun Javlon" → "Test uchun Javlon da") */
private fun peerLocative(name: String): String {
    val n = name.trim()
    if (n.isBlank()) return n
    return if (n.contains(' ')) "$n da" else "${n}da"
}

@Composable
fun DeleteMessageDialog(
    peerName: String,
    isMine: Boolean,
    onDismiss: () -> Unit,
    onDeleteForMe: () -> Unit,
    onDeleteForEveryone: () -> Unit,
) {
    var forEveryone by remember { mutableStateOf(isMine) }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(radius = 24.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    localized("chat_delete_title"),
                    color = LiquidTheme.text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                )
                Spacer(Modifier.height(8.dp))
                if (isMine) {
                    Text(
                        localized("chat_delete_everyone_hint"),
                        color = LiquidTheme.textMuted,
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { forEveryone = !forEveryone }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (forEveryone) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = null,
                            tint = LiquidGlass.Blue,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                localized("chat_delete_for_everyone"),
                                color = LiquidTheme.text,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                            )
                            if (peerName.isNotBlank()) {
                                val hintArg = if (LocalAppLanguage.current == AppLanguage.RU) {
                                    peerName
                                } else {
                                    peerLocative(peerName)
                                }
                                Text(
                                    String.format(localized("chat_delete_peer_hint"), hintArg),
                                    color = LiquidTheme.textMuted,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text(
                            localized("com_cancel"),
                            color = LiquidTheme.textMuted,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onDismiss)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            localized("chat_delete"),
                            color = LiquidGlass.Rose,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (forEveryone) onDeleteForEveryone() else onDeleteForMe()
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                } else {
                    Text(
                        localized("chat_delete_for_me_hint"),
                        color = LiquidTheme.textMuted,
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text(
                            localized("com_cancel"),
                            color = LiquidTheme.textMuted,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onDismiss)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            localized("chat_delete_for_me"),
                            color = LiquidGlass.Rose,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onDeleteForMe)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ForwardPickerDialog(
    peers: List<ChatPeer>,
    loading: Boolean,
    onDismiss: () -> Unit,
    onPick: (ChatPeer) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassCard(radius = 24.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(vertical = 12.dp)) {
                Text(
                    localized("chat_forward"),
                    color = LiquidTheme.text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                if (loading) {
                    Text(
                        localized("com_loading"),
                        color = LiquidTheme.textMuted,
                        modifier = Modifier.padding(16.dp),
                    )
                } else if (peers.isEmpty()) {
                    Text(
                        localized("chat_select_contact"),
                        color = LiquidTheme.textMuted,
                        modifier = Modifier.padding(16.dp),
                    )
                } else {
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(peers, key = { it.id }) { peer ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(peer) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ChatAvatar(peer.displayName, peer.isOnline, size = 42.dp, avatarUrl = peer.avatarUrl)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        peer.displayName,
                                        color = LiquidTheme.text,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    peer.position?.let {
                                        Text(it, color = LiquidTheme.textMuted, fontSize = 12.sp, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
