package uz.vazifa.app.presentation.components

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidTheme

@Composable
fun liquidGlassFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = if (LiquidTheme.isDark) Color.White.copy(0.12f) else Color.White.copy(0.85f),
    unfocusedContainerColor = if (LiquidTheme.isDark) Color.White.copy(0.08f) else Color.White.copy(0.65f),
    focusedTextColor = LiquidTheme.text,
    unfocusedTextColor = LiquidTheme.text,
    focusedBorderColor = LiquidGlass.GlassDarkBorderStrong,
    unfocusedBorderColor = LiquidTheme.textMuted.copy(alpha = 0.35f),
    focusedLabelColor = LiquidGlass.BlueLight,
    unfocusedLabelColor = LiquidTheme.textMuted,
    cursorColor = LiquidGlass.Cyan,
    focusedLeadingIconColor = LiquidGlass.BlueLight,
    unfocusedLeadingIconColor = LiquidTheme.textMuted,
    focusedTrailingIconColor = LiquidGlass.BlueLight,
    unfocusedTrailingIconColor = LiquidTheme.textMuted,
)
