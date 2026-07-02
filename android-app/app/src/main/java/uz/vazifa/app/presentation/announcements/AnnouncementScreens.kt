package uz.vazifa.app.presentation.announcements

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uz.vazifa.app.domain.model.Announcement
import uz.vazifa.app.domain.model.AnnouncementRecipient
import uz.vazifa.app.domain.model.User
import uz.vazifa.app.domain.model.acknowledgedCount
import uz.vazifa.app.domain.model.isAcknowledgedBy
import uz.vazifa.app.domain.model.isCreator
import uz.vazifa.app.domain.model.pendingCount
import uz.vazifa.app.presentation.components.*
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CreateAnnouncementScreen(
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    viewModel: CreateAnnouncementViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val fieldColors = liquidGlassFieldColors()
    val zone = ZoneId.of("Asia/Tashkent")
    val deadlineDisplayFmt = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDateMillis by remember { mutableStateOf<Long?>(null) }
    var showAllSelected by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val searchBringIntoView = remember { BringIntoViewRequester() }
    val focusScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val isKeyboardVisible = WindowInsets.ime.getBottom(density) > 0

    LaunchedEffect(Unit) { viewModel.loadContacts() }
    LaunchedEffect(state.selectedIds.size) {
        if (state.selectedIds.size <= 2) showAllSelected = false
    }
    val errorMessage = state.errorKey?.let { localized(it) }
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    LaunchedEffect(state.created, state.createdId) {
        val id = state.createdId
        if (state.created && id != null) {
            onCreated(id)
            viewModel.resetForm()
        }
    }

    val submit: () -> Unit = {
        when {
            state.title.isBlank() -> viewModel.showTitleError()
            state.selectedIds.isEmpty() -> viewModel.showRecipientError()
            else -> viewModel.create()
        }
    }

    VazifaStackScaffold(
        title = localized("announcement_create"),
        onBack = onBack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    state.title, viewModel::onTitle,
                    label = { Text(localized("task_name")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focus ->
                            if (!focus.isFocused && state.title.isBlank()) viewModel.showTitleError()
                        },
                    isError = state.titleError && state.title.isBlank(),
                    supportingText = if (state.titleError && state.title.isBlank()) {
                        { Text(localized("task_title_empty")) }
                    } else null,
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
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                Text(localized("announcement_reminder_interval"), color = LiquidTheme.textMuted, fontSize = 13.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        state.reminderValue, viewModel::onReminderValue,
                        label = { Text(localized("announcement_interval_value")) },
                        modifier = Modifier.weight(0.4f),
                        shape = RoundedCornerShape(LiquidGlass.RadiusInput),
                        colors = fieldColors,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                    var unitExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = unitExpanded,
                        onExpandedChange = { unitExpanded = it },
                        modifier = Modifier.weight(0.6f),
                    ) {
                        OutlinedTextField(
                            value = localized(
                                if (state.reminderUnit == ReminderUnit.MINUTES) {
                                    "announcement_unit_minutes"
                                } else {
                                    "announcement_unit_hours"
                                },
                            ),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(localized("announcement_interval_unit")) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(LiquidGlass.RadiusInput),
                            colors = fieldColors,
                        )
                        ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(localized("announcement_unit_minutes")) },
                                onClick = {
                                    viewModel.onReminderUnit(ReminderUnit.MINUTES)
                                    unitExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(localized("announcement_unit_hours")) },
                                onClick = {
                                    viewModel.onReminderUnit(ReminderUnit.HOURS)
                                    unitExpanded = false
                                },
                            )
                        }
                    }
                }
                Text(localized("announcement_recipients"), color = LiquidTheme.textMuted, fontSize = 13.sp)
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
                    state.recipientSearch, viewModel::onRecipientSearch,
                    label = { Text(localized("task_search_employee")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(searchBringIntoView)
                        .onFocusEvent { event ->
                            if (event.isFocused) {
                                focusScope.launch {
                                    delay(150)
                                    searchBringIntoView.bringIntoView()
                                }
                            }
                        },
                    shape = RoundedCornerShape(LiquidGlass.RadiusInput),
                    colors = fieldColors,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = LiquidTheme.textMuted) },
                    singleLine = true,
                )
                val selectedContacts = state.selectedContacts
                val visibleSelected = if (showAllSelected || selectedContacts.size <= 2) {
                    selectedContacts
                } else {
                    selectedContacts.take(2)
                }
                val hiddenCount = (selectedContacts.size - 2).coerceAtLeast(0)
                visibleSelected.forEach { c ->
                    RecipientSelectRow(c, true) { viewModel.toggleRecipient(c.id) }
                }
                if (hiddenCount > 0) {
                    TextButton(onClick = { showAllSelected = !showAllSelected }) {
                        Text(
                            if (showAllSelected) {
                                localized("task_show_less_assignees")
                            } else {
                                "${localized("task_show_more_assignees")} (+$hiddenCount)"
                            },
                            color = LiquidGlass.BlueLight,
                            fontSize = 13.sp,
                        )
                    }
                }
                if (state.recipientSearch.isNotBlank()) {
                    state.filteredContacts
                        .filter { it.id !in state.selectedIds }
                        .forEach { c ->
                            RecipientSelectRow(c, false) { viewModel.toggleRecipient(c.id) }
                        }
                }
                Spacer(Modifier.height(if (isKeyboardVisible) 24.dp else 8.dp))
            }
            if (!isKeyboardVisible) {
                Button(
                    onClick = submit,
                    enabled = !state.loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(LiquidGlass.RadiusInput),
                ) {
                    if (state.loading) {
                        CircularProgressIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(localized("announcement_send"))
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        VazifaDatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            onConfirm = { millis ->
                pendingDateMillis = millis
                showDatePicker = false
                showTimePicker = true
            },
            initialDateMillis = state.deadlineDateTime?.atZone(zone)?.toInstant()?.toEpochMilli(),
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
fun AnnouncementDetailScreen(
    announcementId: String,
    onBack: () -> Unit,
    onOpenTracking: (String) -> Unit,
    viewModel: AnnouncementDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(announcementId) { viewModel.load(announcementId) }
    val errorMessage = state.errorKey?.let { localized(it) }
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    VazifaStackScaffold(
        title = localized("announcement_detail"),
        onBack = onBack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val announcement = state.announcement
        if (state.loading && announcement == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = LiquidGlass.Blue)
            }
            return@VazifaStackScaffold
        }
        if (announcement == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(localized("announcement_not_found"), color = LiquidTheme.textMuted)
            }
            return@VazifaStackScaffold
        }

        val isCreator = announcement.isCreator(state.currentUserId)
        val acknowledged = announcement.isAcknowledgedBy(state.currentUserId)

        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(announcement.title, color = LiquidTheme.text, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    announcement.description?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = LiquidTheme.textMuted, fontSize = 14.sp)
                    }
                    announcement.createdBy?.let { creator ->
                        Text(
                            "${localized("announcement_from")}: ${creator.fullName}",
                            color = LiquidTheme.textMuted,
                            fontSize = 13.sp,
                        )
                    }
                    TaskCountdownText(deadlineAt = announcement.deadlineAt, status = null)
                }
            }
            if (isCreator) {
                Button(
                    onClick = { onOpenTracking(announcement.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${localized("announcement_tracking")} " +
                            "(${announcement.acknowledgedCount()}/${announcement.recipients.size})",
                    )
                }
            } else if (!acknowledged) {
                Button(
                    onClick = { viewModel.acknowledge(announcement.id) },
                    enabled = !state.acknowledging,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.acknowledging) {
                        CircularProgressIndicator(Modifier.size(20.dp))
                    } else {
                        Text(localized("announcement_acknowledge"))
                    }
                }
            } else {
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LiquidGlass.Blue)
                        Text(localized("announcement_acknowledged"), color = LiquidTheme.text)
                    }
                }
            }
        }
    }
}

