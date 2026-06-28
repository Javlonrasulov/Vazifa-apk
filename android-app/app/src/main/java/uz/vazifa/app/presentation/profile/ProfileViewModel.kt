package uz.vazifa.app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import uz.vazifa.app.data.remote.UserDto
import uz.vazifa.app.data.repository.AppSettingsRepository
import uz.vazifa.app.data.repository.AuthRepository
import uz.vazifa.app.data.repository.ThemeMode
import uz.vazifa.app.localization.AppLanguage
import javax.inject.Inject

data class ProfileUiState(
    val user: UserDto? = null,
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val language: AppLanguage = AppLanguage.DEFAULT,
    val showChangePassword: Boolean = false,
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val changingPassword: Boolean = false,
    val changePasswordErrorKey: String? = null,
    val changePasswordSuccess: Boolean = false,
    val uploadingAvatar: Boolean = false,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val settings: AppSettingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state = _state.asStateFlow()

    fun load(onSessionExpired: () -> Unit = {}) = viewModelScope.launch {
        val hadSession = auth.hasStoredSessionAsync()
        val user = auth.currentUser()
        if (user == null) {
            if (hadSession) onSessionExpired()
            return@launch
        }
        val themeMode = settings.themeMode.first()
        val lang = settings.language.first()
        _state.update { it.copy(user = user, themeMode = themeMode, language = lang) }
    }

    fun setLanguage(language: AppLanguage) = viewModelScope.launch {
        settings.setLanguage(language)
        _state.update { it.copy(language = language) }
        auth.syncLanguageAsync()
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        settings.setThemeMode(mode)
        _state.update { it.copy(themeMode = mode) }
    }

    fun toggleChangePassword() {
        _state.update {
            it.copy(
                showChangePassword = !it.showChangePassword,
                currentPassword = "",
                newPassword = "",
                confirmPassword = "",
                changePasswordErrorKey = null,
                changePasswordSuccess = false,
            )
        }
    }

    fun onCurrentPasswordChange(value: String) {
        _state.update { it.copy(currentPassword = value, changePasswordErrorKey = null, changePasswordSuccess = false) }
    }

    fun onNewPasswordChange(value: String) {
        _state.update { it.copy(newPassword = value, changePasswordErrorKey = null, changePasswordSuccess = false) }
    }

    fun onConfirmPasswordChange(value: String) {
        _state.update { it.copy(confirmPassword = value, changePasswordErrorKey = null, changePasswordSuccess = false) }
    }

    fun changePassword() {
        val current = _state.value
        if (current.currentPassword.isBlank() || current.newPassword.isBlank() || current.confirmPassword.isBlank()) {
            _state.update { it.copy(changePasswordErrorKey = "profile_password_empty") }
            return
        }
        if (current.newPassword.length < 6) {
            _state.update { it.copy(changePasswordErrorKey = "profile_password_min_length") }
            return
        }
        if (current.newPassword != current.confirmPassword) {
            _state.update { it.copy(changePasswordErrorKey = "profile_password_mismatch") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(changingPassword = true, changePasswordErrorKey = null, changePasswordSuccess = false) }
            try {
                auth.changePassword(current.currentPassword, current.newPassword)
                _state.update {
                    it.copy(
                        changingPassword = false,
                        showChangePassword = false,
                        currentPassword = "",
                        newPassword = "",
                        confirmPassword = "",
                        changePasswordSuccess = true,
                    )
                }
            } catch (e: HttpException) {
                val key = if (e.code() == 401) "profile_wrong_current_password" else "login_network_error"
                _state.update { it.copy(changingPassword = false, changePasswordErrorKey = key) }
            } catch (_: Exception) {
                _state.update { it.copy(changingPassword = false, changePasswordErrorKey = "login_network_error") }
            }
        }
    }

    fun clearChangePasswordSuccess() {
        _state.update { it.copy(changePasswordSuccess = false) }
    }

    fun uploadAvatar(file: java.io.File) {
        viewModelScope.launch {
            _state.update { it.copy(uploadingAvatar = true) }
            runCatching { auth.uploadAvatar(file) }
                .onSuccess { u -> _state.update { it.copy(user = u, uploadingAvatar = false) } }
                .onFailure { _state.update { it.copy(uploadingAvatar = false) } }
        }
    }

    fun deleteAvatar() {
        viewModelScope.launch {
            _state.update { it.copy(uploadingAvatar = true) }
            runCatching { auth.deleteAvatar() }
                .onSuccess { u -> _state.update { it.copy(user = u, uploadingAvatar = false) } }
                .onFailure { _state.update { it.copy(uploadingAvatar = false) } }
        }
    }

    fun logout(onDone: () -> Unit) = viewModelScope.launch {
        auth.logout()
        onDone()
    }
}
