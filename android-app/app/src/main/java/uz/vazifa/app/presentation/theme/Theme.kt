package uz.vazifa.app.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val DarkScheme = darkColorScheme(
    primary = LiquidGlass.Blue,
    secondary = LiquidGlass.Cyan,
    tertiary = LiquidGlass.Violet,
    background = LiquidGlass.BgDark,
    surface = LiquidGlass.BgMidDark,
    onPrimary = Color.White,
    onBackground = LiquidGlass.TextWhite,
    onSurface = LiquidGlass.TextWhite,
    onSurfaceVariant = LiquidGlass.TextWhiteMuted,
    error = VazifaColors.Danger,
)

private val LightScheme = lightColorScheme(
    primary = LiquidGlass.Blue,
    secondary = LiquidGlass.Cyan,
    tertiary = LiquidGlass.Violet,
    background = LiquidGlass.BgLight,
    surface = Color.White,
    onBackground = LiquidGlass.TextDark,
    onSurface = LiquidGlass.TextDark,
    onSurfaceVariant = LiquidGlass.TextDarkMuted,
    error = VazifaColors.Danger,
)

@Composable
fun VazifaTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalVazifaDark provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            content = content,
        )
    }
}
