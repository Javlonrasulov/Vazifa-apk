package uz.vazifa.app.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import uz.vazifa.app.data.repository.AuthRepository
import uz.vazifa.app.data.remote.ApiClient
import uz.vazifa.app.data.remote.UserDto
import uz.vazifa.app.presentation.auth.LoginScreen
import uz.vazifa.app.presentation.dashboard.DirectorDashboardScreen
import uz.vazifa.app.presentation.notifications.NotificationGateScreen
import uz.vazifa.app.presentation.profile.ProfileScreen
import uz.vazifa.app.presentation.tasks.*
import uz.vazifa.app.presentation.theme.LiquidBackground
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidTheme
import javax.inject.Inject

@HiltViewModel
class NavViewModel @Inject constructor(
    val auth: AuthRepository,
    private val api: ApiClient,
) : ViewModel() {
    var currentUser by mutableStateOf<UserDto?>(null)
        private set

    fun checkAuth(onResult: (Boolean, UserDto?) -> Unit) {
        viewModelScope.launch {
            val user = auth.restoreSession()
            currentUser = user
            onResult(user != null, user)
        }
    }

    fun resolveAfterAuth(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val dest = if (auth.shouldSkipNotifGate()) Routes.MAIN else Routes.NOTIFICATION_GATE
            onReady(dest)
        }
    }

    fun setUser(user: UserDto) { currentUser = user }
}

@Composable
fun VazifaNavHost(viewModel: NavViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val user = viewModel.currentUser
    val isDirector = user?.canAssignTasks == true
    val showBottomNav = route == Routes.MAIN

    var selectedTab by remember { mutableStateOf(if (isDirector) AppTab.HOME else AppTab.TASKS) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        NavHost(
            navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.fillMaxSize().then(if (showBottomNav) Modifier.padding(bottom = BottomNavHeight) else Modifier),
        ) {
            composable(Routes.SPLASH) {
                LaunchedEffect(Unit) {
                    viewModel.checkAuth { ok, u ->
                        if (ok && u != null) {
                            viewModel.resolveAfterAuth { dest ->
                                selectedTab = if (u.canAssignTasks) AppTab.HOME else AppTab.TASKS
                                navController.navigate(dest) { popUpTo(Routes.SPLASH) { inclusive = true } }
                            }
                        } else {
                            navController.navigate(Routes.LOGIN) { popUpTo(Routes.SPLASH) { inclusive = true } }
                        }
                    }
                }
                LiquidBackground(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = LiquidGlass.Blue)
                    }
                }
            }
            composable(Routes.LOGIN) {
                LoginScreen(
                    onSuccess = {
                        viewModel.checkAuth { _, u ->
                            u?.let { viewModel.setUser(it) }
                            viewModel.resolveAfterAuth { dest ->
                                selectedTab = if (viewModel.currentUser?.canAssignTasks == true) AppTab.HOME else AppTab.TASKS
                                navController.navigate(dest) { popUpTo(Routes.LOGIN) { inclusive = true } }
                            }
                        }
                    },
                )
            }
            composable(Routes.NOTIFICATION_GATE) {
                NotificationGateScreen(
                    authRepository = viewModel.auth,
                    onGranted = {
                        selectedTab = if (viewModel.currentUser?.canAssignTasks == true) AppTab.HOME else AppTab.TASKS
                        navController.navigate(Routes.MAIN) { popUpTo(0) { inclusive = true } }
                    },
                )
            }
            composable(Routes.MAIN) {
                when (selectedTab) {
                    AppTab.HOME -> if (isDirector) {
                        DirectorDashboardScreen(
                            onTaskClick = { navController.navigate(Routes.taskDetail(it)) },
                            onCreateTask = { selectedTab = AppTab.CREATE },
                        )
                    } else {
                        TasksScreen(onTaskClick = { navController.navigate(Routes.taskDetail(it)) })
                    }
                    AppTab.TASKS -> TasksScreen(onTaskClick = { navController.navigate(Routes.taskDetail(it)) })
                    AppTab.CREATE -> if (isDirector) {
                        CreateTaskScreen(
                            showBack = false,
                            onBack = { selectedTab = AppTab.HOME },
                            onCreated = {
                                selectedTab = AppTab.TASKS
                            },
                        )
                    } else {
                        TasksScreen(onTaskClick = { navController.navigate(Routes.taskDetail(it)) })
                    }
                    AppTab.PROFILE -> ProfileScreen(
                        onLogout = { navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } } },
                    )
                }
            }
            composable(Routes.CREATE_TASK) {
                CreateTaskScreen(
                    onBack = { navController.popBackStack() },
                    onCreated = { navController.popBackStack() },
                )
            }
            composable(Routes.TASK_DETAIL, arguments = listOf(navArgument("taskId") { type = NavType.StringType })) { entry ->
                TaskDetailScreen(
                    taskId = entry.arguments?.getString("taskId").orEmpty(),
                    isDirector = isDirector,
                    onBack = { navController.popBackStack() },
                )
            }
        }

        if (showBottomNav) {
            VazifaBottomNav(
                selected = selectedTab,
                isDirector = isDirector,
                onSelect = { selectedTab = it },
                modifier = Modifier.align(Alignment.BottomCenter).zIndex(100f),
            )
        }
    }
}
