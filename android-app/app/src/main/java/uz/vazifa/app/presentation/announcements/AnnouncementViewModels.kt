package uz.vazifa.app.presentation.announcements

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import com.google.gson.JsonSyntaxException
import org.json.JSONObject
import uz.vazifa.app.data.repository.AnnouncementRepository
import uz.vazifa.app.data.repository.AuthRepository
import uz.vazifa.app.data.repository.TaskRepository
import uz.vazifa.app.domain.model.Announcement
import uz.vazifa.app.domain.model.User
import uz.vazifa.app.domain.model.acknowledgedCount
import uz.vazifa.app.domain.model.isAcknowledgedBy
import uz.vazifa.app.domain.model.isCreator
import uz.vazifa.app.domain.model.isViewedBy
import uz.vazifa.app.domain.model.pendingCount
import uz.vazifa.app.domain.model.isTaskAssignable
import uz.vazifa.app.domain.model.isActive
import uz.vazifa.app.util.TaskDeadlineCountdown
import uz.vazifa.app.util.UzbekTextSearch
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.math.roundToLong

enum class ReminderUnit(val minutesMultiplier: Int) {
    MINUTES(1),
    HOURS(60),
}

@HiltViewModel
class CreateAnnouncementViewModel @Inject constructor(
    private val announcementRepo: AnnouncementRepository,
    private val taskRepo: TaskRepository,
    private val auth: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val editAnnouncementId: String? = savedStateHandle.get<String>("announcementId")
    private val zone = ZoneId.of("Asia/Tashkent")
    private val _state = MutableStateFlow(
        CreateAnnouncementUiState(isEditMode = editAnnouncementId != null).withSyncedDeadline(zone),
    )
    val state = _state.asStateFlow()

    init {
        val raw = savedStateHandle.get<String>("recipientIds").orEmpty()
        val preselected = raw.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
        if (preselected.isNotEmpty()) {
            _state.update { it.copy(selectedIds = preselected) }
        }
    }

    fun loadContacts() = viewModelScope.launch {
        runCatching {
            val selfId = auth.currentUser()?.id
            _state.update {
                it.copy(
                    currentUserId = selfId,
                    contacts = taskRepo.getContacts().filter { c -> c.isTaskAssignable(selfId) },
                )
            }
            if (editAnnouncementId != null) loadForEdit()
        }
    }

    fun loadForEdit() {
        val id = editAnnouncementId ?: return
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            runCatching {
                val announcement = announcementRepo.getById(id)
                if (!announcement.isActive()) {
                    _state.update { it.copy(loading = false, errorKey = "announcement_edit_inactive_forbidden") }
                    return@runCatching
                }
                val deadline = TaskDeadlineCountdown.parseDeadline(announcement.deadlineAt)
                val deadlineDateTime = deadline?.atZone(zone)?.toLocalDateTime()
                val deadlineHours = deadlineDateTime?.let { dt ->
                    hoursUntil(ZonedDateTime.of(dt, zone))
                } ?: _state.value.deadlineHours
                val (reminderValue, reminderUnit) = reminderFromMinutes(announcement.reminderIntervalMinutes)
                _state.update {
                    it.copy(
                        title = announcement.title,
                        description = announcement.description.orEmpty(),
                        deadlineDateTime = deadlineDateTime,
                        deadlineHours = deadlineHours,
                        reminderValue = reminderValue,
                        reminderUnit = reminderUnit,
                        selectedIds = announcement.recipients.map { r -> r.recipientId }.toSet(),
                        loading = false,
                    )
                }
            }.onFailure {
                _state.update { it.copy(loading = false, errorKey = "announcement_update_failed") }
            }
        }
    }

    fun onTitle(v: String) = _state.update { it.copy(title = v, titleError = false) }
    fun onDescription(v: String) = _state.update { it.copy(description = v) }
    fun onVoiceRecorded(file: File) = _state.update { it.copy(voiceFile = file) }
    fun removeVoice() {
        _state.value.voiceFile?.delete()
        _state.update { it.copy(voiceFile = null) }
    }
    fun showTitleError() = _state.update { it.copy(titleError = true) }
    fun showRecipientError() = _state.update { it.copy(errorKey = "announcement_recipient_required") }
    fun onRecipientSearch(v: String) = _state.update { it.copy(recipientSearch = v) }
    fun toggleRecipient(id: String) = _state.update {
        val ids = it.selectedIds.toMutableSet()
        if (ids.contains(id)) ids.remove(id) else ids.add(id)
        it.copy(selectedIds = ids)
    }
    fun onReminderValue(v: String) = _state.update { it.copy(reminderValue = v.filter { ch -> ch.isDigit() }) }
    fun onReminderUnit(unit: ReminderUnit) = _state.update { it.copy(reminderUnit = unit) }

    fun onDeadlineHours(v: String) {
        val filtered = filterDeadlineHoursInput(v)
        _state.update { current ->
            val hours = parseDeadlineHours(filtered)
            current.copy(
                deadlineHours = filtered,
                deadlineDateTime = hours?.let { deadlineFromHours(it) },
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
                if (!deadline.isAfter(now)) deadline = now.plusMinutes(60)
                val dt = deadline.toLocalDateTime()
                current.copy(
                    deadlineDateTime = dt,
                    deadlineHours = hoursUntil(deadline),
                )
            }
        }
    }

    fun create() = viewModelScope.launch {
        val snapshot = _state.value
        if (snapshot.loading || snapshot.created) return@launch
        val title = snapshot.title.trim()
        if (title.isBlank()) {
            _state.update { it.copy(titleError = true, errorKey = "task_title_empty") }
            return@launch
        }
        val selfId = _state.value.currentUserId
        val recipientIds = _state.value.selectedIds.filter { it != selfId }
        if (recipientIds.isEmpty()) {
            _state.update {
                it.copy(
                    errorKey = if (selfId != null && selfId in _state.value.selectedIds) {
                        "task_self_assign_forbidden"
                    } else {
                        "announcement_recipient_required"
                    },
                )
            }
            return@launch
        }
        val intervalMinutes = _state.value.reminderIntervalMinutes()
        if (intervalMinutes < 1) {
            _state.update { it.copy(errorKey = "announcement_interval_invalid") }
            return@launch
        }
        var locked = false
        _state.update { s ->
            if (s.loading || s.created) s
            else {
                locked = true
                s.copy(loading = true, errorKey = null, errorText = null)
            }
        }
        if (!locked) return@launch
        val fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val now = nowZoned()
        val deadlineAt = _state.value.deadlineDateTime?.let { dt ->
            ZonedDateTime.of(dt, zone).format(fmt)
        } ?: run {
            val hours = parseDeadlineHours(_state.value.deadlineHours) ?: 48.0
            val minutes = (hours * 60).roundToLong().coerceAtLeast(1)
            now.plusMinutes(minutes).format(fmt)
        }
        runCatching {
            val announcement = announcementRepo.create(
                title = title,
                description = _state.value.description.ifBlank { null },
                deadlineAt = deadlineAt,
                reminderIntervalMinutes = intervalMinutes,
                recipientIds = recipientIds,
            )
            val voice = _state.value.voiceFile
            if (voice != null) {
                runCatching { announcementRepo.uploadVoice(announcement.id, voice) }
                    .onFailure { _state.update { it.copy(errorKey = "task_voice_upload_failed") } }
            }
            _state.update { it.copy(loading = false, created = true, createdId = announcement.id, voiceFile = null) }
        }.onFailure { e ->
            val err = mapCreateError(e)
            _state.update {
                it.copy(loading = false, errorKey = err.key, errorText = err.detail)
            }
        }
    }

    fun update() = viewModelScope.launch {
        val id = editAnnouncementId ?: return@launch
        val snapshot = _state.value
        if (snapshot.loading || snapshot.saved) return@launch
        val title = snapshot.title.trim()
        if (title.isBlank()) {
            _state.update { it.copy(titleError = true, errorKey = "task_title_empty") }
            return@launch
        }
        val intervalMinutes = _state.value.reminderIntervalMinutes()
        if (intervalMinutes < 1) {
            _state.update { it.copy(errorKey = "announcement_interval_invalid") }
            return@launch
        }
        var locked = false
        _state.update { s ->
            if (s.loading || s.saved) s
            else {
                locked = true
                s.copy(loading = true, errorKey = null, errorText = null)
            }
        }
        if (!locked) return@launch
        val fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val now = nowZoned()
        val deadlineAt = _state.value.deadlineDateTime?.let { dt ->
            ZonedDateTime.of(dt, zone).format(fmt)
        } ?: run {
            val hours = parseDeadlineHours(_state.value.deadlineHours) ?: 48.0
            val minutes = (hours * 60).roundToLong().coerceAtLeast(1)
            now.plusMinutes(minutes).format(fmt)
        }
        runCatching {
            val announcement = announcementRepo.update(
                id = id,
                title = title,
                description = _state.value.description.ifBlank { null },
                deadlineAt = deadlineAt,
                reminderIntervalMinutes = intervalMinutes,
            )
            val voice = _state.value.voiceFile
            if (voice != null) {
                runCatching { announcementRepo.uploadVoice(announcement.id, voice) }
                    .onFailure { _state.update { it.copy(errorKey = "task_voice_upload_failed") } }
            }
            _state.update { it.copy(loading = false, saved = true, savedId = announcement.id, voiceFile = null) }
        }.onFailure { e ->
            val err = mapCreateError(e)
            _state.update {
                it.copy(loading = false, errorKey = err.key ?: "announcement_update_failed", errorText = err.detail)
            }
        }
    }

    fun clearError() = _state.update { it.copy(errorKey = null, errorText = null) }
    fun resetForm() {
        _state.value.voiceFile?.delete()
        _state.update {
            CreateAnnouncementUiState(
                contacts = it.contacts,
                currentUserId = it.currentUserId,
                isEditMode = editAnnouncementId != null,
            ).withSyncedDeadline(zone)
        }
    }

    private fun reminderFromMinutes(minutes: Int): Pair<String, ReminderUnit> {
        return if (minutes % 60 == 0 && minutes >= 60) {
            (minutes / 60).toString() to ReminderUnit.HOURS
        } else {
            minutes.toString() to ReminderUnit.MINUTES
        }
    }

    private fun nowZoned(): ZonedDateTime = ZonedDateTime.now(zone)
    private fun deadlineFromHours(hours: Double): LocalDateTime {
        val minutes = (hours * 60).roundToLong().coerceAtLeast(1)
        return nowZoned().plusMinutes(minutes).toLocalDateTime()
    }
    private fun hoursUntil(deadline: ZonedDateTime): String {
        val minutes = java.time.Duration.between(nowZoned(), deadline).toMinutes().coerceAtLeast(1)
        return minutesToHoursString(minutes)
    }

    private fun mapCreateError(e: Throwable): CreateAnnouncementError = when (e) {
        is HttpException -> {
            val body = e.response()?.errorBody()?.string().orEmpty()
            val serverMsg = parseApiMessage(body)
            when (e.code()) {
                403 -> when {
                    body.contains("NOTIFICATIONS_REQUIRED") -> CreateAnnouncementError("task_notifications_required")
                    body.contains("huquqi") -> CreateAnnouncementError("announcement_create_forbidden")
                    else -> CreateAnnouncementError("announcement_create_failed", serverMsg)
                }
                400 -> when {
                    body.contains("O'zingizga") -> CreateAnnouncementError("task_self_assign_forbidden")
                    body.contains("Muddat") -> CreateAnnouncementError("announcement_deadline_invalid")
                    else -> CreateAnnouncementError("announcement_create_failed", serverMsg)
                }
                404 -> CreateAnnouncementError("announcement_api_missing")
                500, 502, 503 -> CreateAnnouncementError("announcement_server_error", serverMsg)
                else -> CreateAnnouncementError("announcement_create_failed", serverMsg)
            }
        }
        is JsonSyntaxException -> CreateAnnouncementError("announcement_parse_failed")
        else -> CreateAnnouncementError("announcement_create_failed")
    }

    private fun parseApiMessage(body: String): String? {
        if (body.isBlank()) return null
        return runCatching {
            val json = JSONObject(body)
            when (val message = json.opt("message")) {
                is String -> message.takeIf { it.isNotBlank() }
                else -> null
            }
        }.getOrNull()
    }
}

