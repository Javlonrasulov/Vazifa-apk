package uz.vazifa.app.presentation.auth

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import uz.vazifa.app.data.repository.AppSettingsRepository
import uz.vazifa.app.localization.AppLanguage
import uz.vazifa.app.presentation.components.liquidGlassFieldColors
import uz.vazifa.app.presentation.components.localized
import uz.vazifa.app.presentation.theme.*
import uz.vazifa.app.util.UzbekPhoneVisualTransformation
import javax.inject.Inject

@HiltViewModel
class LoginSettingsViewModel @Inject constructor(private val settings: AppSettingsRepository) : ViewModel() {
    fun setLanguage(lang: AppLanguage) = viewModelScope.launch { settings.setLanguage(lang) }
}

@Composable
fun LoginScreen(
    onSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
    settingsViewModel: LoginSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val deviceId = remember { Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) }
    var showLangMenu by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    val fieldColors = liquidGlassFieldColors()
    val phoneTransformation = remember { UzbekPhoneVisualTransformation() }

    LaunchedEffect(state.loggedIn) { if (state.loggedIn) onSuccess() }

    LiquidBackground(Modifier.fillMaxSize()) {
        Box(Modifier.align(Alignment.TopEnd).padding(16.dp)) {
            Box(
                Modifier.size(40.dp).liquidGlassThemed(radius = LiquidGlass.RadiusChip),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(onClick = { showLangMenu = true }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Language, null, tint = LiquidTheme.textMuted, modifier = Modifier.size(20.dp))
                }
            }
            DropdownMenu(expanded = showLangMenu, onDismissRequest = { showLangMenu = false }) {
                AppLanguage.menuOrder.forEach { lang ->
                    DropdownMenuItem(
                        text = { Text(lang.menuLabel) },
                        onClick = {
                            settingsViewModel.setLanguage(lang)
                            showLangMenu = false
                        },
                    )
                }
            }
        }

        Column(
            Modifier.align(Alignment.Center).fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(80.dp)
                    .glowEffect(color = LiquidGlass.Blue, radius = 180f)
                    .clip(RoundedCornerShape(LiquidGlass.RadiusCard))
                    .background(LiquidGlass.GradientPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Assignment, null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(localized("app_name"), color = LiquidTheme.text, fontWeight = FontWeight.Bold, fontSize = 28.sp)
            Text(localized("app_subtitle"), color = LiquidTheme.textMuted, fontSize = 14.sp)
            Spacer(Modifier.height(32.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(LiquidGlass.RadiusInput))
                    .border(1.dp, LiquidTheme.textMuted.copy(alpha = 0.35f), RoundedCornerShape(LiquidGlass.RadiusInput)),
            ) {
                listOf(LoginMode.PHONE to "login_mode_phone", LoginMode.LOGIN to "login_mode_login").forEach { (mode, labelKey) ->
                    val selected = state.mode == mode
                    Box(
                        Modifier
                            .weight(1f)
                            .clickable { viewModel.setMode(mode) }
                            .background(
                                if (selected) LiquidGlass.Blue.copy(alpha = 0.18f) else Color.Transparent,
                            )
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            localized(labelKey),
                            color = if (selected) LiquidGlass.Blue else LiquidTheme.textMuted,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = if (state.mode == LoginMode.PHONE) state.phoneDigits else state.login,
                onValueChange = {
                    if (state.mode == LoginMode.PHONE) viewModel.onPhoneDigitsChange(it)
                    else viewModel.onLoginChange(it)
                },
                label = { Text(localized(if (state.mode == LoginMode.PHONE) "phone" else "login")) },
                leadingIcon = {
                    Icon(
                        if (state.mode == LoginMode.PHONE) Icons.Default.Phone else Icons.Default.Person,
                        null,
                    )
                },
                visualTransformation = if (state.mode == LoginMode.PHONE) {
                    phoneTransformation
                } else {
                    VisualTransformation.None
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (state.mode == LoginMode.PHONE) KeyboardType.Phone else KeyboardType.Text,
                ),
                shape = RoundedCornerShape(LiquidGlass.RadiusInput),
                colors = fieldColors,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text(localized("password")) },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = localized(if (showPassword) "hide_password" else "show_password"),
                            tint = LiquidTheme.textMuted,
                        )
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(LiquidGlass.RadiusInput),
                colors = fieldColors,
            )

            state.errorKey?.let {
                Spacer(Modifier.height(8.dp))
                Text(localized(it), color = VazifaColors.Danger, fontSize = 13.sp)
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { viewModel.login(deviceId) },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(LiquidGlass.RadiusChip),
                colors = ButtonDefaults.buttonColors(containerColor = LiquidGlass.Blue),
            ) {
                if (state.loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text(localized("login_btn"), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
