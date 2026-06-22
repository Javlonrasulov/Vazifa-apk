package uz.vazifa.app.presentation.components

import androidx.compose.runtime.Composable
import uz.vazifa.app.localization.AppStrings
import uz.vazifa.app.localization.LocalAppLanguage

@Composable
fun localized(key: String): String = AppStrings.t(LocalAppLanguage.current, key)

@Composable
fun localizedStatus(status: String): String = localized(statusLabelKey(status))

fun statusLabelKey(status: String): String {
    val key = status.trim().lowercase().replace('-', '_')
    return when (key) {
        "new", "йанги", "янги", "yangi" -> "status_new"
        "accepted" -> "status_accepted"
        "in_progress" -> "status_in_progress"
        "in_review" -> "status_in_review"
        "completed" -> "status_completed"
        "rework" -> "status_rework"
        "cancelled" -> "status_cancelled"
        else -> "status_new"
    }
}

fun roleLabelKey(role: String): String = when (role) {
    "director" -> "role_director"
    "employee" -> "role_employee"
    else -> role
}
