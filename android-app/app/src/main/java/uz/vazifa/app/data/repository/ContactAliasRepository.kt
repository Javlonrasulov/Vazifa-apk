package uz.vazifa.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.contactAliasDataStore by preferencesDataStore("vazifa_contact_aliases")

/**
 * Foydalanuvchi kontakt nomlarini lokal o'zgartira oladi.
 * Alias faqat shu qurilmada va shu akkauntda ko'rinadi (Telegram logikasi).
 */
@Singleton
class ContactAliasRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val gson = Gson()
    private val mapType = object : TypeToken<Map<String, String>>() {}.type

    private val _aliases = MutableStateFlow<Map<String, String>>(emptyMap())
    val aliases: StateFlow<Map<String, String>> = _aliases

    private var ownerId: String = ""

    private fun keyFor(userId: String) = stringPreferencesKey("aliases_$userId")

    suspend fun load(userId: String) {
        if (userId.isBlank()) return
        ownerId = userId
        val prefs = context.contactAliasDataStore.data.first()
        val raw = prefs[keyFor(userId)]
        _aliases.value = raw?.let {
            runCatching { gson.fromJson<Map<String, String>>(it, mapType) }.getOrNull()
        } ?: emptyMap()
    }

    fun aliasFor(peerId: String): String? = _aliases.value[peerId]?.takeIf { it.isNotBlank() }

    suspend fun setAlias(userId: String, peerId: String, alias: String?) {
        if (userId.isBlank() || peerId.isBlank()) return
        ownerId = userId
        val updated = _aliases.value.toMutableMap()
        val clean = alias?.trim().orEmpty()
        if (clean.isEmpty()) updated.remove(peerId) else updated[peerId] = clean
        _aliases.value = updated
        context.contactAliasDataStore.edit { prefs ->
            prefs[keyFor(userId)] = gson.toJson(updated)
        }
    }
}
