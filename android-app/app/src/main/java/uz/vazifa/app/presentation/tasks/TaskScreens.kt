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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import uz.vazifa.app.domain.model.TaskStatus
import uz.vazifa.app.presentation.components.*
import uz.vazifa.app.presentation.components.statusLabelKey
import uz.vazifa.app.presentation.dashboard.TaskRow
import uz.vazifa.app.presentation.theme.GlassCard
import uz.vazifa.app.presentation.theme.LiquidBackground
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidTheme
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun TasksScreen(onTaskClick: (String) -> Unit, viewModel: TasksViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    VazifaTabScaffold(
        title = localized("nav_tasks"),
        actions = { VazifaHeaderActions() },
    ) { padding ->
        LiquidBackground(Modifier.fillMaxSize()) {
            VazifaScreenBox(padding) {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.tasks, key = { it.id }) { task ->
                        TaskRow(task) { onTaskClick(task.id) }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskDetailScreen(taskId: String, isDirector: Boolean, onBack: () -> Unit, viewModel: TaskDetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(taskId) { viewModel.load(taskId) }

    VazifaStackScaffold(title = localized("task_detail"), onBack = onBack) { padding ->
        state.task?.let { task ->
            GlassCard(Modifier.padding(padding).padding(16.dp).fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(task.title, color = LiquidTheme.text, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text(task.description ?: "", color = LiquidTheme.textMuted)
                    Text(
                        "${localized("task_deadline")}: ${task.deadlineAt.take(16).replace('T', ' ')}",
                        color = LiquidTheme.textMuted,
                        fontSize = 13.sp,
                    )

                    val assignment = task.assignments.firstOrNull()
                    if (!isDirector && assignment != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(TaskStatus.ACCEPTED, TaskStatus.IN_PROGRESS, TaskStatus.IN_REVIEW, TaskStatus.COMPLETED).forEach { st ->
                                FilterChip(
                                    selected = assignment.status == st.key,
                                    onClick = { viewModel.updateStatus(task.id, assignment.id, st.key) },
                                    label = { Text(localized(statusLabelKey(st.key)), fontSize = 11.sp) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(
    onBack: () -> Unit,
    onCreated: () -> Unit,
    showBack: Boolean = true,
    viewModel: CreateTaskViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val fieldColors = liquidGlassFieldColors()
    val zone = ZoneId.of("Asia/Tashkent")
    val deadlineDisplayFmt = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDateMillis by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) { viewModel.loadContacts() }
    LaunchedEffect(state.created) {
        if (state.created) {
            onCreated()
            viewModel.resetForm()
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
            OutlinedTextField(
                state.assigneeSearch, viewModel::onAssigneeSearch,
                label = { Text(localized("task_search_employee")) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(LiquidGlass.RadiusInput),
                colors = fieldColors,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = LiquidTheme.textMuted) },
                singleLine = true,
            )
            val visibleContacts = if (state.assigneeSearch.isNotBlank()) {
                state.filteredContacts
            } else {
                state.selectedContacts
            }
            visibleContacts.forEach { c ->
                GlassCard(Modifier.fillMaxWidth().clickable { viewModel.toggleAssignee(c.id) }) {
                    Row(Modifier.padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(state.selectedIds.contains(c.id), onCheckedChange = { viewModel.toggleAssignee(c.id) })
                        Column(Modifier.weight(1f)) {
                            Text(c.fullName, color = LiquidTheme.text)
                            c.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                                Text(phone, color = LiquidTheme.textMuted, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            Button(
                onClick = viewModel::create,
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(LiquidGlass.RadiusChip),
                colors = ButtonDefaults.buttonColors(containerColor = LiquidGlass.Blue),
            ) {
                Text(localized("task_create_btn"))
            }
        }
    }

    if (showBack) {
        VazifaStackScaffold(title = localized("task_create"), onBack = onBack, content = formContent)
    } else {
        VazifaTabScaffold(
            title = localized("task_create"),
            actions = { VazifaHeaderActions() },
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