@Composable
fun AnnouncementTrackingScreen(
    announcementId: String,
    onBack: () -> Unit,
    viewModel: AnnouncementTrackingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(announcementId) { viewModel.load(announcementId) }

    VazifaStackScaffold(
        title = localized("announcement_tracking"),
        onBack = onBack,
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = LiquidGlass.Blue)
            }
            return@VazifaStackScaffold
        }
        if (state.forbidden || state.announcement == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(localized("announcement_not_found"), color = LiquidTheme.textMuted)
            }
            return@VazifaStackScaffold
        }

        LazyColumn(
            Modifier.padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(state.announcement!!.title, color = LiquidTheme.text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "${localized("announcement_acknowledged_count")}: ${state.acknowledged.size}",
                            color = LiquidGlass.Blue,
                            fontSize = 13.sp,
                        )
                        Text(
                            "${localized("announcement_pending_count")}: ${state.pending.size}",
                            color = LiquidTheme.textMuted,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
            item {
                Text(localized("announcement_acknowledged_list"), color = LiquidTheme.text, fontWeight = FontWeight.SemiBold)
            }
            if (state.acknowledged.isEmpty()) {
                item { Text(localized("announcement_none_acknowledged"), color = LiquidTheme.textMuted, fontSize = 13.sp) }
            } else {
                items(state.acknowledged, key = { it.id }) { r ->
                    RecipientStatusRow(r, acknowledged = true)
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                Text(localized("announcement_pending_list"), color = LiquidTheme.text, fontWeight = FontWeight.SemiBold)
            }
            if (state.pending.isEmpty()) {
                item { Text(localized("announcement_all_acknowledged"), color = LiquidGlass.Blue, fontSize = 13.sp) }
            } else {
                items(state.pending, key = { it.id }) { r ->
                    RecipientStatusRow(r, acknowledged = false)
                }
            }
        }
    }
}

