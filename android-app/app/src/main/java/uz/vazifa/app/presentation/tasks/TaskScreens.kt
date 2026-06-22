package uz.vazifa.app.presentation.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import androidx.hilt.navigation.compose.hiltViewModel
import uz.vazifa.app.domain.model.Task
import uz.vazifa.app.domain.model.TaskStatus
import uz.vazifa.app.domain.model.isCreator
import uz.vazifa.app.domain.model.myAssignment
import uz.vazifa.app.presentation.components.*
import uz.vazifa.app.presentation.components.localizedStatus
import uz.vazifa.app.presentation.dashboard.TaskRow
import uz.vazifa.app.presentation.theme.GlassCard
import uz.vazifa.app.presentation.theme.LiquidBackground
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.util.TaskDeadlineCountdown
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

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
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.tasks, key = { it.id }) { task ->
                        val canManage = state.canAssignTasks && task.status != "cancelled"
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
            val isCompleted = myAssignment?.status == TaskStatus.COMPLETED.key

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
                                Text(
                                    "• $name — ${localizedStatus(a.status)}",
                                    color = LiquidTheme.text,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }
                }

                if (task.attachments.isNotEmpty()) {
                    Text(localized("task_photos"), color = LiquidTheme.textMuted, fontSize = 13.sp)
                    TaskAttachmentGrid(task.attachments.map { it.url })
                }

                if (task.comments.isNotEmpty()) {
                    Text(localized("task_comments"), color = LiquidTheme.textMuted, fontSize = 13.sp)
                    task.comments.forEach { c ->
                        GlassCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(c.author?.fullName ?: "", color = LiquidGlass.BlueLight, fontSize = 12.sp)
                                Text(c.body, color = LiquidTheme.text)
                            }
                        }
                    }
                }

                if (showAssigneeView && myAssignment != null && !isCompleted) {
                    if (myAssignment.status == TaskStatus.NEW.key) {
                        Button(
                            onClick = { viewModel.updateStatus(task.id, myAssignment.id, TaskStatus.ACCEPTED.key) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = LiquidGlass.Blue),
                        ) { Text(localized("status_accepted")) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(TaskStatus.IN_PROGRESS, TaskStatus.IN_REVIEW).forEach { st ->
                            FilterChip(
                                selected = myAssignment.status == st.key,
                                onClick = { viewModel.updateStatus(task.id, myAssignment.id, st.key) },
                                label = { Text(localizedStatus(st.key), fontSize = 11.sp) },
                            )
                        }
                    }
                    Button(
                        onClick = { showCompleteDialog = true },
                        enabled = !state.loading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(LiquidGlass.RadiusChip),
                        colors = ButtonDefaults.buttonColors(containerColor = LiquidGlass.Blue),
                    ) { Text(localized("task_complete_btn")) }
                }
            }
        }
    }

    if (showCompleteDialog) {
        val assignment = state.task?.assignments?.firstOrNull { it.assigneeId == state.currentUserId }
        AlertDialog(
            onDismissRequest = { showCompleteDialog = false },
            title = { Text(localized("task_complete_btn")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        completeComment,
                        onValueChange = { completeComment = it },
                        label = { Text(localized("task_complete_comment")) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                    OptionalTaskImagePicker(
                        imageUri = completeImageUri,
                        onImageSelected = { completeImageUri = it },
                        label = localized("task_add_photo"),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        assignment?.let {
                            viewModel.completeWithReport(taskId, it.id, completeComment, completeImageUri)
                        }
                        showCompleteDialog = false
                        completeComment = ""
                        completeImageUri = null
                    },
                ) { Text(localized("task_report_submit")) }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteDialog = false }) { Text(localized("com_cancel")) }
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
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadContacts()
        viewModel.loadForEdit()
    }
    LaunchedEffect(preselectedAssigneeIds) {
        preselectedAssigneeIds?.takeIf { it.isNotEmpty() }?.let { ids ->
            viewModel.preselectAssignees(ids)
            onPreselectConsumed()
        }
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

    val formContent: @Composable (PaddingValues) -> Unit = { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                state.title, viewModel::onTitle,
                label = { Text(localized("task_name")) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(LiquidGlass.RadiusInput),
                colors = fieldColors,
            )
            OutlinedTextField(
                state.description, viewModel::onDescription,
                label = { Text(localized("task_desc")) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(LiquidGlass.RadiusInput),
                colors = fieldColors,
            )
            OutlinedTextField(
                state.deadlineHours, viewModel::onDeadlineHours,
                label = { Text(localized("task_deadline_hours")) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(LiquidGlass.RadiusInput),
                colors = fieldColors,
            )
            OutlinedTextField(
                value = state.deadlineDateTime?.format(deadlineDisplayFmt).orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text(localized("task_deadline_datetime")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                shape = RoundedCornerShape(LiquidGlass.RadiusInput),
                colors = fieldColors,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = LiquidGlass.Blue)
                    }
                },
            )
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
                state.selectedContacts.forEach { c ->
                    EmployeeSelectRow(c, state.selectedIds.contains(c.id)) { viewModel.toggleAssignee(c.id) }
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
            Button(
                onClick = {
                    if (state.isEditMode) viewModel.update() else viewModel.create(createImageUri)
                },
                enabled = state.canCreate,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(LiquidGlass.RadiusChip),
                colors = ButtonDefaults.buttonColors(containerColor = LiquidGlass.Blue),
            ) {
                if (state.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = androidx.compose.ui.graphics.Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(localized(if (state.isEditMode) "com_save" else "task_create_btn"))
                }
            }
        }
    }

    val screenTitle = localized(if (state.isEditMode) "task_edit" else "task_create")
    if (showBack) {
        VazifaStackScaffold(title = screenTitle, onBack = onBack, content = formContent)
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
private fun EmployeeSelectRow(
    contact: uz.vazifa.app.domain.model.User,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    GlassCard(Modifier.fillMaxWidth().clickable(onClick = onToggle)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f)) {
                Text(contact.fullName, color = LiquidTheme.text)
                contact.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                    Text(phone, color = LiquidTheme.textMuted, fontSize = 12.sp)
                }
            }
        }
    }
}
