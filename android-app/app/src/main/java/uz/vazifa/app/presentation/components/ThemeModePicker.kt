package uz.vazifa.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.vazifa.app.data.repository.ThemeMode
import uz.vazifa.app.presentation.theme.*

@Composable
fun ThemeModePicker(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ThemeModeRow(
            label = localized("com_theme_dark"),
            icon = Icons.Default.NightlightRound,
            selected = selected == ThemeMode.DARK,
            accent = Brush.linearGradient(listOf(LiquidGlass.Blue, LiquidGlass.Violet)),
            onClick = { onSelect(ThemeMode.DARK) },
        )
        ThemeModeRow(
            label = localized("com_theme_light"),
            icon = Icons.Default.WbSunny,
            selected = selected == ThemeMode.LIGHT,
            accent = Brush.linearGradient(listOf(LiquidGlass.Amber, LiquidGlass.Cyan)),
            onClick = { onSelect(ThemeMode.LIGHT) },
        )
        ThemeModeRow(
            label = localized("com_theme_system"),
            icon = Icons.Default.SettingsBrightness,
            selected = selected == ThemeMode.SYSTEM,
            accent = Brush.linearGradient(listOf(LiquidGlass.Emerald, LiquidGlass.Cyan)),
            onClick = { onSelect(ThemeMode.SYSTEM) },
        )
    }
}

@Composable
private fun ThemeModeRow(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    accent: Brush,
    onClick: () -> Unit,
) {
    val modifier = Modifier
        .fillMaxWidth()
        .then(
            if (selected) {
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                LiquidGlass.Blue.copy(alpha = 0.28f),
                                LiquidGlass.Violet.copy(alpha = 0.18f),
                            ),
                        ),
                    )
                    .border(
                        2.dp,
                        accent,
                        RoundedCornerShape(20.dp),
                    )
            } else {
                Modifier.liquidGlassThemed(radius = 20.dp)
            },
        )

    Row(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(50))
                .background(if (selected) accent else Brush.linearGradient(listOf(Color.White.copy(0.10f), Color.White.copy(0.06f)))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = if (selected) Color.White else LiquidTheme.textMuted, modifier = Modifier.size(22.dp))
        }
        Text(
            label,
            modifier = Modifier.weight(1f),
            color = if (selected) LiquidTheme.text else LiquidTheme.textMuted,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 15.sp,
        )
        if (selected) {
            Box(
                Modifier.size(22.dp).clip(RoundedCornerShape(50)).background(accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}
