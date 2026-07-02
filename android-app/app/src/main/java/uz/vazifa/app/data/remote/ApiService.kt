package uz.vazifa.app.data.remote

import com.google.gson.annotations.JsonAdapter
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
    val avatarUrl: String? = null,
    val role: String,
    val canAssignTasks: Boolean = false,
    val allowScreenshot: Boolean? = null,
    val position: String? = null,
    val department: String? = null,
    val visibleDepartments: List<String>? = null,
    val phone: String? = null,
    val notificationsEnabled: Boolean = true,
    val isOnline: Boolean = false,
    val lastSeenAt: String? = null,
    @JsonAdapter(RestDaysDeserializer::class)
    val restDays: List<Int>? = null,
)

fun UserDto.canAssignTasksInApp(): Boolean =
    role.equals("director", ignoreCase = true) || role.equals("employee", ignoreCase = true)

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
data class UpdateAnnouncementRequest(
    val title: String,
    val description: String?,
    val deadlineAt: String,
    val reminderIntervalMinutes: Int,
)

data class CreateAnnouncementRequest(
    val title: String,
    val description: String?,
    val deadlineAt: String,
    val reminderIntervalMinutes: Int,
    val recipientIds: List<String>,
)

data class AnnouncementAckResponse(
    val acknowledged: Boolean,
    val acknowledgedAt: String?,
    val viewedAt: String? = null,
)

data class AnnouncementViewResponse(
    val viewed: Boolean,
    val viewedAt: String?,
)

data class AnnouncementRecipientDto(
    val id: String,
    val recipientId: String,
    val acknowledgedAt: String? = null,
    val viewedAt: String? = null,
    val recipient: UserDto? = null,
)

data class AnnouncementAttachmentDto(
    val id: String,
    val fileName: String,
    val mimeType: String,
    val url: String? = null,
)

data class AnnouncementDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val deadlineAt: String,
    val reminderIntervalMinutes: Int,
    val status: String,
    val createdById: String,
    val createdBy: UserDto? = null,
    val recipients: List<AnnouncementRecipientDto> = emptyList(),
    val attachments: List<AnnouncementAttachmentDto> = emptyList(),
    val acknowledgedAt: String? = null,
)

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

    @Multipart
    @POST("users/mobile/avatar")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): UserDto

    @DELETE("users/mobile/avatar")
    suspend fun deleteAvatar(): UserDto

    @GET("users/mobile/departments")
    suspend fun getDepartments(): List<DepartmentDto>

    @GET("chat/conversations")
    suspend fun getConversations(): List<ConversationDto>

    @GET("chat/unread-count")
    suspend fun getChatUnread(): ChatUnreadDto

    @GET("chat/search")
    suspend fun searchChat(@Query("q") q: String): ChatSearchDto

    @GET("chat/aliases")
    suspend fun getChatAliases(): ChatAliasesDto

    @PATCH("chat/aliases/{peerId}")
    suspend fun setChatAlias(@Path("peerId") peerId: String, @Body body: ContactAliasBody): ChatAliasesDto

    @GET("chat/{userId}")
    suspend fun getChatHistory(
        @Path("userId") userId: String,
        @Query("before") before: String? = null,
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

    @GET("rooms")
    suspend fun getRooms(): List<RoomDto>

    @POST("rooms")
    suspend fun createRoom(@Body body: CreateRoomBody): RoomDto

    @GET("rooms/{id}")
    suspend fun getRoom(@Path("id") id: String): RoomDto

    @PATCH("rooms/{id}")
    suspend fun updateRoom(@Path("id") id: String, @Body body: UpdateRoomBody): RoomDto

    @DELETE("rooms/{id}")
    suspend fun deleteRoom(@Path("id") id: String): Map<String, Any>

    @GET("rooms/{id}/members")
    suspend fun getRoomMembers(@Path("id") id: String): List<RoomMemberDto>

    @POST("rooms/{id}/members")
    suspend fun addRoomMembers(@Path("id") id: String, @Body body: AddMembersBody): Map<String, Any>

    @DELETE("rooms/{id}/members/{userId}")
    suspend fun removeRoomMember(@Path("id") id: String, @Path("userId") userId: String): Map<String, Any>

    @GET("rooms/{id}/messages")
    suspend fun getRoomHistory(
        @Path("id") id: String,
        @Query("before") before: String? = null,
        @Query("limit") limit: Int = 40,
    ): List<RoomMessageDto>

    @POST("rooms/{id}/messages")
    suspend fun sendRoomMessage(@Path("id") id: String, @Body body: SendRoomMessageBody): RoomMessageDto

    @POST("rooms/{id}/read")
    suspend fun markRoomRead(@Path("id") id: String): Map<String, Boolean>

    @PATCH("rooms/messages/{msgId}")
    suspend fun editRoomMessage(@Path("msgId") msgId: String, @Body body: EditMessageBody): RoomMessageDto

    @DELETE("rooms/messages/{msgId}")
    suspend fun deleteRoomMessage(@Path("msgId") msgId: String): Map<String, Any>

    @POST("rooms/messages/{msgId}/react")
    suspend fun reactRoomMessage(@Path("msgId") msgId: String, @Body body: ReactBody): RoomMessageDto

    @POST("rooms/messages/{msgId}/pin")
    suspend fun pinRoomMessage(@Path("msgId") msgId: String): RoomMessageDto

    @GET("announcements/sent")
    suspend fun getSentAnnouncements(): List<AnnouncementDto>

    @GET("announcements/received")
    suspend fun getReceivedAnnouncements(): List<AnnouncementDto>

    @GET("announcements/{id}")
    suspend fun getAnnouncement(@Path("id") id: String): AnnouncementDto

    @POST("announcements")
    suspend fun createAnnouncement(@Body body: CreateAnnouncementRequest): AnnouncementDto

    @PATCH("announcements/{id}")
    suspend fun updateAnnouncement(
        @Path("id") id: String,
        @Body body: UpdateAnnouncementRequest,
    ): AnnouncementDto

    @DELETE("announcements/{id}")
    suspend fun deleteAnnouncement(@Path("id") id: String)

    @POST("announcements/{id}/acknowledge")
    suspend fun acknowledgeAnnouncement(@Path("id") id: String): AnnouncementAckResponse

    @POST("announcements/{id}/view")
    suspend fun markAnnouncementViewed(@Path("id") id: String): AnnouncementViewResponse

    @POST("announcements/{id}/cancel")
    suspend fun cancelAnnouncement(@Path("id") id: String): AnnouncementDto

    @Multipart
    @POST("announcements/{id}/attachments")
    suspend fun uploadAnnouncementAttachment(
        @Path("id") id: String,
        @Part file: MultipartBody.Part,
    ): AnnouncementAttachmentDto
}

