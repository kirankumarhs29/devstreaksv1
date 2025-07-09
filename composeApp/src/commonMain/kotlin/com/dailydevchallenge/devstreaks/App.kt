    package com.dailydevchallenge.devstreaks

    import androidx.compose.runtime.*
    import androidx.compose.runtime.Composable
    import androidx.navigation.compose.rememberNavController
    import com.dailydevchallenge.devstreaks.theme.DevStreakTheme

    import androidx.navigation.compose.*
    import com.dailydevchallenge.devstreaks.features.navigation.MainScaffold
    import com.dailydevchallenge.devstreaks.features.routes.Routes
    import com.dailydevchallenge.devstreaks.features.screens.OnboardingScreen
    import com.dailydevchallenge.devstreaks.features.screens.SplashScreen
    import com.dailydevchallenge.devstreaks.auth.AuthService
    import com.dailydevchallenge.devstreaks.features.auth.*
    import com.dailydevchallenge.devstreaks.features.onboarding.OnboardingPreferences

    import com.dailydevchallenge.devstreaks.utils.getLogger
    import com.dailydevchallenge.devstreaks.utils.logAnalyticsEvent
    import org.koin.compose.getKoin


    @Composable
    fun App(
        authService: AuthService = getKoin().get(),
        launchDestination: String? = null
    ) {
        DevStreakTheme {
            val navController = rememberNavController()
            val onboardingCompleted by OnboardingPreferences.onboardingFlow.collectAsState()
            val logger = remember { getLogger() }

            // Determine start screen
            val startDestination = when {
                !onboardingCompleted -> Routes.OnboardingScreen
                !authService.isLoggedIn() -> Routes.Login
                else -> Routes.MainScaffold
            }

            // Log app start
            LaunchedEffect(Unit) {
                logger.log("App started with destination: $startDestination")
                logAnalyticsEvent("app_started", mapOf("start_destination" to startDestination))
            }

            NavHost(navController = navController, startDestination = startDestination) {
                composable(Routes.SplashScreen) {
                    SplashScreen(navController)
                    logger.log("Navigated to SplashScreen")
                    logAnalyticsEvent("screen_viewed", mapOf("screen" to "SplashScreen"))
                }

                composable(Routes.OnboardingScreen) {
                    OnboardingScreen(
                        navController = navController,
                        onSignupNavigate = {
                            OnboardingPreferences.setOnboardingCompleted(true)
                            navController.navigate(Routes.Signup) {
                                popUpTo(Routes.OnboardingScreen) { inclusive = true }
                            }
                            logger.log("Onboarding completed → navigating to Signup")
                            logAnalyticsEvent("onboarding_completed")
                        }
                    )
                    logger.log("Navigated to OnboardingScreen")
                    logAnalyticsEvent("screen_viewed", mapOf("screen" to "OnboardingScreen"))
                }

                composable(Routes.Login) {
                    LoginScreen(
                        navController = navController,
                        onLoginSuccess = {
                            navController.navigate(Routes.MainScaffold) {
                                popUpTo(Routes.Login) { inclusive = true }
                            }
                            logger.log("Login successful → MainScaffold")
                            logAnalyticsEvent("login_success")
                        },
                        onSignupClick = {
                            navController.navigate(Routes.Signup)
                            logger.log("Navigated from Login → Signup")
                            logAnalyticsEvent("login_signup_clicked")
                        }
                    )
                    logger.log("Navigated to LoginScreen")
                    logAnalyticsEvent("screen_viewed", mapOf("screen" to "LoginScreen"))
                }

                composable(Routes.ForgotPassword) {
                    ForgotPasswordScreen(
                        onLogin = {
                            navController.navigate(Routes.Login)
                            logger.log("Forgot password → back to Login")
                            logAnalyticsEvent("forgot_password_back_to_login")
                        }
                    )
                    logger.log("Navigated to ForgotPasswordScreen")
                    logAnalyticsEvent("screen_viewed", mapOf("screen" to "ForgotPasswordScreen"))
                }

                composable(Routes.Signup) {
                    SignupScreen(
                        onSignupSuccess = {
                            navController.navigate(Routes.Login) {
                                popUpTo(Routes.Signup) { inclusive = true }
                            }
                            logger.log("Signup successful → Login")
                            logAnalyticsEvent("signup_success")
                        },
                        onLoginClick = {
                            navController.navigate(Routes.Login)
                            logger.log("Navigated from Signup → Login")
                            logAnalyticsEvent("signup_login_clicked")
                        }
                    )
                    logger.log("Navigated to SignupScreen")
                    logAnalyticsEvent("screen_viewed", mapOf("screen" to "SignupScreen"))
                }

                composable(Routes.MainScaffold) {
                    MainScaffold(
                        onLogout = {
                            navController.navigate(Routes.Login) {
                                popUpTo(Routes.MainScaffold) { inclusive = true }
                            }
                            logger.log("Logout → back to Login")
                            logAnalyticsEvent("logout")
                        },
                        launchDestination = launchDestination
                    )
                    logger.log("Navigated to MainScaffold")
                    logAnalyticsEvent("screen_viewed", mapOf("screen" to "MainScaffold"))
                }
            }
        }
    }
