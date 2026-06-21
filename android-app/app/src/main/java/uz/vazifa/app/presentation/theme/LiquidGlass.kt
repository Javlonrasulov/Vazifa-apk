package uz.vazifa.app.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
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

object LiquidGlass {
    val BgDark = Color(0xFF060B18)
    val BgMidDark = Color(0xFF0D1428)
    val BgLight = Color(0xFFF0F4FF)
    val BgMidLight = Color(0xFFE8EEFF)

    val GlassDarkBorder = Color(0x44FFFFFF)
    val GlassDarkBorderStrong = Color(0x77FFFFFF)

    val Blue = Color(0xFF2563EB)
    val BlueLight = Color(0xFF3B82F6)
    val Cyan = Color(0xFF22D3EE)
    val Violet = Color(0xFF818CF8)
    val Emerald = Color(0xFF34D399)
    val Amber = Color(0xFFFBBF24)
    val Rose = Color(0xFFFB7185)

    val TextWhite = Color(0xFFF8FAFF)
    val TextWhiteMuted = Color(0xAAE2E8FF)
    val TextDark = Color(0xFF0F172A)
    val TextDarkMuted = Color(0xFF64748B)

    val RadiusCard = 24.dp
    val RadiusChip = 50.dp
    val RadiusInput = 16.dp

    val GradientPrimary = Brush.linearGradient(
        listOf(Blue, BlueLight, Cyan),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
    )
}

object LiquidTheme {
    val isDark: Boolean
        @Composable @ReadOnlyComposable get() = LocalVazifaDark.current

    val bg: Color
        @Composable @ReadOnlyComposable get() =
            if (LocalVazifaDark.current) LiquidGlass.BgDark else LiquidGlass.BgLight

    val bgMid: Color
        @Composable @ReadOnlyComposable get() =
            if (LocalVazifaDark.current) LiquidGlass.BgMidDark else LiquidGlass.BgMidLight

    val text: Color
        @Composable @ReadOnlyComposable get() =
            if (LocalVazifaDark.current) LiquidGlass.TextWhite else LiquidGlass.TextDark

    val textMuted: Color
        @Composable @ReadOnlyComposable get() =
            if (LocalVazifaDark.current) LiquidGlass.TextWhiteMuted else LiquidGlass.TextDarkMuted

    val glassAlpha: Float
        @Composable @ReadOnlyComposable get() =
            if (LocalVazifaDark.current) 0.18f else 0.72f

    val glassBorderAlpha: Float
        @Composable @ReadOnlyComposable get() =
            if (LocalVazifaDark.current) 0.28f else 0.55f
}

fun Modifier.liquidGlassDark(
    radius: Dp = LiquidGlass.RadiusCard,
    alpha: Float = 0.18f,
    borderAlpha: Float = 0.28f,
): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .clip(shape)
        .background(Color.White.copy(alpha = alpha))
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = borderAlpha * 2f),
                    Color.White.copy(alpha = borderAlpha),
                    LiquidGlass.Blue.copy(alpha = borderAlpha * 0.5f),
                    Color.White.copy(alpha = borderAlpha * 0.3f),
                ),
            ),
            shape = shape,
        )
}

fun Modifier.liquidGlassLight(radius: Dp = LiquidGlass.RadiusCard): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .clip(shape)
        .background(Color.White.copy(alpha = 0.72f))
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.95f),
                    LiquidGlass.Blue.copy(alpha = 0.15f),
                    Color.White.copy(alpha = 0.60f),
                ),
            ),
            shape = shape,
        )
}

@Composable
fun Modifier.liquidGlassThemed(radius: Dp = LiquidGlass.RadiusCard): Modifier =
    if (LocalVazifaDark.current) {
        liquidGlassDark(radius, LiquidTheme.glassAlpha, LiquidTheme.glassBorderAlpha)
    } else {
        liquidGlassLight(radius)
    }

fun Modifier.glowEffect(color: Color = LiquidGlass.Blue, radius: Float = 120f): Modifier = drawBehind {
    drawCircle(
        brush = Brush.radialGradient(
            listOf(color.copy(alpha = 0.35f), Color.Transparent),
            center = center,
            radius = radius,
        ),
        radius = radius,
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    radius: Dp = LiquidGlass.RadiusCard,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.liquidGlassThemed(radius), content = content)
}

@Composable
fun LiquidBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val isDark = LocalVazifaDark.current
    Box(
        modifier = modifier
            .background(if (isDark) LiquidGlass.BgDark else LiquidGlass.BgLight)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            LiquidGlass.Blue.copy(alpha = if (isDark) 0.24f else 0.08f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.2f, size.height * 0.15f),
                        radius = size.width * 0.6f,
                    ),
                    radius = size.width * 0.6f,
                    center = Offset(size.width * 0.2f, size.height * 0.15f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            LiquidGlass.Violet.copy(alpha = if (isDark) 0.16f else 0.06f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.7f),
                        radius = size.width * 0.5f,
                    ),
                    radius = size.width * 0.5f,
                    center = Offset(size.width * 0.85f, size.height * 0.7f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            LiquidGlass.Cyan.copy(alpha = if (isDark) 0.12f else 0.04f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.9f),
                        radius = size.width * 0.4f,
                    ),
                    radius = size.width * 0.4f,
                    center = Offset(size.width * 0.5f, size.height * 0.9f),
                )
            },
        content = content,
    )
}
