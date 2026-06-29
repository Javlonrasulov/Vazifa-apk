package uz.vazifa.app.presentation.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.vazifa.app.presentation.components.localized
import uz.vazifa.app.presentation.theme.GlassCard
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportContactSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
) {
    if (!visible) return

    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun openDial(uri: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse(uri)))
        }
    }

    fun openSms(uri: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse(uri)))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LiquidTheme.bgMid,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                localized("support_contact_title"),
                color = LiquidTheme.text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Text(
                localized("support_contact_desc"),
                color = LiquidTheme.textMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 24.dp),
            ) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(SupportContacts.TELEGRAM_URL)),
                                )
                            }
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
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = LiquidGlass.Blue,
                            modifier = Modifier.size(28.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                localized("support_telegram"),
                                color = LiquidTheme.text,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                            )
                            Text(
                                "@${SupportContacts.TELEGRAM_USERNAME}",
                                color = LiquidTheme.textMuted,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }

                SupportContacts.phones.forEach { phone ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = LiquidGlass.Blue,
                                    modifier = Modifier.size(26.dp),
                                )
                                Text(
                                    phone.label,
                                    color = LiquidTheme.text,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        onDismiss()
                                        openDial(phone.dialUri)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(LiquidGlass.RadiusChip),
                                ) {
                                    Icon(Icons.Default.Call, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(localized("support_call"), fontSize = 13.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        onDismiss()
                                        openSms(phone.smsUri)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(LiquidGlass.RadiusChip),
                                ) {
                                    Icon(Icons.Default.Sms, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(localized("support_sms"), fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
