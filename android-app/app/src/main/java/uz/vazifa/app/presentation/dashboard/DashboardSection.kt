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
import uz.vazifa.app.data.repository.TaskRepository
import uz.vazifa.app.domain.model.Task
import uz.vazifa.app.domain.model.User
import uz.vazifa.app.domain.model.hasActiveAssignment
import uz.vazifa.app.domain.model.hasCompletedAssignment
import uz.vazifa.app.domain.model.isOverdue
import uz.vazifa.app.presentation.components.*
import uz.vazifa.app.presentation.theme.LiquidBackground
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.presentation.theme.VazifaColors
import uz.vazifa.app.presentation.theme.liquidGlassThemed
import javax.inject.Inject

enum class DashboardSection(val route: String) {
    EMPLOYEES("employees"),
    ACTIVE("active"),
    COMPLETED("completed"),
    OVERDUE("overdue"),
    ;

    fun titleKey(): String = when (this) {
        EMPLOYEES -> "dash_employees"
        ACTIVE -> "dash_active"
        COMPLETED -> "dash_completed"
        OVERDUE -> "dash_overdue"
    }

    companion object {
        fun fromRoute(value: String): DashboardSection? =
            entries.find { it.route == value }
    }
}

data class SectionStyle(
    val icon: ImageVector,
    val accent: Color,
    val gradient: List<Color>,
)

fun DashboardSection.style(): SectionStyle = when (this) {
    DashboardSection.EMPLOYEES -> SectionStyle(
        Icons.Default.People,
        VazifaColors.Primary,
        listOf(LiquidGlass.Blue, LiquidGlass.Cyan),
    )
    DashboardSection.ACTIVE -> SectionStyle(
        Icons.Default.Assignment,
        VazifaColors.PrimaryLight,
        listOf(LiquidGlass.Blue, LiquidGlass.BlueLight),
    )
    DashboardSection.COMPLETED -> SectionStyle(
        Icons.Default.CheckCircle,
        VazifaColors.Success,
        listOf(VazifaColors.Success, LiquidGlass.Emerald),
    )
    DashboardSection.OVERDUE -> SectionStyle(
        Icons.Default.Warning,
        VazifaColors.Danger,
        listOf(VazifaColors.Danger, LiquidGlass.Rose),
    )
}

data class DashboardSectionUiState(
    val loading: Boolean = false,
    val employees: List<User> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val selectedEmployeeIds: Set<String> = emptySet(),
)

