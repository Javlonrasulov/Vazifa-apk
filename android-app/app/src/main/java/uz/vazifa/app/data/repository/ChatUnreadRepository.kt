package uz.vazifa.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.chatUnreadDataStore by preferencesDataStore("vazifa_chat_unread")

@Singleton
class ChatUnreadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val countKey = intPreferencesKey("count")

    val unreadCount: Flow<Int> = context.chatUnreadDataStore.data.map { prefs ->
        prefs[countKey] ?: 0
    }

    suspend fun increment() {
        context.chatUnreadDataStore.edit { prefs ->
            prefs[countKey] = (prefs[countKey] ?: 0) + 1
        }
    }

    suspend fun setCount(count: Int) {
        context.chatUnreadDataStore.edit { prefs ->
            if (count <= 0) prefs.remove(countKey) else prefs[countKey] = count
        }
    }

    suspend fun clear() {
        context.chatUnreadDataStore.edit { prefs ->
            prefs.remove(countKey)
        }
    }
}
