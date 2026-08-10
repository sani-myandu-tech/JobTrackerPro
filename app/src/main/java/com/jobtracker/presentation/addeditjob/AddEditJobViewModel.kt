package com.jobtracker.presentation.addeditjob

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jobtracker.domain.model.ApplicationStatus
import com.jobtracker.domain.model.JobApplication
import com.jobtracker.domain.usecase.GetCurrentUserUseCase
import com.jobtracker.domain.usecase.GetJobByIdUseCase
import com.jobtracker.domain.usecase.SaveJobUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class AddEditJobUiState(
    val jobId: String = "",
    val companyName: String = "",
    val jobTitle: String = "",
    val jobDescription: String = "",
    val location: String = "",
    val salary: String = "",
    val status: ApplicationStatus = ApplicationStatus.APPLIED,
    val appliedDate: Date = Date(),
    val deadlineDate: Date? = null,
    val notes: String = "",
    val jobUrl: String = "",
    val contactName: String = "",
    val contactEmail: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val isEditing: Boolean = false
)

@HiltViewModel
class AddEditJobViewModel @Inject constructor(
    private val saveJobUseCase: SaveJobUseCase,
    private val getJobByIdUseCase: GetJobByIdUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditJobUiState())
    val uiState: StateFlow<AddEditJobUiState> = _uiState.asStateFlow()

    fun loadJob(jobId: String?) {
        if (jobId == null) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val job = getJobByIdUseCase(jobId).first()
            job?.let {
                _uiState.value = AddEditJobUiState(
                    jobId = it.id, companyName = it.companyName, jobTitle = it.jobTitle,
                    jobDescription = it.jobDescription, location = it.location, salary = it.salary,
                    status = it.status, appliedDate = it.appliedDate, deadlineDate = it.deadlineDate,
                    notes = it.notes, jobUrl = it.jobUrl, contactName = it.contactName,
                    contactEmail = it.contactEmail, isLoading = false, isEditing = true
                )
            } ?: run { _uiState.value = _uiState.value.copy(isLoading = false) }
        }
    }

    fun onCompanyNameChange(v: String) { _uiState.value = _uiState.value.copy(companyName = v) }
    fun onJobTitleChange(v: String) { _uiState.value = _uiState.value.copy(jobTitle = v) }
    fun onJobDescriptionChange(v: String) { _uiState.value = _uiState.value.copy(jobDescription = v) }
    fun onLocationChange(v: String) { _uiState.value = _uiState.value.copy(location = v) }
    fun onSalaryChange(v: String) { _uiState.value = _uiState.value.copy(salary = v) }
    fun onStatusChange(v: ApplicationStatus) { _uiState.value = _uiState.value.copy(status = v) }
    fun onAppliedDateChange(v: Date) { _uiState.value = _uiState.value.copy(appliedDate = v) }
    fun onDeadlineDateChange(v: Date?) { _uiState.value = _uiState.value.copy(deadlineDate = v) }
    fun onNotesChange(v: String) { _uiState.value = _uiState.value.copy(notes = v) }
    fun onJobUrlChange(v: String) { _uiState.value = _uiState.value.copy(jobUrl = v) }
    fun onContactNameChange(v: String) { _uiState.value = _uiState.value.copy(contactName = v) }
    fun onContactEmailChange(v: String) { _uiState.value = _uiState.value.copy(contactEmail = v) }
    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }

    fun saveJob() {
        val s = _uiState.value
        if (s.companyName.isBlank() || s.jobTitle.isBlank()) {
            _uiState.value = s.copy(error = "Company name and job title are required")
            return
        }
        viewModelScope.launch {
            _uiState.value = s.copy(isLoading = true, error = null)
            val userId = getCurrentUserUseCase()?.uid ?: ""
            val job = JobApplication(
                id = s.jobId, userId = userId, companyName = s.companyName.trim(),
                jobTitle = s.jobTitle.trim(), jobDescription = s.jobDescription.trim(),
                location = s.location.trim(), salary = s.salary.trim(), status = s.status,
                appliedDate = s.appliedDate, deadlineDate = s.deadlineDate,
                notes = s.notes.trim(), jobUrl = s.jobUrl.trim(),
                contactName = s.contactName.trim(), contactEmail = s.contactEmail.trim()
            )
            try {
                saveJobUseCase(job)
                _uiState.value = _uiState.value.copy(isLoading = false, isSaved = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Failed to save")
            }
        }
    }
}
