package com.dailydevchallenge.devstreaks.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.dailydevchallenge.devstreaks.features.home.HomeViewModel
import com.dailydevchallenge.devstreaks.features.onboarding.LearningProfile
import com.dailydevchallenge.devstreaks.model.ProfileViewModel
import com.dailydevchallenge.devstreaks.model.UserInfoViewModel
import com.dailydevchallenge.devstreaks.settings.DarkModeSettings
import com.dailydevchallenge.devstreaks.settings.UserPreferences
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    viewModel: HomeViewModel = koinInject()
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Feed", "Settings")

    val tabHeaderHeightPx = remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val userId = UserPreferences.getSafeUserId()
    val profileViewModel: ProfileViewModel = koinInject(parameters = { parametersOf(userId) })
    val profileEditViewModel: ProfileEditViewModel = koinInject(parameters = { parametersOf(userId) })
    val userInfoViewModel: UserInfoViewModel = koinInject()
    val learningProfile by profileViewModel.profile.collectAsState()
    val userProfile by profileEditViewModel.user.collectAsState()
    val userInfo by userInfoViewModel.profile.collectAsState()
    var isEditingProfile by remember { mutableStateOf(false) }

    if (isEditingProfile) {
        ProfileEditScreen(
            viewModel = koinInject(),
            onSaveSuccess = { isEditingProfile = false }
        )
        return
    }


    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .onGloballyPositioned { coordinates ->
                    tabHeaderHeightPx.value = coordinates.size.height
                }
        ) {
            learningProfile?.let { ProfileHeader(viewModel, it, onEditProfile = { isEditingProfile = true }) }

            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = with(density) { tabHeaderHeightPx.value.toDp() })
        ) {
            when (selectedTabIndex) {
                0 -> FeedTab()
                1 -> SettingsTab(onLogout = onLogout)
            }
        }
    }
}


@Composable
fun ProfileHeader(
    viewModel: HomeViewModel,
    learningProfile: LearningProfile,
    onEditProfile: () -> Unit
) {
    val stats by viewModel.userStats.collectAsState()
    val level = stats.xp / 100
    val isDark by DarkModeSettings.darkModeFlow.collectAsState()
    val accent = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    if (isDark)
                        listOf(Color(0xFF1C1C1E), Color(0xFF121212))
                    else
                        listOf(Color(0xFFCCE0FF), Color.White)
                )
            )
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile",
                    tint = accent,
                    modifier = Modifier.size(72.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stats.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = accent,
                tonalElevation = 6.dp
            ) {
                Text(
                    text = "Level $level • ${stats.xp} XP",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "🔥 Streak: ${stats.streak} days",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "🎯 Goal: ${learningProfile.goal}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "🛠 Skills: ${learningProfile.skills.joinToString()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onEditProfile,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Edit Profile", color = Color.White)
            }
        }
    }
}
