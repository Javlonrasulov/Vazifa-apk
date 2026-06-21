package uz.vazifa.app.presentation.tasks

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
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(private val repo: TaskRepository) : ViewModel() {
    private val _state = MutableStateFlow(TasksUiState())
    val state = _state.asStateFlow()
    fun load() = viewModelScope.launch {
        runCatching { _state.update { it.copy(tasks = repo.getTasks()) } }
    }
}
data class TasksUiState(val tasks: List<Task> = emptyList())

@HiltViewModel
class TaskDetailViewModel @Inject constructor(private val repo: TaskRepository) : ViewModel() {
    private val _state = MutableStateFlow(TaskDetailUiState())
    val state = _state.asStateFlow()
    fun load(id: String) = viewModelScope.launch {
        runCatching { _state.update { it.copy(task = repo.getTask(id)) } }
    }
    fun updateStatus(taskId: String, assignmentId: String, status: String) = viewModelScope.launch {
        repo.updateStatus(taskId, assignmentId, status)
        load(taskId)
    }
}
data class TaskDetailUiState(val task: Task? = null)

@HiltViewModel
class CreateTaskViewModel @Inject constructor(private val repo: TaskRepository) : ViewModel() {
    private val _state = MutableStateFlow(CreateTaskUiState())
    val state = _state.asStateFlow()

    fun loadContacts() = viewModelScope.launch {
        runCatching {
            _state.update { it.copy(contacts = repo.getContacts().filter { u -> u.role == "employee" }) }
        }
    }
    fun onTitle(v: String) = _state.update { it.copy(title = v) }
    fun onDescription(v: String) = _state.update { it.copy(description = v) }
    fun onDeadlineHours(v: String) = _state.update { it.copy(deadlineHours = v) }
    fun toggleAssignee(id: String) = _state.update {
        val ids = it.selectedIds.toMutableSet()
        if (ids.contains(id)) ids.remove(id) else ids.add(id)
        it.copy(selectedIds = ids)
    }

    fun create() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        val now = ZonedDateTime.now()
        val hours = _state.value.deadlineHours.toLongOrNull() ?: 48
        val fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        runCatching {
            repo.createTask(
                title = _state.value.title,
                description = _state.value.description.ifBlank { null },
                priority = "medium",
                assigneeIds = _state.value.selectedIds.toList(),
                startAt = now.format(fmt),
                deadlineAt = now.plusHours(hours).format(fmt),
            )
            _state.update { it.copy(loading = false, created = true) }
        }.onFailure { _state.update { it.copy(loading = false) } }
    }
}
data class CreateTaskUiState(
    val title: String = "",
    val description: String = "",
    val deadlineHours: String = "48",
    val contacts: List<User> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val loading: Boolean = false,
    val created: Boolean = false,
)
