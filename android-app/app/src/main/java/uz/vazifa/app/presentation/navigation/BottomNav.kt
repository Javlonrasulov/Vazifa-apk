package uz.vazifa.app.presentation.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.vazifa.app.presentation.components.localized
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.presentation.theme.liquidGlassThemed

val BottomNavHeight = 96.dp

enum class AppTab(val route: String) {
    HOME("home"),
    EMPLOYEES("employees"),
    TASKS("tasks"),
    CREATE("create"),
    PROFILE("profile"),
}

@Composable
fun VazifaBottomNav(
    selected: AppTab,
    isDirector: Boolean,
    onSelect: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = if (isDirector) {
        listOf(
            Triple(AppTab.HOME, Icons.Default.Dashboard, localized("nav_home")),
            Triple(AppTab.EMPLOYEES, Icons.Default.People, localized("nav_employees")),
            Triple(AppTab.TASKS, Icons.Default.Assignment, localized("nav_tasks")),
            Triple(AppTab.CREATE, Icons.Default.Add, localized("task_create")),
            Triple(AppTab.PROFILE, Icons.Default.Person, localized("nav_profile")),
        )
    } else {
        listOf(
            Triple(AppTab.TASKS, Icons.Default.Assignment, localized("nav_tasks")),
            Triple(AppTab.PROFILE, Icons.Default.Person, localized("nav_profile")),
        )
    }

    val isDark = LiquidTheme.isDark
    val inactiveTint = if (isDark) LiquidTheme.textMuted else LiquidGlass.TextDarkMuted

    val compact = tabs.size >= 5

    Box(modifier.fillMaxWidth().height(BottomNavHeight).padding(horizontal = 10.dp, vertical = 8.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .drawBehind {
                    if (isDark) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(LiquidGlass.Blue.copy(alpha = 0.22f), Color.Transparent),
                                center = Offset(size.width * 0.5f, size.height),
                                radius = size.width * 0.55f,
                            ),
                            radius = size.width * 0.55f,
                            center = Offset(size.width * 0.5f, size.height),
                        )
                    }
                },
        ) {
            Row(
                Modifier
                    .fillMaxSize()
                    .then(
                        if (isDark) {
                            Modifier
                                .clip(RoundedCornerShape(LiquidGlass.RadiusChip))
                                .background(LiquidTheme.bgMid.copy(alpha = 0.92f))
                                .border(1.dp, LiquidGlass.GlassDarkBorder, RoundedCornerShape(LiquidGlass.RadiusChip))
                        } else {
                            Modifier.liquidGlassThemed(radius = LiquidGlass.RadiusChip)
                        },
                    )
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEach { (tab, icon, label) ->
                    BottomNavItem(
                        icon = icon,
                        label = label,
                        active = selected == tab,
                        inactiveTint = inactiveTint,
                        compact = compact,
                        onClick = { onSelect(tab) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    active: Boolean,
    inactiveTint: Color,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayLabel = if (compact && label.contains(' ')) {
        label.replace(' ', '\n')
    } else {
        label
    }
    val labelSize = if (compact) 8.5.sp else 10.sp
    val labelLineHeight = if (compact) 10.sp else 12.sp
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val indicatorScale by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "indicatorScale",
    )
    val iconScale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.88f
            active -> 1.06f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "iconScale",
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (active) 0.55f else 0f,
        animationSpec = tween(durationMillis = 320),
        label = "glowAlpha",
    )
    val labelColor by animateColorAsState(
        targetValue = if (active) LiquidGlass.Blue else inactiveTint,
        animationSpec = tween(durationMillis = 280),
        label = "labelColor",
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (active) 1f else 0.78f,
        animationSpec = tween(durationMillis = 280),
        label = "labelAlpha",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = 28.dp),
                onClick = onClick,
            )
            .padding(vertical = 2.dp, horizontal = 1.dp),
    ) {
        Box(
            Modifier.size(if (compact) 32.dp else 36.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (glowAlpha > 0.01f) {
                Box(
                    Modifier
                        .size(40.dp)
                        .scale(indicatorScale)
                        .alpha(glowAlpha)
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(LiquidGlass.Blue.copy(alpha = 0.35f), Color.Transparent),
                                    radius = size.minDimension * 0.55f,
                                ),
                                radius = size.minDimension * 0.55f,
                            )
                        },
                )
            }
            if (indicatorScale > 0.01f) {
                Box(
                    Modifier
                        .size(34.dp)
                        .scale(indicatorScale)
                        .alpha(indicatorScale)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(LiquidGlass.Blue, LiquidGlass.Cyan),
                            ),
                        ),
                )
            }
            Icon(
                icon,
                contentDescription = label,
                tint = if (active) Color.White else inactiveTint,
                modifier = Modifier
                    .scale(iconScale)
                    .size(20.dp),
            )
        }
        Spacer(Modifier.height(1.dp))
        Text(
            displayLabel,
            fontSize = labelSize,
            lineHeight = labelLineHeight,
            maxLines = 2,
            softWrap = true,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Clip,
            color = labelColor,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(labelAlpha),
        )
    }
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
    fun taskDetail(id: String) = "task/$id"
    fun editTask(id: String) = "edit_task/$id"
    fun dashSection(section: String) = "dash/$section"
    fun employeeDetail(id: String) = "employee/$id"
}
