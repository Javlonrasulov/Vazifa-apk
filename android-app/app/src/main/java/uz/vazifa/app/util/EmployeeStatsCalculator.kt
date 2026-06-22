package uz.vazifa.app.util

import uz.vazifa.app.domain.model.Task
import uz.vazifa.app.domain.model.TaskAssignment
import uz.vazifa.app.domain.model.TaskStatus
import java.time.Instant

data class EmployeeStats(
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val activeTasks: Int = 0,
    val overdueTasks: Int = 0,
    val onTimeCompleted: Int = 0,
    val lateCompleted: Int = 0,
    val completionRate: Int = 0,
    val onTimeRate: Int = 0,
)

data class EmployeeTaskItem(
    val task: Task,
    val assignment: TaskAssignment,
)

private val finishedStatuses = setOf(TaskStatus.COMPLETED.key, TaskStatus.CANCELLED.key)

object EmployeeStatsCalculator {
    fun compute(employeeId: String, tasks: List<Task>): Pair<EmployeeStats, List<EmployeeTaskItem>> {
        val items = tasks.flatMap { task ->
            task.assignments
                .filter { it.assigneeId == employeeId }
                .map { EmployeeTaskItem(task, it) }
        }.sortedByDescending { it.task.deadlineAt }

        val completed = items.filter { it.assignment.status == TaskStatus.COMPLETED.key }
        val active = items.filter { it.assignment.status !in finishedStatuses }
        val overdue = active.filter { item -> isAssignmentOverdue(item) }

        var onTime = 0
        var late = 0
        completed.forEach { item ->
            val deadline = TaskDeadlineCountdown.parseDeadline(item.task.deadlineAt)
            val completedAt = item.assignment.completedAt?.let { parseInstant(it) }
            when {
                deadline == null || completedAt == null -> onTime++
                !completedAt.isAfter(deadline) -> onTime++
                else -> late++
            }
        }

        val completionRate = if (items.isEmpty()) 0 else completed.size * 100 / items.size
        val onTimeRate = if (completed.isEmpty()) 0 else onTime * 100 / completed.size

        return EmployeeStats(
            totalTasks = items.size,
            completedTasks = completed.size,
            activeTasks = active.size,
            overdueTasks = overdue.size,
            onTimeCompleted = onTime,
            lateCompleted = late,
            completionRate = completionRate,
            onTimeRate = onTimeRate,
        ) to items
    }

    fun isAssignmentOverdue(item: EmployeeTaskItem): Boolean {
        if (item.assignment.status in finishedStatuses) return false
        val deadline = TaskDeadlineCountdown.parseDeadline(item.task.deadlineAt) ?: return false
        return deadline.isBefore(Instant.now())
    }

    private fun parseInstant(raw: String): Instant? = runCatching {
        Instant.parse(raw)
    }.getOrNull()
}
