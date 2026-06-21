package uz.vazifa.app.presentation.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.presentation.theme.VazifaColors
import uz.vazifa.app.presentation.theme.liquidGlassThemed

val BottomNavHeight = 84.dp

enum class AppTab(val route: String) {
    HOME("home"),
    TASKS("tasks"),
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
        listOf(Triple(AppTab.HOME, Icons.Default.Dashboard, "Asosiy"), Triple(AppTab.TASKS, Icons.Default.Assignment, "Vazifalar"), Triple(AppTab.PROFILE, Icons.Default.Person, "Profil"))
    } else {
        listOf(Triple(AppTab.TASKS, Icons.Default.Assignment, "Vazifalar"), Triple(AppTab.PROFILE, Icons.Default.Person, "Profil"))
    }

    Box(modifier.fillMaxWidth().height(BottomNavHeight).padding(horizontal = 20.dp, vertical = 10.dp)) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).liquidGlassThemed(radius = 50.dp).padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { (tab, icon, label) ->
                val active = selected == tab
                val scale by animateFloatAsState(if (active) 1.15f else 1f, spring(Spring.DampingRatioMediumBouncy))
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
                                Modifier.size(36.dp).clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(VazifaColors.Primary, VazifaColors.PrimaryLight))),
                            )
                        }
                        Icon(icon, null, tint = if (active) Color.White else LiquidTheme.textMuted, modifier = Modifier.scale(scale).size(20.dp))
                    }
                    Text(label, fontSize = 9.sp, color = if (active) VazifaColors.Primary else LiquidTheme.textMuted, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
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
