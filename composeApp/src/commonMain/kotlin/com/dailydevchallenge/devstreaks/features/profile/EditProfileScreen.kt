package com.dailydevchallenge.devstreaks.features.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dailydevchallenge.devstreaks.features.onboarding.LearningProfile
import com.dailydevchallenge.devstreaks.model.ProfileViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import com.dailydevchallenge.devstreaks.settings.UserPreferences


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController
) {
    val userId = UserPreferences.getSafeUserId()
    val viewModel: ProfileViewModel = koinInject(parameters = { parametersOf(userId) })
    // to ViewModel

    val profile by viewModel.profile.collectAsState() // ✅ use collectAsState if it's a Flow or MutableStateFlow


    // UI state...
    var goal by remember { mutableStateOf("") }
    var skills by remember { mutableStateOf("") }
    var style by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    var timePerDay by remember { mutableStateOf("") }
    var days by remember { mutableStateOf("") }
    var fear by remember { mutableStateOf("") }

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
                title = { Text("📝 Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    val updated = LearningProfile(
                        goal = goal,
                        skills = skills.split(",").map { it.trim() },
                        style = style,
                        experience = experience,
                        timePerDay = timePerDay,
                        days = days,
                        fear = fear
                    )
                    viewModel.save(updated)
                    navController.popBackStack()
                },
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                Text("Save & Back")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(value = goal, onValueChange = { goal = it }, label = { Text("Goal") })
            OutlinedTextField(value = skills, onValueChange = { skills = it }, label = { Text("Skills") })
            OutlinedTextField(value = style, onValueChange = { style = it }, label = { Text("Style") })
            OutlinedTextField(value = experience, onValueChange = { experience = it }, label = { Text("Experience") })
            OutlinedTextField(value = timePerDay, onValueChange = { timePerDay = it }, label = { Text("Time/day") })
            OutlinedTextField(value = days, onValueChange = { days = it }, label = { Text("Days") })
            OutlinedTextField(value = fear, onValueChange = { fear = it }, label = { Text("Biggest Fear") })
        }
    }
}

