package uz.vazifa.app.util

object UzPhoneFormatter {
    const val PREFIX = "+998 "

    /** Milliy qism (998 dan keyingi 9 ta raqam), +998 prefiksini alohida olib tashlaydi. */
    fun extractNationalDigits(raw: String): String {
        var s = raw.trim()
        when {
            s.startsWith("+998") -> s = s.removePrefix("+998").trimStart()
            s.startsWith("998") && s.length > 3 -> s = s.removePrefix("998").trimStart()
        }
        return s.filter { it.isDigit() }.take(9)
    }

    fun format(nationalDigits: String): String {
        val d = nationalDigits.take(9)
        if (d.isEmpty()) return PREFIX

        val b = StringBuilder("+998")
        if (d.isNotEmpty()) b.append(" ${d.take(2)}")
        if (d.length > 2) b.append(" ${d.substring(2, minOf(5, d.length))}")
        if (d.length > 5) b.append(" ${d.substring(5, minOf(7, d.length))}")
        if (d.length > 7) b.append(" ${d.substring(7, minOf(9, d.length))}")
        return b.toString()
    }

    /** API ga yuborish uchun to'liq formatlangan telefon. */
    fun formattedForApi(nationalDigits: String): String = format(nationalDigits)
}
