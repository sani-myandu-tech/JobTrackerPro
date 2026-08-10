package com.jobtracker.presentation.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jobtracker.domain.usecase.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class RegisterViewModel @Inject constructor(private val registerUseCase: RegisterUseCase) : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onNameChange(v: String) { _uiState.value = _uiState.value.copy(name = v) }
    fun onEmailChange(v: String) { _uiState.value = _uiState.value.copy(email = v) }
    fun onPasswordChange(v: String) { _uiState.value = _uiState.value.copy(password = v) }
    fun onConfirmPasswordChange(v: String) { _uiState.value = _uiState.value.copy(confirmPassword = v) }

    fun register() {
        val s = _uiState.value
        when {
            s.name.isBlank() || s.email.isBlank() || s.password.isBlank() ->
                _uiState.value = s.copy(error = "Please fill in all fields")
            s.password != s.confirmPassword ->
                _uiState.value = s.copy(error = "Passwords do not match")
            s.password.length < 6 ->
                _uiState.value = s.copy(error = "Password must be at least 6 characters")
            else -> viewModelScope.launch {
                _uiState.value = s.copy(isLoading = true, error = null)
                registerUseCase(s.email.trim(), s.password, s.name.trim())
                    .onSuccess { _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true) }
                    .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message ?: "Registration failed") }
            }
        }
    }
}
