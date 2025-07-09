package com.dailydevchallenge.devstreaks.features.feed

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dailydevchallenge.devstreaks.features.ProgressScreen
import com.dailydevchallenge.devstreaks.features.home.HomeViewModel
import org.koin.compose.koinInject
import com.dailydevchallenge.devstreaks.features.navigation.DevStreakTopBar

@Composable
fun ProgressWithLeaderboardScreen(
    navController: NavController,
    viewModel: HomeViewModel = koinInject()) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("📈 Progress", "🏆 Leaderboard")

    Scaffold(
        topBar = {
            DevStreakTopBar(title = "📊 Your Journey")
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    SecondaryIndicator(
                        Modifier
                            .tabIndicatorOffset(tabPositions[selectedTabIndex])
                            .padding(horizontal = 32.dp),
                        height = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    )
                }
            }

            Crossfade(targetState = selectedTabIndex, modifier = Modifier.fillMaxSize()) { index ->
                when (index) {
                    0 -> ProgressScreen(navController, viewModel)
                    1 -> LeaderboardScreen()
                }
            }
        }
    }
}
