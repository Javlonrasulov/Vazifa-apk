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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import uz.vazifa.app.data.repository.AuthRepository
import uz.vazifa.app.data.remote.UserDto
import uz.vazifa.app.presentation.auth.LoginScreen
import uz.vazifa.app.presentation.dashboard.DirectorDashboardScreen
import uz.vazifa.app.presentation.dashboard.DashboardSection
import uz.vazifa.app.presentation.dashboard.DashboardSectionScreen
import uz.vazifa.app.presentation.dashboard.EmployeesTabScreen
import uz.vazifa.app.presentation.dashboard.EmployeeDetailScreen
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
) : ViewModel() {
    var currentUser by mutableStateOf<UserDto?>(null)
        private set
    var bootRoute by mutableStateOf<String?>(null)
        private set

    fun markBootRoute(route: String) {
        bootRoute = route
    }

    fun clearBootRoute() {
        bootRoute = null
    }

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
            markBootRoute(dest)
            onReady(dest)
        }
    }

    fun setUser(user: UserDto) {
        currentUser = user
    }

    fun refreshUser(onSessionExpired: () -> Unit = {}) {
        viewModelScope.launch {
            val hadSession = auth.hasStoredSessionAsync()
            val user = auth.currentUser()
            if (user != null) {
                currentUser = user
            } else if (hadSession) {
                currentUser = null
                onSessionExpired()
            }
        }
    }

    fun ensureNotifications(onAllowed: () -> Unit, onBlocked: () -> Unit) {
        viewModelScope.launch {
            if (auth.shouldSkipNotifGate()) onAllowed() else onBlocked()
        }
    }
}

