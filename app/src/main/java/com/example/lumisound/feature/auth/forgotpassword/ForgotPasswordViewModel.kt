package com.example.lumisound.feature.auth.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lumisound.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ForgotPasswordState {
    data object Idle : ForgotPasswordState()
    data object Loading : ForgotPasswordState()
    data class Success(val email: String) : ForgotPasswordState()
    data class Error(val message: String) : ForgotPasswordState()
}

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ForgotPasswordState>(ForgotPasswordState.Idle)
    val state: StateFlow<ForgotPasswordState> = _state

    fun sendResetEmail(email: String) {
        val trimmed = email.trim()
        if (trimmed.isBlank()) {
            _state.value = ForgotPasswordState.Error("Введите email")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
            _state.value = ForgotPasswordState.Error("Введите корректный email")
            return
        }
        viewModelScope.launch {
            _state.value = ForgotPasswordState.Loading
            val result = authRepository.resetPassword(trimmed)
            _state.value = if (result.isSuccess) {
                ForgotPasswordState.Success(trimmed)
            } else {
                ForgotPasswordState.Error(
                    result.exceptionOrNull()?.message ?: "Не удалось отправить письмо"
                )
            }
        }
    }

    fun resetState() {
        _state.value = ForgotPasswordState.Idle
    }
}
