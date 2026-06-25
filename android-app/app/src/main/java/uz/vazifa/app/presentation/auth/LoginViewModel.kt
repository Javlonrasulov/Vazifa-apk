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
import uz.vazifa.app.util.UzPhoneFormatter
import javax.inject.Inject

enum class LoginMode { PHONE, LOGIN }

data class LoginUiState(
    val mode: LoginMode = LoginMode.PHONE,
    val phoneDigits: String = "",
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

    fun setMode(mode: LoginMode) {
        _state.update {
            it.copy(
                mode = mode,
                phoneDigits = "",
                login = "",
                errorKey = null,
            )
        }
    }

    fun onPhoneDigitsChange(v: String) {
        val digits = UzPhoneFormatter.extractNationalDigits(v)
        _state.update { it.copy(phoneDigits = digits, errorKey = null) }
    }

    fun onLoginChange(v: String) = _state.update { it.copy(login = v, errorKey = null) }

    fun onPasswordChange(v: String) = _state.update { it.copy(password = v, errorKey = null) }

    fun login(deviceId: String, deviceName: String) {
        val current = _state.value
        if (current.mode == LoginMode.PHONE && current.phoneDigits.length < 9) {
            _state.update { it.copy(errorKey = "login_empty") }
            return
        }
        if (current.mode == LoginMode.LOGIN && current.login.isBlank()) {
            _state.update { it.copy(errorKey = "login_empty") }
            return
        }
        if (current.password.isBlank()) {
            _state.update { it.copy(errorKey = "login_empty") }
            return
        }

        val identifier = if (current.mode == LoginMode.PHONE) {
            UzPhoneFormatter.formattedForApi(current.phoneDigits)
        } else {
            current.login.trim()
        }

        viewModelScope.launch {
            _state.update { it.copy(loading = true, errorKey = null) }
            try {
                auth.login(identifier, current.password, deviceId, deviceName)
                _state.update { it.copy(loading = false, loggedIn = true) }
            } catch (e: HttpException) {
                val key = when (e.code()) {
                    403 -> {
                        val code = e.response()?.errorBody()?.string()
                            ?.let { body -> Regex(""""code"\s*:\s*"([^"]+)"""").find(body)?.groupValues?.getOrNull(1) }
                        when (code) {
                            "DEVICE_LIMIT_REACHED" -> "login_device_limit"
                            else -> "login_device_pending"
                        }
                    }
                    else -> "login_error"
                }
                _state.update { it.copy(loading = false, errorKey = key) }
            } catch (_: Exception) {
                _state.update { it.copy(loading = false, errorKey = "login_network_error") }
            }
        }
    }
}
