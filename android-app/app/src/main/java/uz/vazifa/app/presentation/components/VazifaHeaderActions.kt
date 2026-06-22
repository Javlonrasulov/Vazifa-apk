package uz.vazifa.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.vazifa.app.data.repository.AppSettingsRepository
import uz.vazifa.app.data.repository.ThemeMode
import uz.vazifa.app.localization.AppLanguage
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidGlassDropdownItem
import uz.vazifa.app.presentation.theme.LiquidGlassDropdownMenu
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.presentation.theme.liquidGlassThemed
import javax.inject.Inject

@HiltViewModel
class HeaderSettingsViewModel @Inject constructor(
    private val settings: AppSettingsRepository,
) : ViewModel() {
    val themeMode = settings.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.LIGHT)
    val language = settings.language.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppLanguage.DEFAULT)

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settings.setThemeMode(mode) }
    fun setLanguage(language: AppLanguage) = viewModelScope.launch { settings.setLanguage(language) }
}

@Composable
fun VazifaHeaderActions(
    viewModel: HeaderSettingsViewModel = hiltViewModel(),
    extra: @Composable RowScope.() -> Unit = {},
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val language by viewModel.language.collectAsState()
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
