package uz.vazifa.app.domain.model

data class AnnouncementRecipient(
    val id: String,
    val recipientId: String,
    val acknowledgedAt: String? = null,
    val viewedAt: String? = null,
    val recipient: User? = null,
)

data class AnnouncementAttachment(
    val id: String,
    val fileName: String,
    val mimeType: String,
    val url: String,
)

data class Announcement(
    val id: String,
    val title: String,
    val description: String? = null,
    val deadlineAt: String,
    val reminderIntervalMinutes: Int,
    val status: String,
    val createdById: String,
    val createdBy: User? = null,
    val recipients: List<AnnouncementRecipient> = emptyList(),
    val attachments: List<AnnouncementAttachment> = emptyList(),
    val myAcknowledgedAt: String? = null,
)

fun Announcement.isCreator(userId: String?): Boolean =
    userId != null && createdById == userId

fun Announcement.isAcknowledgedBy(userId: String?): Boolean {
    if (userId == null) return false
    return recipients.find { it.recipientId == userId }?.acknowledgedAt != null
        || myAcknowledgedAt != null
}

fun Announcement.isViewedBy(userId: String?): Boolean {
    if (userId == null) return false
    return recipients.find { it.recipientId == userId }?.viewedAt != null
}

fun Announcement.acknowledgedCount(): Int =
    recipients.count { it.acknowledgedAt != null }

fun Announcement.pendingCount(): Int =
    recipients.count { it.acknowledgedAt == null }

fun Announcement.viewedCount(): Int =
    recipients.count { it.viewedAt != null }

fun Announcement.notViewedCount(): Int =
    recipients.count { it.viewedAt == null }
