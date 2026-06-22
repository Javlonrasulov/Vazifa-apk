package uz.vazifa.app.presentation.tasks

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import uz.vazifa.app.data.repository.AuthRepository
import uz.vazifa.app.data.repository.TaskRepository
import uz.vazifa.app.domain.model.Task
import uz.vazifa.app.domain.model.TaskStatus
import uz.vazifa.app.domain.model.User
import uz.vazifa.app.util.TaskDeadlineCountdown
import uz.vazifa.app.util.UzbekTextSearch
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val repo: TaskRepository,
    private val auth: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(TasksUiState())
    val state = _state.asStateFlow()
    fun load() = viewModelScope.launch {
        runCatching {
            val user = auth.currentUser()
            _state.update {
                it.copy(
                    tasks = repo.getTasks(),
                    currentUserId = user?.id,
                    canAssignTasks = user?.canAssignTasks == true,
                )
            }
        }
    }

    fun deleteTask(taskId: String) = viewModelScope.launch {
        runCatching {
            repo.cancelTask(taskId)
            load()
        }
    }
}
data class TasksUiState(
    val tasks: List<Task> = emptyList(),
    val currentUserId: String? = null,
    val canAssignTasks: Boolean = false,
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val repo: TaskRepository,
    private val auth: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(TaskDetailUiState())
    val state = _state.asStateFlow()

    fun load(id: String) = viewModelScope.launch {
        val userId = auth.currentUser()?.id
        runCatching {
            _state.update { it.copy(task = repo.getTask(id), currentUserId = userId, loading = false) }
        }.onFailure {
            _state.update { it.copy(loading = false, error = true) }
        }
    }

    fun updateStatus(taskId: String, assignmentId: String, status: String) = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        runCatching {
            repo.updateStatus(taskId, assignmentId, status)
            load(taskId)
        }.onFailure { _state.update { it.copy(loading = false) } }
    }

    fun completeWithReport(
        taskId: String,
        assignmentId: String,
        comment: String,
        imageUri: Uri?,
    ) = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        runCatching {
            val text = comment.trim()
            if (text.isNotBlank()) repo.addComment(taskId, text)
            if (imageUri != null) repo.uploadAttachment(taskId, imageUri)
            repo.updateStatus(taskId, assignmentId, TaskStatus.COMPLETED.key)
            load(taskId)
        }.onFailure { _state.update { it.copy(loading = false) } }
    }
}

data class TaskDetailUiState(
    val task: Task? = null,
    val currentUserId: String? = null,
    val loading: Boolean = false,
    val error: Boolean = false,
)

@HiltViewModel
class CreateTaskViewModel @Inject constructor(
    private val repo: TaskRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val editTaskId: String? = savedStateHandle.get<String>("taskId")
    private val zone = ZoneId.of("Asia/Tashkent")
    private val _state = MutableStateFlow(CreateTaskUiState(isEditMode = editTaskId != null).withSyncedDeadline(zone))
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

    fun loadForEdit() {
        val id = editTaskId ?: return
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            runCatching {
                val task = repo.getTask(id)
                val deadline = TaskDeadlineCountdown.parseDeadline(task.deadlineAt)
                val deadlineDateTime = deadline?.atZone(zone)?.toLocalDateTime()
                val deadlineHours = deadlineDateTime?.let { dt ->
                    val zoned = ZonedDateTime.of(dt, zone)
                    hoursUntil(zoned).toString()
                } ?: _state.value.deadlineHours
                _state.update {
                    it.copy(
                        title = task.title,
                        description = task.description.orEmpty(),
                        deadlineDateTime = deadlineDateTime,
                        deadlineHours = deadlineHours,
                        selectedIds = task.assignments.map { a -> a.assigneeId }.toSet(),
                        loading = false,
                    )
                }
            }.onFailure {
                _state.update { it.copy(loading = false, errorKey = "task_update_failed") }
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
                if (!deadline.isAfter(now)) deadline = now.plusHours(1)
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

    fun create(imageUri: Uri?) = viewModelScope.launch {
        val title = _state.value.title.trim()
        if (title.isBlank()) {
            _state.update { it.copy(errorKey = "task_title_empty") }
            return@launch
        }
        if (_state.value.selectedIds.isEmpty()) {
            _state.update { it.copy(errorKey = "task_assignee_required") }
            return@launch
        }
        _state.update { it.copy(loading = true, errorKey = null) }
        val now = nowZoned()
        val fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val deadlineAt = _state.value.deadlineDateTime?.let { dt ->
            ZonedDateTime.of(dt, zone).format(fmt)
        } ?: run {
            val hours = _state.value.deadlineHours.toLongOrNull()?.coerceAtLeast(1) ?: 48
            now.plusHours(hours).format(fmt)
        }
        runCatching {
            val task = repo.createTask(
                title = title,
                description = _state.value.description.ifBlank { null },
                priority = "medium",
                assigneeIds = _state.value.selectedIds.toList(),
                startAt = now.format(fmt),
                deadlineAt = deadlineAt,
            )
            if (imageUri != null) {
                runCatching { repo.uploadAttachment(task.id, imageUri) }
                    .onFailure {
                        _state.update { it.copy(errorKey = "task_photo_upload_failed") }
                    }
            }
            _state.update { it.copy(loading = false, created = true) }
        }.onFailure { e ->
            _state.update { it.copy(loading = false, errorKey = mapCreateError(e)) }
        }
    }

    fun update() = viewModelScope.launch {
        val id = editTaskId ?: return@launch
        val title = _state.value.title.trim()
        if (title.isBlank()) {
            _state.update { it.copy(errorKey = "task_title_empty") }
            return@launch
        }
        _state.update { it.copy(loading = true, errorKey = null) }
        val fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val deadlineAt = _state.value.deadlineDateTime?.let { dt ->
            ZonedDateTime.of(dt, zone).format(fmt)
        } ?: run {
            val hours = _state.value.deadlineHours.toLongOrNull()?.coerceAtLeast(1) ?: 48
            nowZoned().plusHours(hours).format(fmt)
        }
        runCatching {
            repo.updateTask(
                id = id,
                title = title,
                description = _state.value.description.ifBlank { null },
                deadlineAt = deadlineAt,
            )
            _state.update { it.copy(loading = false, created = true) }
        }.onFailure { e ->
            _state.update { it.copy(loading = false, errorKey = mapCreateError(e)) }
        }
    }

    fun clearError() = _state.update { it.copy(errorKey = null) }

    private fun mapCreateError(e: Throwable): String = when (e) {
        is HttpException -> when (e.code()) {
            403 -> "task_create_forbidden"
            else -> "task_create_failed"
        }
        else -> "task_create_failed"
    }

    fun preselectAssignees(ids: Set<String>) {
        _state.update {
            CreateTaskUiState(
                contacts = it.contacts,
                selectedIds = ids,
                isEditMode = it.isEditMode,
            ).withSyncedDeadline(zone)
        }
    }

    fun resetForm() {
        _state.update {
            CreateTaskUiState(contacts = it.contacts, isEditMode = editTaskId != null).withSyncedDeadline(zone)
        }
    }

    private fun nowZoned(): ZonedDateTime = ZonedDateTime.now(zone)
    private fun deadlineFromHours(hours: Long): LocalDateTime = nowZoned().plusHours(hours).toLocalDateTime()
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
    val errorKey: String? = null,
    val isEditMode: Boolean = false,
) {
    val canCreate: Boolean
        get() = when {
            loading -> false
            isEditMode -> title.isNotBlank()
            else -> title.isNotBlank() && selectedIds.isNotEmpty()
        }
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
