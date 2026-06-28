package uz.vazifa.app.presentation.navigation

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.vazifa.app.presentation.components.localized
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidTheme

val DockBarHeight = 72.dp
val DockBottomMargin = 20.dp
val DockHorizontalMargin = 16.dp
val BottomNavHeight = DockBarHeight + DockBottomMargin

private val dockShape = RoundedCornerShape(50.dp)
private val tabActiveShape = RoundedCornerShape(50.dp)
private val tabActiveHeight = 56.dp
private val tabPaddingV = 6.dp

enum class AppTab(val route: String) {
    HOME("home"),
    EMPLOYEES("employees"),
    TASKS("tasks"),
    DEPT_TASKS("dept_tasks"),
    CHAT("chat"),
    CREATE("create"),
    PROFILE("profile"),
}

private data class DockTab(
    val tab: AppTab,
    val icon: ImageVector,
    val label: String,
)

@Composable
fun VazifaBottomNav(
    selected: AppTab,
    chatUnreadCount: Int,
    onSelect: (AppTab) -> Unit,
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = listOf(
        DockTab(AppTab.HOME, Icons.Default.Dashboard, localized("nav_home")),
        DockTab(AppTab.TASKS, Icons.Default.Assignment, localized("nav_tasks")),
        DockTab(AppTab.CHAT, Icons.Default.Chat, localized("nav_chat")),
        DockTab(AppTab.PROFILE, Icons.Default.Person, localized("nav_profile")),
    )

    val isDark = LiquidTheme.isDark
    val inactiveTint = if (isDark) LiquidTheme.textMuted else Color(0xFF475569)
    val navigableSelected = selected.takeIf { it != AppTab.CREATE && it != AppTab.EMPLOYEES && it != AppTab.DEPT_TASKS }

    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = DockHorizontalMargin)
            .padding(bottom = DockBottomMargin),
    ) {
        Box(Modifier.fillMaxWidth().height(DockBarHeight)) {
            Box(
                Modifier
                    .matchParentSize()
                    .shadow(
                        elevation = 32.dp,
                        shape = dockShape,
                        ambientColor = if (isDark) LiquidGlass.Blue.copy(alpha = 0.18f) else Color(0xFF2563EB).copy(alpha = 0.10f),
                        spotColor = if (isDark) Color.Black.copy(alpha = 0.45f) else Color(0xFF0F172A).copy(alpha = 0.12f),
                    )
                    .shadow(
                        elevation = 8.dp,
                        shape = dockShape,
                        ambientColor = Color.Transparent,
                        spotColor = if (isDark) Color.Black.copy(alpha = 0.55f) else Color(0xFF0F172A).copy(alpha = 0.18f),
                    )
                    .clip(dockShape)
                    .liquidGlassDockBlur()
                    .drawBehind { drawLiquidGlassDockLayers(isDark) }
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = if (isDark) 0.42f else 0.92f),
                                LiquidGlass.Blue.copy(alpha = if (isDark) 0.22f else 0.14f),
                                LiquidGlass.Cyan.copy(alpha = if (isDark) 0.10f else 0.08f),
                                Color.White.copy(alpha = if (isDark) 0.12f else 0.55f),
                            ),
                        ),
                        shape = dockShape,
                    ),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(DockBarHeight)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                tabs.take(2).forEach { dockTab ->
                    DockNavItem(
                        icon = dockTab.icon,
                        label = dockTab.label,
                        active = navigableSelected == dockTab.tab,
                        inactiveTint = inactiveTint,
                        isDark = isDark,
                        badgeCount = if (dockTab.tab == AppTab.CHAT) chatUnreadCount else 0,
                        onClick = { onSelect(dockTab.tab) },
                        modifier = Modifier.weight(1f),
                    )
                }

                CreateDockButton(onClick = onCreateClick)

                tabs.drop(2).forEach { dockTab ->
                    DockNavItem(
                        icon = dockTab.icon,
                        label = dockTab.label,
                        active = navigableSelected == dockTab.tab,
                        inactiveTint = inactiveTint,
                        isDark = isDark,
                        badgeCount = if (dockTab.tab == AppTab.CHAT) chatUnreadCount else 0,
                        onClick = { onSelect(dockTab.tab) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateDockButton(onClick: () -> Unit) {
    val isDark = LiquidTheme.isDark
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "createScale",
    )

    Box(
        Modifier
            .width(52.dp)
            .padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .scale(scale)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(
                                LiquidGlass.Blue.copy(alpha = if (isDark) 0.40f else 0.28f),
                                Color.Transparent,
                            ),
                            radius = size.minDimension * 0.9f,
                        ),
                    )
                }
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    ambientColor = LiquidGlass.Blue.copy(alpha = 0.30f),
                    spotColor = LiquidGlass.Blue.copy(alpha = 0.20f),
                )
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            LiquidGlass.BlueLight,
                            LiquidGlass.Blue,
                            LiquidGlass.Cyan.copy(alpha = 0.85f),
                        ),
                    ),
                )
                .border(
                    1.5.dp,
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.70f),
                            Color.White.copy(alpha = 0.20f),
                            LiquidGlass.Cyan.copy(alpha = 0.35f),
                        ),
                    ),
                    CircleShape,
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = false, radius = 28.dp),
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = localized("nav_create"),
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

