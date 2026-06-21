package uz.vazifa.app.data.remote

import retrofit2.http.*

data class LoginRequest(val login: String, val password: String, val deviceId: String)
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserDto,
    val devicePendingApproval: Boolean = false,
)
data class UserDto(
    val id: String,
    val login: String,
    val fullName: String,
    val role: String,
    val canAssignTasks: Boolean = false,
    val position: String? = null,
    val department: String? = null,
    val notificationsEnabled: Boolean = true,
)
data class FcmRequest(val fcmToken: String, val notificationsEnabled: Boolean)
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)
data class CreateTaskRequest(
    val title: String,
    val description: String?,
    val priority: String,
    val assigneeIds: List<String>,
    val startAt: String,
    val deadlineAt: String,
)
data class UpdateStatusRequest(val status: String)
data class CommentRequest(val body: String)
data class DashboardStatsDto(
    val totalEmployees: Int,
    val activeTasks: Int,
    val completedTasks: Int,
    val overdueTasks: Int,
    val todayTasks: Int,
)

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body body: Map<String, String>): Map<String, String>

    @GET("auth/me")
    suspend fun me(): UserDto

    @POST("auth/fcm")
    suspend fun updateFcm(@Body body: FcmRequest): UserDto

    @POST("auth/change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequest)

    @GET("auth/time")
    suspend fun serverTime(): Map<String, Any>

    @GET("tasks")
    suspend fun getTasks(): List<TaskDto>

    @GET("tasks/{id}")
    suspend fun getTask(@Path("id") id: String): TaskDto

    @GET("tasks/dashboard/stats")
    suspend fun dashboardStats(): DashboardStatsDto

    @POST("tasks")
    suspend fun createTask(@Body body: CreateTaskRequest): TaskDto

    @PATCH("tasks/{taskId}/assignments/{assignmentId}/status")
    suspend fun updateAssignmentStatus(
        @Path("taskId") taskId: String,
        @Path("assignmentId") assignmentId: String,
        @Body body: UpdateStatusRequest,
    ): TaskAssignmentDto

    @POST("tasks/{id}/comments")
    suspend fun addComment(@Path("id") id: String, @Body body: CommentRequest)

    @GET("users/mobile/contacts")
    suspend fun getContacts(): List<UserDto>
}

data class TaskDto(
    val id: String,
    val title: String,
    val description: String?,
    val priority: String,
    val status: String,
    val startAt: String,
    val deadlineAt: String,
    val createdById: String,
    val assignments: List<TaskAssignmentDto>? = null,
    val createdBy: UserDto? = null,
)

data class TaskAssignmentDto(
    val id: String,
    val assigneeId: String,
    val status: String,
    val assignee: UserDto? = null,
)
