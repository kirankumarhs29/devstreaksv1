package com.dailydevchallenge.devstreaks.features.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dailydevchallenge.devstreaks.features.onboarding.LearningProfile
import com.dailydevchallenge.devstreaks.model.ProfileViewModel
import com.dailydevchallenge.devstreaks.settings.UserPreferences
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: ProfileViewModel = koinInject(),
    onSaveSuccess: () -> Unit,
    navController: NavController
) {
    val profile by viewModel.profile.collectAsState()

    // UI state with validation
    var goal by remember { mutableStateOf("") }
    var skills by remember { mutableStateOf("") }
    var style by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    var timePerDay by remember { mutableStateOf("") }
    var days by remember { mutableStateOf("") }
    var fear by remember { mutableStateOf("") }

    // Validation states
    var goalError by remember { mutableStateOf("") }
    var skillsError by remember { mutableStateOf("") }
    var isFormValid by remember { mutableStateOf(false) }

    // Dropdown states
    var experienceExpanded by remember { mutableStateOf(false) }
    var styleExpanded by remember { mutableStateOf(false) }

    val experienceOptions = listOf("Beginner", "Intermediate", "Advanced", "Expert")
    val styleOptions = listOf("Visual Learner", "Hands-on", "Reading/Research", "Video Tutorials", "Interactive")

    // Form validation
    LaunchedEffect(goal, skills) {
        goalError = when {
            goal.isBlank() -> "Goal is required"
            goal.length < 3 -> "Goal must be at least 3 characters"
            else -> ""
        }

        skillsError = when {
            skills.isBlank() -> "At least one skill is required"
            skills.split(",").any { it.trim().length < 2 } -> "Each skill must be at least 2 characters"
            else -> ""
        }

        isFormValid = goalError.isEmpty() && skillsError.isEmpty() &&
                     goal.isNotBlank() && skills.isNotBlank()
    }

    // Load and bind profile data
    LaunchedEffect(Unit) { viewModel.loadProfile() }
    LaunchedEffect(profile) {
        profile?.let {
            goal = it.goal
            skills = it.skills.joinToString(", ")
            style = it.style
            experience = it.experience
            timePerDay = it.timePerDay
            days = it.days
            fear = it.fear
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "✏️ Edit Profile",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = {
                        if (isFormValid) {
                            val updated = LearningProfile(
                                goal = goal.trim(),
                                skills = skills.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                                style = style,
                                experience = experience,
                                timePerDay = timePerDay,
                                days = days,
                                fear = fear.trim()
                            )
                            viewModel.save(updated)
                            onSaveSuccess()
                        }
                    },
                    enabled = isFormValid,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Save Changes",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section Headers and Improved Fields
            Text(
                "🎯 Learning Goals",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = goal,
                onValueChange = { goal = it },
                label = { Text("Your Learning Goal") },
                placeholder = { Text("e.g., Master Android Development") },
                leadingIcon = {
                    Icon(Icons.Default.Flag, contentDescription = null)
                },
                isError = goalError.isNotEmpty(),
                supportingText = {
                    if (goalError.isNotEmpty()) {
                        Text(goalError, color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("What do you want to achieve?")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = skills,
                onValueChange = { skills = it },
                label = { Text("Skills to Learn") },
                placeholder = { Text("e.g., Kotlin, Jetpack Compose, Firebase") },
                leadingIcon = {
                    Icon(Icons.Default.Code, contentDescription = null)
                },
                isError = skillsError.isNotEmpty(),
                supportingText = {
                    if (skillsError.isNotEmpty()) {
                        Text(skillsError, color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("Separate multiple skills with commas")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Text(
                "📚 Learning Preferences",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // Experience Level Dropdown
            ExposedDropdownMenuBox(
                expanded = experienceExpanded,
                onExpandedChange = { experienceExpanded = !experienceExpanded }
            ) {
                OutlinedTextField(
                    value = experience,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Experience Level") },
                    leadingIcon = {
                        Icon(Icons.Default.School, contentDescription = null)
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = experienceExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = experienceExpanded,
                    onDismissRequest = { experienceExpanded = false }
                ) {
                    experienceOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                experience = option
                                experienceExpanded = false
                            }
                        )
                    }
                }
            }

            // Learning Style Dropdown
            ExposedDropdownMenuBox(
                expanded = styleExpanded,
                onExpandedChange = { styleExpanded = !styleExpanded }
            ) {
                OutlinedTextField(
                    value = style,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Learning Style") },
                    leadingIcon = {
                        Icon(Icons.Default.Psychology, contentDescription = null)
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = styleExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = styleExpanded,
                    onDismissRequest = { styleExpanded = false }
                ) {
                    styleOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                style = option
                                styleExpanded = false
                            }
                        )
                    }
                }
            }

            Text(
                "⏰ Time Management",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = timePerDay,
                onValueChange = { timePerDay = it },
                label = { Text("Time per Day") },
                placeholder = { Text("e.g., 1-2 hours") },
                leadingIcon = {
                    Icon(Icons.Default.AccessTime, contentDescription = null)
                },
                supportingText = { Text("How much time can you dedicate daily?") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = days,
                onValueChange = { days = it },
                label = { Text("Available Days") },
                placeholder = { Text("e.g., Monday to Friday") },
                leadingIcon = {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                },
                supportingText = { Text("Which days work best for you?") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Text(
                "💭 Motivation",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = fear,
                onValueChange = { fear = it },
                label = { Text("Biggest Challenge") },
                placeholder = { Text("e.g., Staying consistent, Complex concepts") },
                leadingIcon = {
                    Icon(Icons.Default.Psychology, contentDescription = null)
                },
                supportingText = { Text("What's your biggest learning challenge?") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
