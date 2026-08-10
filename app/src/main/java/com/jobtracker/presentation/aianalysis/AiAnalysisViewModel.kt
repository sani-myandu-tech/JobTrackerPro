package com.jobtracker.presentation.aianalysis

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jobtracker.data.local.secure.SecureCvCache
import com.jobtracker.domain.model.CvAnalysisResult
import com.jobtracker.domain.model.JobApplication
import com.jobtracker.domain.usecase.AnalyseCvUseCase
import com.jobtracker.domain.usecase.GetJobByIdUseCase
import com.jobtracker.domain.usecase.SaveJobUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiAnalysisUiState(
    val job: JobApplication? = null,
    val selectedFileName: String = "",
    val selectedFileBytes: ByteArray? = null,
    val manualCvText: String = "",
    val useManualText: Boolean = false,
    val isAnalysing: Boolean = false,
    val result: CvAnalysisResult? = null,
    val error: String? = null,
    val isLoading: Boolean = true,
    val hasCachedCv: Boolean = false
)

@HiltViewModel
class AiAnalysisViewModel @Inject constructor(
    private val getJobByIdUseCase: GetJobByIdUseCase,
    private val analyseCvUseCase: AnalyseCvUseCase,
    private val saveJobUseCase: SaveJobUseCase,
    private val secureCvCache: SecureCvCache
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiAnalysisUiState())
    val uiState: StateFlow<AiAnalysisUiState> = _uiState.asStateFlow()

    fun loadJob(jobId: String) {
        viewModelScope.launch {
            val job = getJobByIdUseCase(jobId).first()
            _uiState.value = _uiState.value.copy(
                job = job,
                result = job?.cvAnalysisResult,
                isLoading = false,
                hasCachedCv = secureCvCache.getLastCvText() != null
            )
        }
    }

    /** Pre-fills the manual text field from the encrypted local cache of the last CV used. */
    fun useCachedCv() {
        val cached = secureCvCache.getLastCvText() ?: return
        _uiState.value = _uiState.value.copy(
            manualCvText = cached,
            useManualText = true
        )
    }

    fun onFileSelected(fileName: String, bytes: ByteArray) {
        _uiState.value = _uiState.value.copy(selectedFileName = fileName, selectedFileBytes = bytes, error = null)
    }

    fun onManualCvTextChange(text: String) {
        _uiState.value = _uiState.value.copy(manualCvText = text)
    }

    fun onToggleInputMode(useManual: Boolean) {
        _uiState.value = _uiState.value.copy(useManualText = useManual)
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }

    fun analyse() {
        val s = _uiState.value
        val job = s.job ?: return

        if (job.jobDescription.isBlank()) {
            _uiState.value = s.copy(error = "This job has no description. Please edit the job and add a description first.")
            return
        }

        viewModelScope.launch {
            _uiState.value = s.copy(isAnalysing = true, error = null)

            val result = if (s.useManualText) {
                if (s.manualCvText.isBlank()) {
                    _uiState.value = s.copy(isAnalysing = false, error = "Please paste your CV text")
                    return@launch
                }
                // Plain text, not PDF bytes — goes through the text-only path so it
                // isn't run through the PDF parser (which requires real PDF binary data).
                analyseCvUseCase.invokeWithText(
                    cvText = s.manualCvText,
                    jobDescription = job.jobDescription
                )
            } else {
                val bytes = s.selectedFileBytes
                if (bytes == null) {
                    _uiState.value = s.copy(isAnalysing = false, error = "Please select a CV file or paste your CV text")
                    return@launch
                }
                analyseCvUseCase(
                    pdfBytes = bytes,
                    fileName = s.selectedFileName,
                    jobDescription = job.jobDescription,
                    jobId = job.id
                )
            }

            result.onSuccess { analysisResult ->
                val updatedJob = job.copy(cvAnalysisResult = analysisResult)
                saveJobUseCase(updatedJob)
                _uiState.value = _uiState.value.copy(isAnalysing = false, result = analysisResult, job = updatedJob)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(isAnalysing = false, error = error.message ?: "Analysis failed")
            }
        }
    }
}
