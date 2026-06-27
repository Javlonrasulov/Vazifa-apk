package uz.vazifa.app.presentation.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
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
fun EmployeesPageContent(
    employees: List<User>,
    departments: List<Department>,
    selectedDepartment: String?,
    searchQuery: String,
    selectedEmployeeIds: Set<String>,
    style: SectionStyle,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onDepartmentSelected: (String?) -> Unit,
    onSearch: (String) -> Unit,
    onToggleEmployee: (String) -> Unit,
    onEmployeeClick: (String) -> Unit,
    onAssignTask: (String) -> Unit,
) {
    val fieldColors = liquidGlassFieldColors()
    val listTitle = when {
        searchQuery.isNotBlank() -> localized("emp_search_results")
        selectedDepartment != null -> selectedDepartment
        else -> localized("emp_all_staff")
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = bottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearch,
                label = { Text(localized("task_search_employee")) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(LiquidGlass.RadiusInput),
                colors = fieldColors,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = LiquidTheme.textMuted)
                },
                singleLine = true,
            )
        }
        item {
            EmployeeDepartmentGrid(
                totalEmployees = employees.size,
                departments = departments,
                selectedDepartment = selectedDepartment,
                onDepartmentSelected = onDepartmentSelected,
            )
        }
        item {
            AnimatedContent(
                targetState = listTitle,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
                label = "emp_list_title",
            ) { title ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        title,
                        color = LiquidTheme.text,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                    )
                    Text(
                        "${employees.size} ${localized("dash_unit")}",
                        color = LiquidTheme.textMuted,
                        fontSize = 13.sp,
                    )
                }
            }
        }
        if (employees.isEmpty()) {
            item { EmployeesEmptyCard(style) }
        } else {
            items(employees, key = { it.id }) { employee ->
                EmployeeRow(
                    user = employee,
                    style = style,
                    selected = employee.id in selectedEmployeeIds,
                    onToggleSelect = { onToggleEmployee(employee.id) },
                    onClick = { onEmployeeClick(employee.id) },
                    onAssignTask = { onAssignTask(employee.id) },
                )
            }
        }
    }
}

@Composable
fun EmployeeDepartmentGrid(
    totalEmployees: Int,
    departments: List<Department>,
    selectedDepartment: String?,
    onDepartmentSelected: (String?) -> Unit,
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
                        selected = if (name == null) selectedDepartment == null else selectedDepartment == name,
                        onClick = { onDepartmentSelected(name) },
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
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.03f else 1f,
        animationSpec = tween(220),
        label = "dept_card_scale",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) LiquidGlass.Blue else Color.Transparent,
        animationSpec = tween(220),
        label = "dept_card_border",
    )
    val bgAlpha by animateFloatAsState(
        targetValue = if (selected) 0.14f else 0f,
        animationSpec = tween(220),
        label = "dept_card_bg",
    )

    Column(
        modifier
            .scale(scale)
            .clip(RoundedCornerShape(LiquidGlass.RadiusCard))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(LiquidGlass.RadiusCard),
            )
            .liquidGlassThemed()
            .background(LiquidGlass.Blue.copy(alpha = bgAlpha))
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
        androidx.compose.material3.Checkbox(checked = selected, onCheckedChange = { onToggleSelect() })
        Row(
            Modifier
                .weight(1f)
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.size(42.dp)) {
                Box(
                    Modifier
                        .matchParentSize()
                        .clip(CircleShape)
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(style.gradient),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        employeeInitials(user.fullName),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                }
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

private fun employeeInitials(name: String): String =
    name.trim().split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }

@Composable
private fun EmployeesEmptyCard(style: SectionStyle) {
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
