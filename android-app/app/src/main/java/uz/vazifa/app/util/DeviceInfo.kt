package uz.vazifa.app.util

import android.os.Build
import java.util.Locale

object DeviceInfo {
    fun displayName(): String {
        val manufacturer = Build.MANUFACTURER
            .trim()
            .replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
            }
        val model = Build.MODEL.trim()
        return when {
            model.isBlank() -> manufacturer
            model.equals(manufacturer, ignoreCase = true) -> model
            else -> "$manufacturer $model"
        }
    }
}
