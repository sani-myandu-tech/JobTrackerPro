package com.jobtracker.presentation.joblist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jobtracker.domain.model.ApplicationStatus
import com.jobtracker.domain.model.JobApplication
import com.jobtracker.domain.usecase.DeleteJobUseCase
import com.jobtracker.domain.usecase.GetJobsUseCase
import com.jobtracker.domain.usecase.SearchJobsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JobListUiState(
    val jobs: List<JobApplication> = emptyList(),
    val filteredJobs: List<JobApplication> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: ApplicationStatus? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val deletedJobId: String? = null
)

@OptIn(FlowPreview::class)
@HiltViewModel
class JobListViewModel @Inject constructor(
    private val getJobsUseCase: GetJobsUseCase,
    private val searchJobsUseCase: SearchJobsUseCase,
    private val deleteJobUseCase: DeleteJobUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(JobListUiState())
    val uiState: StateFlow<JobListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getJobsUseCase().collect { jobs ->
                _uiState.value = _uiState.value.copy(
                    jobs = jobs,
                    filteredJobs = applyFilters(jobs, _uiState.value.searchQuery, _uiState.value.selectedFilter),
                    isLoading = false
                )
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyCurrentFilters()
    }

    fun onFilterChange(status: ApplicationStatus?) {
        _uiState.value = _uiState.value.copy(selectedFilter = status)
        applyCurrentFilters()
    }

    fun deleteJob(id: String) {
        viewModelScope.launch {
            try {
                deleteJobUseCase(id)
                _uiState.value = _uiState.value.copy(deletedJobId = id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }

    private fun applyCurrentFilters() {
        val s = _uiState.value
        _uiState.value = s.copy(filteredJobs = applyFilters(s.jobs, s.searchQuery, s.selectedFilter))
    }

    private fun applyFilters(jobs: List<JobApplication>, query: String, status: ApplicationStatus?): List<JobApplication> {
        return jobs.filter { job ->
            val matchesQuery = query.isBlank() ||
                job.companyName.contains(query, ignoreCase = true) ||
                job.jobTitle.contains(query, ignoreCase = true) ||
                job.location.contains(query, ignoreCase = true)
            val matchesStatus = status == null || job.status == status
            matchesQuery && matchesStatus
        }
    }
}
