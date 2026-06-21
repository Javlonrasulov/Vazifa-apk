package uz.vazifa.app.presentation.notifications

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import uz.vazifa.app.data.repository.AuthRepository
import uz.vazifa.app.presentation.theme.LiquidBackground
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.presentation.theme.VazifaColors

@Composable
fun NotificationGateScreen(
    authRepository: AuthRepository,
    onGranted: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }

    fun checkAndProceed() {
        val enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (!enabled) return
        checking = true
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            scope.launch {
                if (task.isSuccessful) {
                    runCatching { authRepository.updateFcm(task.result, true) }
                    onGranted()
                }
                checking = false
            }
        }
    }

    LiquidBackground(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .align(Alignment.Center)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Default.NotificationsOff,
                null,
                tint = VazifaColors.Danger,
                modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "Ilovadan foydalanish uchun bildirishnomalarni yoqing",
                color = LiquidTheme.text,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Vazifalar va muddat eslatmalari push orqali yuboriladi. Ruxsatsiz ilova ishlamaydi.",
                color = LiquidTheme.textMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    } else {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        })
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = VazifaColors.Primary),
            ) {
                Text("Sozlamalarni ochish")
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { checkAndProceed() },
                enabled = !checking,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(50),
            ) {
                if (checking) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                else Text("Tekshirish")
            }
        }
    }

    DisposableEffect(Unit) {
        checkAndProceed()
        onDispose { }
    }
}
