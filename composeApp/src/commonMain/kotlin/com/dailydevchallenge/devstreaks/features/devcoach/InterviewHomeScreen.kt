package com.dailydevchallenge.devstreaks.features.devcoach

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import com.dailydevchallenge.devstreaks.features.navigation.DevStreakTopBar

@Composable
fun InterviewHomeScreen(
    onBack: () -> Unit = {},
    challenges: List<ChallengeItem> = List(5) { index ->
        ChallengeItem(
            id = index,
            title = "Challenge ${index + 1}",
            description = "Identify the logical error in this snippet…"
        )
    },
    onChallengeClick: (ChallengeItem) -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize()) {
        DevStreakTopBar(
            title = "Daily Interview",
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Today's 5 Smart Challenges",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(challenges) { challenge ->
                ChallengeCard(
                    title = challenge.title,
                    description = challenge.description,
                    onClick = { onChallengeClick(challenge) }
                )
            }
        }
    }
}

data class ChallengeItem(
    val id: Int,
    val title: String,
    val description: String
)


@Composable
fun RadarChart(skills: List<String>, scores: List<Float>) {
    Box(modifier = Modifier
        .height(200.dp)
        .fillMaxWidth()
        .background(Color.LightGray, shape = RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("Radar Chart Placeholder")
    }
}

@Composable
fun DailyChallengeSection() {
    Column {
        Text("Today's 5 Smart Challenges", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        LazyColumn {
            items(5) { index ->
                ChallengeCard(
                    title = "Challenge ${index + 1}",
                    description = "Identify the logical error in this snippet…",
                    onClick = { /* Navigate to full challenge */ }
                )
            }
        }
    }
}
@Composable
fun WeeklyTrendSection() {
    Column {
        Text("Weekly Progress", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(Color(0xFFDBEAFE), shape = RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Trend Graph Placeholder")
        }
    }
}
@Composable
fun InterviewReadinessDashboard() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "🎯 My Interview Readiness",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        AIScoreBox(score = 43)

        Spacer(modifier = Modifier.height(24.dp))

        RadarSkillChart(
            logic = 0.7f,
            optimization = 0.5f,
            explanation = 0.4f
        )

        Spacer(modifier = Modifier.height(24.dp))

        WeeklyTrendGraph()

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = { /* Show Daily Challenges */ }) {
            Text("View Daily Challenges")
        }
    }
}

@Composable
fun AIScoreBox(score: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFDEF3FF))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("AI Readiness Score", fontWeight = FontWeight.Medium)
            Text(
                "You're $score% ready for a mid-level FAANG interview",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun RadarSkillChart(logic: Float, optimization: Float, explanation: Float) {
    val maxRadius = 100f
    val centerX = 150f
    val centerY = 150f

    Canvas(modifier = Modifier.size(300.dp)) {
        val points = listOf(logic, optimization, explanation)
        val angles = listOf(0f, 2f * PI.toFloat() / 3f, 4f * PI.toFloat() / 3f)

        val coords = angles.mapIndexed { i, angle ->
            Offset(
                x = centerX + cos(angle) * points[i] * maxRadius,
                y = centerY + sin(angle) * points[i] * maxRadius
            )
        }

        // Draw radar lines
        drawCircle(Color.LightGray, radius = maxRadius, center = Offset(centerX, centerY), style = Stroke(2f))
        coords.forEachIndexed { i, point ->
            drawLine(Color.Blue, Offset(centerX, centerY), point, strokeWidth = 4f)
        }
    }
}

@Composable
fun WeeklyTrendGraph() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("📈 Weekly Progress")
            Spacer(modifier = Modifier.height(8.dp))
            // Replace with actual chart later
            Text("Mon 📉 Tue 🔁 Wed 📈 Thu 📈 Fri 📉", fontSize = 14.sp)
        }
    }
}

