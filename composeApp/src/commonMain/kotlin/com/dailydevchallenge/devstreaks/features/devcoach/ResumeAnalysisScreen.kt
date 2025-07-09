package com.dailydevchallenge.devstreaks.features.devcoach

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dailydevchallenge.devstreaks.model.InterviewQuestion
import com.dailydevchallenge.devstreaks.model.ResumeAnalysis
import org.koin.compose.koinInject
import com.dailydevchallenge.devstreaks.llm.LLMService
import com.dailydevchallenge.devstreaks.utils.getPdfPickerHandler
import kotlinx.coroutines.launch


@Composable
fun ResumeAndInterviewScreen(
    resumeChatViewModel: ResumeChatViewModel = koinInject()
) {
    var showPicker by remember { mutableStateOf(false) }
    var resumeText by remember { mutableStateOf("") }
    var jobRole by remember { mutableStateOf("") }

    var analysis by remember { mutableStateOf<ResumeAnalysis?>(null) }
    var questions by remember { mutableStateOf<List<InterviewQuestion>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val llmService = koinInject<LLMService>()
    val coroutineScope = rememberCoroutineScope()

    if (showPicker) {
        getPdfPickerHandler().PickPdf {
            resumeChatViewModel.setResumeText(it)
            resumeText = it
            showPicker = false
        }
    }

    Column(
       modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("🎯 Resume Analysis + Mock Interview", style = MaterialTheme.typography.titleLarge)

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = jobRole,
            onValueChange = { jobRole = it },
            label = { Text("Enter Target Job Role") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Button(onClick = { showPicker = true }) {
            Text("📄 Pick Resume PDF")
        }

        if (resumeText.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text("✅ Resume Loaded", color = Color.Green)
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                if (resumeText.isNotBlank() && jobRole.isNotBlank()) {
                    isLoading = true
                    analysis = null
                    questions = emptyList()
                    // launch async tasks
                    coroutineScope.launch {
                        try {
                            val a = llmService.analyzeResume(resumeText, jobRole)
                            val q = llmService.generateMockInterview(jobRole, a.summary, a.skillsMatched)
                            analysis = a
                            questions = q
                        } catch (e: Exception) {
                            analysis = null
                            questions = emptyList()
                        } finally {
                            isLoading = false
                        }
                    }
                }
            },
            enabled = resumeText.isNotBlank() && jobRole.isNotBlank()
        ) {
            Text("🚀 Analyze & Interview Me")
        }

        Spacer(Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            ResumeAnalysisSection(analysis)
            InterviewQuestionList(questions)
        }
    }
}


@Composable
fun ResumeAnalysisSection(analysis: ResumeAnalysis?) {
    analysis?.let {
        Column {
            Text("📝 Resume Analysis", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text("🎯 Role Fit: ${it.jobMatchScore}")
            Text("🧠 Summary: ${it.summary}")
            Text("✅ Strengths: ${it.skillsMatched.joinToString()}")
            Text("⚠️ Weaknesses: ${it.skillsMissing.joinToString()}")
            Text("📚 Recommended Skills: ${it.recommendations.joinToString()}")
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun InterviewQuestionList(questions: List<InterviewQuestion>) {
    if (questions.isNotEmpty()) {
        Text("🎤 Mock Interview Questions", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        LazyColumn {
           items(
                questions.size,
                key = { questions[it].question }
            ) { index ->
                val question = questions[index]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Q${index + 1}: ${question.question}", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(8.dp))
                        Text("Type: ${question.type}", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        Text("Expected Answer: ${question.expectedAnswer}", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(4.dp))
                        Text("Follow-up: ${question.followUp}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

