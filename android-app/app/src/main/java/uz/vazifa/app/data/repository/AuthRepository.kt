package uz.vazifa.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import uz.vazifa.app.data.remote.ApiClient
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
        }
    }

    suspend fun updateFcm(token: String, enabled: Boolean) {
        api.api.updateFcm(FcmRequest(token, enabled))
    }
}
