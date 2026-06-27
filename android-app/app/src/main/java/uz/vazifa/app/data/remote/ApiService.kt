package uz.vazifa.app.data.remote

import okhttp3.MultipartBody
import retrofit2.http.*

data class LoginRequest(
    val login: String,
    val password: String,
    val deviceId: String,
    val deviceName: String? = null,
)
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
    val visibleDepartments: List<String>? = null,
    val phone: String? = null,
    val notificationsEnabled: Boolean = true,
    val isOnline: Boolean = false,
    val lastSeenAt: String? = null,
)
data class FcmRequest(
    val fcmToken: String,
    val notificationsEnabled: Boolean,
    val language: String? = null,
)
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
data class UpdateTaskRequest(
    val title: String? = null,
    val description: String? = null,
    val priority: String? = null,
    val startAt: String? = null,
    val deadlineAt: String? = null,
)
data class CommentRequest(val body: String)
data class DepartmentDto(
    val id: String,
    val name: String,
    val employeeCount: Int,
)

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

    @POST("auth/presence")
    suspend fun sendPresence(): UserDto

    @POST("auth/change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequest)

    @GET("auth/time")
    suspend fun serverTime(): Map<String, Any>

    @GET("tasks")
    suspend fun getTasks(): List<TaskDto>

    @GET("tasks/department")
    suspend fun getDepartmentTasks(): List<TaskDto>

    @GET("tasks/{id}")
    suspend fun getTask(@Path("id") id: String): TaskDto

    @GET("tasks/dashboard/stats")
    suspend fun dashboardStats(): DashboardStatsDto

    @POST("tasks")
    suspend fun createTask(@Body body: CreateTaskRequest): TaskDto

    @PATCH("tasks/{id}")
    suspend fun updateTask(@Path("id") id: String, @Body body: UpdateTaskRequest): TaskDto

    @DELETE("tasks/{id}")
    suspend fun cancelTask(@Path("id") id: String): TaskDto

    @PATCH("tasks/{taskId}/assignments/{assignmentId}/status")
    suspend fun updateAssignmentStatus(
        @Path("taskId") taskId: String,
        @Path("assignmentId") assignmentId: String,
        @Body body: UpdateStatusRequest,
    ): TaskAssignmentDto

    @POST("tasks/{id}/comments")
    suspend fun addComment(@Path("id") id: String, @Body body: CommentRequest): TaskCommentDto

    @Multipart
    @POST("tasks/{id}/attachments")
    suspend fun uploadAttachment(
        @Path("id") id: String,
        @Part file: MultipartBody.Part,
    ): TaskAttachmentDto

    @GET("users/mobile/contacts")
    suspend fun getContacts(): List<UserDto>

    @GET("users/mobile/departments")
    suspend fun getDepartments(): List<DepartmentDto>

    @GET("chat/conversations")
    suspend fun getConversations(): List<ConversationDto>

    @GET("chat/unread-count")
    suspend fun getChatUnread(): ChatUnreadDto

    @GET("chat/search")
    suspend fun searchChat(@Query("q") q: String): ChatSearchDto

    @GET("chat/{userId}")
    suspend fun getChatHistory(
        @Path("userId") userId: String,
        @Query("before") before: String? = null,
        @Query("limit") limit: Int = 40,
    ): List<ChatMessageDto>

    @Multipart
    @POST("chat/upload")
    suspend fun uploadChatFile(@Part file: MultipartBody.Part): ChatUploadDto

    @POST("chat/send")
    suspend fun sendChatMessage(@Body body: SendMessageBody): ChatMessageDto

    @POST("chat/read")
    suspend fun markChatRead(@Body body: MarkReadBody): Map<String, Boolean>

    @PATCH("chat/{id}")
    suspend fun editChatMessage(@Path("id") id: String, @Body body: EditMessageBody): ChatMessageDto

    @DELETE("chat/{id}")
    suspend fun deleteChatMessage(@Path("id") id: String): Map<String, Any>

    @POST("chat/{id}/react")
    suspend fun reactChatMessage(@Path("id") id: String, @Body body: ReactBody): ChatMessageDto

    @POST("chat/{id}/pin")
    suspend fun pinChatMessage(@Path("id") id: String): ChatMessageDto
}

data class ChatMetaDto(
    val fileUrl: String? = null,
    val fileSize: Long? = null,
    val durationSec: Int? = null,
    val waveform: List<Int>? = null,
    val width: Int? = null,
    val height: Int? = null,
    val thumbUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val contactName: String? = null,
    val contactPhone: String? = null,
)

data class ChatMessageDto(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val type: String = "text",
    val body: String? = null,
    val filePath: String? = null,
    val fileName: String? = null,
    val mimeType: String? = null,
    val meta: ChatMetaDto? = null,
    val replyToId: String? = null,
    val replyTo: ChatMessageDto? = null,
    val forwardedFrom: String? = null,
    val reactions: Map<String, String>? = null,
    val status: String = "sent",
    val isRead: Boolean = false,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
    val isPinned: Boolean = false,
    val clientId: String? = null,
    val createdAt: String,
)

data class ChatPeerDto(
    val id: String,
    val fullName: String,
    val position: String? = null,
    val department: String? = null,
    val isOnline: Boolean = false,
    val lastSeenAt: String? = null,
)

data class ConversationDto(
    val peer: ChatPeerDto,
    val lastMessage: ChatMessageDto? = null,
    val unreadCount: Int = 0,
)

data class ChatSearchDto(
    val messages: List<ChatMessageDto> = emptyList(),
    val peers: List<ChatPeerDto> = emptyList(),
)

data class ChatUnreadDto(val count: Int = 0)

data class ChatUploadDto(
    val filePath: String,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val fileUrl: String? = null,
)

data class SendMessageBody(
    val receiverId: String,
    val type: String? = null,
    val body: String? = null,
    val filePath: String? = null,
    val fileName: String? = null,
    val mimeType: String? = null,
    val meta: ChatMetaDto? = null,
    val replyToId: String? = null,
    val forwardedFrom: String? = null,
    val clientId: String? = null,
)

data class MarkReadBody(val peerId: String, val messageIds: List<String>? = null)
data class EditMessageBody(val body: String)
data class ReactBody(val emoji: String?)

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
    val attachments: List<TaskAttachmentDto>? = null,
    val comments: List<TaskCommentDto>? = null,
)

data class TaskCommentDto(
    val id: String,
    val body: String,
    val authorId: String,
    val createdAt: String,
    val author: UserDto? = null,
)

data class TaskAttachmentDto(
    val id: String,
    val fileName: String,
    val filePath: String,
    val mimeType: String,
    val fileSize: Long,
    val uploadedById: String,
    val url: String? = null,
)

data class TaskAssignmentDto(
    val id: String,
    val assigneeId: String,
    val status: String,
    val assignee: UserDto? = null,
    val completedAt: String? = null,
    val acceptedAt: String? = null,
)