data class CreateAnnouncementUiState(
    val isEditMode: Boolean = false,
    val title: String = "",
    val description: String = "",
    val deadlineHours: String = "48",
    val deadlineDateTime: LocalDateTime? = null,
    val reminderValue: String = "30",
    val reminderUnit: ReminderUnit = ReminderUnit.MINUTES,
    val contacts: List<User> = emptyList(),
    val currentUserId: String? = null,
    val recipientSearch: String = "",
    val selectedIds: Set<String> = emptySet(),
    val voiceFile: File? = null,
    val titleError: Boolean = false,
    val loading: Boolean = false,
    val created: Boolean = false,
    val createdId: String? = null,
    val saved: Boolean = false,
    val savedId: String? = null,
    val errorKey: String? = null,
    val errorText: String? = null,
) {
    fun reminderIntervalMinutes(): Int {
        val value = reminderValue.toIntOrNull() ?: return 0
        return value * reminderUnit.minutesMultiplier
    }

    fun withSyncedDeadline(zone: ZoneId): CreateAnnouncementUiState {
        val hours = parseDeadlineHours(deadlineHours) ?: 48.0
        val minutes = (hours * 60).roundToLong().coerceAtLeast(1)
        return copy(
            deadlineHours = minutesToHoursString(minutes),
            deadlineDateTime = ZonedDateTime.now(zone).plusMinutes(minutes).toLocalDateTime(),
        )
    }

    val filteredContacts: List<User>
        get() {
            val q = recipientSearch.trim()
            if (q.isBlank()) return emptyList()
            return contacts.filter { user ->
                UzbekTextSearch.matchesEmployee(user.fullName, user.login, user.phone, q)
            }
        }

    val selectedContacts: List<User>
        get() = contacts.filter { it.id in selectedIds }
}

