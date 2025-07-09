package com.dailydevchallenge.devstreaks.features.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import devstreaks.composeapp.generated.resources.Res
import devstreaks.composeapp.generated.resources.onbrd1
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.*


@OptIn(ExperimentalResourceApi::class)
@Composable
fun SplashScreen(navController: NavController) {
    // Trigger navigation after 2 seconds
    LaunchedEffect(Unit) {
        delay(2000) // Wait 2 seconds
//        if (OnboardingPreferences.isOnboardingCompleted()) {
//            navController.navigate(Routes.HomeScreen) {
//                popUpTo(Routes.SplashScreen) { inclusive = true }
//            }
//        } else {
//            navController.navigate(Routes.OnboardingScreen) {
//                popUpTo(Routes.SplashScreen) { inclusive = true }
//            }
//        }
    }

    // UI Content
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(Res.drawable.onbrd1),
                contentDescription = "Splash Logo",
                modifier = Modifier.size(150.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("DevSpark", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Level up. One challenge at a time.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
