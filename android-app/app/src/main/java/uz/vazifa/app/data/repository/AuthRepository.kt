package uz.vazifa.app.data.repository

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import retrofit2.HttpException
import uz.vazifa.app.data.remote.ApiClient
import uz.vazifa.app.data.remote.ChangePasswordRequest
import uz.vazifa.app.data.remote.FcmRequest
import uz.vazifa.app.data.remote.LoginRequest
import uz.vazifa.app.data.remote.TokenStore
import uz.vazifa.app.data.remote.UserDto
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("vazifa_prefs")

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: ApiClient,
    private val tokenStore: TokenStore,
) {
    private val gson = Gson()
    private val refreshMutex = Mutex()

    private val KEY_ACCESS = stringPreferencesKey("access_token")
    private val KEY_REFRESH = stringPreferencesKey("refresh_token")
    private val KEY_USER = stringPreferencesKey("user_json")
    private val KEY_NOTIF_REGISTERED = booleanPreferencesKey("notif_registered")

    fun areNotificationsEnabled(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    private suspend fun isNotifRegistered(): Boolean =
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

    private suspend fun loadPrefs(): Preferences = context.dataStore.data.first()

    private suspend fun loadTokensFromPrefs(prefs: Preferences) {
        tokenStore.accessToken = prefs[KEY_ACCESS]
        tokenStore.refreshToken = prefs[KEY_REFRESH]
    }

    private suspend fun saveTokens(access: String, refresh: String) {
        tokenStore.accessToken = access
        tokenStore.refreshToken = refresh
        context.dataStore.edit {
            it[KEY_ACCESS] = access
            it[KEY_REFRESH] = refresh
        }
    }

    private suspend fun saveUserJson(user: UserDto) {
        context.dataStore.edit { it[KEY_USER] = gson.toJson(user) }
    }

    private fun loadCachedUser(prefs: Preferences): UserDto? {
        val json = prefs[KEY_USER] ?: return null
        return try {
            gson.fromJson(json, UserDto::class.java)
        } catch (_: Exception) {
            null
        }
    }

    /** Bildirishnomalar yoqilgan va serverga token yuborilgan bo'lsa true. */
    suspend fun registerPushToken(): Boolean {
        if (!areNotificationsEnabled()) {
            setNotifRegistered(false)
            return false
        }
        val token = withTimeoutOrNull(15_000) { fetchFcmToken() }
        if (token.isNullOrBlank()) {
            setNotifRegistered(false)
            return false
        }
        return try {
            updateFcm(token, true)
            setNotifRegistered(true)
            true
        } catch (_: Exception) {
            setNotifRegistered(false)
            false
        }
    }

    suspend fun syncFcmTokenIfPossible(token: String? = null) {
        if (!areNotificationsEnabled()) return
        val prefs = loadPrefs()
        if (prefs[KEY_ACCESS].isNullOrBlank()) return
        loadTokensFromPrefs(prefs)
        val resolved = token ?: withTimeoutOrNull(15_000) { fetchFcmToken() } ?: return
        runCatching { updateFcm(resolved, true) }
    }

    /** Bildirishnomalar yoqilgan va internet orqali token yangilangan bo'lsa asosiy ekranga kirish mumkin. */
    suspend fun shouldSkipNotifGate(): Boolean {
        if (!areNotificationsEnabled()) {
            setNotifRegistered(false)
            return false
        }
        return registerPushToken()
    }

    suspend fun login(login: String, password: String, deviceId: String): UserDto {
        val res = api.api.login(LoginRequest(login, password, deviceId))
        saveTokens(res.accessToken, res.refreshToken)
        saveUserJson(res.user)
        return res.user
    }

    suspend fun refreshAndPersist(): Boolean = refreshMutex.withLock {
        val refresh = tokenStore.refreshToken ?: loadPrefs()[KEY_REFRESH]
        if (refresh.isNullOrBlank()) return false
        return try {
            val res = api.api.refresh(mapOf("refreshToken" to refresh))
            val newAccess = res["accessToken"] ?: return false
            val newRefresh = res["refreshToken"] ?: return false
            saveTokens(newAccess, newRefresh)
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun restoreSession(): UserDto? {
        val prefs = loadPrefs()
        val access = prefs[KEY_ACCESS]
        if (access.isNullOrBlank()) return null

        loadTokensFromPrefs(prefs)

        return try {
            val user = api.api.me()
            saveUserJson(user)
            user
        } catch (e: HttpException) {
            if (e.code() == 401) {
                if (refreshAndPersist()) {
                    try {
                        val user = api.api.me()
                        saveUserJson(user)
                        return user
                    } catch (retry: HttpException) {
                        if (retry.code() == 401) {
                            logout()
                            return null
                        }
                    } catch (_: IOException) {
                        return loadCachedUser(prefs)
                    }
                } else {
                    logout()
                    return null
                }
            }
            loadCachedUser(prefs)
        } catch (_: IOException) {
            loadCachedUser(prefs)
        } catch (_: Exception) {
            loadCachedUser(prefs)
        }
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

    suspend fun currentUser(): UserDto? {
        val prefs = loadPrefs()
        return try {
            if (tokenStore.accessToken.isNullOrBlank() && prefs[KEY_ACCESS].isNullOrBlank()) {
                loadCachedUser(prefs)
            } else {
                loadTokensFromPrefs(prefs)
                api.api.me().also { saveUserJson(it) }
            }
        } catch (_: Exception) {
            loadCachedUser(prefs)
        }
    }
}
