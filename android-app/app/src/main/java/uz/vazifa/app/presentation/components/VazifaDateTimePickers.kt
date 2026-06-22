@file:OptIn(ExperimentalMaterial3Api::class)

package uz.vazifa.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import uz.vazifa.app.presentation.theme.GlassCard
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

private fun LocalDate.toPickerUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun createFutureDatesOnlySelectable(minDate: LocalDate): SelectableDates {
    val minMillis = minDate.toPickerUtcMillis()
    return object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= minMillis
        override fun isSelectableYear(year: Int): Boolean = year >= minDate.year
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun vazifaDatePickerColors(): DatePickerColors {
    val isDark = LiquidTheme.isDark
    return DatePickerDefaults.colors(
        containerColor = Color.Transparent,
        titleContentColor = LiquidTheme.textMuted,
        headlineContentColor = LiquidTheme.text,
        weekdayContentColor = LiquidTheme.textMuted,
        subheadContentColor = LiquidTheme.text,
        navigationContentColor = LiquidGlass.BlueLight,
        yearContentColor = LiquidTheme.text,
        disabledYearContentColor = LiquidTheme.textMuted.copy(alpha = 0.35f),
        currentYearContentColor = LiquidGlass.BlueLight,
        selectedYearContentColor = Color.White,
        disabledSelectedYearContentColor = LiquidTheme.textMuted,
        selectedYearContainerColor = LiquidGlass.Blue,
        disabledSelectedYearContainerColor = LiquidTheme.textMuted.copy(alpha = 0.2f),
        dayContentColor = LiquidTheme.text,
        disabledDayContentColor = LiquidTheme.textMuted.copy(alpha = 0.35f),
        selectedDayContentColor = Color.White,
        disabledSelectedDayContentColor = LiquidTheme.textMuted.copy(alpha = 0.35f),
        selectedDayContainerColor = LiquidGlass.Blue,
        disabledSelectedDayContainerColor = LiquidTheme.textMuted.copy(alpha = 0.15f),
        todayContentColor = LiquidGlass.BlueLight,
        todayDateBorderColor = LiquidGlass.Blue,
        dividerColor = LiquidGlass.GlassDarkBorder.copy(alpha = if (isDark) 0.5f else 0.3f),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun vazifaTimePickerColors(): TimePickerColors = TimePickerDefaults.colors(
    clockDialColor = if (LiquidTheme.isDark) Color.White.copy(0.08f) else Color.White.copy(0.65f),
    clockDialSelectedContentColor = Color.White,
    clockDialUnselectedContentColor = LiquidTheme.text,
    selectorColor = LiquidGlass.Blue,
    containerColor = Color.Transparent,
    periodSelectorBorderColor = LiquidGlass.GlassDarkBorder,
    periodSelectorSelectedContainerColor = LiquidGlass.Blue,
    periodSelectorUnselectedContainerColor = if (LiquidTheme.isDark) Color.White.copy(0.08f) else Color.White.copy(0.5f),
    periodSelectorSelectedContentColor = Color.White,
    periodSelectorUnselectedContentColor = LiquidTheme.text,
    timeSelectorSelectedContainerColor = LiquidGlass.Blue,
    timeSelectorUnselectedContainerColor = if (LiquidTheme.isDark) Color.White.copy(0.08f) else Color.White.copy(0.5f),
    timeSelectorSelectedContentColor = Color.White,
    timeSelectorUnselectedContentColor = LiquidTheme.text,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VazifaDatePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (Long?) -> Unit,
    initialDateMillis: Long?,
    zoneId: ZoneId = ZoneId.of("Asia/Tashkent"),
) {
    val today = remember(zoneId) { LocalDate.now(zoneId) }
    val selectableDates = remember(today) { createFutureDatesOnlySelectable(today) }
    val validInitial = initialDateMillis?.takeIf { millis ->
        !Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().isBefore(today)
    } ?: today.toPickerUtcMillis()

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = validInitial,
        selectableDates = selectableDates,
    )
    val pickerColors = vazifaDatePickerColors()

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            radius = LiquidGlass.RadiusCard,
        ) {
            Column(Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                DatePicker(
                    state = datePickerState,
                    colors = pickerColors,
                    showModeToggle = false,
                    title = null,
                    headline = {
                        Text(
                            localized("task_deadline_pick_date"),
                            modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 8.dp),
                            color = LiquidTheme.text,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                        )
                    },
                )
                Row(
                    Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = onDismissRequest,
                        colors = ButtonDefaults.textButtonColors(contentColor = LiquidTheme.textMuted),
                    ) {
                        Text(localized("com_cancel"))
                    }
                    TextButton(
                        onClick = { onConfirm(datePickerState.selectedDateMillis) },
                        colors = ButtonDefaults.textButtonColors(contentColor = LiquidGlass.BlueLight),
                    ) {
                        Text(localized("com_save"), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VazifaTimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
    initialHour: Int = 10,
    initialMinute: Int = 0,
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
    )

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            radius = LiquidGlass.RadiusCard,
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    localized("task_deadline_pick_time"),
                    color = LiquidTheme.text,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                )
                TimePicker(
                    state = timePickerState,
                    colors = vazifaTimePickerColors(),
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = onDismissRequest,
                        colors = ButtonDefaults.textButtonColors(contentColor = LiquidTheme.textMuted),
                    ) {
                        Text(localized("com_cancel"))
                    }
                    TextButton(
                        onClick = { onConfirm(timePickerState.hour, timePickerState.minute) },
                        colors = ButtonDefaults.textButtonColors(contentColor = LiquidGlass.BlueLight),
                    ) {
                        Text(localized("com_save"), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