data class ChatMetaDto(
    val fileUrl: String? = null,
    val fileSize: Double? = null,
    val durationSec: Double? = null,
    val waveform: List<Double>? = null,
    val width: Double? = null,
    val height: Double? = null,
    val thumbUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val contactName: String? = null,
    val contactPhone: String? = null,
    val isRoundVideo: Boolean? = null,
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
    val createdAt: String? = null,
)

data class ChatPeerDto(
    val id: String,
    val fullName: String,
    val avatarUrl: String? = null,
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

data class ChatAliasesDto(val aliases: Map<String, String> = emptyMap())

data class ContactAliasBody(val alias: String? = null)

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

data class RoomMessageDto(
    val id: String,
    val roomId: String,
    val senderId: String,
    val sender: ChatRoomSenderDto? = null,
    val type: String = "text",
    val body: String? = null,
    val filePath: String? = null,
    val fileName: String? = null,
    val mimeType: String? = null,
    val meta: ChatMetaDto? = null,
    val replyToId: String? = null,
    val replyTo: RoomMessageDto? = null,
    val forwardedFrom: String? = null,
    val reactions: Map<String, String>? = null,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
    val isPinned: Boolean = false,
    val clientId: String? = null,
    val createdAt: String? = null,
)

data class ChatRoomSenderDto(
    val id: String,
    val fullName: String? = null,
    val avatarUrl: String? = null,
)

data class RoomDto(
    val id: String,
    val type: String,
    val title: String,
    val description: String? = null,
    val avatarUrl: String? = null,
    val isVerified: Boolean = false,
    val ownerId: String,
    val myRole: String = "member",
    val memberCount: Int = 0,
    val muted: Boolean = false,
    val canPost: Boolean = true,
    val lastMessage: RoomMessageDto? = null,
    val unreadCount: Int = 0,
)

data class RoomMemberDto(
    val id: String,
    val fullName: String,
    val avatarUrl: String? = null,
    val position: String? = null,
    val role: String = "member",
)

data class CreateRoomBody(
    val type: String,
    val title: String,
    val description: String? = null,
    val avatarUrl: String? = null,
    val memberIds: List<String>? = null,
)

data class UpdateRoomBody(
    val title: String? = null,
    val description: String? = null,
    val avatarUrl: String? = null,
)

data class AddMembersBody(val memberIds: List<String>)

data class SendRoomMessageBody(
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
