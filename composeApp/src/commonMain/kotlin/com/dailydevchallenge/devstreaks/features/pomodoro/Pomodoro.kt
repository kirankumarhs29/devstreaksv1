package com.dailydevchallenge.devstreaks.features.pomodoro

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.dailydevchallenge.devstreaks.tts.TTSHelper
import com.mohamedrejeb.calf.core.LocalPlatformContext

fun Int.pad2(): String = this.toString().padStart(2, '0')
@Composable
fun PomodoroScreen(
    ttsHelper: TTSHelper,
    onSessionComplete: (xp: Int, badge: String?) -> Unit,
    getPomodoroCount: () -> Int,
) {
    var isRunning by remember { mutableStateOf(false) }
    var isFocusMode by remember { mutableStateOf(true) }
    var sessionCount by remember { mutableStateOf(0) }

    val context = LocalPlatformContext.current
    var customDurationText by remember { mutableStateOf("") }

    val durationOptions = listOf(15, 25, 45)
    var selectedDuration by remember { mutableStateOf(25) }
    val customDuration = customDurationText.toIntOrNull()
    val effectiveDuration = customDuration ?: selectedDuration

    val totalTime = if (isFocusMode) effectiveDuration * 60L else 300L

    var timeLeft by remember { mutableStateOf(effectiveDuration * 60L) }
    val colorScheme = MaterialTheme.colorScheme

    // Countdown logic
    LaunchedEffect(isRunning, timeLeft) {
        if (isRunning && timeLeft > 0) {
            delay(1000L)
            timeLeft--
        } else if (isRunning && timeLeft == 0L) {
            isRunning = false
            sessionCount++
            val totalCount = getPomodoroCount() + 1

            val badge = when (totalCount) {
                10 -> "Focus Newbie"
                25 -> "Focus Guru"
                50 -> "Streak Master"
                else -> null
            }

            val earnedXP = 10
            onSessionComplete(earnedXP, badge)

            ttsHelper.speak("Great job! You've earned $earnedXP XP")
            badge?.let { ttsHelper.speak("Congratulations! You've earned the $it badge") }

            // Reset
            isFocusMode = !isFocusMode
            timeLeft = if (isFocusMode) effectiveDuration * 60L else 300L
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Gray)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isFocusMode) "Focus Time" else "Break Time",
            color = colorScheme.onBackground,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressBar(
                percentage = (timeLeft.toFloat() / totalTime.toFloat()),
                color = if (isFocusMode) Color(0xFF00BCD4) else Color(0xFF4CAF50),
                strokeWidth = 12f
            )
            val minutes = (timeLeft / 60).toInt()
            val seconds = (timeLeft % 60).toInt()
            val formattedTime = "${minutes.pad2()}:${seconds.pad2()}"

            Text(
                text = formattedTime,
                color = colorScheme.onBackground,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            durationOptions.forEach { duration ->
                Button(
                    onClick = {
                        selectedDuration = duration
                        timeLeft = duration * 60L
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedDuration == duration) Color(0xFF2ECC71) else Color.DarkGray
                    )
                ) {
                    Text("${duration}m")
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Custom input
        OutlinedTextField(
            value = customDurationText,
            onValueChange = { input ->
                if (!isRunning && input.length <= 3 && input.all { it.isDigit() }) {
                    customDurationText = input
                    customDuration?.let {
                        selectedDuration = it
                        timeLeft = it * 60L
                        isRunning = false
                        isFocusMode = true
                    }
                }
            },
            label = { Text("Custom duration (min)", color = Color.White) },
            textStyle = LocalTextStyle.current.copy(color = Color.White),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                disabledIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            ),
            modifier = Modifier.width(220.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { isRunning = !isRunning },
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.primary,
                contentColor = colorScheme.onPrimary
            )
        ) {
            Text(text = if (isRunning) "Pause" else "Start")
        }

        Button(
            onClick = {
                isRunning = false
                timeLeft = effectiveDuration * 60L
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.surfaceVariant,
                contentColor = colorScheme.onSurface
            )
        ) {
            Text("Reset")
        }


        Spacer(modifier = Modifier.height(16.dp))


        Text(
            text = "Sessions Completed: $sessionCount",
            color = colorScheme.onSurfaceVariant,
            fontSize = 16.sp
        )
    }
}
@Composable
fun CircularProgressBar(
    percentage: Float,
    color: Color,
    strokeWidth: Float
) {
    Canvas(modifier = Modifier.size(200.dp)) {
        val sweepAngle = 360 * percentage
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(strokeWidth, cap = StrokeCap.Round),
            size = Size(size.width, size.height),
            topLeft = Offset.Zero
        )
    }
}
