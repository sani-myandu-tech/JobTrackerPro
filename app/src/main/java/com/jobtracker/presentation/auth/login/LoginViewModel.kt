package com.jobtracker.presentation.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jobtracker.domain.usecase.GetCurrentUserUseCase
import com.jobtracker.domain.usecase.SignInUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    val isAlreadyLoggedIn: Boolean get() = getCurrentUserUseCase() != null

    fun onEmailChange(email: String) { _uiState.value = _uiState.value.copy(email = email) }
    fun onPasswordChange(pass: String) { _uiState.value = _uiState.value.copy(password = pass) }
    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }

    fun signIn() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(error = "Please fill in all fields")
            return
        }
        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null)
            signInUseCase(state.email.trim(), state.password)
                .onSuccess { _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message ?: "Sign in failed") }
        }
    }
}
