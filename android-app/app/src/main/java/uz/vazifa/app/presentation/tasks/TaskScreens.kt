package uz.vazifa.app.presentation.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import androidx.hilt.navigation.compose.hiltViewModel
import uz.vazifa.app.domain.model.Task
import uz.vazifa.app.domain.model.TaskAssignment
import uz.vazifa.app.domain.model.TaskStatus
import uz.vazifa.app.domain.model.canCreatorManage
import uz.vazifa.app.domain.model.isCompletedForUser
import uz.vazifa.app.domain.model.isCreator
import uz.vazifa.app.domain.model.myAssignment
import uz.vazifa.app.presentation.components.*
import uz.vazifa.app.presentation.components.localizedStatus
import uz.vazifa.app.presentation.dashboard.TaskRow
import uz.vazifa.app.presentation.theme.GlassCard
import uz.vazifa.app.presentation.theme.LiquidBackground
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.presentation.theme.VazifaColors
import java.time.Instant
import uz.vazifa.app.util.TaskDeadlineCountdown
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onTaskClick: (String) -> Unit,
    onEditTask: (String) -> Unit = {},
    viewModel: TasksViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var taskToDelete by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { viewModel.load() }

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
        title = localized("nav_tasks"),
        actions = { VazifaHeaderActions() },
    ) { padding ->
        LiquidBackground(Modifier.fillMaxSize()) {
            VazifaScreenBox(padding) {
                var selectedTab by remember { mutableIntStateOf(0) }
                val filteredTasks = remember(state.tasks, state.currentUserId, selectedTab) {
                    state.tasks.filter { task ->
                        val completed = task.isCompletedForUser(state.currentUserId)
                        if (selectedTab == 0) !completed else completed
                    }
                }
                Column(Modifier.fillMaxSize()) {
                    PrimaryTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        contentColor = LiquidGlass.Blue,
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text(localized("tasks_tab_new")) },
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text(localized("tasks_tab_completed")) },
                        )
                    }
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (filteredTasks.isEmpty()) {
                            item {
                                Text(
                                    localized("dash_empty"),
                                    color = LiquidTheme.textMuted,
                                    modifier = Modifier.padding(vertical = 24.dp),
                                )
                            }
                        } else {
                            items(filteredTasks, key = { it.id }) { task ->
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
    }
}

