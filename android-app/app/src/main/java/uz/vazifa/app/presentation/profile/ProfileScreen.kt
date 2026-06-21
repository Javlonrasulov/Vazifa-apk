package uz.vazifa.app.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import uz.vazifa.app.presentation.theme.LiquidBackground
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.presentation.theme.VazifaColors
import uz.vazifa.app.presentation.theme.liquidGlassThemed

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    LiquidBackground(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Profil", color = LiquidTheme.text, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            state.user?.let { u ->
                Column(Modifier.liquidGlassThemed().padding(16.dp)) {
                    Text(u.fullName, color = LiquidTheme.text, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("@${u.login}", color = LiquidTheme.textMuted)
                    Text(u.role.replaceFirstChar { it.uppercase() }, color = VazifaColors.Primary, fontSize = 13.sp)
                    u.department?.let { Text(it, color = LiquidTheme.textMuted, fontSize = 13.sp) }
                }
            }
            OutlinedButton(onClick = { viewModel.toggleTheme() }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.DarkMode, null)
                Spacer(Modifier.width(8.dp))
                Text("Tema o'zgartirish")
            }
            Button(
                onClick = { viewModel.logout(onLogout) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = VazifaColors.Danger),
            ) {
                Icon(Icons.Default.Logout, null)
                Spacer(Modifier.width(8.dp))
                Text("Chiqish")
            }
        }
    }
}
