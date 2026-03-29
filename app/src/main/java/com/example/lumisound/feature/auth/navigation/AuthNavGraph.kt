package com.example.lumisound.feature.auth.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.lumisound.feature.auth.login.LoginScreen
import com.example.lumisound.feature.auth.profilesetup.ProfileSetupScreen
import com.example.lumisound.feature.auth.register.RegisterScreen
import com.example.lumisound.feature.auth.register.VerifyEmailScreen
import com.example.lumisound.feature.auth.welcome.AuthWelcomeScreen
import com.example.lumisound.feature.home.HomeScreen

@Composable
fun AuthNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = AuthDestination.Welcome.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        addWelcomeDestination(navController)
        addLoginDestination(navController)
        addRegisterDestination(navController)
        addVerifyEmailDestination(navController)
        addProfileSetupDestination(navController)
        addHomeDestination(navController)
    }
}

private fun NavGraphBuilder.addWelcomeDestination(navController: NavHostController) {
    composable(
        route = AuthDestination.Welcome.route,
        enterTransition = { fadeIn(animationSpec = tween(150)) }, // Уменьшено в 2 раза
        exitTransition = { fadeOut(animationSpec = tween(150)) } // Уменьшено в 2 раза
    ) {
        AuthWelcomeScreen(
            onLoginClick = {
                navController.navigate(AuthDestination.Login.route) {
                    launchSingleTop = true
                }
            },
            onRegisterClick = {
                navController.navigate(AuthDestination.Register.route) {
                    launchSingleTop = true
                }
            }
        )
    }
}

private fun NavGraphBuilder.addLoginDestination(navController: NavHostController) {
    composable(
        route = AuthDestination.Login.route,
        enterTransition = { fadeIn(animationSpec = tween(150)) }, // Уменьшено в 2 раза
        exitTransition = { fadeOut(animationSpec = tween(150)) } // Уменьшено в 2 раза
    ) {
        LoginScreen(
            onNavigateToHome = {
                navController.navigate("home") {
                    popUpTo(AuthDestination.Welcome.route) { inclusive = true }
                    launchSingleTop = true
                }
            },
            onNavigateToProfileSetup = {
                navController.navigate(AuthDestination.ProfileSetup.route) {
                    popUpTo(AuthDestination.Welcome.route) { inclusive = false }
                    launchSingleTop = true
                }
            },
            onNavigateToRegister = {
                navController.navigate(AuthDestination.Register.route) {
                    launchSingleTop = true
                }
            },
            onNavigateToForgot = { /* TODO: Forgot password flow */ }
        )
    }
}

private fun NavGraphBuilder.addRegisterDestination(navController: NavHostController) {
    composable(
        route = AuthDestination.Register.route,
        enterTransition = { fadeIn(animationSpec = tween(150)) }, // Уменьшено в 2 раза
        exitTransition = { fadeOut(animationSpec = tween(150)) } // Уменьшено в 2 раза
    ) {
        RegisterScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToLogin = {
                navController.navigate(AuthDestination.Login.route) {
                    popUpTo(AuthDestination.Welcome.route) { inclusive = false }
                    launchSingleTop = true
                }
            },
            onNavigateToHome = {
                navController.navigate("home") {
                    popUpTo(AuthDestination.Welcome.route) { inclusive = true }
                    launchSingleTop = true
                }
            },
            onNavigateToProfileSetup = {
                navController.navigate(AuthDestination.ProfileSetup.route) {
                    popUpTo(AuthDestination.Welcome.route) { inclusive = false }
                    launchSingleTop = true
                }
            },
            onNavigateToVerifyEmail = {
                navController.navigate(AuthDestination.VerifyEmail.route) {
                    popUpTo(AuthDestination.Welcome.route) { inclusive = false }
                    launchSingleTop = true
                }
            }
        )
    }
}

private fun NavGraphBuilder.addVerifyEmailDestination(navController: NavHostController) {
    composable(
        route = AuthDestination.VerifyEmail.route,
        enterTransition = { fadeIn(animationSpec = tween(150)) },
        exitTransition = { fadeOut(animationSpec = tween(150)) }
    ) {
        VerifyEmailScreen(
            onNavigateToLogin = {
                navController.navigate(AuthDestination.Login.route) {
                    popUpTo(AuthDestination.Welcome.route) { inclusive = false }
                    launchSingleTop = true
                }
            },
            onNavigateToProfileSetup = {
                navController.navigate(AuthDestination.ProfileSetup.route) {
                    popUpTo(AuthDestination.Welcome.route) { inclusive = false }
                    launchSingleTop = true
                }
            }
        )
    }
}

private fun NavGraphBuilder.addProfileSetupDestination(navController: NavHostController) {
    composable(
        route = AuthDestination.ProfileSetup.route,
        enterTransition = { fadeIn(animationSpec = tween(150)) },
        exitTransition = { fadeOut(animationSpec = tween(150)) }
    ) {
        ProfileSetupScreen(
            onNavigateToHome = {
                navController.navigate("home") {
                    popUpTo(AuthDestination.Welcome.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
        )
    }
}

private fun NavGraphBuilder.addHomeDestination(navController: NavHostController) {
    composable("home") {
        com.example.lumisound.navigation.MainNavGraph(
            startDestination = "home",
            userName = "Александр"
        )
    }
}