@Composable
fun SentAnnouncementsScreen(
    onBack: () -> Unit,
    onAnnouncementClick: (String) -> Unit,
    onTrackingClick: (String) -> Unit,
    viewModel: SentAnnouncementsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    VazifaStackScaffold(
        title = localized("announcement_sent_list"),
        onBack = onBack,
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            if (state.announcements.isEmpty() && !state.loading) {
                item {
                    Text(localized("dash_empty"), color = LiquidTheme.textMuted, modifier = Modifier.padding(24.dp))
                }
            }
            items(state.announcements, key = { it.id }) { a ->
                GlassCard(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onAnnouncementClick(a.id) },
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(a.title, color = LiquidTheme.text, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${localized("announcement_acknowledged_count")}: ${a.acknowledgedCount()} / ${a.recipients.size}",
                            color = LiquidTheme.textMuted,
                            fontSize = 12.sp,
                        )
                        TextButton(onClick = { onTrackingClick(a.id) }) {
                            Text(localized("announcement_tracking"), color = LiquidGlass.Blue)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipientSelectRow(contact: User, checked: Boolean, onToggle: () -> Unit) {
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
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(contact.fullName, color = LiquidTheme.text)
                contact.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                    Text(phone, color = LiquidTheme.textMuted, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun RecipientStatusRow(recipient: AnnouncementRecipient, acknowledged: Boolean) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                if (acknowledged) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (acknowledged) LiquidGlass.Blue else LiquidTheme.textMuted,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    recipient.recipient?.fullName ?: recipient.recipientId,
                    color = LiquidTheme.text,
                    fontWeight = FontWeight.Medium,
                )
                recipient.recipient?.department?.let { dept ->
                    Text(dept, color = LiquidTheme.textMuted, fontSize = 12.sp)
                }
            }
        }
    }
}
