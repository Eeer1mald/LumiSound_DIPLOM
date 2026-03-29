package com.example.lumisound.feature.auth.navigation

sealed class AuthDestination(val route: String) {
    data object Welcome : AuthDestination("auth/welcome")
    data object Login : AuthDestination("auth/login")
    data object Register : AuthDestination("auth/register")
    data object VerifyEmail : AuthDestination("auth/verify_email")
    data object ProfileSetup : AuthDestination("auth/profile_setup")
}