@Composable
private fun DockNavItem(
    icon: ImageVector,
    label: String,
    active: Boolean,
    inactiveTint: Color,
    isDark: Boolean,
    badgeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val iconScale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.88f
            active -> 1.05f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "dockIconScale",
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (active) 1f else 0.92f,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "dockLabelAlpha",
    )
    val iconTint by animateColorAsState(
        targetValue = if (active) Color.White else inactiveTint,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "dockIconTint",
    )
    val labelColor by animateColorAsState(
        targetValue = if (active) Color.White else inactiveTint,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "dockLabelColor",
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
                .height(tabActiveHeight)
                .clip(tabActiveShape)
                .then(
                    if (active) {
                        Modifier
                            .drawBehind {
                                drawRoundRect(
                                    brush = Brush.radialGradient(
                                        listOf(
                                            LiquidGlass.Blue.copy(alpha = if (isDark) 0.35f else 0.22f),
                                            Color.Transparent,
                                        ),
                                        center = Offset(size.width / 2f, size.height),
                                        radius = size.width * 0.8f,
                                    ),
                                    cornerRadius = CornerRadius(size.height / 2f),
                                )
                            }
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        if (isDark) Color.White.copy(alpha = 0.22f) else LiquidGlass.Blue.copy(alpha = 0.88f),
                                        if (isDark) LiquidGlass.Blue.copy(alpha = 0.55f) else LiquidGlass.BlueLight.copy(alpha = 0.92f),
                                        if (isDark) LiquidGlass.Cyan.copy(alpha = 0.35f) else LiquidGlass.Cyan.copy(alpha = 0.75f),
                                    ),
                                ),
                            )
                            .border(
                                1.dp,
                                Brush.linearGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.55f),
                                        Color.White.copy(alpha = 0.15f),
                                    ),
                                ),
                                tabActiveShape,
                            )
                    } else {
                        Modifier
                    },
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = false, radius = 24.dp),
                    onClick = onClick,
                )
                .padding(vertical = tabPaddingV),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier.size(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        contentDescription = label,
                        tint = iconTint,
                        modifier = Modifier.scale(iconScale).size(22.dp),
                    )
                    if (badgeCount > 0) {
                        Box(Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-4).dp)) {
                            GlassChatBadge(count = badgeCount)
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    label,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = labelColor,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .alpha(labelAlpha),
                )
            }
        }
    }
}

