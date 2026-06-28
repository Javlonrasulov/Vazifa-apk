package uz.vazifa.app.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import uz.vazifa.app.domain.model.PeerProfile
import uz.vazifa.app.presentation.components.localized
import uz.vazifa.app.presentation.components.roleLabelKey
import uz.vazifa.app.presentation.theme.GlassCard
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.presentation.theme.liquidGlassThemed

@Composable
fun PeerProfileSheet(
    profile: PeerProfile?,
    loading: Boolean,
    onDismiss: () -> Unit,
    onAvatarClick: (String) -> Unit,
    onRename: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val dark = LiquidTheme.isDark
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            LiquidTheme.bg.copy(alpha = 0.98f),
                            LiquidTheme.bgMid.copy(alpha = 0.98f),
                        ),
                    ),
                ),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Close, null, tint = LiquidGlass.Blue, modifier = Modifier.size(24.dp))
                    }
                }

                if (loading && profile == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = LiquidGlass.Blue)
                    }
                    return@Column
                }

                val p = profile ?: return@Column

                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .then(
                                if (!p.avatarUrl.isNullOrBlank()) {
                                    Modifier.clickable { onAvatarClick(p.avatarUrl) }
                                } else Modifier,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        ChatAvatar(
                            p.displayName,
                            p.isOnline,
                            size = 120.dp,
                            avatarUrl = p.avatarUrl,
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        p.displayName,
                        color = LiquidTheme.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (!p.alias.isNullOrBlank() && p.alias != p.fullName) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            localized("peer_profile_original_name") + ": " + p.fullName,
                            color = LiquidTheme.textMuted,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                        )
                    }

                    Spacer(Modifier.height(6.dp))
                    val statusText = when {
                        p.isOnline -> localized("chat_online")
                        p.lastSeenAt != null -> ChatFormat.lastSeen(p.lastSeenAt, rememberLastSeenLabels())
                        else -> localized("chat_offline")
                    }
                    Text(
                        statusText,
                        color = if (p.isOnline) LiquidGlass.Blue else LiquidTheme.textMuted,
                        fontSize = 14.sp,
                    )

                    Spacer(Modifier.height(24.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        ProfileActionButton(
                            icon = Icons.Default.Edit,
                            label = localized("chat_rename_contact"),
                            onClick = onRename,
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    GlassCard(Modifier.fillMaxWidth(), radius = 20.dp) {
                        Column(Modifier.padding(vertical = 6.dp)) {
                            p.phone?.takeIf { it.isNotBlank() }?.let {
                                ProfileInfoRow(Icons.Default.Phone, localized("peer_profile_phone"), it)
                            }
                            p.login?.takeIf { it.isNotBlank() }?.let {
                                ProfileInfoRow(Icons.Default.Person, localized("peer_profile_login"), "@$it")
                            }
                            p.position?.takeIf { it.isNotBlank() }?.let {
                                ProfileInfoRow(Icons.Default.Work, localized("peer_profile_position"), it)
                            }
                            p.department?.takeIf { it.isNotBlank() }?.let {
                                ProfileInfoRow(Icons.Default.Work, localized("peer_profile_department"), it)
                            }
                            p.role?.takeIf { it.isNotBlank() }?.let {
                                ProfileInfoRow(
                                    Icons.Default.Person,
                                    localized("peer_profile_role"),
                                    localized(roleLabelKey(it)),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .liquidGlassThemed(radius = 50.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = LiquidGlass.Blue, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = LiquidTheme.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ProfileInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(LiquidGlass.Blue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = LiquidGlass.Blue, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(value, color = LiquidTheme.text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(label, color = LiquidTheme.textMuted, fontSize = 13.sp)
        }
    }
}
