package uz.vazifa.app.presentation.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
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
import uz.vazifa.app.domain.model.canCreatorManage
import uz.vazifa.app.domain.model.isCreator
import uz.vazifa.app.domain.model.myAssignment
import uz.vazifa.app.presentation.components.*
import uz.vazifa.app.presentation.theme.LiquidGlassDropdownItem
import uz.vazifa.app.presentation.theme.LiquidGlassDropdownMenu
import uz.vazifa.app.presentation.theme.LiquidBackground
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.presentation.theme.VazifaColors
import uz.vazifa.app.presentation.theme.liquidGlassThemed

@Composable
fun DirectorDashboardScreen(
    onTaskClick: (String) -> Unit,
    onCreateTask: () -> Unit,
    onEditTask: (String) -> Unit = {},
    onSectionClick: (DashboardSection) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var taskToDelete by remember { mutableStateOf<String?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) { viewModel.load() }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.load()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    taskToDelete?.let { taskId ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text(localized("task_delete")) },
            text = { Text(localized("task_delete_confirm")) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTask(taskId)
                    taskToDelete = null
                }) { Text(localized("task_delete")) }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) { Text(localized("com_cancel")) }
            },
        )
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
                LazyColumn(contentPadding = tabListContentPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        val canManage = task.canCreatorManage(state.currentUserId)
                        TaskRow(
                            task = task,
                            currentUserId = state.currentUserId,
                            canManage = canManage,
                            onClick = { onTaskClick(task.id) },
                            onEdit = if (canManage) ({ onEditTask(task.id) }) else null,
                            onDelete = if (canManage) ({ taskToDelete = task.id }) else null,
                        )
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
fun TaskRow(
    task: Task,
    onClick: () -> Unit,
    currentUserId: String? = null,
    canManage: Boolean = false,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val myAssignment = task.myAssignment(currentUserId)
    val isCreator = task.isCreator(currentUserId)
    val status = myAssignment?.status?.takeIf { it.isNotBlank() }
        ?: task.assignments.firstOrNull()?.status?.takeIf { it.isNotBlank() }
        ?: task.status.takeIf { it.isNotBlank() }
    val chipLabel = status?.let { localizedStatus(it) } ?: task.priority
    val chipColor = when (status) {
        "completed" -> VazifaColors.Success
        "cancelled" -> VazifaColors.Gray
        "in_progress", "in_review" -> VazifaColors.Primary
        else -> VazifaColors.Warning
    }
    val isSent = isCreator
    val isReceived = !isCreator && myAssignment != null
    val directionIcon = when {
        isSent -> Icons.AutoMirrored.Filled.CallMade
        isReceived -> Icons.AutoMirrored.Filled.CallReceived
        else -> Icons.Default.Assignment
    }
    val directionLabel = when {
        isSent -> localized("task_sent")
        isReceived -> localized("task_received")
        else -> localized("task_detail")
    }
    val directionGradient = when {
        isSent -> listOf(LiquidGlass.Blue, LiquidGlass.Cyan)
        isReceived -> listOf(LiquidGlass.Emerald, VazifaColors.Success)
        else -> listOf(LiquidGlass.Blue.copy(alpha = 0.85f), LiquidGlass.Cyan.copy(alpha = 0.85f))
    }
    var showActions by remember { mutableStateOf(false) }

    Row(
        Modifier
            .fillMaxWidth()
            .liquidGlassThemed()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(directionGradient))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                directionIcon,
                contentDescription = directionLabel,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(
            Modifier
                .weight(1f)
                .clickable(onClick = onClick),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    task.title,
                    color = LiquidTheme.text,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                )
                TaskStatusBadge(chipLabel, chipColor)
            }
            if (!isCreator && myAssignment != null) {
                task.createdBy?.let {
                    Text(
                        "${localized("task_from")}: ${it.fullName}",
                        color = LiquidTheme.textMuted,
                        fontSize = 12.sp,
                    )
                }
            } else if (isCreator && task.assignments.isNotEmpty()) {
                val names = task.assignments.mapNotNull { it.assignee?.fullName }.joinToString(", ")
                if (names.isNotBlank()) {
                    Text(
                        "${localized("task_assignees")}: $names",
                        color = LiquidTheme.textMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                    )
                }
            }
            TaskCountdownText(
                deadlineAt = task.deadlineAt,
                status = status,
            )
        }
        if (canManage) {
            Box {
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(LiquidGlass.Blue.copy(alpha = 0.10f))
                        .border(1.dp, LiquidGlass.Blue.copy(alpha = 0.14f), CircleShape)
                        .clickable { showActions = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = localized("com_settings"),
                        tint = LiquidTheme.textMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
                LiquidGlassDropdownMenu(
                    expanded = showActions,
                    onDismissRequest = { showActions = false },
                ) {
                    LiquidGlassDropdownItem(
                        text = localized("task_edit"),
                        selected = false,
                        onClick = {
                            showActions = false
                            onEdit?.invoke()
                        },
                    )
                    LiquidGlassDropdownItem(
                        text = localized("task_delete"),
                        selected = false,
                        onClick = {
                            showActions = false
                            onDelete?.invoke()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskStatusBadge(label: String, color: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(LiquidGlass.RadiusChip))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.22f), RoundedCornerShape(LiquidGlass.RadiusChip))
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Text(
            label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}
