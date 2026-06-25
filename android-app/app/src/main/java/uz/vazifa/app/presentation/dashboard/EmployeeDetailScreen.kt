package uz.vazifa.app.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import uz.vazifa.app.data.repository.TaskRepository
import uz.vazifa.app.domain.model.User
import uz.vazifa.app.domain.model.isTaskAssignable
import uz.vazifa.app.presentation.components.*
import uz.vazifa.app.presentation.theme.LiquidBackground
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.presentation.theme.VazifaColors
import uz.vazifa.app.presentation.theme.liquidGlassThemed
import uz.vazifa.app.util.EmployeeStats
import uz.vazifa.app.util.EmployeeStatsCalculator
import uz.vazifa.app.util.EmployeeTaskItem
import javax.inject.Inject

data class EmployeeDetailUiState(
    val loading: Boolean = false,
    val employee: User? = null,
    val stats: EmployeeStats = EmployeeStats(),
    val tasks: List<EmployeeTaskItem> = emptyList(),
)

@HiltViewModel
class EmployeeDetailViewModel @Inject constructor(
    private val repo: TaskRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val employeeId: String = savedStateHandle.get<String>("employeeId").orEmpty()
    private val _state = MutableStateFlow(EmployeeDetailUiState())
    val state = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            runCatching {
                val employee = repo.getContacts()
                    .firstOrNull { it.id == employeeId && it.isTaskAssignable() }
                val tasks = repo.getTasks()
                val (stats, items) = EmployeeStatsCalculator.compute(employeeId, tasks)
                _state.update {
                    it.copy(employee = employee, stats = stats, tasks = items, loading = false)
                }
            }.onFailure {
                _state.update { it.copy(loading = false) }
            }
        }
    }
}

@Composable
fun EmployeeDetailScreen(
    onBack: () -> Unit,
    onTaskClick: (String) -> Unit,
    onAssignTask: (String) -> Unit,
    viewModel: EmployeeDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.load()
        while (true) {
            delay(30_000)
            viewModel.load()
        }
    }

    VazifaTabScaffold(
        title = state.employee?.fullName ?: localized("dash_employees"),
        onBack = onBack,
        actions = {
            state.employee?.let { employee ->
                GlassHeaderIconButton(
                    onClick = { onAssignTask(employee.id) },
                    icon = Icons.Default.Add,
                    tint = LiquidGlass.Blue,
                    contentDescription = localized("task_create_for_employee"),
                )
            }
            VazifaHeaderActions()
        },
    ) { padding ->
        LiquidBackground(Modifier.fillMaxSize()) {
            VazifaScreenBox(padding) {
                when {
                    state.loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = LiquidGlass.Blue)
                        }
                    }
                    state.employee == null -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(localized("dash_empty"), color = LiquidTheme.textMuted)
                        }
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            item { EmployeeProfileCard(state.employee!!) }
                            item { EmployeeStatsGrid(state.stats) }
                            item {
                                Text(
                                    localized("emp_task_history"),
                                    color = LiquidTheme.text,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                            if (state.tasks.isEmpty()) {
                                item {
                                    Text(
                                        localized("emp_no_tasks"),
                                        color = LiquidTheme.textMuted,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(vertical = 16.dp),
                                    )
                                }
                            } else {
                                items(state.tasks, key = { it.assignment.id }) { item ->
                                    EmployeeTaskRow(
                                        item = item,
                                        onClick = { onTaskClick(item.task.id) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmployeeProfileCard(employee: User) {
    Row(
        Modifier
            .fillMaxWidth()
            .liquidGlassThemed()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(56.dp)) {
            Box(
                Modifier
                    .matchParentSize()
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(LiquidGlass.Blue, LiquidGlass.Cyan))),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    employeeInitials(employee.fullName),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
            }
            Box(
                Modifier
                    .size(13.dp)
                    .align(Alignment.BottomEnd),
            ) {
                EmployeePresenceDot(employee, dotSize = 13.dp)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(employee.fullName, color = LiquidTheme.text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            EmployeePresenceStatus(employee)
            employee.position?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = LiquidTheme.textMuted, fontSize = 13.sp)
            }
            employee.department?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = LiquidTheme.textMuted, fontSize = 12.sp)
            }
            employee.phone?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = LiquidGlass.BlueLight, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun EmployeeStatsGrid(stats: EmployeeStats) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            EmpStatCard(localized("emp_total"), "${stats.totalTasks}", Icons.Default.Assignment, Modifier.weight(1f))
            EmpStatCard(localized("dash_completed"), "${stats.completedTasks}", Icons.Default.CheckCircle, Modifier.weight(1f), VazifaColors.Success)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            EmpStatCard(localized("dash_active"), "${stats.activeTasks}", Icons.Default.PlayArrow, Modifier.weight(1f))
            EmpStatCard(localized("dash_overdue"), "${stats.overdueTasks}", Icons.Default.Warning, Modifier.weight(1f), VazifaColors.Danger)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            EmpStatCard(localized("emp_on_time"), "${stats.onTimeRate}%", Icons.Default.Schedule, Modifier.weight(1f), VazifaColors.Success)
            EmpStatCard(localized("emp_completion"), "${stats.completionRate}%", Icons.Default.TrendingUp, Modifier.weight(1f), VazifaColors.Primary)
        }
    }
}

@Composable
private fun EmpStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier,
    color: Color = VazifaColors.Primary,
) {
    Column(modifier.liquidGlassThemed().padding(14.dp)) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(8.dp))
        Text(label, color = LiquidTheme.textMuted, fontSize = 11.sp)
        Text(value, color = LiquidTheme.text, fontWeight = FontWeight.Bold, fontSize = 20.sp)
    }
}

@Composable
private fun EmployeeTaskRow(item: EmployeeTaskItem, onClick: () -> Unit) {
    val overdue = EmployeeStatsCalculator.isAssignmentOverdue(item)
    val statusColor = when {
        item.assignment.status == "completed" -> VazifaColors.Success
        overdue -> VazifaColors.Danger
        else -> VazifaColors.Primary
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
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.task.title, color = LiquidTheme.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            TaskCountdownText(
                deadlineAt = item.task.deadlineAt,
                status = item.assignment.status,
            )
        }
        AssistChip(
            onClick = {},
            label = {
                Text(
                    localizedStatus(item.assignment.status),
                    fontSize = 10.sp,
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = statusColor.copy(alpha = 0.14f),
                labelColor = statusColor,
            ),
            border = null,
        )
    }
}

private fun employeeInitials(name: String): String =
    name.trim().split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
