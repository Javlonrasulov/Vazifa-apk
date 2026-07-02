package uz.vazifa.app.presentation.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import uz.vazifa.app.domain.model.Task
import uz.vazifa.app.domain.model.TaskStatus
import uz.vazifa.app.presentation.components.*
import uz.vazifa.app.presentation.components.localizedStatus
import uz.vazifa.app.presentation.theme.LiquidBackground
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.presentation.theme.VazifaColors
import uz.vazifa.app.presentation.theme.liquidGlassThemed
import uz.vazifa.app.util.TaskDeadlineCountdown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartmentTasksScreen(
    onTaskClick: (String) -> Unit,
    viewModel: DepartmentTasksViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    VazifaTabScaffold(
        title = localized("nav_dept_tasks"),
        actions = { VazifaHeaderActions() },
    ) { padding ->
        LiquidBackground(Modifier.fillMaxSize()) {
            VazifaScreenBox(padding) {
                var selectedTab by remember { mutableIntStateOf(0) }
                val filtered = remember(state.tasks, selectedTab) {
                    state.tasks.filter { task ->
                        val allDone = task.assignments.isNotEmpty() &&
                            task.assignments.all {
                                it.status == TaskStatus.COMPLETED.key || it.status == TaskStatus.CANCELLED.key
                            }
                        if (selectedTab == 0) !allDone else allDone
                    }
                }

                Column(Modifier.fillMaxSize()) {
                    if (state.visibleDepartments.isNotEmpty()) {
                        Text(
                            state.visibleDepartments.joinToString(", "),
                            color = LiquidTheme.textMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    PrimaryTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = LiquidGlass.Blue,
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text(localized("tasks_tab_new")) },
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text(localized("tasks_tab_completed")) },
                        )
                    }
                    LazyColumn(
                        contentPadding = tabListContentPadding(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (state.loading) {
                            item {
                                Text(localized("com_loading"), color = LiquidTheme.textMuted, modifier = Modifier.padding(24.dp))
                            }
                        } else if (filtered.isEmpty()) {
                            item {
                                Text(localized("dash_empty"), color = LiquidTheme.textMuted, modifier = Modifier.padding(vertical = 24.dp))
                            }
                        } else {
                            items(filtered, key = { it.id }) { task ->
                                DepartmentTaskRow(task = task, onClick = { onTaskClick(task.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DepartmentTaskRow(task: Task, onClick: () -> Unit) {
    val creator = task.createdBy
    val creatorDept = creator?.department?.takeIf { it.isNotBlank() }
    val allDone = task.assignments.isNotEmpty() &&
        task.assignments.all {
            it.status == TaskStatus.COMPLETED.key || it.status == TaskStatus.CANCELLED.key
        }
    val chipLabel = if (allDone) localizedStatus(TaskStatus.COMPLETED.key) else localizedStatus(
        task.assignments.firstOrNull()?.status ?: task.status,
    )
    val chipColor = if (allDone) VazifaColors.Success else VazifaColors.Warning

    Column(
        Modifier
            .fillMaxWidth()
            .liquidGlassThemed()
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
            Column(Modifier.weight(1f)) {
                Text(task.title, color = LiquidTheme.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                AssistChip(
                    onClick = {},
                    label = { Text(chipLabel, fontSize = 10.sp) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = chipColor.copy(alpha = 0.14f),
                        labelColor = chipColor,
                    ),
                    border = null,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        creator?.let {
            Text(
                "${localized("dept_task_from")}: ${it.fullName}${creatorDept?.let { d -> " ($d)" } ?: ""}",
                color = LiquidTheme.textMuted,
                fontSize = 12.sp,
            )
        }

        task.assignments.forEach { assignment ->
            val assignee = assignment.assignee
            val assigneeDept = assignee?.department?.takeIf { it.isNotBlank() }
            val name = assignee?.fullName ?: assignment.assigneeId
            Text(
                "${localized("dept_task_to")}: $name${assigneeDept?.let { d -> " ($d)" } ?: ""} — ${localizedStatus(assignment.status)}",
                color = LiquidTheme.text,
                fontSize = 12.sp,
            )
            assignment.completedAt?.takeIf { it.isNotBlank() }?.let { completedAt ->
                Text(
                    "${localized("task_completed_at")}: ${TaskDeadlineCountdown.formatDisplay(completedAt)}",
                    color = LiquidTheme.textMuted,
                    fontSize = 11.sp,
                )
            }
        }

        Text(
            "${localized("task_deadline")}: ${TaskDeadlineCountdown.formatDisplay(task.deadlineAt)}",
            color = LiquidTheme.textMuted,
            fontSize = 12.sp,
        )
        TaskDeadlineCountdown.durationBetween(task.startAt, task.deadlineAt)?.let { duration ->
            val totalMinutes = duration.days * 24 * 60 + duration.hours * 60 + duration.minutes
            if (totalMinutes > 0) {
                Text(
                    "${localized("task_time_given")}: ${formatTaskDuration(duration.days, duration.hours, duration.minutes)}",
                    color = LiquidTheme.textMuted,
                    fontSize = 12.sp,
                )
            }
        }
        if (!allDone) {
            TaskCountdownText(deadlineAt = task.deadlineAt, status = task.assignments.firstOrNull()?.status)
        }
    }
}