private data class CreateAnnouncementError(
    val key: String? = null,
    val detail: String? = null,
)

@HiltViewModel
class AnnouncementDetailViewModel @Inject constructor(
    private val repo: AnnouncementRepository,
    private val auth: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AnnouncementDetailUiState())
    val state = _state.asStateFlow()

    fun load(id: String) = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        runCatching {
            val userId = auth.currentUser()?.id
            var announcement = repo.getById(id)
            if (userId != null && !announcement.isCreator(userId) && !announcement.isViewedBy(userId)) {
                runCatching { repo.markViewed(id) }
                announcement = repo.getById(id)
            }
            _state.update {
                it.copy(
                    announcement = announcement,
                    currentUserId = userId,
                    loading = false,
                )
            }
        }.onFailure {
            _state.update { it.copy(loading = false, error = true) }
        }
    }

    fun acknowledge(id: String) = viewModelScope.launch {
        _state.update { it.copy(acknowledging = true) }
        runCatching {
            repo.acknowledge(id)
            load(id)
        }.onFailure {
            _state.update { it.copy(acknowledging = false, errorKey = "announcement_ack_failed") }
        }
    }

    fun clearError() = _state.update { it.copy(errorKey = null) }

    fun delete(onSuccess: () -> Unit) = viewModelScope.launch {
        val id = _state.value.announcement?.id ?: return@launch
        _state.update { it.copy(deleting = true) }
        runCatching {
            repo.delete(id)
            onSuccess()
        }.onFailure {
            _state.update { it.copy(deleting = false, errorKey = "announcement_delete_failed") }
        }
    }
}

