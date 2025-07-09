package com.dailydevchallenge.devstreaks.features.challenge.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.dailydevchallenge.devstreaks.llm.LLMService
import com.dailydevchallenge.devstreaks.model.ChallengeActivity
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun CodeChallengeCard(activity: ChallengeActivity, onViewed: () -> Unit) {
    var userCode by remember { mutableStateOf(activity.starterCode ?: "") }
    var showExplanation by remember { mutableStateOf(false) }
    var aiFeedback by remember { mutableStateOf<String?>(null) }
    var isReviewing by remember { mutableStateOf(false) }
    val llmService: LLMService = koinInject()
    val coroutineScope = rememberCoroutineScope() // ✅ Correct way
    LaunchedEffect(Unit) {
        onViewed()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("💻 Code Challenge", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(activity.prompt, style = MaterialTheme.typography.bodyLarge)

            Spacer(Modifier.height(12.dp))
            Text("Language: ${activity.language ?: "Any"}", style = MaterialTheme.typography.labelSmall)

            Spacer(Modifier.height(12.dp))
            Text("🧑‍💻 Your Code:", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))

            Surface(
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp)
            ) {
                BasicTextField(
                    value = userCode,
                    onValueChange = { userCode = it },
                    textStyle = TextStyle.Default.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { showExplanation = true },
                shape = MaterialTheme.shapes.small
            ) {
                Text("🔍 Check Explanation")
            }
            Button(
                onClick = {
                    isReviewing = true
                    aiFeedback = null
                    // Correct coroutine usage
                    coroutineScope.launch {
                        val response = llmService.reviewCode(activity.prompt, activity.language, userCode)
                        aiFeedback = response
                        isReviewing = false
                    }
                },
                shape = MaterialTheme.shapes.small
            ) {
                Text("🤖 Review My Code")
            }

            if (showExplanation && !activity.explanation.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text("💡 ${activity.explanation}", style = MaterialTheme.typography.bodySmall)
            }
            if (isReviewing) {
                Spacer(Modifier.height(12.dp))
                Text("⏳ Reviewing your code with AI...", style = MaterialTheme.typography.bodySmall)
            }

            if (!aiFeedback.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("🧠 AI Feedback", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(aiFeedback!!, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