@Composable
private fun GlassChatBadge(count: Int) {
    val isDark = LiquidTheme.isDark
    val infiniteTransition = rememberInfiniteTransition(label = "badgePulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "badgePulseScale",
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "badgeGlow",
    )

    val display = if (count > 99) "99+" else count.toString()
    val wide = display.length > 1

    Box(Modifier.scale(pulse)) {
        Box(
            Modifier
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(LiquidGlass.Rose.copy(alpha = glowAlpha), Color.Transparent),
                            radius = size.minDimension,
                        ),
                    )
                }
                .then(if (wide) Modifier.height(16.dp).padding(horizontal = 5.dp) else Modifier.size(16.dp))
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            if (isDark) LiquidGlass.Rose else Color(0xFFEF4444),
                            if (isDark) LiquidGlass.Rose.copy(alpha = 0.85f) else Color(0xFFF87171),
                        ),
                    ),
                )
                .border(
                    1.dp,
                    Color.White.copy(alpha = if (isDark) 0.45f else 0.75f),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                display,
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 9.sp,
            )
        }
    }
}

private fun Modifier.liquidGlassDockBlur(): Modifier {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        return this.graphicsLayer {
            renderEffect = android.graphics.RenderEffect
                .createBlurEffect(18f, 18f, android.graphics.Shader.TileMode.CLAMP)
                .asComposeRenderEffect()
        }
    }
    return this
}

private fun DrawScope.drawLiquidGlassDockLayers(isDark: Boolean) {
    val glassBase = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.14f),
                Color.White.copy(alpha = 0.08f),
                LiquidGlass.BgMidDark.copy(alpha = 0.55f),
            ),
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.82f),
                Color.White.copy(alpha = 0.65f),
                LiquidGlass.BgMidLight.copy(alpha = 0.45f),
            ),
        )
    }
    drawRect(glassBase)

    drawRoundRect(
        brush = Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = if (isDark) 0.22f else 0.55f),
                Color.Transparent,
                Color.Transparent,
            ),
            start = Offset(0f, 0f),
            end = Offset(size.width * 0.6f, size.height * 0.5f),
        ),
        topLeft = Offset(8f, 4f),
        size = Size(size.width - 16f, size.height * 0.45f),
        cornerRadius = CornerRadius(size.height / 2f),
    )

    drawCircle(
        brush = Brush.radialGradient(
            listOf(
                LiquidGlass.Blue.copy(alpha = if (isDark) 0.16f else 0.06f),
                Color.Transparent,
            ),
            center = Offset(size.width * 0.5f, size.height),
            radius = size.width * 0.45f,
        ),
        radius = size.width * 0.45f,
        center = Offset(size.width * 0.5f, size.height),
    )
}

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val NOTIFICATION_GATE = "notification_gate"
    const val MAIN = "main"
    const val TASK_DETAIL = "task/{taskId}"
    const val DASH_SECTION = "dash/{section}"
    const val EMPLOYEE_DETAIL = "employee/{employeeId}"
    const val CREATE_TASK = "create_task"
    const val EDIT_TASK = "edit_task/{taskId}"
    const val CHAT_CONVERSATION = "chat_conv/{peerId}?name={name}"
    const val CHAT_NEW = "chat_new"
    const val CHAT_CONTACTS = "chat_contacts"
    const val CHAT_CREATE_ROOM = "chat_create_room/{type}"
    const val ROOM_CONVERSATION = "room_conv/{roomId}"
    fun taskDetail(id: String) = "task/$id"
    fun editTask(id: String) = "edit_task/$id"
    fun dashSection(section: String) = "dash/$section"
    fun employeeDetail(id: String) = "employee/$id"
    fun chatConversation(peerId: String, name: String) =
        "chat_conv/$peerId?name=${android.net.Uri.encode(name)}"
    fun createRoom(type: String) = "chat_create_room/$type"
    fun roomConversation(roomId: String) = "room_conv/$roomId"
}
