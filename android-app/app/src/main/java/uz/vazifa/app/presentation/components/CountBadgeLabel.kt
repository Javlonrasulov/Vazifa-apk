package uz.vazifa.app.presentation.components

import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun CountBadgeLabel(
    text: String,
    fontSize: TextUnit = 11.sp,
    color: Color = Color.White,
) {
    Text(
        text,
        color = color,
        style = TextStyle(
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            lineHeight = fontSize,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            textAlign = TextAlign.Center,
        ),
        modifier = Modifier.wrapContentSize(Alignment.Center),
    )
}

fun formatBadgeCount(count: Int): String = if (count > 99) "99+" else count.toString()
