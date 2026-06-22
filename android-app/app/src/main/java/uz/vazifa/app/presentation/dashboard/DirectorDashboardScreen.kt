package uz.vazifa.app.presentation.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import uz.vazifa.app.domain.model.DashboardStats
import uz.vazifa.app.domain.model.Task
import uz.vazifa.app.presentation.components.*
import uz.vazifa.app.presentation.theme.LiquidBackground
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.presentation.theme.VazifaColors
import uz.vazifa.app.presentation.theme.liquidGlassThemed

@Composable
fun DirectorDashboardScreen(
    onTaskClick: (String) -> Unit,
    onCreateTask: () -> Unit,
    onSectionClick: (DashboardSection) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.load()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    VazifaTabScaffold(
        title = localized("nav_home"),
        actions = {
            VazifaHeaderActions(
                extra = {
                    GlassHeaderIconButton(
                        onClick = onCreateTask,
                        icon = Icons.Default.Add,
                        tint = LiquidGlass.Blue,
                        contentDescription = localized("task_create"),
                    )
                },
            )
        },
    ) { padding ->
        LiquidBackground(Modifier.fillMaxSize()) {
            VazifaScreenBox(padding) {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { StatsGrid(state.stats ?: DashboardStats(), onSectionClick) }
                    item {
                        Text(
                            localized("dash_recent_tasks"),
                            color = LiquidTheme.text,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                        )
                    }
                    items(state.tasks.take(5), key = { it.id }) { task ->
                        TaskRow(task, onClick = { onTaskClick(task.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsGrid(stats: DashboardStats, onSectionClick: (DashboardSection) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                localized("dash_employees"),
                "${stats.totalEmployees}",
                Icons.Default.People,
                Modifier.weight(1f),
                onClick = { onSectionClick(DashboardSection.EMPLOYEES) },
            )
            StatCard(
                localized("dash_active"),
                "${stats.activeTasks}",
                Icons.Default.Assignment,
                Modifier.weight(1f),
                onClick = { onSectionClick(DashboardSection.ACTIVE) },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                localized("dash_completed"),
                "${stats.completedTasks}",
                Icons.Default.CheckCircle,
                Modifier.weight(1f),
                VazifaColors.Success,
                onClick = { onSectionClick(DashboardSection.COMPLETED) },
            )
            StatCard(
                localized("dash_overdue"),
                "${stats.overdueTasks}",
                Icons.Default.Warning,
                Modifier.weight(1f),
                VazifaColors.Danger,
                onClick = { onSectionClick(DashboardSection.OVERDUE) },
            )
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier,
    color: Color = VazifaColors.Primary,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .clickable(onClick = onClick)
            .liquidGlassThemed()
            .padding(14.dp),
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(label, color = LiquidTheme.textMuted, fontSize = 11.sp)
        Text(value, color = LiquidTheme.text, fontWeight = FontWeight.Bold, fontSize = 22.sp)
    }
}

@Composable
fun TaskRow(task: Task, onClick: () -> Unit) {
    val status = task.assignments.firstOrNull()?.status
    val chipLabel = status?.let { localized(statusLabelKey(it)) } ?: task.priority
    val chipColor = when (status) {
        "completed" -> VazifaColors.Success
        "cancelled" -> VazifaColors.Gray
        "in_progress", "in_review" -> VazifaColors.Primary
        else -> VazifaColors.Warning
    }
    Row(
        Modifier
            .fillMaxWidth()
            .liquidGlassThemed()
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(LiquidGlass.Blue.copy(alpha = 0.85f), LiquidGlass.Cyan.copy(alpha = 0.85f)),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Assignment, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(task.title, color = LiquidTheme.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            TaskCountdownText(
                deadlineAt = task.deadlineAt,
                status = status,
            )
        }
        AssistChip(
            onClick = {},
            label = { Text(chipLabel, fontSize = 10.sp) },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = chipColor.copy(alpha = 0.14f),
                labelColor = chipColor,
            ),
            border = null,
        )
    }
}
