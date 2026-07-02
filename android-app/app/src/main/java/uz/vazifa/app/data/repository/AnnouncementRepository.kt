package uz.vazifa.app.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import uz.vazifa.app.data.remote.AnnouncementDto
import uz.vazifa.app.data.remote.AnnouncementRecipientDto
import uz.vazifa.app.data.remote.ApiClient
import uz.vazifa.app.data.remote.CreateAnnouncementRequest
import uz.vazifa.app.data.remote.UserDto
import uz.vazifa.app.domain.model.Announcement
import uz.vazifa.app.domain.model.AnnouncementAttachment
import uz.vazifa.app.domain.model.AnnouncementRecipient
import uz.vazifa.app.domain.model.User
import uz.vazifa.app.util.MediaUrl
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnnouncementRepository @Inject constructor(
    private val api: ApiClient,
    @ApplicationContext private val context: Context,
) {
    suspend fun getSent(): List<Announcement> =
        api.api.getSentAnnouncements().map { it.toDomain() }

    suspend fun getReceived(): List<Announcement> =
        api.api.getReceivedAnnouncements().map { it.toDomainReceived() }

    suspend fun getById(id: String): Announcement =
        api.api.getAnnouncement(id).toDomain()

    suspend fun create(
        title: String,
        description: String?,
        deadlineAt: String,
        reminderIntervalMinutes: Int,
        recipientIds: List<String>,
    ): Announcement = api.api.createAnnouncement(
        CreateAnnouncementRequest(title, description, deadlineAt, reminderIntervalMinutes, recipientIds),
    ).toDomain()

    suspend fun acknowledge(id: String) {
        api.api.acknowledgeAnnouncement(id)
    }

    suspend fun uploadVoice(announcementId: String, file: File) {
        val part = MultipartBody.Part.createFormData(
            "file",
            file.name,
            file.asRequestBody("audio/mp4".toMediaTypeOrNull()),
        )
        try {
            api.api.uploadAnnouncementAttachment(announcementId, part)
        } finally {
            if (file.exists()) file.delete()
        }
    }

    private fun AnnouncementDto.toDomain(): Announcement = Announcement(
        id = id,
        title = title,
        description = description,
        deadlineAt = deadlineAt,
        reminderIntervalMinutes = reminderIntervalMinutes,
        status = status,
        createdById = createdById,
        createdBy = createdBy?.toDomain(),
        recipients = recipients.map { it.toDomain() },
        attachments = attachments.map { a ->
            AnnouncementAttachment(
                id = a.id,
                fileName = a.fileName,
                mimeType = a.mimeType,
                url = MediaUrl.resolve(a.url.orEmpty()),
            )
        },
    )

    private fun AnnouncementDto.toDomainReceived(): Announcement =
        toDomain().copy(myAcknowledgedAt = acknowledgedAt)

    private fun AnnouncementRecipientDto.toDomain() = AnnouncementRecipient(
        id = id,
        recipientId = recipientId,
        acknowledgedAt = acknowledgedAt,
        recipient = recipient?.toDomain(),
    )

    private fun UserDto.toDomain() = User(
        id = id,
        login = login,
        fullName = fullName,
        role = role,
        position = position,
        department = department,
        phone = phone,
        notificationsEnabled = notificationsEnabled,
        canAssignTasks = canAssignTasks,
        isOnline = isOnline,
        lastSeenAt = lastSeenAt,
        avatarUrl = MediaUrl.resolve(avatarUrl.orEmpty()).takeIf { it.isNotBlank() },
    )
}
