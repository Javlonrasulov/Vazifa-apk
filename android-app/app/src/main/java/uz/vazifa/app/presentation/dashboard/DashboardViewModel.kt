package uz.vazifa.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.vazifa.app.data.repository.AuthRepository
import uz.vazifa.app.data.repository.TaskRepository
import uz.vazifa.app.domain.model.DashboardStats
import uz.vazifa.app.domain.model.Task
import uz.vazifa.app.domain.model.hasActiveAssignment
import uz.vazifa.app.domain.model.hasCompletedAssignment
import uz.vazifa.app.domain.model.isOverdue
import uz.vazifa.app.domain.model.isTaskAssignable
import javax.inject.Inject

data class DashboardUiState(
    val stats: DashboardStats? = null,
    val tasks: List<Task> = emptyList(),
    val currentUserId: String? = null,
    val canAssignTasks: Boolean = false,
    val loading: Boolean = false,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val tasks: TaskRepository,
    private val auth: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(DashboardUiState())
    val state = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            runCatching {
                val user = auth.currentUser()
                val list = runCatching { tasks.getTasks() }.getOrDefault(emptyList())
                val contacts = runCatching {
                    tasks.getContacts().filter { it.isTaskAssignable(user?.id) }
                }.getOrDefault(emptyList())
                val apiStats = runCatching { tasks.getDashboardStats() }.getOrNull()
                val stats = apiStats?.let { s ->
                    DashboardStats(
                        totalEmployees = s.totalEmployees.takeIf { it > 0 } ?: contacts.size,
                        activeTasks = s.activeTasks,
                        completedTasks = s.completedTasks,
                        overdueTasks = s.overdueTasks,
                        todayTasks = s.todayTasks,
                    )
                } ?: DashboardStats(
                    totalEmployees = contacts.size,
                    activeTasks = list.count { it.hasActiveAssignment() },
                    completedTasks = list.count { it.hasCompletedAssignment() },
                    overdueTasks = list.count { it.isOverdue() },
                )
                _state.update {
                    it.copy(
                        stats = stats,
                        tasks = list,
                        currentUserId = user?.id,
                        canAssignTasks = user?.canAssignTasks == true,
                        loading = false,
                    )
                }
            }.onFailure {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    fun deleteTask(taskId: String) = viewModelScope.launch {
        runCatching {
            tasks.cancelTask(taskId)
            load()
        }
    }
}
