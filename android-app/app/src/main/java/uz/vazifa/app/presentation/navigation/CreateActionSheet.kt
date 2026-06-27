package uz.vazifa.app.presentation.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.vazifa.app.presentation.components.localized
import uz.vazifa.app.presentation.theme.GlassCard
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidTheme

enum class CreateAction {
    NEW_TASK,
    NEW_CHAT,
    SUPPORT,
}

private data class CreateActionItem(
    val action: CreateAction,
    val icon: ImageVector,
    val titleKey: String,
    val descKey: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateActionSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onAction: (CreateAction) -> Unit,
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val items = listOf(
        CreateActionItem(CreateAction.NEW_TASK, Icons.Default.Assignment, "create_new_task", "create_new_task_desc"),
        CreateActionItem(CreateAction.NEW_CHAT, Icons.Default.Chat, "create_new_chat", "create_new_chat_desc"),
        CreateActionItem(CreateAction.SUPPORT, Icons.Default.HeadsetMic, "create_support", "create_support_desc"),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LiquidTheme.bgMid,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                localized("create_sheet_title"),
                color = LiquidTheme.text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 24.dp),
            ) {
                items.forEach { item ->
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                onAction(item.action)
                            },
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Icon(
                                item.icon,
                                contentDescription = null,
                                tint = LiquidGlass.Blue,
                                modifier = Modifier.size(28.dp),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    localized(item.titleKey),
                                    color = LiquidTheme.text,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                )
                                Text(
                                    localized(item.descKey),
                                    color = LiquidTheme.textMuted,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
