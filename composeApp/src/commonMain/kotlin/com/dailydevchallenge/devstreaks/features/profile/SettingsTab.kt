package com.dailydevchallenge.devstreaks.features.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dailydevchallenge.devstreaks.auth.AuthService
import com.dailydevchallenge.devstreaks.components.CachedAvatarImage
import com.dailydevchallenge.devstreaks.features.home.HomeViewModel
import com.dailydevchallenge.devstreaks.features.routes.Routes
import com.dailydevchallenge.devstreaks.model.UserInfoViewModel
import com.dailydevchallenge.devstreaks.model.UserStats
import com.dailydevchallenge.devstreaks.notification.getNotificationScheduler
import com.dailydevchallenge.devstreaks.settings.DarkModeSettings
import com.dailydevchallenge.devstreaks.settings.UserPreferences
import org.koin.compose.koinInject

@Composable
fun SettingsTab(
    onLogout: () -> Unit,
    onEditProfile: () -> Unit = {},
    userInfoViewModel: UserInfoViewModel = koinInject(),
    navController: NavController,
    homeViewModel: HomeViewModel = koinInject() // Add HomeViewModel to get real stats
) {
    val profile by userInfoViewModel.profile.collectAsState()
    val profileEditViewModel: ProfileEditViewModel = koinInject()
    val user by profileEditViewModel.user.collectAsState()
    val darkModeEnabled by DarkModeSettings.darkModeFlow.collectAsState()
    val authService: AuthService = koinInject()
//    val coroutineScope = rememberCoroutineScope()

    // Get real stats from HomeViewModel
    val stats by homeViewModel.userStats.collectAsState()

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Show success feedback
    LaunchedEffect(successMessage) {
        successMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            successMessage = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Modern Profile Header
            item {
                ModernProfileHeader(
                    profile = profile,
                    user = user,
                    profileEditViewModel = profileEditViewModel,
                    onEditProfile = onEditProfile,
                    stats = stats // Pass real stats to header
                )
            }

            // Improved Quick Actions Section
            item {
                ImprovedQuickActionsSection(
                    navController = navController,
                    onShowMessage = { message -> successMessage = message }
                )
            }

            // App Preferences Section with feedback
            item {
                AppPreferencesSection(
                    darkModeEnabled = darkModeEnabled,
                    onShowTimePicker = { showTimePickerDialog = true },
                    onShowMessage = { message -> successMessage = message }
                )
            }

            // Account Management Section
            item {
                AccountManagementSection(
                    navController = navController,
                    onLogout = { showLogoutDialog = true }
                )
            }

            // App Information Section
            item {
                AppInformationSection()
            }
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            onConfirm = {
                authService.logout()
                UserPreferences.logout()
                onLogout()
                showLogoutDialog = false
            },
            onDismiss = { showLogoutDialog = false }
        )
    }

    // Time Picker Dialog
    if (showTimePickerDialog) {
        ReminderTimePickerDialog(
            onDismiss = { showTimePickerDialog = false },
            onTimeSelected = { hour, minute ->
                UserPreferences.setReminderTime(hour, minute)
                if (UserPreferences.isNotificationsEnabled()) {
                    getNotificationScheduler().scheduleDailyReminderNotification(hour, minute)
                }
                showTimePickerDialog = false
            }
        )
    }
}

