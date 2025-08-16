package com.dailydevchallenge.devstreaks.features.dailyCoach

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dailydevchallenge.devstreaks.model.ChallengeActivity
import com.dailydevchallenge.devstreaks.model.ChallengeTask


data class AIChallengeContent(
    val topic: String,
    val lesson: String,
    val codeExample: String,
    val quiz: List<QuizQuestion>,
    val challenge: String,
    val expectedOutcome: String,
    val reflectionPrompt: String = "What did you learn or struggle with?"
)

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswer: String
)
data class AILesson(
    val title: String,
    val concept: String,
    val codeSnippet: String,
    val quiz: List<QuizQuestion>,
    val devTip: String
)

@Composable
fun AILessonCard(content: ChallengeActivity, onComplete: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text("📘 Lesson", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        // take user input and generate content
        val aiContent = AIChallengeContentGenerator.generateContent(content.prompt)
        Text(aiContent.lesson, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onComplete) { Text("Next") }
    }
}


object AIChallengeContentGenerator {

    fun generateContent(topic: String): AIChallengeContent {
        return when (topic.lowercase()) {

            "kotlin when" -> AIChallengeContent(
                topic = "Kotlin `when` Expression",
                lesson = "The `when` expression in Kotlin is used as a cleaner alternative to `if-else`. It matches values or conditions and can be used as an expression that returns a result.",
                codeExample = """
                    fun getMood(hour: Int): String {
                        return when (hour) {
                            in 0..5 -> "Sleepy"
                            in 6..11 -> "Productive"
                            in 12..17 -> "Tired"
                            else -> "Relaxing"
                        }
                    }
                """.trimIndent(),
                quiz = listOf(
                    QuizQuestion(
                        question = "What does `when` replace in Kotlin?",
                        options = listOf("for-loop", "switch-case", "try-catch", "class declaration"),
                        correctAnswer = "switch-case"
                    ),
                    QuizQuestion(
                        question = "What will `getMood(3)` return in the above example?",
                        options = listOf("Productive", "Sleepy", "Tired", "Relaxing"),
                        correctAnswer = "Sleepy"
                    )
                ),
                challenge = """
                    Fix the following code:

                    fun checkGrade(grade: Char): String {
                        when (grade) {
                            'A' -> return "Excellent"
                            'B' -> return "Good"
                            'C' -> return "Average"
                            else -> return "Fail"
                        }
                    }
                """.trimIndent(),
                expectedOutcome = "Make `when` an expression by removing `return` and using assignment instead."
            )

            // 🔁 Add more topics like this
            "kotlin loops" -> AIChallengeContent(
                topic = "Kotlin For-Loops",
                lesson = "Kotlin supports enhanced for-loops using ranges or collections. It's clean and readable.",
                codeExample = """
                    for (i in 1..5) {
                        println("Step ")
                    }
                """.trimIndent(),
                quiz = listOf(
                    QuizQuestion(
                        question = "What is the output of `for (i in 1..3)`?",
                        options = listOf("1 2", "1 2 3", "0 1 2", "None"),
                        correctAnswer = "1 2 3"
                    ),
                    QuizQuestion(
                        question = "How do you exclude the last number in a range?",
                        options = listOf("1..5", "1 until 5", "1 to 5", "range(1,5)"),
                        correctAnswer = "1 until 5"
                    )
                ),
                challenge = """
                    Print all even numbers between 2 and 10 using a for loop.
                """.trimIndent(),
                expectedOutcome = """
                    for (i in 2..10 step 2) {
                        println(i)
                    }
                """.trimIndent()
            )

            else -> AIChallengeContent(
                topic = topic,
                lesson = "Lesson not available. Ask the AI coach to add it soon!",
                codeExample = "// Code will be added later",
                quiz = emptyList(),
                challenge = "// Challenge will be available soon",
                expectedOutcome = "// Expected solution will be shown here"
            )
        }
    }
}
