package com.dailydevchallenge.devstreaks.features.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import com.dailydevchallenge.devstreaks.utils.getLogger
import com.dailydevchallenge.devstreaks.utils.logAnalyticsEvent

@Composable
fun OnboardingScreen(
    navController: NavController,
    onSignupNavigate: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 4 })
    val logger = remember { getLogger() }


    // Log screen load once
    LaunchedEffect(Unit) {
        logger.log("Screen viewed: OnboardingScreen")
        logAnalyticsEvent("screen_viewed", mapOf("screen" to "OnboardingScreen"))
    }

    // Log page changes
    LaunchedEffect(pagerState.currentPage) {
        logger.log("Onboarding page viewed: page ${pagerState.currentPage}")
        logAnalyticsEvent("page_swiped", mapOf("page" to pagerState.currentPage + 1))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            when (page) {
                0 -> OnboardingPage(
                    icon = "🚀",
                    title = "This isn’t just coding.",
                    subtitle = "It’s your transformation journey."
                )
                1 -> OnboardingPage(
                    icon = "📅",
                    title = "Tiny wins every day.",
                    subtitle = "Streaks build habits. Habits build success."
                )
                2 -> OnboardingPage(
                    icon = "🏆",
                    title = "DevStreak = Your XP Tracker",
                    subtitle = "Track your progress. Stay accountable. Own it."
                )
                3 -> OnboardingPage(
                    icon = "🎯",
                    title = "Ready to Begin?",
                    subtitle = "Let’s create your account & start your streak.",
                    showContinue = true,
                    onContinueClick = {
                        logger.log("User clicked 'Continue' on onboarding")
                        logAnalyticsEvent("onboarding_continue_clicked")
                        logAnalyticsEvent("page_swiped", mapOf("page" to 1))
                        onSignupNavigate()
                    }
                )
            }
        }

        OnboardingProgressBar(
            currentPage = pagerState.currentPage,
            totalPages = 4
        )
    }
}


@Composable
fun OnboardingPage(
    icon: String,
    title: String,
    subtitle: String,
    showContinue: Boolean = false,
    onContinueClick: (() -> Unit)? = null
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = icon,
            fontSize = 72.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        if (showContinue && onContinueClick != null) {
            Spacer(modifier = Modifier.height(36.dp))
            Button(
                onClick = onContinueClick,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(48.dp)
            ) {
                Text("Continue →")
            }
        }
    }
}

@Composable
fun OnboardingProgressBar(
    currentPage: Int,
    totalPages: Int
) {
    val progress = (currentPage + 1).toFloat() / totalPages

    val backgroundTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    val filledColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .padding(horizontal = 32.dp)
            .clip(RoundedCornerShape(50))
            .background(backgroundTrackColor)
    ) {
        LinearProgressIndicator(
            progress = { progress },
            color = filledColor,
            trackColor = Color.Transparent,
            modifier = Modifier
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
        )
    }
}
