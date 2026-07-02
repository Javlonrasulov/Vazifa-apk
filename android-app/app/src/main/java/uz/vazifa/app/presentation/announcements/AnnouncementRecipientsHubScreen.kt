package uz.vazifa.app.presentation.announcements

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import uz.vazifa.app.presentation.components.VazifaStackScaffold
import uz.vazifa.app.presentation.components.localized
import uz.vazifa.app.presentation.dashboard.EmployeesHubContent
import uz.vazifa.app.presentation.dashboard.EmployeesTabViewModel
import uz.vazifa.app.presentation.theme.LiquidBackground
import uz.vazifa.app.presentation.theme.LiquidTheme

@Composable
fun AnnouncementRecipientsHubScreen(
    onBack: () -> Unit,
    onDepartmentClick: (String?, String) -> Unit,
    onSelectRecipients: (Set<String>) -> Unit,
    viewModel: EmployeesTabViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.load()
        while (true) {
            delay(30_000)
            viewModel.load()
        }
    }

    VazifaStackScaffold(
        title = localized("announcement_create"),
        onBack = onBack,
    ) { padding ->
        LiquidBackground(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                when {
                    state.loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AnnouncementAccent.Primary)
                        }
                    }
                    else -> {
                        EmployeesHubContent(
                            totalEmployees = state.totalEmployees,
                            departments = state.departments,
                            filteredEmployees = state.filteredEmployees,
                            searchQuery = state.searchQuery,
                            onSearch = viewModel::onSearch,
                            onDepartmentClick = { dept -> onDepartmentClick(dept, state.searchQuery) },
                            onAssignTask = onSelectRecipients,
                            topContent = { AnnouncementScreenBanner() },
                        )
                    }
                }
            }
        }
    }
}

