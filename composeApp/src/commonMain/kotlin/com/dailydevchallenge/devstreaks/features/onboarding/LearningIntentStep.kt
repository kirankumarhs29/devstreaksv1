package com.dailydevchallenge.devstreaks.features.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dailydevchallenge.devstreaks.model.LearningIntent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningIntentStep(
    currentIntent: LearningIntent?,
    onIntentUpdate: (LearningIntent) -> Unit
) {
    var intent by remember {
        mutableStateOf(currentIntent ?: LearningIntent())
    }

    // Update parent whenever intent changes
    LaunchedEffect(intent) {
        onIntentUpdate(intent)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(vertical = 24.dp)
    ) {
        item {
            HeaderCard()
        }

        item {
            PrimaryGoalSection(
                currentGoal = intent.primaryGoal,
                onGoalChange = { intent = intent.copy(primaryGoal = it) }
            )
        }

        item {
            SkillFocusSection(
                selectedSkills = intent.skillFocus,
                onSkillsChange = { intent = intent.copy(skillFocus = it) }
            )
        }

        item {
            CareerTrackSection(
                currentTrack = intent.careerTrack,
                onTrackChange = { intent = intent.copy(careerTrack = it) }
            )
        }

        item {
            ExperienceSection(
                currentExperience = intent.experience,
                onExperienceChange = { intent = intent.copy(experience = it) }
            )
        }

        item {
            TimeCommitmentSection(
                currentTime = intent.timePerDay,
                onTimeChange = { intent = intent.copy(timePerDay = it) }
            )
        }

        item {
            LearningStyleSection(
                currentStyle = intent.learningStyle,
                onStyleChange = { intent = intent.copy(learningStyle = it) }
            )
        }
    }
}

@Composable
private fun HeaderCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🎯",
                style = MaterialTheme.typography.displaySmall
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Let's personalize your journey",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Tell us about your goals so we can create the perfect learning path for you",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PrimaryGoalSection(
    currentGoal: String,
    onGoalChange: (String) -> Unit
) {
    val goals = listOf(
        "Land my first developer job",
        "Switch to a tech career",
        "Get promoted at work",
        "Learn a new programming language",
        "Build my own startup",
        "Improve coding skills",
        "Prepare for technical interviews",
        "Build a portfolio project"
    )

    SectionCard(
        title = "What's your main goal?",
        icon = Icons.Default.Flag
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(goals) { goal ->
                FilterChip(
                    selected = currentGoal == goal,
                    onClick = { onGoalChange(goal) },
                    label = { Text(goal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        if (currentGoal.isNotEmpty() && currentGoal !in goals) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = currentGoal,
                onValueChange = onGoalChange,
                label = { Text("Custom goal") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                onGoalChange(if (currentGoal in goals) "" else "Custom goal")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add custom goal")
        }
    }
}

@Composable
private fun SkillFocusSection(
    selectedSkills: List<String>,
    onSkillsChange: (List<String>) -> Unit
) {
    val skillCategories = mapOf(
        "Frontend" to listOf("React", "Vue.js", "Angular", "HTML/CSS", "JavaScript", "TypeScript"),
        "Backend" to listOf("Node.js", "Python", "Java", "C#", "Go", "Ruby", "PHP"),
        "Mobile" to listOf("React Native", "Flutter", "Swift", "Kotlin", "Xamarin"),
        "Data & AI" to listOf("Python", "SQL", "Machine Learning", "Data Analysis", "TensorFlow"),
        "DevOps" to listOf("Docker", "Kubernetes", "AWS", "CI/CD", "Linux", "Git")
    )

    SectionCard(
        title = "Which skills do you want to focus on?",
        subtitle = "Select up to 5 skills",
        icon = Icons.Default.Code
    ) {
        skillCategories.forEach { (category, skills) ->
            Text(
                text = category,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(skills) { skill ->
                    val isSelected = skill in selectedSkills
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) {
                                onSkillsChange(selectedSkills - skill)
                            } else if (selectedSkills.size < 5) {
                                onSkillsChange(selectedSkills + skill)
                            }
                        },
                        label = { Text(skill) },
                        enabled = isSelected || selectedSkills.size < 5,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                        )
                    )
                }
            }

            if (category != skillCategories.keys.last()) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun CareerTrackSection(
    currentTrack: String,
    onTrackChange: (String) -> Unit
) {
    val tracks = listOf(
        "Frontend Developer",
        "Backend Developer",
        "Full Stack Developer",
        "Mobile Developer",
        "Data Scientist",
        "DevOps Engineer",
        "Product Manager",
        "Engineering Manager"
    )

    SectionCard(
        title = "What's your target career track?",
        icon = Icons.Default.Work
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(tracks) { track ->
                FilterChip(
                    selected = currentTrack == track,
                    onClick = { onTrackChange(track) },
                    label = { Text(track) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                        selectedLabelColor = MaterialTheme.colorScheme.onTertiary
                    )
                )
            }
        }
    }
}

@Composable
private fun ExperienceSection(
    currentExperience: String,
    onExperienceChange: (String) -> Unit
) {
    val experiences = listOf(
        "Complete beginner",
        "Some coding experience",
        "1-2 years experience",
        "3-5 years experience",
        "5+ years experience"
    )

    SectionCard(
        title = "What's your current experience level?",
        icon = Icons.Default.Psychology
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            experiences.forEach { experience ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = currentExperience == experience,
                            onClick = { onExperienceChange(experience) }
                        )
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentExperience == experience,
                        onClick = { onExperienceChange(experience) }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = experience,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeCommitmentSection(
    currentTime: Int,
    onTimeChange: (Int) -> Unit
) {
    val timeOptions = listOf(15, 30, 45, 60, 90, 120)

    SectionCard(
        title = "How much time can you dedicate daily?",
        icon = Icons.Default.Schedule
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(timeOptions) { time ->
                FilterChip(
                    selected = currentTime == time,
                    onClick = { onTimeChange(time) },
                    label = { Text("${time}min") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    }
}

@Composable
private fun LearningStyleSection(
    currentStyle: String,
    onStyleChange: (String) -> Unit
) {
    val styles = listOf(
        "Hands-on projects",
        "Step-by-step tutorials",
        "Video explanations",
        "Reading documentation",
        "Interactive challenges",
        "Peer collaboration"
    )

    SectionCard(
        title = "How do you prefer to learn?",
        icon = Icons.Default.School
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(styles) { style ->
                FilterChip(
                    selected = currentStyle == style,
                    onClick = { onStyleChange(style) },
                    label = { Text(style) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                    )
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    subtitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            content()
        }
    }
}
