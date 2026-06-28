package uz.vazifa.app.presentation.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PhotoCamera
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
import uz.vazifa.app.presentation.chat.ChatAvatar
import uz.vazifa.app.util.ChatFiles
import uz.vazifa.app.presentation.components.*
import uz.vazifa.app.presentation.components.roleLabelKey
import uz.vazifa.app.presentation.theme.GlassCard
import uz.vazifa.app.presentation.theme.LiquidBackground
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.presentation.theme.VazifaColors

@Composable
fun ProfileScreen(onLogout: () -> Unit, viewModel: ProfileViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val fieldColors = liquidGlassFieldColors()
    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            ChatFiles.copyToCache(context, uri)?.let { viewModel.uploadAvatar(it) }
        }
    }
    var showCurrentPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.load(onSessionExpired = onLogout) }

    val passwordChangedMsg = localized("profile_password_changed")
    LaunchedEffect(state.changePasswordSuccess) {
        if (state.changePasswordSuccess) {
            snackbarHostState.showSnackbar(passwordChangedMsg)
            viewModel.clearChangePasswordSuccess()
        }
    }

    VazifaTabScaffold(
        title = localized("nav_profile"),
        actions = { VazifaHeaderActions() },
    ) { padding ->
        LiquidBackground(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                VazifaScreenBox(padding) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    state.user?.let { u ->
                        GlassCard(Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    ChatAvatar(u.fullName, online = false, size = 72.dp, showPresence = false, avatarUrl = u.avatarUrl)
                                    Box(
                                        Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(LiquidGlass.Blue)
                                            .clickable(enabled = !state.uploadingAvatar) { imagePicker.launch("image/*") },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (state.uploadingAvatar) {
                                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                        } else {
                                            Icon(Icons.Default.PhotoCamera, null, tint = Color.White, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }
                                Spacer(Modifier.width(16.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(u.fullName, color = LiquidTheme.text, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                    Text("@${u.login}", color = LiquidTheme.textMuted)
                                    Text(localized(roleLabelKey(u.role)), color = LiquidGlass.BlueLight, fontSize = 13.sp)
                                    u.department?.let { Text(it, color = LiquidTheme.textMuted, fontSize = 13.sp) }
                                    if (!u.avatarUrl.isNullOrBlank()) {
                                        Text(
                                            localized("profile_remove_photo"),
                                            color = LiquidGlass.Rose,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(top = 4.dp).clickable(enabled = !state.uploadingAvatar) { viewModel.deleteAvatar() },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (state.showChangePassword) {
                        GlassCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    localized("profile_change_password"),
                                    color = LiquidTheme.text,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                )

                                OutlinedTextField(
                                    value = state.currentPassword,
                                    onValueChange = viewModel::onCurrentPasswordChange,
                                    label = { Text(localized("profile_current_password")) },
                                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                                    trailingIcon = {
                                        IconButton(onClick = { showCurrentPassword = !showCurrentPassword }) {
                                            Icon(
                                                if (showCurrentPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = localized(if (showCurrentPassword) "hide_password" else "show_password"),
                                                tint = LiquidTheme.textMuted,
                                            )
                                        }
                                    },
                                    visualTransformation = if (showCurrentPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(LiquidGlass.RadiusInput),
                                    colors = fieldColors,
                                )

                                OutlinedTextField(
                                    value = state.newPassword,
                                    onValueChange = viewModel::onNewPasswordChange,
                                    label = { Text(localized("profile_new_password")) },
                                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                                    trailingIcon = {
                                        IconButton(onClick = { showNewPassword = !showNewPassword }) {
                                            Icon(
                                                if (showNewPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = localized(if (showNewPassword) "hide_password" else "show_password"),
                                                tint = LiquidTheme.textMuted,
                                            )
                                        }
                                    },
                                    visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(LiquidGlass.RadiusInput),
                                    colors = fieldColors,
                                )

                                OutlinedTextField(
                                    value = state.confirmPassword,
                                    onValueChange = viewModel::onConfirmPasswordChange,
                                    label = { Text(localized("profile_confirm_password")) },
                                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                                    trailingIcon = {
                                        IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                            Icon(
                                                if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = localized(if (showConfirmPassword) "hide_password" else "show_password"),
                                                tint = LiquidTheme.textMuted,
                                            )
                                        }
                                    },
                                    visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(LiquidGlass.RadiusInput),
                                    colors = fieldColors,
                                )

                                state.changePasswordErrorKey?.let {
                                    Text(localized(it), color = VazifaColors.Danger, fontSize = 13.sp)
                                }

                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedButton(
                                        onClick = { viewModel.toggleChangePassword() },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        shape = RoundedCornerShape(LiquidGlass.RadiusChip),
                                        enabled = !state.changingPassword,
                                    ) {
                                        Text(localized("com_cancel"))
                                    }
                                    Button(
                                        onClick = { viewModel.changePassword() },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        shape = RoundedCornerShape(LiquidGlass.RadiusChip),
                                        colors = ButtonDefaults.buttonColors(containerColor = LiquidGlass.Blue),
                                        enabled = !state.changingPassword,
                                    ) {
                                        if (state.changingPassword) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                        } else {
                                            Text(localized("com_save"))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.toggleChangePassword() },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(LiquidGlass.RadiusChip),
                        ) {
                            Icon(Icons.Default.Lock, null, tint = LiquidGlass.Blue)
                            Spacer(Modifier.width(8.dp))
                            Text(localized("profile_change_password"), color = LiquidGlass.Blue)
                        }
                    }

                    Button(
                        onClick = { viewModel.logout(onLogout) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(LiquidGlass.RadiusChip),
                        colors = ButtonDefaults.buttonColors(containerColor = VazifaColors.Danger),
                    ) {
                        Icon(Icons.Default.Logout, null)
                        Spacer(Modifier.width(8.dp))
                        Text(localized("profile_logout"))
                    }
                }
                }
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                )
            }
        }
    }
}
