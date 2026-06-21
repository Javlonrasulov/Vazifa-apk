package uz.vazifa.app.presentation.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import uz.vazifa.app.domain.model.DashboardStats
import uz.vazifa.app.domain.model.Task
import uz.vazifa.app.presentation.theme.LiquidBackground
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.presentation.theme.VazifaColors
import uz.vazifa.app.presentation.theme.liquidGlassThemed

@Composable
fun DirectorDashboardScreen(
    onTaskClick: (String) -> Unit,
    onCreateTask: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    LiquidBackground(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Dashboard", color = LiquidTheme.text, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    FloatingActionButton(
                        onClick = onCreateTask,
                        containerColor = VazifaColors.Primary,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.White)
                    }
                }
            }
            item {
                state.stats?.let { StatsGrid(it) }
            }
            item {
                Text("So'nggi vazifalar", color = LiquidTheme.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
            items(state.tasks.take(5), key = { it.id }) { task ->
                TaskRow(task, onClick = { onTaskClick(task.id) })
            }
        }
    }
}

@Composable
private fun StatsGrid(stats: DashboardStats) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Xodimlar", "${stats.totalEmployees}", Icons.Default.People, Modifier.weight(1f))
            StatCard("Faol", "${stats.activeTasks}", Icons.Default.Assignment, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Bajarilgan", "${stats.completedTasks}", Icons.Default.CheckCircle, Modifier.weight(1f), VazifaColors.Success)
            StatCard("Kechikkan", "${stats.overdueTasks}", Icons.Default.Warning, Modifier.weight(1f), VazifaColors.Danger)
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier, color: Color = VazifaColors.Primary) {
    Column(modifier.liquidGlassThemed().padding(14.dp)) {
        Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, color = LiquidTheme.textMuted, fontSize = 11.sp)
        Text(value, color = LiquidTheme.text, fontWeight = FontWeight.Bold, fontSize = 20.sp)
    }
}

@Composable
fun TaskRow(task: Task, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().liquidGlassThemed().clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(task.title, color = LiquidTheme.text, fontWeight = FontWeight.SemiBold)
            Text(task.deadlineAt.take(16).replace('T', ' '), color = LiquidTheme.textMuted, fontSize = 12.sp)
        }
        AssistChip(onClick = {}, label = { Text(task.priority, fontSize = 10.sp) })
    }
}