@Composable
fun ModernProfileHeader(
    profile: Any?,
    user: Any?,
    profileEditViewModel: ProfileEditViewModel,
    onEditProfile: () -> Unit,
    stats: UserStats? = null // Add stats parameter
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Gradient background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.primaryContainer
                            ),
                            radius = 400f
                        )
                    )
            )

            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Avatar with edit indicator
                Box(
                    modifier = Modifier.size(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.size(100.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 8.dp
                    ) {
                        when {
                            profileEditViewModel.avatarBytes != null -> {
                                // Show temporary selected avatar
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.AccountCircle,
                                        contentDescription = "Selected Avatar",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(80.dp)
                                    )
                                }
                            }
                            else -> {
                                CachedAvatarImage(
                                    avatarUrl = (user as? com.dailydevchallenge.devstreaks.model.User)?.avatarUrl,
                                    contentDescription = "User Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    loadingSize = 24.dp,
                                    fallbackSize = 60.dp
                                )
                            }
                        }
                    }

                    // Edit indicator
                    Surface(
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.BottomEnd)
                            .clickable { onEditProfile() },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 4.dp
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(8.dp)
                        )
                    }
                }

                // Profile Info with Stats
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Display actual username or learning goal
                    Text(
                        text = (user as? com.dailydevchallenge.devstreaks.model.User)?.username
                            ?: (profile as? com.dailydevchallenge.devstreaks.features.onboarding.LearningProfile)?.goal
                            ?: "DevStreaks Learner",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    // User stats row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        StatItem("🔥", "${stats?.currentStreak ?: 0}", "Day Streak")
                        StatItem("⭐", "${stats?.totalXp ?: 0}", "XP")
                        StatItem("🏆", "Level ${stats?.level ?: 1}", "Progress")
                    }

                    // Dynamic account type based on user level or learning progress
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    ) {
                        val accountType = when {
                            (stats?.level ?: 1) >= 10 -> "🏆 Expert Developer"
                            (stats?.level ?: 1) >= 5 -> "🚀 Advanced Learner"
                            (stats?.totalXp ?: 0) > 100 -> "💪 Active Coder"
                            else -> "🌱 Learning Journey"
                        }
                        Text(
                            text = accountType,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(emoji: String, value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(emoji, fontSize = 12.sp)
            Text(
                value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun QuickActionsSection(
    navController: NavController,
    onEditProfile: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle("⚡ Quick Actions")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Edit,
                title = "Edit Profile",
                subtitle = "Update your learning goals",
                onClick = { onEditProfile() }
            )
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Star,
                title = "Upgrade Pro",
                subtitle = "Premium features",
                onClick = { navController.navigate(Routes.Subscription) }
            )
        }
    }
}

@Composable
fun QuickActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AppPreferencesSection(
    darkModeEnabled: Boolean,
    onShowTimePicker: () -> Unit,
    onShowMessage: (String) -> Unit = {} // Add the missing parameter
) {
    val notificationsEnabled = remember {
        mutableStateOf(UserPreferences.isNotificationsEnabled())
    }
    val currentTime = UserPreferences.getReminderTime()
    val timeText = remember(currentTime) {
        "${currentTime.first.toString().padStart(2, '0')}:${currentTime.second.toString().padStart(2, '0')}"
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle("⚙️ App Preferences")

        ModernSettingsCard {
            PreferenceItem(
                icon = Icons.Default.Notifications,
                title = "Push Notifications",
                subtitle = if (notificationsEnabled.value) "Daily reminders enabled" else "Get reminded to practice",
                isToggle = true,
                toggled = notificationsEnabled.value,
                onToggleChange = { enabled ->
                    notificationsEnabled.value = enabled
                    UserPreferences.setNotificationsEnabled(enabled)
                    if (enabled) {
                        getNotificationScheduler().scheduleDailyReminderNotification(
                            currentTime.first, currentTime.second
                        )
                        onShowMessage("Daily reminders enabled")
                    } else {
                        getNotificationScheduler().cancelDailyReminderNotification()
                        onShowMessage("Daily reminders disabled")
                    }
                }
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                thickness = 0.5.dp
            )

            PreferenceItem(
                icon = if (darkModeEnabled) Icons.Default.DarkMode else Icons.Default.LightMode,
                title = "Dark Mode",
                subtitle = if (darkModeEnabled) "Dark theme active" else "Light theme active",
                isToggle = true,
                toggled = darkModeEnabled,
                onToggleChange = { enabled ->
                    DarkModeSettings.toggleDarkMode(enabled)
                    onShowMessage(if (enabled) "Dark mode enabled" else "Light mode enabled")
                }
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                thickness = 0.5.dp
            )

            PreferenceItem(
                icon = Icons.Default.AccessTime,
                title = "Daily Reminder",
                subtitle = "Remind me at $timeText",
                showChevron = true,
                onClick = onShowTimePicker
            )
        }
    }
}

@Composable
fun AccountManagementSection(
    navController: NavController,
    onLogout: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle("👤 Account")

        ModernSettingsCard {
            PreferenceItem(
                icon = Icons.Default.Security,
                title = "Privacy & Security",
                subtitle = "Manage your data and privacy settings",
                showChevron = true,
                onClick = { /* Navigate to privacy settings */ }
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                thickness = 0.5.dp
            )

            PreferenceItem(
                icon = Icons.Default.Storage,
                title = "Data Export",
                subtitle = "Download your learning progress",
                showChevron = true,
                onClick = { /* Navigate to data export */ }
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                thickness = 0.5.dp
            )

            PreferenceItem(
                icon = Icons.Default.Logout,
                title = "Sign Out",
                subtitle = "Sign out from your account",
                showChevron = true,
                onClick = onLogout,
                textColor = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun AppInformationSection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle("ℹ️ About")

        ModernSettingsCard {
            PreferenceItem(
                icon = Icons.Default.Info,
                title = "Version",
                subtitle = "DevStreak v2.1.0",
                showChevron = false
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                thickness = 0.5.dp
            )

            PreferenceItem(
                icon = Icons.Default.Help,
                title = "Help & Support",
                subtitle = "Get help and contact support",
                showChevron = true,
                onClick = { /* Navigate to help */ }
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                thickness = 0.5.dp
            )

            PreferenceItem(
                icon = Icons.Default.RateReview,
                title = "Rate the App",
                subtitle = "Share your feedback",
                showChevron = true,
                onClick = { /* Navigate to rating */ }
            )
        }
    }
}

@Composable
fun ModernSettingsCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            content = content
        )
    }
}

