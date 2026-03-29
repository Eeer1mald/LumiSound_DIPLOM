package com.example.lumisound.feature.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lumisound.data.local.PendingUsernameStore
import com.example.lumisound.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val pendingUsernameStore: PendingUsernameStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(
        LoginUiState.Idle()
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<LoginSideEffect>()
    val sideEffect: SharedFlow<LoginSideEffect> = _sideEffect.asSharedFlow()

    fun handleAction(action: LoginUiAction) {
        when (action) {
            is LoginUiAction.EmailChanged -> {
                _uiState.update { currentState ->
                    val newEmail = action.email
                    val (newPassword, newIsPasswordVisible) = when (currentState) {
                        is LoginUiState.Idle -> currentState.password to currentState.isPasswordVisible
                        is LoginUiState.InputChanged -> currentState.password to currentState.isPasswordVisible
                        is LoginUiState.Submitting -> currentState.password to currentState.isPasswordVisible
                        is LoginUiState.Error -> currentState.password to currentState.isPasswordVisible
                        is LoginUiState.Success -> currentState.password to false
                    }
                    LoginUiState.InputChanged(
                        email = newEmail,
                        password = newPassword,
                        isPasswordVisible = newIsPasswordVisible,
                        emailError = validateEmail(newEmail),
                        passwordError = validatePassword(newPassword)
                    )
                }
            }

            is LoginUiAction.PasswordChanged -> {
                _uiState.update { currentState ->
                    val newPassword = action.password
                    val (newEmail, newIsPasswordVisible) = when (currentState) {
                        is LoginUiState.Idle -> currentState.email to currentState.isPasswordVisible
                        is LoginUiState.InputChanged -> currentState.email to currentState.isPasswordVisible
                        is LoginUiState.Submitting -> currentState.email to currentState.isPasswordVisible
                        is LoginUiState.Error -> currentState.email to currentState.isPasswordVisible
                        is LoginUiState.Success -> currentState.email to false
                    }
                    LoginUiState.InputChanged(
                        email = newEmail,
                        password = newPassword,
                        isPasswordVisible = newIsPasswordVisible,
                        emailError = validateEmail(newEmail),
                        passwordError = validatePassword(newPassword)
                    )
                }
            }

            is LoginUiAction.TogglePasswordVisibility -> {
                _uiState.update { currentState ->
                    when (currentState) {
                        is LoginUiState.Idle -> currentState.copy(isPasswordVisible = !currentState.isPasswordVisible)
                        is LoginUiState.InputChanged -> currentState.copy(isPasswordVisible = !currentState.isPasswordVisible)
                        is LoginUiState.Submitting -> currentState.copy(isPasswordVisible = !currentState.isPasswordVisible)
                        is LoginUiState.Error -> currentState.copy(isPasswordVisible = !currentState.isPasswordVisible)
                        is LoginUiState.Success -> currentState
                    }
                }
            }

            is LoginUiAction.Submit -> {
                val currentState = _uiState.value
                val email = when (currentState) {
                    is LoginUiState.Idle -> currentState.email
                    is LoginUiState.InputChanged -> currentState.email
                    is LoginUiState.Submitting -> currentState.email
                    is LoginUiState.Error -> currentState.email
                    is LoginUiState.Success -> currentState.email
                }
                val password = when (currentState) {
                    is LoginUiState.Idle -> currentState.password
                    is LoginUiState.InputChanged -> currentState.password
                    is LoginUiState.Submitting -> currentState.password
                    is LoginUiState.Error -> currentState.password
                    is LoginUiState.Success -> currentState.password
                }
                val isPasswordVisible = when (currentState) {
                    is LoginUiState.Idle -> currentState.isPasswordVisible
                    is LoginUiState.InputChanged -> currentState.isPasswordVisible
                    is LoginUiState.Submitting -> currentState.isPasswordVisible
                    is LoginUiState.Error -> currentState.isPasswordVisible
                    is LoginUiState.Success -> false
                }

                val emailError = validateEmail(email)
                val passwordError = validatePassword(password)

                if (emailError != null || passwordError != null) {
                    _uiState.value = LoginUiState.Error(
                        email = email,
                        password = password,
                        isPasswordVisible = isPasswordVisible,
                        emailError = emailError,
                        passwordError = passwordError,
                        errorMessage = "Проверьте правильность введенных данных"
                    )
                    return
                }

                _uiState.value = LoginUiState.Submitting(
                    email = email,
                    password = password,
                    isPasswordVisible = isPasswordVisible
                )

                viewModelScope.launch {
                    authRepository.login(email, password)
                        .onSuccess { token ->
                            // Проверяем, есть ли pending username (пользователь только что подтвердил email)
                            val pendingUsername = pendingUsernameStore.get(email)
                            if (pendingUsername != null) {
                                // Пользователь только что зарегистрировался и подтвердил email
                                // Переходим на экран профиля для ввода данных
                                _uiState.value = LoginUiState.Success(email = email, password = password)
                                _sideEffect.emit(LoginSideEffect.NavigateToProfileSetup)
                            } else {
                                // Обычный вход - создадим профиль, если ждал username (старая логика)
                                runCatching { authRepository.syncProfileIfNeeded(token.accessToken, email) }
                                _uiState.value = LoginUiState.Success(email = email, password = password)
                                _sideEffect.emit(LoginSideEffect.NavigateToHome)
                            }
                        }
                        .onFailure { exception ->
                            _uiState.value = LoginUiState.Error(
                                email = email,
                                password = password,
                                isPasswordVisible = isPasswordVisible,
                                emailError = null,
                                passwordError = null,
                                errorMessage = exception.message ?: "Не удалось войти. Повторите попытку."
                            )
                            _sideEffect.emit(LoginSideEffect.ShowSnackbar(exception.message ?: "Не удалось войти. Повторите попытку."))
                        }
                }
            }

            is LoginUiAction.GoogleSignIn -> {
                val currentState = _uiState.value
                val email = when (currentState) {
                    is LoginUiState.Idle -> currentState.email
                    is LoginUiState.InputChanged -> currentState.email
                    is LoginUiState.Submitting -> currentState.email
                    is LoginUiState.Error -> currentState.email
                    is LoginUiState.Success -> currentState.email
                }
                val password = when (currentState) {
                    is LoginUiState.Idle -> currentState.password
                    is LoginUiState.InputChanged -> currentState.password
                    is LoginUiState.Submitting -> currentState.password
                    is LoginUiState.Error -> currentState.password
                    is LoginUiState.Success -> currentState.password
                }
                val isPasswordVisible = when (currentState) {
                    is LoginUiState.Idle -> currentState.isPasswordVisible
                    is LoginUiState.InputChanged -> currentState.isPasswordVisible
                    is LoginUiState.Submitting -> currentState.isPasswordVisible
                    is LoginUiState.Error -> currentState.isPasswordVisible
                    is LoginUiState.Success -> false
                }

                _uiState.value = LoginUiState.Submitting(
                    email = email,
                    password = password,
                    isPasswordVisible = isPasswordVisible
                )

                viewModelScope.launch {
                    authRepository.googleSignIn(action.idToken)
                        .onSuccess {
                            _uiState.value = LoginUiState.Success(email = email, password = password)
                            _sideEffect.emit(LoginSideEffect.NavigateToHome)
                        }
                        .onFailure { exception ->
                            _uiState.value = LoginUiState.Error(
                                email = email,
                                password = password,
                                isPasswordVisible = isPasswordVisible,
                                emailError = null,
                                passwordError = null,
                                errorMessage = exception.message ?: "Не удалось войти через Google. Повторите попытку."
                            )
                            _sideEffect.emit(LoginSideEffect.ShowSnackbar(exception.message ?: "Не удалось войти через Google."))
                        }
                }
            }

            is LoginUiAction.GoToRegister -> {
                // TODO: Навигация
            }

            is LoginUiAction.GoToForgot -> {
                // TODO: Навигация
            }
        }
    }

    private fun validateEmail(email: String): String? {
        if (email.isEmpty()) return null
        if (email.length > 254) return "Email слишком длинный"
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$".toRegex()
        return if (!emailRegex.matches(email)) "Введите корректный email" else null
    }

    private fun validatePassword(password: String): String? {
        if (password.isEmpty()) return null
        return if (password.length < 8) "Минимум 8 символов" else null
    }
}

