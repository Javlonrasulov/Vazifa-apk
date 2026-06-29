package uz.vazifa.app.presentation.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.vazifa.app.data.repository.TaskRepository
import uz.vazifa.app.domain.model.User
import uz.vazifa.app.domain.model.isTaskAssignable
import uz.vazifa.app.domain.model.matchesDepartment
import uz.vazifa.app.presentation.components.*
import uz.vazifa.app.presentation.theme.LiquidBackground
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.util.UzbekTextSearch
import javax.inject.Inject

data class DepartmentEmployeesUiState(
    val loading: Boolean = false,
    val employees: List<User> = emptyList(),
    val searchQuery: String = "",
    val selectedEmployeeIds: Set<String> = emptySet(),
) {
    val filteredEmployees: List<User>
        get() {
            val q = searchQuery.trim()
            if (q.isBlank()) return employees
            return employees.filter { user ->
                UzbekTextSearch.matchesEmployee(user.fullName, user.login, user.phone, q)
            }
        }
}

@HiltViewModel
class DepartmentEmployeesViewModel @Inject constructor(
    private val repo: TaskRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val departmentKey: String = savedStateHandle.get<String>("department")
        .orEmpty()
        .let { if (it == "_all_") "" else android.net.Uri.decode(it) }
    val departmentName: String? = departmentKey.takeIf { it.isNotBlank() }

    val screenTitle: String
        get() = departmentName ?: "dash_total"

    private val initialSearch: String = savedStateHandle.get<String>("q").orEmpty()

    private val _state = MutableStateFlow(DepartmentEmployeesUiState(searchQuery = initialSearch))
    val state = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            runCatching {
                val all = repo.getContacts()
                    .filter { it.isTaskAssignable() }
                    .sortedBy { it.fullName }
                val list = departmentName?.let { dept ->
                    all.filter { it.matchesDepartment(dept) }
                } ?: all
                _state.update { it.copy(employees = list, loading = false) }
            }.onFailure {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    fun onSearch(query: String) = _state.update { it.copy(searchQuery = query) }

    fun toggleEmployee(id: String) {
        _state.update {
            val ids = it.selectedEmployeeIds.toMutableSet()
            if (ids.contains(id)) ids.remove(id) else ids.add(id)
            it.copy(selectedEmployeeIds = ids)
        }
    }

    fun selectAllEmployees() {
        _state.update { state ->
            state.copy(selectedEmployeeIds = state.filteredEmployees.map { it.id }.toSet())
        }
    }

    fun clearSelection() = _state.update { it.copy(selectedEmployeeIds = emptySet()) }
}

@Composable
fun DepartmentEmployeesScreen(
    onBack: () -> Unit,
    onEmployeeClick: (String) -> Unit,
    onAssignTask: (Set<String>) -> Unit,
    viewModel: DepartmentEmployeesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val style = DashboardSection.EMPLOYEES.style()
    val visibleEmployees = state.filteredEmployees
    val allSelected = visibleEmployees.isNotEmpty() &&
        visibleEmployees.all { it.id in state.selectedEmployeeIds }
    val title = if (viewModel.departmentName != null) {
        viewModel.departmentName!!
    } else {
        localized("emp_all_staff")
    }

    LaunchedEffect(Unit) {
        viewModel.load()
        while (true) {
            delay(30_000)
            viewModel.load()
        }
    }

    VazifaTabScaffold(
        title = title,
        onBack = onBack,
        actions = {
            if (visibleEmployees.isNotEmpty()) {
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
                                    bottom = if (state.selectedEmployeeIds.isNotEmpty()) 88.dp else 16.dp,
                                ),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                item {
                                    EmployeeSearchField(
                                        value = state.searchQuery,
                                        onValueChange = viewModel::onSearch,
                                    )
                                }
                                item {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            localized("dash_employees"),
                                            color = LiquidTheme.textMuted,
                                            fontSize = 13.sp,
                                        )
                                        Text(
                                            "${visibleEmployees.size} ${localized("dash_unit")}",
                                            color = LiquidTheme.textMuted,
                                            fontSize = 13.sp,
                                        )
                                    }
                                }
                                if (visibleEmployees.isEmpty()) {
                                    item { EmployeesEmptyCard(style) }
                                } else {
                                    items(visibleEmployees, key = { it.id }) { employee ->
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
                            }
                        }
                    }
                }
                if (state.selectedEmployeeIds.isNotEmpty()) {
                    Button(
                        onClick = { onAssignTask(state.selectedEmployeeIds) },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .height(52.dp),
                        shape = RoundedCornerShape(LiquidGlass.RadiusChip),
                        colors = ButtonDefaults.buttonColors(containerColor = LiquidGlass.Blue),
                    ) {
                        Icon(Icons.Default.Add, null, tint = androidx.compose.ui.graphics.Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${localized("task_create")} (${state.selectedEmployeeIds.size})",
                            color = androidx.compose.ui.graphics.Color.White,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