@Composable
fun TaskDetailScreen(
    taskId: String,
    canAssignTasks: Boolean,
    onBack: () -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showCompleteDialog by remember { mutableStateOf(false) }
    var completeComment by remember { mutableStateOf("") }
    var completeImageUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(taskId) { viewModel.load(taskId) }

    VazifaStackScaffold(title = localized("task_detail"), onBack = onBack) { padding ->
        state.task?.let { task ->
            val myAssignment = task.myAssignment(state.currentUserId)
            val isCreator = task.isCreator(state.currentUserId)
            val showManagerView = canAssignTasks && isCreator
            val showAssigneeView = myAssignment != null
            val showObserverView = !showManagerView && !showAssigneeView
            val isCompleted = task.isCompletedForUser(state.currentUserId)
            val displayStatus = myAssignment?.status ?: task.assignments.firstOrNull()?.status

            Column(
                Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(task.title, color = LiquidTheme.text, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        if (!task.description.isNullOrBlank()) {
                            Text(task.description, color = LiquidTheme.textMuted)
                        }
                        Text(
                            "${localized("task_deadline")}: ${TaskDeadlineCountdown.formatDisplay(task.deadlineAt)}",
                            color = LiquidTheme.textMuted,
                            fontSize = 13.sp,
                        )
                        if (!task.isCompletedForUser(state.currentUserId)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "${localized("task_time_remaining")}:",
                                    color = LiquidTheme.textMuted,
                                    fontSize = 13.sp,
                                )
                                TaskCountdownText(
                                    deadlineAt = task.deadlineAt,
                                    status = displayStatus,
                                    fontSize = 13.sp,
                                    showPrefix = false,
                                )
                            }
                        } else {
                            CompletedTaskTiming(
                                task = task,
                                completedAssignment = if (showManagerView) {
                                    null
                                } else {
                                    myAssignment?.takeIf { it.status == TaskStatus.COMPLETED.key }
                                },
                            )
                        }
                        if (showAssigneeView && !isCreator) {
                            Text(
                                localized("task_assigned_to_you"),
                                color = LiquidGlass.BlueLight,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            task.createdBy?.let {
                                Text("${localized("task_from")}: ${it.fullName}", color = LiquidTheme.textMuted, fontSize = 13.sp)
                            }
                            myAssignment?.let { a ->
                                Text(
                                    "${localized("task_status")}: ${localizedStatus(a.status)}",
                                    color = LiquidTheme.text,
                                    fontSize = 14.sp,
                                )
                            }
                        }
                        if (showManagerView) {
                            Text(localized("task_you_assigned"), color = LiquidGlass.BlueLight, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(localized("task_assignee_status"), color = LiquidTheme.textMuted, fontSize = 13.sp)
                            task.assignments.forEach { a ->
                                val name = a.assignee?.fullName ?: a.assigneeId
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        "• $name — ${localizedStatus(a.status)}",
                                        color = LiquidTheme.text,
                                        fontSize = 13.sp,
                                    )
                                    if (a.status == TaskStatus.COMPLETED.key && !a.completedAt.isNullOrBlank()) {
                                        Text(
                                            "  ${localized("task_completed_at")}: ${TaskDeadlineCountdown.formatDisplay(a.completedAt)}",
                                            color = LiquidTheme.textMuted,
                                            fontSize = 12.sp,
                                        )
                                    }
                                }
                            }
                        }
                        if (showObserverView) {
                            task.createdBy?.let { creator ->
                                val dept = creator.department?.takeIf { it.isNotBlank() }
                                Text(
                                    "${localized("dept_task_from")}: ${creator.fullName}${dept?.let { " ($it)" } ?: ""}",
                                    color = LiquidTheme.textMuted,
                                    fontSize = 13.sp,
                                )
                            }
                            Text(localized("task_assignee_status"), color = LiquidTheme.textMuted, fontSize = 13.sp)
                            task.assignments.forEach { a ->
                                val assignee = a.assignee
                                val dept = assignee?.department?.takeIf { it.isNotBlank() }
                                val name = assignee?.fullName ?: a.assigneeId
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                uz.vazifa.app.presentation.chat.ChatAvatar(
                                    name = assignee?.fullName ?: "?",
                                    online = assignee?.isOnline ?: false,
                                    size = 32.dp,
                                    showPresence = false,
                                    avatarUrl = assignee?.avatarUrl,
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        "${localized("dept_task_to")}: $name${dept?.let { " ($it)" } ?: ""} — ${localizedStatus(a.status)}",
                                        color = LiquidTheme.text,
                                        fontSize = 13.sp,
                                    )
                                    if (a.status == TaskStatus.COMPLETED.key && !a.completedAt.isNullOrBlank()) {
                                        Text(
                                            "  ${localized("task_completed_at")}: ${TaskDeadlineCountdown.formatDisplay(a.completedAt)}",
                                            color = LiquidTheme.textMuted,
                                            fontSize = 12.sp,
                                        )
                                    }
                                }
                                }
                            }
                            TaskDeadlineCountdown.durationBetween(task.startAt, task.deadlineAt)?.let { duration ->
                                val totalMinutes = duration.days * 24 * 60 + duration.hours * 60 + duration.minutes
                                if (totalMinutes > 0) {
                                    Text(
                                        "${localized("task_time_given")}: ${formatTaskDuration(duration.days, duration.hours, duration.minutes)}",
                                        color = LiquidTheme.textMuted,
                                        fontSize = 13.sp,
                                    )
                                }
                            }
                        }
                    }
                }

                if (task.attachments.isNotEmpty()) {
                    Text(localized("task_attachments"), color = LiquidTheme.textMuted, fontSize = 13.sp)
                    TaskAttachmentsList(task.attachments)
                }

                if (task.comments.isNotEmpty()) {
                    Text(localized("task_comments"), color = LiquidTheme.textMuted, fontSize = 13.sp)
                    task.comments.forEach { c ->
                        GlassCard(Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                uz.vazifa.app.presentation.chat.ChatAvatar(
                                    name = c.author?.fullName ?: "?",
                                    online = false,
                                    size = 36.dp,
                                    showPresence = false,
                                    avatarUrl = c.author?.avatarUrl,
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(c.author?.fullName ?: "", color = LiquidGlass.BlueLight, fontSize = 12.sp)
                                    Text(c.body, color = LiquidTheme.text)
                                }
                            }
                        }
                    }
                }

                if (showAssigneeView && myAssignment != null && !task.isCompletedForUser(state.currentUserId)) {
                    Button(
                        onClick = { showCompleteDialog = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = !state.loading,
                        shape = RoundedCornerShape(LiquidGlass.RadiusChip),
                        colors = ButtonDefaults.buttonColors(containerColor = VazifaColors.Success),
                    ) {
                        Text(localized("task_complete_btn"))
                    }
                }
            }
        }
    }

    if (showCompleteDialog) {
        val assignment = state.task?.myAssignment(state.currentUserId)
        AlertDialog(
            onDismissRequest = {
                if (!state.loading) {
                    showCompleteDialog = false
                    completeComment = ""
                    completeImageUri = null
                }
            },
            title = { Text(localized("task_complete_btn")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        completeComment,
                        onValueChange = { completeComment = it },
                        label = { Text(localized("task_complete_comment")) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        enabled = !state.loading,
                    )
                    OptionalTaskImagePicker(
                        imageUri = completeImageUri,
                        onImageSelected = { completeImageUri = it },
                        label = localized("task_add_photo"),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        assignment?.let {
                            viewModel.completeWithReport(taskId, it.id, completeComment, completeImageUri)
                        }
                        showCompleteDialog = false
                        completeComment = ""
                        completeImageUri = null
                    },
                    enabled = !state.loading && assignment != null,
                ) {
                    if (state.loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = androidx.compose.ui.graphics.Color.White,
                        )
                    } else {
                        Text(localized("task_report_submit"))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCompleteDialog = false
                        completeComment = ""
                        completeImageUri = null
                    },
                    enabled = !state.loading,
                ) {
                    Text(localized("com_cancel"))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(
    onBack: () -> Unit,
    onCreated: () -> Unit,
    showBack: Boolean = true,
    preselectedAssigneeIds: Set<String>? = null,
    onPreselectConsumed: () -> Unit = {},
    viewModel: CreateTaskViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val fieldColors = liquidGlassFieldColors()
    val zone = ZoneId.of("Asia/Tashkent")
    val deadlineDisplayFmt = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDateMillis by remember { mutableStateOf<Long?>(null) }
    var createImageUri by remember { mutableStateOf<Uri?>(null) }
    var showAllSelectedAssignees by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadContacts()
        viewModel.loadForEdit()
    }
    LaunchedEffect(preselectedAssigneeIds, state.contacts) {
        val ids = preselectedAssigneeIds?.takeIf { it.isNotEmpty() } ?: return@LaunchedEffect
        if (state.contacts.isEmpty()) return@LaunchedEffect
        if (state.selectedIds != ids) {
            viewModel.preselectAssignees(ids)
        }
        onPreselectConsumed()
    }
    LaunchedEffect(state.selectedIds.size) {
        if (state.selectedIds.size <= 2) showAllSelectedAssignees = false
    }
    val errorMessage = state.errorKey?.let { localized(it) }
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    LaunchedEffect(state.created) {
        if (state.created) {
            onCreated()
            viewModel.resetForm()
            createImageUri = null
        }
    }

    val submitCreate: () -> Unit = submit@{
        when {
            state.title.isBlank() -> viewModel.showTitleError()
            !state.isEditMode && state.selectedIds.isEmpty() -> viewModel.showAssigneeError()
            state.isEditMode -> viewModel.update()
            else -> viewModel.create(createImageUri)
        }
    }

    val formContent: @Composable (PaddingValues) -> Unit = { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
            OutlinedTextField(
                state.title, viewModel::onTitle,
                label = { Text(localized("task_name")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focus ->
                        if (!focus.isFocused && state.title.isBlank()) {
                            viewModel.showTitleError()
                        }
                    },
                isError = state.titleError && state.title.isBlank(),
                supportingText = if (state.titleError && state.title.isBlank()) {
                    { Text(localized("task_title_empty")) }
                } else {
                    null
                },
                shape = RoundedCornerShape(LiquidGlass.RadiusInput),
                colors = fieldColors,
            )
            DescriptionVoiceInput(
                value = state.description,
                onValueChange = viewModel::onDescription,
                voiceFile = state.voiceFile,
                onVoiceRecorded = viewModel::onVoiceRecorded,
                onVoiceRemove = viewModel::removeVoice,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    state.deadlineHours, viewModel::onDeadlineHours,
                    label = { Text(localized("task_deadline_hours")) },
                    modifier = Modifier.weight(0.38f),
                    shape = RoundedCornerShape(LiquidGlass.RadiusInput),
                    colors = fieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.deadlineDateTime?.format(deadlineDisplayFmt).orEmpty(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(localized("task_deadline_datetime")) },
                    modifier = Modifier
                        .weight(0.62f)
                        .clickable { showDatePicker = true },
                    shape = RoundedCornerShape(LiquidGlass.RadiusInput),
                    colors = fieldColors,
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = LiquidGlass.Blue)
                        }
                    },
                )
            }
            Text(localized("task_assignees"), color = LiquidTheme.textMuted, fontSize = 13.sp)
            if (!state.isEditMode) {
                Text(localized("task_assignee_hint"), color = LiquidTheme.textMuted, fontSize = 12.sp)
                if (state.selectedIds.isNotEmpty()) {
                    Text(
                        "${localized("task_selected_count")}: ${state.selectedIds.size}",
                        color = LiquidGlass.BlueLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                OutlinedTextField(
                    state.assigneeSearch, viewModel::onAssigneeSearch,
                    label = { Text(localized("task_search_employee")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(LiquidGlass.RadiusInput),
                    colors = fieldColors,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = LiquidTheme.textMuted) },
                    singleLine = true,
                )
                val selectedContacts = state.selectedContacts
                val visibleSelectedContacts = if (showAllSelectedAssignees || selectedContacts.size <= 2) {
                    selectedContacts
                } else {
                    selectedContacts.take(2)
                }
                val hiddenSelectedCount = (selectedContacts.size - 2).coerceAtLeast(0)
                visibleSelectedContacts.forEach { c ->
                    EmployeeSelectRow(c, state.selectedIds.contains(c.id)) { viewModel.toggleAssignee(c.id) }
                }
                if (hiddenSelectedCount > 0) {
                    TextButton(onClick = { showAllSelectedAssignees = !showAllSelectedAssignees }) {
                        Text(
                            if (showAllSelectedAssignees) {
                                localized("task_show_less_assignees")
                            } else {
                                "${localized("task_show_more_assignees")} (+$hiddenSelectedCount)"
                            },
                            color = LiquidGlass.BlueLight,
                            fontSize = 13.sp,
                        )
                    }
                }
                if (state.assigneeSearch.isNotBlank()) {
                    state.filteredContacts
                        .filter { it.id !in state.selectedIds }
                        .forEach { c ->
                            EmployeeSelectRow(c, false) { viewModel.toggleAssignee(c.id) }
                        }
                }
            } else if (state.selectedContacts.isNotEmpty()) {
                state.selectedContacts.forEach { c ->
                    Text("• ${c.fullName}", color = LiquidTheme.text, fontSize = 13.sp)
                }
            }
            if (!state.isEditMode) {
                OptionalTaskImagePicker(
                    imageUri = createImageUri,
                    onImageSelected = { createImageUri = it },
                    label = localized("task_add_photo"),
                )
            }
                Spacer(Modifier.height(8.dp))
            }
            CreateTaskSubmitBar(
                isEditMode = state.isEditMode,
                loading = state.loading,
                onSubmit = submitCreate,
            )
        }
    }

    val screenTitle = localized(if (state.isEditMode) "task_edit" else "task_create")
    if (showBack) {
        VazifaStackScaffold(
            title = screenTitle,
            onBack = onBack,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            content = formContent,
        )
    } else {
        VazifaTabScaffold(
            title = screenTitle,
            actions = { VazifaHeaderActions() },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            content = formContent,
        )
    }

    if (showDatePicker) {
        VazifaDatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            onConfirm = { millis ->
                pendingDateMillis = millis
                showDatePicker = false
                showTimePicker = true
            },
            initialDateMillis = state.deadlineDateTime
                ?.atZone(zone)
                ?.toInstant()
                ?.toEpochMilli(),
            zoneId = zone,
        )
    }

    if (showTimePicker) {
        val initial = state.deadlineDateTime
        VazifaTimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            onConfirm = { hour, minute ->
                val dateMillis = pendingDateMillis
                    ?: state.deadlineDateTime?.atZone(zone)?.toInstant()?.toEpochMilli()
                if (dateMillis != null) {
                    val date = Instant.ofEpochMilli(dateMillis).atZone(ZoneOffset.UTC).toLocalDate()
                    viewModel.onDeadlineDateTime(
                        LocalDateTime.of(date.year, date.monthValue, date.dayOfMonth, hour, minute),
                    )
                }
                showTimePicker = false
            },
            initialHour = initial?.hour ?: 10,
            initialMinute = initial?.minute ?: 0,
        )
    }
}

