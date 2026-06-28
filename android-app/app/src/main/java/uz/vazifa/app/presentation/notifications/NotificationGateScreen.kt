package uz.vazifa.app.presentation.notifications

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch
import uz.vazifa.app.data.repository.AuthRepository
import uz.vazifa.app.presentation.components.localized
import uz.vazifa.app.presentation.theme.LiquidBackground
import uz.vazifa.app.presentation.theme.LiquidGlass
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
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val msgStillOff = localized("notif_still_off")
    val msgLoading = localized("com_loading")
    val msgFcmError = localized("notif_fcm_error")

    fun checkAndProceed() {
        if (checking) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            statusMessage = msgStillOff
            return
        }
        checking = true
        scope.launch {
            if (!authRepository.hasStoredSessionAsync()) {
                checking = false
                return@launch
            }
            val ok = authRepository.registerPushToken()
            checking = false
            if (ok) {
                onGranted()
            } else {
                statusMessage = msgFcmError
            }
        }
    }

    LaunchedEffect(Unit) { checkAndProceed() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkAndProceed()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LiquidBackground(Modifier.fillMaxSize()) {
        Column(
            Modifier.align(Alignment.Center).padding(32.dp),
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
                localized("notif_title"),
                color = LiquidTheme.text,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(localized("notif_desc"), color = LiquidTheme.textMuted, fontSize = 14.sp, textAlign = TextAlign.Center)
            statusMessage?.let { msg ->
                Spacer(Modifier.height(12.dp))
                Text(msg, color = VazifaColors.Danger, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            },
                        )
                    } else {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            },
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(LiquidGlass.RadiusChip),
                colors = ButtonDefaults.buttonColors(containerColor = LiquidGlass.Blue),
            ) {
                Text(localized("notif_open_settings"), fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { checkAndProceed() },
                enabled = !checking,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(LiquidGlass.RadiusChip),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LiquidTheme.text),
                border = BorderStroke(1.dp, LiquidGlass.GlassDarkBorderStrong),
            ) {
                if (checking) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                else Text(localized("notif_check"))
            }
        }
    }
}
