package uz.vazifa.app.presentation.chat

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.vazifa.app.presentation.components.localized
import uz.vazifa.app.presentation.theme.GlassCard
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.presentation.theme.liquidGlassThemed
import kotlin.math.roundToInt

enum class ChatDrawerAction {
    PROFILE, CONTACTS, NEW_GROUP, NEW_CHANNEL, SAVED, SETTINGS,
}

private data class DrawerMenuEntry(
    val icon: ImageVector,
    val labelKey: String,
    val action: ChatDrawerAction,
    val accent: Color,
)

private val drawerMenu = listOf(
    DrawerMenuEntry(Icons.Default.Person, "chat_drawer_profile", ChatDrawerAction.PROFILE, LiquidGlass.Blue),
    DrawerMenuEntry(Icons.Default.Contacts, "chat_drawer_contacts", ChatDrawerAction.CONTACTS, LiquidGlass.Cyan),
    DrawerMenuEntry(Icons.Default.GroupAdd, "chat_drawer_new_group", ChatDrawerAction.NEW_GROUP, LiquidGlass.Violet),
    DrawerMenuEntry(Icons.Default.Campaign, "chat_drawer_new_channel", ChatDrawerAction.NEW_CHANNEL, LiquidGlass.Emerald),
    DrawerMenuEntry(Icons.Default.Bookmark, "chat_drawer_saved", ChatDrawerAction.SAVED, LiquidGlass.Amber),
    DrawerMenuEntry(Icons.Default.Settings, "chat_drawer_settings", ChatDrawerAction.SETTINGS, LiquidGlass.Rose),
)

