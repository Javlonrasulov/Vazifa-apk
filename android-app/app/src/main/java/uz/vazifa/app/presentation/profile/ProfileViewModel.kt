package uz.vazifa.app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.vazifa.app.data.remote.UserDto
import uz.vazifa.app.data.repository.AuthRepository
import uz.vazifa.app.data.remote.ApiClient
import javax.inject.Inject

data class ProfileUiState(val user: UserDto? = null, val isDark: Boolean = true)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val api: ApiClient,
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state = _state.asStateFlow()

    fun load() = viewModelScope.launch {
        runCatching {
            val user = api.api.me()
            val dark = auth.isDarkMode.first()
            _state.update { it.copy(user = user, isDark = dark) }
        }
    }

    fun toggleTheme() = viewModelScope.launch {
        val next = !_state.value.isDark
        auth.setDarkMode(next)
        _state.update { it.copy(isDark = next) }
    }

    fun logout(onDone: () -> Unit) = viewModelScope.launch {
        auth.logout()
        onDone()
    }
}
