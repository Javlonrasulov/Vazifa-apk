package uz.vazifa.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.vazifa.app.data.repository.TaskRepository
import uz.vazifa.app.domain.model.DashboardStats
import uz.vazifa.app.domain.model.Task
import javax.inject.Inject

data class DashboardUiState(
    val stats: DashboardStats? = null,
    val tasks: List<Task> = emptyList(),
    val loading: Boolean = false,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(private val tasks: TaskRepository) : ViewModel() {
    private val _state = MutableStateFlow(DashboardUiState())
    val state = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            runCatching {
                val stats = tasks.getDashboardStats()
                val list = tasks.getTasks()
                _state.update { it.copy(stats = stats, tasks = list, loading = false) }
            }.onFailure {
                _state.update { it.copy(loading = false) }
            }
        }
    }
}