@Composable
fun VazifaNavHost(
    pendingTaskId: String? = null,
    onPendingTaskConsumed: () -> Unit = {},
    viewModel: NavViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val user = viewModel.currentUser
    val isDirector = user?.canAssignTasks == true
    val showDepartmentTasks = !user?.visibleDepartments.isNullOrEmpty()
    val showBottomNav = route == Routes.MAIN
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var selectedTab by remember { mutableStateOf(if (isDirector) AppTab.HOME else AppTab.TASKS) }
    var preselectedAssigneeIds by remember { mutableStateOf<Set<String>?>(null) }
    val startDestination = viewModel.bootRoute ?: Routes.SPLASH
    val lifecycleOwner = LocalLifecycleOwner.current

    fun redirectToNotificationGate() {
        viewModel.markBootRoute(Routes.NOTIFICATION_GATE)
        navController.navigate(Routes.NOTIFICATION_GATE) {
            popUpTo(0) { inclusive = true }
        }
    }

    fun requiresNotificationAccess(route: String?): Boolean {
        if (route == null) return false
        return route !in setOf(Routes.SPLASH, Routes.LOGIN, Routes.NOTIFICATION_GATE)
    }

    DisposableEffect(lifecycleOwner, route) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (route == Routes.MAIN) {
                    viewModel.refreshUser {
                        viewModel.clearBootRoute()
                        navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                    }
                }
                if (requiresNotificationAccess(route)) {
                    viewModel.ensureNotifications(
                        onAllowed = {},
                        onBlocked = { redirectToNotificationGate() },
                    )
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(startDestination) {
        if (startDestination == Routes.MAIN && !viewModel.auth.shouldSkipNotifGate()) {
            redirectToNotificationGate()
        }
    }

    LaunchedEffect(pendingTaskId, route, user) {
        val taskId = pendingTaskId ?: return@LaunchedEffect
        if (user == null) return@LaunchedEffect
        if (route == Routes.LOGIN || route == Routes.SPLASH || route == Routes.NOTIFICATION_GATE) return@LaunchedEffect
        if (route != Routes.taskDetail(taskId)) {
            navController.navigate(Routes.taskDetail(taskId))
        }
        onPendingTaskConsumed()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        NavHost(
            navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize().then(
                if (showBottomNav) Modifier.padding(bottom = BottomNavHeight + navigationBarPadding) else Modifier,
            ),
        ) {
            composable(Routes.SPLASH) {
                LaunchedEffect(Unit) {
                    if (viewModel.bootRoute != null) return@LaunchedEffect
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
                        viewModel.markBootRoute(Routes.MAIN)
                        selectedTab = if (viewModel.currentUser?.canAssignTasks == true) AppTab.HOME else AppTab.TASKS
                        navController.navigate(Routes.MAIN) { popUpTo(0) { inclusive = true } }
                    },
                )
            }
            composable(Routes.MAIN) {
                val goLogin = {
                    viewModel.clearBootRoute()
                    navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                }
                LaunchedEffect(Unit) {
                    viewModel.refreshUser(onSessionExpired = goLogin)
                    if (!viewModel.auth.shouldSkipNotifGate()) {
                        redirectToNotificationGate()
                    }
                }
                LaunchedEffect(isDirector) {
                    if (isDirector && selectedTab == AppTab.TASKS) {
                        selectedTab = AppTab.HOME
                    }
                }
                when (selectedTab) {
                    AppTab.HOME -> if (isDirector) {
                        DirectorDashboardScreen(
                            onTaskClick = { navController.navigate(Routes.taskDetail(it)) },
                            onCreateTask = { selectedTab = AppTab.CREATE },
                            onEditTask = { navController.navigate(Routes.editTask(it)) },
                            onSectionClick = { section ->
                                when (section) {
                                    DashboardSection.EMPLOYEES -> selectedTab = AppTab.EMPLOYEES
                                    else -> navController.navigate(Routes.dashSection(section.route))
                                }
                            },
                        )
                    } else {
                        TasksScreen(
                            onTaskClick = { navController.navigate(Routes.taskDetail(it)) },
                            onEditTask = { navController.navigate(Routes.editTask(it)) },
                        )
                    }
                    AppTab.EMPLOYEES -> if (isDirector) {
                        EmployeesTabScreen(
                            onEmployeeClick = { navController.navigate(Routes.employeeDetail(it)) },
                            onAssignTask = { ids ->
                                preselectedAssigneeIds = ids
                                selectedTab = AppTab.CREATE
                            },
                        )
                    } else {
                        TasksScreen(
                            onTaskClick = { navController.navigate(Routes.taskDetail(it)) },
                            onEditTask = { navController.navigate(Routes.editTask(it)) },
                        )
                    }
                    AppTab.TASKS -> TasksScreen(
                        onTaskClick = { navController.navigate(Routes.taskDetail(it)) },
                        onEditTask = { navController.navigate(Routes.editTask(it)) },
                    )
                    AppTab.DEPT_TASKS -> DepartmentTasksScreen(
                        onTaskClick = { navController.navigate(Routes.taskDetail(it)) },
                    )
                    AppTab.CREATE -> if (isDirector) {
                        CreateTaskScreen(
                            showBack = false,
                            onBack = { selectedTab = AppTab.HOME },
                            onCreated = { selectedTab = AppTab.TASKS },
                            preselectedAssigneeIds = preselectedAssigneeIds,
                            onPreselectConsumed = { preselectedAssigneeIds = null },
                        )
                    } else {
                        TasksScreen(onTaskClick = { navController.navigate(Routes.taskDetail(it)) })
                    }
                    AppTab.PROFILE -> ProfileScreen(
                        onLogout = {
                            viewModel.clearBootRoute()
                            navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                        },
                    )
                }
            }
            composable(
                Routes.DASH_SECTION,
                arguments = listOf(navArgument("section") { type = NavType.StringType }),
            ) { entry ->
                DashboardSectionScreen(
                    onBack = { navController.popBackStack() },
                    onTaskClick = { navController.navigate(Routes.taskDetail(it)) },
                    onEditTask = { navController.navigate(Routes.editTask(it)) },
                    currentUserId = user?.id,
                    onAssignTask = { ids ->
                        preselectedAssigneeIds = ids
                        navController.popBackStack()
                        selectedTab = AppTab.CREATE
                    },
                    onEmployeeClick = { employeeId ->
                        navController.navigate(Routes.employeeDetail(employeeId))
                    },
                )
            }
            composable(
                Routes.EMPLOYEE_DETAIL,
                arguments = listOf(navArgument("employeeId") { type = NavType.StringType }),
            ) {
                EmployeeDetailScreen(
                    onBack = { navController.popBackStack() },
                    onTaskClick = { navController.navigate(Routes.taskDetail(it)) },
                    onAssignTask = { employeeId ->
                        preselectedAssigneeIds = setOf(employeeId)
                        navController.popBackStack(Routes.MAIN, false)
                        selectedTab = AppTab.CREATE
                    },
                )
            }
            composable(Routes.CREATE_TASK) {
                CreateTaskScreen(
                    onBack = { navController.popBackStack() },
                    onCreated = { navController.popBackStack() },
                )
            }
            composable(
                Routes.EDIT_TASK,
                arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
            ) {
                CreateTaskScreen(
                    onBack = { navController.popBackStack() },
                    onCreated = { navController.popBackStack() },
                )
            }
            composable(Routes.TASK_DETAIL, arguments = listOf(navArgument("taskId") { type = NavType.StringType })) { entry ->
                TaskDetailScreen(
                    taskId = entry.arguments?.getString("taskId").orEmpty(),
                    canAssignTasks = isDirector,
                    onBack = { navController.popBackStack() },
                )
            }
        }

        if (showBottomNav) {
            VazifaBottomNav(
                selected = selectedTab,
                isDirector = isDirector,
                showDepartmentTasks = showDepartmentTasks,
                onSelect = { selectedTab = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .zIndex(100f),
            )
        }
    }
}
