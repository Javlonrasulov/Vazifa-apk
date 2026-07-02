package uz.vazifa.app.notifications

import android.content.Context

/** Server qayta yuborgan pushlarni ikki marta ko'rsatmaslik uchun. */
object PushDedup {
    private const val PREFS = "vazifa_push_dedup"
    private const val KEY_IDS = "ids"
    private const val MAX = 300

    fun isDuplicate(context: Context, outboxId: String): Boolean {
        if (outboxId.isBlank()) return false
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val ids = prefs.getStringSet(KEY_IDS, emptySet()) ?: emptySet()
        return outboxId in ids
    }

    fun remember(context: Context, outboxId: String) {
        if (outboxId.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = (prefs.getStringSet(KEY_IDS, emptySet()) ?: emptySet()).toMutableList()
        current.remove(outboxId)
        current.add(0, outboxId)
        while (current.size > MAX) current.removeAt(current.lastIndex)
        prefs.edit().putStringSet(KEY_IDS, current.toSet()).apply()
    }
}
