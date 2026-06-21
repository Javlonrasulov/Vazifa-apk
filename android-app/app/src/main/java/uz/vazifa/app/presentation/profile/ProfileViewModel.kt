package uz.vazifa.app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.vazifa.app.data.remote.ApiClient
import uz.vazifa.app.data.remote.UserDto
import uz.vazifa.app.data.repository.AppSettingsRepository
import uz.vazifa.app.data.repository.AuthRepository
import uz.vazifa.app.data.repository.ThemeMode
import uz.vazifa.app.localization.AppLanguage
import javax.inject.Inject

data class ProfileUiState(
    val user: UserDto? = null,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val language: AppLanguage = AppLanguage.DEFAULT,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val api: ApiClient,
    private val settings: AppSettingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state = _state.asStateFlow()

    fun load() = viewModelScope.launch {
        runCatching {
            val user = api.api.me()
            val themeMode = settings.themeMode.first()
            val lang = settings.language.first()
            _state.update { it.copy(user = user, themeMode = themeMode, language = lang) }
        }
    }

    fun setLanguage(language: AppLanguage) = viewModelScope.launch {
        settings.setLanguage(language)
        _state.update { it.copy(language = language) }
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        settings.setThemeMode(mode)
        _state.update { it.copy(themeMode = mode) }
    }

    fun logout(onDone: () -> Unit) = viewModelScope.launch {
        auth.logout()
        onDone()
    }
}
