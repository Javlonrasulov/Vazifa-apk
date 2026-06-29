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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
import javax.inject.Provider
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("vazifa_prefs")

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: ApiClient,
    private val tokenStore: TokenStore,
    private val appSettings: AppSettingsRepository,
    private val chatRepositoryProvider: Provider<ChatRepository>,
) {
    private val gson = Gson()
    private val refreshMutex = Mutex()
    private val bgScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

    fun hasStoredSession(): Boolean =
        !tokenStore.accessToken.isNullOrBlank()

    suspend fun hasStoredSessionAsync(): Boolean {
        val prefs = loadPrefs()
        return !prefs[KEY_ACCESS].isNullOrBlank()
    }

    /** Serverdan foydalanuvchini oladi; autentifikatsiya xatosi bo'lsa sessiyani tozalaydi. */
    private suspend fun fetchUserFromServer(): UserDto? {
        return try {
            val user = api.api.me()
            saveUserJson(user)
            user
        } catch (e: HttpException) {
            if (e.code() == 401 && refreshAndPersist()) {
                try {
                    val user = api.api.me()
                    saveUserJson(user)
                    return user
                } catch (_: Exception) {
                    logout()
                    return null
                }
            }
            logout()
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

    /** Tokenni fonda ro'yxatdan o'tkazadi — ilovaga kirishni kechiktirmaydi. */
    fun registerPushTokenAsync() {
        bgScope.launch { runCatching { registerPushToken() } }
    }

    /**
     * Bildirishnomalar yoqilgan bo'lsa asosiy ekranga kirishga ruxsat beradi.
     * Token ro'yxatdan o'tkaziladi — muvaffaqiyatsiz bo'lsa ham foydalanuvchi kira oladi.
     */
    suspend fun shouldSkipNotifGate(): Boolean {
        if (!hasStoredSessionAsync()) return false
        if (!areNotificationsEnabled()) {
            setNotifRegistered(false)
            return false
        }
        registerPushToken()
        return true
    }

    suspend fun login(login: String, password: String, deviceId: String, deviceName: String? = null): UserDto {
        val res = api.api.login(LoginRequest(login, password, deviceId, deviceName))
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
            runCatching { chatRepositoryProvider.get().reconnect() }
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Ilova ochilishi: faqat server tasdiqlagan yoki offline rejimda
     * to'liq token + kesh bo'lsa sessiya qabul qilinadi.
     * Vaqt tugasa yoki sessiya yaroqsiz bo'lsa — login ekrani.
     */
    suspend fun restoreSessionForBoot(): UserDto? {
        val prefs = loadPrefs()
        val access = prefs[KEY_ACCESS]
        val refresh = prefs[KEY_REFRESH]
        if (access.isNullOrBlank() || refresh.isNullOrBlank()) {
            if (!access.isNullOrBlank() || prefs[KEY_USER] != null) {
                logout()
            }
            return null
        }

        loadTokensFromPrefs(prefs)

        return try {
            withTimeoutOrNull(5_000) {
                fetchUserFromServer()
            }
        } catch (_: IOException) {
            loadCachedUser(prefs)?.takeIf { it.id.isNotBlank() }
        }
    }

    suspend fun restoreSession(): UserDto? {
        val prefs = loadPrefs()
        val access = prefs[KEY_ACCESS]
        if (access.isNullOrBlank()) {
            context.dataStore.edit { it.remove(KEY_USER) }
            return null
        }

        loadTokensFromPrefs(prefs)

        return try {
            fetchUserFromServer()
        } catch (_: IOException) {
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
        val lang = appSettings.language.first().code
        api.api.updateFcm(FcmRequest(token, enabled, lang))
    }

    /** Til o'zgarganda serverga yangi tilni yuboradi (push xabarlar tili uchun). */
    fun syncLanguageAsync() {
        bgScope.launch { runCatching { syncFcmTokenIfPossible() } }
    }

    suspend fun changePassword(currentPassword: String, newPassword: String) {
        api.api.changePassword(ChangePasswordRequest(currentPassword, newPassword))
    }

    suspend fun uploadAvatar(file: java.io.File): UserDto {
        val body = file.asRequestBody("image/*".toMediaTypeOrNull())
        val part = okhttp3.MultipartBody.Part.createFormData("file", file.name, body)
        val user = api.api.uploadAvatar(part)
        saveUserJson(user)
        return user
    }

    suspend fun deleteAvatar(): UserDto {
        val user = api.api.deleteAvatar()
        saveUserJson(user)
        return user
    }

    suspend fun sendPresenceHeartbeat() {
        api.api.sendPresence()
    }

    suspend fun currentUser(): UserDto? {
        val prefs = loadPrefs()
        val hasTokens = !tokenStore.accessToken.isNullOrBlank() || !prefs[KEY_ACCESS].isNullOrBlank()
        if (!hasTokens) return null

        loadTokensFromPrefs(prefs)
        return try {
            fetchUserFromServer()
        } catch (_: IOException) {
            loadCachedUser(prefs)
        }
    }

    /** Tarmoqsiz tez ID olish (chat ochilishi uchun) — token ham yuklanadi */
    suspend fun cachedUserId(): String? {
        val prefs = loadPrefs()
        loadTokensFromPrefs(prefs)
        return loadCachedUser(prefs)?.id?.takeIf { it.isNotBlank() }
    }

    /** Chat/API chaqiruvidan oldin sessiya va tokenlarni tayyorlaydi */
    suspend fun ensureSessionForApi(): String? {
        val prefs = loadPrefs()
        loadTokensFromPrefs(prefs)
        loadCachedUser(prefs)?.id?.takeIf { it.isNotBlank() }?.let { return it }
        return currentUser()?.id
    }
}
