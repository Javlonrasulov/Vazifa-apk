package uz.vazifa.app.presentation.security

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext

@Composable
fun ScreenshotPolicyEffect(allowScreenshot: Boolean) {
    val context = LocalContext.current
    SideEffect {
        val window = (context as? Activity)?.window ?: return@SideEffect
        if (allowScreenshot) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
