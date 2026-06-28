package uz.vazifa.app.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.vazifa.app.presentation.components.CountBadgeLabel
import uz.vazifa.app.presentation.components.formatBadgeCount
import uz.vazifa.app.presentation.theme.LiquidGlass

@Composable
fun ChatUnreadBadge(
    count: Int,
    modifier: Modifier = Modifier,
    background: Color = LiquidGlass.Blue,
) {
    if (count <= 0) return
    val text = formatBadgeCount(count)
    val wide = text.length >= 2
    Box(
        modifier
            .defaultMinSize(minWidth = if (wide) 22.dp else 18.dp, minHeight = 18.dp)
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = if (wide) 5.dp else 0.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        CountBadgeLabel(
            text = text,
            fontSize = if (text == "99+") 9.sp else if (wide) 10.sp else 11.sp,
        )
    }
}
