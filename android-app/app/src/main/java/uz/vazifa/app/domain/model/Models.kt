package uz.vazifa.app.domain.model

enum class UserRole { DIRECTOR, EMPLOYEE, ADMIN }

enum class TaskStatus(val key: String, val label: String) {
    NEW("new", "Yangi"),
    ACCEPTED("accepted", "Qabul qilindi"),
    IN_PROGRESS("in_progress", "Jarayonda"),
    IN_REVIEW("in_review", "Tekshiruvda"),
    COMPLETED("completed", "Bajarildi"),
    REWORK("rework", "Qayta ishlash"),
    CANCELLED("cancelled", "Bekor qilindi"),
}

enum class TaskPriority(val key: String, val label: String) {
    LOW("low", "Past"),
    MEDIUM("medium", "O'rta"),
    HIGH("high", "Yuqori"),
    URGENT("urgent", "Shoshilinch"),
}

data class User(
    val id: String,
    val login: String,
    val fullName: String,
    val role: String,
    val position: String? = null,
    val department: String? = null,
    val notificationsEnabled: Boolean = true,
)

data class TaskAssignment(
    val id: String,
    val assigneeId: String,
    val status: String,
    val assignee: User? = null,
)

data class Task(
    val id: String,
    val title: String,
    val description: String? = null,
    val priority: String,
    val status: String,
    val startAt: String,
    val deadlineAt: String,
    val createdById: String,
    val assignments: List<TaskAssignment> = emptyList(),
    val createdBy: User? = null,
)

data class DashboardStats(
    val totalEmployees: Int = 0,
    val activeTasks: Int = 0,
    val completedTasks: Int = 0,
    val overdueTasks: Int = 0,
    val todayTasks: Int = 0,
)
