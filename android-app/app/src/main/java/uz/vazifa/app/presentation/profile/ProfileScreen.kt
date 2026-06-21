package uz.vazifa.app.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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
    LaunchedEffect(Unit) { viewModel.load() }

    VazifaTabScaffold(
        title = localized("nav_profile"),
        actions = { VazifaHeaderActions() },
    ) { padding ->
        LiquidBackground(Modifier.fillMaxSize()) {
            VazifaScreenBox(padding) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    state.user?.let { u ->
                        GlassCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text(u.fullName, color = LiquidTheme.text, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                Text("@${u.login}", color = LiquidTheme.textMuted)
                                Text(localized(roleLabelKey(u.role)), color = LiquidGlass.BlueLight, fontSize = 13.sp)
                                u.department?.let { Text(it, color = LiquidTheme.textMuted, fontSize = 13.sp) }
                            }
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
        }
    }
}