data class AnnouncementDetailUiState(
    val announcement: Announcement? = null,
    val currentUserId: String? = null,
    val loading: Boolean = false,
    val acknowledging: Boolean = false,
    val deleting: Boolean = false,
    val error: Boolean = false,
    val errorKey: String? = null,
)

@HiltViewModel
class AnnouncementTrackingViewModel @Inject constructor(
    private val repo: AnnouncementRepository,
    private val auth: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AnnouncementTrackingUiState())
    val state = _state.asStateFlow()

    fun load(id: String) = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        runCatching {
            val userId = auth.currentUser()?.id
            val announcement = repo.getById(id)
            if (!announcement.isCreator(userId)) {
                _state.update { it.copy(loading = false, forbidden = true) }
                return@runCatching
            }
            val acknowledged = announcement.recipients.filter { it.acknowledgedAt != null }
            val pending = announcement.recipients.filter { it.acknowledgedAt == null }
            val viewed = announcement.recipients.filter { it.viewedAt != null }
            val notViewed = announcement.recipients.filter { it.viewedAt == null }
            _state.update {
                it.copy(
                    announcement = announcement,
                    acknowledged = acknowledged,
                    pending = pending,
                    viewed = viewed,
                    notViewed = notViewed,
                    loading = false,
                )
            }
        }.onFailure {
            _state.update { it.copy(loading = false, error = true) }
        }
    }
}

