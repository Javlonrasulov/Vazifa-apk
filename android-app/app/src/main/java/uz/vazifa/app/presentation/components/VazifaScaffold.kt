package uz.vazifa.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uz.vazifa.app.presentation.theme.LiquidBackground
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.presentation.theme.VazifaColors

private val topBarBorder = LiquidGlass.GlassDarkBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VazifaTabScaffold(
    title: String? = null,
    titleContent: (@Composable () -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = LiquidTheme.bg,
        topBar = {
            TopAppBar(
                modifier = Modifier.drawBehind {
                    drawLine(
                        color = topBarBorder,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1f,
                    )
                },
                title = {
                    when {
                        titleContent != null -> titleContent()
                        title != null -> Text(
                            title,
                            fontWeight = FontWeight.Bold,
                            color = LiquidTheme.text,
                        )
                    }
                },
                actions = { actions() },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LiquidTheme.bgMid.copy(alpha = 0.88f),
                    titleContentColor = LiquidTheme.text,
                    actionIconContentColor = LiquidTheme.textMuted,
                    navigationIconContentColor = LiquidTheme.textMuted,
                ),
            )
        },
    ) { padding -> content(padding) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VazifaStackScaffold(
    title: String,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = LiquidTheme.bg,
        topBar = {
            TopAppBar(
                modifier = Modifier.drawBehind {
                    drawLine(
                        color = topBarBorder,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1f,
                    )
                },
                title = {
                    Text(
                        title,
                        fontWeight = FontWeight.Bold,
                        color = LiquidTheme.text,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                },
                navigationIcon = { VazifaBackButton(onBack = onBack) },
                actions = { actions() },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LiquidTheme.bgMid.copy(alpha = 0.88f),
                    titleContentColor = LiquidTheme.text,
                    actionIconContentColor = LiquidTheme.textMuted,
                    navigationIconContentColor = LiquidGlass.BlueLight,
                ),
            )
        },
    ) { padding ->
        LiquidBackground(Modifier.fillMaxSize()) {
            content(padding)
        }
    }
}

@Composable
fun VazifaBackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val primary = if (LiquidTheme.isDark) VazifaColors.Primary else VazifaColors.PrimaryLight
    IconButton(
        onClick = onBack,
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(primary.copy(alpha = 0.12f))
            .border(1.dp, primary.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = localized("com_back"),
            tint = primary,
        )
    }
}

@Composable
fun VazifaScreenBox(padding: PaddingValues, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(padding)) {
        content()
    }
}