@Composable
fun PreferenceItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isToggle: Boolean = false,
    toggled: Boolean = false,
    onToggleChange: ((Boolean) -> Unit)? = null,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null || onToggleChange != null) {
                if (isToggle && onToggleChange != null) {
                    onToggleChange(!toggled)
                } else {
                    onClick?.invoke()
                }
            }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        when {
            isToggle && onToggleChange != null -> {
                Switch(
                    checked = toggled,
                    onCheckedChange = onToggleChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                )
            }
            showChevron -> {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
fun LogoutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Logout,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
        },
        title = {
            Text(
                "Sign Out?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                "Are you sure you want to sign out? You'll need to sign in again to access your progress.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Sign Out", color = MaterialTheme.colorScheme.onError)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun ReminderTimePickerDialog(
    onDismiss: () -> Unit,
    onTimeSelected: (Int, Int) -> Unit
) {
    var selectedHour by remember { mutableIntStateOf(UserPreferences.getReminderTime().first) }
    var selectedMinute by remember { mutableIntStateOf(UserPreferences.getReminderTime().second) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.AccessTime,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        },
        title = {
            Text(
                "Set Reminder Time",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Choose when you'd like to be reminded to practice coding",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                // Interactive time picker with increment/decrement buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour picker with controls
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Hour", style = MaterialTheme.typography.labelMedium)

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        selectedHour = if (selectedHour == 23) 0 else selectedHour + 1
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowUp, null, modifier = Modifier.size(16.dp))
                                }

                                Text(
                                    selectedHour.toString().padStart(2, '0'),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )

                                IconButton(
                                    onClick = {
                                        selectedHour = if (selectedHour == 0) 23 else selectedHour - 1
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowDown, null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Text(":", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                    // Minute picker with controls
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Minute", style = MaterialTheme.typography.labelMedium)

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        selectedMinute = if (selectedMinute == 59) 0 else selectedMinute + 1
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowUp, null, modifier = Modifier.size(16.dp))
                                }

                                Text(
                                    selectedMinute.toString().padStart(2, '0'),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )

                                IconButton(
                                    onClick = {
                                        selectedMinute = if (selectedMinute == 0) 59 else selectedMinute - 1
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowDown, null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Quick time presets
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    listOf(
                        "9:00" to Pair(9, 0),
                        "12:00" to Pair(12, 0),
                        "18:00" to Pair(18, 0),
                        "20:00" to Pair(20, 0)
                    ).forEach { (label, time) ->
                        FilterChip(
                            onClick = {
                                selectedHour = time.first
                                selectedMinute = time.second
                            },
                            label = { Text(label, fontSize = 12.sp) },
                            selected = selectedHour == time.first && selectedMinute == time.second,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onTimeSelected(selectedHour, selectedMinute) }
            ) {
                Text("Set Time")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun ImprovedQuickActionsSection(
    navController: NavController,
    onShowMessage: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle("⚡ Quick Actions")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Profile and Upgrade actions
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Edit,
                title = "Edit Profile",
                subtitle = "Update your learning goals",
                onClick = { navController.navigate(Routes.editProfile) }
            )
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Star,
                title = "Upgrade to Pro",
                subtitle = "Unlock premium features",
                onClick = { navController.navigate(Routes.Subscription) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // New Quick Actions with Activity Feed
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Timeline,
                title = "Activity Feed",
                subtitle = "View your learning progress",
                onClick = {
                    navController.navigate("activity_feed")
                }
            )
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Code,
                title = "Daily Challenge",
                subtitle = "Practice coding problems",
                onClick = {
                    onShowMessage("Daily Challenge started!")
                    // TODO: Implement daily challenge navigation
                }
            )
        }
    }
}
