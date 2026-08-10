package com.jobtracker.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jobtracker.domain.model.AnalyticsData
import com.jobtracker.domain.model.ApplicationStatus
import com.jobtracker.domain.model.JobApplication
import com.jobtracker.domain.usecase.GetAnalyticsUseCase
import com.jobtracker.domain.usecase.GetCurrentUserUseCase
import com.jobtracker.domain.usecase.GetJobsUseCase
import com.jobtracker.domain.usecase.SignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val userName: String = "",
    val recentJobs: List<JobApplication> = emptyList(),
    val analytics: AnalyticsData = AnalyticsData(),
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getJobsUseCase: GetJobsUseCase,
    private val getAnalyticsUseCase: GetAnalyticsUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val signOutUseCase: SignOutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        val user = getCurrentUserUseCase()
        _uiState.value = _uiState.value.copy(userName = user?.displayName?.ifBlank { user.email } ?: "")
        viewModelScope.launch {
            getJobsUseCase().collect { jobs ->
                _uiState.value = _uiState.value.copy(
                    recentJobs = jobs.take(5),
                    isLoading = false
                )
            }
        }
        viewModelScope.launch {
            getAnalyticsUseCase().collect { analytics ->
                _uiState.value = _uiState.value.copy(analytics = analytics)
            }
        }
    }

    fun signOut() { viewModelScope.launch { signOutUseCase() } }
}
