package uz.vazifa.app.domain.model

import java.time.LocalDate
import java.time.ZoneId

data class RestDayWarning(
    val fullName: String,
    val isToday: Boolean,
)

/** Dam olish kunlari: 0=yakshanba ... 6=shanba (admin panel bilan bir xil). */
object RestDayHelper {
    private val zone = ZoneId.of("Asia/Tashkent")

    fun todayIndex(): Int = LocalDate.now(zone).dayOfWeek.value % 7

    fun tomorrowIndex(): Int = (todayIndex() + 1) % 7

    fun restingAssigneesTodayOrTomorrow(assignees: List<User>): List<RestDayWarning> {
        val today = todayIndex()
        val tomorrow = tomorrowIndex()
        return assignees.mapNotNull { user ->
            val days = user.restDays?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            when {
                today in days -> RestDayWarning(user.fullName, isToday = true)
                tomorrow in days -> RestDayWarning(user.fullName, isToday = false)
                else -> null
            }
        }
    }
}
