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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import uz.vazifa.app.data.remote.ApiService
import uz.vazifa.app.data.remote.ContactAliasBody
import javax.inject.Inject
import javax.inject.Singleton

private val Context.contactAliasDataStore by preferencesDataStore("vazifa_contact_aliases")

/**
 * Kontakt aliaslari: serverda saqlanadi, lokal cache tezlik uchun.
 * Har foydalanuvchi faqat o'z aliaslarini ko'radi.
 */
@Singleton
class ContactAliasRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: ApiService,
) {
    private val gson = Gson()
    private val mapType = object : TypeToken<Map<String, String>>() {}.type
    private val mutex = Mutex()

    private val _aliases = MutableStateFlow<Map<String, String>>(emptyMap())
    val aliases: StateFlow<Map<String, String>> = _aliases.asStateFlow()

    private var ownerId: String = ""

    private fun keyFor(userId: String) = stringPreferencesKey("aliases_$userId")

    suspend fun load(userId: String) {
        if (userId.isBlank()) return
        ownerId = userId
        mutex.withLock {
            val prefs = context.contactAliasDataStore.data.first()
            val raw = prefs[keyFor(userId)]
            _aliases.value = raw?.let {
                runCatching { gson.fromJson<Map<String, String>>(it, mapType) }.getOrNull()
            } ?: emptyMap()
        }
        syncFromServer()
    }

    suspend fun syncFromServer() {
        if (ownerId.isBlank()) return
        runCatching {
            val remote = api.getChatAliases().aliases
            val local = _aliases.value
            if (remote.isEmpty() && local.isNotEmpty()) {
                local.forEach { (peerId, alias) ->
                    runCatching { api.setChatAlias(peerId, ContactAliasBody(alias = alias)) }
                }
            }
            val merged = api.getChatAliases().aliases
            mutex.withLock {
                _aliases.value = merged
                persistLocked()
            }
        }
    }

    fun aliasFor(peerId: String): String? = _aliases.value[peerId]?.takeIf { it.isNotBlank() }

    suspend fun resolveDisplayName(peerId: String, fallback: String, ownerIdHint: String? = null): String {
        val uid = ownerIdHint?.takeIf { it.isNotBlank() } ?: ownerId
        if (uid.isNotBlank() && ownerId != uid) ownerId = uid
        ensureLoadedFromDisk()
        return aliasFor(peerId) ?: fallback
    }

    private suspend fun ensureLoadedFromDisk() {
        if (_aliases.value.isNotEmpty() || ownerId.isBlank()) return
        mutex.withLock {
            if (_aliases.value.isNotEmpty() || ownerId.isBlank()) return
            val prefs = context.contactAliasDataStore.data.first()
            val raw = prefs[keyFor(ownerId)]
            _aliases.value = raw?.let {
                runCatching { gson.fromJson<Map<String, String>>(it, mapType) }.getOrNull()
            } ?: emptyMap()
        }
    }

    suspend fun setAlias(peerId: String, alias: String?) {
        if (ownerId.isBlank() || peerId.isBlank()) return
        val clean = alias?.trim()?.takeIf { it.isNotEmpty() }
        mutex.withLock {
            val updated = _aliases.value.toMutableMap()
            if (clean == null) updated.remove(peerId) else updated[peerId] = clean
            _aliases.value = updated
            persistLocked()
        }
        runCatching {
            val remote = api.setChatAlias(peerId, ContactAliasBody(alias = clean)).aliases
            mutex.withLock {
                _aliases.value = remote
                persistLocked()
            }
        }.onFailure {
            syncFromServer()
        }
    }

    private suspend fun persistLocked() {
        if (ownerId.isBlank()) return
        context.contactAliasDataStore.edit { prefs ->
            prefs[keyFor(ownerId)] = gson.toJson(_aliases.value)
        }
    }
}
