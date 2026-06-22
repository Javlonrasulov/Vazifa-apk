package uz.vazifa.app.util

import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

object TaskDeadlineCountdown {
    private val zone = ZoneId.of("Asia/Tashkent")

    data class Remaining(
        val hours: Long,
        val minutes: Long,
        val isOverdue: Boolean,
    )

    fun parseDeadline(raw: String): Instant? = runCatching {
        when {
            raw.contains('+') || raw.endsWith('Z') -> Instant.parse(raw)
            raw.length >= 19 -> LocalDateTime.parse(
                raw.take(19),
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            ).atZone(zone).toInstant()
            raw.length >= 16 -> LocalDateTime.parse(
                raw.take(16),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
            ).atZone(zone).toInstant()
            else -> null
        }
    }.getOrNull()

    fun remaining(deadline: Instant, now: Instant = Instant.now()): Remaining {
        val totalMinutes = Duration.between(now, deadline).toMinutes()
        val isOverdue = totalMinutes < 0
        val absMinutes = abs(totalMinutes)
        return Remaining(
            hours = absMinutes / 60,
            minutes = absMinutes % 60,
            isOverdue = isOverdue,
        )
    }
}
