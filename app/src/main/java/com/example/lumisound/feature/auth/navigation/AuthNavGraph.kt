package com.example.lumisound.feature.auth.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.lumisound.data.local.SessionManager
import com.example.lumisound.feature.auth.forgotpassword.ForgotPasswordScreen
import com.example.lumisound.feature.auth.login.LoginScreen
import com.example.lumisound.feature.auth.navigation.AuthDestination
import com.example.lumisound.feature.auth.profilesetup.ProfileSetupScreen
import com.example.lumisound.feature.auth.register.RegisterScreen
import com.example.lumisound.feature.auth.register.VerifyEmailScreen
import com.example.lumisound.feature.auth.welcome.AuthWelcomeScreen
import com.example.lumisound.feature.home.HomeScreen
import dagger.hilt.android.EntryPointAccessors

@Composable
fun AuthNavGraph(
    navController: NavHostController = rememberNavController(),
    synthesisInviteCode: String? = null
) {
    val context = LocalContext.current
    // Проверяем токен — если залогинен, сразу на home
    val startDestination = remember {
        try {
            val sessionManager = EntryPointAccessors.fromApplication(
                context.applicationContext,
                AuthNavGraphEntryPoint::class.java
            ).sessionManager()
            if (!sessionManager.getAccessToken().isNullOrBlank()) "home"
            else AuthDestination.Welcome.route
        } catch (e: Exception) {
            AuthDestination.Welcome.route
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        addWelcomeDestination(navController)
        addLoginDestination(navController)
        addRegisterDestination(navController)
        addVerifyEmailDestination(navController)
        addProfileSetupDestination(navController)
        addForgotPasswordDestination(navController)
        addHomeDestination(navController, synthesisInviteCode)
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
                    popUpTo(AuthDestination.Welcome.route) {
                        this.inclusive = true
                    }
                    launchSingleTop = true
                }
            },
            onNavigateToProfileSetup = {
                navController.navigate(AuthDestination.ProfileSetup.route) {
                    popUpTo(AuthDestination.Welcome.route) {
                        this.inclusive = false
                    }
                    launchSingleTop = true
                }
            },
            onNavigateToRegister = {
                navController.navigate(AuthDestination.Register.route) {
                    launchSingleTop = true
                }
            },
            // TODO: Реализовать экран восстановления пароля
            onNavigateToForgot = {
                navController.navigate(AuthDestination.ForgotPassword.route) {
                    launchSingleTop = true
                }
            }
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
                    popUpTo(AuthDestination.Welcome.route) {
                        this.inclusive = false
                    }
                    launchSingleTop = true
                }
            },
            onNavigateToHome = {
                navController.navigate("home") {
                    popUpTo(AuthDestination.Welcome.route) {
                        this.inclusive = true
                    }
                    launchSingleTop = true
                }
            },
            onNavigateToProfileSetup = {
                navController.navigate(AuthDestination.ProfileSetup.route) {
                    popUpTo(AuthDestination.Welcome.route) {
                        this.inclusive = false
                    }
                    launchSingleTop = true
                }
            },
            onNavigateToVerifyEmail = {
                navController.navigate(AuthDestination.VerifyEmail.route) {
                    popUpTo(AuthDestination.Welcome.route) {
                        this.inclusive = false
                    }
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
                    popUpTo(AuthDestination.Welcome.route) {
                        this.inclusive = false
                    }
                    launchSingleTop = true
                }
            },
            onNavigateToProfileSetup = {
                navController.navigate(AuthDestination.ProfileSetup.route) {
                    popUpTo(AuthDestination.Welcome.route) {
                        this.inclusive = false
                    }
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
                    popUpTo(AuthDestination.Welcome.route) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        )
    }
}

private fun NavGraphBuilder.addForgotPasswordDestination(navController: NavHostController) {
    composable(
        route = AuthDestination.ForgotPassword.route,
        enterTransition = { fadeIn(animationSpec = tween(150)) },
        exitTransition = { fadeOut(animationSpec = tween(150)) }
    ) {
        ForgotPasswordScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }
}

private fun NavGraphBuilder.addHomeDestination(navController: NavHostController, synthesisInviteCode: String? = null) {
    composable("home") {
        com.example.lumisound.navigation.MainNavGraph(
            startDestination = "home",
            userName = "Александр",
            synthesisInviteCode = synthesisInviteCode
        )
    }
}
