package uz.vazifa.app.data.repository

import uz.vazifa.app.data.remote.ApiClient
import uz.vazifa.app.data.remote.CreateTaskRequest
import uz.vazifa.app.data.remote.TaskDto
import uz.vazifa.app.domain.model.DashboardStats
import uz.vazifa.app.domain.model.Task
import uz.vazifa.app.domain.model.TaskAssignment
import uz.vazifa.app.domain.model.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(private val api: ApiClient) {

    suspend fun getTasks(): List<Task> = api.api.getTasks().map { it.toDomain() }

    suspend fun getTask(id: String): Task = api.api.getTask(id).toDomain()

    suspend fun getDashboardStats(): DashboardStats {
        val d = api.api.dashboardStats()
        return DashboardStats(d.totalEmployees, d.activeTasks, d.completedTasks, d.overdueTasks, d.todayTasks)
    }

    suspend fun createTask(
        title: String,
        description: String?,
        priority: String,
        assigneeIds: List<String>,
        startAt: String,
        deadlineAt: String,
    ): Task = api.api.createTask(
        CreateTaskRequest(title, description, priority, assigneeIds, startAt, deadlineAt),
    ).toDomain()

    suspend fun updateStatus(taskId: String, assignmentId: String, status: String) {
        api.api.updateAssignmentStatus(taskId, assignmentId, uz.vazifa.app.data.remote.UpdateStatusRequest(status))
    }

    suspend fun getContacts(): List<User> = api.api.getContacts().map {
        User(it.id, it.login, it.fullName, it.role, it.position, it.department)
    }

    private fun TaskDto.toDomain() = Task(
        id, title, description, priority, status, startAt, deadlineAt, createdById,
        assignments?.map { TaskAssignment(it.id, it.assigneeId, it.status, it.assignee?.let { u ->
            User(u.id, u.login, u.fullName, u.role, u.position, u.department)
        }) } ?: emptyList(),
        createdBy?.let { User(it.id, it.login, it.fullName, it.role, it.position, it.department) },
    )
}