@Composable
fun ChatDrawerContent(
    userName: String,
    userSubtitle: String,
    userAvatarUrl: String?,
    isOnline: Boolean,
    isOpen: Boolean,
    onAction: (ChatDrawerAction) -> Unit,
    modifier: Modifier = Modifier,
    uploadingAvatar: Boolean = false,
    onAvatarClick: (() -> Unit)? = null,
) {
    val isDark = LiquidTheme.isDark
    Box(
        modifier
            .fillMaxHeight()
            .width(312.dp)
            .drawBehind { drawDrawerBackdrop(isDark) },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            DrawerHeaderCard(
                name = userName,
                subtitle = userSubtitle,
                avatarUrl = userAvatarUrl,
                isOnline = isOnline,
                isOpen = isOpen,
                uploadingAvatar = uploadingAvatar,
                onAvatarClick = onAvatarClick,
            )

            Spacer(Modifier.height(14.dp))

            GlassCard(Modifier.fillMaxWidth(), radius = 22.dp) {
                Column(Modifier.padding(vertical = 6.dp)) {
                    drawerMenu.forEachIndexed { index, entry ->
                        DrawerMenuItem(
                            entry = entry,
                            isOpen = isOpen,
                            index = index + 1,
                            onClick = { onAction(entry.action) },
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            DrawerFooter(isOpen = isOpen)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDrawerBackdrop(isDark: Boolean) {
    drawRect(if (isDark) LiquidGlass.BgMidDark.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.42f))
    drawCircle(
        brush = Brush.radialGradient(
            listOf(LiquidGlass.Blue.copy(alpha = if (isDark) 0.28f else 0.14f), Color.Transparent),
            center = Offset(size.width * 0.15f, size.height * 0.12f),
            radius = size.width * 0.75f,
        ),
        radius = size.width * 0.75f,
        center = Offset(size.width * 0.15f, size.height * 0.12f),
    )
    drawCircle(
        brush = Brush.radialGradient(
            listOf(LiquidGlass.Cyan.copy(alpha = if (isDark) 0.18f else 0.10f), Color.Transparent),
            center = Offset(size.width * 0.9f, size.height * 0.55f),
            radius = size.width * 0.55f,
        ),
        radius = size.width * 0.55f,
        center = Offset(size.width * 0.9f, size.height * 0.55f),
    )
    drawCircle(
        brush = Brush.radialGradient(
            listOf(LiquidGlass.Violet.copy(alpha = if (isDark) 0.14f else 0.07f), Color.Transparent),
            center = Offset(size.width * 0.5f, size.height * 0.92f),
            radius = size.width * 0.45f,
        ),
        radius = size.width * 0.45f,
        center = Offset(size.width * 0.5f, size.height * 0.92f),
    )
}

@Composable
private fun DrawerHeaderCard(
    name: String,
    subtitle: String,
    avatarUrl: String?,
    isOnline: Boolean,
    isOpen: Boolean,
    uploadingAvatar: Boolean = false,
    onAvatarClick: (() -> Unit)? = null,
) {
    val slide by animateFloatAsState(
        targetValue = if (isOpen) 0f else -28f,
        animationSpec = tween(380, easing = FastOutSlowInEasing),
        label = "drawerHeaderSlide",
    )
    val alpha by animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "drawerHeaderAlpha",
    )

    Box(
        Modifier
            .fillMaxWidth()
            .offset { IntOffset(slide.roundToInt(), 0) }
            .alpha(alpha),
    ) {
        GlassCard(Modifier.fillMaxWidth(), radius = 24.dp) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(LiquidGlass.Blue.copy(alpha = 0.22f), Color.Transparent),
                                center = Offset(size.width * 0.85f, size.height * 0.2f),
                                radius = size.width * 0.45f,
                            ),
                            radius = size.width * 0.45f,
                            center = Offset(size.width * 0.85f, size.height * 0.2f),
                        )
                    }
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.then(
                            if (onAvatarClick != null) {
                                Modifier.clip(CircleShape).clickable(enabled = !uploadingAvatar) { onAvatarClick() }
                            } else Modifier,
                        ),
                    ) {
                        ChatAvatar(
                            name.ifBlank { "?" },
                            online = false,
                            size = 62.dp,
                            showPresence = false,
                            avatarUrl = avatarUrl,
                        )
                        if (uploadingAvatar) {
                            Box(
                                Modifier
                                    .matchParentSize()
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                )
                            }
                        } else if (onAvatarClick != null) {
                            Box(
                                Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(LiquidTheme.bgMid)
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(LiquidGlass.Blue),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Default.PhotoCamera,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                            }
                        }
                        if (isOnline && !uploadingAvatar && onAvatarClick == null) {
                            Box(
                                Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(LiquidTheme.bgMid)
                                    .padding(2.dp),
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(LiquidGlass.Emerald),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            name.ifBlank { "—" },
                            color = LiquidTheme.text,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1,
                        )
                        if (subtitle.isNotBlank()) {
                            Spacer(Modifier.height(3.dp))
                            Text(
                                subtitle,
                                color = if (isOnline) LiquidGlass.Blue else LiquidTheme.textMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerMenuItem(
    entry: DrawerMenuEntry,
    isOpen: Boolean,
    index: Int,
    onClick: () -> Unit,
) {
    val delay = 60 + index * 45
    val slide by animateFloatAsState(
        targetValue = if (isOpen) 0f else -36f,
        animationSpec = tween(340, delayMillis = delay, easing = FastOutSlowInEasing),
        label = "drawerItemSlide$index",
    )
    val alpha by animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = tween(280, delayMillis = delay, easing = FastOutSlowInEasing),
        label = "drawerItemAlpha$index",
    )

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(120),
        label = "drawerItemScale$index",
    )

    Row(
        Modifier
            .fillMaxWidth()
            .offset { IntOffset(slide.roundToInt(), 0) }
            .alpha(alpha)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .clickable(interactionSource = interaction, indication = ripple(bounded = true), onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(entry.accent, entry.accent.copy(alpha = 0.72f))))
                .border(
                    1.dp,
                    Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.15f)),
                    ),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(entry.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            localized(entry.labelKey),
            color = LiquidTheme.text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(entry.accent.copy(alpha = 0.35f)),
        )
    }
}

@Composable
private fun DrawerFooter(isOpen: Boolean) {
    val alpha by animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = tween(420, delayMillis = 380, easing = FastOutSlowInEasing),
        label = "drawerFooterAlpha",
    )
    Box(
        Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .padding(top = 8.dp, bottom = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .liquidGlassThemed(radius = 50.dp)
                .padding(horizontal = 16.dp, vertical = 9.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(LiquidGlass.Blue, LiquidGlass.Cyan))),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Vazifa Chat",
                    color = LiquidTheme.text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
