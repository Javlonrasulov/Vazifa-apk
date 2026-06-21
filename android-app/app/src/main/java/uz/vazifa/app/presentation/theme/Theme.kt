package uz.vazifa.app.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val LocalVazifaDark = compositionLocalOf { true }

object LiquidTheme {
    val isDark: Boolean @Composable get() = LocalVazifaDark.current
    val bg @Composable get() = if (isDark) VazifaColors.BgDark else VazifaColors.BgLight
    val bgMid @Composable get() = if (isDark) VazifaColors.BgMidDark else VazifaColors.BgMidLight
    val text @Composable get() = if (isDark) VazifaColors.TextWhite else VazifaColors.TextDark
    val textMuted @Composable get() = if (isDark) VazifaColors.TextMutedDark else VazifaColors.TextMutedLight
    val primary @Composable get() = VazifaColors.Primary
}

@Composable
fun Modifier.liquidGlassThemed(radius: Dp = 24.dp): Modifier {
    val isDark = LocalVazifaDark.current
    val shape = RoundedCornerShape(radius)
    return this
        .clip(shape)
        .background(Color.White.copy(alpha = if (isDark) 0.18f else 0.72f))
        .border(
            1.dp,
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.5f),
                    VazifaColors.Primary.copy(alpha = 0.2f),
                ),
            ),
            shape,
        )
}

@Composable
fun LiquidBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val isDark = LocalVazifaDark.current
    Box(
        modifier = modifier
            .background(if (isDark) VazifaColors.BgDark else VazifaColors.BgLight)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(VazifaColors.Primary.copy(alpha = if (isDark) 0.22f else 0.08f), Color.Transparent),
                        center = Offset(size.width * 0.2f, size.height * 0.15f),
                        radius = size.width * 0.6f,
                    ),
                    radius = size.width * 0.6f,
                    center = Offset(size.width * 0.2f, size.height * 0.15f),
                )
            },
        content = content,
    )
}

private val DarkScheme = darkColorScheme(
    primary = VazifaColors.Primary,
    background = VazifaColors.BgDark,
    surface = VazifaColors.BgMidDark,
    onBackground = VazifaColors.TextWhite,
    onSurface = VazifaColors.TextWhite,
    error = VazifaColors.Danger,
)

private val LightScheme = lightColorScheme(
    primary = VazifaColors.Primary,
    background = VazifaColors.BgLight,
    surface = Color.White,
    onBackground = VazifaColors.TextDark,
    onSurface = VazifaColors.TextDark,
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
