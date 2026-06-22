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
import uz.vazifa.app.util.UzbekTextSearch
import java.time.LocalDateTime
import java.time.ZoneId
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
    private val zone = ZoneId.of("Asia/Tashkent")
    private val _state = MutableStateFlow(CreateTaskUiState().withSyncedDeadline(zone))
    val state = _state.asStateFlow()

    fun loadContacts() = viewModelScope.launch {
        runCatching {
            _state.update {
                it.copy(
                    contacts = repo.getContacts()
                        .filter { u -> u.role == "employee" && u.login != "xodim1" },
                )
            }
        }
    }
    fun onTitle(v: String) = _state.update { it.copy(title = v) }
    fun onDescription(v: String) = _state.update { it.copy(description = v) }
    fun onDeadlineHours(v: String) {
        val digits = v.filter { it.isDigit() }
        _state.update { current ->
            val hours = digits.toLongOrNull()
            current.copy(
                deadlineHours = digits,
                deadlineDateTime = hours?.takeIf { it > 0 }?.let { deadlineFromHours(it) },
            )
        }
    }
    fun onDeadlineDateTime(v: LocalDateTime?) {
        _state.update { current ->
            if (v == null) {
                current.copy(deadlineDateTime = null, deadlineHours = "")
            } else {
                val now = nowZoned()
                var deadline = ZonedDateTime.of(v, zone)
                if (!deadline.isAfter(now)) {
                    deadline = now.plusHours(1)
                }
                val dt = deadline.toLocalDateTime()
                current.copy(
                    deadlineDateTime = dt,
                    deadlineHours = hoursUntil(deadline).toString(),
                )
            }
        }
    }
    fun onAssigneeSearch(v: String) = _state.update { it.copy(assigneeSearch = v) }
    fun toggleAssignee(id: String) = _state.update {
        val ids = it.selectedIds.toMutableSet()
        if (ids.contains(id)) ids.remove(id) else ids.add(id)
        it.copy(selectedIds = ids)
    }

    fun create() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        val now = nowZoned()
        val fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val deadlineAt = _state.value.deadlineDateTime?.let { dt ->
            ZonedDateTime.of(dt, zone).format(fmt)
        } ?: run {
            val hours = _state.value.deadlineHours.toLongOrNull()?.coerceAtLeast(1) ?: 48
            now.plusHours(hours).format(fmt)
        }
        runCatching {
            repo.createTask(
                title = _state.value.title,
                description = _state.value.description.ifBlank { null },
                priority = "medium",
                assigneeIds = _state.value.selectedIds.toList(),
                startAt = now.format(fmt),
                deadlineAt = deadlineAt,
            )
            _state.update { it.copy(loading = false, created = true) }
        }.onFailure { _state.update { it.copy(loading = false) } }
    }

    fun resetForm() {
        _state.update {
            CreateTaskUiState(contacts = it.contacts).withSyncedDeadline(zone)
        }
    }

    private fun nowZoned(): ZonedDateTime = ZonedDateTime.now(zone)

    private fun deadlineFromHours(hours: Long): LocalDateTime =
        nowZoned().plusHours(hours).toLocalDateTime()

    private fun hoursUntil(deadline: ZonedDateTime): Long {
        val minutes = java.time.Duration.between(nowZoned(), deadline).toMinutes()
        return ((minutes + 59) / 60).coerceAtLeast(1)
    }
}
data class CreateTaskUiState(
    val title: String = "",
    val description: String = "",
    val deadlineHours: String = "48",
    val deadlineDateTime: LocalDateTime? = null,
    val contacts: List<User> = emptyList(),
    val assigneeSearch: String = "",
    val selectedIds: Set<String> = emptySet(),
    val loading: Boolean = false,
    val created: Boolean = false,
) {
    fun withSyncedDeadline(zone: ZoneId): CreateTaskUiState {
        val hours = deadlineHours.toLongOrNull()?.coerceAtLeast(1) ?: 48
        return copy(
            deadlineHours = hours.toString(),
            deadlineDateTime = ZonedDateTime.now(zone).plusHours(hours).toLocalDateTime(),
        )
    }

    val filteredContacts: List<User>
        get() {
            val q = assigneeSearch.trim()
            if (q.isBlank()) return emptyList()
            return contacts.filter { user ->
                UzbekTextSearch.matchesEmployee(user.fullName, user.login, user.phone, q)
            }
        }

    val selectedContacts: List<User>
        get() = contacts.filter { it.id in selectedIds }
}
