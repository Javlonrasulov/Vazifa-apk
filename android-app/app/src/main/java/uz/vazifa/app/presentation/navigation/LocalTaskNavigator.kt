package uz.vazifa.app.presentation.navigation

import androidx.compose.runtime.staticCompositionLocalOf

val LocalTaskNavigator = staticCompositionLocalOf<(String) -> Unit> { {} }
