package com.example.lumisound.feature.auth.navigation

/**
 * Навигационные маршруты для аутентификации.
 */
sealed class AuthDestination(val route: String) {
    data object Welcome : AuthDestination("welcome")
    data object Login : AuthDestination("login")
    data object Register : AuthDestination("register")
    data object VerifyEmail : AuthDestination("verify_email")
    data object ProfileSetup : AuthDestination("profile_setup")
}