package uz.vazifa.app.util

import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object LastSeenFormatter {
    private val zone = ZoneId.of("Asia/Tashkent")
    private val displayFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    fun parseInstant(raw: String): Instant? {
        return runCatching { Instant.parse(raw) }.getOrNull()
            ?: runCatching {
                LocalDateTime.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    .atZone(zone)
                    .toInstant()
            }.getOrNull()
            ?: runCatching {
                ZonedDateTime.parse(raw).toInstant()
            }.getOrNull()
    }

    fun formatRelative(
        raw: String?,
        justNow: String,
        minutesAgo: (Long) -> String,
        hoursAgo: (Long) -> String,
        yesterdayAt: (String) -> String,
        never: String,
    ): String {
        if (raw.isNullOrBlank()) return never
        val instant = parseInstant(raw) ?: return never
        val seen = instant.atZone(zone)
        val now = ZonedDateTime.now(zone)
        val minutes = Duration.between(seen, now).toMinutes().coerceAtLeast(0)
        return when {
            minutes < 1 -> justNow
            minutes < 60 -> minutesAgo(minutes)
            minutes < 24 * 60 -> hoursAgo(minutes / 60)
            minutes < 48 * 60 -> yesterdayAt(seen.format(DateTimeFormatter.ofPattern("HH:mm")))
            else -> seen.format(displayFormatter)
        }
    }
}
