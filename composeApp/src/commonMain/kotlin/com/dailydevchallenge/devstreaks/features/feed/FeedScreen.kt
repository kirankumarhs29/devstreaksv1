package com.dailydevchallenge.devstreaks.features.feed

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FeedScreen() {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(24.dp)) {
        Text("🌍 Dev Community Feed", style = MaterialTheme.typography.headlineSmall)

        // Placeholder content
        repeat(5) { i ->
            Card(modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Post #$i", style = MaterialTheme.typography.titleMedium)
                    Text("Exciting progress update or tip from the community!", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
