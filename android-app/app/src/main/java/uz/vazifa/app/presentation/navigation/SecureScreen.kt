package uz.vazifa.app.presentation.navigation

internal fun isTaskRelatedScreen(route: String?, selectedTab: AppTab): Boolean {
    if (route == null) return false
    if (route in setOf(Routes.LOGIN, Routes.SPLASH, Routes.NOTIFICATION_GATE)) return false

    if (route == Routes.MAIN) {
        return selectedTab in setOf(
            AppTab.HOME,
            AppTab.TASKS,
            AppTab.CREATE,
            AppTab.DEPT_TASKS,
            AppTab.EMPLOYEES,
        )
    }

    return route.startsWith("task/") ||
        route.startsWith("edit_task/") ||
        route.startsWith("create_task") ||
        route.startsWith("dash/") ||
        route.startsWith("employee/")
}
