package uz.vazifa.app.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.vazifa.app.data.repository.AppSettingsRepository
import uz.vazifa.app.data.repository.AuthRepository
import uz.vazifa.app.data.repository.ChatRepository
import uz.vazifa.app.data.repository.ChatUnreadRepository
import uz.vazifa.app.data.repository.NotificationInboxRepository
import uz.vazifa.app.data.remote.ChatEvent
import uz.vazifa.app.data.remote.UserDto
import uz.vazifa.app.data.remote.canAssignTasksInApp
import uz.vazifa.app.presentation.security.ScreenshotPolicyEffect
import uz.vazifa.app.localization.AppLanguage
import uz.vazifa.app.localization.AppStrings
import uz.vazifa.app.util.InboxPreview
import uz.vazifa.app.presentation.auth.LoginScreen
import uz.vazifa.app.presentation.dashboard.DirectorDashboardScreen
import uz.vazifa.app.presentation.dashboard.DashboardSection
import uz.vazifa.app.presentation.dashboard.DashboardSectionScreen
import uz.vazifa.app.presentation.dashboard.EmployeesTabScreen
import uz.vazifa.app.presentation.dashboard.DepartmentEmployeesScreen
import uz.vazifa.app.presentation.dashboard.EmployeeDetailScreen
import uz.vazifa.app.presentation.notifications.NotificationGateScreen
import uz.vazifa.app.presentation.components.localized
import uz.vazifa.app.presentation.chat.ChatListScreen
import uz.vazifa.app.presentation.chat.ChatConversationScreen
import uz.vazifa.app.presentation.chat.NewChatScreen
import uz.vazifa.app.presentation.chat.CreateRoomScreen
import uz.vazifa.app.presentation.chat.RoomConversationScreen
import uz.vazifa.app.presentation.profile.ProfileScreen
import uz.vazifa.app.presentation.splash.SplashScreen
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.presentation.tasks.*
import uz.vazifa.app.presentation.announcements.*

import javax.inject.Inject

@HiltViewModel
class NavViewModel @Inject constructor(
    val auth: AuthRepository,
    val chat: ChatRepository,
    private val inbox: NotificationInboxRepository,
    private val chatUnread: ChatUnreadRepository,
    private val settings: AppSettingsRepository,
) : ViewModel() {
    var currentUser by mutableStateOf<UserDto?>(null)
        private set
    var bootRoute by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            chat.events.collect { ev ->
                when (ev) {
                    is ChatEvent.NewMessage -> {
                        if (!uz.vazifa.app.AppForegroundState.isInForeground) return@collect
                        val me = currentUser?.id ?: return@collect
                        if (ev.message.senderId == me) return@collect
                        chatUnread.increment()
                        val lang = settings.language.first()
                        val peer = chat.knownPeer(ev.message.senderId)
                        val title = peer?.displayName
                            ?: AppStrings.t(lang, "nav_chat")
                        val body = InboxPreview.chatBody(
                            ev.message.type,
                            ev.message.body,
                            ev.message.fileName,
                            lang,
                        )
                        inbox.add(
                            title = title,
                            body = body,
                            type = "chat",
                            chatUserId = ev.message.senderId,
                        )
                    }
                    is ChatEvent.RoomNewMessage -> {
                        if (!uz.vazifa.app.AppForegroundState.isInForeground) return@collect
                        val me = currentUser?.id ?: return@collect
                        if (ev.message.senderId == me) return@collect
                        chatUnread.increment()
                        val lang = settings.language.first()
                        val senderName = ev.message.sender?.fullName
                            ?: AppStrings.t(lang, "nav_chat")
                        val preview = InboxPreview.chatBody(
                            ev.message.type,
                            ev.message.body,
                            ev.message.fileName,
                            lang,
                        )
                        inbox.add(
                            title = senderName,
                            body = preview,
                            type = "room",
                            roomId = ev.message.roomId,
                        )
                    }
                    else -> Unit
                }
            }
        }
    }

    fun connectChatSocket() = chat.connect()

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
            if (user != null) {
                connectChatSocket()
                runCatching { chatUnread.setCount(chat.unreadCount()) }
            }
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

    suspend fun resolveSplashDestination(): String {
        bootRoute?.let { route ->
            if (route == Routes.LOGIN) return route
            val user = auth.restoreSessionForBoot()
            if (user == null) {
                bootRoute = null
                currentUser = null
                return Routes.LOGIN
            }
            currentUser = user
            connectChatSocket()
            return route
        }
        val user = auth.restoreSessionForBoot()
        currentUser = user
        if (user == null) return Routes.LOGIN
        connectChatSocket()
        val dest = if (auth.shouldSkipNotifGate()) Routes.MAIN else Routes.NOTIFICATION_GATE
        markBootRoute(dest)
        return dest
    }

    fun setUser(user: UserDto) {
        currentUser = user
        connectChatSocket()
    }

    fun refreshUser(onSessionExpired: () -> Unit = {}) {
        viewModelScope.launch {
            val user = auth.currentUser()
            if (user != null) {
                currentUser = user
                connectChatSocket()
            } else {
                currentUser = null
                onSessionExpired()
            }
        }
    }

    fun syncPushToken() {
        viewModelScope.launch { auth.registerPushToken() }
    }

    fun ensureNotifications(onAllowed: () -> Unit, onBlocked: () -> Unit) {
        viewModelScope.launch {
            if (auth.shouldSkipNotifGate()) onAllowed() else onBlocked()
        }
    }

    fun clearNotificationBadge() {
        viewModelScope.launch { inbox.clearAll() }
    }

    val chatUnreadCount = chatUnread.unreadCount.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        0,
    )

    fun clearChatUnread() {
        viewModelScope.launch { chatUnread.clear() }
    }
}

