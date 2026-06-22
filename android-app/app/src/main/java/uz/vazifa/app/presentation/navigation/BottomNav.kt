package uz.vazifa.app.presentation.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.vazifa.app.presentation.components.localized
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.presentation.theme.liquidGlassThemed

val BottomNavHeight = 84.dp

enum class AppTab(val route: String) {
    HOME("home"),
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

    Box(modifier.fillMaxWidth().height(BottomNavHeight).padding(horizontal = 20.dp, vertical = 10.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
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
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEach { (tab, icon, label) ->
                    val active = selected == tab
                    val scale by animateFloatAsState(
                        targetValue = if (active) 1.12f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "navScale",
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50.dp))
                            .clickable { onSelect(tab) }
                            .padding(vertical = 6.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (active) {
                                Box(
                                    Modifier
                                        .size(38.dp)
                                        .scale(scale)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(LiquidGlass.Blue, LiquidGlass.Cyan),
                                            ),
                                        )
                                        .drawBehind {
                                            drawCircle(
                                                brush = Brush.radialGradient(
                                                    listOf(LiquidGlass.Blue.copy(0.45f), Color.Transparent),
                                                    radius = size.maxDimension,
                                                ),
                                                radius = size.maxDimension * 1.4f,
                                            )
                                        },
                                )
                            }
                            Icon(
                                icon,
                                contentDescription = label,
                                tint = if (active) Color.White else inactiveTint,
                                modifier = Modifier.scale(if (active) 1f else scale).size(20.dp),
                            )
                        }
                        Text(
                            label,
                            fontSize = 9.sp,
                            color = if (active) LiquidGlass.BlueLight else inactiveTint,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val NOTIFICATION_GATE = "notification_gate"
    const val MAIN = "main"
    const val TASK_DETAIL = "task/{taskId}"
    const val CREATE_TASK = "create_task"
    fun taskDetail(id: String) = "task/$id"
}
