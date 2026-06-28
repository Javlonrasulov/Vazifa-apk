package uz.vazifa.app.presentation.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.vazifa.app.data.repository.AppSettingsRepository
import uz.vazifa.app.data.repository.AuthRepository
import uz.vazifa.app.data.repository.NotificationInboxRepository
import uz.vazifa.app.data.repository.ThemeMode
import uz.vazifa.app.domain.model.InboxNotification
import uz.vazifa.app.localization.AppLanguage
import uz.vazifa.app.presentation.navigation.LocalInboxNavigator
import uz.vazifa.app.presentation.notifications.NotificationInboxSheet
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidGlassDropdownItem
import uz.vazifa.app.presentation.theme.LiquidGlassDropdownMenu
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.presentation.theme.liquidGlassThemed
import javax.inject.Inject

@HiltViewModel
class HeaderSettingsViewModel @Inject constructor(
    private val settings: AppSettingsRepository,
    private val auth: AuthRepository,
    private val inbox: NotificationInboxRepository,
) : ViewModel() {
    val themeMode = settings.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.LIGHT)
    val language = settings.language.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppLanguage.DEFAULT)
    val unreadCount = inbox.unreadCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    var showInbox by mutableStateOf(false)
        private set
    var inboxItems by mutableStateOf<List<InboxNotification>>(emptyList())
        private set

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settings.setThemeMode(mode) }
    fun setLanguage(language: AppLanguage) = viewModelScope.launch {
        settings.setLanguage(language)
        auth.syncLanguageAsync()
    }

    fun openInbox() {
        viewModelScope.launch {
            inboxItems = inbox.takeAll()
            showInbox = true
        }
    }

    fun dismissInbox() {
        showInbox = false
        inboxItems = emptyList()
    }
}

@Composable
fun VazifaHeaderActions(
    showNotifications: Boolean = true,
    extra: @Composable RowScope.() -> Unit = {},
) {
    val activity = LocalContext.current as ComponentActivity
    val viewModel: HeaderSettingsViewModel = hiltViewModel(activity)
    val themeMode by viewModel.themeMode.collectAsState()
    val language by viewModel.language.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val openInboxItem = LocalInboxNavigator.current
    var showThemeMenu by remember { mutableStateOf(false) }
    var showLangMenu by remember { mutableStateOf(false) }
    val themeIcon = when (themeMode) {
        ThemeMode.LIGHT -> Icons.Default.WbSunny
        ThemeMode.SYSTEM -> Icons.Default.SettingsBrightness
        ThemeMode.DARK -> Icons.Default.NightlightRound
    }
    val themeOptions = listOf(
        ThemeMode.DARK to "com_theme_dark",
        ThemeMode.LIGHT to "com_theme_light",
        ThemeMode.SYSTEM to "com_theme_system",
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        extra()
        if (showNotifications) {
            NotificationHeaderIconButton(
                count = unreadCount,
                onClick = { viewModel.openInbox() },
            )
        }
        Box {
            GlassHeaderIconButton(
                onClick = { showThemeMenu = true },
                icon = themeIcon,
                tint = LiquidGlass.Blue,
            )
            LiquidGlassDropdownMenu(
                expanded = showThemeMenu,
                onDismissRequest = { showThemeMenu = false },
            ) {
                themeOptions.forEach { (mode, labelKey) ->
                    LiquidGlassDropdownItem(
                        text = localized(labelKey),
                        selected = themeMode == mode,
                        onClick = {
                            viewModel.setThemeMode(mode)
                            showThemeMenu = false
                        },
                    )
                }
            }
        }
        Box {
            GlassHeaderIconButton(
                onClick = { showLangMenu = true },
                icon = Icons.Default.Language,
                tint = LiquidGlass.Blue,
            )
            LiquidGlassDropdownMenu(
                expanded = showLangMenu,
                onDismissRequest = { showLangMenu = false },
            ) {
                AppLanguage.menuOrder.forEach { option ->
                    LiquidGlassDropdownItem(
                        text = option.menuLabel,
                        selected = language == option,
                        onClick = {
                            viewModel.setLanguage(option)
                            showLangMenu = false
                        },
                    )
                }
            }
        }
    }

    NotificationInboxSheet(
        visible = viewModel.showInbox,
        items = viewModel.inboxItems,
        onDismiss = { viewModel.dismissInbox() },
        onItemClick = { item ->
            viewModel.dismissInbox()
            openInboxItem(item)
        },
    )
}

@Composable
fun NotificationHeaderIconButton(
    count: Int,
    onClick: () -> Unit,
) {
    Box {
        GlassHeaderIconButton(
            onClick = onClick,
            icon = Icons.Default.Notifications,
            tint = LiquidGlass.Blue,
            contentDescription = localized("notif_inbox_title"),
        )
        if (count > 0) {
            val badgeText = if (count > 99) "99+" else count.toString()
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(if (badgeText.length > 1) 18.dp else 16.dp)
                    .clip(CircleShape)
                    .background(LiquidGlass.Rose),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    badgeText,
                    color = Color.White,
                    fontSize = if (badgeText.length > 1) 8.sp else 9.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = if (badgeText.length > 1) 8.sp else 9.sp,
                )
            }
        }
    }
}

@Composable
fun GlassHeaderIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    tint: Color,
    contentDescription: String? = null,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .liquidGlassThemed(radius = LiquidGlass.RadiusChip),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
            Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(20.dp))
        }
    }
}
