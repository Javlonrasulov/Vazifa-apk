package uz.vazifa.app.util

import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

object TaskDeadlineCountdown {
    private val zone = ZoneId.of("Asia/Tashkent")
    private val displayFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")

    data class Remaining(
        val days: Long,
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

    fun formatDisplay(raw: String): String {
        val instant = parseDeadline(raw)
            ?: return raw.take(16).replace('T', ' ')
        return displayFormatter.withZone(zone).format(instant)
    }

    fun remaining(deadline: Instant, now: Instant = Instant.now()): Remaining {
        val totalMinutes = Duration.between(now, deadline).toMinutes()
        val isOverdue = totalMinutes < 0
        val absMinutes = abs(totalMinutes)
        val days = absMinutes / (24 * 60)
        val hours = (absMinutes % (24 * 60)) / 60
        val minutes = absMinutes % 60
        return Remaining(
            days = days,
            hours = hours,
            minutes = minutes,
            isOverdue = isOverdue,
        )
    }

    fun durationBetween(startRaw: String, endRaw: String): Remaining? {
        val start = parseDeadline(startRaw) ?: return null
        val end = parseDeadline(endRaw) ?: return null
        val totalMinutes = Duration.between(start, end).toMinutes().coerceAtLeast(0)
        val days = totalMinutes / (24 * 60)
        val hours = (totalMinutes % (24 * 60)) / 60
        val minutes = totalMinutes % 60
        return Remaining(days = days, hours = hours, minutes = minutes, isOverdue = false)
    }
}
