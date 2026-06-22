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
    val phone: String? = null,
    val notificationsEnabled: Boolean = true,
)

data class TaskAssignment(
    val id: String,
    val assigneeId: String,
    val status: String,
    val assignee: User? = null,
    val completedAt: String? = null,
    val acceptedAt: String? = null,
)

data class TaskComment(
    val id: String,
    val body: String,
    val authorId: String,
    val createdAt: String,
    val author: User? = null,
)

data class TaskAttachment(
    val id: String,
    val fileName: String,
    val filePath: String,
    val mimeType: String,
    val fileSize: Long,
    val uploadedById: String,
    val url: String,
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
    val attachments: List<TaskAttachment> = emptyList(),
    val comments: List<TaskComment> = emptyList(),
)

data class DashboardStats(
    val totalEmployees: Int = 0,
    val activeTasks: Int = 0,
    val completedTasks: Int = 0,
    val overdueTasks: Int = 0,
    val todayTasks: Int = 0,
)

private val finishedStatuses = setOf(TaskStatus.COMPLETED.key, TaskStatus.CANCELLED.key)

fun Task.hasActiveAssignment(): Boolean =
    assignments.any { it.status !in finishedStatuses }

fun Task.hasCompletedAssignment(): Boolean =
    assignments.any { it.status == TaskStatus.COMPLETED.key }

fun Task.myAssignment(userId: String?) =
    userId?.let { uid -> assignments.firstOrNull { it.assigneeId == uid } }

fun Task.isCreator(userId: String?) =
    userId != null && createdById == userId

fun Task.isOverdue(): Boolean {
    if (!hasActiveAssignment()) return false
    return runCatching {
        java.time.Instant.parse(deadlineAt).isBefore(java.time.Instant.now())
    }.getOrDefault(false)
}
