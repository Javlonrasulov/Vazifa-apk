package uz.vazifa.app.data.repository

import android.content.Context
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import uz.vazifa.app.data.remote.ApiClient
import uz.vazifa.app.data.remote.ChangePasswordRequest
import uz.vazifa.app.data.remote.FcmRequest
import uz.vazifa.app.data.remote.LoginRequest
import uz.vazifa.app.data.remote.TokenStore
import uz.vazifa.app.data.remote.UserDto
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("vazifa_prefs")

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: ApiClient,
    private val tokenStore: TokenStore,
) {
    private val KEY_ACCESS = stringPreferencesKey("access_token")
    private val KEY_REFRESH = stringPreferencesKey("refresh_token")
    private val KEY_USER = stringPreferencesKey("user_json")
    private val KEY_NOTIF_REGISTERED = booleanPreferencesKey("notif_registered")

    fun areNotificationsEnabled(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    suspend fun isNotifRegistered(): Boolean =
        context.dataStore.data.first()[KEY_NOTIF_REGISTERED] == true

    private suspend fun setNotifRegistered(registered: Boolean) {
        context.dataStore.edit {
            if (registered) it[KEY_NOTIF_REGISTERED] = true else it.remove(KEY_NOTIF_REGISTERED)
        }
    }

    private suspend fun fetchFcmToken(): String? = suspendCancellableCoroutine { cont ->
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (cont.isActive) cont.resume(if (task.isSuccessful) task.result else null)
        }
    }

    private fun fallbackPushToken(): String {
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        return "local-$deviceId"
    }

    /** Bildirishnomalar yoqilgan bo'lsa FCM yoki zaxira token bilan serverni yangilaydi. */
    suspend fun registerPushToken(): Boolean {
        if (!areNotificationsEnabled()) return false
        val token = withTimeoutOrNull(10_000) { fetchFcmToken() } ?: fallbackPushToken()
        return try {
            updateFcm(token, true)
            setNotifRegistered(true)
            true
        } catch (_: Exception) {
            false
        }
    }

    /** Keyingi ochilishda bildirishnoma ekranini o'tkazib yuborish mumkinmi. */
    suspend fun shouldSkipNotifGate(): Boolean {
        if (isNotifRegistered()) return true
        return areNotificationsEnabled() && registerPushToken()
    }

    suspend fun login(login: String, password: String, deviceId: String): UserDto {
        val res = api.api.login(LoginRequest(login, password, deviceId))
        tokenStore.accessToken = res.accessToken
        tokenStore.refreshToken = res.refreshToken
        context.dataStore.edit {
            it[KEY_ACCESS] = res.accessToken
            it[KEY_REFRESH] = res.refreshToken
            it[KEY_USER] = """{"id":"${res.user.id}","login":"${res.user.login}","fullName":"${res.user.fullName}","role":"${res.user.role}"}"""
        }
        return res.user
    }

    suspend fun restoreSession(): UserDto? {
        val prefs = context.dataStore.data.first()
        val access = prefs[KEY_ACCESS]
        tokenStore.accessToken = access
        tokenStore.refreshToken = prefs[KEY_REFRESH]
        if (access.isNullOrBlank()) return null
        return try { api.api.me() } catch (_: Exception) { null }
    }

    suspend fun logout() {
        tokenStore.accessToken = null
        tokenStore.refreshToken = null
        context.dataStore.edit {
            it.remove(KEY_ACCESS)
            it.remove(KEY_REFRESH)
            it.remove(KEY_USER)
            it.remove(KEY_NOTIF_REGISTERED)
        }
    }

    suspend fun updateFcm(token: String, enabled: Boolean) {
        api.api.updateFcm(FcmRequest(token, enabled))
    }

    suspend fun changePassword(currentPassword: String, newPassword: String) {
        api.api.changePassword(ChangePasswordRequest(currentPassword, newPassword))
    }
}
