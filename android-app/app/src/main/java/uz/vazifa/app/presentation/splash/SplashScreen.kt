package uz.vazifa.app.presentation.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.vazifa.app.R
import uz.vazifa.app.presentation.components.localized
import uz.vazifa.app.presentation.theme.LiquidBackground
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.presentation.theme.glowEffect

private const val SPLASH_BAR_MS = 400

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
) {
    val infinite = rememberInfiniteTransition(label = "splash-orbs")
    val orbDrift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "orbDrift",
    )
    val glowPulse by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowPulse",
    )
    val shimmer by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer",
    )

    val logoScale = 1f
    val logoAlpha = 1f
    val titleAlpha = 1f
    val titleOffset = 0f
    val subtitleAlpha = 1f

    val barProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(SPLASH_BAR_MS, easing = FastOutSlowInEasing),
        label = "barProgress",
    )

    LiquidBackground(modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .size(220.dp)
                    .align(Alignment.TopStart)
                    .offset(x = (-40 + orbDrift * 24).dp, y = (60 + orbDrift * 18).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                LiquidGlass.Cyan.copy(alpha = 0.22f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
            Box(
                Modifier
                    .size(180.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = (20 - orbDrift * 16).dp, y = (-80 - orbDrift * 12).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                LiquidGlass.Violet.copy(alpha = 0.18f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )

            Column(
                Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(112.dp * glowPulse)
                        .scale(logoScale)
                        .alpha(logoAlpha)
                        .glowEffect(color = LiquidGlass.Blue, radius = 220f),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        LiquidGlass.Blue,
                                        LiquidGlass.BlueLight,
                                        LiquidGlass.Cyan.copy(alpha = 0.95f + shimmer * 0.05f),
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(200f, 200f),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier
                                .size(72.dp)
                                .padding(8.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                Column(
                    Modifier
                        .alpha(titleAlpha)
                        .offset(y = titleOffset.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        localized("app_name"),
                        color = LiquidTheme.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        letterSpacing = 0.5.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        localized("app_subtitle"),
                        modifier = Modifier.alpha(subtitleAlpha),
                        color = LiquidTheme.textMuted,
                        fontSize = 14.sp,
                    )
                }
            }

            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
                    .fillMaxWidth(0.55f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(LiquidTheme.textMuted.copy(alpha = 0.15f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(barProgress)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(LiquidGlass.GradientPrimary),
                    )
                }
                Spacer(Modifier.height(16.dp))
                LoadingDots(alpha = titleAlpha)
            }
        }
    }
}

@Composable
private fun LoadingDots(alpha: Float) {
    val infinite = rememberInfiniteTransition(label = "dots")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.alpha(alpha * 0.85f)) {
        repeat(3) { index ->
            val scale by infinite.animateFloat(
                initialValue = 0.6f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = index * 140, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot$index",
            )
            Box(
                Modifier
                    .size(8.dp * scale)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(LiquidGlass.Blue, LiquidGlass.Cyan),
                        ),
                    ),
            )
        }
    }
}