@Composable
private fun CompletedTaskTiming(
    task: Task,
    completedAssignment: TaskAssignment?,
) {
    val muted = LiquidTheme.textMuted
    val fontSize = 13.sp
    Text(
        "${localized("task_assigned_at")}: ${TaskDeadlineCountdown.formatDisplay(task.startAt)}",
        color = muted,
        fontSize = fontSize,
    )
    TaskDeadlineCountdown.durationBetween(task.startAt, task.deadlineAt)?.let { duration ->
        val totalMinutes = duration.days * 24 * 60 + duration.hours * 60 + duration.minutes
        if (totalMinutes > 0) {
            Text(
                "${localized("task_time_given")}: ${formatTaskDuration(duration.days, duration.hours, duration.minutes)}",
                color = muted,
                fontSize = fontSize,
            )
        }
    }
    completedAssignment?.completedAt?.takeIf { it.isNotBlank() }?.let { completedAt ->
        Text(
            "${localized("task_completed_at")}: ${TaskDeadlineCountdown.formatDisplay(completedAt)}",
            color = LiquidTheme.text,
            fontSize = fontSize,
        )
    }
}

@Composable
private fun CreateTaskSubmitBar(
    isEditMode: Boolean,
    loading: Boolean,
    onSubmit: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Button(
            onClick = onSubmit,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(LiquidGlass.RadiusChip),
            colors = ButtonDefaults.buttonColors(containerColor = LiquidGlass.Blue),
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = androidx.compose.ui.graphics.Color.White,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(localized(if (isEditMode) "com_save" else "task_create_btn"))
            }
        }
    }
}

@Composable
private fun EmployeeSelectRow(
    contact: uz.vazifa.app.domain.model.User,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    GlassCard(Modifier.fillMaxWidth().clickable(onClick = onToggle)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked, onCheckedChange = { onToggle() })
            uz.vazifa.app.presentation.chat.ChatAvatar(
                name = contact.fullName,
                online = contact.isOnline,
                size = 40.dp,
                showPresence = false,
                avatarUrl = contact.avatarUrl,
            )
            androidx.compose.foundation.layout.Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(contact.fullName, color = LiquidTheme.text)
                contact.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                    Text(phone, color = LiquidTheme.textMuted, fontSize = 12.sp)
                }
            }
        }
    }
}
