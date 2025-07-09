package com.dailydevchallenge.devstreaks.features.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import androidx.navigation.toRoute
import com.dailydevchallenge.devstreaks.data.ChallengeDetail
import com.dailydevchallenge.devstreaks.features.routes.LearnRoute
import com.dailydevchallenge.devstreaks.features.onboarding.LearningProfile
import com.dailydevchallenge.devstreaks.features.routes.PathDetail
import com.dailydevchallenge.devstreaks.settings.UserPreferences
import com.dailydevchallenge.devstreaks.features.routes.Routes
import com.dailydevchallenge.devstreaks.features.home.HomeScreen
import com.dailydevchallenge.devstreaks.features.challenge.ChallengeDetailScreen
import com.dailydevchallenge.devstreaks.features.challenge.ChallengeFlowScreen
import com.dailydevchallenge.devstreaks.features.path.PathDetailScreen
import com.dailydevchallenge.devstreaks.features.path.PathsScreen
import com.dailydevchallenge.devstreaks.features.journal.MyDayScreen
import com.dailydevchallenge.devstreaks.features.profile.ProfileScreen
import com.dailydevchallenge.devstreaks.features.challenge.ChallengePathScreen
import com.dailydevchallenge.devstreaks.features.devcoach.ConversationHistoryScreen
import com.dailydevchallenge.devstreaks.features.devcoach.DevCoachScreen
import com.dailydevchallenge.devstreaks.features.devcoach.ResumeAndInterviewScreen
import com.dailydevchallenge.devstreaks.features.devcoach.ResumeChatViewModel
import com.dailydevchallenge.devstreaks.features.feed.ProgressWithLeaderboardScreen
import com.dailydevchallenge.devstreaks.features.home.HomeViewModel
import com.dailydevchallenge.devstreaks.features.onboarding.LearningIntentScreen
import com.dailydevchallenge.devstreaks.features.onboarding.OnboardingViewModel
import com.dailydevchallenge.devstreaks.features.profile.EditProfileScreen
import com.dailydevchallenge.devstreaks.model.ChallengeTask
import com.dailydevchallenge.devstreaks.repository.ChallengeRepository
import com.dailydevchallenge.devstreaks.utils.getLogger
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun MainScaffold(
    onLogout: () -> Unit,
    launchDestination: String? = null
) {
    val navController = rememberNavController()
    val onboardingViewModel: OnboardingViewModel = koinInject()
    val homeViewModel: HomeViewModel = koinInject()
    val resumeChatViewModel: ResumeChatViewModel = koinInject()
//    var shouldNavigateBack by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
//    var hasNavigatedToChallengePath by remember { mutableStateOf(false) }
    val logger = remember { getLogger() }

// this will navigate to the challenge path screen if it exists
//    val uiState by onboardingViewModel.uiState.collectAsState()
//
//    LaunchedEffect(uiState.challengePath) {
//        if (uiState.challengePath != null) {
//            navController.navigate(Routes.ChallengePath) {
//                popUpTo(Routes.HomeScreen) { inclusive = true }
//            }
//        }
//    }
    // use this for Notification launch
    LaunchedEffect(launchDestination) {
        logger.log("Launch destination = $launchDestination")
        // Wait until NavHost is ready
        navController.currentBackStackEntryFlow.collect {
            when (launchDestination) {
                "login" -> navController.navigate(Routes.HomeScreen)
                "course" -> navController.navigate(Routes.ChallengePath)
                "xp" -> navController.navigate(Routes.Progress)
            }
        }
    }

    Scaffold(
        bottomBar = { BottomNavBar(navController) }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .consumeWindowInsets(paddingValues)) {
            NavHost(
                navController = navController,
                startDestination = Routes.HomeScreen,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Routes.HomeScreen) {
                    HomeScreen(navController, viewModel = koinInject())
                }


                composable(Routes.Paths) {
                    PathsScreen(navController)
                }
                composable(Routes.Journal) { MyDayScreen() }
                composable(Routes.Progress) {
                    ProgressWithLeaderboardScreen(navController,viewModel = homeViewModel)
                }
                composable(Routes.Profile) {
                    ProfileScreen(onLogout = onLogout)
                }



                composable(Routes.LearningIntent) {
                    LearningIntentScreen(
                        viewModel = onboardingViewModel,
                        navController = navController,
                        onFinish = { goal, skills, experience, time, days, style, fear ->
                            coroutineScope.launch {
                                logger.log("Learning Intent submitted: $goal, $skills, $experience, $time, $days, $style, $fear")
                                onboardingViewModel.submitIntent(
                                    LearningProfile(
                                        goal = goal,
                                        skills = skills,
                                        experience = experience,
                                        timePerDay = time,
                                        days = days,
                                        style = style,
                                        fear = fear
                                    )
                                )
                            }
                        }
                    )
                }

                composable(Routes.ChallengePath) {
                    val uiState by onboardingViewModel.uiState.collectAsState()
                    logger.log("Navigating to ChallengePath with state: ${uiState.challengePath}")

                    if (uiState.challengePath != null) {
                        ChallengePathScreen(
                            navController = navController,
                            challengePath = uiState.challengePath!!,
                            onStartDayClick = { /* handle click if needed */ }
                        )
                    } else {
                        // Optional: Loading UI
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }

                composable<PathDetail> { backStackEntry ->
                    val route = backStackEntry.toRoute<PathDetail>()
                    PathDetailScreen(navController, route.pathId)
                }

                composable<LearnRoute> { backStackEntry ->
                    val route = backStackEntry.toRoute<LearnRoute>()
                    val task = remember(route.taskId) {
                        homeViewModel.getTaskByIdSync(route.taskId)
                    }

                    task?.let { challenge ->
                        ChallengeFlowScreen(
                            task = challenge,
                            navController = navController,
                            isCompleted = homeViewModel.isTaskCompletedSync(challenge.id), // Optional sync call or set false
                            onMarkAsDone = {
                                coroutineScope.launch {
                                    homeViewModel.markTaskCompleted(challenge.id, challenge.xp)
                                }
                            }
                        )
                    } ?: run {
                        Text("Challenge not found", modifier = Modifier.padding(24.dp))
                    }
                }

                composable(Routes.DevCoach) {
                    DevCoachScreen(
                        navController = navController,
                        onBack = { navController.popBackStack() })
                }
                composable(Routes.clearDevChat) {
                    ConversationHistoryScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.editProfile) {
                    EditProfileScreen(
                        navController
                    )
                }




                // Important: define the route with argument placeholder
                composable<ChallengeDetail> { backStackEntry ->
                    val detail = backStackEntry.toRoute<ChallengeDetail>()
                    val challengeRepo = koinInject<ChallengeRepository>()
                    var shouldNavigateToHome by remember { mutableStateOf(false) }

                    val task by produceState<ChallengeTask?>(initialValue = null) {
                        value = challengeRepo.getTaskById(detail.challengeId)
                    }
                    val userId = UserPreferences.getSafeUserId()

                    val isCompleted by produceState(initialValue = false) {
                        value = challengeRepo.isTaskCompleted(detail.challengeId, userId)
                    }

                    task?.let { challenge ->
                        ChallengeDetailScreen(
                            day = challenge,
                            isCompleted = isCompleted,
                            onMarkAsDone = {
                                coroutineScope.launch {
                                    homeViewModel.markTaskCompleted(challenge.id, challenge.xp)
                                    shouldNavigateToHome = true
                                }
                            },
                            navController = navController
                        )
                    } ?: Text("Challenge not found")

                    if (shouldNavigateToHome) {
                        LaunchedEffect(Unit) {
                            navController.navigate(Routes.HomeScreen) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                }
                composable(Routes.ResumeAnalysis) {
                    ResumeAndInterviewScreen(
                        resumeChatViewModel
                    )
                }

            }
        }
    }
}
