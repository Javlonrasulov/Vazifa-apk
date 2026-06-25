package uz.vazifa.app.data.repository

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import uz.vazifa.app.data.remote.ApiClient
import uz.vazifa.app.data.remote.CommentRequest
import uz.vazifa.app.data.remote.CreateTaskRequest
import uz.vazifa.app.data.remote.TaskAssignmentDto
import uz.vazifa.app.data.remote.TaskAttachmentDto
import uz.vazifa.app.data.remote.TaskCommentDto
import uz.vazifa.app.data.remote.TaskDto
import uz.vazifa.app.data.remote.UpdateStatusRequest
import uz.vazifa.app.data.remote.UpdateTaskRequest
import uz.vazifa.app.data.remote.UserDto
import uz.vazifa.app.domain.model.DashboardStats
import uz.vazifa.app.domain.model.Task
import uz.vazifa.app.domain.model.TaskAssignment
import uz.vazifa.app.domain.model.TaskAttachment
import uz.vazifa.app.domain.model.TaskComment
import uz.vazifa.app.domain.model.User
import uz.vazifa.app.util.ImageCompress
import uz.vazifa.app.util.MediaUrl
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
  private val api: ApiClient,
  @ApplicationContext private val context: Context,
) {

    suspend fun getTasks(): List<Task> = api.api.getTasks()
        .map { it.toDomain() }
        .distinctBy { it.id }

    suspend fun getDepartmentTasks(): List<Task> = api.api.getDepartmentTasks()
        .map { it.toDomain() }
        .distinctBy { it.id }

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

    suspend fun updateTask(
        id: String,
        title: String,
        description: String?,
        deadlineAt: String,
    ): Task = api.api.updateTask(
        id,
        UpdateTaskRequest(title = title, description = description, deadlineAt = deadlineAt),
    ).toDomain()

    suspend fun cancelTask(id: String) {
        api.api.cancelTask(id)
    }

    suspend fun updateStatus(taskId: String, assignmentId: String, status: String) {
        api.api.updateAssignmentStatus(taskId, assignmentId, UpdateStatusRequest(status))
    }

    suspend fun addComment(taskId: String, body: String) {
        api.api.addComment(taskId, CommentRequest(body))
    }

    suspend fun uploadFileAttachment(
        taskId: String,
        file: java.io.File,
        mimeType: String,
        fileName: String = file.name,
    ) {
        val part = MultipartBody.Part.createFormData(
            "file",
            fileName,
            file.asRequestBody(mimeType.toMediaTypeOrNull()),
        )
        try {
            api.api.uploadAttachment(taskId, part)
        } finally {
            if (file.exists()) file.delete()
        }
    }

    suspend fun uploadAttachment(taskId: String, imageUri: Uri) {
        val file = ImageCompress.compressToFile(context, imageUri)
        uploadFileAttachment(taskId, file, "image/jpeg", file.name)
    }

    suspend fun getContacts(): List<User> = api.api.getContacts().map { it.toUser() }

    suspend fun getDepartments(): List<String> = api.api.getDepartments()

    private fun UserDto.toUser() = User(
        id, login, fullName, role, position, department, phone, notificationsEnabled, canAssignTasks,
        isOnline, lastSeenAt,
    )

    private fun TaskDto.toDomain() = Task(
        id, title, description, priority, status, startAt, deadlineAt, createdById,
        assignments?.map { it.toDomain() } ?: emptyList(),
        createdBy?.toUser(),
        attachments?.map { it.toDomain() } ?: emptyList(),
        comments?.map { it.toDomain() } ?: emptyList(),
    )

    private fun TaskAssignmentDto.toDomain() = TaskAssignment(
        id, assigneeId, status,
        assignee?.toUser(),
        completedAt, acceptedAt,
    )

    private fun TaskAttachmentDto.toDomain() = TaskAttachment(
        id, fileName, filePath, mimeType, fileSize, uploadedById,
        MediaUrl.attachmentApiUrl(id),
    )

    private fun TaskCommentDto.toDomain() = TaskComment(
        id, body, authorId, createdAt, author?.toUser(),
    )
}
