package uz.vazifa.app.presentation.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import uz.vazifa.app.domain.model.InboxNotification

val LocalInboxNavigator = staticCompositionLocalOf<(InboxNotification) -> Unit> { {} }
