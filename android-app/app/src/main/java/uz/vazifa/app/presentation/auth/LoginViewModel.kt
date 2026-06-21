package uz.vazifa.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import uz.vazifa.app.data.repository.AuthRepository
import javax.inject.Inject

data class LoginUiState(
    val login: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val errorKey: String? = null,
    val loggedIn: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state = _state.asStateFlow()

    fun onLoginChange(v: String) = _state.update { it.copy(login = v, errorKey = null) }
    fun onPasswordChange(v: String) = _state.update { it.copy(password = v, errorKey = null) }

    fun login(deviceId: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, errorKey = null) }
            try {
                auth.login(_state.value.login.trim(), _state.value.password, deviceId)
                _state.update { it.copy(loading = false, loggedIn = true) }
            } catch (e: HttpException) {
                val key = when (e.code()) {
                    403 -> "login_device_pending"
                    else -> "login_error"
                }
                _state.update { it.copy(loading = false, errorKey = key) }
            } catch (_: Exception) {
                _state.update { it.copy(loading = false, errorKey = "login_network_error") }
            }
        }
    }
}
