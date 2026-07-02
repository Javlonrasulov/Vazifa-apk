package uz.vazifa.app.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import uz.vazifa.app.data.repository.AuthRepository
import uz.vazifa.app.data.repository.TaskRepository
import uz.vazifa.app.domain.model.Department
import uz.vazifa.app.domain.model.Task
import uz.vazifa.app.domain.model.isTaskAssignable
import uz.vazifa.app.domain.model.User
import uz.vazifa.app.domain.model.hasActiveAssignment
import uz.vazifa.app.domain.model.hasCompletedAssignment
import uz.vazifa.app.domain.model.canCreatorManage
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
    val departments: List<Department> = emptyList(),
    val searchQuery: String = "",
    val tasks: List<Task> = emptyList(),
) {
    val totalEmployees: Int get() = employees.size
}

@HiltViewModel
class DashboardSectionViewModel @Inject constructor(
    private val repo: TaskRepository,
    private val auth: AuthRepository,
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
                        val selfId = auth.currentUser()?.id
                        val employees = repo.getContacts()
                            .filter { it.isTaskAssignable(selfId) }
                            .sortedBy { it.fullName }
                        val apiDepartments = runCatching { repo.getDepartments() }.getOrDefault(emptyList())
                        _state.update {
                            it.copy(employees = employees, departments = apiDepartments, loading = false)
                        }
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

    fun onSearch(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun deleteTask(taskId: String) = viewModelScope.launch {
        runCatching {
            repo.cancelTask(taskId)
            load()
        }
    }
}

@HiltViewModel
class EmployeesTabViewModel @Inject constructor(
    private val repo: TaskRepository,
    private val auth: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(DashboardSectionUiState())
    val state = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            runCatching {
                val selfId = auth.currentUser()?.id
                val employees = repo.getContacts()
                    .filter { it.isTaskAssignable(selfId) }
                    .sortedBy { it.fullName }
                val apiDepartments = runCatching { repo.getDepartments() }.getOrDefault(emptyList())
                _state.update {
                    it.copy(employees = employees, departments = apiDepartments, loading = false)
                }
            }.onFailure {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    fun onSearch(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }
}

@Composable
fun EmployeesTabScreen(
    onEmployeeClick: (String) -> Unit,
    onAssignTask: (Set<String>) -> Unit,
    onDepartmentClick: (String?) -> Unit,
    onSearchAll: (String) -> Unit,
    viewModel: EmployeesTabViewModel = hiltViewModel(),
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
        title = localized("nav_employees"),
        actions = { VazifaHeaderActions() },
    ) { padding ->
        LiquidBackground(Modifier.fillMaxSize()) {
            VazifaScreenBox(padding) {
                when {
                    state.loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = LiquidGlass.Blue)
                        }
                    }
                    else -> {
                        EmployeesHubContent(
                            totalEmployees = state.totalEmployees,
                            departments = state.departments,
                            searchQuery = state.searchQuery,
                            onSearch = viewModel::onSearch,
                            onSearchSubmit = { onSearchAll(state.searchQuery) },
                            onDepartmentClick = onDepartmentClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardSectionScreen(
    onBack: () -> Unit,
    onTaskClick: (String) -> Unit,
    onAssignTask: (Set<String>) -> Unit = {},
    onEmployeeClick: (String) -> Unit = {},
    onEditTask: (String) -> Unit = {},
    onDepartmentClick: (String?) -> Unit = {},
    onSearchAll: (String) -> Unit = {},
    currentUserId: String? = null,
    viewModel: DashboardSectionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var taskToDelete by remember { mutableStateOf<String?>(null) }
    val section = viewModel.section
    val style = section.style()
    val isEmployees = section == DashboardSection.EMPLOYEES
    val count = when {
        isEmployees -> state.employees.size
        else -> state.tasks.size
    }

    LaunchedEffect(Unit) {
        viewModel.load()
        if (isEmployees) {
            while (true) {
                delay(30_000)
                viewModel.load()
            }
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
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
        title = localized(section.titleKey()),
        onBack = onBack,
        actions = {
            if (!isEmployees) VazifaHeaderActions()
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
                        isEmployees -> {
                            EmployeesHubContent(
                                totalEmployees = state.totalEmployees,
                                departments = state.departments,
                                searchQuery = state.searchQuery,
                                onSearch = viewModel::onSearch,
                                onSearchSubmit = { onSearchAll(state.searchQuery) },
                                onDepartmentClick = onDepartmentClick,
                            )
                        }
                        else -> {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                item {
                                    SectionSummaryCard(section = section, style = style, count = count)
                                }
                                if (state.tasks.isEmpty()) {
                                    item { SectionEmptyCard(style) }
                                } else {
                                    items(state.tasks, key = { it.id }) { task ->
                                        val canManage = task.canCreatorManage(currentUserId)
                                        TaskRow(
                                            task = task,
                                            currentUserId = currentUserId,
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
