package uz.vazifa.app.presentation.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import uz.vazifa.app.domain.model.TaskStatus
import uz.vazifa.app.presentation.dashboard.TaskRow
import uz.vazifa.app.presentation.theme.LiquidBackground
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.presentation.theme.VazifaColors

@Composable
fun TasksScreen(
    onTaskClick: (String) -> Unit,
    viewModel: TasksViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    LiquidBackground(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text("Vazifalarim", color = LiquidTheme.text, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            }
            items(state.tasks, key = { it.id }) { task ->
                TaskRow(task) { onTaskClick(task.id) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: String,
    isDirector: Boolean,
    onBack: () -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(taskId) { viewModel.load(taskId) }

    Scaffold(
        containerColor = LiquidTheme.bg,
        topBar = {
            TopAppBar(
                title = { Text("Vazifa", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LiquidTheme.bgMid),
            )
        },
    ) { padding ->
        state.task?.let { task ->
            Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(task.title, color = LiquidTheme.text, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text(task.description ?: "", color = LiquidTheme.textMuted)
                Text("Muddat: ${task.deadlineAt.take(16).replace('T', ' ')}", color = LiquidTheme.textMuted, fontSize = 13.sp)

                val assignment = task.assignments.firstOrNull()
                if (!isDirector && assignment != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(TaskStatus.ACCEPTED, TaskStatus.IN_PROGRESS, TaskStatus.IN_REVIEW, TaskStatus.COMPLETED).forEach { st ->
                            FilterChip(
                                selected = assignment.status == st.key,
                                onClick = { viewModel.updateStatus(task.id, assignment.id, st.key) },
                                label = { Text(st.label, fontSize = 11.sp) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(onBack: () -> Unit, onCreated: () -> Unit, viewModel: CreateTaskViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadContacts() }
    LaunchedEffect(state.created) { if (state.created) onCreated() }

    Scaffold(
        containerColor = LiquidTheme.bg,
        topBar = {
            TopAppBar(
                title = { Text("Yangi vazifa") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(state.title, viewModel::onTitle, label = { Text("Nomi") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.description, viewModel::onDescription, label = { Text("Tavsif") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            OutlinedTextField(state.deadlineHours, viewModel::onDeadlineHours, label = { Text("Muddat (soat)") }, modifier = Modifier.fillMaxWidth())
            Text("Mas'ul xodimlar", color = LiquidTheme.textMuted, fontSize = 13.sp)
            state.contacts.forEach { c ->
                Row(Modifier.fillMaxWidth().clickable { viewModel.toggleAssignee(c.id) }) {
                    Checkbox(state.selectedIds.contains(c.id), onCheckedChange = { viewModel.toggleAssignee(c.id) })
                    Text(c.fullName, color = LiquidTheme.text)
                }
            }
            Button(
                onClick = viewModel::create,
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = VazifaColors.Primary),
            ) {
                Text("Yaratish")
            }
        }
    }
}