@HiltViewModel
class DashboardSectionViewModel @Inject constructor(
    private val repo: TaskRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val section: DashboardSection = DashboardSection.fromRoute(
        savedStateHandle.get<String>("section").orEmpty(),
    ) ?: DashboardSection.ACTIVE

    private val _state = MutableStateFlow(DashboardSectionUiState())
    val state = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            runCatching {
                when (section) {
                    DashboardSection.EMPLOYEES -> {
                        val employees = repo.getContacts()
                            .filter { it.role == "employee" && it.login != "xodim1" }
                            .sortedBy { it.fullName }
                        _state.update { it.copy(employees = employees, loading = false) }
                    }
                    else -> {
                        val tasks = repo.getTasks().filter { task ->
                            when (section) {
                                DashboardSection.ACTIVE -> task.hasActiveAssignment()
                                DashboardSection.COMPLETED -> task.hasCompletedAssignment()
                                DashboardSection.OVERDUE -> task.isOverdue()
                                DashboardSection.EMPLOYEES -> false
                            }
                        }
                        _state.update { it.copy(tasks = tasks, loading = false) }
                    }
                }
            }.onFailure {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    fun toggleEmployee(id: String) {
        _state.update {
            val ids = it.selectedEmployeeIds.toMutableSet()
            if (ids.contains(id)) ids.remove(id) else ids.add(id)
            it.copy(selectedEmployeeIds = ids)
        }
    }

    fun selectAllEmployees() {
        _state.update { state ->
            state.copy(selectedEmployeeIds = state.employees.map { it.id }.toSet())
        }
    }

    fun clearSelection() {
        _state.update { it.copy(selectedEmployeeIds = emptySet()) }
    }
}

@Composable
fun DashboardSectionScreen(
    onBack: () -> Unit,
    onTaskClick: (String) -> Unit,
    onAssignTask: (Set<String>) -> Unit = {},
    onEmployeeClick: (String) -> Unit = {},
    viewModel: DashboardSectionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val section = viewModel.section
    val style = section.style()
    val isEmployees = section == DashboardSection.EMPLOYEES
    val count = when {
        isEmployees -> state.employees.size
        else -> state.tasks.size
    }
    val allSelected = isEmployees && state.employees.isNotEmpty() &&
        state.selectedEmployeeIds.size == state.employees.size

    LaunchedEffect(Unit) { viewModel.load() }

    VazifaTabScaffold(
        title = localized(section.titleKey()),
        onBack = onBack,
        actions = {
            if (isEmployees && state.employees.isNotEmpty()) {
                TextButton(onClick = {
                    if (allSelected) viewModel.clearSelection() else viewModel.selectAllEmployees()
                }) {
                    Text(
                        localized(if (allSelected) "emp_deselect_all" else "emp_select_all"),
                        color = LiquidGlass.Blue,
                        fontSize = 13.sp,
                    )
                }
            }
            VazifaHeaderActions()
        },
    ) { padding ->
        LiquidBackground(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                VazifaScreenBox(padding) {
                    when {
                        state.loading -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = LiquidGlass.Blue)
                            }
                        }
                        else -> {
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 16.dp,
                                    bottom = if (isEmployees && state.selectedEmployeeIds.isNotEmpty()) 88.dp else 16.dp,
                                ),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                item {
                                    SectionSummaryCard(section = section, style = style, count = count)
                                }
                                if (isEmployees) {
                                    if (state.employees.isEmpty()) {
                                        item { SectionEmptyCard(style) }
                                    } else {
                                        items(state.employees, key = { it.id }) { employee ->
                                            EmployeeRow(
                                                user = employee,
                                                style = style,
                                                selected = employee.id in state.selectedEmployeeIds,
                                                onToggleSelect = { viewModel.toggleEmployee(employee.id) },
                                                onClick = { onEmployeeClick(employee.id) },
                                                onAssignTask = { onAssignTask(setOf(employee.id)) },
                                            )
                                        }
                                    }
                                } else if (state.tasks.isEmpty()) {
                                    item { SectionEmptyCard(style) }
                                } else {
                                    items(state.tasks, key = { it.id }) { task ->
                                        TaskRow(task, onClick = { onTaskClick(task.id) })
                                    }
                                }
                            }
                        }
                    }
                }
                if (isEmployees && state.selectedEmployeeIds.isNotEmpty()) {
                    Button(
                        onClick = { onAssignTask(state.selectedEmployeeIds) },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .navigationBarsPadding()
                            .height(52.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(LiquidGlass.RadiusChip),
                        colors = ButtonDefaults.buttonColors(containerColor = LiquidGlass.Blue),
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${localized("task_create")} (${state.selectedEmployeeIds.size})",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionSummaryCard(section: DashboardSection, style: SectionStyle, count: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .liquidGlassThemed()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(style.gradient)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(style.icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(localized(section.titleKey()), color = LiquidTheme.textMuted, fontSize = 12.sp)
            Text(
                "$count ${localized("dash_unit")}",
                color = LiquidTheme.text,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
            )
        }
        Box(
            Modifier
                .clip(CircleShape)
                .background(style.accent.copy(alpha = 0.12f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                localized("dash_total"),
                color = style.accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SectionEmptyCard(style: SectionStyle) {
    Column(
        Modifier
            .fillMaxWidth()
            .liquidGlassThemed()
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(style.accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(style.icon, null, tint = style.accent, modifier = Modifier.size(28.dp))
        }
        Text(
            localized("dash_empty"),
            color = LiquidTheme.textMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun EmployeeRow(
    user: User,
    style: SectionStyle,
    selected: Boolean,
    onToggleSelect: () -> Unit,
    onClick: () -> Unit,
    onAssignTask: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .liquidGlassThemed()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(checked = selected, onCheckedChange = { onToggleSelect() })
        Row(
            Modifier
                .weight(1f)
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(style.gradient)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    employeeInitials(user.fullName),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(user.fullName, color = LiquidTheme.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                val subtitle = listOfNotNull(
                    user.position?.takeIf { it.isNotBlank() },
                    user.department?.takeIf { it.isNotBlank() },
                    user.phone?.takeIf { it.isNotBlank() },
                ).joinToString(" · ")
                if (subtitle.isNotBlank()) {
                    Text(subtitle, color = LiquidTheme.textMuted, fontSize = 12.sp)
                }
            }
        }
        GlassHeaderIconButton(
            onClick = onAssignTask,
            icon = Icons.Default.Add,
            tint = LiquidGlass.Blue,
            contentDescription = localized("task_create_for_employee"),
        )
    }
}

private fun employeeInitials(name: String): String =
    name.trim().split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
