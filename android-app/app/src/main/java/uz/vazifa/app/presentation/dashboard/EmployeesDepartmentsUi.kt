package uz.vazifa.app.presentation.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.vazifa.app.domain.model.Department
import uz.vazifa.app.domain.model.User
import uz.vazifa.app.presentation.components.EmployeePresenceDot
import uz.vazifa.app.presentation.components.EmployeePresenceStatus
import uz.vazifa.app.presentation.components.GlassHeaderIconButton
import uz.vazifa.app.presentation.components.localized
import uz.vazifa.app.presentation.components.liquidGlassFieldColors
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.presentation.theme.VazifaColors
import uz.vazifa.app.presentation.theme.liquidGlassThemed

private val departmentPalette = listOf(
    VazifaColors.Primary,
    LiquidGlass.BlueLight,
    VazifaColors.Success,
    LiquidGlass.Cyan,
    LiquidGlass.Emerald,
    VazifaColors.Danger,
    LiquidGlass.Rose,
)

@Composable
fun EmployeeSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSearch: (() -> Unit)? = null,
) {
    val fieldColors = liquidGlassFieldColors()
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                localized("task_search_employee"),
                color = LiquidTheme.textMuted,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp, max = 48.dp),
        shape = RoundedCornerShape(LiquidGlass.RadiusInput),
        colors = fieldColors,
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = LiquidTheme.textMuted,
                modifier = Modifier.size(20.dp),
            )
        },
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
    )
}

@Composable
fun EmployeesHubContent(
    totalEmployees: Int,
    departments: List<Department>,
    searchQuery: String,
    onSearch: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onDepartmentClick: (String?) -> Unit,
    topContent: (@Composable () -> Unit)? = null,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        topContent?.let { content ->
            item { content() }
        }
        item {
            EmployeeSearchField(
                value = searchQuery,
                onValueChange = onSearch,
                onSearch = onSearchSubmit,
            )
        }
        item {
            EmployeeDepartmentGrid(
                totalEmployees = totalEmployees,
                departments = departments,
                onDepartmentClick = onDepartmentClick,
            )
        }
    }
}

@Composable
fun EmployeeDepartmentGrid(
    totalEmployees: Int,
    departments: List<Department>,
    onDepartmentClick: (String?) -> Unit,
) {
    val allItems = buildList {
        add(null to totalEmployees)
        departments.forEach { add(it.name to it.employeeCount) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        allItems.chunked(2).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { (name, count) ->
                    EmployeeDepartmentCard(
                        label = name ?: localized("dash_total"),
                        count = count,
                        icon = if (name == null) Icons.Default.People else Icons.Default.Business,
                        color = if (name == null) {
                            VazifaColors.Primary
                        } else {
                            departmentPalette[kotlin.math.abs(name.hashCode()) % departmentPalette.size]
                        },
                        onClick = { onDepartmentClick(name) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EmployeeDepartmentCard(
    label: String,
    count: Int,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(220),
        label = "dept_card_scale",
    )

    Column(
        modifier
            .scale(scale)
            .clip(RoundedCornerShape(LiquidGlass.RadiusCard))
            .border(
                width = 1.dp,
                color = Color.Transparent,
                shape = RoundedCornerShape(LiquidGlass.RadiusCard),
            )
            .liquidGlassThemed()
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            label,
            color = LiquidTheme.textMuted,
            fontSize = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 14.sp,
        )
        Text(
            "$count",
            color = LiquidTheme.text,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
        )
    }
}

@Composable
fun EmployeeRow(
    user: User,
    style: SectionStyle,
    selected: Boolean,
    onToggleSelect: () -> Unit,
    onClick: () -> Unit,
    onAssignTask: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .liquidGlassThemed()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(checked = selected, onCheckedChange = { onToggleSelect() })
        Row(
            Modifier
                .weight(1f)
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.size(42.dp)) {
                uz.vazifa.app.presentation.chat.ChatAvatar(
                    name = user.fullName,
                    online = false,
                    size = 42.dp,
                    showPresence = false,
                    avatarUrl = user.avatarUrl,
                )
                Box(
                    Modifier
                        .size(11.dp)
                        .align(Alignment.BottomEnd),
                ) {
                    EmployeePresenceDot(user)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(user.fullName, color = LiquidTheme.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                val subtitle = listOfNotNull(
                    user.position?.takeIf { it.isNotBlank() },
                    user.department?.takeIf { it.isNotBlank() },
                    user.phone?.takeIf { it.isNotBlank() },
                ).joinToString(" · ")
                if (subtitle.isNotBlank()) {
                    Text(subtitle, color = LiquidTheme.textMuted, fontSize = 12.sp)
                }
                EmployeePresenceStatus(user)
            }
        }
        GlassHeaderIconButton(
            onClick = onAssignTask,
            icon = Icons.Default.Add,
            tint = LiquidGlass.Blue,
            contentDescription = localized("task_create_for_employee"),
        )
    }
}

@Composable
fun EmployeesEmptyCard(style: SectionStyle) {
    Column(
        Modifier
            .fillMaxWidth()
            .liquidGlassThemed()
            .padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(style.accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(style.icon, null, tint = style.accent, modifier = Modifier.size(28.dp))
        }
        Text(
            localized("dash_empty"),
            color = LiquidTheme.textMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