data class AnnouncementTrackingUiState(
    val announcement: Announcement? = null,
    val acknowledged: List<uz.vazifa.app.domain.model.AnnouncementRecipient> = emptyList(),
    val pending: List<uz.vazifa.app.domain.model.AnnouncementRecipient> = emptyList(),
    val viewed: List<uz.vazifa.app.domain.model.AnnouncementRecipient> = emptyList(),
    val notViewed: List<uz.vazifa.app.domain.model.AnnouncementRecipient> = emptyList(),
    val loading: Boolean = false,
    val error: Boolean = false,
    val forbidden: Boolean = false,
)

@HiltViewModel
class SentAnnouncementsViewModel @Inject constructor(
    private val repo: AnnouncementRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SentAnnouncementsUiState())
    val state = _state.asStateFlow()

    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        runCatching {
            _state.update { it.copy(announcements = repo.getSent(), loading = false) }
        }.onFailure {
            _state.update { it.copy(loading = false) }
        }
    }

    fun delete(id: String, onFailure: () -> Unit = {}) = viewModelScope.launch {
        runCatching {
            repo.delete(id)
            load()
        }.onFailure {
            onFailure()
        }
    }
}

data class SentAnnouncementsUiState(
    val announcements: List<Announcement> = emptyList(),
    val loading: Boolean = false,
)

@HiltViewModel
class ReceivedAnnouncementsViewModel @Inject constructor(
    private val repo: AnnouncementRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ReceivedAnnouncementsUiState())
    val state = _state.asStateFlow()

    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        runCatching {
            _state.update { it.copy(announcements = repo.getReceived(), loading = false) }
        }.onFailure {
            _state.update { it.copy(loading = false) }
        }
    }
}

data class ReceivedAnnouncementsUiState(
    val announcements: List<Announcement> = emptyList(),
    val loading: Boolean = false,
)

private fun filterDeadlineHoursInput(v: String): String = buildString {
    var dotUsed = false
    for (ch in v) {
        when {
            ch.isDigit() -> append(ch)
            (ch == '.' || ch == ',') && !dotUsed -> {
                append('.')
                dotUsed = true
            }
        }
    }
}

private fun parseDeadlineHours(input: String): Double? {
    val normalized = input.trim().replace(',', '.')
    if (normalized.isBlank() || normalized == ".") return null
    return normalized.toDoubleOrNull()?.takeIf { it > 0 }
}

private fun minutesToHoursString(minutes: Long): String {
    val hours = minutes / 60.0
    val whole = hours.toLong()
    if (hours == whole.toDouble()) return whole.toString()
    val tenths = (hours * 10).roundToInt() / 10.0
    if (tenths == tenths.toLong().toDouble()) return tenths.toLong().toString()
    return String.format(java.util.Locale.US, "%.1f", tenths)
}
