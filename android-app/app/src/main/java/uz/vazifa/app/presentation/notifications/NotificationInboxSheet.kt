package uz.vazifa.app.presentation.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.vazifa.app.domain.model.InboxNotification
import uz.vazifa.app.presentation.components.localized
import uz.vazifa.app.presentation.theme.GlassCard
import uz.vazifa.app.presentation.theme.LiquidTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationInboxSheet(
    visible: Boolean,
    items: List<InboxNotification>,
    onDismiss: () -> Unit,
    onItemClick: (InboxNotification) -> Unit,
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val timeFmt = DateTimeFormatter.ofPattern("dd.MM HH:mm")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LiquidTheme.bgMid,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                localized("notif_inbox_title"),
                color = LiquidTheme.text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            if (items.isEmpty()) {
                Text(
                    localized("notif_inbox_empty"),
                    color = LiquidTheme.textMuted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(items, key = { it.id }) { item ->
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onItemClick(item) },
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text(
                                    item.title,
                                    color = LiquidTheme.text,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                )
                                if (item.body.isNotBlank()) {
                                    Text(
                                        item.body,
                                        color = LiquidTheme.textMuted,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(top = 4.dp),
                                    )
                                }
                                Text(
                                    Instant.ofEpochMilli(item.receivedAt)
                                        .atZone(ZoneId.systemDefault())
                                        .format(timeFmt),
                                    color = LiquidTheme.textMuted,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
