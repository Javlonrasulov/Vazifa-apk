package uz.vazifa.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import uz.vazifa.app.domain.model.InboxNotification
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.inboxDataStore by preferencesDataStore("vazifa_notification_inbox")

@Singleton
class NotificationInboxRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val gson = Gson()
    private val inboxKey = stringPreferencesKey("items")
    private val listType = object : TypeToken<List<InboxNotification>>() {}.type

    val items: Flow<List<InboxNotification>> = context.inboxDataStore.data.map { prefs ->
        parseItems(prefs[inboxKey])
    }

    val unreadCount: Flow<Int> = items.map { it.size }

    suspend fun add(
        taskId: String?,
        title: String,
        body: String,
        type: String?,
    ) {
        context.inboxDataStore.edit { prefs ->
            val current = parseItems(prefs[inboxKey]).toMutableList()
            current.add(
                0,
                InboxNotification(
                    id = UUID.randomUUID().toString(),
                    taskId = taskId,
                    title = title,
                    body = body,
                    type = type,
                    receivedAt = System.currentTimeMillis(),
                ),
            )
            prefs[inboxKey] = gson.toJson(current)
        }
    }

    suspend fun takeAll(): List<InboxNotification> {
        val current = items.first()
        clearAll()
        return current
    }

    suspend fun clearAll() {
        context.inboxDataStore.edit { prefs ->
            prefs.remove(inboxKey)
        }
    }

    private fun parseItems(json: String?): List<InboxNotification> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching { gson.fromJson<List<InboxNotification>>(json, listType) }.getOrDefault(emptyList())
    }
}
