package com.example.lumisound.feature.auth.login

sealed class LoginUiState {
    data class Idle(
        val email: String = "",
        val password: String = "",
        val isPasswordVisible: Boolean = false,
        val emailError: String? = null,
        val passwordError: String? = null,
        val errorMessage: String? = null
    ) : LoginUiState()

    data class InputChanged(
        val email: String,
        val password: String,
        val isPasswordVisible: Boolean,
        val emailError: String? = null,
        val passwordError: String? = null
    ) : LoginUiState()

    data class Submitting(
        val email: String,
        val password: String,
        val isPasswordVisible: Boolean
    ) : LoginUiState()

    data class Error(
        val email: String,
        val password: String,
        val isPasswordVisible: Boolean,
        val emailError: String? = null,
        val passwordError: String? = null,
        val errorMessage: String
    ) : LoginUiState()

    data class Success(
        val email: String,
        val password: String
    ) : LoginUiState()
}

sealed class LoginUiAction {
    data class EmailChanged(val email: String) : LoginUiAction()
    data class PasswordChanged(val password: String) : LoginUiAction()
    object TogglePasswordVisibility : LoginUiAction()
    object Submit : LoginUiAction()
    data class GoogleSignIn(val idToken: String) : LoginUiAction()
    object GoToRegister : LoginUiAction()
    object GoToForgot : LoginUiAction()
}

sealed class LoginSideEffect {
    data object NavigateToHome : LoginSideEffect()
    data object NavigateToProfileSetup : LoginSideEffect()
    data class ShowSnackbar(val message: String) : LoginSideEffect()
}