@Composable
fun VazifaNavHost(
    pendingTaskId: String? = null,
    onPendingTaskConsumed: () -> Unit = {},
    pendingChatUserId: String? = null,
    pendingChatPeerName: String? = null,
    onPendingChatConsumed: () -> Unit = {},
    pendingRoomId: String? = null,
    onPendingRoomConsumed: () -> Unit = {},
    pendingAnnouncementId: String? = null,
    onPendingAnnouncementConsumed: () -> Unit = {},
    viewModel: NavViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val user = viewModel.currentUser
    val canAssignTasks = user?.canAssignTasksInApp() == true
    ScreenshotPolicyEffect(allowScreenshot = user?.allowScreenshot != false)
    val showBottomNav = route == Routes.MAIN
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val chatUnreadCount by viewModel.chatUnreadCount.collectAsState()

    var selectedTab by remember { mutableStateOf(AppTab.HOME) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var showSupportSheet by remember { mutableStateOf(false) }
    var preselectedAssigneeIds by remember { mutableStateOf<Set<String>?>(null) }
    val skipSplash = viewModel.bootRoute != null
    var splashDone by remember { mutableStateOf(skipSplash) }
    var destination by remember { mutableStateOf(viewModel.bootRoute) }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        if (skipSplash) return@LaunchedEffect
        coroutineScope {
            val boot = async { viewModel.resolveSplashDestination() }
            val minWait = async { delay(350) }
            val resolved = boot.await()
            destination = resolved
            if (resolved == Routes.MAIN) selectedTab = AppTab.HOME
            minWait.await()
            splashDone = true
        }
    }

    val showNav = destination != null
    val startDestination = destination ?: Routes.LOGIN

    fun redirectToNotificationGate() {
        viewModel.markBootRoute(Routes.NOTIFICATION_GATE)
        navController.navigate(Routes.NOTIFICATION_GATE) {
            popUpTo(0) { inclusive = true }
        }
    }

    fun requiresNotificationAccess(route: String?): Boolean {
        if (route == null) return false
        return route !in setOf(Routes.LOGIN, Routes.NOTIFICATION_GATE)
    }

    DisposableEffect(lifecycleOwner, route) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (route == Routes.MAIN) {
                    viewModel.refreshUser {
                        viewModel.clearBootRoute()
                        navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                    }
                    viewModel.syncPushToken()
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

    LaunchedEffect(startDestination, splashDone) {
        if (!splashDone) return@LaunchedEffect
        if (startDestination == Routes.MAIN && !viewModel.auth.shouldSkipNotifGate()) {
            redirectToNotificationGate()
        }
    }

    LaunchedEffect(pendingTaskId, route, user) {
        val taskId = pendingTaskId ?: return@LaunchedEffect
        if (user == null) return@LaunchedEffect
        if (route == Routes.LOGIN || route == Routes.NOTIFICATION_GATE) return@LaunchedEffect
        viewModel.clearNotificationBadge()
        if (route != Routes.taskDetail(taskId)) {
            navController.navigate(Routes.taskDetail(taskId))
        }
        onPendingTaskConsumed()
    }

    LaunchedEffect(pendingChatUserId, route, user) {
        val chatUserId = pendingChatUserId ?: return@LaunchedEffect
        if (user == null) return@LaunchedEffect
        if (route == Routes.LOGIN || route == Routes.NOTIFICATION_GATE) return@LaunchedEffect
        viewModel.clearChatUnread()
        if (route == Routes.MAIN) selectedTab = AppTab.CHAT
        viewModel.auth.ensureSessionForApi()
        val peerName = pendingChatPeerName?.takeIf { it.isNotBlank() }
            ?: runCatching {
                viewModel.chat.resolvePeer(chatUserId)?.displayName.orEmpty()
            }.getOrElse { "" }
        if (peerName.isNotBlank()) {
            viewModel.chat.rememberPeer(
                uz.vazifa.app.domain.model.ChatPeer(id = chatUserId, fullName = peerName),
            )
        }
        navController.navigate(Routes.chatConversation(chatUserId, peerName))
        onPendingChatConsumed()
    }

    LaunchedEffect(pendingRoomId, route, user) {
        val roomId = pendingRoomId ?: return@LaunchedEffect
        if (user == null) return@LaunchedEffect
        if (route == Routes.LOGIN || route == Routes.NOTIFICATION_GATE) return@LaunchedEffect
        viewModel.clearChatUnread()
        if (route == Routes.MAIN) selectedTab = AppTab.CHAT
        navController.navigate(Routes.roomConversation(roomId))
        onPendingRoomConsumed()
    }

    LaunchedEffect(pendingAnnouncementId, route, user) {
        val announcementId = pendingAnnouncementId ?: return@LaunchedEffect
        if (user == null) return@LaunchedEffect
        if (route == Routes.LOGIN || route == Routes.NOTIFICATION_GATE) return@LaunchedEffect
        viewModel.clearNotificationBadge()
        if (route != Routes.announcementDetail(announcementId)) {
            navController.navigate(Routes.announcementDetail(announcementId))
        }
        onPendingAnnouncementConsumed()
    }

    CompositionLocalProvider(
        LocalTaskNavigator provides { taskId ->
            navController.navigate(Routes.taskDetail(taskId))
        },
        LocalInboxNavigator provides { item ->
            when {
                item.roomId != null -> {
                    if (route == Routes.MAIN) selectedTab = AppTab.CHAT
                    navController.navigate(Routes.roomConversation(item.roomId))
                }
                item.chatUserId != null -> {
                    if (route == Routes.MAIN) selectedTab = AppTab.CHAT
                    navController.navigate(Routes.chatConversation(item.chatUserId, item.title))
                }
                item.taskId != null -> navController.navigate(Routes.taskDetail(item.taskId))
                item.announcementId != null -> navController.navigate(Routes.announcementDetail(item.announcementId))
            }
        },
    ) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (showNav) {
        NavHost(
            navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize().padding(
                bottom = if (showBottomNav) BottomNavHeight + navigationBarPadding else navigationBarPadding,
            ),
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    onSuccess = {
                        viewModel.checkAuth { _, u ->
                            u?.let { viewModel.setUser(it) }
                            viewModel.resolveAfterAuth { dest ->
                                selectedTab = AppTab.HOME
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
                        if (viewModel.currentUser == null) {
                            viewModel.clearBootRoute()
                            navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                            return@NotificationGateScreen
                        }
                        viewModel.markBootRoute(Routes.MAIN)
                        selectedTab = AppTab.HOME
                        navController.navigate(Routes.MAIN) { popUpTo(0) { inclusive = true } }
                    },
                )
            }
            composable(Routes.MAIN) {
                if (user == null) {
                    LaunchedEffect(Unit) {
                        viewModel.clearBootRoute()
                        navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                    }
                    return@composable
                }
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
                key(selectedTab) {
                when (selectedTab) {
                    AppTab.HOME -> DirectorDashboardScreen(
                        onTaskClick = { navController.navigate(Routes.taskDetail(it)) },
                        onCreateTask = { selectedTab = AppTab.CREATE },
                        onEditTask = { navController.navigate(Routes.editTask(it)) },
                        onSectionClick = { section ->
                            navController.navigate(Routes.dashSection(section.route))
                        },
                    )
                    AppTab.EMPLOYEES -> EmployeesTabScreen(
                        onEmployeeClick = { navController.navigate(Routes.employeeDetail(it)) },
                        onAssignTask = { ids ->
                            navController.navigate(Routes.createTask(ids))
                        },
                        onDepartmentClick = { dept, query ->
                            navController.navigate(Routes.employeesDepartment(dept, query))
                        },
                    )
                    AppTab.TASKS -> TasksScreen(
                        onTaskClick = { navController.navigate(Routes.taskDetail(it)) },
                        onEditTask = { navController.navigate(Routes.editTask(it)) },
                    )
                    AppTab.DEPT_TASKS -> DepartmentTasksScreen(
                        onTaskClick = { navController.navigate(Routes.taskDetail(it)) },
                    )
                    AppTab.CHAT -> {
                        val ctx = LocalContext.current
                        val soonText = localized("chat_feature_soon")
                        ChatListScreen(
                            currentUserId = user?.id.orEmpty(),
                            currentUserName = user?.fullName.orEmpty(),
                            currentUserAvatar = user?.avatarUrl,
                            onOpenChat = { peerId, name -> navController.navigate(Routes.chatConversation(peerId, name)) },
                            onNewChat = { navController.navigate(Routes.CHAT_NEW) },
                            onOpenContacts = { navController.navigate(Routes.CHAT_CONTACTS) },
                            onOpenProfile = { selectedTab = AppTab.PROFILE },
                            onNewGroup = { navController.navigate(Routes.createRoom("group")) },
                            onNewChannel = { navController.navigate(Routes.createRoom("channel")) },
                            onOpenRoom = { roomId -> navController.navigate(Routes.roomConversation(roomId)) },
                            onComingSoon = {
                                android.widget.Toast.makeText(ctx, soonText, android.widget.Toast.LENGTH_SHORT).show()
                            },
                        )
                    }
                    AppTab.CREATE -> CreateTaskScreen(
                        showBack = false,
                        onBack = { selectedTab = AppTab.HOME },
                        onCreated = { selectedTab = AppTab.TASKS },
                        preselectedAssigneeIds = preselectedAssigneeIds,
                        onPreselectConsumed = { preselectedAssigneeIds = null },
                    )
                    AppTab.PROFILE -> ProfileScreen(
                        onLogout = {
                            viewModel.clearBootRoute()
                            navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                        },
                    )
                }
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
                        navController.navigate(Routes.createTask(ids))
                    },
                    onEmployeeClick = { employeeId ->
                        navController.navigate(Routes.employeeDetail(employeeId))
                    },
                    onDepartmentClick = { dept, query ->
                        navController.navigate(Routes.employeesDepartment(dept, query))
                    },
                )
            }
            composable(
                Routes.EMPLOYEES_DEPARTMENT,
                arguments = listOf(
                    navArgument("department") { type = NavType.StringType },
                    navArgument("q") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { entry ->
                DepartmentEmployeesScreen(
                    onBack = { navController.popBackStack() },
                    onEmployeeClick = { navController.navigate(Routes.employeeDetail(it)) },
                    onAssignTask = { ids -> navController.navigate(Routes.createTask(ids)) },
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
                        navController.navigate(Routes.createTask(setOf(employeeId)))
                    },
                )
            }
            composable(
                Routes.CREATE_TASK,
                arguments = listOf(
                    navArgument("assigneeIds") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) { entry ->
                val raw = entry.arguments?.getString("assigneeIds").orEmpty()
                val assigneeIds = raw.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toSet()
                    .takeIf { it.isNotEmpty() }
                CreateTaskScreen(
                    onBack = { navController.popBackStack() },
                    onCreated = { navController.popBackStack() },
                    preselectedAssigneeIds = assigneeIds,
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
                    canAssignTasks = canAssignTasks,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                Routes.CREATE_ANNOUNCEMENT,
                arguments = listOf(
                    navArgument("recipientIds") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) {
                CreateAnnouncementScreen(
                    onBack = { navController.popBackStack() },
                    onCreated = { id ->
                        navController.navigate(Routes.announcementTracking(id)) {
                            popUpTo(Routes.MAIN) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(
                Routes.EDIT_ANNOUNCEMENT,
                arguments = listOf(navArgument("announcementId") { type = NavType.StringType }),
            ) {
                CreateAnnouncementScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }
            composable(Routes.ANNOUNCEMENT_RECIPIENTS) {
                AnnouncementRecipientsHubScreen(
                    onBack = { navController.popBackStack() },
                    onDepartmentClick = { dept, query ->
                        navController.navigate(Routes.announcementDepartment(dept, query))
                    },
                    onSelectRecipients = { ids ->
                        navController.navigate(Routes.createAnnouncement(ids))
                    },
                )
            }
            composable(
                Routes.ANNOUNCEMENT_DEPT,
                arguments = listOf(
                    navArgument("department") { type = NavType.StringType },
                    navArgument("q") { type = NavType.StringType; defaultValue = "" },
                ),
            ) {
                DepartmentEmployeesScreen(
                    onBack = { navController.popBackStack() },
                    onEmployeeClick = { navController.navigate(Routes.employeeDetail(it)) },
                    onAssignTask = { ids -> navController.navigate(Routes.createAnnouncement(ids)) },
                    selectionButtonKey = "announcement_continue",
                )
            }
            composable(
                Routes.ANNOUNCEMENT_TRACKING,
                arguments = listOf(navArgument("announcementId") { type = NavType.StringType }),
            ) { entry ->
                AnnouncementTrackingScreen(
                    announcementId = entry.arguments?.getString("announcementId").orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                Routes.ANNOUNCEMENT_DETAIL,
                arguments = listOf(navArgument("announcementId") { type = NavType.StringType }),
            ) { entry ->
                AnnouncementDetailScreen(
                    announcementId = entry.arguments?.getString("announcementId").orEmpty(),
                    onBack = { navController.popBackStack() },
                    onOpenTracking = { navController.navigate(Routes.announcementTracking(it)) },
                    onEdit = { navController.navigate(Routes.editAnnouncement(it)) },
                    onDeleted = { navController.popBackStack() },
                )
            }
            composable(Routes.ANNOUNCEMENTS_SENT) {
                SentAnnouncementsScreen(
                    onBack = { navController.popBackStack() },
                    onAnnouncementClick = { navController.navigate(Routes.announcementDetail(it)) },
                    onTrackingClick = { navController.navigate(Routes.announcementTracking(it)) },
                    onEditClick = { navController.navigate(Routes.editAnnouncement(it)) },
                )
            }
            composable(Routes.ANNOUNCEMENTS_RECEIVED) {
                ReceivedAnnouncementsScreen(
                    onBack = { navController.popBackStack() },
                    onAnnouncementClick = { navController.navigate(Routes.announcementDetail(it)) },
                )
            }
            composable(
                Routes.CHAT_CONVERSATION,
                arguments = listOf(
                    navArgument("peerId") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { entry ->
                ChatConversationScreen(
                    peerId = entry.arguments?.getString("peerId").orEmpty(),
                    peerName = entry.arguments?.getString("name").orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.CHAT_NEW) {
                NewChatScreen(
                    currentUserId = user?.id.orEmpty(),
                    onBack = { navController.popBackStack() },
                    onPick = { peerId, name ->
                        navController.popBackStack()
                        navController.navigate(Routes.chatConversation(peerId, name))
                    },
                )
            }
            composable(Routes.CHAT_CONTACTS) {
                NewChatScreen(
                    currentUserId = user?.id.orEmpty(),
                    titleKey = "chat_contacts",
                    onBack = { navController.popBackStack() },
                    onPick = { peerId, name ->
                        navController.popBackStack()
                        navController.navigate(Routes.chatConversation(peerId, name))
                    },
                )
            }
            composable(
                Routes.CHAT_CREATE_ROOM,
                arguments = listOf(navArgument("type") { type = NavType.StringType }),
            ) { entry ->
                CreateRoomScreen(
                    currentUserId = user?.id.orEmpty(),
                    roomTypeKey = entry.arguments?.getString("type").orEmpty(),
                    onBack = { navController.popBackStack() },
                    onCreated = { roomId ->
                        navController.popBackStack()
                        navController.navigate(Routes.roomConversation(roomId))
                    },
                )
            }
            composable(
                Routes.ROOM_CONVERSATION,
                arguments = listOf(navArgument("roomId") { type = NavType.StringType }),
            ) { entry ->
                RoomConversationScreen(
                    roomId = entry.arguments?.getString("roomId").orEmpty(),
                    currentUserId = user?.id.orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
        }
        }

        if (!splashDone) {
            SplashScreen(modifier = Modifier.zIndex(200f))
        }

        if (showBottomNav && showNav) {
            VazifaBottomNav(
                selected = selectedTab,
                chatUnreadCount = chatUnreadCount,
                onSelect = { tab ->
                    selectedTab = tab
                    if (tab == AppTab.CHAT) viewModel.clearChatUnread()
                    if (tab == AppTab.HOME) viewModel.refreshUser()
                },
                onCreateClick = { showCreateSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .zIndex(100f),
            )
        }

        CreateActionSheet(
            visible = showCreateSheet,
            canAssignTasks = canAssignTasks,
            onDismiss = { showCreateSheet = false },
            onAction = { action ->
                when (action) {
                    CreateAction.NEW_TASK -> navController.navigate(Routes.createTask())
                    CreateAction.NEW_ANNOUNCEMENT -> navController.navigate(Routes.ANNOUNCEMENT_RECIPIENTS)
                    CreateAction.SENT_ANNOUNCEMENTS -> navController.navigate(Routes.ANNOUNCEMENTS_SENT)
                    CreateAction.RECEIVED_ANNOUNCEMENTS -> navController.navigate(Routes.ANNOUNCEMENTS_RECEIVED)
                    CreateAction.NEW_CHAT -> navController.navigate(Routes.CHAT_NEW)
                    CreateAction.SUPPORT -> showSupportSheet = true
                }
            },
        )

        SupportContactSheet(
            visible = showSupportSheet,
            onDismiss = { showSupportSheet = false },
        )
    }
    }
}
