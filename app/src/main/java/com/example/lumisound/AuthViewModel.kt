package com.example.lumisound

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthResult {
    object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
    object Loading : AuthResult()
    object Idle : AuthResult()
}

class AuthViewModel : ViewModel() {
    private val _authState = MutableStateFlow<AuthResult>(AuthResult.Idle)
    val authState: StateFlow<AuthResult> = _authState

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthResult.Loading
            delay(1200)
            // Здесь имитация: email=="test@test.com" и password=="123456" успешны
            if (email == "test@test.com" && password == "123456") {
                _authState.value = AuthResult.Success
            } else {
                _authState.value = AuthResult.Error("Неверные данные для входа")
            }
        }
    }

    fun onGoogleSignIn(token: String?) {
        viewModelScope.launch {
            _authState.value = AuthResult.Loading
            delay(1200)
            // Упрощенно: любой не-null или не-пустой token считается успехом
            if (!token.isNullOrEmpty()) {
                _authState.value = AuthResult.Success
            } else {
                _authState.value = AuthResult.Error("Ошибка Google-входа")
            }
        }
    }
    fun resetState() {
        _authState.value = AuthResult.Idle
    }
}
