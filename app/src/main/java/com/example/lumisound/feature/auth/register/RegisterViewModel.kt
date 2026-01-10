package com.example.lumisound.feature.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lumisound.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class RegisterSideEffect {
    data object NavigateToProfileSetup : RegisterSideEffect()
    data object NavigateToVerifyEmail : RegisterSideEffect()
    data object NavigateToHome : RegisterSideEffect()
    data object NavigateToLogin : RegisterSideEffect()
    data class ShowSnackbar(val message: String) : RegisterSideEffect()
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _sideEffect = MutableSharedFlow<RegisterSideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()

    fun signUpCreateProfileAndLogin(username: String, email: String, password: String) {
        viewModelScope.launch {
            // Сохраняем данные для использования после подтверждения email
            authRepository.savePendingUsername(email, username)
            // Регистрируем пользователя (отправит email для подтверждения)
            authRepository.signUp(email, password)
                .onSuccess {
                    // Navigate to verify email screen
                    _sideEffect.emit(RegisterSideEffect.NavigateToVerifyEmail)
                }
                .onFailure {
                    _sideEffect.emit(RegisterSideEffect.ShowSnackbar(it.message ?: "Произошла неизвестная ошибка"))
                }
        }
    }
}